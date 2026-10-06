package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.model.CreditCardEntity
import com.example.receiver.PaymentReminderReceiver
import java.util.Calendar
import java.util.Locale

data class PaymentAlertSchedule(
    val cardId: Long,
    val cardName: String,
    val periodName: String,
    val paymentLimitDateStr: String,
    val paymentLimitDesc: String,
    val alertDateStr: String,
    val alertDateDesc: String,
    val triggerMillis: Long,
    val daysUntilAlert: Int,
    val leadDays: Int
)

object NotificationScheduler {
    private const val TAG = "NotificationScheduler"
    const val BASE_ALARM_REQUEST_CODE = 3000

    private val MONTH_NAMES = listOf(
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    )

    private val MONTH_NAMES_LOWER = listOf(
        "enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
    )

    private val DAY_NAMES = listOf(
        "", "Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"
    )

    /**
     * Calculates the exact upcoming payment limit date and the corresponding notification reminder date.
     * Guarantees that the returned triggerMillis is strictly in the future on the exact date established.
     */
    fun calculateUpcomingAlert(
        card: CreditCardEntity,
        leadDays: Int,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): PaymentAlertSchedule {
        val nowCal = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
        val currentYear = nowCal.get(Calendar.YEAR)
        val currentMonth = nowCal.get(Calendar.MONTH) // 0-based

        // Look ahead up to 12 months to find the first payment reminder date that is strictly in the future
        for (monthOffset in 0..12) {
            val cutCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, currentYear)
                set(Calendar.MONTH, currentMonth)
                set(Calendar.DAY_OF_MONTH, 1)
                add(Calendar.MONTH, monthOffset)
                val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
                set(Calendar.DAY_OF_MONTH, card.cutDay.coerceIn(1, maxDay))
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val cutMonthIndex = cutCal.get(Calendar.MONTH)
            val cutYear = cutCal.get(Calendar.YEAR)
            val periodName = "${MONTH_NAMES[cutMonthIndex]} $cutYear"

            // Compute payment limit date by adding business days (excluding Sat & Sun)
            val payLimitCal = (cutCal.clone() as Calendar)
            var remainingBusinessDays = card.payBusinessDays.coerceIn(1, 30)
            while (remainingBusinessDays > 0) {
                payLimitCal.add(Calendar.DAY_OF_MONTH, 1)
                val dow = payLimitCal.get(Calendar.DAY_OF_WEEK)
                if (dow != Calendar.SATURDAY && dow != Calendar.SUNDAY) {
                    remainingBusinessDays--
                }
            }

            val payDayNum = payLimitCal.get(Calendar.DAY_OF_MONTH)
            val payMonthNum = payLimitCal.get(Calendar.MONTH) + 1
            val payYearNum = payLimitCal.get(Calendar.YEAR)
            val payDowName = DAY_NAMES.getOrElse(payLimitCal.get(Calendar.DAY_OF_WEEK)) { "" }
            val payMonthName = MONTH_NAMES_LOWER.getOrElse(payLimitCal.get(Calendar.MONTH)) { "" }

            val paymentLimitDateStr = String.format(Locale.US, "%02d/%02d/%04d", payDayNum, payMonthNum, payYearNum)
            val paymentLimitDesc = "$payDowName, $payDayNum de $payMonthName de $payYearNum"

            // Calculate alert date by subtracting leadDays
            val alertCal = (payLimitCal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_MONTH, -leadDays)
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val alertMillis = alertCal.timeInMillis
            if (alertMillis > currentTimeMillis) {
                val alertDayNum = alertCal.get(Calendar.DAY_OF_MONTH)
                val alertMonthNum = alertCal.get(Calendar.MONTH) + 1
                val alertYearNum = alertCal.get(Calendar.YEAR)
                val alertDowName = DAY_NAMES.getOrElse(alertCal.get(Calendar.DAY_OF_WEEK)) { "" }
                val alertMonthName = MONTH_NAMES_LOWER.getOrElse(alertCal.get(Calendar.MONTH)) { "" }

                val alertDateStr = String.format(Locale.US, "%02d/%02d/%04d", alertDayNum, alertMonthNum, alertYearNum)
                val alertDateDesc = "$alertDowName, $alertDayNum de $alertMonthName de $alertYearNum"

                val diffMillis = alertMillis - currentTimeMillis
                val daysUntil = (diffMillis / (1000L * 60 * 60 * 24)).toInt().coerceAtLeast(0)

                return PaymentAlertSchedule(
                    cardId = card.id,
                    cardName = card.cardName,
                    periodName = periodName,
                    paymentLimitDateStr = paymentLimitDateStr,
                    paymentLimitDesc = paymentLimitDesc,
                    alertDateStr = alertDateStr,
                    alertDateDesc = alertDateDesc,
                    triggerMillis = alertMillis,
                    daysUntilAlert = daysUntil,
                    leadDays = leadDays
                )
            }
        }

