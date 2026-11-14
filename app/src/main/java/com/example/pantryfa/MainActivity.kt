package com.example.pantryfa

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.NumberPicker
import android.widget.ScrollView
import android.widget.Spinner
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
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

val CATS = listOf("یخچال", "فریزر", "کابینت و انبار", "دارو", "بهداشتی و آرایشی", "سایر")

class MainActivity : AppCompatActivity() {

    private lateinit var list: ListView
    private lateinit var adapter: RowAdapter
    private lateinit var filterSp: Spinner
    private var items: MutableList<Item> = mutableListOf()

    private val scanLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
            val code = r.data?.getStringExtra("code")
            if (r.resultCode == RESULT_OK && code != null) showItemDialog(code, null)
        }

    private inner class RowAdapter : ArrayAdapter<Item>(this@MainActivity, 0, mutableListOf<Item>()) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val item = getItem(position)!!
            val d = daysLeft(item)
            val status = when {
                d < 0 -> "منقضی شده (" + toFa("${-d}") + " روز پیش)"
                d == 0 -> "امروز منقضی می‌شود"
                else -> toFa("$d") + " روز مانده"
            }
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutDirection = View.LAYOUT_DIRECTION_RTL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(8, 16, 8, 16)
            }
            val tv = TextView(context).apply {
                textSize = 16f
                text = item.name + " (×" + toFa("${item.qty}") + ")  [" + item.cat + "]\n" +
                    Jalali.format(LocalDate.parse(item.date)) + " — " + status
            }
            val edit = ImageButton(context).apply {
                setImageResource(android.R.drawable.ic_menu_edit)
                setBackgroundColor(Color.TRANSPARENT)
                setOnClickListener { showItemDialog(item.barcode, item) }
            }
            val del = ImageButton(context).apply {
                setImageResource(android.R.drawable.ic_menu_delete)
                setBackgroundColor(Color.TRANSPARENT)
                setOnClickListener {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("حذف «" + item.name + "»؟")
                        .setPositiveButton("حذف") { _, _ ->
                            val idx = items.indexOfFirst { it === item }
                            if (idx >= 0) {
                                items.removeAt(idx)
                                Store.save(this@MainActivity, items)
                                refresh()
                            }
                        }
                        .setNegativeButton("لغو", null)
                        .show()
                }
            }
            row.addView(tv, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(edit)
            row.addView(del)
            return row
        }
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
        addBtn.setOnClickListener { showItemDialog("", null) }
        row.addView(scanBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(addBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(row)

        list = ListView(this)
        adapter = RowAdapter()
        list.adapter = adapter

        filterSp = Spinner(this)
        filterSp.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, listOf("همه دسته‌ها") + CATS
        )
        filterSp.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                refresh()
            }

            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        root.addView(filterSp)
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
        val pos = filterSp.selectedItemPosition
        val shown = if (pos <= 0) items else items.filter { it.cat == CATS[pos - 1] }
        adapter.clear()
        adapter.addAll(shown)
        adapter.notifyDataSetChanged()
    }

    private fun pickDate(target: EditText) {
        val cur = Jalali.parse(target.text.toString()) ?: LocalDate.now().plusDays(30)
        val j = Jalali.g2j(cur.year, cur.monthValue, cur.dayOfMonth)

        val yearP = NumberPicker(this).apply {
            minValue = j[0] - 2
            maxValue = j[0] + 10
            value = j[0]
            wrapSelectorWheel = false
        }
        val monthP = NumberPicker(this).apply {
            minValue = 1
            maxValue = 12
            value = j[1]
        }
        val dayP = NumberPicker(this).apply {
            minValue = 1
            maxValue = if (j[1] > 6) 30 else 31
            value = j[2]
        }
        monthP.setOnValueChangedListener { _, _, nv -> dayP.maxValue = if (nv > 6) 30 else 31 }

        fun col(label: String, p: NumberPicker): LinearLayout {
            val c = LinearLayout(this)
            c.orientation = LinearLayout.VERTICAL
            c.gravity = Gravity.CENTER_HORIZONTAL
            val t = TextView(this)
            t.text = label
            t.gravity = Gravity.CENTER
            c.addView(t)
            c.addView(p)
            return c
        }

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            gravity = Gravity.CENTER
            setPadding(24, 24, 24, 0)
        }
        box.addView(col("سال", yearP), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        box.addView(col("ماه", monthP), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        box.addView(col("روز", dayP), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        AlertDialog.Builder(this)
            .setTitle("انتخاب تاریخ شمسی")
            .setView(box)
            .setPositiveButton("تایید") { _, _ ->
                val txt = String.format(Locale.US, "%04d/%02d/%02d", yearP.value, monthP.value, dayP.value)
                if (Jalali.parse(txt) == null) {
                    Toast.makeText(this, "تاریخ نامعتبر است", Toast.LENGTH_SHORT).show()
                } else {
                    target.setText(toFa(txt))
                }
            }
            .setNegativeButton("لغو", null)
            .show()
    }

    // existing == null یعنی کالای جدید، وگرنه ویرایش
    private fun showItemDialog(barcode: String, existing: Item?) {
        val known = if (existing != null || barcode.isEmpty()) null
        else items.firstOrNull { it.barcode == barcode }
        val base = existing ?: known

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(48, 24, 48, 0)
        }
        val err = TextView(this).apply {
            setTextColor(Color.RED)
            textSize = 15f
            visibility = View.GONE
        }
        val hideErr = Runnable { err.visibility = View.GONE }
        fun showErr(msg: String) {
            err.text = msg
            err.visibility = View.VISIBLE
            err.removeCallbacks(hideErr)
            err.postDelayed(hideErr, 3000)
        }
        box.addView(err)
        if (barcode.isNotEmpty()) {
            box.addView(TextView(this).apply { text = "بارکد: $barcode" })
        }

        val name = EditText(this).apply {
            hint = "نام کالا"
            setText(base?.name ?: "")
        }
        val qty = EditText(this).apply {
            hint = "تعداد"
            setText(if (existing != null) "${existing.qty}" else "1")
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        val catSp = Spinner(this)
        catSp.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, CATS)
        val ci = CATS.indexOf(base?.cat ?: "سایر")
        catSp.setSelection(if (ci >= 0) ci else CATS.size - 1)

        val date = EditText(this).apply {
            hint = "تاریخ شمسی، مثلا " + Jalali.format(LocalDate.now().plusDays(30))
            inputType = InputType.TYPE_CLASS_TEXT
            if (existing != null) setText(Jalali.format(LocalDate.parse(existing.date)))
        }
        val pickBtn = Button(this).apply { text = "تقویم" }
        pickBtn.setOnClickListener { pickDate(date) }
        val dateRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        dateRow.addView(date, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        dateRow.addView(pickBtn)

        box.addView(name)
        box.addView(qty)
        box.addView(catSp)
        box.addView(dateRow)

        val scroll = ScrollView(this)
        scroll.addView(box)

        val dlg = AlertDialog.Builder(this)
            .setTitle(if (existing != null) "ویرایش کالا" else "کالای جدید")
            .setView(scroll)
            .setPositiveButton("ذخیره", null)
            .setNegativeButton("لغو", null)
            .create()
        dlg.setOnShowListener {
            dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val n = name.text.toString().trim()
                if (n.isEmpty()) {
                    showErr("نام کالا را وارد کن")
                    return@setOnClickListener
                }
                val d = Jalali.parse(date.text.toString())
                if (d == null) {
                    showErr("تاریخ نامعتبر است")
                    return@setOnClickListener
                }
                val q = normDigits(qty.text.toString()).trim().toIntOrNull() ?: 1
                val item = Item(barcode, n, d.toString(), if (q < 1) 1 else q, CATS[catSp.selectedItemPosition])
                if (existing != null) {
                    val idx = items.indexOfFirst { it === existing }
                    if (idx >= 0) items[idx] = item else items.add(item)
                } else {
                    items.add(item)
                }
                Store.save(this, items)
                refresh()
                dlg.dismiss()
            }
        }
        dlg.show()
    }
}

class ScanActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var flashBtn: Button
    private val executor = Executors.newSingleThreadExecutor()
    private var done = false

    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
            if (ok) startCamera() else finish()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val frame = FrameLayout(this)
        previewView = PreviewView(this)
        frame.addView(
            previewView,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        flashBtn = Button(this).apply {
            text = "روشن کردن فلش"
            visibility = View.GONE
        }
        val lp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.TOP or Gravity.END
        )
        lp.setMargins(32, 96, 32, 0)
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
                    flashBtn.text = if (on) "خاموش کردن فلش" else "روشن کردن فلش"
                }
            }
        }, ContextCompat.getMainExecutor(this))
    }
}
