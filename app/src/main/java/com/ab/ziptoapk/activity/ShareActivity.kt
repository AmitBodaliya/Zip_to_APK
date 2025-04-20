package com.ab.ziptoapk.activity

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.ab.ziptoapk.MyApplication
import com.ab.ziptoapk.R
import com.ab.ziptoapk.databinding.ActivityShareBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textview.MaterialTextView
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import androidx.core.graphics.drawable.toDrawable
import java.io.InputStream
import java.io.OutputStream

class ShareActivity : AppCompatActivity() {

    private val permissionRequestCode = 101

    companion object {
        private const val INSTALL_REQUEST_CODE = 1001
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(MyApplication().applyCustomThemeTran())
        super.onCreate(savedInstanceState)




        if (needsStoragePermission() && !hasStoragePermission()) {
            requestStoragePermission()
        } else {
            handleSharedZip(intent)
        }
    }





    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSharedZip(intent)
    }







    ///////////////////////////////////////// Handle PERMISSION /////////////////////////////////////////

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionRequestCode &&
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            handleSharedZip(intent)
        } else {
            Toast.makeText(this, "Permission denied. Cannot read ZIP file.", Toast.LENGTH_LONG).show()
        }
    }

    private fun needsStoragePermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
    }

    private fun hasStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            permissionRequestCode
        )
    }








    ///////////////////////////////////////// Handle INPUT /////////////////////////////////////////
    private fun handleSharedZip(intent: Intent?) {
        if (intent == null) {
            showToastAndFinish("Intent is null")
            return
        }

        val uri: Uri? = when (intent.action) {
            Intent.ACTION_SEND -> intent.getParcelableExtra(Intent.EXTRA_STREAM)
            Intent.ACTION_VIEW -> intent.data
            else -> null
        }

        if (uri == null) {
            showToastAndFinish("No file received")
            return
        }

        val fileName = getFileNameFromUri(uri)
        if (fileName == null) {
            showToastAndFinish("Unable to detect file type")
            return
        }

        when {
            fileName.endsWith(".zip", ignoreCase = true) -> {
                val zipFile = getFileFromUri(uri, "shared.zip")
                if (zipFile != null) unzipAndHandle(zipFile)
                else showToastAndFinish("Invalid ZIP file")
            }

            fileName.endsWith(".apk", ignoreCase = true) -> {
                val apkFile = getFileFromUri(uri, "shared.apk")
                if (apkFile != null) installAPK(apkFile)
                else showToastAndFinish("Invalid APK file")
            }

            else -> {
                showToastAndFinish("Unsupported file type: $fileName")
            }
        }
    }




    private fun getFileNameFromUri(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    result = it.getString(it.getColumnIndexOrThrow(android.provider.OpenableColumns.DISPLAY_NAME))
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result.substring(cut + 1)
            }
        }
        return result
    }




    private fun getFileFromUri(uri: Uri, outputFileName: String): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val outFile = File(cacheDir, outputFileName)
            FileOutputStream(outFile).use { output ->
                inputStream.copyTo(output)
            }
            outFile
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }




    private fun unzipAndHandle(zipFile: File) {
        try {
            val targetDir = File(cacheDir, "unzipped")
            if (!targetDir.exists()) targetDir.mkdirs()

            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                var entry: ZipEntry?
                val buffer = ByteArray(1024)

                while (zis.nextEntry.also { entry = it } != null) {
                    val entryName = entry!!.name

                    // Security: Avoid Zip Slip vulnerability
                    val outFile = File(targetDir, entryName).canonicalFile
                    if (!outFile.path.startsWith(targetDir.canonicalPath)) {
                        throw SecurityException("Blocked Zip Slip attempt: $entryName")
                    }

                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { fos ->
                            var len: Int
                            while (zis.read(buffer).also { len = it } > 0) {
                                fos.write(buffer, 0, len)
                            }
                        }
                    }
                }
            }

            // Look for the APK file inside unzipped folder
            val apkFile = targetDir.listFiles()?.find { it.name.endsWith(".apk", ignoreCase = true) }

            if (apkFile != null && apkFile.exists()) {
                showActionBottomSheet(apkFile)
            } else {
                showToastAndFinish("No APK found inside ZIP")
            }

        } catch (e: Exception) {
            e.printStackTrace()
            showToastAndFinish("Failed to unzip file: ${e.message}")
        }
    }




    ////////////////////////////////////////// Show Dialog /////////////////////////////////////////

    private fun showActionBottomSheet(apkFile: File) {

        val dialog = BottomSheetDialog(this, R.style.BottomSheetDialogStyle)
        dialog.setContentView(R.layout.bottom_sheet_actions)


        dialog.findViewById<LinearLayout>(R.id.installAPK)?.setOnClickListener {
            installAPK(apkFile)
            dialog.dismiss()
        }

        dialog.findViewById<LinearLayout>(R.id.shareAPK)?.setOnClickListener {
            shareAPK(apkFile)
            dialog.dismiss()
        }

        dialog.findViewById<LinearLayout>(R.id.saveAPK)?.setOnClickListener {
            saveAPK(apkFile)
            dialog.dismiss()
        }


        dialog.setOnDismissListener { finish() }

        dialog.show()
    }




    private fun installAPK(apkFile: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            if (packageManager.canRequestPackageInstalls()) {
                val apkUri = FileProvider.getUriForFile(this, "$packageName.provider", apkFile)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(apkUri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
                finish()
            } else {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                startActivityForResult(intent, INSTALL_REQUEST_CODE)
            }
        } else {
            // For lower versions, just start the installation
            val apkUri = FileProvider.getUriForFile(this, "$packageName.provider", apkFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            finish()
        }
    }




    private fun showToastAndFinish(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        finish()
    }





    private fun shareAPK(apkFile: File) {
        val uri = FileProvider.getUriForFile(this, "$packageName.provider", apkFile)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.android.package-archive"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Share APK"))
    }


    private fun saveAPK(apkFile: File) {
        // Check if external storage is available
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Use scoped storage (Android 10 and above)
            val downloadsDirectory = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val destinationFile = File(downloadsDirectory, apkFile.name)
            copyFile(apkFile, destinationFile)
        } else {
            // For older versions, use legacy external storage
            val downloadsDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val destinationFile = File(downloadsDirectory, apkFile.name)
            copyFile(apkFile, destinationFile)
        }
    }


    private fun copyFile(sourceFile: File, destinationFile: File) {
        try {
            // Create input and output streams
            val inputStream: InputStream = FileInputStream(sourceFile)
            val outputStream: OutputStream = FileOutputStream(destinationFile)

            // Buffer for file copy
            val buffer = ByteArray(1024)
            var length: Int
            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }

            // Close streams
            inputStream.close()
            outputStream.close()

            // Notify user that file is saved
            showToastAndFinish("APK saved to Downloads folder")

        } catch (e: IOException) {
            e.printStackTrace()
            showToastAndFinish("Failed to save APK")
        }
    }



}
