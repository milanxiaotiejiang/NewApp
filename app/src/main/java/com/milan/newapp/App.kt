package com.milan.newapp

import android.app.Application
import com.elvishew.xlog.LogConfiguration
import com.elvishew.xlog.XLog
import com.elvishew.xlog.printer.AndroidPrinter

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        val config = LogConfiguration.Builder()
            .tag("newapp")
            .build()

        XLog.init(config, AndroidPrinter())

    }
}