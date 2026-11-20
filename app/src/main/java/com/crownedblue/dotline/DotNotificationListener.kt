package com.crownedblue.dotline
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
class DotNotificationListener:NotificationListenerService(){
 override fun onListenerConnected(){super.onListenerConnected();publish(activeNotifications?.toList()?:emptyList())}
 override fun onNotificationPosted(sbn:StatusBarNotification){publish(activeNotifications?.toList()?:listOf(sbn))}
 override fun onNotificationRemoved(sbn:StatusBarNotification){publish(activeNotifications?.toList()?:emptyList())}
 private fun publish(items:List<StatusBarNotification>){
  val p=getSharedPreferences("dotline",MODE_PRIVATE);val selected=p.getStringSet("selectedPackages",emptySet())?:emptySet();val configured=p.getBoolean("selectionConfigured",false)
  val apps=items.filter{!it.isOngoing&&(!configured||selected.contains(it.packageName))}.map{it.packageName}.distinct()
  p.edit().putStringSet("activePackages",apps.toSet()).apply()
  sendBroadcast(android.content.Intent(DotOverlayService.ACTION_REFRESH).setPackage(packageName))
 }
}