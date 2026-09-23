package com.qiandaizi.app

import android.app.Application
import android.content.Context
import com.qiandaizi.app.core.AppGraph
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class QianApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
        CrashStore.install(this)
    }
}

/** 崩溃日志：闪退时把堆栈写入文件，下次启动展示，便于定位 */
object CrashStore {

    private fun file(context: Context): File = File(context.filesDir, "last-crash.txt")

    fun install(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val sw = StringWriter()
                PrintWriter(sw).use { pw ->
                    pw.println("thread=${thread.name}")
                    throwable.printStackTrace(pw)
                }
                file(context).writeText(sw.toString())
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    fun read(context: Context): String? =
        file(context).takeIf { it.exists() }?.readText()

    fun clear(context: Context) {
        file(context).delete()
    }
}
