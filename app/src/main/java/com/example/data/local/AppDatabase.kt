package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.CreditCardEntity
import com.example.data.model.ExpenseEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [CreditCardEntity::class, ExpenseEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun creditCardDao(): CreditCardDao
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "creditia_vault.db"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database)
                    }
                }
            }
        }

        suspend fun populateInitialData(database: AppDatabase) {
            val cardDao = database.creditCardDao()
            val expenseDao = database.expenseDao()

            val card1 = CreditCardEntity(
                id = 1,
                bankName = "BANCO PICHINCHA",
                cardName = "Visa Signature Banco Pichincha",
                network = "VISA",
                tier = "SIGNATURE",
                last4 = "4892",
                cutDay = 24,
                payDay = 10,
                cardColorTheme = "blue",
                creditLimit = 5000.0,
                isDefault = true,
                payBusinessDays = 12
            )

            val card2 = CreditCardEntity(
                id = 2,
                bankName = "PACIFIC BANK CORP",
                cardName = "Mastercard Black",
                network = "MASTERCARD",
                tier = "",
                last4 = "9104",
                cutDay = 5,
                payDay = 21,
                cardColorTheme = "black",
                creditLimit = 7500.0,
                isDefault = false,
                payBusinessDays = 12
            )

            cardDao.insertCards(listOf(card1, card2))

            // Initial expenses matching the prototype in Image 3 & 7
            val initialExpenses = listOf(
                ExpenseEntity(
                    id = 1,
                    sequenceNumber = 1,
                    cardId = 1,
                    establishment = "Supermaxi Quicentro",
                    description = "Compras semanales despensa",
                    amount = 145.20,
                    dateString = "2026/10/18",
                    billingPeriod = "Octubre 2026",
                    timestamp = 1792330000000L
                ),
                ExpenseEntity(
                    id = 2,
                    sequenceNumber = 2,
                    cardId = 1,
                    establishment = "Gasolinera Primax",
                    description = "Tanque lleno combustible",
                    amount = 35.00,
                    dateString = "2026/10/12",
                    billingPeriod = "Octubre 2026",
                    timestamp = 1792243600000L
                ),
                ExpenseEntity(
                    id = 3,
                    sequenceNumber = 3,
                    cardId = 1,
                    establishment = "Restaurante Carmine",
                    description = "Cena aniversario trabajo",
                    amount = 82.50,
                    dateString = "2026/10/05",
                    billingPeriod = "Octubre 2026",
                    timestamp = 1792157200000L
                ),
                ExpenseEntity(
                    id = 4,
                    sequenceNumber = 4,
                    cardId = 1,
                    establishment = "Farmacias Fybeca",
                    description = "Medicamentos",
                    amount = 18.75,
                    dateString = "2026/09/28",
                    billingPeriod = "Octubre 2026",
                    timestamp = 1792070800000L
                ),
                ExpenseEntity(
                    id = 5,
                    sequenceNumber = 5,
                    cardId = 1,
                    establishment = "Netflix Suscripción",
                    description = "Plan mensual streaming",
                    amount = 10.00,
                    dateString = "2026/09/26",
                    billingPeriod = "Octubre 2026",
                    timestamp = 1791984400000L
                ),
                // Additional item to complete the $1,485.50 period sum shown in screenshot
                ExpenseEntity(
                    id = 6,
                    sequenceNumber = 6,
                    cardId = 1,
                    establishment = "Latam Airlines",
                    description = "Pasajes aéreos corporativos",
                    amount = 1194.05,
                    dateString = "2026/10/02",
                    billingPeriod = "Octubre 2026",
                    timestamp = 1791898000000L
                ),
                // Historical previous period items for Card 1 to reach $4,120.00 historical total
                ExpenseEntity(
                    id = 7,
                    sequenceNumber = 7,
                    cardId = 1,
                    establishment = "IKEA Muebles Hogar",
                    description = "Escritorio ergonómico",
                    amount = 1530.00,
                    dateString = "2026/08/15",
                    billingPeriod = "Septiembre 2026",
                    timestamp = 1790000000000L
                ),
                ExpenseEntity(
                    id = 8,
                    sequenceNumber = 8,
                    cardId = 1,
                    establishment = "Apple Store Online",
                    description = "Accesorios y cargadores",
                    amount = 1104.50,
                    dateString = "2026/07/20",
                    billingPeriod = "Agosto 2026",
                    timestamp = 1788000000000L
                ),
                // Card 2 expenses to match the $320.00 in current cycle
                ExpenseEntity(
                    id = 9,
                    sequenceNumber = 9,
                    cardId = 2,
                    establishment = "Zara Moda",
                    description = "Ropa formal oficina",
                    amount = 220.00,
                    dateString = "2026/10/04",
                    billingPeriod = "Octubre 2026",
                    timestamp = 1792100000000L
                ),
                ExpenseEntity(
                    id = 10,
                    sequenceNumber = 10,
                    cardId = 2,
                    establishment = "Amazon Prime",
                    description = "Suscripción anual y compras",
                    amount = 100.00,
                    dateString = "2026/09/29",
                    billingPeriod = "Octubre 2026",
                    timestamp = 1792000000000L
                )
            )

            expenseDao.insertExpenses(initialExpenses)
        }
    }
}
