package com.example.receiptsaver

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import com.example.receiptsaver.db.MyDatabaseRepository
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MonthFragment : Fragment() {
    private lateinit var totalAmountTextView: TextView
    private lateinit var totalReceiptsTextView: TextView
    private lateinit var weeklyExpenditureChart: BarChart
    private lateinit var dbRepo: MyDatabaseRepository
    private val currencyFormat = NumberFormat.getCurrencyInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_month, container, false)
        totalAmountTextView = view.findViewById(R.id.tvTotalAmount)
        totalReceiptsTextView = view.findViewById(R.id.tvTotalReceipts) // Initialize tvTotalReceipts
        weeklyExpenditureChart = view.findViewById(R.id.chartWeeklyExpenditure)
        dbRepo = MyDatabaseRepository(requireContext())
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val (startOfWeek, endOfWeek) = getFourWeekRange()
        Log.d("ExpensesFragment", "Date from $startOfWeek to $endOfWeek")
        fetchTotalAmountFromDatabase(startOfWeek, endOfWeek)
        fetchTotalReceiptsFromDatabase(startOfWeek, endOfWeek)
        fetchWeeklyExpenditureFromDatabase(startOfWeek, endOfWeek)
    }

    private fun fetchTotalAmountFromDatabase(startOfWeek: String, endOfWeek: String) {
        dbRepo.fetchTotalAmount(startOfWeek, endOfWeek).observe(viewLifecycleOwner) { totalAmount ->
            val formattedTotalAmount = currencyFormat.format(totalAmount)
            totalAmountTextView.text = "Total Amount: $formattedTotalAmount"
        }
    }

    private fun fetchWeeklyExpenditureFromDatabase(startOfWeek: String, endOfWeek: String) {
        dbRepo.fetchWeeklyExpenditure(startOfWeek, endOfWeek)
            .observe(viewLifecycleOwner) { weeklyExpenditureData ->
                weeklyExpenditureData?.let {
                    populateChart(it)
                }
            }
    }
    private fun fetchTotalReceiptsFromDatabase(startOfWeek: String, endOfWeek: String) {
        dbRepo.countTotalReceipts(startOfWeek, endOfWeek).observe(viewLifecycleOwner) { totalReceipts ->
            totalReceiptsTextView.text = "Total Receipts: $totalReceipts"
        }
    }


    private fun populateChart(weeklyExpenditureData: List<Pair<String?, Double>>) {
        // Define an array of abbreviated week names
        val abbreviatedWeeks = arrayOf("Week 1", "Week 2", "Week 3", "Week 4")
        // Create a map to store expenditure data for each month
        val expenditureMap = abbreviatedWeeks.associateWith { 0.0 }.toMutableMap()

        // Populate the map with available data
        weeklyExpenditureData.forEach { (week, expenditure) ->
            week?.toIntOrNull()?.let { weekIndex ->
                val weekAbbreviation = abbreviatedWeeks.getOrNull(weekIndex - 1)
                weekAbbreviation?.let {
                    expenditureMap[it] = expenditure
                    Log.d("ExpensesFragment", "Assigned value $expenditure to month $weekAbbreviation")
                }
            }
        }

        // Create a list of BarEntry objects for each week
        val barEntries = abbreviatedWeeks.indices.map { index ->
            val month = abbreviatedWeeks[index]
            val expenditure = expenditureMap[month] ?: 0.0
            BarEntry(index.toFloat(), expenditure.toFloat())
        }

        // Set custom labels for the x-axis
        val labels = abbreviatedWeeks.toList()
        weeklyExpenditureChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        weeklyExpenditureChart.xAxis.labelCount = labels.size

        // Create a BarDataSet and set its data
        val barDataSet = BarDataSet(barEntries, "Weekly Expenditure")
        val data = BarData(barDataSet)

        // Apply data to the chart and refresh it
        weeklyExpenditureChart.data = data
        weeklyExpenditureChart.invalidate()
    }



    private fun getFourWeekRange(): Pair<String, String> {
        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)
        val calendar = Calendar.getInstance()

        // Calculate the end date (current date)
        val endDate = dateFormat.format(calendar.time)

        // Calculate the start date (four weeks ago)
        calendar.add(Calendar.DATE, -27)
        val startDate = dateFormat.format(calendar.time)

        return Pair(startDate, endDate)
    }
}