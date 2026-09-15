package com.zabtaai

import android.app.Service
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

/**
 * REAL Android ForegroundService.
 * Maintains continuous operation with persistent notification.
 * Handles background restrictions and battery optimization.
 */
class ZabtaAIForegroundService : Service() {
    
    private val TAG = "ZabtaAI-Foreground"
    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "zabtaai_channel"

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "ForegroundService Created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "ForegroundService Started")
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ZabtaAI")
            .setContentText("Listening for voice commands...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        
        startForeground(NOTIFICATION_ID, notification)
        
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "ForegroundService Destroyed")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ZabtaAI Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "ZabtaAI voice control background service"
                enableVibration(false)
                enableLights(false)
            }
            
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
