package com.example.qr_wallet

import android.os.Bundle
import android.content.pm.ActivityInfo
import com.journeyapps.barcodescanner.CaptureActivity

class MyCaptureActivity : CaptureActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
}

