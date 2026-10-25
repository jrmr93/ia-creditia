package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY sequenceNumber ASC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY sequenceNumber ASC")
    suspend fun getAllExpensesList(): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE cardId = :cardId ORDER BY sequenceNumber ASC")
    fun getExpensesByCard(cardId: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    suspend fun getExpenseById(id: Long): ExpenseEntity?

    @Query("SELECT * FROM expenses WHERE cardId = :cardId")
    suspend fun getExpensesListByCard(cardId: Long): List<ExpenseEntity>

    @Query("SELECT MAX(sequenceNumber) FROM expenses")
    suspend fun getMaxSequenceNumber(): Int?

    @Query("SELECT COUNT(*) FROM expenses")
    suspend fun getExpenseCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<ExpenseEntity>)

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Long)

    @Query("DELETE FROM expenses WHERE cardId = :cardId")
    suspend fun deleteExpensesByCard(cardId: Long)

    @Query("DELETE FROM expenses")
    suspend fun clearAllExpenses()

    @Query("SELECT * FROM expenses WHERE deferredGroupId = :groupId")
    suspend fun getExpensesByDeferredGroupId(groupId: String): List<ExpenseEntity>

    @Query("DELETE FROM expenses WHERE deferredGroupId = :groupId")
    suspend fun deleteExpensesByDeferredGroupId(groupId: String): Int

    @Query("SELECT * FROM expenses WHERE cardId = :cardId AND establishment = :establishment AND description LIKE :descriptionPattern")
    suspend fun findSimilarDeferredExpenses(cardId: Long, establishment: String, descriptionPattern: String): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE transactionGroupId = :groupId")
    suspend fun getExpensesByTransactionGroupId(groupId: String): List<ExpenseEntity>

    @Query("UPDATE expenses SET transactionGroupId = :groupId, transactionGroupName = :groupName WHERE id IN (:expenseIds)")
    suspend fun updateTransactionGroup(expenseIds: List<Long>, groupId: String?, groupName: String?)

    @Query("UPDATE expenses SET transactionGroupId = NULL, transactionGroupName = NULL WHERE transactionGroupId = :groupId")
    suspend fun clearTransactionGroup(groupId: String)

    @Update
    suspend fun updateExpenses(expenses: List<ExpenseEntity>)

    @Query("UPDATE expenses SET establishmentLogoUrl = :logoUrl WHERE deferredGroupId = :groupId")
    suspend fun updateLogoByDeferredGroupId(groupId: String, logoUrl: String?): Int

    @Query("UPDATE expenses SET establishmentLogoUrl = :logoUrl WHERE LOWER(establishment) = LOWER(:establishment)")
    suspend fun updateLogoByEstablishment(establishment: String, logoUrl: String?): Int
}
