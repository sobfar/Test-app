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
import android.util.Size
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.time.LocalDate
import java.util.concurrent.Executors

// کادر راهنما: بخش‌های اطراف تیره، گوشه‌های سفید، خط قرمز وسط و چند خط متن راهنما
class ViewfinderView(
    context: Context,
    private val ratio: Float,
    private val topPos: Float,
    private val messages: List<String>
) : View(context) {
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
        val fw = w * 0.88f
        val fh = fw * ratio
        val cx = w / 2f
        val cy = h * topPos
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

        for (i in messages.indices) {
            canvas.drawText(messages[i], cx, b + (36f + 26f * i) * d, text)
        }
    }
}

private fun circleBg(color: String): GradientDrawable {
    val g = GradientDrawable()
    g.shape = GradientDrawable.OVAL
    g.setColor(Color.parseColor(color))
    return g
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
            ViewfinderView(
                this, 0.5f, 0.38f,
                listOf(
                    "بارکد را وسط کادر بگیر",
                    "گوشی را ثابت و ۱۵ تا ۲۰ سانت دورتر نگه دار",
                    "نور کم است؟ فلش را روشن کن"
                )
            ),
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )

        val size = (64 * d).toInt()
        flashBtn = TextView(this)
        flashBtn.text = "🔦"
        flashBtn.textSize = 28f
        flashBtn.gravity = Gravity.CENTER
        flashBtn.background = circleBg("#AA000000")
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
                    flashBtn.background = circleBg(if (on) "#FFEB3B" else "#AA000000")
                }
            }
        }, ContextCompat.getMainExecutor(this))
    }
}

