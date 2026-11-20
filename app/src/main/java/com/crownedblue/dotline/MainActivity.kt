package com.crownedblue.dotline

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("dotline", MODE_PRIVATE) }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun label(s: String) = TextView(this).apply { text=s; textSize=15f; setTextColor(Color.WHITE); setPadding(0,dp(8),0,dp(8)) }
    private fun edit(h: String, v: String) = EditText(this).apply { hint=h; setText(v); setSingleLine(); setTextColor(-1); setHintTextColor(0xff999999.toInt()); setBackgroundColor(0xff25252b.toInt()) }
    private fun button(t: String, f: () -> Unit) = Button(this).apply { text=t; isAllCaps=false; setOnClickListener { f() } }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor=0xff101014.toInt(); window.navigationBarColor=0xff101014.toInt()
        val root=LinearLayout(this).apply { orientation=1; setPadding(dp(20),dp(18),dp(20),dp(24)); setBackgroundColor(0xff101014.toInt()) }
        val scroll=ScrollView(this); scroll.addView(root)
        root.addView(label("DOTLINE — Notification dots"))
        root.addView(button("Grant display-over-apps permission") { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:"+packageName))) })
        root.addView(button("Enable notification access") { startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")) })
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
        val x=edit("Horizontal position dp",prefs.getInt("x",120).toString()); val y=edit("Vertical position dp",prefs.getInt("y",4).toString()); root.addView(x); root.addView(y)
        root.addView(label("Choose apps (leave all unchecked to allow every app)"))
        val appList=LinearLayout(this).apply { orientation=1 }
        val selected=prefs.getStringSet("selectedPackages",emptySet())?:emptySet()
        val apps=packageManager.getInstalledApplications(0).filter { packageManager.getLaunchIntentForPackage(it.packageName)!=null }.sortedBy { packageManager.getApplicationLabel(it).toString() }
        apps.forEach { a -> appList.addView(CheckBox(this).apply { text=packageManager.getApplicationLabel(a).toString(); tag=a.packageName; isChecked=selected.contains(a.packageName); setTextColor(-1) }) }
        root.addView(appList)
        root.addView(button("Save settings") {
            val chosen=mutableSetOf<String>()
            for(i in 0 until appList.childCount) { val c=appList.getChildAt(i) as CheckBox; if(c.isChecked) chosen.add(c.tag as String) }
            prefs.edit().putStringSet("selectedPackages",chosen).putBoolean("selectionConfigured",chosen.isNotEmpty())
                .putString("barColor",color.text.toString().trim()).putString("effect",listOf("clean","glow","glass")[effect.selectedItemPosition])
                .putInt("dotSize",size.text.toString().toIntOrNull()?.coerceIn(3,28)?:8)
                .putInt("barWidth",width.text.toString().toIntOrNull()?.coerceIn(40,600)?:180)
                .putInt("barHeight",height.text.toString().toIntOrNull()?.coerceIn(18,100)?:30)
                .putInt("maxDots",maxDots.text.toString().toIntOrNull()?.coerceIn(1,30)?:8)
                .putInt("x",x.text.toString().toIntOrNull()?.coerceAtLeast(0)?:120)
                .putInt("y",y.text.toString().toIntOrNull()?.coerceAtLeast(0)?:4).apply()
            sendBroadcast(Intent(DotOverlayService.ACTION_REFRESH).setPackage(packageName))
            Toast.makeText(this,"Saved. Restart Dotline if dimensions changed.",Toast.LENGTH_SHORT).show()
        })
        root.addView(button("Start floating dots") { if(Settings.canDrawOverlays(this)) { val i=Intent(this,DotOverlayService::class.java); startService(i) } else Toast.makeText(this,"Grant overlay permission first",Toast.LENGTH_LONG).show() })
        root.addView(button("Stop floating dots") { stopService(Intent(this,DotOverlayService::class.java)) })
        setContentView(scroll)
    }
}