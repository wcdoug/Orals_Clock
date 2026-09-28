package com.wenonah.oralstimer

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

/**
 * Required by Android's Device Owner API.
 * Declared in the manifest so the system recognises this app as a
 * candidate device owner (set via ADB: `dpm set-device-owner`).
 */
class AdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        // Device admin enabled — no action needed
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        // Device admin disabled — no action needed
    }
}
