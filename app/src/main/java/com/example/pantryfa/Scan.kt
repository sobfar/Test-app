package com.example.pantryfa

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

// کادر راهنما: بخش‌های اطراف تیره، گوشه‌ها و خط قرمز وسط
class ViewfinderView(context: Context) : View(context) {
    private val d = context.resources.displayMetrics.density
    private val dim = Paint().apply { color = Color.parseColor("#99000000") }
    private val corner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = 4f * d
        strokeCap = Paint.Cap.ROUND
    }
    private val line = Paint().apply {
        color = Color.parseColor("#E53935")
        strokeWidth = 2f * d
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 15f * d
        textAlign = Paint.Align.CENTER
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val fw = w * 0.86f
        val fh = fw * 0.5f
        val cx = w / 2f
        val cy = h * 0.38f
        val l = cx - fw / 2f
        val r = cx + fw / 2f
        val t = cy - fh / 2f
        val b = cy + fh / 2f

        canvas.drawRect(0f, 0f, w, t, dim)
        canvas.drawRect(0f, b, w, h, dim)
        canvas.drawRect(0f, t, l, b, dim)
        canvas.drawRect(r, t, w, b, dim)

        val len = 28f * d
        canvas.drawLine(l, t, l + len, t, corner)
        canvas.drawLine(l, t, l, t + len, corner)
        canvas.drawLine(r, t, r - len, t, corner)
        canvas.drawLine(r, t, r, t + len, corner)
        canvas.drawLine(l, b, l + len, b, corner)
        canvas.drawLine(l, b, l, b - len, corner)
        canvas.drawLine(r, b, r - len, b, corner)
        canvas.drawLine(r, b, r, b - len, corner)

        canvas.drawLine(l + 10f * d, cy, r - 10f * d, cy, line)

        canvas.drawText("بارکد را وسط کادر بگیر", cx, b + 36f * d, text)
        canvas.drawText("گوشی را ثابت و ۱۵ تا ۲۰ سانت دورتر نگه دار", cx, b + 62f * d, text)
        canvas.drawText("نور کم است؟ فلش را روشن کن", cx, b + 88f * d, text)
    }
}

class ScanActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var flashBtn: TextView
    private val executor = Executors.newSingleThreadExecutor()
    private var done = false

    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
            if (ok) startCamera() else finish()
        }

    private fun circle(color: String): GradientDrawable {
        val g = GradientDrawable()
        g.shape = GradientDrawable.OVAL
        g.setColor(Color.parseColor(color))
        return g
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "اسکن بارکد"
        val d = resources.displayMetrics.density

        val frame = FrameLayout(this)
        previewView = PreviewView(this)
        frame.addView(
            previewView,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        frame.addView(
            ViewfinderView(this),
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )

        val size = (64 * d).toInt()
        flashBtn = TextView(this)
        flashBtn.text = "🔦"
        flashBtn.textSize = 28f
        flashBtn.gravity = Gravity.CENTER
        flashBtn.background = circle("#AA000000")
        flashBtn.visibility = View.GONE
        val lp = FrameLayout.LayoutParams(size, size, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
        lp.bottomMargin = (48 * d).toInt()
        frame.addView(flashBtn, lp)
        setContentView(frame)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            permLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            val provider = future.get()
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)

            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E,
                    Barcode.FORMAT_CODE_128, Barcode.FORMAT_CODE_39,
                    Barcode.FORMAT_QR_CODE
                )
                .build()
            val scanner = BarcodeScanning.getClient(options)

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(executor) { proxy ->
                val media = proxy.image
                if (media == null || done) {
                    proxy.close()
                } else {
                    val img = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
                    scanner.process(img)
                        .addOnSuccessListener { codes ->
                            val v = codes.firstOrNull()?.rawValue
                            if (v != null && !done) {
                                done = true
                                setResult(RESULT_OK, Intent().putExtra("code", v))
                                finish()
                            }
                        }
                        .addOnCompleteListener { proxy.close() }
                }
            }
            provider.unbindAll()
            val camera = provider.bindToLifecycle(
                this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis
            )
            if (camera.cameraInfo.hasFlashUnit()) {
                flashBtn.visibility = View.VISIBLE
                var on = false
                flashBtn.setOnClickListener {
                    on = !on
                    camera.cameraControl.enableTorch(on)
                    flashBtn.background = circle(if (on) "#FFEB3B" else "#AA000000")
                }
            }
        }, ContextCompat.getMainExecutor(this))
    }
}
