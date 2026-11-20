package com.crownedblue.dotline
import android.app.*
import android.content.*
import android.graphics.*
import android.os.*
import android.view.*
import kotlin.math.max

class DotOverlayService : Service() {
 companion object {
  const val ACTION_REFRESH="com.crownedblue.dotline.REFRESH"
  const val ACTION_SHADE="com.crownedblue.dotline.SHADE"
  private const val CHANNEL="dotline_overlay"
 }
 private lateinit var wm:WindowManager
 private var panel:DotView?=null
 private lateinit var params:WindowManager.LayoutParams
 private var shade=false
 private val receiver=object:BroadcastReceiver(){
  override fun onReceive(c:Context?,i:Intent?){
   if(i?.action==ACTION_SHADE) shade=i.getBooleanExtra("open",false)
   panel?.visibility=if(shade) View.GONE else View.VISIBLE
   panel?.invalidate()
  }
 }
 override fun onCreate(){
  super.onCreate(); wm=getSystemService(WINDOW_SERVICE) as WindowManager
  if(Build.VERSION.SDK_INT>=26) getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL,"Dotline overlay",NotificationManager.IMPORTANCE_LOW))
  val n=Notification.Builder(this,CHANNEL).setContentTitle("Dotline is running").setContentText("Notification dots are active").setSmallIcon(android.R.drawable.presence_online).setOngoing(true).build()
  startForeground(7,n)
  val f=IntentFilter().apply{addAction(ACTION_REFRESH);addAction(ACTION_SHADE)}
  if(Build.VERSION.SDK_INT>=33) registerReceiver(receiver,f,RECEIVER_NOT_EXPORTED) else @Suppress("DEPRECATION") registerReceiver(receiver,f)
  val p=getSharedPreferences("dotline",MODE_PRIVATE); val d=resources.displayMetrics.density
  params=WindowManager.LayoutParams((p.getInt("barWidth",180)*d).toInt(),(p.getInt("barHeight",30)*d).toInt(),WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT).apply{gravity=Gravity.TOP or Gravity.START;x=(p.getInt("x",120)*d).toInt();y=(p.getInt("y",4)*d).toInt()}
  panel=DotView(this).apply{setBackgroundColor(Color.TRANSPARENT);setLayerType(View.LAYER_TYPE_SOFTWARE,null)}
  panel!!.setOnTouchListener{_,e->when(e.action){MotionEvent.ACTION_DOWN->true;MotionEvent.ACTION_MOVE->{params.x+=e.x.toInt()-panel!!.width/2;params.y+=e.y.toInt()-panel!!.height/2;wm.updateViewLayout(panel,params);true};MotionEvent.ACTION_UP->true;else->false}}
  wm.addView(panel,params)
 }
 override fun onStartCommand(i:Intent?,f:Int,s:Int)=START_STICKY
 override fun onDestroy(){try{unregisterReceiver(receiver)}catch(_:Exception){};panel?.let{try{wm.removeView(it)}catch(_:Exception){}};panel=null;super.onDestroy()}
 override fun onBind(i:Intent?):IBinder?=null
 inner class DotView(c:Context):View(c){
  private val p=getSharedPreferences("dotline",MODE_PRIVATE); private val paint=Paint(Paint.ANTI_ALIAS_FLAG); private val density=resources.displayMetrics.density
  override fun onDraw(c:Canvas){
   super.onDraw(c); c.drawColor(Color.TRANSPARENT,PorterDuff.Mode.CLEAR)
   val raw=try{Color.parseColor(p.getString("barColor","#00000000"))}catch(_:Exception){Color.TRANSPARENT}
   val alpha=(Color.alpha(raw)*p.getInt("barOpacity",0).coerceIn(0,100)/100f).toInt()
   if(alpha>0){paint.clearShadowLayer();paint.style=Paint.Style.FILL;paint.color=Color.argb(alpha,Color.red(raw),Color.green(raw),Color.blue(raw));c.drawRoundRect(0f,0f,width.toFloat(),height.toFloat(),height/2f,height/2f,paint)}
   val all=p.getStringSet("activePackages",emptySet())?.toList()?.sorted()?:emptyList()
   val selected=p.getStringSet("selectedPackages",emptySet())?:emptySet()
   val apps=all.filter{!p.getBoolean("selectionConfigured",false)||selected.contains(it)}.take(p.getInt("maxDots",8).coerceIn(1,30))
   if(apps.isEmpty())return
   val dot=p.getInt("dotSize",8).coerceIn(3,28)*density;val gap=max(dot*1.5f,8f*density);val total=apps.size*dot+max(0,apps.size-1)*gap
   var x=(width-total)/2f+dot/2f;val y=height/2f;val a=p.getInt("dotOpacity",100).coerceIn(0,100)*255/100
   val effect=p.getString("effect","clean")
   apps.forEach{app->
    val color=try{p.getString("app_"+app,null)?.let{Color.parseColor(it)}?:palette(app)}catch(_:Exception){palette(app)}
    paint.style=Paint.Style.FILL;paint.color=Color.argb(a,Color.red(color),Color.green(color),Color.blue(color))
    when(effect){
     "glow"->{paint.setShadowLayer(8f*density,0f,0f,Color.argb(a,Color.red(color),Color.green(color),Color.blue(color)));c.drawCircle(x,y,dot/2f,paint);paint.clearShadowLayer()}
     "glass"->{paint.color=Color.argb(a*72/100,Color.red(color),Color.green(color),Color.blue(color));c.drawCircle(x,y,dot/2f,paint);paint.style=Paint.Style.STROKE;paint.strokeWidth=density;paint.color=Color.argb(a*80/100,255,255,255);c.drawCircle(x,y,dot/2f,paint);paint.style=Paint.Style.FILL;paint.color=Color.argb(a*45/100,255,255,255);c.drawOval(x-dot*.23f,y-dot*.25f,x+dot*.12f,y-dot*.05f,paint)}
     else->c.drawCircle(x,y,dot/2f,paint)
    };x+=dot+gap
   };paint.clearShadowLayer()
  }
  private fun palette(s:String):Int{val colors=intArrayOf(0xff7c9cff.toInt(),0xffff7e9d.toInt(),0xffffc857.toInt(),0xff56d6b2.toInt(),0xffc69cff.toInt(),0xffff9466.toInt(),0xff6ecbff.toInt());return colors[(s.hashCode() and 0x7fffffff)%colors.size]}
 }
}