package com.example.receiptsaver

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.receiptsaver.db.MyDatabaseRepository
import com.example.receiptsaver.db.Receipts
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.Executors


private const val LOG_TAG = "DashboardFragment"
private const val REQUEST_PDF_SELECTION = 2


class DashboardFragment : Fragment() {

    private lateinit var uploadButton: Button
    private lateinit var searchView: SearchView
    private lateinit var receiptRecyclerView: RecyclerView
    private var adapter: ReceiptAdapter? = null
    private lateinit var dbRepo: MyDatabaseRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(LOG_TAG, "onCreate called from DashboardFragment")

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        Log.e(LOG_TAG, "onCreateView called from DashboardFragment")
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)
        dbRepo = MyDatabaseRepository(requireContext())
        this.uploadButton = view.findViewById(R.id.buttonUpload)
        this.searchView = view.findViewById(R.id.searchView)
        this.receiptRecyclerView = view.findViewById(R.id.recyclerViewReceipts) as RecyclerView
        this.adapter = ReceiptAdapter(emptyList()) // Initialize adapter with an empty list
        this.receiptRecyclerView.adapter =
            adapter // Set the adapter before initializing the RecyclerView
        this.receiptRecyclerView.layoutManager = GridLayoutManager(context, 2)
        this.updateUI()
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        searchView.isIconified = true

        // Set up query text listener to handle search functionality
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) {
                    val fragmentTransaction = parentFragmentManager.beginTransaction()
                    fragmentTransaction.replace(R.id.fragment_container, SearchFragment())
                    fragmentTransaction.commit()
                    return true
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return true
            }
        })
        uploadButton.setOnClickListener {
            openPdfPicker()
        }
    }

    private fun openPdfPicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "application/pdf"
        startActivityForResult(intent, REQUEST_PDF_SELECTION)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PDF_SELECTION && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                try {
                    val inputStream: InputStream? =
                        requireContext().contentResolver.openInputStream(uri)
                    val pdfByteArray = inputStream?.readBytes()
                    inputStream?.close()
                    pdfByteArray?.let { savePdfToDatabase(it) }
                } catch (e: Exception) {
                    Log.e(LOG_TAG, "Error reading PDF file", e)
                }
            }
        }
    }

    private fun savePdfToDatabase(pdfByteArray: ByteArray) {
        Executors.newSingleThreadExecutor().execute {
            val receipt = Receipts(
                id = UUID.randomUUID(),
                name = "PDF Receipt",
                date = "2024-02-29",
                totalAmount = 0.0,
                image = pdfByteArray
            )
            dbRepo.addReceipt(receipt)
        }
    }

    private inner class ReceiptHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val receiptPhoto: ImageView = itemView.findViewById(R.id.receiptPhoto)
        private val storeName: TextView = itemView.findViewById(R.id.storeName)
        private val totalAmount: TextView = itemView.findViewById(R.id.totalAmount)

        fun bind(receipts: Receipts) {
            storeName.text = receipts.name
            totalAmount.text = receipts.totalAmount.toString()
            val imageData = receipts.image
            if (imageData != null) {
                val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)

                receiptPhoto.setImageBitmap(bitmap)
            } else {
                receiptPhoto.setImageResource(R.drawable.receipt_image)
            }

            itemView.setOnClickListener {
                try {
                    val fragment = ReceiptDetailFragment.newInstance(
                        receipts.id.toString(),
                        receipts.name,
                        receipts.image,
                        receipts.totalAmount,
                        receipts.date
                    )
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null) // Optional: Adds the transaction to the back stack
                        .commit()
                } catch (e: Exception) {
                    Log.e("ReceiptAdapter", "Error navigating to detail fragment", e)
                    // Handle the error, such as showing a toast or logging
                }
            }
        }
    }

    private inner class ReceiptAdapter(var receiptList: List<Receipts>) :
        RecyclerView.Adapter<ReceiptHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReceiptHolder {
            Log.d(LOG_TAG, "onCreateViewHolder called")
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.receipt_item, parent, false)
            return ReceiptHolder(view)
        }

        override fun onBindViewHolder(holder: ReceiptHolder, position: Int) {
            Log.d(LOG_TAG, "onBindViewHolder called")
            val receiptData = receiptList[position]
            Log.d(LOG_TAG, "receiptData: $receiptData")
            holder.bind(receiptData)
        }

        override fun getItemCount() = receiptList.size
    }

    private fun updateUI() {
        dbRepo.fetchAllReceipts().observe(viewLifecycleOwner) { receiptList ->
            Log.e(LOG_TAG, "albumList observe called with receiptList=$receiptList")
            adapter = receiptList?.let { ReceiptAdapter(it) }
            this.receiptRecyclerView.adapter = adapter
        }
    }

    companion object {
        fun newInstance(): DashboardFragment {
            return DashboardFragment()
        }
    }
}
