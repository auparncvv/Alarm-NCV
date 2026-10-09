package com.example.alarmkata

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        c.startForegroundService(Intent(c, AlarmService::class.java))
        if (c.getSharedPreferences("p", 0).getBoolean("repeat", false)) {
            Scheduler.schedule(c)
        }
    }
}
