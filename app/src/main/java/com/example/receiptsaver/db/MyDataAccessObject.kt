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

    @Query("UPDATE RECEIPTS SET name = :name, date = :date, totalAmount = :totalAmount WHERE id = :receiptId")
    fun updateReceipt(name: String, date: String, totalAmount: Double, receiptId: String)


    @Query("DELETE FROM RECEIPTS WHERE id=(:id)")
    fun removeReceipt(id: String)

    // Query to fetch the total amount from the receipts of year
    @Query("SELECT SUM(totalAmount) FROM RECEIPTS WHERE substr(date, -4) = :year")
    fun fetchTotalAmountOfYear(year: String): LiveData<Double>

    // Query to fetch monthly expenditure data
    @Query("SELECT substr(date, 1, 2) AS month, SUM(totalAmount) AS total FROM RECEIPTS WHERE substr(date, -4) = :year GROUP BY substr(date, 1, 2)")
    fun fetchMonthlyExpenditure(year: String): LiveData<List<MonthlyExpenditure>>

    // Query to count the total number of receipts of the year
    @Query("SELECT COUNT(*) FROM RECEIPTS WHERE substr(date, -4) = :year")
    fun countTotalReceiptsOfYear(year: String): LiveData<Int>

    // Query to get distinct year from the database
    @Query("SELECT DISTINCT substr(date, -4) AS year FROM receipts ORDER BY year DESC")
    fun fetchDistinctYears(): LiveData<List<String>>

//    // Query to fetch the total amount from the receipts of month
//    @Query("SELECT SUM(totalAmount) FROM RECEIPTS WHERE substr(date, -4) = :year and substr(date, 1, 2) = :month")
//    fun fetchTotalAmountOfMonth(month: String, year: String): LiveData<Double>
//
//    // Query to fetch weekly expenditure data
//    @Query("SELECT substr(date, 1, 2) AS month, SUM(totalAmount) AS total FROM RECEIPTS WHERE substr(date, -4) = :year GROUP BY substr(date, 1, 2)")
//    fun fetchWeeklyExpenditure(year: String): LiveData<List<WeeklyExpenditure>>
//
//    // Query to count the total number of receipts of the month
//    @Query("SELECT COUNT(*) FROM RECEIPTS WHERE substr(date, -4) = :year and substr(date, 1, 2) = :month")
//    fun countTotalReceiptsOfMonth(month: String, year: String): LiveData<Int>

    // Query to fetch the total amount from the receipts of week
    @Query("SELECT SUM(totalAmount) AS totalAmount FROM receipts WHERE date >= :startDate AND date <= :endDate")
    fun fetchTotalAmountOfWeek(startDate: String, endDate: String): LiveData<Double>

    // Query to fetch daily expenditure data
    @Query("SELECT date, SUM(totalAmount) AS total FROM receipts WHERE date BETWEEN :startOfWeek AND :endOfWeek GROUP BY date")
    fun fetchDailyExpenditure(startOfWeek: String, endOfWeek: String): LiveData<List<DailyExpenditure>>

    // Query to count the total number of receipts of the week
    @Query("SELECT COUNT(*) FROM receipts WHERE date BETWEEN :startOfWeek AND :endOfWeek")
    fun countTotalReceiptsOfWeek(startOfWeek: String, endOfWeek: String): LiveData<Int>

}