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
            try {
                myDao.addReceipt(receipt)
                Log.d(TAG, "Receipt added: $receipt")
            } catch (e: Exception) {
                Log.e(TAG, "Error adding receipt: $e")
            }
        }
    }
    fun updateReceipt(name: String, date: String, totalAmount: Double, receiptId: String) {
        executor.execute {
            try {
                myDao.updateReceipt(name, date, totalAmount, receiptId)
                Log.d(TAG, "Receipt updated: ID=$receiptId, Name=$name, Date=$date, TotalAmount=$totalAmount")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating receipt: $e")
            }
        }
    }

    fun removeReceipt(id: String) {
        executor.execute {
            try {
                myDao.removeReceipt(id)
                Log.d(TAG, "Receipt removed with ID: $id")
            } catch (e: Exception) {
                Log.e(TAG, "Error removing receipt: $e")
            }
        }
    }

    fun fetchAllReceipts(): LiveData<List<Receipts>> = myDao.fetchAllReceipts()
    fun fetchReceiptsByName(name: String): LiveData<List<Receipts>> = myDao.fetchReceiptsByName(name)
    fun fetchReceiptByID(id: String): LiveData<Receipts?> = myDao.fetchReceiptByID(id)

    // Function to count the total number of receipts of year
    fun countTotalReceiptsOfYear(year: String): LiveData<Int> = myDao.countTotalReceiptsOfYear(year)
    // Function to fetch total amount of year
    fun fetchTotalAmountOfYear(year: String): LiveData<Double> = myDao.fetchTotalAmountOfYear(year)
    // Functions to get distinct years in the database
    fun fetchDistinctYears(): LiveData<List<String>> = myDao.fetchDistinctYears()
    // Function to fetch monthly expenditure
    fun fetchMonthlyExpenditure(year: String): LiveData<List<Pair<String?, Double>>> {
        return myDao.fetchMonthlyExpenditure(year).map { monthlyExpenditureList ->
            // Log the size of the fetched list
            Log.d("FetchMonthlyExpenditure", "Fetched data size: ${monthlyExpenditureList.size}")

            // Map the data to pairs and log each pair
            val mappedData = monthlyExpenditureList.map { monthlyExpenditure ->
                val pair = monthlyExpenditure.month to monthlyExpenditure.total
                Log.d("FetchMonthlyExpenditure", "Mapped pair: $pair")
                pair
            }

            // Return the mapped data
            mappedData
        }
    }

    fun fetchWeeklyExpenditure(startOfWeek: String, endOfWeek: String): LiveData<List<Pair<String?, Double>>> {
        return myDao.fetchWeeklyExpenditure(startOfWeek, endOfWeek).map { weeklyExpenditureList ->
            // Log the size of the fetched list
            Log.d("FetchWeeklyExpenditure", "Fetched data size: ${weeklyExpenditureList.size}")

            // Map the data to pairs and log each pair
            val mappedData = weeklyExpenditureList.map { weeklyExpenditure ->
                val pair = weeklyExpenditure.weekNumber to weeklyExpenditure.total
                Log.d("FetchWeeklyExpenditure", "Mapped pair: $pair")
                pair
            }

            // Return the mapped data
            mappedData
        }
    }


    // Function to count the total number of receipts from date to date
    fun countTotalReceipts(startOfWeek: String, endOfWeek: String): LiveData<Int> = myDao.countTotalReceipts(startOfWeek,endOfWeek)
    // Function to fetch total amount from date to date
    fun fetchTotalAmount(startOfWeek: String, endOfWeek: String): LiveData<Double> = myDao.fetchTotalAmount(startOfWeek,endOfWeek)
    // Functions to fetch daily expenditure
    fun fetchDailyExpenditure(startOfWeek: String, endOfWeek: String): LiveData<List<Pair<String?, Double>>> {
        return myDao.fetchDailyExpenditure(startOfWeek, endOfWeek).map { dailyExpenditureList ->
            // Log the size of the fetched list
            Log.d("FetchDailyExpenditure", "Fetched data size: ${dailyExpenditureList.size}")

            // Map the data to pairs and log each pair
            val mappedData = dailyExpenditureList.map { dailyExpenditure ->
                val pair = dailyExpenditure.date to dailyExpenditure.total
                Log.d("FetchDailyExpenditure", "Mapped pair: $pair")
                pair
            }

            // Return the mapped data
            mappedData
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
