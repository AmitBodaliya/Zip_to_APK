package com.ab.ziptoapk;

import android.app.Application;
import android.os.Build;

import com.google.android.material.color.DynamicColors;


class MyApplication : Application() {


    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)



    }

    fun applyCustomTheme(): Int {
        return R.style.Theme_App_MC
//        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            R.style.Theme_App_M3
//        } else {
//            R.style.Theme_App_MC
//        }
    }


    fun applyCustomThemeTran(): Int {
        return R.style.Theme_App_Tran_MC
//        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            R.style.Theme_App_Tran_M3
//        } else {
//            R.style.Theme_App_Tran_MC
//        }
    }

}