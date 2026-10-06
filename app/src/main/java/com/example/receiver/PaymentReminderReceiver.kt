package com.example.receiver

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.local.AppDatabase
import com.example.ui.viewmodel.CreditiaViewModel
import com.example.util.NotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PaymentReminderReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "PaymentReminderReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val cardId = intent.getLongExtra("card_id", 0L)
        val cardName = intent.getStringExtra("card_name") ?: "Tarjeta de Crédito"
        val paymentLimit = intent.getStringExtra("payment_limit") ?: ""
        val paymentLimitDesc = intent.getStringExtra("payment_limit_desc") ?: paymentLimit
        val periodName = intent.getStringExtra("period_name") ?: ""
        val leadDays = intent.getIntExtra("lead_days", 3)
        val alertDate = intent.getStringExtra("alert_date") ?: ""
        val businessDays = intent.getIntExtra("business_days", 12)
        val cutDay = intent.getIntExtra("cut_day", 24)

        Log.d(TAG, "Payment alarm received for $cardName (cardId: $cardId, limit: $paymentLimit, alertDate: $alertDate)")

        // Ensure notification channel exists
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CreditiaViewModel.NOTIFICATION_CHANNEL_ID,
                "Recordatorios de Pago",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones para no olvidar pagar la tarjeta antes de la fecha máxima de pago"
                enableLights(true)
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }

        // Check POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted. Cannot display notification.")
                return
            }
        }

        try {
            val notifId = (CreditiaViewModel.PAYMENT_NOTIFICATION_ID + cardId).toInt()
            val builder = NotificationCompat.Builder(context, CreditiaViewModel.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("🔔 Recordatorio de Pago: $cardName")
                .setContentText("Tu pago vence en $leadDays días ($paymentLimit). ¡Paga a tiempo!")
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText(
                        "¡No olvides realizar el pago de tu tarjeta $cardName!\n\n" +
                        "• Periodo contable: $periodName\n" +
                        "• Fecha máxima de pago: $paymentLimitDesc\n" +
                        "• Aviso programado: $leadDays días antes ($alertDate)\n\n" +
                        "Fecha calculada sumando $businessDays días hábiles tras la fecha superior de corte (día $cutDay de $periodName)."
                    )
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            NotificationManagerCompat.from(context).notify(notifId, builder.build())
            Log.d(TAG, "Notification successfully posted for $cardName (id: $notifId)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to display payment notification", e)
        }

        // Automatically reschedule next cycle payment reminder
        val prefs = context.getSharedPreferences("creditia_secure_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("payment_notifications_enabled", false) && cardId > 0L) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val card = db.creditCardDao().getCardById(cardId)
                    if (card != null) {
                        NotificationScheduler.scheduleCardPaymentAlarm(context, card, leadDays)
                        Log.d(TAG, "Rescheduled next cycle alarm for ${card.cardName}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to reschedule next cycle alarm", e)
                }
            }
        }
    }
}
