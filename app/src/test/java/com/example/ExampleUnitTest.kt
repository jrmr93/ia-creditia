package com.example

import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import com.example.data.repository.CreditiaRepository

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

  @Test
  fun testDeferredFilteringLogic() {
    val deferred1 = com.example.data.model.ExpenseEntity(
      id = 1,
      cardId = 1,
      sequenceNumber = 1,
      establishment = "Tienda Ripley",
      description = "Televisor Smart 4K (Cuota 1/6)",
      amount = 250.0,
      dateString = "2026/10/10",
      billingPeriod = "Octubre 2026",
      deferredGroupId = "group_123"
    )

    val regularExpense = com.example.data.model.ExpenseEntity(
      id = 2,
      cardId = 1,
      sequenceNumber = 2,
      establishment = "Starbucks Café",
      description = "Café Americano",
      amount = 15.0,
      dateString = "2026/10/12",
      billingPeriod = "Octubre 2026",
      deferredGroupId = null
    )

    val deferred2 = com.example.data.model.ExpenseEntity(
      id = 3,
      cardId = 1,
      sequenceNumber = 3,
      establishment = "Saga Falabella",
      description = "Zapatillas Nike (Cuota 2/3)",
      amount = 120.0,
      dateString = "2026/10/15",
      billingPeriod = "Octubre 2026",
      deferredGroupId = null
    )

    val allExpenses = listOf(deferred1, regularExpense, deferred2)

    val allCount = allExpenses.size
    val deferredOnly = allExpenses.filter { it.isDeferredExpense() }

    assertEquals(3, allCount)
    assertEquals(2, deferredOnly.size)
    assertTrue("deferred1 debe ser identificado como diferido", deferred1.isDeferredExpense())
    assertFalse("regularExpense no debe ser identificado como diferido", regularExpense.isDeferredExpense())
    assertTrue("deferred2 debe ser identificado como diferido por sufijo cuota", deferred2.isDeferredExpense())
  }

  @Test
  fun testPeriodFinancialSummaryWithPayments() {
    // Requisito 1 y 2: Consumos y Pagos a la tarjeta, y cálculo Pago Final = consumos - pagos
    val expense1 = com.example.data.model.ExpenseEntity(
      id = 1,
      cardId = 1,
      establishment = "Supermercado Wong",
      description = "Compras de víveres",
      amount = 300.0,
      dateString = "2026/10/05",
      billingPeriod = "Octubre 2026",
      transactionType = com.example.data.model.ExpenseEntity.TYPE_EXPENSE
    )
    val expense2 = com.example.data.model.ExpenseEntity(
      id = 2,
      cardId = 1,
      establishment = "Farmacia San Pablo",
      description = "Medicamentos",
      amount = 50.0,
      dateString = "2026/10/08",
      billingPeriod = "Octubre 2026",
      transactionType = com.example.data.model.ExpenseEntity.TYPE_EXPENSE
    )
    val payment1 = com.example.data.model.ExpenseEntity(
      id = 3,
      cardId = 1,
      establishment = "Pago Banco BCP",
      description = "Abono mensual a la tarjeta",
      amount = 120.0,
      dateString = "2026/10/12",
      billingPeriod = "Octubre 2026",
      transactionType = com.example.data.model.ExpenseEntity.TYPE_PAYMENT
    )

    val list = listOf(expense1, expense2, payment1)
    val totalExpenses = list.filter { it.isExpense() }.sumOf { it.amount }
    val totalPayments = list.filter { it.isPayment() }.sumOf { it.amount }
    val finalPaymentAmount = (totalExpenses - totalPayments).coerceAtLeast(0.0)

    assertEquals(350.0, totalExpenses, 0.001)
    assertEquals(120.0, totalPayments, 0.001)
    assertEquals(230.0, finalPaymentAmount, 0.001)
    assertTrue(expense1.isExpense())
    assertFalse(expense1.isPayment())
    assertTrue(payment1.isPayment())
    assertFalse(payment1.isExpense())
  }

  @Test
  fun testDeferralOffsetWithCreditNote() {
    // Requisito 3: Se conserva el consumo original diferido y se crea una Nota de Crédito
    // para anular el monto y que solo queden las cuotas calculadas
    val originalDeferred = com.example.data.model.ExpenseEntity(
      id = 10,
      cardId = 1,
      establishment = "Apple Store",
      description = "MacBook Air M3",
      amount = 1200.0,
      dateString = "2026/10/01",
      billingPeriod = "Octubre 2026",
      deferredGroupId = "group_def_001",
      transactionType = com.example.data.model.ExpenseEntity.TYPE_EXPENSE
    )

    val creditNote = com.example.data.model.ExpenseEntity(
      id = 11,
      cardId = 1,
      establishment = "Nota de Crédito - Apple Store",
      description = "Anulación por diferido a 3 cuotas",
      amount = 1200.0,
      dateString = "2026/10/01",
      billingPeriod = "Octubre 2026",
      deferredGroupId = "group_def_001",
      transactionType = com.example.data.model.ExpenseEntity.TYPE_CREDIT_NOTE
    )

    val cuota1 = com.example.data.model.ExpenseEntity(
      id = 12,
      cardId = 1,
      establishment = "Apple Store",
      description = "MacBook Air M3 (Cuota 1/3)",
      amount = 400.0,
      dateString = "2026/10/01",
      billingPeriod = "Octubre 2026",
      deferredGroupId = "group_def_001",
      transactionType = com.example.data.model.ExpenseEntity.TYPE_EXPENSE
    )

    val periodExpenses = listOf(originalDeferred, creditNote, cuota1)
    val totalExp = periodExpenses.filter { it.isExpense() }.sumOf { it.amount }
    val totalCredit = periodExpenses.filter { it.isCreditNote() }.sumOf { it.amount }
    val netPeriodPayment = totalExp - totalCredit

    assertEquals(1600.0, totalExp, 0.001)
    assertEquals(1200.0, totalCredit, 0.001)
    // El consumo original (1200) y la nota de crédito (1200) se anulan mutuamente, quedando solo la cuota activa (400)
    assertEquals(400.0, netPeriodPayment, 0.001)
    assertTrue(creditNote.isCreditNote())
  }

  @Test
  fun testTransactionGroupingModel() {
    // Requisito 5: Agrupación de consumos que pertenecen a una sola transacción
    val c1 = com.example.data.model.ExpenseEntity(
      id = 101,
      cardId = 1,
      establishment = "Cineplanet",
      description = "Entradas 2D",
      amount = 38.0,
      dateString = "2026/10/20",
      billingPeriod = "Octubre 2026",
      transactionGroupId = "trx_cine_cena",
      transactionGroupName = "Noche de Cine y Cena"
    )

    val c2 = com.example.data.model.ExpenseEntity(
      id = 102,
      cardId = 1,
      establishment = "Chili's Restaurant",
      description = "Cena familiar",
      amount = 95.0,
      dateString = "2026/10/20",
      billingPeriod = "Octubre 2026",
      transactionGroupId = "trx_cine_cena",
      transactionGroupName = "Noche de Cine y Cena"
    )

    val standalone = com.example.data.model.ExpenseEntity(
      id = 103,
      cardId = 1,
      establishment = "Gasolinera Repsol",
      description = "Combustible 95",
      amount = 70.0,
      dateString = "2026/10/21",
      billingPeriod = "Octubre 2026",
      transactionGroupId = null
    )

    val all = listOf(c1, c2, standalone)
    val grouped = all.filter { it.transactionGroupId == "trx_cine_cena" }
    assertEquals(2, grouped.size)
    assertEquals("Noche de Cine y Cena", grouped.first().transactionGroupName)
    assertEquals(133.0, grouped.sumOf { it.amount }, 0.001)
  }

  @Test
  fun testCreditNoteExcludedFromDeferredExpense() {
    // La Nota de Crédito tiene deferredGroupId pero es TYPE_CREDIT_NOTE, por lo que NO debe ser considerada
    // ni cuota ni gasto diferido, sino que se mantiene estrictamente como Nota de Crédito.
    val creditNote = com.example.data.model.ExpenseEntity(
      id = 50,
      cardId = 1,
      establishment = "Nota de Crédito - Zara",
      description = "Anulación por diferido a 3 cuotas",
      amount = 300.0,
      dateString = "2026/10/10",
      billingPeriod = "Octubre 2026",
      deferredGroupId = "def_zara_100",
      transactionType = com.example.data.model.ExpenseEntity.TYPE_CREDIT_NOTE
    )

    assertTrue("Debe identificarse como Nota de Crédito", creditNote.isCreditNote())
    assertFalse("No debe ser identificada como gasto diferido", creditNote.isDeferredExpense())
    assertFalse("No debe ser identificada como cuota diferida", creditNote.isDeferredInstallment())
    assertFalse("No debe ser identificada como pago", creditNote.isPayment())
    assertFalse("No debe ser identificada como consumo normal", creditNote.isExpense())
  }

  @Test
  fun testColorDifferentiationModel() {
    // Requisitos de color:
    // 1. Consumos: Rojo (0xFFEF4444)
    // 2. Pagos a la tarjeta: Verde (0xFF10B981)
    // 3. Registros por diferidos: Amarillo (0xFFF59E0B)
    // 4. Notas de crédito: Verde azulado / Teal (0xFF0D9488)

    val normalExpense = com.example.data.model.ExpenseEntity(
      id = 1, cardId = 1, establishment = "Supermercado", description = "Víveres",
      amount = 100.0, dateString = "2026/10/10", billingPeriod = "Octubre 2026",
      transactionType = com.example.data.model.ExpenseEntity.TYPE_EXPENSE
    )
    val payment = com.example.data.model.ExpenseEntity(
      id = 2, cardId = 1, establishment = "Pago BCP", description = "Abono",
      amount = 50.0, dateString = "2026/10/12", billingPeriod = "Octubre 2026",
      transactionType = com.example.data.model.ExpenseEntity.TYPE_PAYMENT
    )
    val deferredInstallment = com.example.data.model.ExpenseEntity(
      id = 3, cardId = 1, establishment = "Sony Store", description = "PlayStation 5 (Cuota 1/3)",
      amount = 200.0, dateString = "2026/10/15", billingPeriod = "Octubre 2026",
      deferredGroupId = "def_ps5", transactionType = com.example.data.model.ExpenseEntity.TYPE_EXPENSE
    )
    val creditNote = com.example.data.model.ExpenseEntity(
      id = 4, cardId = 1, establishment = "Nota de Crédito - Sony Store", description = "Anulación por diferido a 3 cuotas",
      amount = 600.0, dateString = "2026/10/15", billingPeriod = "Octubre 2026",
      deferredGroupId = "def_ps5", transactionType = com.example.data.model.ExpenseEntity.TYPE_CREDIT_NOTE
    )

    fun resolveColorName(exp: com.example.data.model.ExpenseEntity): String {
      return when {
        exp.isPayment() -> "VERDE"
        exp.isCreditNote() -> "TEAL"
        exp.isDeferredExpense() && !exp.isCreditNote() && !exp.isPayment() -> "AMARILLO"
        else -> "ROJO"
      }
    }

    assertEquals("ROJO", resolveColorName(normalExpense))
    assertEquals("VERDE", resolveColorName(payment))
    assertEquals("AMARILLO", resolveColorName(deferredInstallment))
    assertEquals("TEAL", resolveColorName(creditNote))
  }

  @Test
  fun testCreditNotePreservationDuringInstallmentReorganization() {
    // Simulación del comportamiento de actualización de fecha en CreditiaRepository:
    // Al cambiar la fecha de Cuota 1, solo se reorganizan las cuotas.
    // La Nota de Crédito se debe mantener intacta (mismo tipo, mismo monto, misma descripción).
    val creditNote = com.example.data.model.ExpenseEntity(
      id = 200,
      cardId = 1,
      establishment = "Nota de Crédito - Laptop",
      description = "Anulación por diferido a 3 cuotas",
      amount = 900.0,
      dateString = "2026/10/01",
      billingPeriod = "Octubre 2026",
      deferredGroupId = "def_lap_001",
      transactionType = com.example.data.model.ExpenseEntity.TYPE_CREDIT_NOTE
    )

    val cuota1 = com.example.data.model.ExpenseEntity(
      id = 201,
      cardId = 1,
      establishment = "Laptop",
      description = "Laptop (Cuota 1/3)",
      amount = 300.0,
      dateString = "2026/10/01",
      billingPeriod = "Octubre 2026",
      deferredGroupId = "def_lap_001",
      transactionType = com.example.data.model.ExpenseEntity.TYPE_EXPENSE
    )

    val cuota2 = com.example.data.model.ExpenseEntity(
      id = 202,
      cardId = 1,
      establishment = "Laptop",
      description = "Laptop (Cuota 2/3)",
      amount = 300.0,
      dateString = "2026/11/01",
      billingPeriod = "Noviembre 2026",
      deferredGroupId = "def_lap_001",
      transactionType = com.example.data.model.ExpenseEntity.TYPE_EXPENSE
    )

    val group = listOf(creditNote, cuota1, cuota2)

    val creditNotes = group.filter { it.isCreditNote() }
    val installments = group.filter { !it.isCreditNote() && it.isDeferredInstallment() }

    assertEquals(1, creditNotes.size)
    assertEquals(2, installments.size)

    // Se verifica que la nota de crédito mantenga su integridad
    val preservedNote = creditNotes.first()
    assertEquals(com.example.data.model.ExpenseEntity.TYPE_CREDIT_NOTE, preservedNote.transactionType)
    assertEquals(900.0, preservedNote.amount, 0.001)
    assertEquals("Nota de Crédito - Laptop", preservedNote.establishment)
    assertTrue("La nota de crédito nunca debe convertirse en cuota", !preservedNote.description.contains("(Cuota"))
  }

  @Test
  fun testCreditNoteDateChangesWhenDeferredInstallmentDateChanges() = kotlinx.coroutines.test.runTest {
    val fakeExpenseDao = FakeExpenseDao()
    val fakeCardDao = FakeCreditCardDao()
    val repository = com.example.data.repository.CreditiaRepository(fakeCardDao, fakeExpenseDao)

    // 1. Crear un consumo regular inicial
    val original = com.example.data.model.ExpenseEntity(
      id = 10,
      sequenceNumber = 1,
      cardId = 1,
      establishment = "Apple Store",
      description = "iPhone 15 Pro",
      amount = 1200.0,
      dateString = "2026/10/05",
      billingPeriod = "Octubre 2026",
      transactionType = com.example.data.model.ExpenseEntity.TYPE_EXPENSE
    )
    fakeExpenseDao.insertExpense(original)

    // 2. Diferir el consumo a 3 cuotas (esto genera nota de crédito y cuotas 1..3)
    val deferredResult = repository.deferExpense(
      originalExpense = original,
      months = 3,
      monthlyInterestRate = 0.0,
      cutDay = 24
    )
    assertEquals(5, deferredResult.size) // preserved + notaCredito + 3 cuotas

    val notaCredito = fakeExpenseDao.expenses.find { it.isCreditNote() }
    assertNotNull("Debe existir una nota de crédito al diferir un consumo", notaCredito)
    assertEquals("2026/10/05", notaCredito!!.dateString)
    assertEquals("Octubre 2026", notaCredito.billingPeriod)

    val cuota1 = fakeExpenseDao.expenses.find { it.description.contains("(Cuota 1/3)") }
    assertNotNull("Debe existir la cuota 1", cuota1)

    // 3. Usuario cambia la fecha de la Cuota 1 a Noviembre (2026/11/10)
    val updatedCuota1 = cuota1!!.copy(
      dateString = "2026/11/10"
    )
    repository.updateExpenseWithDeferredSync(updatedCuota1, cutDay = 24)

    // 4. Verificar que la Nota de Crédito ahora tiene la nueva fecha (2026/11/10) y nuevo billingPeriod (Noviembre 2026)
    val updatedNotaCredito = fakeExpenseDao.expenses.find { it.isCreditNote() }
    assertNotNull(updatedNotaCredito)
    assertEquals("2026/11/10", updatedNotaCredito!!.dateString)
    assertEquals("Noviembre 2026", updatedNotaCredito.billingPeriod)
    assertEquals(com.example.data.model.ExpenseEntity.TYPE_CREDIT_NOTE, updatedNotaCredito.transactionType)
    assertEquals(1200.0, updatedNotaCredito.amount, 0.001)

    // Verificar que el consumo original preservado conserva su fecha y periodo original
    val preservedOriginal = fakeExpenseDao.expenses.find { it.id == original.id }
    assertNotNull(preservedOriginal)
    assertEquals("2026/10/05", preservedOriginal!!.dateString)
    assertEquals("Octubre 2026", preservedOriginal.billingPeriod)

    // Verificar que las cuotas 1, 2, 3 se reorganizaron correlativamente
    val finalCuota1 = fakeExpenseDao.expenses.find { it.description.contains("(Cuota 1/3)") }
    val finalCuota2 = fakeExpenseDao.expenses.find { it.description.contains("(Cuota 2/3)") }
    val finalCuota3 = fakeExpenseDao.expenses.find { it.description.contains("(Cuota 3/3)") }

    assertEquals("2026/11/10", finalCuota1?.dateString)
    assertEquals("2026/12/10", finalCuota2?.dateString)
    assertEquals("2027/01/10", finalCuota3?.dateString)
  }

  @Test
  fun testDirectDeferredCreationDoesNotGenerateCreditNote() = kotlinx.coroutines.test.runTest {
    val fakeExpenseDao = FakeExpenseDao()
    val fakeCardDao = FakeCreditCardDao()
    val repository = com.example.data.repository.CreditiaRepository(fakeCardDao, fakeExpenseDao)

    // Registrar directamente un diferido desde "+ Registrar"
    val installments = repository.createDirectDeferred(
      cardId = 1,
      totalAmount = 600.0,
      establishment = "Tienda departamental",
      description = "Mueble de sala",
      startDateString = "2026/10/15",
      months = 3,
      monthlyInterestRate = 0.0,
      receiptUri = null,
      cutDay = 24
    )

    // Verificar que se crearon exactamente 3 registros
    assertEquals(3, installments.size)
    assertEquals(3, fakeExpenseDao.expenses.size)

    // Verificar que NO existe ninguna nota de crédito
    val creditNotes = fakeExpenseDao.expenses.filter { it.isCreditNote() || it.transactionType == com.example.data.model.ExpenseEntity.TYPE_CREDIT_NOTE }
    assertTrue("El diferido directo NO debe generar nota de crédito", creditNotes.isEmpty())

    // Verificar que los 3 registros son cuotas con color amarillo (isDeferredExpense() == true)
    fakeExpenseDao.expenses.forEach { exp ->
      assertEquals(com.example.data.model.ExpenseEntity.TYPE_EXPENSE, exp.transactionType)
      assertTrue("Cada cuota directa debe identificarse como diferido (amarillo)", exp.isDeferredExpense())
      assertTrue("Cada cuota directa debe ser una cuota mensual", exp.isDeferredInstallment())
      assertEquals(200.0, exp.amount, 0.001)
    }

    val cuota1 = fakeExpenseDao.expenses.find { it.description.contains("Cuota 1/3") }
    val cuota2 = fakeExpenseDao.expenses.find { it.description.contains("Cuota 2/3") }
    val cuota3 = fakeExpenseDao.expenses.find { it.description.contains("Cuota 3/3") }
    assertNotNull(cuota1)
    assertNotNull(cuota2)
    assertNotNull(cuota3)
  }

  @Test
  fun testGroupExpensesWithoutNameOrDate() = kotlinx.coroutines.test.runTest {
    val fakeExpenseDao = FakeExpenseDao()
    val fakeCardDao = FakeCreditCardDao()
    val repository = com.example.data.repository.CreditiaRepository(fakeCardDao, fakeExpenseDao)

    val exp1 = com.example.data.model.ExpenseEntity(
      id = 1,
      cardId = 1,
      amount = 50.0,
      establishment = "Restaurante",
      description = "Almuerzo",
      dateString = "2026/10/20",
      billingPeriod = "Octubre 2026"
    )
    val exp2 = com.example.data.model.ExpenseEntity(
      id = 2,
      cardId = 1,
      amount = 30.0,
      establishment = "Cine",
      description = "Pelicula",
      dateString = "2026/10/20",
      billingPeriod = "Octubre 2026"
    )
    fakeExpenseDao.insertExpense(exp1)
    fakeExpenseDao.insertExpense(exp2)

    // Agrupar sin nombre ni fecha personalizada (nombre = null)
    repository.groupExpenses(listOf(1L, 2L), null)

    val updated1 = fakeExpenseDao.getExpenseById(1L)
    val updated2 = fakeExpenseDao.getExpenseById(2L)

    assertNotNull(updated1?.transactionGroupId)
    assertEquals(updated1?.transactionGroupId, updated2?.transactionGroupId)
    assertNull("El nombre de la transacción agrupada debe ser nulo cuando no se especifica", updated1?.transactionGroupName)
    assertNull("El nombre de la transacción agrupada debe ser nulo cuando no se especifica", updated2?.transactionGroupName)
  }

  @Test
  fun testOriginalDeferredExpenseIsLockedAndGrayWhileInstallmentsExist() = kotlinx.coroutines.test.runTest {
    val fakeExpenseDao = FakeExpenseDao()
    val fakeCardDao = FakeCreditCardDao()
    val repository = com.example.data.repository.CreditiaRepository(fakeCardDao, fakeExpenseDao)

    val original = com.example.data.model.ExpenseEntity(
      id = 10,
      cardId = 1,
      amount = 300.0,
      establishment = "Computadoras SA",
      description = "Monitor 4K",
      dateString = "2026/10/12",
      billingPeriod = "Octubre 2026",
      transactionType = com.example.data.model.ExpenseEntity.TYPE_EXPENSE
    )
    fakeExpenseDao.insertExpense(original)

    // Diferir el consumo original a 3 meses
    repository.deferExpense(
      originalExpense = original,
      months = 3,
      monthlyInterestRate = 0.0,
      cutDay = 24
    )

    val allAfterDefer = fakeExpenseDao.expenses.toList()
    val preservedOriginal = allAfterDefer.find { it.id == 10L }
    assertNotNull(preservedOriginal)

    // 1. Debe identificarse como consumo original utilizado para diferir
    assertTrue(
      "El consumo original diferido debe marcar isOriginalDeferredExpense() == true",
      preservedOriginal!!.isOriginalDeferredExpense()
    )

    // 2. Mientras existan las cuotas diferidas activas, hasActiveDeferredInstallments debe ser true (Gris y Bloqueado)
    assertTrue(
      "Debe detectar cuotas diferidas activas para el consumo original",
      preservedOriginal.hasActiveDeferredInstallments(allAfterDefer)
    )

    // 3. Ni las cuotas (amarillas) ni las notas de crédito (teal) deben marcarse como originales anulados
    val cuotas = allAfterDefer.filter { it.isDeferredInstallment() }
    val creditNotes = allAfterDefer.filter { it.isCreditNote() }

    assertEquals(3, cuotas.size)
    assertEquals(1, creditNotes.size)

    cuotas.forEach { cuota ->
      assertFalse("Las cuotas no son el consumo original", cuota.isOriginalDeferredExpense())
    }
    creditNotes.forEach { nc ->
      assertFalse("La nota de crédito no es el consumo original", nc.isOriginalDeferredExpense())
    }

    // 4. Si se eliminan las cuotas diferidas en cascada, el original queda desbloqueado
    val someCuota = cuotas.first()
    repository.deleteExpenseWithCascade(someCuota)

    val allAfterCascade = fakeExpenseDao.expenses.toList()
    val unlinkedOriginal = allAfterCascade.find { it.id == 10L }
    assertNotNull(unlinkedOriginal)
    assertNull("El original debe tener deferredGroupId desvinculado al eliminar el diferido", unlinkedOriginal?.deferredGroupId)
    assertFalse("Ya no debe ser consumo original diferido", unlinkedOriginal!!.isOriginalDeferredExpense())
    assertFalse("Ya no tiene cuotas activas", unlinkedOriginal.hasActiveDeferredInstallments(allAfterCascade))
  }

  @Test
  fun testFilterOnlyDeferredExcludesOriginalBlockedExpenses() = kotlinx.coroutines.test.runTest {
    val fakeExpenseDao = FakeExpenseDao()
    val fakeCardDao = FakeCreditCardDao()
    val repository = com.example.data.repository.CreditiaRepository(fakeCardDao, fakeExpenseDao)

    val original = com.example.data.model.ExpenseEntity(
      id = 100,
      cardId = 1,
      amount = 450.0,
      establishment = "Tienda Electro",
      description = "Nevera",
      dateString = "2026/10/05",
      billingPeriod = "Octubre 2026"
    )
    fakeExpenseDao.insertExpense(original)

    // Diferir a 3 cuotas
    repository.deferExpense(
      originalExpense = original,
      months = 3,
      monthlyInterestRate = 0.0,
      cutDay = 24
    )

    val allExpenses = fakeExpenseDao.expenses.toList()
    val preservedOriginal = allExpenses.find { it.id == 100L }!!

    // El original bloqueado NO debe considerarse diferido en los filtros de solo diferidos
    assertFalse("El original utilizado para diferir no debe ser isDeferredExpense()", preservedOriginal.isDeferredExpense())
    assertTrue("El original debe ser isOriginalDeferredExpense()", preservedOriginal.isOriginalDeferredExpense())

    // Filtro 'Solo diferidos'
    val onlyDeferred = allExpenses.filter { it.isDeferredInstallment() && !it.isOriginalDeferredExpense() }
    assertEquals(3, onlyDeferred.size)
    assertFalse("La lista de solo diferidos no debe contener el consumo original bloqueado", onlyDeferred.any { it.id == 100L })
    assertFalse("La lista de solo diferidos no debe contener la nota de crédito", onlyDeferred.any { it.isCreditNote() })
    assertTrue("Todos los registros filtrados deben ser cuotas de diferido", onlyDeferred.all { it.isDeferredInstallment() })
  }

  @Test
  fun testCutDayIsStrictlyDeterminedByCardConfiguration() {
    // Regla 1:
    // El día de corte de la tarjeta está determinado en la configuración (cutDay).
    // No se ajusta por fines de semana:
    // Octubre 2026 con corte 24: Sábado 24 de octubre permanece exactamente día 24
    val cal24 = com.example.data.repository.CreditiaRepository.getCutCalendar(2026, Calendar.OCTOBER, 24)
    assertEquals(2026, cal24.get(Calendar.YEAR))
    assertEquals(Calendar.OCTOBER, cal24.get(Calendar.MONTH))
    assertEquals(24, cal24.get(Calendar.DAY_OF_MONTH))
    assertEquals(Calendar.SATURDAY, cal24.get(Calendar.DAY_OF_WEEK))
    assertEquals(24, com.example.data.repository.CreditiaRepository.getCutDay(2026, Calendar.OCTOBER, 24))

    // Octubre 2026 con corte 25: Domingo 25 de octubre permanece exactamente día 25
    val cal25 = com.example.data.repository.CreditiaRepository.getCutCalendar(2026, Calendar.OCTOBER, 25)
    assertEquals(2026, cal25.get(Calendar.YEAR))
    assertEquals(Calendar.OCTOBER, cal25.get(Calendar.MONTH))
    assertEquals(25, cal25.get(Calendar.DAY_OF_MONTH))
    assertEquals(Calendar.SUNDAY, cal25.get(Calendar.DAY_OF_WEEK))
    assertEquals(25, com.example.data.repository.CreditiaRepository.getCutDay(2026, Calendar.OCTOBER, 25))

    // Día laboral entre semana (ej. Miércoles 21 de octubre) permanece día 21
    val cal21 = com.example.data.repository.CreditiaRepository.getCutCalendar(2026, Calendar.OCTOBER, 21)
    assertEquals(21, cal21.get(Calendar.DAY_OF_MONTH))
    assertEquals(Calendar.WEDNESDAY, cal21.get(Calendar.DAY_OF_WEEK))
  }

  @Test
  fun testUserRule1October2026BillingCycleRangeAndCutDay() {
    val repository = com.example.data.repository.CreditiaRepository(FakeCreditCardDao(), FakeExpenseDao())

    // Regla 1:
    // El día de corte de la tarjeta está determinado en la configuración (cutDay = 24).
    // Para el periodo de octubre 2026 el corte es exactamente el 24 de octubre (sábado).
    val cutCalOct2026 = com.example.data.repository.CreditiaRepository.getCutCalendar(2026, Calendar.OCTOBER, 24)
    assertEquals(24, cutCalOct2026.get(Calendar.DAY_OF_MONTH))
    assertEquals(Calendar.SATURDAY, cutCalOct2026.get(Calendar.DAY_OF_WEEK))

    // El rango de fecha de facturación para octubre 2026 es desde 25 de septiembre hasta 24 de octubre
    val rangeOct2026 = repository.getBillingCycleRange("Octubre 2026", 24)
    assertEquals("2026/09/25 - 2026/10/24", rangeOct2026)

    // Comprobación de asignación de periodos para transacciones según corte el día 24:
    // 24 de septiembre -> Septiembre 2026
    assertEquals("Septiembre 2026", repository.calculateBillingPeriod("2026/09/24", 24))
    // 25 de septiembre -> Octubre 2026 (primer día del ciclo)
    assertEquals("Octubre 2026", repository.calculateBillingPeriod("2026/09/25", 24))
    // 23 de octubre -> Octubre 2026
    assertEquals("Octubre 2026", repository.calculateBillingPeriod("2026/10/23", 24))
    // 24 de octubre -> Octubre 2026 (día de corte)
    assertEquals("Octubre 2026", repository.calculateBillingPeriod("2026/10/24", 24))
    // 25 de octubre -> Noviembre 2026 (primer día del ciclo siguiente)
    assertEquals("Noviembre 2026", repository.calculateBillingPeriod("2026/10/25", 24))
  }

  @Test
  fun testUserRule2October2026MaxPaymentDateTwelveBusinessDays() {
    // Regla 2:
    // Para el cálculo de fecha máxima de pago se debe contabilizar el número de días
    // establecido en la configuración de la tarjeta tomando en cuenta desde el día de corte.
    // Para el periodo de octubre 2026 la fecha de corte es 24 de octubre (sábado).
    // Si son 12 días configurados, se cuentan 12 días después del 24 de octubre solo contabilizando
    // días de lunes a viernes (días hábiles), resultando exactamente en el martes 10 de noviembre de 2026.
    val cutCalOct2026 = com.example.data.repository.CreditiaRepository.getCutCalendar(2026, Calendar.OCTOBER, 24)
    val payCalOct2026 = com.example.data.repository.CreditiaRepository.calculateMaxPaymentCalendar(cutCalOct2026, 12)

    assertEquals(2026, payCalOct2026.get(Calendar.YEAR))
    assertEquals(Calendar.NOVEMBER, payCalOct2026.get(Calendar.MONTH))
    assertEquals(10, payCalOct2026.get(Calendar.DAY_OF_MONTH))
    assertEquals(Calendar.TUESDAY, payCalOct2026.get(Calendar.DAY_OF_WEEK))
  }

  @Test
  fun testUserRule1AndRule2AllPeriodsConsistency() {
    val repository = com.example.data.repository.CreditiaRepository(FakeCreditCardDao(), FakeExpenseDao())

    // Periodo Septiembre 2026: corte 24 de septiembre de 2026 (jueves)
    val cutCalSep = com.example.data.repository.CreditiaRepository.getCutCalendar(2026, Calendar.SEPTEMBER, 24)
    assertEquals(24, cutCalSep.get(Calendar.DAY_OF_MONTH))
    assertEquals(Calendar.THURSDAY, cutCalSep.get(Calendar.DAY_OF_WEEK))
    assertEquals("2026/08/25 - 2026/09/24", repository.getBillingCycleRange("Septiembre 2026", 24))

    // 12 días hábiles (lunes a viernes) tras el 24 de septiembre -> Lunes 12 de octubre de 2026
    val payCalSep = com.example.data.repository.CreditiaRepository.calculateMaxPaymentCalendar(cutCalSep, 12)
    assertEquals(12, payCalSep.get(Calendar.DAY_OF_MONTH))
    assertEquals(Calendar.OCTOBER, payCalSep.get(Calendar.MONTH))
    assertEquals(Calendar.MONDAY, payCalSep.get(Calendar.DAY_OF_WEEK))

    // Periodo Enero 2027: corte 24 de enero de 2027 (domingo, sin desplazamiento)
    val cutCalJan = com.example.data.repository.CreditiaRepository.getCutCalendar(2027, Calendar.JANUARY, 24)
    assertEquals(24, cutCalJan.get(Calendar.DAY_OF_MONTH))
    assertEquals(Calendar.SUNDAY, cutCalJan.get(Calendar.DAY_OF_WEEK))
    assertEquals("2026/12/25 - 2027/01/24", repository.getBillingCycleRange("Enero 2027", 24))

    // 12 días hábiles tras el domingo 24 de enero de 2027 -> Martes 9 de febrero de 2027
    val payCalJan = com.example.data.repository.CreditiaRepository.calculateMaxPaymentCalendar(cutCalJan, 12)
    assertEquals(9, payCalJan.get(Calendar.DAY_OF_MONTH))
    assertEquals(Calendar.FEBRUARY, payCalJan.get(Calendar.MONTH))
    assertEquals(Calendar.TUESDAY, payCalJan.get(Calendar.DAY_OF_WEEK))
  }

  @Test
  fun testLogoCandidateDataModel() {
    val candidate = com.example.util.LogoCandidate(
      url = "https://www.google.com/s2/favicons?domain=supermaxi.com&sz=256",
      title = "Supermaxi",
      domain = "supermaxi.com",
      source = "Google HD (256px)"
    )
    assertEquals("https://www.google.com/s2/favicons?domain=supermaxi.com&sz=256", candidate.url)
    assertEquals("Supermaxi", candidate.title)
    assertEquals("supermaxi.com", candidate.domain)
    assertEquals("Google HD (256px)", candidate.source)
  }

  @Test
  fun testSearchLogoCandidatesReturnsResultsForAnt() = kotlinx.coroutines.test.runTest {
    val candidates = com.example.util.GeminiService.searchLogoCandidates(
      apiKey = "",
      modelName = "",
      query = "ANT"
    )
    assertTrue("Debe encontrar imágenes para 'ANT'", candidates.isNotEmpty())
    assertTrue("Los candidatos deben contener URLs válidas", candidates.all { it.url.startsWith("http") })
  }

  @Test
  fun testReceiptFilePhysicalDeletion() {
    val tempFile = java.io.File.createTempFile("test_receipt", ".jpg")
    tempFile.writeText("sample image content")
    assertTrue(tempFile.exists())

    // Eliminar mediante ReceiptStorageUtil
    com.example.util.ReceiptStorageUtil.deleteReceiptFile(tempFile.absolutePath)
    assertFalse("El archivo físico debe eliminarse del almacenamiento", tempFile.exists())
  }

  @Test
  fun testEstablishmentLogoPersistenceAndEntity() {
    val expense = com.example.data.model.ExpenseEntity(
      id = 1,
      cardId = 1,
      establishment = "Supermaxi",
      description = "Compras de víveres",
      amount = 85.50,
      dateString = "2026/10/12",
      billingPeriod = "Octubre 2026",
      establishmentLogoUrl = "/data/user/0/com.example/files/logos/logo_supermaxi.png"
    )

    assertEquals("/data/user/0/com.example/files/logos/logo_supermaxi.png", expense.establishmentLogoUrl)

    // Verificar actualización y persistencia en entidad
    val updated = expense.copy(establishmentLogoUrl = "/data/user/0/com.example/files/logos/logo_nuevo.png")
    assertEquals("/data/user/0/com.example/files/logos/logo_nuevo.png", updated.establishmentLogoUrl)

    // Verificar eliminación de logo
    val cleared = updated.copy(establishmentLogoUrl = null)
    assertNull(cleared.establishmentLogoUrl)
  }

  @Test
  fun testDeferExpenseInheritsAndStoresLogo() = kotlinx.coroutines.runBlocking {
    val expenseDao = FakeExpenseDao()
    val cardDao = FakeCreditCardDao()
    val repo = com.example.data.repository.CreditiaRepository(cardDao, expenseDao)

    val original = com.example.data.model.ExpenseEntity(
      id = 101,
      cardId = 1,
      sequenceNumber = 1,
      establishment = "Supermaxi Mall",
      description = "Compra de supermercado",
      amount = 300.0,
      dateString = "2026/10/15",
      billingPeriod = "Octubre 2026",
      establishmentLogoUrl = "/data/user/0/com.example/files/logos/logo_supermaxi.png"
    )
    expenseDao.insertExpense(original)

    val results = repo.deferExpense(
      originalExpense = original,
      months = 3,
      monthlyInterestRate = 0.0,
      cutDay = 24
    )

    // results: [preservedOriginal, notaCredito, cuota1, cuota2, cuota3]
    assertEquals(5, results.size)
    val notaCredito = results.find { it.isCreditNote() }
    assertNotNull(notaCredito)
    assertEquals("/data/user/0/com.example/files/logos/logo_supermaxi.png", notaCredito?.establishmentLogoUrl)

    val installments = results.filter { it.isDeferredInstallment() && !it.isCreditNote() && it.id != original.id }
    assertEquals(3, installments.size)
    installments.forEach { inst ->
      assertEquals("Cada cuota debe heredar el logo del consumo diferido", "/data/user/0/com.example/files/logos/logo_supermaxi.png", inst.establishmentLogoUrl)
    }
  }

  @Test
  fun testDeferMultipleExpensesInheritsAllLogos() = kotlinx.coroutines.runBlocking {
    val expenseDao = FakeExpenseDao()
    val cardDao = FakeCreditCardDao()
    val repo = com.example.data.repository.CreditiaRepository(cardDao, expenseDao)

    val exp1 = com.example.data.model.ExpenseEntity(
      id = 201,
      cardId = 1,
      sequenceNumber = 1,
      establishment = "Zara",
      description = "Ropa",
      amount = 150.0,
      dateString = "2026/10/10",
      billingPeriod = "Octubre 2026",
      establishmentLogoUrl = "/data/user/0/com.example/files/logos/logo_zara.png"
    )
    val exp2 = com.example.data.model.ExpenseEntity(
      id = 202,
      cardId = 1,
      sequenceNumber = 2,
      establishment = "Pull & Bear",
      description = "Zapatos",
      amount = 100.0,
      dateString = "2026/10/12",
      billingPeriod = "Octubre 2026",
      establishmentLogoUrl = "/data/user/0/com.example/files/logos/logo_pullbear.png"
    )
    expenseDao.insertExpense(exp1)
    expenseDao.insertExpense(exp2)

    val results = repo.deferMultipleExpenses(
      selectedExpenses = listOf(exp1, exp2),
      months = 2,
      monthlyInterestRate = 0.0,
      cutDay = 24
    )

    // Nota de crédito y 2 cuotas
    assertEquals(3, results.size)
    val expectedCombined = "/data/user/0/com.example/files/logos/logo_zara.png|/data/user/0/com.example/files/logos/logo_pullbear.png"

    val notaCredito = results.find { it.isCreditNote() }
    assertNotNull(notaCredito)
    assertEquals(expectedCombined, notaCredito?.establishmentLogoUrl)
    assertEquals(listOf("/data/user/0/com.example/files/logos/logo_zara.png", "/data/user/0/com.example/files/logos/logo_pullbear.png"), notaCredito?.getLogoUrls())

    val installments = results.filter { !it.isCreditNote() }
    assertEquals(2, installments.size)
    installments.forEach { inst ->
      assertEquals("Las cuotas deben contener todos los logos de los consumos diferidos", expectedCombined, inst.establishmentLogoUrl)
      assertEquals(2, inst.getLogoUrls().size)
    }

    // Los consumos originales conservan su propio logo respectivo
    val savedExp1 = expenseDao.getExpenseById(201)
    val savedExp2 = expenseDao.getExpenseById(202)
    assertEquals("/data/user/0/com.example/files/logos/logo_zara.png", savedExp1?.establishmentLogoUrl)
    assertEquals("/data/user/0/com.example/files/logos/logo_pullbear.png", savedExp2?.establishmentLogoUrl)
  }

  @Test
  fun testSubsequentLogoUploadPropagatesToDeferredInstallmentsAndCreditNote() = kotlinx.coroutines.runBlocking {
    val expenseDao = FakeExpenseDao()
    val cardDao = FakeCreditCardDao()
    val repo = com.example.data.repository.CreditiaRepository(cardDao, expenseDao)

    // Dos consumos seleccionados para diferir: exp1 tiene logo, exp2 inicialmente NO tiene logo
    val exp1 = com.example.data.model.ExpenseEntity(
      id = 401,
      cardId = 1,
      sequenceNumber = 1,
      establishment = "Supermaxi",
      description = "Víveres",
      amount = 120.0,
      dateString = "2026/10/05",
      billingPeriod = "Octubre 2026",
      establishmentLogoUrl = "/data/user/0/com.example/files/logos/logo_supermaxi.png"
    )
    val exp2 = com.example.data.model.ExpenseEntity(
      id = 402,
      cardId = 1,
      sequenceNumber = 2,
      establishment = "Kywi",
      description = "Herramientas",
      amount = 80.0,
      dateString = "2026/10/08",
      billingPeriod = "Octubre 2026",
      establishmentLogoUrl = null // SIN LOGO INICIALMENTE
    )
    expenseDao.insertExpense(exp1)
    expenseDao.insertExpense(exp2)

    val results = repo.deferMultipleExpenses(
      selectedExpenses = listOf(exp1, exp2),
      months = 3,
      monthlyInterestRate = 0.0,
      cutDay = 24
    )
    val groupId = results.first().deferredGroupId ?: "def_test_group"

    // Inicialmente solo está el logo de Supermaxi en cuotas y nota de crédito
    val initialNc = expenseDao.getExpensesByDeferredGroupId(groupId).find { it.isCreditNote() }
    assertEquals("/data/user/0/com.example/files/logos/logo_supermaxi.png", initialNc?.establishmentLogoUrl)

    // POSTERIORMENTE se sube/asigna logo al consumo de Kywi
    val savedKywi = expenseDao.getExpenseById(402)!!
    val updatedKywiWithLogo = savedKywi.copy(
      establishmentLogoUrl = "/data/user/0/com.example/files/logos/logo_kywi.png"
    )
    repo.updateExpense(updatedKywiWithLogo)

    // Sincronizar grupo diferido
    repo.syncLogosForDeferredGroup(groupId)

    // Verificar que los registros consecuentes (cuotas y nota de crédito) ahora contienen TODOS los logos
    val updatedGroup = expenseDao.getExpensesByDeferredGroupId(groupId)
    val expectedCombined = "/data/user/0/com.example/files/logos/logo_supermaxi.png|/data/user/0/com.example/files/logos/logo_kywi.png"

    val updatedNc = updatedGroup.find { it.isCreditNote() }
    assertNotNull(updatedNc)
    assertEquals("La nota de crédito debe reflejar todos los logos", expectedCombined, updatedNc?.establishmentLogoUrl)
    assertEquals(2, updatedNc?.getLogoUrls()?.size)

    val updatedInstallments = updatedGroup.filter { it.isDeferredInstallment() }
    assertEquals(3, updatedInstallments.size)
    updatedInstallments.forEach { inst ->
      assertEquals("Cada cuota debe reflejar todos los logos tras la subida posterior", expectedCombined, inst.establishmentLogoUrl)
      assertEquals(listOf("/data/user/0/com.example/files/logos/logo_supermaxi.png", "/data/user/0/com.example/files/logos/logo_kywi.png"), inst.getLogoUrls())
    }
  }

  @Test
  fun testExpenseEntityGetLogoUrlsHelper() {
    val single = com.example.data.model.ExpenseEntity(
      cardId = 1, establishment = "Test", description = "Test", amount = 10.0, dateString = "2026/10/10", billingPeriod = "Octubre 2026",
      establishmentLogoUrl = "/logos/logo1.png"
    )
    assertEquals(listOf("/logos/logo1.png"), single.getLogoUrls())

    val multiple = com.example.data.model.ExpenseEntity(
      cardId = 1, establishment = "Test", description = "Test", amount = 10.0, dateString = "2026/10/10", billingPeriod = "Octubre 2026",
      establishmentLogoUrl = "/logos/logo1.png | /logos/logo2.png| /logos/logo1.png "
    )
    assertEquals(listOf("/logos/logo1.png", "/logos/logo2.png"), multiple.getLogoUrls())

    val empty = com.example.data.model.ExpenseEntity(
      cardId = 1, establishment = "Test", description = "Test", amount = 10.0, dateString = "2026/10/10", billingPeriod = "Octubre 2026",
      establishmentLogoUrl = null
    )
    assertTrue(empty.getLogoUrls().isEmpty())
  }

  @Test
  fun testDirectDeferredWithLogo() = kotlinx.coroutines.runBlocking {
    val expenseDao = FakeExpenseDao()
    val cardDao = FakeCreditCardDao()
    val repo = com.example.data.repository.CreditiaRepository(cardDao, expenseDao)

    val installments = repo.createDirectDeferred(
      cardId = 1,
      totalAmount = 600.0,
      establishment = "Sony Store",
      description = "PlayStation 5",
      startDateString = "2026/10/05",
      months = 6,
      monthlyInterestRate = 1.5,
      receiptUri = null,
      cutDay = 24,
      establishmentLogoUrl = "/data/user/0/com.example/files/logos/logo_sony.png"
    )

    assertEquals(6, installments.size)
    installments.forEach { inst ->
      assertEquals("/data/user/0/com.example/files/logos/logo_sony.png", inst.establishmentLogoUrl)
      assertTrue(inst.deferredGroupId?.startsWith("def_direct_") == true)
    }
  }

  @Test
  fun testDeferredGroupLogoBidirectionalSync() = kotlinx.coroutines.runBlocking {
    val expenseDao = FakeExpenseDao()
    val cardDao = FakeCreditCardDao()
    val repo = com.example.data.repository.CreditiaRepository(cardDao, expenseDao)

    // Consumo creado inicialmente SIN logo
    val original = com.example.data.model.ExpenseEntity(
      id = 301,
      cardId = 1,
      sequenceNumber = 1,
      establishment = "Marathon Sports",
      description = "Camiseta",
      amount = 80.0,
      dateString = "2026/10/10",
      billingPeriod = "Octubre 2026",
      establishmentLogoUrl = null
    )
    expenseDao.insertExpense(original)

    val deferredResult = repo.deferExpense(
      originalExpense = original,
      months = 3,
      monthlyInterestRate = 0.0,
      cutDay = 24
    )
    val groupId = deferredResult.first().deferredGroupId ?: "def_test"

    // Posteriormente se asigna un logo al consumo original que se utilizó para crear el diferido
    val updatedOriginalWithLogo = original.copy(
      deferredGroupId = groupId,
      establishmentLogoUrl = "/data/user/0/com.example/files/logos/logo_marathon.png"
    )
    repo.updateExpenseWithDeferredSync(updatedOriginalWithLogo, 24)

    // Verificar que los registros consecuentes (nota de crédito y cuotas) se actualizaron automáticamente
    val allInGroup = expenseDao.getExpensesByDeferredGroupId(groupId)
    assertEquals(5, allInGroup.size)
    allInGroup.forEach { item ->
      assertEquals(
        "El registro ${item.description} del grupo diferido debe sincronizar su logo",
        "/data/user/0/com.example/files/logos/logo_marathon.png",
        item.establishmentLogoUrl
      )
    }

    // Probar también actualización directa por grupo diferido
    repo.updateLogoForDeferredGroup(groupId, "/data/user/0/com.example/files/logos/logo_marathon_v2.png")
    val updatedAll = expenseDao.getExpensesByDeferredGroupId(groupId)
    updatedAll.forEach { item ->
      if (item.id == original.id) {
        // El consumo original que se utilizó para crear el diferido nunca altera su logo
        assertEquals("/data/user/0/com.example/files/logos/logo_marathon.png", item.establishmentLogoUrl)
      } else {
        // Los registros creados por consecuencia (nota de crédito y cuotas) sí actualizan su logo
        assertEquals("/data/user/0/com.example/files/logos/logo_marathon_v2.png", item.establishmentLogoUrl)
      }
    }
  }

  @Test
  fun testCannotDeferDeferredInstallmentsAndPreservesOriginalIntegrity() = kotlinx.coroutines.runBlocking {
    val expenseDao = FakeExpenseDao()
    val cardDao = FakeCreditCardDao()
    val repo = com.example.data.repository.CreditiaRepository(cardDao, expenseDao)

    val exp1 = com.example.data.model.ExpenseEntity(
      id = 401,
      cardId = 1,
      sequenceNumber = 1,
      establishment = "Supermaxi",
      description = "Compras de víveres",
      amount = 120.0,
      dateString = "2026/10/02",
      billingPeriod = "Octubre 2026",
      establishmentLogoUrl = "/logos/supermaxi.png"
    )
    val exp2 = com.example.data.model.ExpenseEntity(
      id = 402,
      cardId = 1,
      sequenceNumber = 2,
      establishment = "Marathon",
      description = "Zapatos deportivos",
      amount = 180.0,
      dateString = "2026/10/05",
      billingPeriod = "Octubre 2026",
      establishmentLogoUrl = "/logos/marathon.png"
    )
    expenseDao.insertExpense(exp1)
    expenseDao.insertExpense(exp2)

    // 1. Diferir múltiples registros
    val deferredRes = repo.deferMultipleExpenses(
      selectedExpenses = listOf(exp1, exp2),
      months = 3,
      monthlyInterestRate = 0.0,
      cutDay = 24,
      startDateString = "2026/10/05"
    )
    assertTrue(deferredRes.isNotEmpty())

    // 2. Verificar que los registros originales conservaron sus fechas y logos individuales
    val savedExp1 = expenseDao.getExpenseById(401)
    val savedExp2 = expenseDao.getExpenseById(402)
    assertNotNull(savedExp1)
    assertNotNull(savedExp2)
    assertEquals("2026/10/02", savedExp1!!.dateString)
    assertEquals("/logos/supermaxi.png", savedExp1.establishmentLogoUrl)
    assertEquals("2026/10/05", savedExp2!!.dateString)
    assertEquals("/logos/marathon.png", savedExp2.establishmentLogoUrl)

    // 3. Verificar que los nuevos registros (cuotas y nota de crédito) unieron los logos
    val cuotas = deferredRes.filter { it.isDeferredInstallment() }
    val notaCredito = deferredRes.find { it.isCreditNote() }
    assertNotNull(notaCredito)
    assertEquals("/logos/supermaxi.png|/logos/marathon.png", notaCredito!!.establishmentLogoUrl)
    cuotas.forEach { cuota ->
      assertEquals("/logos/supermaxi.png|/logos/marathon.png", cuota.establishmentLogoUrl)
    }

    // 4. Validar que no se puede diferir un registro de tipo cuota diferida
    var threwException = false
    try {
      repo.deferExpense(cuotas.first(), 3, 0.0, 24)
    } catch (e: IllegalArgumentException) {
      threwException = true
      assertEquals("No se pueden diferir registros de tipo cuota diferida", e.message)
    }
    assertTrue("Debe lanzar excepción al intentar diferir una cuota diferida individual", threwException)

    threwException = false
    try {
      repo.deferMultipleExpenses(listOf(cuotas.first()), 3, 0.0, 24)
    } catch (e: IllegalArgumentException) {
      threwException = true
      assertEquals("No se pueden diferir registros de tipo cuota diferida", e.message)
    }
    assertTrue("Debe lanzar excepción al intentar diferir una lista que contiene cuotas diferidas", threwException)

    // 5. Validar que no se puede diferir un registro de tipo pago a tarjeta
    val payment = com.example.data.model.ExpenseEntity(
      id = 405,
      cardId = 1,
      sequenceNumber = 5,
      establishment = "Transferencia Banco Pichincha",
      description = "Pago de tarjeta",
      amount = 100.0,
      dateString = "2026/10/12",
      billingPeriod = "Octubre 2026",
      transactionType = com.example.data.model.ExpenseEntity.TYPE_PAYMENT
    )
    expenseDao.insertExpense(payment)

    var paymentThrew = false
    try {
      repo.deferExpense(payment, 3, 0.0, 24)
    } catch (e: IllegalArgumentException) {
      paymentThrew = true
      assertEquals("No se pueden diferir registros de tipo pago a tarjeta", e.message)
    }
    assertTrue("Debe lanzar excepción al intentar diferir un pago a tarjeta individual", paymentThrew)

    paymentThrew = false
    try {
      repo.deferMultipleExpenses(listOf(payment), 3, 0.0, 24)
    } catch (e: IllegalArgumentException) {
      paymentThrew = true
      assertEquals("No se pueden diferir registros de tipo pago a tarjeta", e.message)
    }
    assertTrue("Debe lanzar excepción al intentar diferir una lista con pago a tarjeta", paymentThrew)
  }

  @Test
  fun testEstablishmentNormalizationAndAutocomplete() {
    fun normalize(name: String): String = name.trim().lowercase(Locale.ROOT)

    assertEquals("supermaxi", normalize("  Supermaxi "))
    assertEquals("kfc quicentro", normalize("KFC Quicentro   "))
    assertEquals("banco pichincha", normalize("Banco PICHINCHA"))

    val knownEstablishments = listOf(
      "Supermaxi",
      "Supercines",
      "KFC",
      "Mi Comisariato",
      "Fybeca",
      "Supermercados Santa María"
    )

    fun getSuggestions(input: String, list: List<String>): List<String> {
      val trimmed = input.trim()
      if (trimmed.isEmpty()) return emptyList()
      return list.filter {
        it.contains(trimmed, ignoreCase = true) && !it.equals(trimmed, ignoreCase = true)
      }.take(4)
    }

    val sups = getSuggestions("super", knownEstablishments)
    assertEquals(3, sups.size)
    assertTrue(sups.contains("Supermaxi"))
    assertTrue(sups.contains("Supercines"))
    assertTrue(sups.contains("Supermercados Santa María"))

    val exactKfc = getSuggestions("kfc", knownEstablishments)
    assertTrue("Si coincide exactamente, no se sugiere a sí mismo", exactKfc.isEmpty())

    val comi = getSuggestions("comis", knownEstablishments)
    assertEquals(1, comi.size)
    assertEquals("Mi Comisariato", comi.first())
  }
}

