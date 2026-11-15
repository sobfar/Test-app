package com.example.pantryfa

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.time.LocalDate
import java.util.Locale

class CalendarActivity : AppCompatActivity() {

    private var jy = 0
    private var jm = 0
    private lateinit var header: TextView
    private lateinit var gridBox: LinearLayout
    private lateinit var listTv: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "تقویم انقضا"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val t = LocalDate.now()
        val j = Jalali.g2j(t.year, t.monthValue, t.dayOfMonth)
        jy = j[0]
        jm = j[1]

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(16, 16, 16, 16)
        }
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val prev = Button(this).apply { text = "ماه قبل" }
        val next = Button(this).apply { text = "ماه بعد" }
        header = TextView(this).apply {
            gravity = Gravity.CENTER
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
        }
        prev.setOnClickListener {
            jm -= 1
            if (jm < 1) {
                jm = 12
                jy -= 1
            }
            render()
        }
        next.setOnClickListener {
            jm += 1
            if (jm > 12) {
                jm = 1
                jy += 1
            }
            render()
        }
        nav.addView(prev)
        nav.addView(header, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        nav.addView(next)
        root.addView(nav)

        gridBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        listTv = TextView(this).apply {
            textSize = 16f
            setPadding(8, 24, 8, 24)
        }
        val inner = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        inner.addView(gridBox)
        inner.addView(listTv)
        val scroll = ScrollView(this)
        scroll.addView(inner)
        root.addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun cell(text: String, bg: Int?, bold: Boolean, color: Int): TextView {
        val tv = TextView(this)
        tv.text = text
        tv.gravity = Gravity.CENTER
        tv.textSize = 14f
        tv.setPadding(0, 18, 0, 18)
        tv.setTextColor(color)
        if (bold) tv.setTypeface(null, Typeface.BOLD)
        if (bg != null) tv.setBackgroundColor(bg)
        return tv
    }

    private fun render() {
        val all = Store.load(this)
        val byDay = HashMap<Int, MutableList<Item>>()
        for (it in all) {
            val d = LocalDate.parse(it.date)
            val j = Jalali.g2j(d.year, d.monthValue, d.dayOfMonth)
            if (j[0] == jy && j[1] == jm) byDay.getOrPut(j[2]) { mutableListOf() }.add(it)
        }
        header.text = MONTHS[jm - 1] + " " + toFa("$jy")

        val len = monthLen(jy, jm)
        val g = Jalali.j2g(jy, jm, 1)
        val first = (LocalDate.of(g[0], g[1], g[2]).dayOfWeek.value + 1) % 7

        val t = LocalDate.now()
        val tj = Jalali.g2j(t.year, t.monthValue, t.dayOfMonth)

        gridBox.removeAllViews()
        val names = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
        val head = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        for (n in names) {
            head.addView(
                cell(n, null, true, Color.DKGRAY),
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            )
        }
        gridBox.addView(head)

        var day = 1 - first
        while (day <= len) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutDirection = View.LAYOUT_DIRECTION_RTL
            }
            for (c in 0..6) {
                val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                lp.setMargins(2, 2, 2, 2)
                if (day in 1..len) {
                    val cnt = byDay[day]?.size ?: 0
                    val isToday = (jy == tj[0] && jm == tj[1] && day == tj[2])
                    val txt = toFa("$day") + if (cnt > 0) "\n" + toFa("$cnt") + " کالا" else ""
                    val bg = if (cnt > 0) Color.parseColor("#FFCDD2") else Color.parseColor("#F5F5F5")
                    row.addView(cell(txt, bg, isToday, if (isToday) Color.BLUE else Color.BLACK), lp)
                } else {
                    row.addView(cell("", null, false, Color.BLACK), lp)
                }
                day++
            }
            gridBox.addView(row)
        }

        if (byDay.isEmpty()) {
            listTv.text = "کالایی در این ماه منقضی نمی‌شود"
        } else {
            val sb = StringBuilder()
            for (d in byDay.keys.sorted()) {
                sb.append(toFa("$d") + " " + MONTHS[jm - 1] + " " + toFa("$jy") + "\n")
                for (it in byDay[d]!!) {
                    sb.append("   • " + it.name + " (×" + toFa("${it.qty}") + ") [" + it.cat + "]\n")
                }
                sb.append("\n")
            }
            listTv.text = sb.toString().trim()
        }
    }
}

class SettingsActivity : AppCompatActivity() {

