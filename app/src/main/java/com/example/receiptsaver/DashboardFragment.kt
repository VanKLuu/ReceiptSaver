package com.example.receiptsaver

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
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.receiptsaver.db.MyDatabaseRepository
import com.example.receiptsaver.db.Receipts
import java.io.InputStream
import android.app.Activity
import java.util.UUID
import java.util.concurrent.Executors


private const val TAG = "DashboardFragment"
private const val REQUEST_PDF_SELECTION = 2

class DashboardFragment : Fragment() {

    private lateinit var uploadButton: Button
    private lateinit var searchView: SearchView
    private lateinit var receiptRecyclerView: RecyclerView
    private var adapter: ReceiptAdapter? = null
    private lateinit var dbRepo: MyDatabaseRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called from DashboardFragment")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.e(TAG, "onCreateView called from DashboardFragment")
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)
        dbRepo = MyDatabaseRepository(requireContext())

        // Initialize views and set up RecyclerView adapter
        initializeViews(view)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up click listener for upload button
        uploadButton.setOnClickListener {
            openPdfPicker()
        }

        // Set up query text listener for search functionality
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Handle search query submission
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Handle search query text change
                return false
            }
        })
    }

    private fun initializeViews(view: View) {
        uploadButton = view.findViewById(R.id.buttonUpload)
        searchView = view.findViewById(R.id.searchView)
        receiptRecyclerView = view.findViewById(R.id.recyclerViewReceipts)
        adapter = ReceiptAdapter(emptyList())
        receiptRecyclerView.adapter = adapter
        receiptRecyclerView.layoutManager = GridLayoutManager(context, 2)
        updateUI()
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
                    val inputStream: InputStream? = requireContext().contentResolver.openInputStream(uri)
                    val pdfByteArray = inputStream?.readBytes()
                    inputStream?.close()
                    pdfByteArray?.let { savePdfToDatabase(it) }
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading PDF file", e)
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

    private fun updateUI() {
        dbRepo.fetchAllReceipts().observe(viewLifecycleOwner) { receiptList ->
            adapter?.updateReceipts(receiptList)
        }
    }

    private inner class ReceiptAdapter(private var receiptList: List<Receipts>) :
        RecyclerView.Adapter<ReceiptAdapter.ReceiptHolder>() {

        inner class ReceiptHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            // Initialize views
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReceiptHolder {
            // Inflate layout and create ViewHolder
            return ReceiptHolder(LayoutInflater.from(parent.context).inflate(R.layout.receipt_item, parent, false))
        }

        override fun onBindViewHolder(holder: ReceiptHolder, position: Int) {
            // Bind data to ViewHolder
        }

        override fun getItemCount() = receiptList.size

        fun updateReceipts(newReceiptList: List<Receipts>) {
            receiptList = newReceiptList
            notifyDataSetChanged()
        }
    }

    companion object {
        fun newInstance(): DashboardFragment {
            return DashboardFragment()
        }
    }
}