class FakeExpenseDao : com.example.data.local.ExpenseDao {
  val expenses = mutableListOf<com.example.data.model.ExpenseEntity>()
  private var idCounter = 1L

  override fun getAllExpenses(): kotlinx.coroutines.flow.Flow<List<com.example.data.model.ExpenseEntity>> =
    kotlinx.coroutines.flow.flowOf(expenses)

  override suspend fun getAllExpensesList(): List<com.example.data.model.ExpenseEntity> = expenses

  override fun getExpensesByCard(cardId: Long): kotlinx.coroutines.flow.Flow<List<com.example.data.model.ExpenseEntity>> =
    kotlinx.coroutines.flow.flowOf(expenses.filter { it.cardId == cardId })

  override suspend fun getExpenseById(id: Long): com.example.data.model.ExpenseEntity? =
    expenses.find { it.id == id }

  override suspend fun getExpensesListByCard(cardId: Long): List<com.example.data.model.ExpenseEntity> =
    expenses.filter { it.cardId == cardId }

  override suspend fun getMaxSequenceNumber(): Int? =
    expenses.maxOfOrNull { it.sequenceNumber }

  override suspend fun getExpenseCount(): Int = expenses.size

  override suspend fun insertExpense(expense: com.example.data.model.ExpenseEntity): Long {
    val newId = if (expense.id == 0L) idCounter++ else expense.id
    val toSave = expense.copy(id = newId)
    expenses.removeAll { it.id == newId }
    expenses.add(toSave)
    return newId
  }

