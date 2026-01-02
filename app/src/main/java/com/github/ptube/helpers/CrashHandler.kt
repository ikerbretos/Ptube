package com.github.ptube.helpers

import android.content.Context
import android.content.Intent
import android.os.Process
import com.github.ptube.ui.activities.CrashActivity
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

class CrashHandler(private val context: Context) : Thread.UncaughtExceptionHandler {
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        val stringWriter = StringWriter()
        throwable.printStackTrace(PrintWriter(stringWriter))
        val stackTrace = stringWriter.toString()

        val intent = Intent(context, CrashActivity::class.java).apply {
            putExtra("error", stackTrace)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        context.startActivity(intent)

        Process.killProcess(Process.myPid())
        exitProcess(10)
    }
}
