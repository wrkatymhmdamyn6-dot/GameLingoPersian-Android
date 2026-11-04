package com.gamelingo.persian

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.graphics.Color
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {
    private val bg = Color.rgb(14, 12, 24)
    private val purple = Color.rgb(151, 91, 255)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 48, 28, 24)
            setBackgroundColor(bg)
        }
        val title = TextView(this).apply {
            text = "GameLingo فارسی"
            textSize = 28f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        val subtitle = TextView(this).apply {
            text = "مترجم همراه بازی‌های تو"
            textSize = 15f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 30)
        }
        val info = TextView(this).apply {
            text = "نسخه آزمایشی ۰٫۱\n\nاین نسخه پنجره شناور و ترجمه متن انگلیسی به فارسی را فراهم می‌کند. برای ترجمه مستقیم نوشته‌های داخل بازی، مرحله تشخیص متن از تصویر (OCR) باید اضافه شود."
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(18, 20, 18, 20)
            setBackgroundColor(Color.rgb(31, 26, 48))
        }
        val enable = Button(this).apply {
            text = "فعال‌کردن دکمه شناور"
            setTextColor(Color.WHITE)
            setBackgroundTintList(android.content.res.ColorStateList.valueOf(purple))
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName"))
                    startActivity(intent)
                } else {
                    startService(Intent(this@MainActivity, FloatingService::class.java))
                    Toast.makeText(this@MainActivity, "دکمه شناور فعال شد", Toast.LENGTH_SHORT).show()
                }
            }
        }
        val note = TextView(this).apply {
            text = "رایگان • رابط فارسی • تم مشکی و بنفش"
            textSize = 13f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 28, 0, 0)
        }
        root.addView(title)
        root.addView(subtitle)
        root.addView(info)
        root.addView(enable)
        root.addView(note)
        setContentView(root)
    }
}
