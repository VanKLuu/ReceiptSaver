package com.example.receiptsaver

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import com.example.receiptsaver.db.MyDatabaseRepository
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.components.XAxis
import android.util.Log



class ExpensesFragment : Fragment() {
    private lateinit var totalAmountTextView: TextView
    private lateinit var monthlyExpenditureChart: BarChart
    private lateinit var databaseRepository: MyDatabaseRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_expenses, container, false)
        totalAmountTextView = view.findViewById(R.id.tvTotalAmount)
        monthlyExpenditureChart = view.findViewById(R.id.chartMonthlyExpenditure)
        databaseRepository = MyDatabaseRepository(requireContext())
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Fetch total amount from receipts database
        fetchTotalAmountFromDatabase()

        // Fetch monthly expenditure data from receipts database
        fetchMonthlyExpenditureFromDatabase()
    }

    private fun fetchTotalAmountFromDatabase() {
        GlobalScope.launch(Dispatchers.Main) {
            // Fetch all receipts from the database
            val allReceipts = databaseRepository.fetchAllReceipts().value ?: emptyList()

            // Calculate total amount by summing up the totalAmount property of all receipts
            var totalAmount = 0.0
            for (receipt in allReceipts) {
                totalAmount += receipt.totalAmount
            }

            // Set the total amount to the TextView
            totalAmountTextView.text = "Total Amount: $totalAmount"
        }
    }

    private fun fetchMonthlyExpenditureFromDatabase() {
        databaseRepository.fetchMonthlyExpenditure()
            .observe(viewLifecycleOwner, { monthlyExpenditureData ->
                monthlyExpenditureData?.let { populateChart(it) }
            })
    }

    private fun populateChart(monthlyExpenditureData: List<Pair<String?, Double>>) {
        // Define an array of abbreviated month names
        val abbreviatedMonths = arrayOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        )

        // Create a map to store expenditure data for each month
        val expenditureMap = abbreviatedMonths.associateWith { 0.0 }.toMutableMap()

        // Log the initial map
        Log.d("ExpensesFragment", "Initial expenditure map: $expenditureMap")

        // Populate the map with available data
        monthlyExpenditureData.forEach { (month, expenditure) ->
            month?.let { expenditureMap[month] = expenditure }
        }

        // Log the final map
        Log.d("ExpensesFragment", "Populated expenditure map: $expenditureMap")

        // Create a list of BarEntry objects for each month
        val barEntries = abbreviatedMonths.indices.map { index ->
            BarEntry(index.toFloat(), expenditureMap[abbreviatedMonths[index]]!!.toFloat())
        }

        // Log the bar entries
        Log.d("ExpensesFragment", "Bar entries: $barEntries")

        val barDataSet = BarDataSet(barEntries, "Monthly Expenditure")
        val data = BarData(barDataSet)

        // Configure the axis to display only positive values
        monthlyExpenditureChart.axisLeft.axisMinimum = 0f

        // Remove gridlines and labels if needed
        monthlyExpenditureChart.xAxis.setDrawGridLines(false)
        monthlyExpenditureChart.axisLeft.setDrawGridLines(false)
        monthlyExpenditureChart.axisRight.setDrawGridLines(false)

        // Set custom labels for the x-axis
        monthlyExpenditureChart.xAxis.valueFormatter = IndexAxisValueFormatter(abbreviatedMonths)
        monthlyExpenditureChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        monthlyExpenditureChart.xAxis.granularity = 1f
        monthlyExpenditureChart.xAxis.labelCount = abbreviatedMonths.size

        monthlyExpenditureChart.data = data
        monthlyExpenditureChart.invalidate() // Refresh the chart
    }

}