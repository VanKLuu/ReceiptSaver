package com.example.receiptsaver

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.receiptsaver.db.MyDatabaseRepository
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class WeekFragment : Fragment() {
    private lateinit var totalAmountTextView: TextView
    private lateinit var totalReceiptsTextView: TextView
    private lateinit var dailyExpenditureChart: BarChart
    private lateinit var dbRepo: MyDatabaseRepository
    private val currencyFormat = NumberFormat.getCurrencyInstance()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_week, container, false)
        totalAmountTextView = view.findViewById(R.id.tvTotalAmount)
        totalReceiptsTextView = view.findViewById(R.id.tvTotalReceipts) // Initialize tvTotalReceipts
        dailyExpenditureChart = view.findViewById(R.id.chartDailyExpenditure)
        dbRepo = MyDatabaseRepository(requireContext())
        return view
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val (startOfWeek, endOfWeek) = getStartAndEndOfWeek()

        fetchTotalAmountFromDatabase(startOfWeek, endOfWeek)
        fetchTotalReceiptsFromDatabase(startOfWeek, endOfWeek)
        fetchDailyExpenditureFromDatabase(startOfWeek, endOfWeek)
    }

    private fun fetchTotalAmountFromDatabase(startOfWeek: String, endOfWeek: String) {
        dbRepo.fetchTotalAmount(startOfWeek, endOfWeek).observe(viewLifecycleOwner) { totalAmount ->
            if (totalAmount != null) {
                val formattedTotalAmount = currencyFormat.format(totalAmount)
                totalAmountTextView.text = "Total Amount: $formattedTotalAmount"
            }
            else
                totalAmountTextView.text = "Total Amount: $0.0"
        }
    }

    private fun fetchDailyExpenditureFromDatabase(startOfWeek: String, endOfWeek: String) {
        dbRepo.fetchDailyExpenditure(startOfWeek, endOfWeek)
            .observe(viewLifecycleOwner) { dailyExpenditureData ->
                dailyExpenditureData?.let {
                    populateChart(it)
                }
            }
    }
    private fun fetchTotalReceiptsFromDatabase(startOfWeek: String, endOfWeek: String) {
        dbRepo.countTotalReceipts(startOfWeek, endOfWeek).observe(viewLifecycleOwner) { totalReceipts ->
            totalReceiptsTextView.text = "Total Receipts: $totalReceipts"
        }
    }
    private fun populateChart(dailyExpenditureData: List<Pair<String?, Double>>) {
        val expenditureMap = mutableMapOf<String, Double>()
        dailyExpenditureData.forEach { (dateString, expenditure) ->
            dateString?.let { date ->
                val dayOfWeek = getDayOfWeek(date)
                expenditureMap[dayOfWeek] = expenditure
                Log.d("ExpensesFragment", "Assigned value $expenditure to day $dayOfWeek")
            }
        }
        // Define an array of abbreviated day names
        val abbreviatedDayOfWeeks = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        // Create a list of BarEntry objects for each day of the week
        val barEntries = abbreviatedDayOfWeeks.mapIndexed { index, dayOfWeek ->
            val expenditure = expenditureMap[dayOfWeek] ?: 0.0
            BarEntry(index.toFloat(), expenditure.toFloat())
        }
        // Set custom labels for the x-axis
        val labels = abbreviatedDayOfWeeks.toList()
        dailyExpenditureChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        dailyExpenditureChart.xAxis.labelCount = labels.size
        // Create a BarDataSet and set its data
        val barDataSet = BarDataSet(barEntries, "Daily Expenditure")
        val data = BarData(barDataSet)
        // Apply data to the chart and refresh it
        dailyExpenditureChart.data = data
        dailyExpenditureChart.invalidate()
    }

    private fun getDayOfWeek(dateString: String): String {
        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)
        val calendar = Calendar.getInstance()
        calendar.time = dateFormat.parse(dateString) ?: Date()
        val dayOfWeekIndex = calendar.get(Calendar.DAY_OF_WEEK)
        val abbreviatedDayOfWeeks = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        return abbreviatedDayOfWeeks[dayOfWeekIndex - 1]
    }
    private fun getStartAndEndOfWeek(): Pair<String, String> {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.SUNDAY
        calendar.time = Date()
        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)

        // Get the start date of the week (Sunday)
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        val startOfWeek = dateFormat.format(calendar.time)

        // Get the end date of the week (Saturday)
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
        val endOfWeek = dateFormat.format(calendar.time)

        return Pair(startOfWeek, endOfWeek)
    }
}