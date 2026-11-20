package com.crownedblue.dotline

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.LinearGradient
import android.graphics.Shader
import android.view.View
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.app.AlertDialog
import android.widget.*

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("dotline", MODE_PRIVATE) }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun label(s: String) = TextView(this).apply { text=s; textSize=15f; setTextColor(Color.WHITE); setPadding(0,dp(8),0,dp(8)) }
    private fun edit(h: String, v: String) = EditText(this).apply { hint=h; setText(v); setSingleLine(); setTextColor(-1); setHintTextColor(0xff999999.toInt()); setBackgroundColor(0xff25252b.toInt()); setPadding(dp(10),dp(6),dp(10),dp(6)) }
    private fun button(t: String, f: () -> Unit) = Button(this).apply { text=t; isAllCaps=false; setOnClickListener { f() } }

    private data class AppRow(val name: EditText, val color: EditText, val container: LinearLayout)

    private val rows = mutableListOf<AppRow>()

    private fun addAppRow(holder: LinearLayout, appName: String = "", hex: String = "#7C9CFF") {
        val wrap=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(0,dp(5),0,dp(8)) }
        val name=edit("App name (e.g. WhatsApp)",appName)
        val line=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=android.view.Gravity.CENTER_VERTICAL }
        val color=edit("#RRGGBB",hex).apply { layoutParams=LinearLayout.LayoutParams(0,dp(48),1f) }
        val pick=button("Pick custom colour") { showColorPicker(color) }
        val remove=button("Remove") { holder.removeView(wrap); rows.removeAll { it.container===wrap } }
        line.addView(color); line.addView(pick); line.addView(remove)
        wrap.addView(name); wrap.addView(line); holder.addView(wrap)
        rows.add(AppRow(name,color,wrap))
    }


    private fun showColorPicker(target: EditText) {
        val initial = try { Color.parseColor(target.text.toString().trim()) } catch (_: Exception) { 0xff7c9cff.toInt() }
        val hsv = FloatArray(3)
        Color.colorToHSV(initial, hsv)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(8))
        }
        val preview = View(this).apply {
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(initial); cornerRadius = dp(10).toFloat()
                setStroke(dp(1), 0xff777777.toInt())
            }
            layoutParams = LinearLayout.LayoutParams(-1, dp(42)).apply { bottomMargin = dp(12) }
        }
        content.addView(preview)
        val picker = SaturationValuePicker(this, hsv[0], hsv[1], hsv[2]) { s, v ->
            hsv[1] = s; hsv[2] = v
            val selected = Color.HSVToColor(hsv)
            preview.background = android.graphics.drawable.GradientDrawable().apply {
                setColor(selected); cornerRadius = dp(10).toFloat()
                setStroke(dp(1), 0xff777777.toInt())
            }
            hex.setText(String.format("#%06X", 0xFFFFFF and selected))
        }
        content.addView(picker, LinearLayout.LayoutParams(-1, dp(190)))
        content.addView(label("Hue"))
        val hue = SeekBar(this).apply { max = 360; progress = hsv[0].toInt() }
        content.addView(hue)
        val hex = edit("Hex colour", String.format("#%06X", 0xFFFFFF and initial))
        hex.setSingleLine()
        content.addView(hex)
        hue.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, value: Int, fromUser: Boolean) {
                if (!fromUser) return
                hsv[0] = value.toFloat()
                picker.setHue(hsv[0])
                val selected = Color.HSVToColor(hsv)
                preview.background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(selected); cornerRadius = dp(10).toFloat()
                    setStroke(dp(1), 0xff777777.toInt())
                }
                hex.setText(String.format("#%06X", 0xFFFFFF and selected))
            }
            override fun onStartTrackingTouch(bar: SeekBar?) {}
            override fun onStopTrackingTouch(bar: SeekBar?) {}
        })
        hex.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) try {
                val parsed = Color.parseColor(hex.text.toString().trim())
                Color.colorToHSV(parsed, hsv)
                hue.progress = hsv[0].toInt()
                picker.setSelection(hsv[1], hsv[2])
                preview.background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(parsed); cornerRadius = dp(10).toFloat()
                    setStroke(dp(1), 0xff777777.toInt())
                }
            } catch (_: Exception) { }
        }
        AlertDialog.Builder(this).setTitle("Custom dot colour")
            .setView(content)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Use colour") { _, _ ->
                try { Color.parseColor(hex.text.toString().trim()); target.setText(hex.text.toString().trim()) }
                catch (_: Exception) { Toast.makeText(this, "Invalid hex colour", Toast.LENGTH_SHORT).show() }
            }.show()
    }

    private inner class SaturationValuePicker(
        context: android.content.Context,
        private var hue: Float,
        private var saturation: Float,
        private var value: Float,
        private val changed: (Float, Float) -> Unit
    ) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val white = Paint(Paint.ANTI_ALIAS_FLAG)
        private val black = Paint(Paint.ANTI_ALIAS_FLAG)
        private val selector = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = dp(2).toFloat(); color = Color.WHITE
        }
        private val bounds = RectF()
        fun setHue(h: Float) { hue = h; invalidate() }
        fun setSelection(s: Float, v: Float) { saturation = s; value = v; invalidate() }
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            bounds.set(0f, 0f, width.toFloat(), height.toFloat())
            paint.shader = null; paint.color = Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
            canvas.drawRoundRect(bounds, dp(8).toFloat(), dp(8).toFloat(), paint)
            white.shader = LinearGradient(0f, 0f, width.toFloat(), 0f, Color.WHITE, 0x00FFFFFF, Shader.TileMode.CLAMP)
            canvas.drawRoundRect(bounds, dp(8).toFloat(), dp(8).toFloat(), white)
            black.shader = LinearGradient(0f, 0f, 0f, height.toFloat(), 0x00000000, Color.BLACK, Shader.TileMode.CLAMP)
            canvas.drawRoundRect(bounds, dp(8).toFloat(), dp(8).toFloat(), black)
            white.shader = null; black.shader = null
            val x = saturation * width
            val y = (1f - value) * height
            canvas.drawCircle(x, y, dp(9).toFloat(), selector)
            selector.color = Color.BLACK
            canvas.drawCircle(x, y, dp(11).toFloat(), selector)
            selector.color = Color.WHITE
        }
        override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
            if (event.action == android.view.MotionEvent.ACTION_DOWN || event.action == android.view.MotionEvent.ACTION_MOVE || event.action == android.view.MotionEvent.ACTION_UP) {
                saturation = (event.x / width.coerceAtLeast(1)).coerceIn(0f, 1f)
                value = (1f - event.y / height.coerceAtLeast(1)).coerceIn(0f, 1f)
                changed(saturation, value); invalidate()
                return true
            }
            return true
        }
    }
