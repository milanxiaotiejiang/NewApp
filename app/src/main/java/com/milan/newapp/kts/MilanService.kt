package com.milan.newapp.kts

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.elvishew.xlog.XLog
import com.milan.newapp.MainActivity
import com.milan.newapp.R
import kotlin.random.Random

class DailyForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "DailyForegroundServiceChannel"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 这里可以执行额外的任务
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun startForegroundService() {
        createNotificationChannel()

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Daily Task Service")
            .setContentText("Running daily tasks")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID, "Daily Task Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }
}


class MockLocationService : Service() {
    companion object {
        const val MOCK_PROVIDER_NAME = LocationManager.GPS_PROVIDER
        const val CHANNEL_ID = "MockLocationServiceChannel"
        const val NOTIFICATION_ID = 2
    }

    private val handler = Handler(Looper.getMainLooper())
    private val updateLocationRunnable = object : Runnable {
        override fun run() {
            sendMockLocation()
            handler.postDelayed(this, 1000) // 每秒更新位置
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        startMockLocation()
        handler.post(updateLocationRunnable) // 启动周期性位置更新
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        stopMockLocation()
        handler.removeCallbacks(updateLocationRunnable) // 停止位置更新
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun startForegroundService() {
        createNotificationChannel()

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Mock Location Service")
            .setContentText("Mocking location for testing")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Mock Location Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

    private fun startMockLocation() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            locationManager.addTestProvider(
                MOCK_PROVIDER_NAME,
                false, false, false, false,
                true, true, true,
                ProviderProperties.POWER_USAGE_HIGH,
                ProviderProperties.ACCURACY_COARSE
            )
            locationManager.setTestProviderEnabled(MOCK_PROVIDER_NAME, true)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun sendMockLocation() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val mockLocation = Location(MOCK_PROVIDER_NAME).apply {
            latitude = 60.1 + (Random.nextFloat() - 0.5) * 0.0002
            longitude = 116.4 + (Random.nextFloat() - 0.5) * 0.0002
//            latitude = 40.001154
//            longitude = 116.437802
            altitude = 550.0
            time = System.currentTimeMillis()
            accuracy = 550.0f
            elapsedRealtimeNanos = System.nanoTime()
            bearing = 90.0f           // 方向
            speed = 0.5f           // 速度

            verticalAccuracyMeters = 0.0f    // 垂直精度
            speedAccuracyMetersPerSecond = 0.0f // 速度精度
            bearingAccuracyDegrees = 0.0f   // 方位精度
        }

//        定位成功: 北京市朝阳区北湖渠路8号靠近京环大厦 - 40.001154, 116.437802, 0.0, 550.0, 0.0, 0.0, 0.0, 0.0, 0.0
        try {
            locationManager.setTestProviderLocation(MOCK_PROVIDER_NAME, mockLocation)
//            XLog.e("Mock location updated: $mockLocation")
        } catch (e: SecurityException) {
            XLog.e("Security Exception in updating mock location: ${e.message}")
        }
    }

    private fun stopMockLocation() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            locationManager.removeTestProvider(MOCK_PROVIDER_NAME)
            XLog.e("Mock location stopped")
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}