  override suspend fun insertExpenses(expensesList: List<com.example.data.model.ExpenseEntity>) {
    expensesList.forEach { insertExpense(it) }
  }

  override suspend fun updateExpense(expense: com.example.data.model.ExpenseEntity) {
    val idx = expenses.indexOfFirst { it.id == expense.id }
    if (idx != -1) {
      expenses[idx] = expense
    } else {
      expenses.add(expense)
    }
  }

  override suspend fun deleteExpense(expense: com.example.data.model.ExpenseEntity) {
    expenses.removeAll { it.id == expense.id }
  }

  override suspend fun deleteExpenseById(id: Long) {
    expenses.removeAll { it.id == id }
  }

  override suspend fun deleteExpensesByCard(cardId: Long) {
    expenses.removeAll { it.cardId == cardId }
  }

  override suspend fun clearAllExpenses() {
    expenses.clear()
  }

  override suspend fun getExpensesByDeferredGroupId(groupId: String): List<com.example.data.model.ExpenseEntity> =
    expenses.filter { it.deferredGroupId == groupId }

  override suspend fun deleteExpensesByDeferredGroupId(groupId: String): Int {
    val count = expenses.count { it.deferredGroupId == groupId }
    expenses.removeAll { it.deferredGroupId == groupId }
    return count
  }

