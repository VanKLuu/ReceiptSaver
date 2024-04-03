package com.example.receiptsaver.db

data class WeeklyExpenditure(
    val weekNumber: String?,
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