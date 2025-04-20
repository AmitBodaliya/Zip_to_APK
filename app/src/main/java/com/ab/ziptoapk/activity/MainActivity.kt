package com.ab.ziptoapk.activity

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.ab.ziptoapk.MyApplication
import com.ab.ziptoapk.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val filePickerRequestCode = 102



    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(MyApplication().applyCustomTheme())
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        if (Settings.canDrawOverlays(this)) {
            // Show your dialog or bottom sheet
        } else {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            startActivityForResult(intent, 10001)
        }



        //btn select
        binding.btnSelect.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/zip", "application/vnd.android.package-archive") )
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            startActivityForResult(Intent.createChooser(intent, "Select ZIP or APK"), 102)
        }


    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 102 && resultCode == RESULT_OK && data?.data != null) {
            val fileUri = data.data!!

            val sendIntent = Intent(this, ShareActivity::class.java).apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, fileUri)
                type = contentResolver.getType(fileUri) ?: "*/*"
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            startActivity(sendIntent)
        }
    }




}