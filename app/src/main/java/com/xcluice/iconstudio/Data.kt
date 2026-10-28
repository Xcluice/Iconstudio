package com.xcluice.iconstudio

import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.app.PendingIntent
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

fun loadApps(ctx: Context): List<AppEntry> {
    val pm = ctx.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(intent, 0)
        .filter { it.activityInfo.packageName != ctx.packageName }
        .map {
            val ai = it.activityInfo
            AppEntry(
                pkg = ai.packageName,
                label = it.loadLabel(pm).toString(),
                component = ComponentName(ai.packageName, ai.name),
                icon = it.loadIcon(pm)
            )
        }
        .distinctBy { it.id }
        .sortedBy { it.label.lowercase() }
}

object Pinner {
    const val ACTION = "com.xcluice.iconstudio.PIN_DONE"

    fun pin(ctx: Context, app: AppEntry, bmp: Bitmap): Boolean {
        val sm = ctx.getSystemService(ShortcutManager::class.java) ?: return false
        if (!sm.isRequestPinShortcutSupported) return false
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(app.component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        val info = ShortcutInfo.Builder(ctx, "is_${app.id}_${System.currentTimeMillis()}")
            .setShortLabel(app.label)
            .setLongLabel(app.label)
            .setIcon(Icon.createWithBitmap(bmp))
            .setIntent(intent)
            .build()
        val cb = PendingIntent.getBroadcast(
            ctx, 7,
            Intent(ACTION).setPackage(ctx.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        ).intentSender
        return sm.requestPinShortcut(info, cb)
    }

    fun export(ctx: Context, bmp: Bitmap, name: String): Boolean {
        if (Build.VERSION.SDK_INT < 29) return false
        val cv = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$name.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/IconStudio")
        }
        val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv) ?: return false
        ctx.contentResolver.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return true
    }
}

object PinQueue {
    var items by mutableStateOf<List<AppEntry>>(emptyList())
    var index by mutableIntStateOf(0)
    var foreground: Boolean = true
    var pendingPin: Boolean = false
    private var style: IconStyle = IconStyle.ONE_UI
    private var tweak: Tweak = Tweak()

    val active: Boolean get() = items.isNotEmpty() && index < items.size

    fun start(ctx: Context, list: List<AppEntry>, st: IconStyle, tw: Tweak) {
        items = list
        index = 0
        style = st
        tweak = tw
        pendingPin = false
        pinCurrent(ctx)
    }

    fun pinCurrent(ctx: Context) {
        val app = items.getOrNull(index)
        if (app == null) {
            finish(ctx)
            return
        }
        val bmp = IconRenderer.render(app, style, tweak, 192)
        if (!Pinner.pin(ctx, app, bmp)) {
            Toast.makeText(ctx, "This launcher can't pin shortcuts. Try Export instead.", Toast.LENGTH_LONG).show()
            stop()
        }
    }

    fun onPinned(ctx: Context) {
        items.getOrNull(index)?.let { History.add(ctx, it.label, style.title) }
        advance(ctx)
    }

    fun skip(ctx: Context) = advance(ctx)

    private fun advance(ctx: Context) {
        index++
        if (index >= items.size) {
            finish(ctx)
        } else if (foreground) {
            pinCurrent(ctx)
        } else {
            pendingPin = true
        }
    }

    fun stop() {
        items = emptyList()
        index = 0
        pendingPin = false
    }

    private fun finish(ctx: Context) {
        Toast.makeText(ctx, "All set. Check your home screen.", Toast.LENGTH_LONG).show()
        stop()
    }
}

data class Hist(val label: String, val style: String, val time: Long)

object History {
    val items = mutableStateListOf<Hist>()
    private const val SEP = "\u0001"

    fun load(ctx: Context) {
        if (items.isNotEmpty()) return
        val raw = ctx.getSharedPreferences("history", Context.MODE_PRIVATE).getString("h", "") ?: ""
        raw.split("\n").filter { it.isNotBlank() }.forEach {
            val p = it.split(SEP)
            if (p.size == 3) items.add(Hist(p[0], p[1], p[2].toLongOrNull() ?: 0L))
        }
    }

    fun add(ctx: Context, label: String, style: String) {
        items.add(0, Hist(label, style, System.currentTimeMillis()))
        while (items.size > 60) items.removeAt(items.size - 1)
        save(ctx)
    }

    fun clear(ctx: Context) {
        items.clear()
        save(ctx)
    }

    private fun save(ctx: Context) {
        val raw = items.joinToString("\n") { "${it.label}$SEP${it.style}$SEP${it.time}" }
        ctx.getSharedPreferences("history", Context.MODE_PRIVATE).edit().putString("h", raw).apply()
    }
}