// ---------- خواندن تاریخ از روی بسته با دوربین (OCR) ----------
//
// برای دقت بیشتر:
//  - تصویر با رزولوشن بالا تحلیل می‌شود
//  - یک تاریخ فقط وقتی پیشنهاد می‌شود که در چند فریم پشت‌سرهم یکسان خوانده شده باشد
//  - نوع تاریخ (انقضا / تولید) از کلمه‌های EXP, BB, MFG و ... تشخیص داده می‌شود
//  - اگر تاریخ چند معنی داشت (مثلا ۰۵/۰۳/۲۷)، برداشت‌های ممکن به کاربر نشان داده می‌شود
//  - تاپ روی تصویر = فوکوس، نوار زوم و فلش برای نوشته‌های ریز
class DateScanActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var flashBtn: TextView
    private lateinit var statusTv: TextView
    private lateinit var candBox: LinearLayout
    private val executor = Executors.newSingleThreadExecutor()
    private var camera: Camera? = null
    private val recent = ArrayDeque<List<DateCandidate>>()
    private var shownKeys = ""
    private var finished = false
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
            if (ok) startCamera() else finish()
        }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "اسکن تاریخ"

        val frame = FrameLayout(this)
        previewView = PreviewView(this)
        frame.addView(
            previewView,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        frame.addView(
            ViewfinderView(
                this, 0.28f, 0.26f,
                listOf(
                    "تاریخ چاپ‌شده روی بسته را داخل کادر بگیر",
                    "تاریخ باید واضح و بدون سایه باشد؛ اگر ریز است زوم کن",
                    "برای فوکوس، روی تصویر بزن"
                )
            ),
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )

        previewView.setOnTouchListener { _, e ->
            if (e.action == MotionEvent.ACTION_UP) {
                val cam = camera
                if (cam != null) {
                    val pt = previewView.meteringPointFactory.createPoint(e.x, e.y)
                    cam.cameraControl.startFocusAndMetering(FocusMeteringAction.Builder(pt).build())
                }
            }
            true
        }

        val panel = LinearLayout(this)
        panel.orientation = LinearLayout.VERTICAL
        panel.layoutDirection = View.LAYOUT_DIRECTION_RTL
        panel.setBackgroundColor(Color.parseColor("#CC000000"))
        panel.setPadding(dp(12), dp(10), dp(12), dp(14))

        statusTv = TextView(this)
        statusTv.setTextColor(Color.WHITE)
        statusTv.textSize = 14f
        statusTv.text = "در حال خواندن..."
        panel.addView(statusTv)

        candBox = LinearLayout(this)
        candBox.orientation = LinearLayout.VERTICAL
        panel.addView(candBox)

        val zoomRow = LinearLayout(this)
        zoomRow.orientation = LinearLayout.HORIZONTAL
        zoomRow.gravity = Gravity.CENTER_VERTICAL
        zoomRow.setPadding(0, dp(8), 0, dp(4))
        val zoomLabel = TextView(this)
        zoomLabel.text = "زوم"
        zoomLabel.setTextColor(Color.WHITE)
        val zoom = SeekBar(this)
        zoom.max = 100
        zoom.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                camera?.cameraControl?.setLinearZoom(p / 100f)
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        zoomRow.addView(zoomLabel)
        zoomRow.addView(zoom, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        panel.addView(zoomRow)

        flashBtn = TextView(this)
        flashBtn.text = "🔦"
        flashBtn.textSize = 26f
        flashBtn.gravity = Gravity.CENTER
        flashBtn.background = circleBg("#AA444444")
        flashBtn.visibility = View.GONE
        val flp = LinearLayout.LayoutParams(dp(60), dp(60))
        flp.gravity = Gravity.CENTER_HORIZONTAL
        panel.addView(flashBtn, flp)

        frame.addView(
            panel,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM
            )
        )
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

            val selector = ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(Size(1920, 1080), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)
                )
                .build()
            val analysis = ImageAnalysis.Builder()
                .setResolutionSelector(selector)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(executor) { proxy ->
                val media = proxy.image
                if (media == null || finished) {
                    proxy.close()
                } else {
                    val img = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
                    recognizer.process(img)
                        .addOnSuccessListener { vt ->
                            val lines = ArrayList<String>()
                            for (b in vt.textBlocks) {
                                for (l in b.lines) lines.add(l.text)
                            }
                            onFrame(lines)
                        }
                        .addOnCompleteListener { proxy.close() }
                }
            }
            provider.unbindAll()
            val cam = provider.bindToLifecycle(
                this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis
            )
            camera = cam
            if (cam.cameraInfo.hasFlashUnit()) {
                flashBtn.visibility = View.VISIBLE
                var on = false
                flashBtn.setOnClickListener {
                    on = !on
                    cam.cameraControl.enableTorch(on)
                    flashBtn.background = circleBg(if (on) "#FFEB3B" else "#AA444444")
                }
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun onFrame(lines: List<String>) {
        if (finished) return
        val cands = DateOcr.parse(lines, LocalDate.now())
        recent.addLast(cands)
        if (recent.size > 10) recent.removeFirst()

        // هر تاریخ باید حداقل در ۳ فریم از ۱۰ فریم آخر دیده شده باشد
        val counts = HashMap<String, Int>()
        val latest = LinkedHashMap<String, DateCandidate>()
        for (f in recent) {
            val seen = HashSet<String>()
            for (c in f) {
                if (seen.add(c.key)) {
                    counts[c.key] = (counts[c.key] ?: 0) + 1
                    latest[c.key] = c
                }
            }
        }
        val stable = DateOcr.rank(latest.values.filter { (counts[it.key] ?: 0) >= 3 })

        val raw = lines.filter { l -> l.any { ch -> ch.isDigit() } }.take(2).joinToString("  |  ")
        statusTv.text = when {
            stable.isNotEmpty() -> "تاریخ پیدا شد. اگر درست است، انتخابش کن:"
            cands.isEmpty() -> "تاریخی دیده نمی‌شود. نزدیک‌تر کن، زوم کن یا نور را بیشتر کن"
            else -> "در حال بررسی... " + raw
        }

        val keys = stable.joinToString(",") { it.key + it.kind }
        if (keys != shownKeys) {
            shownKeys = keys
            renderCandidates(stable)
        }
    }

    private fun kindLabel(c: DateCandidate): String {
        val base = when (c.kind) {
            DateKind.EXPIRY -> "انقضا"
            DateKind.PRODUCTION -> "تولید"
            DateKind.UNKNOWN -> "تاریخ"
        }
        return if (c.guess) "$base (حدسی)" else base
    }

    private fun renderCandidates(stable: List<DateCandidate>) {
        candBox.removeAllViews()
        for (c in stable.take(3)) {
            val color = when (c.kind) {
                DateKind.EXPIRY -> "#2E7D32"
                DateKind.PRODUCTION -> "#757575"
                DateKind.UNKNOWN -> "#1565C0"
            }
            val b = Button(this)
            b.isAllCaps = false
            b.setTextColor(Color.WHITE)
            val bg = GradientDrawable()
            bg.cornerRadius = dp(24).toFloat()
            bg.setColor(Color.parseColor(color))
            b.background = bg
            val warn = if (c.alternatives.isNotEmpty()) "  ⚠ چند برداشت" else ""
            b.text = kindLabel(c) + ":  " + Jalali.format(c.date) + "   (" + c.date + ")" + warn
            b.setOnClickListener { choose(c) }
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, dp(6), 0, 0)
            candBox.addView(b, lp)
        }
    }

    private fun choose(c: DateCandidate) {
        if (c.alternatives.isEmpty()) {
            finishWith(c.date)
            return
        }
        val all = listOf(c.date) + c.alternatives
        val labels = all.map { Jalali.format(it) + "   (" + it + ")" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("متن «" + c.raw + "» چند برداشت دارد. کدام درست است؟")
            .setItems(labels) { _, i -> finishWith(all[i]) }
            .setNegativeButton("لغو", null)
            .show()
    }

    private fun finishWith(d: LocalDate) {
        finished = true
        setResult(RESULT_OK, Intent().putExtra("date_iso", d.toString()))
        finish()
    }
}
