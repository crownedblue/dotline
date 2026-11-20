package com.crownedblue.dotline

import android.app.*
import android.content.*
import android.graphics.*
import android.os.Build
import android.os.IBinder
import android.view.*
import kotlin.math.max

class DotOverlayService : Service() {
    companion object { const val ACTION_REFRESH = "com.crownedblue.dotline.REFRESH"; private const val CHANNEL = "dotline_overlay" }
    private lateinit var wm: WindowManager
    private var panel: DotView? = null
    private lateinit var params: WindowManager.LayoutParams
    private val refresh = object : BroadcastReceiver() { override fun onReceive(c: Context?, i: Intent?) { panel?.invalidate() } }
    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        createChannel()
        val notification = Notification.Builder(this, CHANNEL).setContentTitle("Dotline is running").setContentText("Notification dots are active").setSmallIcon(android.R.drawable.presence_online).setOngoing(true).build()
        startForeground(7, notification)
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(refresh, IntentFilter(ACTION_REFRESH), RECEIVER_NOT_EXPORTED)
        else @Suppress("DEPRECATION") registerReceiver(refresh, IntentFilter(ACTION_REFRESH))
        val p = getSharedPreferences("dotline", MODE_PRIVATE)
        val w = (p.getInt("barWidth", 180) * resources.displayMetrics.density).toInt()
        val h = (p.getInt("barHeight", 30) * resources.displayMetrics.density).toInt()
        params = WindowManager.LayoutParams(w, h, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (p.getInt("x", 120) * resources.displayMetrics.density).toInt()
            y = (p.getInt("y", 4) * resources.displayMetrics.density).toInt()
        }
        panel = DotView(this)
        panel!!.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> true
                MotionEvent.ACTION_MOVE -> { params.x += e.x.toInt() - (panel!!.width / 2); params.y += e.y.toInt() - (panel!!.height / 2); wm.updateViewLayout(panel, params); true }
                else -> false
            }
        }
        wm.addView(panel, params)
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY
    override fun onDestroy() {
        try { unregisterReceiver(refresh) } catch (_: Exception) {}
        panel?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        panel = null
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(CHANNEL, "Dotline overlay", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }
    inner class DotView(c: Context) : View(c) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val prefs = getSharedPreferences("dotline", MODE_PRIVATE)
        private val density = resources.displayMetrics.density
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val bg = try { Color.parseColor(prefs.getString("barColor", "#66000000")) } catch (_: Exception) { 0x66000000 }
            paint.color = bg
            canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), height / 2f, height / 2f, paint)
            val apps = prefs.getStringSet("activePackages", emptySet())?.toList()?.sorted() ?: emptyList()
            val dot = prefs.getInt("dotSize", 8) * density
            val gap = max(dot * 1.5f, 8f * density)
            val total = apps.size * dot + max(0, apps.size - 1) * gap
            var cx = (width - total) / 2f + dot / 2f
            val cy = height / 2f
            apps.take(30).forEach { app ->
                val override = prefs.getString("app_$app", null)
                paint.color = try { if (override != null) Color.parseColor(override) else palette(app) } catch (_: Exception) { palette(app) }
                canvas.drawCircle(cx, cy, dot / 2f, paint)
                cx += dot + gap
            }
        }
        private fun palette(s: String): Int {
            val colors = intArrayOf(0xff7c9cff.toInt(),0xffff7e9d.toInt(),0xffffc857.toInt(),0xff56d6b2.toInt(),0xffc69cff.toInt(),0xffff9466.toInt(),0xff6ecbff.toInt())
            return colors[(s.hashCode() and 0x7fffffff) % colors.size]
        }
    }
}
