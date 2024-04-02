package com.example.receiptsaver.db

data class WeeklyExpenditure(
    val weekNumber: Int,
    val startDate: String?,
    val endDate: String?,
    val total: Double
)
data class MonthlyExpenditure(
    val month: String?,
    val total: Double
)
data class DailyExpenditure(
    val date: String?,
    val total: Double
)