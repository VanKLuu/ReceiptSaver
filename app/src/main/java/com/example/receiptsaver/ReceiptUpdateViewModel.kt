package com.example.receiptsaver

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.receiptsaver.db.MyDatabaseRepository
import com.example.receiptsaver.db.Receipts
private const val TAG = "ReceiptUpdateViewModel"

class ReceiptUpdateViewModel(private val repository: MyDatabaseRepository) : ViewModel() {

    fun fetchReceiptById(id: String): LiveData<Receipts?> {
        return repository.fetchReceiptByID(id)
    }

    fun updateReceipt(name: String, date: String, totalAmount: Double, id: String) {
        try {
            repository.updateReceipt(name, date, totalAmount, id)
            Log.d(TAG, "Receipt updated: ID=$id, Name=$name, Date=$date, TotalAmount=$totalAmount")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating receipt: $e")
        }
    }
}