\n    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor=0xff101014.toInt(); window.navigationBarColor=0xff101014.toInt()
        val root=LinearLayout(this).apply { orientation=1; setPadding(dp(20),dp(18),dp(20),dp(24)); setBackgroundColor(0xff101014.toInt()) }
        val scroll=ScrollView(this); scroll.addView(root)
        root.addView(label("DOTLINE — Notification dots"))
        root.addView(button("Grant display-over-apps permission") { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:"+packageName))) })
        root.addView(button("Enable notification access") { startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")) })
        root.addView(label("Apps and their dot colours"))
        root.addView(label("Type the installed app's name. Add only the apps you want dots for."))
        val appRows=LinearLayout(this).apply { orientation=1 }
        val saved=prefs.getStringSet("selectedPackages",emptySet())?:emptySet()
        val pm=packageManager
        val oldRows=saved.mapNotNull { pkg ->
            val info=try { pm.getApplicationInfo(pkg,0) } catch(_:Exception){ null }
            info?.let { (pm.getApplicationLabel(it).toString()) to (prefs.getString("appColor_"+pkg,"#7C9CFF")?: "#7C9CFF") }
        }
        oldRows.forEach { addAppRow(appRows,it.first,it.second) }
        if(rows.isEmpty()) addAppRow(appRows)
        root.addView(appRows)
        root.addView(button("+ Add app") { addAppRow(appRows) })

        root.addView(label("Bar colour (#RRGGBB or #AARRGGBB)"))
        val color=edit("e.g. #00000000",prefs.getString("barColor","#00000000")!!); root.addView(color)
        fun addSeek(title:String,key:String,def:Int) {
            val l=label(title+": "+prefs.getInt(key,def)+"%"); root.addView(l)
            val s=SeekBar(this).apply { max=100; progress=prefs.getInt(key,def) }; root.addView(s)
            s.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(b:SeekBar?,v:Int,u:Boolean) { l.text=title+": "+v+"%" }
                override fun onStartTrackingTouch(b:SeekBar?) {}
                override fun onStopTrackingTouch(b:SeekBar?) { prefs.edit().putInt(key,s.progress).apply() }
            })
        }
        addSeek("Bar opacity","barOpacity",0); addSeek("Dot opacity","dotOpacity",100)
        root.addView(label("Dot effect"))
        val effect=Spinner(this).apply { adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,listOf("Clean","Glow","Glass")); setSelection(listOf("clean","glow","glass").indexOf(prefs.getString("effect","clean")).coerceAtLeast(0)) }; root.addView(effect)
        val size=edit("Dot size dp",prefs.getInt("dotSize",8).toString()); root.addView(label("Dot size")); root.addView(size)
        val width=edit("Bar width dp",prefs.getInt("barWidth",180).toString()); root.addView(label("Bar width")); root.addView(width)
        val height=edit("Bar height dp",prefs.getInt("barHeight",30).toString()); root.addView(label("Bar height")); root.addView(height)
        val maxDots=edit("Maximum visible app dots",prefs.getInt("maxDots",8).toString()); root.addView(label("Maximum dots")); root.addView(maxDots)

        root.addView(label("Position lock — separate saved position for each orientation"))
        root.addView(label("Drag the bar in portrait and landscape once. Dotline remembers both positions independently."))

        root.addView(button("Save settings") {
            val packages=mutableSetOf<String>()
            val editor=prefs.edit()
            rows.forEach { row ->
                val entered=row.name.text.toString().trim()
                if(entered.isNotEmpty()) {
                    val match=try {
                        pm.getInstalledApplications(0).firstOrNull { pm.getApplicationLabel(it).toString().equals(entered,true) || it.packageName.equals(entered,true) }
                    } catch(_:Exception){ null }
                    if(match!=null) {
                        val hex=row.color.text.toString().trim()
                        try { Color.parseColor(hex); packages.add(match.packageName); editor.putString("appColor_"+match.packageName,hex) }
                        catch(_:Exception) { Toast.makeText(this,"Invalid colour for $entered: use #RRGGBB",Toast.LENGTH_LONG).show() }
                    } else Toast.makeText(this,"App not found: $entered",Toast.LENGTH_LONG).show()
                }
            }
            editor.putStringSet("selectedPackages",packages).putBoolean("selectionConfigured",true)
                .putString("barColor",color.text.toString().trim()).putString("effect",listOf("clean","glow","glass")[effect.selectedItemPosition])
                .putInt("dotSize",size.text.toString().toIntOrNull()?.coerceIn(3,28)?:8)
                .putInt("barWidth",width.text.toString().toIntOrNull()?.coerceIn(40,600)?:180)
                .putInt("barHeight",height.text.toString().toIntOrNull()?.coerceIn(18,100)?:30)
                .putInt("maxDots",maxDots.text.toString().toIntOrNull()?.coerceIn(1,30)?:8)
                .apply()
            sendBroadcast(Intent(DotOverlayService.ACTION_REFRESH).setPackage(packageName))
            Toast.makeText(this,"Saved. Only added apps will show dots.",Toast.LENGTH_SHORT).show()
        })
        root.addView(button("Start floating dots") { if(Settings.canDrawOverlays(this)) { startService(Intent(this,DotOverlayService::class.java)) } else Toast.makeText(this,"Grant overlay permission first",Toast.LENGTH_LONG).show() })
        root.addView(button("Stop floating dots") { stopService(Intent(this,DotOverlayService::class.java)) })
        setContentView(scroll)
    }
}