    private val times = mutableListOf<String>()
    private lateinit var timesBox: LinearLayout

    private val exportLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri != null) {
                try {
                    contentResolver.openOutputStream(uri)?.use {
                        it.write(exportBackup(this).toByteArray(Charsets.UTF_8))
                    }
                    Toast.makeText(this, "پشتیبان ذخیره شد", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "ذخیره‌ی پشتیبان ناموفق بود", Toast.LENGTH_LONG).show()
                }
            }
        }

    private val importLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                try {
                    val text = contentResolver.openInputStream(uri)?.use {
                        String(it.readBytes(), Charsets.UTF_8)
                    } ?: ""
                    val n = importBackup(this, text)
                    val msg = if (n < 0) "فایل پشتیبان معتبر نیست"
                    else toFa("$n") + " کالای جدید بازیابی شد"
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "خواندن فایل ناموفق بود", Toast.LENGTH_LONG).show()
                }
            }
        }

    private fun fmt(h: Int, m: Int): String = String.format(Locale.US, "%02d:%02d", h, m)

    private fun pickTime(initial: String?, onPick: (String) -> Unit) {
        val p = (initial ?: "09:00").split(":")
        val h = p[0].toIntOrNull() ?: 9
        val m = p.getOrNull(1)?.toIntOrNull() ?: 0
        TimePickerDialog(this, { _, hh, mm -> onPick(fmt(hh, mm)) }, h, m, true).show()
    }

    private fun renderTimes() {
        times.sort()
        timesBox.removeAllViews()
        for (i in times.indices) {
            val t = times[i]
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL

            val b = Button(this)
            b.text = "🔔  " + toFa(t)
            b.setOnClickListener {
                pickTime(t) { nt ->
                    if (nt != t && times.contains(nt)) {
                        Toast.makeText(this, "این ساعت قبلا اضافه شده", Toast.LENGTH_SHORT).show()
                    } else {
                        times[i] = nt
                        renderTimes()
                    }
                }
            }
            val del = ImageButton(this)
            del.setImageResource(android.R.drawable.ic_menu_delete)
            del.setBackgroundColor(Color.TRANSPARENT)
            del.setOnClickListener {
                if (times.size <= 1) {
                    Toast.makeText(this, "حداقل یک ساعت لازم است", Toast.LENGTH_SHORT).show()
                } else {
                    times.removeAt(i)
                    renderTimes()
                }
            }
            row.addView(b, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(del)
            timesBox.addView(row)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "تنظیمات"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        times.addAll(Prefs.times(this))

        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.layoutDirection = View.LAYOUT_DIRECTION_RTL
        box.setPadding(32, 32, 32, 32)

        fun header(text: String): TextView {
            val t = TextView(this)
            t.text = text
            t.textSize = 17f
            t.setTypeface(null, Typeface.BOLD)
            t.setPadding(0, 40, 0, 8)
            return t
        }

        box.addView(header("اسکن"))
        val contSw = Switch(this)
        contSw.text = "اسکن پشت‌سرهم: بعد از ذخیره‌ی هر کالا، دوربین دوباره باز شود"
        contSw.isChecked = Prefs.continuous(this)
        box.addView(contSw)

        box.addView(header("اعلان‌ها"))
        val enabledSw = Switch(this)
        enabledSw.text = "فعال بودن اعلان‌ها"
        enabledSw.isChecked = Prefs.enabled(this)
        box.addView(enabledSw)

        val daysLabel = TextView(this)
        daysLabel.text = "هشدار چند روز قبل از انقضا"
        daysLabel.setPadding(0, 24, 0, 0)
        box.addView(daysLabel)
        val daysEt = EditText(this)
        daysEt.inputType = InputType.TYPE_CLASS_NUMBER
        daysEt.setText("${Prefs.days(this)}")
        box.addView(daysEt)

        val expiredSw = Switch(this)
        expiredSw.text = "کالاهای منقضی‌شده هم در اعلان بیاید"
        expiredSw.isChecked = Prefs.expired(this)
        expiredSw.setPadding(0, 24, 0, 0)
        box.addView(expiredSw)

        box.addView(header("ساعت‌های اعلان در روز"))
        timesBox = LinearLayout(this)
        timesBox.orientation = LinearLayout.VERTICAL
        box.addView(timesBox)
        renderTimes()
        val addTime = Button(this)
        addTime.text = "➕  افزودن ساعت"
        addTime.setOnClickListener {
            pickTime(null) { nt ->
                if (times.contains(nt)) {
                    Toast.makeText(this, "این ساعت قبلا اضافه شده", Toast.LENGTH_SHORT).show()
                } else {
                    times.add(nt)
                    renderTimes()
                }
            }
        }
        box.addView(addTime)

        val saveBtn = Button(this)
        saveBtn.text = "ذخیره تنظیمات"
        saveBtn.setOnClickListener {
            var days = normDigits(daysEt.text.toString()).trim().toIntOrNull() ?: 3
            if (days < 0) days = 0
            if (days > 365) days = 365
            Prefs.save(this, enabledSw.isChecked, days, times, expiredSw.isChecked, contSw.isChecked)
            scheduleReminder(this, false)
            Toast.makeText(this, "ذخیره شد", Toast.LENGTH_SHORT).show()
        }
        box.addView(saveBtn)

        val testBtn = Button(this)
        testBtn.text = "ارسال اعلان آزمایشی"
        testBtn.setOnClickListener {
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
                Toast.makeText(this, "اجازه اعلان را بده و دوباره امتحان کن", Toast.LENGTH_LONG).show()
            } else {
                sendNotification(this, "این یک اعلان آزمایشی است")
            }
        }
        box.addView(testBtn)

        val note = TextView(this)
        note.text = "توجه: اندروید زمان اعلان را دقیق اجرا نمی‌کند و ممکن است چند دقیقه یا بیشتر جابه‌جا شود."
        note.setPadding(0, 16, 0, 0)
        box.addView(note)

        box.addView(header("پشتیبان‌گیری"))
        val expBtn = Button(this)
        expBtn.text = "ذخیره‌ی پشتیبان (خروجی فایل)"
        expBtn.setOnClickListener { exportLauncher.launch("rz-backup-" + LocalDate.now() + ".json") }
        box.addView(expBtn)
        val impBtn = Button(this)
        impBtn.text = "بازیابی از فایل پشتیبان"
        impBtn.setOnClickListener { importLauncher.launch(arrayOf("*/*")) }
        box.addView(impBtn)
        val bkNote = TextView(this)
        bkNote.text = "اطلاعات فقط روی همین گوشی ذخیره می‌شود. فایل پشتیبان را جای امنی (مثلا Saved Messages تلگرام یا درایو) نگه دار."
        bkNote.setPadding(0, 8, 0, 0)
        box.addView(bkNote)

        box.addView(header("بارکدها"))
        val bcBtn = Button(this)
        bcBtn.text = "مدیریت بارکدها و اسم‌ها"
        bcBtn.setOnClickListener { startActivity(Intent(this, BarcodesActivity::class.java)) }
        box.addView(bcBtn)

        val scroll = ScrollView(this)
        scroll.addView(box)
        setContentView(scroll)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

class BarcodesActivity : AppCompatActivity() {

    private lateinit var adapter: RegAdapter
    private var onScanned: ((String) -> Unit)? = null

    private val scanLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
            val code = r.data?.getStringExtra("code")
            val cb = onScanned
            onScanned = null
            if (r.resultCode == RESULT_OK && code != null && cb != null) cb(code)
        }

    private inner class RegAdapter : ArrayAdapter<Reg>(this@BarcodesActivity, 0, mutableListOf<Reg>()) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val reg = getItem(position)!!
            val row = LinearLayout(context)
            row.orientation = LinearLayout.HORIZONTAL
            row.layoutDirection = View.LAYOUT_DIRECTION_RTL
            row.gravity = Gravity.CENTER_VERTICAL
            row.setPadding(8, 16, 8, 16)
            val tv = TextView(context)
            tv.textSize = 16f
            tv.text = reg.name + "\n" + reg.barcode + "  ·  " + reg.cat
            val edit = ImageButton(context)
            edit.setImageResource(android.R.drawable.ic_menu_edit)
            edit.setBackgroundColor(Color.TRANSPARENT)
            edit.setOnClickListener { showEdit(reg) }
            row.addView(tv, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(edit)
            row.setOnClickListener { showEdit(reg) }
            return row
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "بارکدهای ثبت‌شده"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.layoutDirection = View.LAYOUT_DIRECTION_RTL
        root.setPadding(24, 24, 24, 24)

        val hint = TextView(this)
        hint.text = "اسم هر بارکد را اینجا ببین و ویرایش کن. دفعه بعد که اسکنش کنی، همین اسم پر می‌شود."
        hint.setPadding(0, 0, 0, 16)
        root.addView(hint)

        val addBtn = Button(this)
        addBtn.text = "➕  افزودن بارکد"
        addBtn.setOnClickListener { showEdit(null) }
        root.addView(addBtn)

        val list = ListView(this)
        adapter = RegAdapter()
        list.adapter = adapter
        val empty = TextView(this)
        empty.gravity = Gravity.CENTER
        empty.text = "هنوز بارکدی ثبت نشده"
        val frame = FrameLayout(this)
        frame.addView(
            list,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        frame.addView(
            empty,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER
            )
        )
        list.emptyView = empty
        root.addView(frame, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun refresh() {
        Store.syncReg(this, Store.load(this))
        adapter.clear()
        adapter.addAll(Store.loadReg(this).sortedBy { it.name })
        adapter.notifyDataSetChanged()
    }

    // خطا یا null
    private fun saveEdit(old: Reg?, bc: String, name: String, cat: String): String? {
        val regs = Store.loadReg(this)
        if (regs.any { it.barcode == bc && it.barcode != old?.barcode }) {
            return "این بارکد قبلا ثبت شده"
        }
        if (old == null) {
            regs.add(Reg(bc, name, cat))
        } else {
            val i = regs.indexOfFirst { it.barcode == old.barcode }
            if (i >= 0) regs[i] = Reg(bc, name, cat) else regs.add(Reg(bc, name, cat))
        }
        Store.saveReg(this, regs)
        if (old != null) {
            val its = Store.load(this)
            var changed = false
            for (k in its.indices) {
                if (its[k].barcode == old.barcode) {
                    its[k] = its[k].copy(barcode = bc, name = name)
                    changed = true
                }
            }
            if (changed) Store.save(this, its)
        }
        return null
    }

    private fun showEdit(old: Reg?) {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.layoutDirection = View.LAYOUT_DIRECTION_RTL
        box.setPadding(48, 24, 48, 0)

        val err = TextView(this)
        err.setTextColor(Color.RED)
        err.visibility = View.GONE
        val hideErr = Runnable { err.visibility = View.GONE }
        fun showErr(msg: String) {
            err.text = msg
            err.visibility = View.VISIBLE
            err.removeCallbacks(hideErr)
            err.postDelayed(hideErr, 3000)
        }
        box.addView(err)

        val bcEt = EditText(this)
        bcEt.hint = "بارکد"
        bcEt.inputType = InputType.TYPE_CLASS_TEXT
        bcEt.setText(old?.barcode ?: "")
        val scanBtn = Button(this)
        scanBtn.text = "اسکن"
        val bcRow = LinearLayout(this)
        bcRow.orientation = LinearLayout.HORIZONTAL
        bcRow.gravity = Gravity.CENTER_VERTICAL
        bcRow.addView(bcEt, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        bcRow.addView(scanBtn)
        scanBtn.setOnClickListener {
            onScanned = { code -> bcEt.setText(code) }
            scanLauncher.launch(Intent(this, ScanActivity::class.java))
        }

        val nameEt = EditText(this)
        nameEt.hint = "اسم کالا"
        nameEt.setText(old?.name ?: "")

        val catSp = Spinner(this)
        catSp.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, CATS)
        val ci = CATS.indexOf(old?.cat ?: "سایر")
        catSp.setSelection(if (ci >= 0) ci else CATS.size - 1)

        box.addView(bcRow)
        box.addView(nameEt)
        box.addView(TextView(this).apply { text = "دسته‌بندی پیش‌فرض" })
        box.addView(catSp)

        val scroll = ScrollView(this)
        scroll.addView(box)

        val dlg = AlertDialog.Builder(this)
            .setTitle(if (old == null) "بارکد جدید" else "ویرایش بارکد")
            .setView(scroll)
            .setPositiveButton("ذخیره", null)
            .setNegativeButton("لغو", null)
            .create()
        dlg.setOnShowListener {
            dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val bc = normDigits(bcEt.text.toString()).trim()
                val n = nameEt.text.toString().trim()
                if (bc.isEmpty()) {
                    showErr("بارکد را وارد کن")
                    return@setOnClickListener
                }
                if (n.isEmpty()) {
                    showErr("اسم کالا را وارد کن")
                    return@setOnClickListener
                }
                val e = saveEdit(old, bc, n, CATS[catSp.selectedItemPosition])
                if (e != null) {
                    showErr(e)
                    return@setOnClickListener
                }
                refresh()
                dlg.dismiss()
            }
        }
        dlg.show()
    }
}
