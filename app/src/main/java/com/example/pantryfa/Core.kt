package com.example.pantryfa

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.json.JSONArray
import org.json.JSONObject
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import java.time.temporal.ChronoUnit

// date = تاریخ میلادی به صورت ISO (مثلا 2026-11-14)
data class Item(val barcode: String, val name: String, val date: String, val qty: Int = 1, val cat: String = "سایر")

object Store {
    private fun prefs(c: Context) = c.getSharedPreferences("pantry", Context.MODE_PRIVATE)

    fun load(c: Context): MutableList<Item> {
        val s = prefs(c).getString("items", "[]") ?: "[]"
        val a = JSONArray(s)
        val out = mutableListOf<Item>()
        for (i in 0 until a.length()) {
            val o = a.getJSONObject(i)
            out.add(Item(o.getString("b"), o.getString("n"), o.getString("d"), o.optInt("q", 1), o.optString("c", "سایر")))
        }
        return out
    }

    fun save(c: Context, items: List<Item>) {
        val a = JSONArray()
        for (it in items) {
            a.put(JSONObject().put("b", it.barcode).put("n", it.name).put("d", it.date).put("q", it.qty).put("c", it.cat))
        }
        prefs(c).edit().putString("items", a.toString()).apply()
    }
}

fun daysLeft(item: Item): Int =
    ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(item.date)).toInt()

fun normDigits(s: String): String {
    val sb = StringBuilder()
    for (ch in s) {
        when (ch) {
            in '\u06F0'..'\u06F9' -> sb.append('0' + (ch - '\u06F0'))
            in '\u0660'..'\u0669' -> sb.append('0' + (ch - '\u0660'))
            else -> sb.append(ch)
        }
    }
    return sb.toString()
}

fun toFa(s: String): String {
    val sb = StringBuilder()
    for (ch in s) {
        if (ch in '0'..'9') sb.append('\u06F0' + (ch - '0')) else sb.append(ch)
    }
    return sb.toString()
}

object Jalali {
    fun g2j(gy: Int, gm: Int, gd: Int): IntArray {
        val gdm = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        val gy2 = if (gm > 2) gy + 1 else gy
        var days = 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) +
            ((gy2 + 399) / 400) + gd + gdm[gm - 1]
        var jy = -1595 + (33 * (days / 12053))
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val jm: Int
        val jd: Int
        if (days < 186) {
            jm = 1 + days / 31
            jd = 1 + days % 31
        } else {
            jm = 7 + (days - 186) / 30
            jd = 1 + (days - 186) % 30
        }
        return intArrayOf(jy, jm, jd)
    }

    fun j2g(jy: Int, jm: Int, jd: Int): IntArray {
        val jy2 = jy + 1595
        var days = -355668 + (365 * jy2) + ((jy2 / 33) * 8) + (((jy2 % 33) + 3) / 4) + jd +
            (if (jm < 7) (jm - 1) * 31 else ((jm - 7) * 30) + 186)
        var gy = 400 * (days / 146097)
        days %= 146097
        if (days > 36524) {
            days -= 1
            gy += 100 * (days / 36524)
            days %= 36524
            if (days >= 365) days += 1
        }
        gy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            gy += (days - 1) / 365
            days = (days - 1) % 365
        }
        var gd = days + 1
        val leap = (gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0)
        val sal = intArrayOf(0, 31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var gm = 0
        while (gm < 13 && gd > sal[gm]) {
            gd -= sal[gm]
            gm++
        }
        return intArrayOf(gy, gm, gd)
    }

    fun format(d: LocalDate): String {
        val j = g2j(d.year, d.monthValue, d.dayOfMonth)
        return toFa(String.format("%04d/%02d/%02d", j[0], j[1], j[2]))
    }

    // ورودی شمسی مثل 1405/08/23 (ارقام فارسی یا انگلیسی) -> تاریخ میلادی یا null
    fun parse(text: String): LocalDate? {
        val sb = StringBuilder()
        for (ch in text.trim()) {
            when (ch) {
                in '\u06F0'..'\u06F9' -> sb.append('0' + (ch - '\u06F0'))
                in '\u0660'..'\u0669' -> sb.append('0' + (ch - '\u0660'))
                else -> sb.append(ch)
            }
        }
        val parts = sb.toString().split('/', '-', '.', ' ').filter { it.isNotEmpty() }
        if (parts.size != 3) return null
        var y = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        val d = parts[2].toIntOrNull() ?: return null
        if (y < 100) y += 1400
        if (m !in 1..12 || d !in 1..31 || (m > 6 && d > 30)) return null
        val g = j2g(y, m, d)
        val date = try {
            LocalDate.of(g[0], g[1], g[2])
        } catch (e: Exception) {
            return null
        }
        val back = g2j(g[0], g[1], g[2])
        if (back[0] != y || back[1] != m || back[2] != d) return null
        return date
    }
}

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("settings", Context.MODE_PRIVATE)
    fun enabled(c: Context) = sp(c).getBoolean("enabled", true)
    fun days(c: Context) = sp(c).getInt("days", 3)
    fun hour(c: Context) = sp(c).getInt("hour", 9)
    fun minute(c: Context) = sp(c).getInt("minute", 0)
    fun expired(c: Context) = sp(c).getBoolean("expired", true)
    fun save(c: Context, enabled: Boolean, days: Int, hour: Int, minute: Int, expired: Boolean) {
        sp(c).edit()
            .putBoolean("enabled", enabled)
            .putInt("days", days)
            .putInt("hour", hour)
            .putInt("minute", minute)
            .putBoolean("expired", expired)
            .apply()
    }
}

