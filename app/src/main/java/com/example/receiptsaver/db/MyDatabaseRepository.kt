package com.example.receiptsaver.db

import android.content.Context
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.room.Room
import java.util.*
import java.util.concurrent.Executors
import androidx.lifecycle.map
import kotlin.collections.map



private const val TAG = "MyDatabaseRepository"
private const val DATABASE_NAME = "ReceiptsSaver"

class MyDatabaseRepository(context: Context) {
    private val database: MyDatabase = Room.databaseBuilder(
        context.applicationContext,
        MyDatabase::class.java,
        DATABASE_NAME
    )
        .fallbackToDestructiveMigration()
        .build()

    // Data Access Object
    private val myDao = database.myDao()

    // Executor makes it easier to run stuff in a background thread
    private val executor = Executors.newSingleThreadExecutor()

    fun addReceipt(receipt: Receipts) {
        executor.execute {
            myDao.addReceipt(receipt)
        }
    }

    fun removeReceipt(id: String) {
        executor.execute {
            myDao.removeReceipt(id)
        }
    }

    fun fetchAllReceipts(): LiveData<List<Receipts>> = myDao.fetchAllReceipts()
    fun fetchReceiptsByName(name: String): LiveData<List<Receipts>> = myDao.fetchReceiptsByName(name)
    fun fetchReceiptByID(id: String): LiveData<Receipts?> = myDao.fetchReceiptByID(id)

    // New method to fetch total amount
    fun fetchTotalAmount(): LiveData<Double> = myDao.fetchTotalAmount()

    // New method to fetch monthly expenditure
    fun fetchMonthlyExpenditure(): LiveData<List<Pair<String?, Double>>> {
        return myDao.fetchMonthlyExpenditure().map { monthlyExpenditureList ->
            monthlyExpenditureList.map { monthlyExpenditure ->
                monthlyExpenditure.month to monthlyExpenditure.total
            }
        }
    }

    companion object {
        @Volatile
        private var instance: MyDatabaseRepository? = null

        fun getInstance(context: Context): MyDatabaseRepository {
            return instance ?: synchronized(this) {
                instance ?: MyDatabaseRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
