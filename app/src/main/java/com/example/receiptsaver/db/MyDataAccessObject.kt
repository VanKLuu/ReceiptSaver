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
}