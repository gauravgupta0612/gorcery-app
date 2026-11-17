package com.golagrocery.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.golagrocery.app.di.AppContainer

class GolaApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel("orders", "Order updates", NotificationManager.IMPORTANCE_HIGH))
            nm.createNotificationChannel(NotificationChannel("offers", "Offers & announcements", NotificationManager.IMPORTANCE_DEFAULT))
        }
    }
}
