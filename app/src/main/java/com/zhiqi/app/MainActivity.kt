package com.zhiqi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.zhiqi.app.security.AppLockManager
import com.zhiqi.app.ui.ZhiQiApp
import com.zhiqi.app.ui.ZhiQiTheme

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
            ZhiQiTheme {
                ZhiQiApp(lockManager = lockManager)
            }
        }
    }
}
