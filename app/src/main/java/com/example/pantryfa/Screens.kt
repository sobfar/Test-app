package com.example.pantryfa

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.time.LocalDate
import java.util.Locale

val MONTHS = listOf(
    "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
    "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
)

fun monthLen(y: Int, m: Int): Int = when {
    m <= 6 -> 31
    m <= 11 -> 30
    Jalali.parse("$y/12/30") != null -> 30
    else -> 29
}

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

    private var hour = 9
    private var minute = 0

    private fun timeText(): String =
        "ساعت اعلان: " + toFa(String.format(Locale.US, "%02d:%02d", hour, minute))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "تنظیمات"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        hour = Prefs.hour(this)
        minute = Prefs.minute(this)

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(32, 32, 32, 32)
        }

        val enabledSw = Switch(this)
        enabledSw.text = "فعال بودن اعلان‌ها"
        enabledSw.isChecked = Prefs.enabled(this)
        box.addView(enabledSw)

        val daysLabel = TextView(this)
        daysLabel.text = "هشدار چند روز قبل از انقضا"
        daysLabel.setPadding(0, 32, 0, 0)
        box.addView(daysLabel)
        val daysEt = EditText(this)
        daysEt.inputType = InputType.TYPE_CLASS_NUMBER
        daysEt.setText("${Prefs.days(this)}")
        box.addView(daysEt)

        val expiredSw = Switch(this)
        expiredSw.text = "کالاهای منقضی‌شده هم در اعلان بیاید"
        expiredSw.isChecked = Prefs.expired(this)
        expiredSw.setPadding(0, 32, 0, 0)
        box.addView(expiredSw)

        val timeBtn = Button(this)
        timeBtn.text = timeText()
        timeBtn.setOnClickListener {
            TimePickerDialog(this, { _, h, m ->
                hour = h
                minute = m
                timeBtn.text = timeText()
            }, hour, minute, true).show()
        }
        box.addView(timeBtn)

        val saveBtn = Button(this)
        saveBtn.text = "ذخیره تنظیمات"
        saveBtn.setOnClickListener {
            var days = normDigits(daysEt.text.toString()).trim().toIntOrNull() ?: 3
            if (days < 0) days = 0
            if (days > 365) days = 365
            Prefs.save(this, enabledSw.isChecked, days, hour, minute, expiredSw.isChecked)
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
        note.setPadding(0, 32, 0, 0)
        box.addView(note)

        val scroll = ScrollView(this)
        scroll.addView(box)
        setContentView(scroll)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
