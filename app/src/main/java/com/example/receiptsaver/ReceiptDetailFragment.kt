package com.example.receiptsaver

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import com.example.receiptsaver.db.MyDatabaseRepository
import com.example.receiptsaver.db.Receipts
import java.util.UUID


private const val TAG = "ReceiptDetailFragment"
class ReceiptDetailFragment : Fragment() {

    private lateinit var receiptPhoto: ImageView
    private lateinit var storeName: TextView
    private lateinit var totalAmount: TextView
    private lateinit var receiptDate: TextView
    private lateinit var receiptId: String
    private lateinit var dbRepo: MyDatabaseRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_receipt_detail, container, false)

        // Initialize views
        receiptPhoto = view.findViewById(R.id.receiptPhoto)
        storeName = view.findViewById(R.id.storeName)
        totalAmount = view.findViewById(R.id.totalAmount)
        receiptDate = view.findViewById(R.id.receiptDate)

        // Initialize database repository
        dbRepo = MyDatabaseRepository(requireContext())

        // Retrieve receipt ID from arguments
        arguments?.let { args ->
            receiptId = args.getString("id") ?: ""
        }

        // Load receipt information from the database
        loadReceiptFromDatabase()

        return view
    }

    private fun loadReceiptFromDatabase() {
        dbRepo.fetchReceiptByID(receiptId).observe(viewLifecycleOwner)  { receipt ->
            if (receipt != null) {
                // Bind receipt information to views
                storeName.text = receipt.name
                receiptDate.text = receipt.date
                totalAmount.text = receipt.totalAmount.toString()

                val imageData = receipt.image
                if (imageData != null) {
                    val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
                    receiptPhoto.setImageBitmap(bitmap)
                } else {
                    receiptPhoto.setImageResource(R.drawable.receipt_image)
                }
            } else {
                // Handle the case when receipt is not found
                Log.e(TAG, "Receipt with ID $receiptId not found in the database")
            }
        }
    }

    companion object {
        fun newInstance(id: String): ReceiptDetailFragment {
            val fragment = ReceiptDetailFragment()
            val args = Bundle()
            args.putString("id", id)
            fragment.arguments = args
            return fragment
        }
    }
}