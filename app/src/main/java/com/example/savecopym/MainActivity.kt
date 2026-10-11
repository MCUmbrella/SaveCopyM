package com.example.savecopym

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import java.io.IOException
import java.net.URLDecoder

class MainActivity : AppCompatActivity() {

    private lateinit var tvDestination: TextView
    private lateinit var prefs: SharedPreferences

    private val PREFS_NAME = "SaveCopyMPrefs"
    private val KEY_TREE_URI = "tree_uri"

    // Prevent processing the same incoming intent more than once
    private var hasProcessedIncoming = false

    // SAF folder picker
    private val folderPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, takeFlags)

            prefs.edit().putString(KEY_TREE_URI, uri.toString()).apply()
            updateDestinationDisplay()
            Toast.makeText(this, "Destination set", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvDestination = findViewById(R.id.tvDestination)
        val btnSelect = findViewById<Button>(R.id.btnSelect)

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        btnSelect.setOnClickListener {
            folderPicker.launch(null)
        }

        updateDestinationDisplay()

        // Only handle incoming intent on first creation (not configuration change)
        // and only if we haven't processed it yet in this activity instance.
        if (savedInstanceState == null) {
            handleIncomingIntent(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Reset flag so a brand-new open-with can still work
        hasProcessedIncoming = false
        handleIncomingIntent(intent)
    }

    private fun updateDestinationDisplay() {
        val uriString = prefs.getString(KEY_TREE_URI, null)
        if (uriString.isNullOrEmpty()) {
            tvDestination.text = getString(R.string.none)
        } else {
            try {
                val uri = Uri.parse(uriString)
                val doc = DocumentFile.fromTreeUri(this, uri)
                // Prefer a human-readable path if possible
                val display = getReadableTreePath(uri) ?: (doc?.name ?: uriString)
                tvDestination.text = display
            } catch (e: Exception) {
                tvDestination.text = uriString
            }
        }
    }

    /**
     * Try to turn a SAF tree URI into something readable like
     * "/sdcard/Download/SCM" or at least "primary:Download/SCM"
     */
    private fun getReadableTreePath(treeUri: Uri): String? {
        return try {
            val pathSeg = treeUri.lastPathSegment ?: return null
            // Typical form: primary:Download/SCM  or  raw:...
            val decoded = URLDecoder.decode(pathSeg, "UTF-8")
            when {
                decoded.startsWith("primary:") -> {
                    "/sdcard/" + decoded.removePrefix("primary:")
                }
                decoded.contains(":") -> {
                    // e.g. XXXX-XXXX:Download/foo  →  show as-is after colon
                    "/storage/" + decoded
                }
                else -> decoded
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null || hasProcessedIncoming) return

        val action = intent.action
        val uri: Uri? = when {
            Intent.ACTION_VIEW == action -> intent.data
            Intent.ACTION_SEND == action -> {
                if (intent.type?.startsWith("text/") == true) {
                    null
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(Intent.EXTRA_STREAM)
                    }
                }
            }
            Intent.ACTION_SEND_MULTIPLE == action -> {
                val uris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                }
                uris?.firstOrNull()
            }
            else -> null
        }

        if (uri != null) {
            hasProcessedIncoming = true
            // Clear the intent data immediately so that if the activity is
            // brought back from Recents it won't re-trigger a save.
            intent.data = null
            intent.replaceExtras(Bundle())
            intent.action = Intent.ACTION_MAIN
            setIntent(intent)

            saveFile(uri)
        }
    }

    private fun saveFile(sourceUri: Uri) {
        val treeUriString = prefs.getString(KEY_TREE_URI, null)
        if (treeUriString.isNullOrEmpty()) {
            AlertDialog.Builder(this)
                .setMessage(R.string.no_destination)
                .setPositiveButton(R.string.ok) { _, _ -> finishAndCleanup() }
                .setCancelable(false)
                .show()
            return
        }

        try {
            val treeUri = Uri.parse(treeUriString)
            val treeDoc = DocumentFile.fromTreeUri(this, treeUri)
                ?: throw IOException("Cannot access destination folder")

            var fileName = getFileName(sourceUri) ?: "saved_file_${System.currentTimeMillis()}"

            // Avoid name conflict
            var destFile = treeDoc.findFile(fileName)
            if (destFile != null && destFile.exists()) {
                val base = fileName.substringBeforeLast('.', fileName)
                val ext = if (fileName.contains('.')) ".${fileName.substringAfterLast('.')}" else ""
                var i = 1
                do {
                    fileName = "${base}_$i$ext"
                    destFile = treeDoc.findFile(fileName)
                    i++
                } while (destFile != null && destFile.exists())
            }

            val newFile = treeDoc.createFile(
                contentResolver.getType(sourceUri) ?: "application/octet-stream",
                fileName
            ) ?: throw IOException("Failed to create file in destination")

            contentResolver.openInputStream(sourceUri)?.use { input ->
                contentResolver.openOutputStream(newFile.uri)?.use { output ->
                    input.copyTo(output)
                } ?: throw IOException("Cannot open output stream")
            } ?: throw IOException("Cannot open input stream")

            // Build a nicer path for the dialog
            val folderPath = getReadableTreePath(treeUri) ?: (treeDoc.name ?: "selected folder")
            val savedPath = "$folderPath/$fileName"

            AlertDialog.Builder(this)
                .setTitle(R.string.file_saved)
                .setMessage(savedPath)
                .setPositiveButton(R.string.ok) { _, _ -> finishAndCleanup() }
                .setCancelable(false)
                .show()

        } catch (e: Exception) {
            e.printStackTrace()
            AlertDialog.Builder(this)
                .setMessage(getString(R.string.save_failed) + "\n${e.message}")
                .setPositiveButton(R.string.ok) { _, _ -> finishAndCleanup() }
                .setCancelable(false)
                .show()
        }
    }

    /**
     * Finish the activity and remove it from Recents so the old VIEW intent
     * cannot be revived and cause a duplicate save.
     */
    private fun finishAndCleanup() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask()
        } else {
            finish()
        }
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        name = cursor.getString(index)
                    }
                }
            }
        }
        if (name == null) {
            name = uri.lastPathSegment
        }
        return name
    }
}
