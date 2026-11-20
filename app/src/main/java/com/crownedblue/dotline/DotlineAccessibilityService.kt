package com.crownedblue.dotline
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
class DotlineAccessibilityService:AccessibilityService(){
 override fun onServiceConnected(){serviceInfo=serviceInfo.apply{eventTypes=AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOWS_CHANGED;feedbackType=AccessibilityServiceInfo.FEEDBACK_GENERIC;flags=AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;notificationTimeout=100}}
 override fun onAccessibilityEvent(event:AccessibilityEvent?){
  val h=resources.displayMetrics.heightPixels;val w=resources.displayMetrics.widthPixels
  val open=windows.orEmpty().any{win->try{val root=win.root?:return@any false;if(root.packageName!="com.android.systemui")return@any false;val r=Rect();root.getBoundsInScreen(r);r.height()>h*.28&&r.width()>w*.65}catch(_:Exception){false}}
  sendBroadcast(Intent(DotOverlayService.ACTION_SHADE).setPackage(packageName).putExtra("open",open))
 }
 override fun onInterrupt(){sendBroadcast(Intent(DotOverlayService.ACTION_SHADE).setPackage(packageName).putExtra("open",false))}
}