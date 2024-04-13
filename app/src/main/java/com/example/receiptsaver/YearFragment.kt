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
import java.util.Calendar

class YearFragment : Fragment() {
    private lateinit var totalAmountTextView: TextView
    private lateinit var totalReceiptsTextView: TextView
    private lateinit var spinner: Spinner
    private lateinit var monthlyExpenditureChart: BarChart
    private lateinit var dbRepo: MyDatabaseRepository
    private val currencyFormat = NumberFormat.getCurrencyInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_year, container, false)
        totalAmountTextView = view.findViewById(R.id.tvTotalAmount)
        totalReceiptsTextView = view.findViewById(R.id.tvTotalReceipts) // Initialize tvTotalReceipts
        monthlyExpenditureChart = view.findViewById(R.id.chartMonthlyExpenditure)
        spinner = view.findViewById(R.id.yearSpinner)
        dbRepo = MyDatabaseRepository(requireContext())
        dbRepo.fetchDistinctYears().observe(viewLifecycleOwner) { distinctYears ->
            if (distinctYears != null && distinctYears.isNotEmpty()) {
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, distinctYears)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinner.adapter = adapter
            } else {
                Log.e("ExpensesFragment", "Distinct years list is null or empty")
                val currentYear = Calendar.getInstance().get(Calendar.YEAR).toString()
                val defaultAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, listOf(currentYear))
                defaultAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinner.adapter = defaultAdapter
            }
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedYear = parent?.getItemAtPosition(position).toString()
                fetchTotalAmountFromDatabase(selectedYear)
                fetchTotalReceiptsFromDatabase(selectedYear)
                fetchMonthlyExpenditureFromDatabase(selectedYear)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                val currentYear = Calendar.getInstance().get(Calendar.YEAR).toString()
                fetchTotalAmountFromDatabase(currentYear)
                fetchTotalReceiptsFromDatabase(currentYear)
                fetchMonthlyExpenditureFromDatabase(currentYear)
            }
        }
    }

    private fun fetchTotalAmountFromDatabase(year: String) {
        dbRepo.fetchTotalAmountOfYear(year).observe(viewLifecycleOwner) { totalAmount ->
            if (totalAmount != null) {
                val formattedTotalAmount = currencyFormat.format(totalAmount)
                totalAmountTextView.text = "Total Amount: $formattedTotalAmount"
            }
            else
                totalAmountTextView.text = "Total Amount: $0.0"
        }
    }

    private fun fetchMonthlyExpenditureFromDatabase(year: String) {
        dbRepo.fetchMonthlyExpenditure(year)
            .observe(viewLifecycleOwner) { monthlyExpenditureData ->
                monthlyExpenditureData?.let {
                    populateChart(it)
                }
            }
    }
    private fun populateChart(monthlyExpenditureData: List<Pair<String?, Double>>) {
        // Define an array of abbreviated month names
        val abbreviatedMonths = arrayOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        )
        // Create a map to store expenditure data for each month
        val expenditureMap = abbreviatedMonths.associateWith { 0.0 }.toMutableMap()
        // Populate the map with available data
        monthlyExpenditureData.forEach { (month, expenditure) ->
            month?.toIntOrNull()?.let { monthIndex ->
                val monthAbbreviation = abbreviatedMonths.getOrNull(monthIndex - 1)
                monthAbbreviation?.let {
                    expenditureMap[it] = expenditure
                    Log.d("ExpensesFragment", "Assigned value $expenditure to month $monthAbbreviation")
                }
            }
        }
        // Create a list of BarEntry objects for each month
        val barEntries = abbreviatedMonths.indices.map { index ->
            val month = abbreviatedMonths[index]
            val expenditure = expenditureMap[month] ?: 0.0
            BarEntry(index.toFloat(), expenditure.toFloat())
        }
        // Set custom labels for the x-axis
        val labels = abbreviatedMonths.toList()
        monthlyExpenditureChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        monthlyExpenditureChart.xAxis.labelCount = labels.size
        // Create a BarDataSet and set its data
        val barDataSet = BarDataSet(barEntries, "Monthly Expenditure")
        val data = BarData(barDataSet)
        // Apply data to the chart and refresh it
        monthlyExpenditureChart.data = data
        monthlyExpenditureChart.invalidate()
    }
    private fun fetchTotalReceiptsFromDatabase(year: String) {
        dbRepo.countTotalReceiptsOfYear(year).observe(viewLifecycleOwner) { totalReceipts ->
            totalReceiptsTextView.text = "Total Receipts: $totalReceipts"
        }
    }
}