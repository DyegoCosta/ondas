package com.dyegocosta.ondas

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.RemoteViews

class SmallWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = Widgets.refresh(context)
}

class WideWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = Widgets.refresh(context)
}

/** Recebe os toques nos widgets: registra sem abrir o app, desfaz e volta ao estado inicial. */
class WidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Widgets.ACTION_RECORD -> Widgets.record(context)
            Widgets.ACTION_UNDO -> Widgets.undo(context)
            Widgets.ACTION_REVERT -> Widgets.refresh(context)
        }
    }
}

object Widgets {
    const val ACTION_RECORD = "com.dyegocosta.ondas.RECORD"
    const val ACTION_UNDO = "com.dyegocosta.ondas.UNDO"
    const val ACTION_REVERT = "com.dyegocosta.ondas.REVERT"

    private const val PREFS = "widgets"
    private const val KEY_ID = "confirm_id"
    private const val KEY_UNTIL = "confirm_until"
    private const val CONFIRM_MS = 60_000L

    fun record(context: Context) {
        val repo = OndasRepository.get(context)
        val now = System.currentTimeMillis()
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val id = repo.add(now)
        prefs.edit().putLong(KEY_ID, id).putLong(KEY_UNTIL, now + CONFIRM_MS).apply()
        vibrate(context)
        refresh(context)
        val alarm = context.getSystemService(AlarmManager::class.java)
        alarm.set(AlarmManager.RTC, now + CONFIRM_MS + 500, broadcast(context, ACTION_REVERT, 3))
    }

    fun undo(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val id = prefs.getLong(KEY_ID, -1)
        clearConfirmation(context)
        if (id > 0) OndasRepository.get(context).delete(id) else refresh(context)
    }

    fun clearConfirmation(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_ID).remove(KEY_UNTIL).apply()
        refresh(context)
    }

    fun refresh(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val smallIds = manager.getAppWidgetIds(ComponentName(context, SmallWidget::class.java))
        val wideIds = manager.getAppWidgetIds(ComponentName(context, WideWidget::class.java))
        if (smallIds.isEmpty() && wideIds.isEmpty()) return
        val confirmed = confirmedEntry(context)
        if (smallIds.isNotEmpty()) manager.updateAppWidget(smallIds, small(context, confirmed))
        if (wideIds.isNotEmpty()) manager.updateAppWidget(wideIds, wide(context, confirmed))
    }

    private fun confirmedEntry(context: Context): Entry? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (System.currentTimeMillis() >= prefs.getLong(KEY_UNTIL, 0)) return null
        return OndasRepository.get(context).find(prefs.getLong(KEY_ID, -1))
    }

    private fun small(context: Context, confirmed: Entry?): RemoteViews {
        if (confirmed == null) {
            return RemoteViews(context.packageName, R.layout.widget_small_ready).apply {
                setOnClickPendingIntent(R.id.root, broadcast(context, ACTION_RECORD, 1))
            }
        }
        return RemoteViews(context.packageName, R.layout.widget_small_done).apply {
            setTextViewText(R.id.time, Format.clock(confirmed.at))
            setOnClickPendingIntent(R.id.annotate, openNote(context, confirmed.id))
        }
    }

    private fun wide(context: Context, confirmed: Entry?): RemoteViews {
        if (confirmed == null) {
            val repo = OndasRepository.get(context)
            return RemoteViews(context.packageName, R.layout.widget_wide_ready).apply {
                setTextViewText(R.id.today, Format.todayLabel(repo.todayCount()))
                setTextViewText(R.id.last, Format.lastLabel(repo.entries.value.firstOrNull()?.at))
                setOnClickPendingIntent(R.id.root, openApp(context))
                setOnClickPendingIntent(R.id.register, broadcast(context, ACTION_RECORD, 1))
            }
        }
        return RemoteViews(context.packageName, R.layout.widget_wide_done).apply {
            setTextViewText(R.id.time, "Registrada às ${Format.clock(confirmed.at)}")
            setOnClickPendingIntent(R.id.annotate, openNote(context, confirmed.id))
            setOnClickPendingIntent(R.id.undo, broadcast(context, ACTION_UNDO, 2))
        }
    }

    private fun broadcast(context: Context, action: String, code: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context, code,
            Intent(context, WidgetActionReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun openApp(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context, 10,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun openNote(context: Context, id: Long): PendingIntent =
        PendingIntent.getActivity(
            context, 11,
            Intent(context, MainActivity::class.java)
                .setAction(MainActivity.ACTION_NOTE)
                .putExtra(MainActivity.EXTRA_ENTRY_ID, id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun vibrate(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            context.getSystemService(Vibrator::class.java)
        }
        vibrator?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}