fun expiringCount(c: Context): Int {
    val days = Prefs.days(c)
    val withExpired = Prefs.expired(c)
    return Store.load(c).count {
        val d = daysLeft(it)
        d <= days && (withExpired || d >= 0)
    }
}

fun expiryMessage(c: Context, count: Int): String {
    val days = Prefs.days(c)
    val tail = if (Prefs.expired(c)) " یا شده‌اند" else ""
    return toFa("$count") + " کالا تا " + toFa("$days") + " روز دیگر منقضی می‌شوند" + tail
}

fun sendNotification(c: Context, text: String) {
    val nm = c.getSystemService(NotificationManager::class.java)
    nm.createNotificationChannel(
        NotificationChannel("expiry", "انقضا", NotificationManager.IMPORTANCE_DEFAULT)
    )
    val n = Notification.Builder(c, "expiry")
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("هشدار تاریخ انقضا")
        .setContentText(text)
        .setAutoCancel(true)
        .build()
    nm.notify(1, n)
}

// keep = true: اگر قبلا زمان‌بندی شده، دست نزن (موقع باز شدن اپ)
fun scheduleReminder(c: Context, keep: Boolean) {
    val wm = WorkManager.getInstance(c)
    if (!Prefs.enabled(c)) {
        wm.cancelUniqueWork("expiry")
        return
    }
    val now = LocalDateTime.now()
    var next = now.toLocalDate().atTime(Prefs.hour(c), Prefs.minute(c))
    if (!next.isAfter(now)) next = next.plusDays(1)
    val delay = Duration.between(now, next).toMillis()
    val req = PeriodicWorkRequest.Builder(ExpiryWorker::class.java, 1, TimeUnit.DAYS)
        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
        .build()
    val policy = if (keep) ExistingPeriodicWorkPolicy.KEEP else ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
    wm.enqueueUniquePeriodicWork("expiry", policy, req)
}

class ExpiryWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val c = applicationContext
        if (Prefs.enabled(c)) {
            val n = expiringCount(c)
            if (n > 0) sendNotification(c, expiryMessage(c, n))
        }
        return Result.success()
    }
}
