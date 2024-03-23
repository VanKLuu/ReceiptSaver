package com.example.receiptsaver.db

import androidx.lifecycle.LiveData
import androidx.room.*
import java.util.UUID

@Dao // Data Access Object
interface MyDataAccessObject
{

    @Query("SELECT * FROM RECEIPTS ORDER BY date DESC")
    fun fetchAllReceipts(): LiveData<List<Receipts>>

    @Query("SELECT * FROM Receipts WHERE name LIKE '%' || :name || '%'")
    fun fetchReceiptsByName(name: String): LiveData<List<Receipts>>

    @Query("SELECT * FROM RECEIPTS WHERE id=(:id)")
    fun fetchReceiptByID(id: String): LiveData<Receipts?>

    @Insert
    fun addReceipt(receipt: Receipts)

    @Update
    fun updateReceipt(receipt: Receipts)

    @Query("DELETE FROM RECEIPTS WHERE id=(:id)")
    fun removeReceipt(id: String)

    // Query to fetch the total amount from the receipts
    @Query("SELECT SUM(totalAmount) FROM RECEIPTS")
    fun fetchTotalAmount(): LiveData<Double>

    // Query to fetch monthly expenditure data
    @Query("SELECT substr(date, 1, 2) AS month, SUM(totalAmount) AS total FROM RECEIPTS GROUP BY substr(date, 1, 2)")
    fun fetchMonthlyExpenditure(): LiveData<List<MonthlyExpenditure>>

    // Query to count the total number of receipts
    @Query("SELECT COUNT(*) FROM RECEIPTS")
    fun countTotalReceipts(): LiveData<Int>
}