  override suspend fun findSimilarDeferredExpenses(cardId: Long, establishment: String, descriptionPattern: String): List<com.example.data.model.ExpenseEntity> =
    expenses.filter { it.cardId == cardId && it.establishment == establishment }

  override suspend fun getExpensesByTransactionGroupId(groupId: String): List<com.example.data.model.ExpenseEntity> =
    expenses.filter { it.transactionGroupId == groupId }

  override suspend fun updateTransactionGroup(expenseIds: List<Long>, groupId: String?, groupName: String?) {
    expenses.indices.forEach { i ->
      if (expenses[i].id in expenseIds) {
        expenses[i] = expenses[i].copy(transactionGroupId = groupId, transactionGroupName = groupName)
      }
    }
  }

  override suspend fun clearTransactionGroup(groupId: String) {
    expenses.indices.forEach { i ->
      if (expenses[i].transactionGroupId == groupId) {
        expenses[i] = expenses[i].copy(transactionGroupId = null, transactionGroupName = null)
      }
    }
  }

  override suspend fun updateExpenses(expensesList: List<com.example.data.model.ExpenseEntity>) {
    expensesList.forEach { updateExpense(it) }
  }

  override suspend fun updateLogoByDeferredGroupId(groupId: String, logoUrl: String?): Int {
    var count = 0
    expenses.indices.forEach { i ->
      if (expenses[i].deferredGroupId == groupId) {
        expenses[i] = expenses[i].copy(establishmentLogoUrl = logoUrl)
        count++
      }
    }
    return count
  }

