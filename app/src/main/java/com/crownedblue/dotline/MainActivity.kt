package com.crownedblue.dotline

import android.app.Activity
import android.content.Intent
import android.graphics.Color
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

    private val swatches = listOf("#7C9CFF","#FF7E9D","#FFC857","#56D6B2","#C69CFF","#FF9466","#6ECBFF","#FFFFFF","#FF5252","#40C4FF","#B2FF59","#FFD740")
    private val rows = mutableListOf<AppRow>()

    private fun addAppRow(holder: LinearLayout, appName: String = "", hex: String = "#7C9CFF") {
        val wrap=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(0,dp(5),0,dp(8)) }
        val name=edit("App name (e.g. WhatsApp)",appName)
        val line=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=android.view.Gravity.CENTER_VERTICAL }
        val color=edit("#RRGGBB",hex).apply { layoutParams=LinearLayout.LayoutParams(0,dp(48),1f) }
        val pick=button("Choose colour") {
            val labels=swatches.map { it }
            AlertDialog.Builder(this).setTitle("Choose dot colour").setItems(labels.toTypedArray()) { _, which -> color.setText(swatches[which]) }.setNegativeButton("Cancel",null).show()
        }
        val remove=button("Remove") { holder.removeView(wrap); rows.removeAll { it.container===wrap } }
        line.addView(color); line.addView(pick); line.addView(remove)
        wrap.addView(name); wrap.addView(line); holder.addView(wrap)
        rows.add(AppRow(name,color,wrap))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
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
