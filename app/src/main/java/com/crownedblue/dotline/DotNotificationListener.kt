package com.crownedblue.dotline

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class DotNotificationListener : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        publish(activeNotifications?.toList() ?: emptyList())
    }
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        publish(activeNotifications?.toList() ?: listOf(sbn))
    }
    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        publish(activeNotifications?.toList() ?: emptyList())
    }
    private fun publish(items: List<StatusBarNotification>) {
        val packages = items.filter { !it.isOngoing }.map { it.packageName }.distinct()
        getSharedPreferences("dotline", MODE_PRIVATE).edit().putStringSet("activePackages", packages.toSet()).apply()
        sendBroadcast(android.content.Intent(DotOverlayService.ACTION_REFRESH).setPackage(packageName))
    }
}
