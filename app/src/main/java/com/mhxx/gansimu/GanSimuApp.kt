package com.mhxx.gansimu

import android.app.Application
import com.mhxx.gansimu.data.DataRepository

class GanSimuApp : Application() {
    lateinit var repository: DataRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        repository = DataRepository(this)
        // バックグラウンドでデータ読み込み
        Thread {
            try {
                repository.load()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    companion object {
        lateinit var instance: GanSimuApp
            private set
    }
}
