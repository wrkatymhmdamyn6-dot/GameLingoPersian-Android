package com.gamelingo.persian

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.graphics.Color
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {
    private val bg = Color.rgb(14, 12, 24)
    private val purple = Color.rgb(151, 91, 255)
    private val captureRequest = 4102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 48, 28, 24)
            setBackgroundColor(bg)
        }
        root.addView(TextView(this).apply {
            text = "GameLingo فارسی"; textSize = 28f
            setTextColor(Color.WHITE); gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = "مترجم زنده بازی‌ها • English → فارسی"
            textSize = 15f; setTextColor(Color.LTGRAY); gravity = Gravity.CENTER
            setPadding(0, 8, 0, 24)
        })
        root.addView(TextView(this).apply {
            text = "۱) مجوز پنجره شناور را فعال کن.\n۲) اجازه ضبط صفحه را تأیید کن.\n۳) وارد بازی شو و روی حباب بنفش بزن تا نوشته‌های قابل مشاهده شناسایی و ترجمه شوند.\n\nتصویر صفحه فقط برای OCR روی دستگاه پردازش می‌شود."
            textSize = 15f; setTextColor(Color.WHITE); setPadding(18,20,18,20)
            setBackgroundColor(Color.rgb(31,26,48))
        })
        root.addView(Button(this).apply {
            text = "فعال‌کردن مترجم شناور"; setTextColor(Color.WHITE)
            setBackgroundTintList(android.content.res.ColorStateList.valueOf(purple))
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                } else {
                    val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                    startActivityForResult(manager.createScreenCaptureIntent(), captureRequest)
                }
            }
        })
        root.addView(TextView(this).apply {
            text = "رایگان • OCR روی دستگاه • تم مشکی و بنفش"
            textSize = 13f; setTextColor(Color.LTGRAY); gravity = Gravity.CENTER
            setPadding(0,24,0,0)
        })
        setContentView(root)
    }

    @Deprecated("Deprecated API retained for compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == captureRequest && resultCode == RESULT_OK && data != null) {
            val service = Intent(this, FloatingService::class.java).apply {
                putExtra(FloatingService.EXTRA_RESULT_CODE, resultCode)
                putExtra(FloatingService.EXTRA_RESULT_DATA, data)
            }
            if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(service) else startService(service)
            Toast.makeText(this, "مترجم شناور فعال شد؛ وارد بازی شو.", Toast.LENGTH_LONG).show()
        }
    }
}
