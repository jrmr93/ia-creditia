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

  @Test
  fun `test deferred installment reorganization when editing date on cuota 1`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.local.AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val repo = com.example.data.repository.CreditiaRepository(db.creditCardDao(), db.expenseDao())

    val card = com.example.data.model.CreditCardEntity(
      id = 1,
      bankName = "Pichincha",
      cardName = "Visa",
      network = "VISA",
      tier = "GOLD",
      last4 = "1234",
      cutDay = 24,
      payDay = 10
    )
    db.creditCardDao().insertCard(card)

    val groupId = "def_test_123"
    val cuota1 = com.example.data.model.ExpenseEntity(
      id = 101,
      sequenceNumber = 1,
      cardId = 1,
      establishment = "Tienda Hogar",
      description = "Refrigeradora (Cuota 1/3)",
      amount = 300.0,
      dateString = "2026/09/01",
      billingPeriod = "Septiembre 2026",
      deferredGroupId = groupId
    )
    val cuota2 = com.example.data.model.ExpenseEntity(
      id = 102,
      sequenceNumber = 2,
      cardId = 1,
      establishment = "Tienda Hogar",
      description = "Refrigeradora (Cuota 2/3)",
      amount = 300.0,
      dateString = "2026/10/01",
      billingPeriod = "Octubre 2026",
      deferredGroupId = groupId
    )
    val cuota3 = com.example.data.model.ExpenseEntity(
      id = 103,
      sequenceNumber = 3,
      cardId = 1,
      establishment = "Tienda Hogar",
      description = "Refrigeradora (Cuota 3/3)",
      amount = 300.0,
      dateString = "2026/11/01",
      billingPeriod = "Noviembre 2026",
      deferredGroupId = groupId
    )
    db.expenseDao().insertExpenses(listOf(cuota1, cuota2, cuota3))

    // Editar Cuota 1 para moverla del 1 de septiembre al 1 de octubre de 2026
    val editedCuota1 = cuota1.copy(
      dateString = "2026/10/01",
      establishment = "Tienda Hogar Central",
      description = "Refrigeradora Inverter (Cuota 1/3)"
    )

    val updatedCount = repo.updateExpenseWithDeferredSync(editedCuota1, cutDay = 24)
    assertEquals(3, updatedCount)

    val updated1 = db.expenseDao().getExpenseById(101)!!
    val updated2 = db.expenseDao().getExpenseById(102)!!
    val updated3 = db.expenseDao().getExpenseById(103)!!

    // Verificar Cuota 1
    assertEquals("2026/10/01", updated1.dateString)
    assertEquals("Octubre 2026", updated1.billingPeriod)
    assertEquals("Tienda Hogar Central", updated1.establishment)
    assertEquals("Refrigeradora Inverter (Cuota 1/3)", updated1.description)

    // Verificar Cuota 2 reorganizada
    assertEquals("2026/11/01", updated2.dateString)
    assertEquals("Noviembre 2026", updated2.billingPeriod)
    assertEquals("Tienda Hogar Central", updated2.establishment)
    assertEquals("Refrigeradora Inverter (Cuota 2/3)", updated2.description)

    // Verificar Cuota 3 reorganizada
    assertEquals("2026/12/01", updated3.dateString)
    assertEquals("Diciembre 2026", updated3.billingPeriod)
    assertEquals("Tienda Hogar Central", updated3.establishment)
    assertEquals("Refrigeradora Inverter (Cuota 3/3)", updated3.description)

    db.close()
  }

  @Test
  fun `test deferred installment reorganization when editing intermediate cuota 2`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.local.AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val repo = com.example.data.repository.CreditiaRepository(db.creditCardDao(), db.expenseDao())

    val card = com.example.data.model.CreditCardEntity(
      id = 1,
      bankName = "Pichincha",
      cardName = "Visa",
      network = "VISA",
      tier = "GOLD",
      last4 = "1234",
      cutDay = 24,
      payDay = 10
    )
    db.creditCardDao().insertCard(card)

    val groupId = "def_test_456"
    val cuota1 = com.example.data.model.ExpenseEntity(
      id = 201,
      sequenceNumber = 1,
      cardId = 1,
      establishment = "Sony Store",
      description = "PlayStation 5 (Cuota 1/3)",
      amount = 400.0,
      dateString = "2026/09/01",
      billingPeriod = "Septiembre 2026",
      deferredGroupId = groupId
    )
    val cuota2 = com.example.data.model.ExpenseEntity(
      id = 202,
      sequenceNumber = 2,
      cardId = 1,
      establishment = "Sony Store",
      description = "PlayStation 5 (Cuota 2/3)",
      amount = 400.0,
      dateString = "2026/10/01",
      billingPeriod = "Octubre 2026",
      deferredGroupId = groupId
    )
    val cuota3 = com.example.data.model.ExpenseEntity(
      id = 203,
      sequenceNumber = 3,
      cardId = 1,
      establishment = "Sony Store",
      description = "PlayStation 5 (Cuota 3/3)",
      amount = 400.0,
      dateString = "2026/11/01",
      billingPeriod = "Noviembre 2026",
      deferredGroupId = groupId
    )
    db.expenseDao().insertExpenses(listOf(cuota1, cuota2, cuota3))

    // Editar Cuota 2 para moverla del 1 de octubre al 1 de noviembre de 2026
    val editedCuota2 = cuota2.copy(
      dateString = "2026/11/01"
    )

    val updatedCount = repo.updateExpenseWithDeferredSync(editedCuota2, cutDay = 24)
    assertEquals(3, updatedCount)

    val updated1 = db.expenseDao().getExpenseById(201)!!
    val updated2 = db.expenseDao().getExpenseById(202)!!
    val updated3 = db.expenseDao().getExpenseById(203)!!

    // Cuota 1 ajustada a 2026/10/01
    assertEquals("2026/10/01", updated1.dateString)
    assertEquals("Octubre 2026", updated1.billingPeriod)

    // Cuota 2 fijada a 2026/11/01
    assertEquals("2026/11/01", updated2.dateString)
    assertEquals("Noviembre 2026", updated2.billingPeriod)

    // Cuota 3 ajustada a 2026/12/01
    assertEquals("2026/12/01", updated3.dateString)
    assertEquals("Diciembre 2026", updated3.billingPeriod)

    db.close()
  }

  @Test
  fun `test custom billing cycle range override and reset`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = com.example.ui.viewmodel.CreditiaViewModel(app)

    val cardId = 1L
    val period = "Octubre 2026"
    val cutDay = 24

    // Default calculado por fórmula (desde el día siguiente al corte anterior hasta el corte actual)
    val defaultRange = vm.getBillingCycleRange(cardId, period, cutDay)
    assertEquals("2026/09/25 - 2026/10/24", defaultRange)
    org.junit.Assert.assertNull(vm.getCustomBillingCycleRange(cardId, period))

    // Sobrescribir rango manualmente
    val customRange = "2026/09/20 - 2026/10/25"
    vm.setCustomBillingCycleRange(cardId, period, customRange)

    assertEquals(customRange, vm.getBillingCycleRange(cardId, period, cutDay))
    assertEquals(customRange, vm.getCustomBillingCycleRange(cardId, period))

    // Restablecer cálculo automático
    vm.setCustomBillingCycleRange(cardId, period, null)
    assertEquals(defaultRange, vm.getBillingCycleRange(cardId, period, cutDay))
    org.junit.Assert.assertNull(vm.getCustomBillingCycleRange(cardId, period))
  }

  @Test
  fun `test selection mode and multi select on expense toggle`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = com.example.ui.viewmodel.CreditiaViewModel(app)

    org.junit.Assert.assertFalse(vm.uiState.value.isSelectionMode)
    org.junit.Assert.assertTrue(vm.uiState.value.selectedExpenseIds.isEmpty())

    // Long press o toggle sobre registro 101 activa modo selección
    vm.toggleExpenseSelection(101L)
    org.junit.Assert.assertTrue(vm.uiState.value.isSelectionMode)
    org.junit.Assert.assertTrue(vm.uiState.value.selectedExpenseIds.contains(101L))

    // Seleccionar registro 102 adicional (multi-selección)
    vm.toggleExpenseSelection(102L)
    assertEquals(2, vm.uiState.value.selectedExpenseIds.size)
    org.junit.Assert.assertTrue(vm.uiState.value.selectedExpenseIds.contains(101L))
    org.junit.Assert.assertTrue(vm.uiState.value.selectedExpenseIds.contains(102L))

    // Deseleccionar 101L
    vm.toggleExpenseSelection(101L)
    assertEquals(1, vm.uiState.value.selectedExpenseIds.size)
    org.junit.Assert.assertTrue(vm.uiState.value.selectedExpenseIds.contains(102L))
    org.junit.Assert.assertTrue(vm.uiState.value.isSelectionMode)

    // Deseleccionar el último registro desactiva modo selección
    vm.toggleExpenseSelection(102L)
    org.junit.Assert.assertFalse(vm.uiState.value.isSelectionMode)
    org.junit.Assert.assertTrue(vm.uiState.value.selectedExpenseIds.isEmpty())
  }
}

