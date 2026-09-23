package com.qiandaizi.app

import android.app.Application
import com.qiandaizi.app.core.AppGraph

class QianApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
    }
}
