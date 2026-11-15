package com.example.pantryfa

import android.app.Application
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import androidx.appcompat.app.AppCompatDelegate

// 0 = مثل سیستم، 1 = روشن، 2 = تیره
fun applyTheme(c: Context) {
    val mode = when (Prefs.theme(c)) {
        1 -> AppCompatDelegate.MODE_NIGHT_NO
        2 -> AppCompatDelegate.MODE_NIGHT_YES
        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }
    AppCompatDelegate.setDefaultNightMode(mode)
}

class RzApp : Application() {
    override fun onCreate() {
        super.onCreate()
        applyTheme(this)
    }
}

// ---------- ویجت صفحه‌ی اصلی ----------

class RzWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val v = buildWidgetViews(context)
        for (id in appWidgetIds) appWidgetManager.updateAppWidget(id, v)
    }
}

fun buildWidgetViews(c: Context): RemoteViews {
    val v = RemoteViews(c.packageName, R.layout.widget)
    val lineIds = intArrayOf(R.id.w_l1, R.id.w_l2, R.id.w_l3, R.id.w_l4, R.id.w_l5)
    val all = Store.load(c).sortedBy { it.date }
    val warn = Prefs.days(c)
    val soon = all.filter { daysLeft(it) <= warn }

    for (id in lineIds) v.setViewVisibility(id, View.GONE)
    v.setViewVisibility(R.id.w_more, View.GONE)

    if (all.isEmpty()) {
        v.setTextViewText(lineIds[0], "هنوز کالایی ثبت نشده")
        v.setTextColor(lineIds[0], Color.parseColor("#757575"))
        v.setViewVisibility(lineIds[0], View.VISIBLE)
    } else if (soon.isEmpty()) {
        v.setTextViewText(lineIds[0], "✓ هیچ کالایی نزدیک انقضا نیست")
        v.setTextColor(lineIds[0], Color.parseColor("#2E7D32"))
        v.setViewVisibility(lineIds[0], View.VISIBLE)
    } else {
        for (i in 0 until minOf(lineIds.size, soon.size)) {
            val item = soon[i]
            val d = daysLeft(item)
            val qty = if (item.qty > 1) " ×" + toFa("${item.qty}") else ""
            v.setTextViewText(lineIds[i], "• " + item.name + qty + " — " + statusText(d))
            v.setTextColor(
                lineIds[i],
                Color.parseColor(if (d <= 0) "#C62828" else "#EF6C00")
            )
            v.setViewVisibility(lineIds[i], View.VISIBLE)
        }
        val more = soon.size - lineIds.size
        if (more > 0) {
            v.setTextViewText(R.id.w_more, "و " + toFa("$more") + " مورد دیگر…")
            v.setViewVisibility(R.id.w_more, View.VISIBLE)
        }
    }

    val open = Intent(c, MainActivity::class.java)
        .putExtra("soon", true)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    val pi = PendingIntent.getActivity(
        c, 1, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    v.setOnClickPendingIntent(R.id.widget_root, pi)
    return v
}

fun updateWidgets(c: Context) {
    val mgr = AppWidgetManager.getInstance(c)
    val ids = mgr.getAppWidgetIds(ComponentName(c, RzWidget::class.java))
    if (ids.isNotEmpty()) {
        val v = buildWidgetViews(c)
        for (id in ids) mgr.updateAppWidget(id, v)
    }
}
