package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.util.NotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d(TAG, "Boot completed event received ($action). Checking payment notifications...")
            val prefs = context.getSharedPreferences("creditia_secure_prefs", Context.MODE_PRIVATE)
            val enabled = prefs.getBoolean("payment_notifications_enabled", false)
            if (enabled) {
                val leadDays = prefs.getInt("notification_lead_days", 3)
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val database = AppDatabase.getDatabase(context)
                        val cards = database.creditCardDao().getAllCardsList()
                        Log.d(TAG, "Rescheduling payment alarms for ${cards.size} cards (leadDays: $leadDays)")
                        NotificationScheduler.scheduleAllCards(context, cards, leadDays)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error rescheduling alarms on boot", e)
                    }
                }
            }
        }
    }
}
