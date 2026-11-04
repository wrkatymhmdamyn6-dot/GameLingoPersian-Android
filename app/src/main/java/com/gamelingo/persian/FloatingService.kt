package com.gamelingo.persian

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.*
import android.widget.*
import com.google.mlkit.nl.translate.*

class FloatingService : Service() {
    private lateinit var wm: WindowManager
    private var bubble: View? = null
    private var panel: View? = null
    private val purple = Color.rgb(151, 91, 255)
    private val dark = Color.rgb(24, 20, 38)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (bubble == null) showBubble()
        return START_STICKY
    }

    private fun showBubble() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams(
            58, 58,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 18; y = 260
        }
        val b = TextView(this).apply {
            text = "文"
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setBackgroundColor(purple)
            elevation = 12f
        }
        b.setOnTouchListener(object : View.OnTouchListener {
            var downX = 0f; var downY = 0f; var startX = 0; var startY = 0
            var moved = false
            override fun onTouch(v: View, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = e.rawX; downY = e.rawY
                        startX = params.x; startY = params.y; moved = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (downX - e.rawX).toInt()
                        val dy = (e.rawY - downY).toInt()
                        if (kotlin.math.abs(dx) + kotlin.math.abs(dy) > 8) moved = true
                        params.x = (startX + dx).coerceAtLeast(0)
                        params.y = (startY + dy).coerceAtLeast(0)
                        wm.updateViewLayout(b, params)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!moved) togglePanel()
                        return true
                    }
                }
                return false
            }
        })
        wm.addView(b, params)
        bubble = b
    }

    private fun togglePanel() {
        if (panel != null) {
            try { wm.removeView(panel) } catch (_: Exception) {}
            panel = null
            return
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 20, 22, 18)
            setBackgroundColor(dark)
        }
        val heading = TextView(this).apply {
            text = "GameLingo  •  English → فارسی"
            textSize = 17f
            setTextColor(Color.WHITE)
        }
        val input = EditText(this).apply {
            hint = "متن انگلیسی را اینجا وارد کن"
            setHintTextColor(Color.LTGRAY)
            setTextColor(Color.WHITE)
            textSize = 16f
            minLines = 2
            setBackgroundColor(Color.rgb(43, 36, 62))
        }
        val output = TextView(this).apply {
            text = "ترجمه اینجا نمایش داده می‌شود"
            textSize = 17f
            setTextColor(Color.WHITE)
            setPadding(0, 18, 0, 12)
        }
        val translate = Button(this).apply {
            text = "ترجمه به فارسی"
            setTextColor(Color.WHITE)
            setBackgroundTintList(android.content.res.ColorStateList.valueOf(purple))
            setOnClickListener {
                val source = input.text.toString().trim()
                if (source.isEmpty()) {
                    output.text = "اول متن انگلیسی را وارد کن."
                } else {
                    output.text = "در حال آماده‌سازی ترجمه…"
                    translateText(source) { result -> output.text = result }
                }
            }
        }
        val close = Button(this).apply {
            text = "بستن"
            setOnClickListener { togglePanel() }
        }
        box.addView(heading)
        box.addView(input)
        box.addView(translate)
        box.addView(output)
        box.addView(close)
        val p = WindowManager.LayoutParams(
            (resources.displayMetrics.widthPixels * 0.88).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            flags = flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
            dimAmount = 0.25f
        }
        wm.addView(box, p)
        panel = box
    }

    private fun translateText(source: String, done: (String) -> Unit) {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.PERSIAN)
            .build()
        val translator = Translation.getClient(options)
        translator.downloadModelIfNeeded()
            .addOnSuccessListener {
                translator.translate(source)
                    .addOnSuccessListener { translated ->
                        done(translated)
                        translator.close()
                    }
                    .addOnFailureListener {
                        done("ترجمه انجام نشد. اتصال اینترنت و دانلود مدل زبان را بررسی کن.")
                        translator.close()
                    }
            }
            .addOnFailureListener {
                done("دانلود مدل ترجمه ناموفق بود. یک‌بار با اینترنت تلاش کن.")
                translator.close()
            }
    }

    override fun onDestroy() {
        try { panel?.let { wm.removeView(it) } } catch (_: Exception) {}
        try { bubble?.let { wm.removeView(it) } } catch (_: Exception) {}
        super.onDestroy()
    }
}
