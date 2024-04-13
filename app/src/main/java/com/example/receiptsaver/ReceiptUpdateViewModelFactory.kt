package com.example.receiptsaver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.receiptsaver.db.MyDatabaseRepository

class ReceiptUpdateViewModelFactory(private val repository: MyDatabaseRepository) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReceiptUpdateViewModel::class.java)) {
            return ReceiptUpdateViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
