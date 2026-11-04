package com.gamelingo.persian

import android.app.*
import android.content.*
import android.graphics.*
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.view.*
import android.widget.*
import androidx.core.app.NotificationCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.nl.translate.*

class FloatingService : Service() {
    companion object {
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        private const val CHANNEL = "gamelingo_capture"
    }
    private lateinit var wm: WindowManager
    private var bubble: View? = null
    private var panel: View? = null
    private var projection: MediaProjection? = null
    private var busy = false
    private val purple = Color.rgb(151, 91, 255)
    private val dark = Color.rgb(24, 20, 38)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL, "GameLingo screen translation", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        startForeground(7, NotificationCompat.Builder(this, CHANNEL)
            .setContentTitle("GameLingo فعال است")
            .setContentText("برای ترجمه روی حباب بنفش بزن")
            .setSmallIcon(android.R.drawable.ic_menu_search).setOngoing(true).build())
        val code = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val data = if (Build.VERSION.SDK_INT >= 33)
            intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        else @Suppress("DEPRECATION") (intent?.getParcelableExtra(EXTRA_RESULT_DATA) as? Intent)
        if (projection == null && data != null && code == Activity.RESULT_OK) {
            val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projection = mgr.getMediaProjection(code, data)
        }
        if (bubble == null) showBubble()
        return START_NOT_STICKY
    }

    private fun showBubble() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val p = WindowManager.LayoutParams(58, 58, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT)
            .apply { gravity = Gravity.TOP or Gravity.END; x = 18; y = 260 }
        val b = TextView(this).apply {
            text = "文"; textSize = 22f; gravity = Gravity.CENTER
            setTextColor(Color.WHITE); setBackgroundColor(purple); elevation = 12f
        }
        b.setOnTouchListener(object : View.OnTouchListener {
            var sx=0f; var sy=0f; var px=0; var py=0; var moved=false
            override fun onTouch(v: View, e: MotionEvent): Boolean {
                when(e.action) {
                    MotionEvent.ACTION_DOWN -> { sx=e.rawX; sy=e.rawY; px=p.x; py=p.y; moved=false; return true }
                    MotionEvent.ACTION_MOVE -> {
                        val dx=(sx-e.rawX).toInt(); val dy=(e.rawY-sy).toInt()
                        if (kotlin.math.abs(dx)+kotlin.math.abs(dy)>8) moved=true
                        p.x=(px+dx).coerceAtLeast(0); p.y=(py+dy).coerceAtLeast(0); wm.updateViewLayout(b,p); return true
                    }
                    MotionEvent.ACTION_UP -> { if(!moved) captureAndTranslate(); return true }
                }
                return false
            }
        })
        wm.addView(b,p); bubble=b
    }

    private fun captureAndTranslate() {
        if (busy) return
        val media = projection
        if (media == null) { showResult("مجوز ضبط صفحه فعال نیست. برنامه را دوباره باز کن و اجازه بده."); return }
        busy=true; showResult("در حال خواندن متن صفحه…")
        val dm=resources.displayMetrics
        val reader=ImageReader.newInstance(dm.widthPixels,dm.heightPixels,PixelFormat.RGBA_8888,2)
        val display=media.createVirtualDisplay("GameLingoOCR",dm.widthPixels,dm.heightPixels,dm.densityDpi,
            android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,reader.surface,null,null)
        var received=false
        reader.setOnImageAvailableListener({ r ->
            var image: Image?=null
            try {
                image=r.acquireLatestImage()
                if(image!=null && !received) {
                    received=true
                    val bitmap=imageToBitmap(image!!)
                    display.release(); r.close()
                    recognize(bitmap)
                }
            } catch (_: Exception) {
                display.release(); r.close(); busy=false; showResult("گرفتن تصویر ناموفق بود.")
            } finally { image?.close() }
        },Handler(mainLooper))
        Handler(mainLooper).postDelayed({
            if(!received) { try { display.release(); reader.close() } catch (_:Exception) {}; busy=false; showResult("تصویر صفحه دریافت نشد؛ دوباره تلاش کن.") }
        },1800)
    }

    private fun imageToBitmap(image: Image): Bitmap {
        val plane=image.planes[0]; val stride=plane.pixelStride
        val padded=Bitmap.createBitmap(image.width+(plane.rowStride-stride*image.width)/stride,image.height,Bitmap.Config.ARGB_8888)
        padded.copyPixelsFromBuffer(plane.buffer)
        val result=Bitmap.createBitmap(padded,0,0,image.width,image.height); padded.recycle(); return result
    }

    private fun recognize(bitmap: Bitmap) {
        val client=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        client.process(InputImage.fromBitmap(bitmap,0))
            .addOnSuccessListener { result ->
                bitmap.recycle()
                val found=result.text.trim()
                if(found.isEmpty()) { busy=false; showResult("متنی پیدا نشد؛ صفحه را واضح‌تر کن."); client.close() }
                else translate(found) { translated -> busy=false; showResult(translated); client.close() }
            }
            .addOnFailureListener { bitmap.recycle(); busy=false; showResult("تشخیص متن ناموفق بود."); client.close() }
    }

    private fun translate(source: String, done: (String)->Unit) {
        val opts=TranslatorOptions.Builder().setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.PERSIAN).build()
        val translator=Translation.getClient(opts)
        translator.downloadModelIfNeeded().addOnSuccessListener {
            translator.translate(source).addOnSuccessListener { done(it); translator.close() }
                .addOnFailureListener { done("ترجمه انجام نشد؛ دوباره تلاش کن."); translator.close() }
        }.addOnFailureListener { done("دانلود مدل ترجمه ناموفق بود؛ اینترنت را بررسی کن."); translator.close() }
    }

    private fun showResult(message: String) {
        try { panel?.let { wm.removeView(it) } } catch (_:Exception) {}
        val box=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(22,20,22,18); setBackgroundColor(dark) }
        box.addView(TextView(this).apply { text="GameLingo • ترجمه صفحه"; textSize=17f; setTextColor(Color.WHITE) })
        box.addView(TextView(this).apply { text=message; textSize=17f; setTextColor(Color.WHITE); setPadding(0,16,0,12) })
        box.addView(Button(this).apply { text="بستن"; setOnClickListener { try { wm.removeView(box) } catch (_:Exception) {}; panel=null } })
        val p=WindowManager.LayoutParams((resources.displayMetrics.widthPixels*.88).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,PixelFormat.TRANSLUCENT)
            .apply { gravity=Gravity.CENTER }
        try { wm.addView(box,p); panel=box } catch (_:Exception) {}
    }

    override fun onDestroy() {
        try { panel?.let { wm.removeView(it) } } catch (_:Exception) {}
        try { bubble?.let { wm.removeView(it) } } catch (_:Exception) {}
        projection?.stop(); super.onDestroy()
    }
}
