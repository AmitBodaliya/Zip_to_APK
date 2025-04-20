package com.ab.ziptoapk.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ab.ziptoapk.MyApplication
import com.ab.ziptoapk.R


class PermissionActivity : AppCompatActivity() {

    private val permissionsBelowAndroidQ = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private val permissionsForAndroid13Plus = arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.READ_MEDIA_AUDIO
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(MyApplication().applyCustomTheme())
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission)
        checkAndRequestPermissions()
    }



    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
                Toast.makeText(this, "Grant 'Manage All Files' permission", Toast.LENGTH_LONG).show()
                return
            }
        }

        val neededPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsForAndroid13Plus
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            permissionsBelowAndroidQ
        } else {
            permissionsBelowAndroidQ // fallback for Android 10 - 12
        }

        val toRequest = neededPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (toRequest.isEmpty()) {
            goToMainActivity()
        } else {
            ActivityCompat.requestPermissions(this, toRequest.toTypedArray(), 101)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                goToMainActivity()
            } else {
                Toast.makeText(this, "Permissions denied. Cannot continue.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun goToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
