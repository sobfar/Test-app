package com.example.pantryfa

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
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
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
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
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate
import java.util.Locale

class RowHolder(
    val row: LinearLayout,
    val title: TextView,
    val sub: TextView,
    val status: TextView,
    val use: Button,
    val edit: ImageButton,
    val del: ImageButton
) : RecyclerView.ViewHolder(row)

class MainActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: RowAdapter
    private lateinit var filterSp: Spinner
    private lateinit var searchEt: EditText
    private lateinit var summaryTv: TextView
    private lateinit var emptyTv: TextView
    private lateinit var undoBar: LinearLayout
    private lateinit var undoText: TextView
    private lateinit var undoBtn: Button
    private var items: MutableList<Item> = mutableListOf()
    private val hideUndo = Runnable { undoBar.visibility = View.GONE }

    // اگر فرم افزودن/ویرایش باز باشد و از داخلش اسکن بزنیم، نتیجه به اینجا می‌رسد
    private var onScanned: ((String) -> Unit)? = null
    private var onDate: ((LocalDate) -> Unit)? = null

    private val scanLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
            val code = r.data?.getStringExtra("code")
            val cb = onScanned
            onScanned = null
            if (r.resultCode == RESULT_OK && code != null) {
                if (cb != null) cb(code) else handleScan(code)
            }
        }

    private val dateLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
            val iso = r.data?.getStringExtra("date_iso")
            val cb = onDate
            onDate = null
            if (r.resultCode == RESULT_OK && iso != null && cb != null) cb(LocalDate.parse(iso))
        }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun roundBg(color: String, radius: Int): GradientDrawable {
        val bg = GradientDrawable()
        bg.cornerRadius = dp(radius).toFloat()
        bg.setColor(Color.parseColor(color))
        return bg
    }

    private inner class RowAdapter : RecyclerView.Adapter<RowHolder>() {
        var data: List<Item> = emptyList()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowHolder {
            val ctx = this@MainActivity
            val row = LinearLayout(ctx)
            row.orientation = LinearLayout.HORIZONTAL
            row.layoutDirection = View.LAYOUT_DIRECTION_RTL
            row.gravity = Gravity.CENTER_VERTICAL
            row.setBackgroundColor(Color.WHITE)
            row.setPadding(dp(8), dp(10), dp(4), dp(10))
            row.layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )

            val texts = LinearLayout(ctx)
            texts.orientation = LinearLayout.VERTICAL
            val title = TextView(ctx)
            title.textSize = 17f
            title.setTypeface(null, Typeface.BOLD)
            val sub = TextView(ctx)
            sub.textSize = 13f
            sub.setTextColor(Color.GRAY)
            val st = TextView(ctx)
            st.textSize = 14f
            texts.addView(title)
            texts.addView(sub)
            texts.addView(st)

            val use = Button(ctx)
            use.text = "مصرف −۱"
            use.isAllCaps = false
            use.setTextColor(Color.WHITE)
            use.textSize = 13f
            use.background = roundBg("#00897B", 18)
            use.minWidth = 0
            use.minimumWidth = 0
            use.setPadding(dp(12), 0, dp(12), 0)

            val edit = ImageButton(ctx)
            edit.setImageResource(android.R.drawable.ic_menu_edit)
            edit.setBackgroundColor(Color.TRANSPARENT)
            val del = ImageButton(ctx)
            del.setImageResource(android.R.drawable.ic_menu_delete)
            del.setBackgroundColor(Color.TRANSPARENT)

            row.addView(texts, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(use, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(38)))
            row.addView(edit)
            row.addView(del)
            return RowHolder(row, title, sub, st, use, edit, del)
        }

        override fun onBindViewHolder(h: RowHolder, position: Int) {
            val item = data[position]
            val d = daysLeft(item)
            val warn = Prefs.days(this@MainActivity)
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
            h.title.text = item.name + "   ×" + toFa("${item.qty}")
            h.sub.text = Jalali.format(LocalDate.parse(item.date)) + "  ·  " + item.cat
            h.status.text = status
            h.status.setTextColor(color)
            h.use.setOnClickListener { consume(item) }
            h.edit.setOnClickListener { showItemDialog(item.barcode, item, false) }
            h.del.setOnClickListener { deleteItem(item) }
            h.row.setOnClickListener { showItemDialog(item.barcode, item, false) }
        }

        override fun getItemCount(): Int = data.size
    }

    private fun pill(label: String, color: String): Button {
        val b = Button(this)
        b.text = label
        b.setTextColor(Color.WHITE)
        b.isAllCaps = false
        b.background = roundBg(color, 28)
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

        adapter = RowAdapter()
        recycler = RecyclerView(this)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter
        recycler.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        recycler.clipToPadding = false
        recycler.setPadding(0, 0, 0, dp(100))

        // کشیدن ردیف به چپ یا راست = حذف (با امکان بازگردانی)
        val red = Paint()
        red.color = Color.parseColor("#E53935")
        val swipe = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                val pos = vh.bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION && pos < adapter.data.size) {
                    deleteItem(adapter.data[pos])
                }
            }

            override fun onChildDraw(
                c: Canvas, rv: RecyclerView, vh: RecyclerView.ViewHolder,
                dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
            ) {
                val v = vh.itemView
                if (dX > 0) {
                    c.drawRect(v.left.toFloat(), v.top.toFloat(), v.left + dX, v.bottom.toFloat(), red)
                } else if (dX < 0) {
                    c.drawRect(v.right + dX, v.top.toFloat(), v.right.toFloat(), v.bottom.toFloat(), red)
                }
                super.onChildDraw(c, rv, vh, dX, dY, actionState, isCurrentlyActive)
            }
        }
        ItemTouchHelper(swipe).attachToRecyclerView(recycler)

        val listFrame = FrameLayout(this)
        listFrame.addView(
            recycler,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        listFrame.addView(
            emptyTv,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER
            )
        )

        content.addView(searchEt)
        content.addView(filterSp)
        content.addView(summaryTv)
        content.addView(listFrame, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        frame.addView(
            content,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )

        // نوار بازگردانی
        undoBar = LinearLayout(this)
        undoBar.orientation = LinearLayout.HORIZONTAL
        undoBar.gravity = Gravity.CENTER_VERTICAL
        undoBar.layoutDirection = View.LAYOUT_DIRECTION_RTL
        undoBar.background = roundBg("#323232", 10)
        undoBar.setPadding(dp(14), dp(4), dp(6), dp(4))
        undoBar.visibility = View.GONE
        undoText = TextView(this)
        undoText.setTextColor(Color.WHITE)
        undoBtn = Button(this)
        undoBtn.text = "بازگردانی"
        undoBtn.setTextColor(Color.parseColor("#FFD54F"))
        undoBtn.setBackgroundColor(Color.TRANSPARENT)
        undoBar.addView(undoText, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        undoBar.addView(undoBtn)
        val undoLp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM
        )
        undoLp.setMargins(dp(12), 0, dp(12), dp(88))
        frame.addView(undoBar, undoLp)

        val scanPill = pill("📷  اسکن بارکد", "#1565C0")
        scanPill.setOnClickListener {
            scanLauncher.launch(Intent(this, ScanActivity::class.java))
        }
        val addPill = pill("➕  افزودن دستی", "#2E7D32")
        addPill.setOnClickListener { showItemDialog("", null, false) }

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
        adapter.data = shown
        adapter.notifyDataSetChanged()

        val warn = Prefs.days(this)
        var expired = 0
        var soon = 0
        for (item in items) {
            val d = daysLeft(item)
            if (d < 0) expired++ else if (d <= warn) soon++
        }
        summaryTv.text = toFa("${items.size}") + " ردیف   |   منقضی‌شده: " + toFa("$expired") +
            "   |   نزدیک انقضا: " + toFa("$soon")
        emptyTv.text = if (items.isEmpty()) "هنوز کالایی ثبت نشده.\nبا دکمه «اسکن بارکد» شروع کن."
        else "کالایی پیدا نشد"
        emptyTv.visibility = if (shown.isEmpty()) View.VISIBLE else View.GONE
    }

    // ---------- مصرف، حذف و بازگردانی ----------

    private fun showUndo(msg: String, snapshot: List<Item>) {
        undoText.text = msg
        undoBar.visibility = View.VISIBLE
        undoBtn.setOnClickListener {
            Store.save(this, snapshot)
            undoBar.visibility = View.GONE
            refresh()
        }
        undoBar.removeCallbacks(hideUndo)
        undoBar.postDelayed(hideUndo, 6000)
    }

    private fun consume(item: Item) {
        val idx = items.indexOfFirst { it === item }
        if (idx < 0) return
        val snap = items.toList()
        if (item.qty > 1) {
            items[idx] = item.copy(qty = item.qty - 1)
            Store.save(this, items)
            refresh()
            showUndo("«" + item.name + "» مصرف شد؛ " + toFa("${item.qty - 1}") + " عدد مانده", snap)
        } else {
            items.removeAt(idx)
            Store.save(this, items)
            refresh()
            showUndo("«" + item.name + "» تمام شد و حذف شد", snap)
        }
    }

    private fun deleteItem(item: Item) {
        val idx = items.indexOfFirst { it === item }
        if (idx < 0) return
        val snap = items.toList()
        items.removeAt(idx)
        Store.save(this, items)
        refresh()
        showUndo("«" + item.name + "» حذف شد", snap)
    }

    // ---------- اسکن ----------

    private fun handleScan(code: String) {
        val matches = items.filter { it.barcode == code }
        if (matches.isEmpty()) {
            showItemDialog(code, null, true)
            return
        }
        val labels = matches.map {
            it.name + " (×" + toFa("${it.qty}") + ") — " + Jalali.format(LocalDate.parse(it.date))
        }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("این بارکد قبلا ثبت شده. برای ویرایش انتخاب کن")
            .setItems(labels) { _, i -> showItemDialog(code, matches[i], true) }
            .setNeutralButton("ثبت جدید با تاریخ دیگر") { _, _ -> showItemDialog(code, null, true) }
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
        box.addView(col("سال", yearP), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        box.addView(col("ماه", monthP), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        box.addView(col("روز", dayP), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

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

    // existing == null یعنی کالای جدید، وگرنه ویرایش.
    // fromScan: فرم بعد از اسکن بارکد باز شده (برای «اسکن پشت‌سرهم»)
    private fun showItemDialog(barcode: String, existing: Item?, fromScan: Boolean) {
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
        bcRow.addView(barcodeEt, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
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
        qtyRow.addView(qty, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        qtyRow.addView(plus)

        // تاریخ: تایپ، تقویم یا خواندن از روی بسته با دوربین
        val date = EditText(this).apply {
            hint = "تاریخ شمسی، مثلا " + Jalali.format(LocalDate.now().plusDays(30))
            inputType = InputType.TYPE_CLASS_TEXT
            if (existing != null) setText(Jalali.format(LocalDate.parse(existing.date)))
        }
        val pickBtn = Button(this).apply { text = "تقویم" }
        pickBtn.setOnClickListener { pickDate(date) }
        val camBtn = Button(this).apply { text = "📷 تاریخ" }
        camBtn.setOnClickListener {
            onDate = { d ->
                date.setText(Jalali.format(d))
                Toast.makeText(this, "تاریخ از روی بسته پر شد؛ یک بار بررسی‌اش کن", Toast.LENGTH_LONG).show()
            }
            dateLauncher.launch(Intent(this, DateScanActivity::class.java))
        }
        val dateRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        dateRow.addView(date, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        dateRow.addView(pickBtn)
        dateRow.addView(camBtn)

        // میان‌بر: به تاریخ داخل فیلد اضافه می‌شود (اگر خالی بود، از امروز)
        val chipRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val todayBtn = Button(this).apply {
            text = "امروز"
            isAllCaps = false
        }
        todayBtn.setOnClickListener { date.setText(Jalali.format(LocalDate.now())) }
        chipRow.addView(todayBtn)
        fun chip(label: String, f: (LocalDate) -> LocalDate) {
            val b = Button(this)
            b.text = label
            b.isAllCaps = false
            b.setOnClickListener {
                val base = Jalali.parse(date.text.toString()) ?: LocalDate.now()
                date.setText(Jalali.format(f(base)))
            }
            chipRow.addView(b)
        }
        chip("+۱ هفته") { it.plusDays(7) }
        chip("+۱ ماه") { addJalaliMonths(it, 1) }
        chip("+۳ ماه") { addJalaliMonths(it, 3) }
        chip("+۶ ماه") { addJalaliMonths(it, 6) }
        chip("+۱ سال") { addJalaliMonths(it, 12) }
        chip("+۲ سال") { addJalaliMonths(it, 24) }
        val chipScroll = HorizontalScrollView(this)
        chipScroll.addView(chipRow)

        box.addView(bcRow)
        box.addView(name)
        box.addView(TextView(this).apply { text = "تعداد" })
        box.addView(qtyRow)
        box.addView(TextView(this).apply { text = "دسته‌بندی" })
        box.addView(catSp)
        box.addView(TextView(this).apply { text = "تاریخ انقضا" })
        box.addView(dateRow)
        box.addView(
            TextView(this).apply {
                text = "میان‌بر: به تاریخ بالا اضافه می‌شود (اگر خالی باشد، از امروز). مثلا تاریخ تولید را بخوان و «+۱۲ ماه» بزن."
                textSize = 12f
                setTextColor(Color.GRAY)
            }
        )
        box.addView(chipScroll)

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
                val iso = d.toString()
                val item = Item(bc, n, iso, if (q < 1) 1 else q, cat)
                var merged = false
                if (existing != null) {
                    val idx = items.indexOfFirst { it === existing }
                    if (idx >= 0) items[idx] = item else items.add(item)
                } else {
                    // اگر همین کالا با همین تاریخ قبلا ثبت شده، فقط تعدادش زیاد می‌شود
                    val idx = items.indexOfFirst {
                        it.date == iso && (
                            (bc.isNotEmpty() && it.barcode == bc) ||
                                (bc.isEmpty() && it.barcode.isEmpty() && it.name == n)
                            )
                    }
                    if (idx >= 0) {
                        val old = items[idx]
                        items[idx] = old.copy(qty = old.qty + item.qty)
                        merged = true
                    } else {
                        items.add(item)
                    }
                }
                Store.save(this, items)
                Store.upsertReg(this, bc, n, cat)
                refresh()
                dlg.dismiss()
                if (merged) {
                    Toast.makeText(this, "به تعداد کالای قبلی (با همین تاریخ) اضافه شد", Toast.LENGTH_SHORT).show()
                }
                if (fromScan && Prefs.continuous(this)) {
                    Toast.makeText(this, "ذخیره شد. بارکد بعدی را اسکن کن", Toast.LENGTH_SHORT).show()
                    scanLauncher.launch(Intent(this, ScanActivity::class.java))
                }
            }
        }
        dlg.show()
    }
}
