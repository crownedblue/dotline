package com.crownedblue.dotline

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.*
import android.graphics.Typeface

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("dotline", MODE_PRIVATE) }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun label(s: String, size: Float = 15f) = TextView(this).apply {
        text = s; textSize = size; setTextColor(Color.WHITE); setPadding(0, dp(8), 0, dp(8))
    }
    private fun section(s: String) = TextView(this).apply {
        text = s; textSize = 19f; setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD
        setPadding(0, dp(18), 0, dp(4))
    }
    private fun edit(hint: String, value: String) = EditText(this).apply {
        setSingleLine(); setHint(hint); setText(value); setTextColor(Color.WHITE); setHintTextColor(0xff999999.toInt())
        setPadding(dp(12), dp(8), dp(12), dp(8)); setBackgroundColor(0xff25252b.toInt())
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = 0xff101014.toInt(); window.navigationBarColor = 0xff101014.toInt()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(0xff101014.toInt()); setPadding(dp(20), dp(18), dp(20), dp(24)) }
        val scroll = ScrollView(this); scroll.addView(root)
        root.addView(TextView(this).apply { text="DOTLINE"; textSize=27f; typeface=Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE) })
        root.addView(label("A tiny notification indicator that stays visible over your apps.", 14f))
        root.addView(section("Permissions"))
        root.addView(button("Grant display-over-apps permission") {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        })
        root.addView(button("Enable notification access") { startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")) })
        root.addView(section("Floating bar"))
        val barColor = edit("Bar colour (e.g. #66000000)", prefs.getString("barColor", "#66000000")!!)
        root.addView(label("Bar colour / transparency")); root.addView(barColor)
        val dotSize = edit("Dot size in dp", prefs.getInt("dotSize", 8).toString())
        val barWidth = edit("Bar width in dp", prefs.getInt("barWidth", 180).toString())
        val barHeight = edit("Bar height in dp", prefs.getInt("barHeight", 30).toString())
        root.addView(label("Dot size")); root.addView(dotSize)
        root.addView(label("Bar width")); root.addView(barWidth)
        root.addView(label("Bar height")); root.addView(barHeight)
        val x = edit("Horizontal position (dp)", prefs.getInt("x", 120).toString())
        val y = edit("Vertical position (dp)", prefs.getInt("y", 4).toString())
        root.addView(label("Position from left")); root.addView(x)
        root.addView(label("Position from top")); root.addView(y)
        root.addView(section("App dot colour"))
        root.addView(label("Optional override for a specific app package."))
        val pkg = edit("Package name e.g. com.whatsapp", "")
        val colour = edit("Dot colour e.g. #25D366", "#25D366")
        root.addView(pkg); root.addView(colour)
        root.addView(button("Save app colour") {
            val p = pkg.text.toString().trim()
            val c = colour.text.toString().trim()
            if (p.isNotEmpty() && validColor(c)) {
                prefs.edit().putString("app_$p", c).apply()
                Toast.makeText(this, "Colour saved for $p", Toast.LENGTH_SHORT).show()
            } else Toast.makeText(this, "Enter a package name and valid #RRGGBB colour", Toast.LENGTH_LONG).show()
        })
        root.addView(section("Run Dotline"))
        root.addView(button("Save settings") {
            prefs.edit()
                .putString("barColor", barColor.text.toString().trim())
                .putInt("dotSize", dotSize.text.toString().toIntOrNull()?.coerceIn(3, 28) ?: 8)
                .putInt("barWidth", barWidth.text.toString().toIntOrNull()?.coerceIn(40, 500) ?: 180)
                .putInt("barHeight", barHeight.text.toString().toIntOrNull()?.coerceIn(18, 100) ?: 30)
                .putInt("x", x.text.toString().toIntOrNull()?.coerceAtLeast(0) ?: 120)
                .putInt("y", y.text.toString().toIntOrNull()?.coerceAtLeast(0) ?: 4).apply()
            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
        })
        root.addView(button("Start floating dots") {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Grant display-over-apps permission first", Toast.LENGTH_LONG).show()
            } else {
                val i = Intent(this, DotOverlayService::class.java)
                if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(i) else startService(i)
            }
        })
        root.addView(button("Stop floating dots") { stopService(Intent(this, DotOverlayService::class.java)) })
        root.addView(label("Enable notification access and overlay permission, then start Dotline. Drag the bar to reposition it. Secure screens and protected video may hide overlays.", 13f))
        setContentView(scroll)
    }
    private fun validColor(s: String) = try { Color.parseColor(s); true } catch (_: Exception) { false }
    private fun button(text: String, action: () -> Unit) = Button(this).apply {
        this.text = text; isAllCaps = false; setTextColor(Color.WHITE); setBackgroundColor(0xff34343d.toInt())
        setOnClickListener { action() }
    }
}