        // Fallback for safety: tomorrow at 9 AM
        val fallbackCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val fDay = fallbackCal.get(Calendar.DAY_OF_MONTH)
        val fMonth = fallbackCal.get(Calendar.MONTH) + 1
        val fYear = fallbackCal.get(Calendar.YEAR)
        val fDateStr = String.format(Locale.US, "%02d/%02d/%04d", fDay, fMonth, fYear)

        return PaymentAlertSchedule(
            cardId = card.id,
            cardName = card.cardName,
            periodName = "${MONTH_NAMES[nowCal.get(Calendar.MONTH)]} $currentYear",
            paymentLimitDateStr = fDateStr,
            paymentLimitDesc = "$fDay de ${MONTH_NAMES_LOWER[fallbackCal.get(Calendar.MONTH)]} de $fYear",
            alertDateStr = fDateStr,
            alertDateDesc = "$fDay de ${MONTH_NAMES_LOWER[fallbackCal.get(Calendar.MONTH)]} de $fYear",
            triggerMillis = fallbackCal.timeInMillis,
            daysUntilAlert = 1,
            leadDays = leadDays
        )
    }

    /**
     * Schedules a payment reminder alarm using AlarmManager.
     * Uses canScheduleExactAlarms() on Android 12+ and falls back gracefully to setAndAllowWhileIdle.
     */
    fun scheduleCardPaymentAlarm(
        context: Context,
        card: CreditCardEntity,
        leadDays: Int
    ): PaymentAlertSchedule? {
        val schedule = calculateUpcomingAlert(card, leadDays)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return null

        val intent = Intent(context, PaymentReminderReceiver::class.java).apply {
            putExtra("card_id", card.id)
            putExtra("card_name", card.cardName)
            putExtra("payment_limit", schedule.paymentLimitDateStr)
            putExtra("payment_limit_desc", schedule.paymentLimitDesc)
            putExtra("period_name", schedule.periodName)
            putExtra("lead_days", leadDays)
            putExtra("alert_date", schedule.alertDateStr)
            putExtra("alert_date_desc", schedule.alertDateDesc)
            putExtra("business_days", card.payBusinessDays)
            putExtra("cut_day", card.cutDay)
        }

        val requestCode = (BASE_ALARM_REQUEST_CODE + card.id).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    try {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            schedule.triggerMillis,
                            pendingIntent
                        )
                    } catch (e: SecurityException) {
                        Log.w(TAG, "Exact alarm permission rejected, using setAndAllowWhileIdle", e)
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            schedule.triggerMillis,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        schedule.triggerMillis,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    schedule.triggerMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    schedule.triggerMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled alarm for ${card.cardName} on ${schedule.alertDateStr} at 09:00 AM (trigger: ${schedule.triggerMillis})")
            return schedule
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling alarm for ${card.cardName}", e)
            return null
        }
    }

    /**
     * Cancels the scheduled alarm for a specific card.
     */
    fun cancelCardPaymentAlarm(context: Context, cardId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, PaymentReminderReceiver::class.java)
        val requestCode = (BASE_ALARM_REQUEST_CODE + cardId).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled alarm for cardId $cardId")
        }
    }

    /**
     * Schedules alarms for all given cards.
     */
    fun scheduleAllCards(
        context: Context,
        cards: List<CreditCardEntity>,
        leadDays: Int
    ): List<PaymentAlertSchedule> {
        return cards.mapNotNull { scheduleCardPaymentAlarm(context, it, leadDays) }
    }

    /**
     * Cancels alarms for all given cards.
     */
    fun cancelAllCards(context: Context, cards: List<CreditCardEntity>) {
        cards.forEach { cancelCardPaymentAlarm(context, it.id) }
    }
}
