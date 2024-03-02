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
    private lateinit var receipt: Receipts


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called")
    }

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

        // Retrieve data from arguments
        arguments?.let { args ->
            val receiptId = args.getString("id")
            val receiptName = args.getString("name")
            val receiptTotal = args.getDouble("total", 0.0) // Default value is 0.0
            val receiptDate = args.getString("date")
            val receiptImage = args.getByteArray("img")

            // Create Receipts object
            receipt = Receipts(
                UUID.fromString(receiptId),
                receiptName ?: "",
                receiptDate ?: "",
                receiptTotal,
                receiptImage
            )
        }

        // Bind data to views
        storeName.text = receipt.name
        receiptDate.text = receipt.date
        totalAmount.text = receipt.totalAmount.toString()

        // Load image if available
        receipt.image?.let { imageData ->
            if (imageData.isNotEmpty()) {
                receiptPhoto.setImageBitmap(BitmapFactory.decodeByteArray(imageData, 0, imageData.size))
            }
        }

        return view
    }
    companion object {
        fun newInstance(id: String, name: String, image: ByteArray?, totalAmount: Double, date: String): ReceiptDetailFragment {
            val fragment = ReceiptDetailFragment()
            val args = Bundle()
            args.putString("id", id)
            args.putString("name", name)
            args.putByteArray("img", image)
            args.putDouble("total", totalAmount)
            args.putString("date", date)
            fragment.arguments = args
            return fragment
        }
    }
}