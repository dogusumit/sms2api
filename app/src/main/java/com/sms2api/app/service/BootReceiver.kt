package com.sms2api.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sms2api.app.SMS2ApiApplication
import com.sms2api.app.worker.SmsForwardWorker

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            val app = context.applicationContext as? SMS2ApiApplication ?: return
            val prefs = app.preferencesManager

            // Restart Foreground Service if auto-start or service was previously enabled
            if (prefs.isBootAutostartEnabled && prefs.isServiceEnabled) {
                SmsForwarderService.start(context)
            }

            // Reschedule periodic fallback sync
            app.setupPeriodicSyncWorker()

            // Trigger immediate check for any pending records
            SmsForwardWorker.enqueue(context)
        }
    }
}