  override suspend fun updateLogoByEstablishment(establishment: String, logoUrl: String?): Int {
    var count = 0
    expenses.indices.forEach { i ->
      if (expenses[i].establishment.equals(establishment, ignoreCase = true)) {
        expenses[i] = expenses[i].copy(establishmentLogoUrl = logoUrl)
        count++
      }
    }
    return count
  }
}

class FakeCreditCardDao : com.example.data.local.CreditCardDao {
  val cards = mutableListOf<com.example.data.model.CreditCardEntity>()
  private var idCounter = 1L

  override fun getAllCards(): kotlinx.coroutines.flow.Flow<List<com.example.data.model.CreditCardEntity>> =
    kotlinx.coroutines.flow.flowOf(cards)

  override suspend fun getAllCardsList(): List<com.example.data.model.CreditCardEntity> = cards

  override suspend fun getCardById(id: Long): com.example.data.model.CreditCardEntity? =
    cards.find { it.id == id }

  override suspend fun insertCard(card: com.example.data.model.CreditCardEntity): Long {
    val newId = if (card.id == 0L) idCounter++ else card.id
    val toSave = card.copy(id = newId)
    cards.removeAll { it.id == newId }
    cards.add(toSave)
    return newId
  }

  override suspend fun insertCards(cardsList: List<com.example.data.model.CreditCardEntity>) {
    cardsList.forEach { insertCard(it) }
  }

