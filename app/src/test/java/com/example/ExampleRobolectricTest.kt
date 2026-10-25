package com.example

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.CreditCardEntity
import com.example.data.model.ExpenseEntity
import com.example.data.repository.CreditiaRepository
import com.example.ui.viewmodel.CreditiaViewModel
import kotlinx.coroutines.test.runTest
import java.util.Locale
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

  @Test
  fun `test full backup export and restore includes all logos, custom cut dates, payment limits, and data`() = kotlinx.coroutines.runBlocking {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(app, com.example.data.local.AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val repo = com.example.data.repository.CreditiaRepository(db.creditCardDao(), db.expenseDao())
    val vm = com.example.ui.viewmodel.CreditiaViewModel(app, repo)
    val prefs = app.getSharedPreferences("creditia_secure_prefs", Context.MODE_PRIVATE)
    val logoPrefs = app.getSharedPreferences("creditia_establishment_logos", Context.MODE_PRIVATE)

    try {
      // 1. Crear fotos de comprobante y logo en disco
      val receiptsDir = com.example.util.ReceiptStorageUtil.getReceiptsDir(app)
      val dummyReceipt = java.io.File(receiptsDir, "receipt_test_full.jpg").apply {
        writeText("dummy receipt content")
      }

      val logosDir = com.example.util.LogoStorageUtil.getLogosDir(app)
      val dummyLogo = java.io.File(logosDir, "logo_starbucks_test.png").apply {
        writeText("dummy logo content")
      }

      // 2. Configurar tarjeta y consumo con logo y comprobante
      val card = com.example.data.model.CreditCardEntity(
        id = 15,
        bankName = "INTERBANK",
        cardName = "Visa Platinum",
        network = "VISA",
        tier = "PLATINUM",
        last4 = "7777",
        cutDay = 24,
        payDay = 18,
        payBusinessDays = 15,
        cardColorTheme = "emerald",
        creditLimit = 12000.0,
        isDefault = true
      )
      val cardId = vm.repository.insertCard(card)

      val expense = com.example.data.model.ExpenseEntity(
        id = 100,
        cardId = cardId,
        sequenceNumber = 1,
        establishment = "Starbucks",
        description = "Café Latte",
        amount = 18.50,
        dateString = "2026/10/20",
        billingPeriod = "Noviembre 2026",
        receiptUri = dummyReceipt.absolutePath,
        establishmentLogoUrl = dummyLogo.absolutePath,
        timestamp = System.currentTimeMillis()
      )
      vm.repository.insertExpense(expense)

      // 3. Configurar fechas personalizadas de corte y pago máximo en preferencias
      vm.setCustomCutDate(cardId, "Noviembre 2026", "2026/11/22")
      vm.setCustomPaymentLimitDate(cardId, "Noviembre 2026", "2026/12/16")
      vm.setCustomBillingCycleRange(cardId, "Noviembre 2026", "2026/10/25 - 2026/11/22")
      vm.rememberEstablishmentLogo("Starbucks", dummyLogo.absolutePath)

      assertEquals("2026/11/22", vm.getCustomCutDate(cardId, "Noviembre 2026"))
      assertEquals("2026/12/16", vm.getCustomPaymentLimitDate(cardId, "Noviembre 2026"))
      assertEquals(dummyLogo.absolutePath, vm.getRememberedLogo("Starbucks"))

      // 4. Exportar respaldo completo ZIP
      val outputStream = java.io.ByteArrayOutputStream()
      vm.exportFullBackup(outputStream)
      val zipBytes = outputStream.toByteArray()
      org.junit.Assert.assertTrue("El archivo ZIP de respaldo completo no debe estar vacío", zipBytes.isNotEmpty())

      // 5. Eliminar comprobantes, logos, base de datos y preferencias locales (simular instalación limpia)
      com.example.util.ReceiptStorageUtil.deleteAllReceiptFiles(app)
      com.example.util.LogoStorageUtil.deleteAllLogoFiles(app)
      vm.repository.clearAllData()
      logoPrefs.edit().clear().apply()
      prefs.edit().clear().apply()
      org.junit.Assert.assertFalse("El comprobante debe haberse borrado", dummyReceipt.exists())
      org.junit.Assert.assertFalse("El logo debe haberse borrado", dummyLogo.exists())

      // 6. Restaurar respaldo completo
      val inputStream = java.io.ByteArrayInputStream(zipBytes)
      val triple = vm.restoreFullBackup(inputStream)

      // Verificar cantidad de tarjetas y consumos restaurados
      assertEquals(1, triple.first)
      assertEquals(1, triple.second)
      org.junit.Assert.assertTrue("Fotos y logos deben estar incluidos", triple.third >= 2)

      // 7. Verificar que el archivo del comprobante y del logotipo se restauraron en almacenamiento interno
      val restoredReceipt = java.io.File(receiptsDir, "receipt_test_full.jpg")
      val restoredLogo = java.io.File(logosDir, "logo_starbucks_test.png")
      org.junit.Assert.assertTrue("El comprobante debe existir tras la restauración", restoredReceipt.exists())
      org.junit.Assert.assertTrue("El logotipo debe existir tras la restauración", restoredLogo.exists())

      // 8. Verificar que los consumos tienen los paths apuntando a los archivos restaurados
      val allRestoredExpenses = vm.repository.getAllExpensesList()
      assertEquals(1, allRestoredExpenses.size)
      val restoredExpense = allRestoredExpenses[0]
      assertEquals(restoredReceipt.absolutePath, restoredExpense.receiptUri)
      assertEquals(restoredLogo.absolutePath, restoredExpense.establishmentLogoUrl)

      // 9. Verificar que las fechas de corte, rangos y fechas máximas de pago personalizadas se restauraron
      val restoredCard = vm.repository.getAllCardsList()[0]
      assertEquals("2026/11/22", vm.getCustomCutDate(restoredCard.id, "Noviembre 2026"))
      assertEquals("2026/12/16", vm.getCustomPaymentLimitDate(restoredCard.id, "Noviembre 2026"))
      assertEquals("2026/10/25 - 2026/11/22", vm.getCustomBillingCycleRange(restoredCard.id, "Noviembre 2026"))

      // 10. Verificar catálogo de logos recordados restaurado
      assertEquals(restoredLogo.absolutePath, vm.getRememberedLogo("Starbucks"))
    } finally {
      db.close()
    }
  }

  @Test
  fun `test confirmClearDb deletes absolutely everything including logos, receipts, and dates`() = kotlinx.coroutines.runBlocking {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(app, com.example.data.local.AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val repo = com.example.data.repository.CreditiaRepository(db.creditCardDao(), db.expenseDao())
    val vm = com.example.ui.viewmodel.CreditiaViewModel(app, repo)

    try {
      // 1. Crear fotos de comprobante y logo
      val receiptsDir = com.example.util.ReceiptStorageUtil.getReceiptsDir(app)
      val receiptFile = java.io.File(receiptsDir, "receipt_to_delete.jpg").apply { writeText("receipt") }

      val logosDir = com.example.util.LogoStorageUtil.getLogosDir(app)
      val logoFile = java.io.File(logosDir, "logo_to_delete.png").apply { writeText("logo") }

      // 2. Insertar tarjeta, consumo, fechas personalizadas y logo recordado
      val card = com.example.data.model.CreditCardEntity(
        id = 30,
        bankName = "BBVA",
        cardName = "Visa Signature",
        network = "VISA",
        tier = "SIGNATURE",
        last4 = "1234",
        cutDay = 20,
        payDay = 15,
        payBusinessDays = 15,
        cardColorTheme = "blue",
        creditLimit = 5000.0,
        isDefault = true
      )
      val cardId = vm.repository.insertCard(card)
      vm.repository.insertExpense(com.example.data.model.ExpenseEntity(
        id = 300,
        cardId = cardId,
        sequenceNumber = 1,
        establishment = "Zara",
        description = "Ropa",
        amount = 120.0,
        dateString = "2026/10/10",
        billingPeriod = "Octubre 2026",
        receiptUri = receiptFile.absolutePath,
        establishmentLogoUrl = logoFile.absolutePath,
        timestamp = System.currentTimeMillis()
      ))
      vm.setCustomCutDate(cardId, "Octubre 2026", "2026/10/18")
      vm.setCustomPaymentLimitDate(cardId, "Octubre 2026", "2026/11/12")
      vm.setCustomBillingCycleRange(cardId, "Octubre 2026", "2026/09/21 - 2026/10/18")
      vm.rememberEstablishmentLogo("Zara", logoFile.absolutePath)

      org.junit.Assert.assertTrue(receiptFile.exists())
      org.junit.Assert.assertTrue(logoFile.exists())
      org.junit.Assert.assertNotNull(vm.getRememberedLogo("Zara"))
      org.junit.Assert.assertNotNull(vm.getCustomCutDate(cardId, "Octubre 2026"))

      // 3. Ejecutar Limpiar Base de Datos Local
      vm.clearLocalDatabase()

      // 4. Verificar que se borró absolutamente todo:
      // a. Comprobantes y logos en disco
      org.junit.Assert.assertFalse("El comprobante debe ser eliminado", receiptFile.exists())
      org.junit.Assert.assertFalse("La foto del logo debe ser eliminada", logoFile.exists())
      assertEquals(0, com.example.util.ReceiptStorageUtil.getReceiptsDir(app).listFiles()?.size ?: 0)
      assertEquals(0, com.example.util.LogoStorageUtil.getAllLogoFiles(app).size)

      // b. Base de datos Room
      assertEquals(0, vm.repository.getAllCardsList().size)
      assertEquals(0, vm.repository.getAllExpensesList().size)

      // c. Logos recordados
      org.junit.Assert.assertNull(vm.getRememberedLogo("Zara"))

      // d. Fechas personalizadas de corte, pago y rangos
      org.junit.Assert.assertNull(vm.getCustomCutDate(cardId, "Octubre 2026"))
      org.junit.Assert.assertNull(vm.getCustomPaymentLimitDate(cardId, "Octubre 2026"))
      org.junit.Assert.assertNull(vm.getCustomBillingCycleRange(cardId, "Octubre 2026"))

      // e. UI state reiniciado
      assertEquals(0, vm.uiState.value.cards.size)
      org.junit.Assert.assertNull(vm.uiState.value.selectedCard)
      org.junit.Assert.assertTrue(vm.uiState.value.customCutDates.isEmpty())
      org.junit.Assert.assertTrue(vm.uiState.value.customPaymentLimits.isEmpty())
      org.junit.Assert.assertTrue(vm.uiState.value.customCycleRanges.isEmpty())
    } finally {
      db.close()
    }
  }

  @Test
  fun test_summedLogosAndDeferredInstallmentsAreNotRememberedNorSuggested() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val repo = CreditiaRepository(db.creditCardDao(), db.expenseDao())
    val app = ApplicationProvider.getApplicationContext<Application>()
    val vm = CreditiaViewModel(app, repo)

    try {
      // 1. Crear tarjeta y 2 consumos individuales con logos independientes
      val cardId = repo.insertCard(
        CreditCardEntity(
          bankName = "Banco",
          cardName = "Visa Gold",
          network = "VISA",
          last4 = "1234",
          cutDay = 24,
          payDay = 15
        )
      )
      val exp1 = ExpenseEntity(
        sequenceNumber = 1,
        cardId = cardId,
        establishment = "Supermaxi",
        description = "Compras del mes",
        amount = 120.0,
        dateString = "2026/10/10",
        billingPeriod = "Octubre 2026",
        establishmentLogoUrl = "/data/user/0/com.aistudio.applet.shptdr/files/logos/supermaxi.png"
      )
      val exp2 = ExpenseEntity(
        sequenceNumber = 2,
        cardId = cardId,
        establishment = "Zara",
        description = "Ropa",
        amount = 80.0,
        dateString = "2026/10/12",
        billingPeriod = "Octubre 2026",
        establishmentLogoUrl = "/data/user/0/com.aistudio.applet.shptdr/files/logos/zara.png"
      )
      val id1 = repo.insertExpense(exp1)
      val id2 = repo.insertExpense(exp2)
      val insertedExp1 = exp1.copy(id = id1)
      val insertedExp2 = exp2.copy(id = id2)

      // 2. Ejecutar diferido unificado de ambos consumos
      val deferredResults = repo.deferMultipleExpenses(
        selectedExpenses = listOf(insertedExp1, insertedExp2),
        months = 3,
        monthlyInterestRate = 0.0,
        cutDay = 24
      )
      org.junit.Assert.assertTrue("Debe crear nota de crédito y cuotas", deferredResults.isNotEmpty())

      // 3. Verificar que getRememberedLogo nunca devuelve logos sumados ni comercios combinados
      org.junit.Assert.assertNull(
        "Un comercio combinado con '+' no debe tener logo recordado",
        vm.getRememberedLogo("Supermaxi + Zara")
      )
      org.junit.Assert.assertNull(
        "Una nota de crédito no debe tener logo recordado",
        vm.getRememberedLogo("Nota de Crédito - Supermaxi + Zara")
      )

      // 4. Intentar guardar manualmente un logo sumado con '|' debe ser rechazado
      vm.rememberEstablishmentLogo("Supermaxi + Zara", "/path/1.png|/path/2.png")
      org.junit.Assert.assertNull(
        "No debe almacenarse un logo sumado con '|'",
        vm.getRememberedLogo("Supermaxi + Zara")
      )

      vm.rememberEstablishmentLogo("Nuevo Comercio", "/path/a.png|/path/b.png")
      org.junit.Assert.assertNull(
        "No debe almacenarse un logo con múltiples rutas",
        vm.getRememberedLogo("Nuevo Comercio")
      )

      // 5. Verificar que el filtrado de knownEstablishments para sugerencias excluye cuotas diferidas y combinadas
      val allExpenses = repo.getAllExpensesList()
      val filteredEstablishments = allExpenses
        .filter { exp ->
          !exp.isDeferredInstallment() &&
          !exp.isDeferredExpense() &&
          !exp.isCreditNote() &&
          !exp.establishment.startsWith("Nota de Crédito", ignoreCase = true) &&
          !exp.establishment.contains(" + ") &&
          (exp.establishmentLogoUrl == null || !exp.establishmentLogoUrl.contains("|"))
        }
        .map { it.establishment.trim() }
        .filter { it.isNotBlank() }
        .distinctBy { it.lowercase(Locale.ROOT) }

      org.junit.Assert.assertTrue("Debe contener Supermaxi", filteredEstablishments.contains("Supermaxi"))
      org.junit.Assert.assertTrue("Debe contener Zara", filteredEstablishments.contains("Zara"))
      org.junit.Assert.assertFalse("No debe contener Supermaxi + Zara", filteredEstablishments.contains("Supermaxi + Zara"))
      org.junit.Assert.assertFalse("No debe contener notas de crédito", filteredEstablishments.any { it.startsWith("Nota de Crédito") })
    } finally {
      db.close()
    }
  }
  @Test
  fun testCustomCycleDatesRestoredCorrectlyAfterBackupAndRestore() = kotlinx.coroutines.runBlocking {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val db = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java).allowMainThreadQueries().build()
    AppDatabase.populateInitialData(db)
    val repo = CreditiaRepository(db.creditCardDao(), db.expenseDao())
    val vm = CreditiaViewModel(app, repo)

    // El usuario selecciona Octubre 2026 y modifica manualmente las fechas en la Tarjeta 1
    vm.selectPeriod("Octubre 2026")
    vm.setCustomBillingCycleRange(1, "Octubre 2026", "2026/09/20 - 2026/10/22")
    vm.setCustomCutDate(1, "Octubre 2026", "Jue 22/10/2026")
    vm.setCustomPaymentLimitDate(1, "Octubre 2026", "Lun 09/11/2026")

    // Exportar respaldo completo
    val out = java.io.ByteArrayOutputStream()
    vm.exportFullBackup(out)

    // Restaurar respaldo completo
    val inStream = java.io.ByteArrayInputStream(out.toByteArray())
    vm.restoreFullBackup(inStream)

    val currentCard = vm.uiState.value.selectedCard
    val currentPeriod = vm.uiState.value.selectedPeriod

    org.junit.Assert.assertEquals("El periodo seleccionado debe restaurarse a Octubre 2026", "Octubre 2026", currentPeriod)
    org.junit.Assert.assertEquals("La tarjeta seleccionada debe restaurarse a la Tarjeta 1", 1L, currentCard?.id)

    // Validar que las fechas modificadas manualmente NO se hayan cambiado a las por defecto
    org.junit.Assert.assertEquals(
      "El rango de facturación modificado debe restaurarse exactamente",
      "2026/09/20 - 2026/10/22",
      vm.getBillingCycleRange(currentCard?.id, currentPeriod, currentCard?.cutDay ?: 24)
    )
    org.junit.Assert.assertEquals(
      "La fecha de corte modificada debe restaurarse exactamente",
      "Jue 22/10/2026",
      vm.getEffectiveCutDateForPeriod(currentCard, currentPeriod)
    )
    org.junit.Assert.assertEquals(
      "La fecha máxima de pago modificada debe restaurarse exactamente",
      "Lun 09/11/2026",
      vm.getEffectivePaymentLimitDateForPeriod(currentCard, currentPeriod)
    )

    // Validar que el periodo personalizado esté presente en los periodos disponibles
    val available = vm.getAvailablePeriods(currentCard, repo.getAllExpensesList())
    org.junit.Assert.assertTrue("Octubre 2026 debe estar disponible en la lista de periodos", available.contains("Octubre 2026"))

    db.close()
  }
}


