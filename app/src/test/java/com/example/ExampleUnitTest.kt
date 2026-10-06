package com.example

import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.Locale

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testNotificationDateCalculation() {
    val paymentLimit = "10/11/2026"
    val leadDays = 3
    val parts = paymentLimit.split("/")
    val d = parts[0].toInt()
    val m = parts[1].toInt() - 1
    val y = parts[2].toInt()
    val cal = Calendar.getInstance()
    cal.set(y, m, d)
    cal.add(Calendar.DAY_OF_MONTH, -leadDays)
    val alertDate = String.format(Locale.US, "%02d/%02d/%04d", cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR))
    assertEquals("07/11/2026", alertDate)
  }

  @Test
  fun testNotificationSchedulerCalculation() {
    val card = com.example.data.model.CreditCardEntity(
      id = 1,
      bankName = "BANCO PICHINCHA",
      cardName = "Visa Signature",
      network = "VISA",
      tier = "SIGNATURE",
      last4 = "4892",
      cutDay = 24,
      payDay = 10,
      payBusinessDays = 12
    )
    val testCurrentTime = Calendar.getInstance().apply {
      set(2026, Calendar.OCTOBER, 5, 10, 0, 0)
    }.timeInMillis

    val schedule = com.example.util.NotificationScheduler.calculateUpcomingAlert(
      card = card,
      leadDays = 3,
      currentTimeMillis = testCurrentTime
    )

    assertEquals("10/11/2026", schedule.paymentLimitDateStr)
    assertEquals("07/11/2026", schedule.alertDateStr)
    assertTrue("Trigger millis must be after test current time", schedule.triggerMillis > testCurrentTime)
  }
}
