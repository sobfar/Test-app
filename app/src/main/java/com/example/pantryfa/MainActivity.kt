package com.example.pantryfa

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import java.time.LocalDate
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var list: ListView
    private lateinit var adapter: RowAdapter
    private lateinit var filterSp: Spinner
    private lateinit var searchEt: EditText
    private lateinit var summaryTv: TextView
    private lateinit var emptyTv: TextView
    private var items: MutableList<Item> = mutableListOf()

    // اگر فرم افزودن/ویرایش باز باشد و از داخلش اسکن بزنیم، نتیجه به اینجا می‌رسد
    private var onScanned: ((String) -> Unit)? = null

    private val scanLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
            val code = r.data?.getStringExtra("code")
            val cb = onScanned
            onScanned = null
            if (r.resultCode == RESULT_OK && code != null) {
                if (cb != null) cb(code) else handleScan(code)
            }
        }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private inner class RowAdapter : ArrayAdapter<Item>(this@MainActivity, 0, mutableListOf<Item>()) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val item = getItem(position)!!
            val d = daysLeft(item)
            val warn = Prefs.days(context)
            val status: String
            val color: Int
            when {
                d < 0 -> {
                    status = "منقضی شده (" + toFa("${-d}") + " روز پیش)"
                    color = Color.parseColor("#C62828")
                }
                d == 0 -> {
                    status = "امروز منقضی می‌شود"
                    color = Color.parseColor("#C62828")
                }
                d <= warn -> {
                    status = toFa("$d") + " روز مانده"
                    color = Color.parseColor("#EF6C00")
                }
                else -> {
                    status = toFa("$d") + " روز مانده"
                    color = Color.parseColor("#2E7D32")
                }
            }
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutDirection = View.LAYOUT_DIRECTION_RTL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(8), dp(10), dp(4), dp(10))
            }
            val texts = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
            val title = TextView(context).apply {
                textSize = 17f
                setTypeface(null, android.graphics.Typeface.BOLD)
                text = item.name + "   ×" + toFa("${item.qty}")
            }
            val sub = TextView(context).apply {
                textSize = 13f
                setTextColor(Color.GRAY)
                text = Jalali.format(LocalDate.parse(item.date)) + "  ·  " + item.cat
            }
            val st = TextView(context).apply {
                textSize = 14f
                setTextColor(color)
                text = status
            }
            texts.addView(title)
            texts.addView(sub)
            texts.addView(st)

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
            row.addView(texts, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(edit)
            row.addView(del)
            row.setOnClickListener { showItemDialog(item.barcode, item) }
            return row
        }
    }

    private fun pill(label: String, color: String): Button {
        val b = Button(this)
        b.text = label
        b.setTextColor(Color.WHITE)
        b.isAllCaps = false
        val bg = GradientDrawable()
        bg.cornerRadius = dp(28).toFloat()
        bg.setColor(Color.parseColor(color))
        b.background = bg
        b.setPadding(dp(22), 0, dp(22), 0)
        return b
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Rz"

        val frame = FrameLayout(this)
        frame.layoutDirection = View.LAYOUT_DIRECTION_RTL

        val content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(dp(12), dp(8), dp(12), 0)

        searchEt = EditText(this)
        searchEt.hint = "جستجوی نام کالا یا بارکد"
        searchEt.inputType = InputType.TYPE_CLASS_TEXT
        searchEt.setSingleLine()

        filterSp = Spinner(this)
        filterSp.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, listOf("همه دسته‌ها") + CATS
        )

        summaryTv = TextView(this)
        summaryTv.textSize = 13f
        summaryTv.setTextColor(Color.DKGRAY)
        summaryTv.setPadding(dp(4), dp(4), dp(4), dp(4))

        emptyTv = TextView(this)
        emptyTv.gravity = Gravity.CENTER
        emptyTv.textSize = 16f
        emptyTv.setTextColor(Color.GRAY)

        list = ListView(this)
        adapter = RowAdapter()
        list.adapter = adapter
        list.clipToPadding = false
        list.setPadding(0, 0, 0, dp(100))

        val listFrame = FrameLayout(this)
        listFrame.addView(
            list,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        listFrame.addView(
            emptyTv,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER
            )
        )
        list.emptyView = emptyTv

        content.addView(searchEt)
        content.addView(filterSp)
        content.addView(summaryTv)
        content.addView(listFrame, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        frame.addView(
            content,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )

        val scanPill = pill("📷  اسکن بارکد", "#1565C0")
        scanPill.setOnClickListener {
            scanLauncher.launch(Intent(this, ScanActivity::class.java))
        }
        val addPill = pill("➕  افزودن دستی", "#2E7D32")
        addPill.setOnClickListener { showItemDialog("", null) }

        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.gravity = Gravity.CENTER
        val lp1 = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(52))
        lp1.setMargins(dp(6), 0, dp(6), 0)
        val lp2 = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(52))
        lp2.setMargins(dp(6), 0, dp(6), 0)
        bar.addView(scanPill, lp1)
        bar.addView(addPill, lp2)
        val barLp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM
        )
        barLp.bottomMargin = dp(20)
        frame.addView(bar, barLp)
        setContentView(frame)

        searchEt.doAfterTextChanged { refresh() }
        filterSp.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                refresh()
            }

            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        scheduleReminder(this, true)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, 1, 0, "تقویم")
            .setIcon(android.R.drawable.ic_menu_my_calendar)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu.add(0, 2, 1, "تنظیمات")
            .setIcon(android.R.drawable.ic_menu_preferences)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            1 -> startActivity(Intent(this, CalendarActivity::class.java))
            2 -> startActivity(Intent(this, SettingsActivity::class.java))
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        items = Store.load(this)
        items.sortBy { it.date }
        Store.syncReg(this, items)

        val pos = filterSp.selectedItemPosition
        var shown: List<Item> = if (pos <= 0) items else items.filter { it.cat == CATS[pos - 1] }
        val q = searchEt.text.toString().trim()
        if (q.isNotEmpty()) {
            val qd = normDigits(q)
            shown = shown.filter { it.name.contains(q, true) || it.barcode.contains(qd) }
        }
        adapter.clear()
        adapter.addAll(shown)
        adapter.notifyDataSetChanged()

        val warn = Prefs.days(this)
        var expired = 0
        var soon = 0
        for (item in items) {
            val d = daysLeft(item)
            if (d < 0) expired++ else if (d <= warn) soon++
        }
        summaryTv.text = toFa("${items.size}") + " کالا   |   منقضی‌شده: " + toFa("$expired") +
            "   |   نزدیک انقضا: " + toFa("$soon")
        emptyTv.text = if (items.isEmpty()) "هنوز کالایی ثبت نشده.\nبا دکمه «اسکن بارکد» شروع کن."
        else "کالایی پیدا نشد"
    }

    private fun handleScan(code: String) {
        val matches = items.filter { it.barcode == code }
        if (matches.isEmpty()) {
            showItemDialog(code, null)
            return
        }
        val labels = matches.map {
            it.name + " (×" + toFa("${it.qty}") + ") — " + Jalali.format(LocalDate.parse(it.date))
        }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("این بارکد قبلا ثبت شده. برای ویرایش انتخاب کن")
            .setItems(labels) { _, i -> showItemDialog(code, matches[i]) }
            .setNeutralButton("ثبت جدید با تاریخ دیگر") { _, _ -> showItemDialog(code, null) }
            .setNegativeButton("لغو", null)
            .show()
    }

    // اسم و دسته‌ی یک بارکد را از کالاها یا فهرست بارکدها پیدا می‌کند
    private fun lookup(code: String): Pair<String, String>? {
        if (code.isEmpty()) return null
        val a = items.firstOrNull { it.barcode == code }
        if (a != null) return Pair(a.name, a.cat)
        val r = Store.loadReg(this).firstOrNull { it.barcode == code }
        if (r != null) return Pair(r.name, r.cat)
        return null
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
        val pre = if (existing != null) null else lookup(barcode)

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

        // بارکد (قابل تایپ و ویرایش، یا اسکن)
        val barcodeEt = EditText(this).apply {
            hint = "بارکد (اختیاری)"
            inputType = InputType.TYPE_CLASS_TEXT
            setText(barcode)
        }
        val scanBtn = Button(this).apply { text = "اسکن" }
        val bcRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        bcRow.addView(barcodeEt, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        bcRow.addView(scanBtn)

        val name = EditText(this).apply {
            hint = "نام کالا"
            setText(existing?.name ?: pre?.first ?: "")
        }

        val catSp = Spinner(this)
        catSp.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, CATS)
        val ci = CATS.indexOf(existing?.cat ?: pre?.second ?: "سایر")
        catSp.setSelection(if (ci >= 0) ci else CATS.size - 1)

        scanBtn.setOnClickListener {
            onScanned = { code ->
                barcodeEt.setText(code)
                val f = lookup(code)
                if (f != null) {
                    if (name.text.toString().trim().isEmpty()) name.setText(f.first)
                    val i = CATS.indexOf(f.second)
                    if (i >= 0) catSp.setSelection(i)
                }
            }
            scanLauncher.launch(Intent(this, ScanActivity::class.java))
        }

        // تعداد با دکمه‌های + و −
        val qty = EditText(this).apply {
            setText(if (existing != null) "${existing.qty}" else "1")
            inputType = InputType.TYPE_CLASS_NUMBER
            gravity = Gravity.CENTER
        }
        fun bump(delta: Int) {
            val cur = normDigits(qty.text.toString()).trim().toIntOrNull() ?: 1
            val n = cur + delta
            qty.setText("${if (n < 1) 1 else n}")
        }
        val minus = Button(this).apply { text = "−" }
        val plus = Button(this).apply { text = "+" }
        minus.setOnClickListener { bump(-1) }
        plus.setOnClickListener { bump(1) }
        val qtyRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        qtyRow.addView(minus)
        qtyRow.addView(qty, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        qtyRow.addView(plus)

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

        box.addView(bcRow)
        box.addView(name)
        box.addView(TextView(this).apply { text = "تعداد" })
        box.addView(qtyRow)
        box.addView(TextView(this).apply { text = "دسته‌بندی" })
        box.addView(catSp)
        box.addView(TextView(this).apply { text = "تاریخ انقضا" })
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
                val bc = normDigits(barcodeEt.text.toString()).trim()
                val cat = CATS[catSp.selectedItemPosition]
                val item = Item(bc, n, d.toString(), if (q < 1) 1 else q, cat)
                if (existing != null) {
                    val idx = items.indexOfFirst { it === existing }
                    if (idx >= 0) items[idx] = item else items.add(item)
                } else {
                    items.add(item)
                }
                Store.save(this, items)
                Store.upsertReg(this, bc, n, cat)
                refresh()
                dlg.dismiss()
            }
        }
        dlg.show()
    }
}
