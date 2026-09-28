package com.wenonah.oralstimer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Listens for BOOT_COMPLETED and launches MainActivity automatically,
 * so the timer app starts the moment the tablet finishes booting.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val launch = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(launch)
        }
    }
}
