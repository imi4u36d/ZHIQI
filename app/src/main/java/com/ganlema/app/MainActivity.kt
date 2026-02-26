package com.ganlema.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.ganlema.app.security.AppLockManager
import com.ganlema.app.ui.GanLeMeApp
import com.ganlema.app.ui.GanLeMeTheme

class MainActivity : ComponentActivity() {
    private lateinit var lockManager: AppLockManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lockManager = AppLockManager(applicationContext)
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                lockManager.onAppForegrounded()
            }

            override fun onStop(owner: LifecycleOwner) {
                lockManager.onAppBackgrounded()
            }
        })

        setContent {
            GanLeMeTheme {
                GanLeMeApp(lockManager = lockManager)
            }
        }
    }
}
