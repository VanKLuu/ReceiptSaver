package com.example.receiptsaver

import android.app.DatePickerDialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.example.receiptsaver.db.MyDatabaseRepository
import com.github.chrisbanes.photoview.PhotoView
import com.github.chrisbanes.photoview.PhotoViewAttacher
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private const val TAG = "ReceiptUpdateFragment"

class ReceiptUpdateFragment : Fragment() , DatePickerDialog.OnDateSetListener{
    private lateinit var receiptPhoto: PhotoView
    private lateinit var storeName: EditText
    private lateinit var totalAmount: EditText
    private lateinit var receiptDate: TextView
    private lateinit var receiptId: String
    private lateinit var dbRepo: MyDatabaseRepository
    private lateinit var photoViewAttacher: PhotoViewAttacher
    private lateinit var viewModel: ReceiptUpdateViewModel


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_receipt_update, container, false)
        receiptPhoto = view.findViewById(R.id.receiptPhoto)
        storeName = view.findViewById(R.id.storeName)
        totalAmount = view.findViewById(R.id.totalAmount)
        receiptDate = view.findViewById(R.id.receiptDate)
        dbRepo = MyDatabaseRepository(requireContext())
        arguments?.let { args ->
            receiptId = args.getString("id") ?: ""
        }
        photoViewAttacher = PhotoViewAttacher(receiptPhoto)
        val saveButton: Button = view.findViewById(R.id.saveButton)
        receiptDate.setOnClickListener {
            showDatePickerDialog()
        }
        saveButton.setOnClickListener {
            val name = storeName.text.toString()
            val date = receiptDate.text.toString()
            val amount = totalAmount.text.toString().toDoubleOrNull() ?: 0.0
            saveReceiptChanges(name, date, amount, receiptId)
        }
        setupViewModel()
        loadReceiptFromDatabase()
        return view
    }

    private fun setupViewModel() {
        val viewModelFactory = ReceiptUpdateViewModelFactory(dbRepo)
        viewModel = ViewModelProvider(this, viewModelFactory).get(ReceiptUpdateViewModel::class.java)
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(requireContext(), this, year, month, dayOfMonth)
        datePickerDialog.show()
    }
    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        val selectedCalendar = Calendar.getInstance()
        selectedCalendar.set(Calendar.YEAR, year)
        selectedCalendar.set(Calendar.MONTH, month)
        selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        val formattedDate = dateFormat.format(selectedCalendar.time)
        receiptDate.text = formattedDate
    }
    private fun saveReceiptChanges(name: String, date: String, totalAmount: Double, receiptId: String) {
        viewModel.updateReceipt(name, date, totalAmount, receiptId)
        navigateToReceiptDetail(receiptId)
    }


    private fun navigateToReceiptDetail(receiptId: String)
    {
        try {
            val fragment = ReceiptDetailFragment.newInstance(receiptId)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to detail fragment", e)
        }
    }

    private fun loadReceiptFromDatabase() {
        viewModel.fetchReceiptById(receiptId).observe(viewLifecycleOwner) { receipt ->
            if (receipt != null) {
                storeName.setText(receipt.name ?: "")
                receiptDate.text = receipt.date
                totalAmount.setText(receipt.totalAmount.toString())

                val imageData = receipt.image
                if (imageData != null) {
                    val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
                    receiptPhoto.setImageBitmap(bitmap)
                    photoViewAttacher.update()
                } else {
                    receiptPhoto.setImageResource(R.drawable.receipt_image)
                }
            } else {
                Log.e(TAG, "Receipt with ID $receiptId not found in the database")
            }
        }
    }

    companion object {
        fun newInstance(id: String): ReceiptUpdateFragment {
            val fragment = ReceiptUpdateFragment()
            val args = Bundle()
            args.putString("id", id)
            fragment.arguments = args
            return fragment
        }
    }
}