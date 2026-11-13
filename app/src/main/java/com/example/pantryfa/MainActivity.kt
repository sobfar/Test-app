package com.example.pantryfa

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.time.LocalDate
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var list: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private var items: MutableList<Item> = mutableListOf()

    private val scanLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
            val code = r.data?.getStringExtra("code")
            if (r.resultCode == RESULT_OK && code != null) showAddDialog(code)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Rz"

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(24, 24, 24, 24)
        }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val scanBtn = Button(this).apply { text = "اسکن بارکد" }
        scanBtn.setOnClickListener {
            scanLauncher.launch(Intent(this@MainActivity, ScanActivity::class.java))
        }
        val addBtn = Button(this).apply { text = "افزودن دستی" }
        addBtn.setOnClickListener { showAddDialog("") }
        row.addView(scanBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(addBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(row)

        val hint = TextView(this).apply { text = "برای حذف یک کالا، روی آن نگه دارید" }
        root.addView(hint)

        list = ListView(this)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf<String>())
        list.adapter = adapter
        list.setOnItemLongClickListener { _, _, pos, _ ->
            AlertDialog.Builder(this)
                .setTitle("حذف شود؟")
                .setPositiveButton("حذف") { _, _ ->
                    items.removeAt(pos)
                    Store.save(this, items)
                    refresh()
                }
                .setNegativeButton("لغو", null)
                .show()
            true
        }
        root.addView(list, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        val req = PeriodicWorkRequest.Builder(ExpiryWorker::class.java, 1, TimeUnit.DAYS).build()
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork("expiry", ExistingPeriodicWorkPolicy.KEEP, req)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        items = Store.load(this)
        items.sortBy { it.date }
        adapter.clear()
        for (it in items) {
            val d = daysLeft(it)
            val status = when {
                d < 0 -> "منقضی شده (" + toFa("${-d}") + " روز پیش)"
                d == 0 -> "امروز منقضی می‌شود"
                else -> toFa("$d") + " روز مانده"
            }
            adapter.add(it.name + "\n" + Jalali.format(LocalDate.parse(it.date)) + " — " + status)
        }
        adapter.notifyDataSetChanged()
    }

    private fun showAddDialog(barcode: String) {
        val known = if (barcode.isEmpty()) null else items.firstOrNull { it.barcode == barcode }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(48, 24, 48, 0)
        }
        if (barcode.isNotEmpty()) {
            box.addView(TextView(this).apply { text = "بارکد: $barcode" })
        }
        val name = EditText(this).apply {
            hint = "نام کالا"
            setText(known?.name ?: "")
        }
        val date = EditText(this).apply {
            hint = "تاریخ انقضا شمسی، مثلا " + Jalali.format(LocalDate.now().plusDays(30))
            inputType = InputType.TYPE_CLASS_TEXT
        }
        box.addView(name)
        box.addView(date)
        AlertDialog.Builder(this)
            .setTitle("کالای جدید")
            .setView(box)
            .setPositiveButton("ذخیره") { _, _ ->
                val d = Jalali.parse(date.text.toString())
                val n = name.text.toString().trim()
                if (d == null || n.isEmpty()) {
                    Toast.makeText(this, "نام یا تاریخ نامعتبر است، دوباره امتحان کن", Toast.LENGTH_LONG).show()
                } else {
                    items.add(Item(barcode, n, d.toString()))
                    Store.save(this, items)
                    refresh()
                }
            }
            .setNegativeButton("لغو", null)
            .show()
    }
}

class ScanActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private val executor = Executors.newSingleThreadExecutor()
    private var done = false

    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
            if (ok) startCamera() else finish()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        previewView = PreviewView(this)
        setContentView(previewView)
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
            provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
        }, ContextCompat.getMainExecutor(this))
    }
}
