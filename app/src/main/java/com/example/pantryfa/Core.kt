package com.example.pantryfa

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.json.JSONArray
import org.json.JSONObject
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

val CATS = listOf(
    "یخچال", "فریزر", "لبنیات", "پروتئینی", "نوشیدنی", "حبوبات",
    "کنسرویجات", "شوینده‌ها", "اسنک‌ها", "دارو", "بهداشتی و آرایشی", "سایر"
)

val MONTHS = listOf(
    "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
    "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
)

// date = تاریخ میلادی به صورت ISO (مثلا 2026-11-14)
data class Item(
    val barcode: String,
    val name: String,
    val date: String,
    val qty: Int = 1,
    val cat: String = "سایر"
)

// بارکد ثبت‌شده با اسم و دسته‌ی پیش‌فرض
data class Reg(val barcode: String, val name: String, val cat: String)

object Store {
    private fun prefs(c: Context) = c.getSharedPreferences("pantry", Context.MODE_PRIVATE)

    fun load(c: Context): MutableList<Item> {
        val s = prefs(c).getString("items", "[]") ?: "[]"
        val a = JSONArray(s)
        val out = mutableListOf<Item>()
        for (i in 0 until a.length()) {
            val o = a.getJSONObject(i)
            val cat0 = o.optString("c", "سایر")
            val cat = if (cat0 in CATS) cat0 else "سایر"
            out.add(Item(o.getString("b"), o.getString("n"), o.getString("d"), o.optInt("q", 1), cat))
        }
        return out
    }

    fun save(c: Context, items: List<Item>) {
        val a = JSONArray()
        for (it in items) {
            a.put(
                JSONObject().put("b", it.barcode).put("n", it.name).put("d", it.date)
                    .put("q", it.qty).put("c", it.cat)
            )
        }
        prefs(c).edit().putString("items", a.toString()).apply()
        updateWidgets(c)
    }

    fun loadReg(c: Context): MutableList<Reg> {
        val s = prefs(c).getString("reg", "[]") ?: "[]"
        val a = JSONArray(s)
        val out = mutableListOf<Reg>()
        for (i in 0 until a.length()) {
            val o = a.getJSONObject(i)
            val cat0 = o.optString("c", "سایر")
            out.add(Reg(o.getString("b"), o.getString("n"), if (cat0 in CATS) cat0 else "سایر"))
        }
        return out
    }

    fun saveReg(c: Context, regs: List<Reg>) {
        val a = JSONArray()
        for (r in regs) {
            a.put(JSONObject().put("b", r.barcode).put("n", r.name).put("c", r.cat))
        }
        prefs(c).edit().putString("reg", a.toString()).apply()
    }

    fun upsertReg(c: Context, barcode: String, name: String, cat: String) {
        if (barcode.isEmpty()) return
        val l = loadReg(c)
        val i = l.indexOfFirst { it.barcode == barcode }
        if (i >= 0) l[i] = Reg(barcode, name, cat) else l.add(Reg(barcode, name, cat))
        saveReg(c, l)
    }

    // بارکدهای کالاهای موجود را که هنوز ثبت نشده‌اند به فهرست بارکدها اضافه می‌کند
    fun syncReg(c: Context, items: List<Item>) {
        val regs = loadReg(c)
        var changed = false
        for (item in items) {
            if (item.barcode.isNotEmpty() && regs.none { r -> r.barcode == item.barcode }) {
                regs.add(Reg(item.barcode, item.name, item.cat))
                changed = true
            }
        }
        if (changed) saveReg(c, regs)
    }
}

fun daysLeft(item: Item): Int =
    ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(item.date)).toInt()

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("settings", Context.MODE_PRIVATE)
    fun enabled(c: Context) = sp(c).getBoolean("enabled", true)
    fun days(c: Context) = sp(c).getInt("days", 3)
    fun expired(c: Context) = sp(c).getBoolean("expired", true)
    fun continuous(c: Context) = sp(c).getBoolean("continuous", true)
    fun group(c: Context) = sp(c).getBoolean("group", false)
    fun setGroup(c: Context, v: Boolean) = sp(c).edit().putBoolean("group", v).apply()
    fun theme(c: Context) = sp(c).getInt("theme", 0)
    fun setTheme(c: Context, v: Int) = sp(c).edit().putInt("theme", v).apply()

    // ساعت‌های اعلان به صورت HH:MM
    fun times(c: Context): MutableList<String> {
        val s = sp(c).getString("times", "09:00") ?: "09:00"
        val l = s.split(",").filter { it.isNotBlank() }.toMutableList()
        if (l.isEmpty()) l.add("09:00")
        return l
    }

    fun save(
        c: Context, enabled: Boolean, days: Int, times: List<String>,
        expired: Boolean, continuous: Boolean
    ) {
        sp(c).edit()
            .putBoolean("enabled", enabled)
            .putInt("days", days)
            .putString("times", times.joinToString(","))
            .putBoolean("expired", expired)
            .putBoolean("continuous", continuous)
            .apply()
    }
}

fun statusText(d: Int): String = when {
    d < 0 -> "منقضی شده (" + toFa("${-d}") + " روز پیش)"
    d == 0 -> "امروز منقضی می‌شود"
    else -> toFa("$d") + " روز مانده"
}

