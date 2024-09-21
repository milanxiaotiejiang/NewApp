package com.milan.newapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.elvishew.xlog.XLog
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.milan.newapp.databinding.ActivityMainBinding
import com.milan.newapp.kts.DailyBootReceiver
import com.milan.newapp.kts.DailyBootWorker
import com.milan.newapp.kts.DailyForegroundService
import com.milan.newapp.kts.MockLocationService
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 101
        private const val CALENDAR_HOUR_OF_DAY = 10
        private const val CALENDAR_MINUTE = 0
        private const val DAILY_BOOT_ACTION = "DAILY_BOOT_ACTION"
    }

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private lateinit var locationClient: AMapLocationClient
    private lateinit var locationOption: AMapLocationClientOption

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        binding.appBarMain.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .setAnchorView(R.id.fab).show()
        }
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        checkAndRequestPermissions()

        getInstalledApps()

//        startMockLocationService()
//        initLocation()

        startDailyForegroundService()

        cancelAlarm()  // 首先取消现有的闹钟
        setDailyAlarm()
        setDailyWork()

        requestIgnoreBatteryOptimizations()

    }

    private fun checkAndRequestPermissions() {
        val permissionsNeeded = listOf(
            android.Manifest.permission.RECEIVE_BOOT_COMPLETED,
            android.Manifest.permission.SCHEDULE_EXACT_ALARM,
            android.Manifest.permission.WAKE_LOCK,
            android.Manifest.permission.FOREGROUND_SERVICE,
            android.Manifest.permission.QUERY_ALL_PACKAGES,
            android.Manifest.permission.INTERNET,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_NETWORK_STATE,
//            android.Manifest.permission.ACCESS_MOCK_LOCATION,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_PHONE_STATE,
            android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )

        val permissionsToRequest = permissionsNeeded.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest, PERMISSIONS_REQUEST_CODE)
        }
    }

    private fun getInstalledApps() {
        val packageManager = packageManager
        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        for (packageInfo in packages) {
            val appName = packageManager.getApplicationLabel(packageInfo).toString()
            val packageName = packageInfo.packageName

            XLog.d("App Name: $appName, Package Name: $packageName")
        }
    }

    private fun startMockLocationService() {
        val intent = Intent(this, MockLocationService::class.java)
        startForegroundService(intent)
    }

    private fun initLocation() {
        // 初始化客户端
        locationClient = AMapLocationClient(applicationContext)

        // 设置定位参数
        locationOption = AMapLocationClientOption().apply {
            locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
            isOnceLocation = true
            isNeedAddress = true
            httpTimeOut = 8000
        }

        // 设置定位参数到客户端
        locationClient.setLocationOption(locationOption)

        // 设置定位监听
        locationClient.setLocationListener { location ->
            if (location != null) {
                if (location.errorCode == 0) {
                    // 定位成功，可以获取经纬度和地址信息
                    val latitude = location.latitude
                    val longitude = location.longitude
                    val altitude = location.altitude
                    val accuracy = location.accuracy
                    val bearing = location.bearing
                    val speed = location.speed
                    val verticalAccuracyMeters = location.verticalAccuracyMeters
                    val speedAccuracyMetersPerSecond = location.speedAccuracyMetersPerSecond
                    val bearingAccuracyDegrees = location.bearingAccuracyDegrees

                    val address = location.address
                    XLog.e("定位成功: $address - $latitude, $longitude, $altitude, $accuracy, $bearing, $speed, $verticalAccuracyMeters, $speedAccuracyMetersPerSecond, $bearingAccuracyDegrees")
                } else {
                    // 定位失败
                    XLog.e("定位失败: ${location.errorCode} - ${location.errorInfo}")
                }
            }
        }

        // 启动定位
        locationClient.startLocation()
    }

    private fun startDailyForegroundService() {
        val serviceIntent = Intent(this, DailyForegroundService::class.java)
        startForegroundService(serviceIntent)
    }

    private fun setDailyAlarm() {
        // 获取系统的 AlarmManager 服务，用于设置定时任务。
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 创建一个指向 DailyBootReceiver 的 Intent，并为其设置一个自定义的动作。
        val intent = Intent(this, DailyBootReceiver::class.java).apply {
            action = DAILY_BOOT_ACTION
        }

        // 创建一个 PendingIntent，当触发条件满足时，这个 PendingIntent 会发送上面创建的广播。
        // 设置标志 FLAG_UPDATE_CURRENT 表示如果 PendingIntent 已存在则更新它，FLAG_IMMUTABLE 表明内容不会改变。
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 计算触发时间，确保每天定时触发任务。
        val triggerTime = getNextTriggerTime(CALENDAR_HOUR_OF_DAY, CALENDAR_MINUTE)

        // 设置重复的闹钟，使用 RTC_WAKEUP 模式，在设备休眠时也能唤醒设备。
        // 闹钟首次触发时间由 triggerTime 指定，之后每天重复。
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    private fun setDailyWork() {
        // 计算工作任务首次触发时间，设置为比闹钟任务晚1分钟。
        val triggerTime = getNextTriggerTime(CALENDAR_HOUR_OF_DAY, CALENDAR_MINUTE + 1)
        // 计算从当前时间到首次触发时间的毫秒数。
        val initialDelayTime = triggerTime - System.currentTimeMillis()

        // 创建一个周期性的工作请求，设置周期为每24小时。
        val dailyWorkRequest = PeriodicWorkRequestBuilder<DailyBootWorker>(24, TimeUnit.HOURS)
            // 设置初始延迟，确保任务在计划的首次触发时间开始执行。
            .setInitialDelay(initialDelayTime, TimeUnit.MILLISECONDS)
            .build()

        // 将创建的工作请求提交给 WorkManager，让它管理任务的调度和执行。
        WorkManager.getInstance(this).enqueue(dailyWorkRequest)
    }


    private fun getNextTriggerTime(hour: Int, minute: Int): Long {
        // 获取当前时间的毫秒值。
        val now = System.currentTimeMillis()
        // 使用当前时间初始化一个 Calendar 对象，并设置特定的小时和分钟，秒数设置为0。
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        // 如果计算出的触发时间早于当前时间，说明触发时间已过，需要将触发时间调整到下一天的相同时间。
        if (calendar.timeInMillis < now) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        // 返回计算后的触发时间毫秒值。
        return calendar.timeInMillis
    }

    private fun cancelAlarm() {
        val intent = Intent(this, DailyBootReceiver::class.java).apply {
            action = DAILY_BOOT_ACTION
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)  // 取消闹钟
    }

    private fun requestIgnoreBatteryOptimizations() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        val packageName = packageName
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            startActivity(intent)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            grantResults.forEachIndexed { index, result ->
                if (result != PackageManager.PERMISSION_GRANTED) {
                    XLog.e("Permission ${permissions[index]} was not granted")
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationClient.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

}