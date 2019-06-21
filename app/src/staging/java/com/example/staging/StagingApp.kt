package com.example.staging

import android.Manifest
import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.tbruyelle.rxpermissions2.RxPermissions
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class StagingApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "StagingApp")
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            var activitySize = 0
            override fun onActivityPaused(activity: Activity?) {
            }

            override fun onActivityResumed(activity: Activity?) {
                if (activitySize == 1) {
                    (activity as? FragmentActivity)?.let {
                        val rxPerm = RxPermissions(it)
                        rxPerm.request(Manifest.permission.WRITE_EXTERNAL_STORAGE).subscribe({ result ->
                            if (!result) {
                                Toast.makeText(
                                    it,
                                    "You have to grant the permission to save coverage file",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }, { e ->
                            e.printStackTrace()
                        })
                    }
                }
            }

            override fun onActivityStarted(activity: Activity?) {
            }

            override fun onActivityDestroyed(activity: Activity?) {
                activitySize -= 1

                if (activitySize <= 0) {
                    generateCoverageReport(createFile())
                }
            }

            override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {
            }

            override fun onActivityStopped(activity: Activity?) {
            }

            override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {
                activitySize += 1
            }

        })

    }


    private fun generateCoverageReport(file: File) {
        Log.d(TAG, "generateCoverageReport():${file.absolutePath}")
        FileOutputStream(file, false).use {
            val agent = Class.forName("org.jacoco.agent.rt.RT")
                .getMethod("getAgent")
                .invoke(null)


            Log.d(TAG, agent.toString())
            it.write(
                agent.javaClass.getMethod("getExecutionData", Boolean::class.javaPrimitiveType)
                    .invoke(agent, false) as ByteArray
            )
        }
    }

    fun createFile(): File {
        val file = File(Environment.getExternalStorageDirectory(), "jacoco/$DEFAULT_COVERAGE_FILE_PATH")
        if (!file.exists()) {
            try {
                file.parentFile?.mkdirs()
                file.createNewFile()
            } catch (e: IOException) {
                Log.d(TAG, "异常 : $e")
                e.printStackTrace()
            }
        }
        return file
    }

    companion object {
        const val DEFAULT_COVERAGE_FILE_PATH = "jacoco-coverage.ec"
        const val TAG = "StagingApp"
    }
}