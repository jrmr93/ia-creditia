package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Creditia", appName)
  }

  @Test
  fun `test backup export and restore includes all config and data`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = context.getSharedPreferences("creditia_secure_prefs", Context.MODE_PRIVATE)

    // Configurar preferencias de la app
    prefs.edit()
      .putString("master_pin", "4321")
      .putBoolean("biometrics_enabled", true)
      .putBoolean("payment_notifications_enabled", true)
      .putInt("notification_lead_days", 5)
      .putBoolean("gemini_ai_enabled", true)
      .putString("gemini_api_key", "test-api-key-gemini")
      .putString("gemini_model", "gemini-3.5-flash")
      .apply()

    val card = com.example.data.model.CreditCardEntity(
      id = 10,
      bankName = "BCP",
      cardName = "Visa Infinite",
      network = "VISA",
      tier = "INFINITE",
      last4 = "9999",
      cutDay = 20,
      payDay = 15,
      payBusinessDays = 15,
      cardColorTheme = "black",
      creditLimit = 15000.0,
      isDefault = true
    )

    val expense = com.example.data.model.ExpenseEntity(
      id = 20,
      cardId = 10,
      sequenceNumber = 1,
      establishment = "Supermercado Wong",
      description = "Compra del mes",
      amount = 250.75,
      dateString = "2026/10/19",
      billingPeriod = "Noviembre 2026",
      timestamp = System.currentTimeMillis()
    )

    val outputStream = java.io.ByteArrayOutputStream()
    com.example.util.BackupUtil.exportFullBackup(
      context = context,
      cards = listOf(card),
      expenses = listOf(expense),
      outputStream = outputStream
    )

    val bytes = outputStream.toByteArray()
    org.junit.Assert.assertTrue("El archivo ZIP de respaldo no debe estar vacío", bytes.isNotEmpty())

    // Parsear el respaldo
    val inputStream = java.io.ByteArrayInputStream(bytes)
    val parsed = com.example.util.BackupUtil.parseBackup(context, inputStream)

    // Validar datos de tarjetas y consumos
    assertEquals(1, parsed.cards.size)
    assertEquals("Visa Infinite", parsed.cards[0].cardName)
    assertEquals(1, parsed.expenses.size)
    assertEquals("Supermercado Wong", parsed.expenses[0].establishment)

    // Validar que toda la configuración fue exportada y restaurada
    org.junit.Assert.assertNotNull(parsed.preferences)
    assertEquals("4321", parsed.preferences["master_pin"])
    assertEquals(true, parsed.preferences["biometrics_enabled"])
    assertEquals(true, parsed.preferences["payment_notifications_enabled"])
    assertEquals(5, (parsed.preferences["notification_lead_days"] as Number).toInt())
    assertEquals(true, parsed.preferences["gemini_ai_enabled"])
    assertEquals("test-api-key-gemini", parsed.preferences["gemini_api_key"])
    assertEquals("gemini-3.5-flash", parsed.preferences["gemini_model"])
  }
}