// کالاهای نزدیک انقضا (و در صورت فعال بودن گزینه، منقضی‌شده‌ها) به ترتیب تاریخ
fun expiringItems(c: Context): List<Item> {
    val days = Prefs.days(c)
    val withExpired = Prefs.expired(c)
    return Store.load(c).filter {
        val d = daysLeft(it)
        d <= days && (withExpired || d >= 0)
    }.sortedBy { it.date }
}

fun expiringCount(c: Context): Int = expiringItems(c).size

// متن کوتاه (سه کالای اول) و متن کامل (تا ۸ کالا) اعلان
fun expirySummary(list: List<Item>): Pair<String, String> {
    val names = list.take(3).joinToString("، ") { it.name }
    val rest = list.size - 3
    val short = toFa("${list.size}") + " کالا نزدیک انقضاست: " + names +
        (if (rest > 0) " و " + toFa("$rest") + " مورد دیگر" else "")
    val big = list.take(8).joinToString("\n") { "• " + it.name + " — " + statusText(daysLeft(it)) } +
        (if (list.size > 8) "\n…" else "")
    return Pair(short, big)
}

fun sendNotification(c: Context, text: String, big: String? = null) {
    val nm = c.getSystemService(NotificationManager::class.java)
    nm.createNotificationChannel(
        NotificationChannel("expiry", "انقضا", NotificationManager.IMPORTANCE_DEFAULT)
    )
    val open = Intent(c, MainActivity::class.java)
        .putExtra("soon", true)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    val pi = PendingIntent.getActivity(
        c, 0, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    val b = Notification.Builder(c, "expiry")
        .setSmallIcon(R.drawable.ic_notif)
        .setContentTitle("هشدار تاریخ انقضا")
        .setContentText(text)
        .setContentIntent(pi)
        .setAutoCancel(true)
    if (big != null) b.setStyle(Notification.BigTextStyle().bigText(big))
    nm.notify(1, b.build())
}

// برای هر ساعت تنظیم‌شده یک کار تکراری روزانه ثبت می‌شود.
// keep = true: اگر قبلا زمان‌بندی شده، دست نزن (موقع باز شدن اپ)
fun scheduleReminder(c: Context, keep: Boolean) {
    val wm = WorkManager.getInstance(c)
    wm.cancelUniqueWork("expiry")
    if (!keep || !Prefs.enabled(c)) wm.cancelAllWorkByTag("expiry-times")
    if (!Prefs.enabled(c)) return
    val now = LocalDateTime.now()
    for (t in Prefs.times(c)) {
        val parts = t.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: continue
        val m = parts.getOrNull(1)?.toIntOrNull() ?: continue
        var next = now.toLocalDate().atTime(h, m)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delay = Duration.between(now, next).toMillis()
        val req = PeriodicWorkRequest.Builder(ExpiryWorker::class.java, 1, TimeUnit.DAYS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag("expiry-times")
            .build()
        val policy = if (keep) ExistingPeriodicWorkPolicy.KEEP
        else ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
        wm.enqueueUniquePeriodicWork("expiry_" + t, policy, req)
    }
}

class ExpiryWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val c = applicationContext
        updateWidgets(c)
        if (Prefs.enabled(c)) {
            val list = expiringItems(c)
            if (list.isNotEmpty()) {
                val m = expirySummary(list)
                sendNotification(c, m.first, m.second)
            }
        }
        return Result.success()
    }
}

// ---------- پشتیبان‌گیری ----------

fun exportBackup(c: Context): String {
    val o = JSONObject()
    o.put("app", "Rz")
    o.put("version", 1)
    val a = JSONArray()
    for (x in Store.load(c)) {
        a.put(
            JSONObject().put("b", x.barcode).put("n", x.name).put("d", x.date)
                .put("q", x.qty).put("c", x.cat)
        )
    }
    o.put("items", a)
    val r = JSONArray()
    for (x in Store.loadReg(c)) {
        r.put(JSONObject().put("b", x.barcode).put("n", x.name).put("c", x.cat))
    }
    o.put("reg", r)
    return o.toString(2)
}

// کالاهای تکراری (بارکد + اسم + تاریخ یکسان) اضافه نمی‌شوند.
// خروجی: تعداد کالای جدید، یا -1 اگر فایل معتبر نبود
fun importBackup(c: Context, text: String): Int {
    return try {
        val o = JSONObject(text)
        val items = Store.load(c)
        var added = 0
        val a = o.optJSONArray("items") ?: JSONArray()
        for (i in 0 until a.length()) {
            val x = a.getJSONObject(i)
            val cat0 = x.optString("c", "سایر")
            val item = Item(
                x.getString("b"), x.getString("n"), x.getString("d"),
                x.optInt("q", 1), if (cat0 in CATS) cat0 else "سایر"
            )
            LocalDate.parse(item.date)
            if (items.none { it.barcode == item.barcode && it.name == item.name && it.date == item.date }) {
                items.add(item)
                added++
            }
        }
        Store.save(c, items)
        val regs = Store.loadReg(c)
        val r = o.optJSONArray("reg") ?: JSONArray()
        for (i in 0 until r.length()) {
            val x = r.getJSONObject(i)
            val bc = x.getString("b")
            val cat0 = x.optString("c", "سایر")
            if (regs.none { it.barcode == bc }) {
                regs.add(Reg(bc, x.getString("n"), if (cat0 in CATS) cat0 else "سایر"))
            }
        }
        Store.saveReg(c, regs)
        added
    } catch (e: Exception) {
        -1
    }
}