  override suspend fun updateCard(card: com.example.data.model.CreditCardEntity) {
    val idx = cards.indexOfFirst { it.id == card.id }
    if (idx != -1) cards[idx] = card else cards.add(card)
  }

  override suspend fun deleteCard(card: com.example.data.model.CreditCardEntity) {
    cards.removeAll { it.id == card.id }
  }

  override suspend fun deleteCardById(id: Long) {
    cards.removeAll { it.id == id }
  }

  override suspend fun clearAllCards() {
    cards.clear()
  }

  override suspend fun getCardCount(): Int = cards.size
}

class ImageSearchTest {
  @Test
  fun testSearchCandidates() {
    val client = okhttp3.OkHttpClient.Builder()
      .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
      .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
      .build()

    // 1. Test Wikimedia Commons API
    try {
      val url = "https://commons.wikimedia.org/w/api.php?action=query&generator=search&gsrnamespace=6&gsrsearch=ANT+Ecuador+logo&gsrlimit=10&prop=imageinfo&iiprop=url|thumburl|dimensions&iiurlwidth=400&format=json"
      val req = okhttp3.Request.Builder()
        .url(url)
        .header("User-Agent", "CreditiaApp/1.0 (Android; contacto@creditia.app)")
        .build()
      val resp = client.newCall(req).execute()
      val body = resp.body?.string() ?: ""
      println("Wikimedia code: " + resp.code + " length: " + body.length)
      val urlMatches = Regex(""""url":"([^"]+)"""").findAll(body).toList()
      val thumbMatches = Regex(""""thumburl":"([^"]+)"""").findAll(body).toList()
      val titleMatches = Regex(""""title":"File:([^"]+)"""").findAll(body).toList()
      println("Wikimedia found files: " + urlMatches.size)
      for (i in 0 until urlMatches.size.coerceAtMost(5)) {
        println("  Wiki #${i + 1}: ${titleMatches.getOrNull(i)?.groupValues?.get(1)} -> full: ${urlMatches[i].groupValues[1]} | thumb: ${thumbMatches.getOrNull(i)?.groupValues?.get(1)}")
      }
    } catch (e: Exception) {
      println("Wikimedia error: " + e.message)
    }

    // Test domain resolution for Ecuadorian establishments
    val testBrands = listOf(
      "ANT" to "ant.gob.ec",
      "SRI" to "sri.gob.ec",
      "Supermaxi" to "supermaxi.com",
      "Banco Pichincha" to "bancopichincha.com",
      "Tia" to "tia.com.ec",
      "Fybeca" to "fybeca.com",
      "CNT" to "cnt.com.ec",
      "De Prati" to "deprati.com.ec"
    )

    for ((brand, domain) in testBrands) {
      val gUrl = "https://www.google.com/s2/favicons?domain=$domain&sz=512"
      val req = okhttp3.Request.Builder().url(gUrl).build()
      val resp = client.newCall(req).execute()
      println("Brand $brand ($domain) -> Google S2 code: ${resp.code} size: ${resp.body?.bytes()?.size}")
    }
  }

  @Test
  fun testDeferMultipleExpensesWithCustomStartDate() = kotlinx.coroutines.runBlocking {
    val expenseDao = FakeExpenseDao()
    val cardDao = FakeCreditCardDao()
    val repo = com.example.data.repository.CreditiaRepository(cardDao, expenseDao)

    val exp1 = com.example.data.model.ExpenseEntity(
      id = 501,
      cardId = 1,
      sequenceNumber = 1,
      establishment = "Restaurante",
      description = "Cena",
      amount = 100.0,
      dateString = "2026/10/05",
      billingPeriod = "Octubre 2026"
    )
    val exp2 = com.example.data.model.ExpenseEntity(
      id = 502,
      cardId = 1,
      sequenceNumber = 2,
      establishment = "Cine",
      description = "Entradas",
      amount = 50.0,
      dateString = "2026/10/08",
      billingPeriod = "Octubre 2026"
    )
    expenseDao.insertExpense(exp1)
    expenseDao.insertExpense(exp2)

    val customStartDate = "2026/11/15"
    val results = repo.deferMultipleExpenses(
      selectedExpenses = listOf(exp1, exp2),
      months = 3,
      monthlyInterestRate = 0.0,
      cutDay = 24,
      startDateString = customStartDate
    )

    // 1 Nota de crédito + 3 cuotas
    assertEquals(4, results.size)
    val notaCredito = results.find { it.isCreditNote() }
    assertNotNull(notaCredito)
    assertEquals(customStartDate, notaCredito?.dateString)

    val installments = results.filter { !it.isCreditNote() }
    assertEquals(3, installments.size)
    assertEquals(50.0, installments[0].amount, 0.01)

    // 1. Verificar que los consumos originales conservan sus fechas intactas
    val savedExp1 = expenseDao.getExpenseById(501)
    val savedExp2 = expenseDao.getExpenseById(502)
    assertEquals("2026/10/05", savedExp1?.dateString)
    assertEquals("2026/10/08", savedExp2?.dateString)

    // 2. Verificar que están bloqueados
    val allExpenses = expenseDao.expenses.toList()
    assertTrue(savedExp1?.isLockedByActiveDeferred(allExpenses) == true)
    assertTrue(savedExp2?.isLockedByActiveDeferred(allExpenses) == true)
    assertTrue(notaCredito?.isLockedByActiveDeferred(allExpenses) == true)

    // 3. Verificar que la nota de crédito no se puede modificar ni eliminar directamente
    val dbNc = expenseDao.expenses.find { it.isCreditNote() }!!
    repo.updateExpense(dbNc.copy(amount = 999.0))
    val checkNc = expenseDao.getExpenseById(dbNc.id)
    assertEquals("El monto de la nota de crédito no debe modificarse", 150.0, checkNc?.amount)

    val deleteNcResult = repo.deleteExpenseWithCascade(dbNc)
    assertEquals(0, deleteNcResult)
    val checkNcAfterDelete = expenseDao.getExpenseById(dbNc.id)
    assertNotNull("La nota de crédito no debe eliminarse directamente", checkNcAfterDelete)

    // 4. Modificar fecha de la primera cuota diferida:
    // Debe cambiar la fecha de las cuotas y de la nota de crédito, pero NO de los consumos originales
    val cuota1 = expenseDao.expenses.find { it.description.contains("(Cuota 1/3)") }!!
    val updatedInstallment = cuota1.copy(dateString = "2026/12/01")
    repo.updateExpenseWithDeferredSync(updatedInstallment, cutDay = 24)

    val afterExp1 = expenseDao.getExpenseById(501)
    val afterExp2 = expenseDao.getExpenseById(502)
    assertEquals("Los consumos originales deben conservar su fecha original", "2026/10/05", afterExp1?.dateString)
    assertEquals("Los consumos originales deben conservar su fecha original", "2026/10/08", afterExp2?.dateString)

    val afterNc = expenseDao.getExpenseById(dbNc.id)
    assertEquals("La nota de crédito sí debe actualizar su fecha", "2026/12/01", afterNc?.dateString)
  }

  @Test
  fun testCustomBillingPeriodValidationAndAssignment() {
    val repo = CreditiaRepository(FakeCreditCardDao(), FakeExpenseDao())

    // 1. Cálculo por defecto (sin rangos personalizados) con cutDay = 24
    assertEquals("Octubre 2026", repo.calculateBillingPeriod("2026/10/20", 24))
    assertEquals("Octubre 2026", repo.calculateBillingPeriod("2026/10/24", 24))
    assertEquals("Noviembre 2026", repo.calculateBillingPeriod("2026/10/25", 24))

    // 2. Modificación manual: Octubre 2026 se recorta para terminar el 20 de octubre (2026/09/25 - 2026/10/20)
    val customRangesOct20 = mapOf("Octubre 2026" to "2026/09/25 - 2026/10/20")
    // Dentro de Octubre modificado
    assertEquals("Octubre 2026", repo.calculateBillingPeriod("2026/10/20", 24, customRangesOct20))
    // Fuera de Octubre modificado -> DEBE pasar a Noviembre 2026
    assertEquals("Noviembre 2026", repo.calculateBillingPeriod("2026/10/21", 24, customRangesOct20))
    assertEquals("Noviembre 2026", repo.calculateBillingPeriod("2026/10/22", 24, customRangesOct20))
    assertEquals("Noviembre 2026", repo.calculateBillingPeriod("2026/10/24", 24, customRangesOct20))

    // El rango calculado para Noviembre 2026 debe iniciar el 21 de octubre (día siguiente al corte anterior)
    val novRange = repo.getBillingCycleRange("Noviembre 2026", 24, customRangesOct20)
    assertEquals("2026/10/21 - 2026/11/24", novRange)

    // 3. Modificación manual: Octubre 2026 se extiende hasta el 28 de octubre
    val customRangesOct28 = mapOf("Octubre 2026" to "2026/09/25 - 2026/10/28")
    // El 26 de octubre normalmente sería Noviembre (26 > 24), pero al extenderse Octubre pertenece a Octubre
    assertEquals("Octubre 2026", repo.calculateBillingPeriod("2026/10/26", 24, customRangesOct28))
    assertEquals("Octubre 2026", repo.calculateBillingPeriod("2026/10/28", 24, customRangesOct28))
    assertEquals("Noviembre 2026", repo.calculateBillingPeriod("2026/10/29", 24, customRangesOct28))

    // 4. Modificación manual: Octubre 2026 inicia antes (2026/09/20 - 2026/10/24)
    val customRangesOctEarly = mapOf("Octubre 2026" to "2026/09/20 - 2026/10/24")
    // 22 de septiembre normalmente es Septiembre, pero al iniciar Octubre el 20 pertenece a Octubre
    assertEquals("Octubre 2026", repo.calculateBillingPeriod("2026/09/22", 24, customRangesOctEarly))
    assertEquals("Septiembre 2026", repo.calculateBillingPeriod("2026/09/19", 24, customRangesOctEarly))
  }
}

