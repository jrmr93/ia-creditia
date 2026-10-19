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

  @Test
  fun testPeriodChronologicalOrdering() {
    val monthNames = listOf(
      "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
      "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    )

    fun parseYearMonth(p: String): Pair<Int, Int>? {
      val parts = p.trim().split(" ")
      if (parts.size == 2) {
        val mIdx = monthNames.indexOfFirst { it.equals(parts[0], ignoreCase = true) }
        val yr = parts[1].toIntOrNull()
        if (mIdx != -1 && yr != null) {
          return Pair(yr, mIdx)
        }
      }
      return null
    }

    val currentActivePeriod = "Octubre 2026"
    val testPeriods = listOf(
      "Septiembre 2026",
      "Agosto 2026",
      "Diciembre 2026",
      "Noviembre 2026",
      "Enero 2027",
      "Julio 2026"
    )

    val allPeriods = (testPeriods + currentActivePeriod).distinct()
    val sortedPeriods = allPeriods
      .mapNotNull { p -> parseYearMonth(p)?.let { Triple(it.first, it.second, p) } }
      .sortedWith(compareByDescending<Triple<Int, Int, String>> { it.first }.thenByDescending { it.second })
      .map { it.third }

    val fullList = if (sortedPeriods.size > 1) sortedPeriods + listOf("Todos los históricos") else sortedPeriods

    val currentIdx = fullList.indexOf(currentActivePeriod)
    assertTrue("El periodo actual debe estar presente en la lista", currentIdx != -1)

    // Verificar que los periodos futuros (que aún no llegan) están ubicados hacia arriba (índices menores)
    val novIdx = fullList.indexOf("Noviembre 2026")
    val dicIdx = fullList.indexOf("Diciembre 2026")
    val ene27Idx = fullList.indexOf("Enero 2027")

    assertTrue("Enero 2027 debe estar antes que Diciembre 2026", ene27Idx < dicIdx)
    assertTrue("Diciembre 2026 debe estar antes que Noviembre 2026", dicIdx < novIdx)
    assertTrue("Noviembre 2026 debe estar antes que Octubre 2026 (hacia arriba)", novIdx < currentIdx)

    // Verificar que los periodos pasados están ubicados hacia abajo (índices mayores)
    val sepIdx = fullList.indexOf("Septiembre 2026")
    val agoIdx = fullList.indexOf("Agosto 2026")
    val julIdx = fullList.indexOf("Julio 2026")

    assertTrue("Septiembre 2026 debe estar después de Octubre 2026 (hacia abajo)", sepIdx > currentIdx)
    assertTrue("Agosto 2026 debe estar después de Septiembre 2026", agoIdx > sepIdx)
    assertTrue("Julio 2026 debe estar después de Agosto 2026", julIdx > agoIdx)
    assertEquals("Todos los históricos", fullList.last())
  }
}
