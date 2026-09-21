package com.example.savecopym

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
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

class MainActivity : AppCompatActivity() {

    private lateinit var tvDestination: TextView
    private lateinit var prefs: SharedPreferences

    private val PREFS_NAME = "SaveCopyMPrefs"
    private val KEY_TREE_URI = "tree_uri"

    // SAF folder picker
    private val folderPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            // Persist permission
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

        // Handle incoming file
        handleIncomingIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
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
                tvDestination.text = doc?.name ?: uriString
            } catch (e: Exception) {
                tvDestination.text = uriString
            }
        }
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return

        val action = intent.action
        val uri: Uri? = when {
            Intent.ACTION_VIEW == action -> intent.data
            Intent.ACTION_SEND == action -> {
                if (intent.type?.startsWith("text/") == true) {
                    // text share, ignore for now or handle differently
                    null
                } else {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
            }
            Intent.ACTION_SEND_MULTIPLE == action -> {
                // Take first file for simplicity
                val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                uris?.firstOrNull()
            }
            else -> null
        }

        if (uri != null) {
            saveFile(uri)
        }
    }

    private fun saveFile(sourceUri: Uri) {
        val treeUriString = prefs.getString(KEY_TREE_URI, null)
        if (treeUriString.isNullOrEmpty()) {
            AlertDialog.Builder(this)
                .setMessage(R.string.no_destination)
                .setPositiveButton(R.string.ok) { _, _ -> finish() }
                .setCancelable(false)
                .show()
            return
        }

        try {
            val treeUri = Uri.parse(treeUriString)
            val treeDoc = DocumentFile.fromTreeUri(this, treeUri)
                ?: throw IOException("Cannot access destination folder")

            // Get original filename
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

            // Success dialog
            val savedPath = "${treeDoc.name}/$fileName"
            AlertDialog.Builder(this)
                .setTitle(R.string.file_saved)
                .setMessage(savedPath)
                .setPositiveButton(R.string.ok) { _, _ -> finish() }
                .setCancelable(false)
                .show()

        } catch (e: Exception) {
            e.printStackTrace()
            AlertDialog.Builder(this)
                .setMessage(getString(R.string.save_failed) + "\n${e.message}")
                .setPositiveButton(R.string.ok) { _, _ -> finish() }
                .setCancelable(false)
                .show()
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
