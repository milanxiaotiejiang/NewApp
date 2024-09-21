package com.milan.newapp.kts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.elvishew.xlog.XLog
import com.milan.newapp.MainActivity

/**
 * 每日定时启动：通过 DailyBootReceiver 监听定时的自定义广播 ("DAILY_BOOT_ACTION")，这允许应用在每天特定时间自动启动或执行某些操作。
 */
class DailyBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        XLog.e("Broadcast received: ${intent?.action}")
        if (intent?.action == "DAILY_BOOT_ACTION") {
            XLog.e("Daily alarm received. Starting the app...")

            val activityIntent = Intent(context, MainActivity::class.java)
            activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context?.startActivity(activityIntent)
        }
    }
}

class DailyBootWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        XLog.e("WorkManager triggered. Starting the app...")

        // 启动前台服务
        val serviceIntent = Intent(applicationContext, DailyForegroundService::class.java)
        applicationContext.startForegroundService(serviceIntent)

        // 启动第三方应用
        // 启动第一个应用
//        val result1 = startApp("io.dcloud.H5B1841EE")
        // 启动第二个应用
//        val result2 = startApp("com.ss.android.lark")
        // 启动第三个应用
        val result3 = startApp("com.alibaba.android.rimet")

        return if (result3) {
            Result.success()
        } else {
            Result.failure()
        }

    }


    private fun startApp(packageName: String): Boolean {
        val launchIntent = applicationContext.packageManager.getLaunchIntentForPackage(packageName)
        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            applicationContext.startActivity(launchIntent)
            true
        } else {
            XLog.e("Unable to find package: $packageName")
            false
        }
    }
}

/**
 * 设备启动完成后自动启动应用：通过 BootCompletedReceiver 监听设备启动完成的广播 (Intent.ACTION_BOOT_COMPLETED)，应用可以在设备重启后自动启动，恢复用户的工作环境或继续执行未完成的任务。
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            XLog.e("Device boot completed. Starting the app...")

            val launchIntent = context.packageManager.getLaunchIntentForPackage("com.milan.newapp")
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
            } else {
                XLog.e("Unable to find package: com.milan.newapp")
            }
        }
    }
}