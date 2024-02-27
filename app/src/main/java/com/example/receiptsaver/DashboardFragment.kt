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


private const val LOG_TAG = "DashboardFragment"

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
        this.searchView = view.findViewById(R.id.searchView)
        this.receiptRecyclerView = view.findViewById(R.id.recyclerViewReceipts) as RecyclerView
        this.adapter = ReceiptAdapter(emptyList()) // Initialize adapter with an empty list
        this.receiptRecyclerView.adapter = adapter // Set the adapter before initializing the RecyclerView
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
    }

    private inner class ReceiptHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {

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
                val intent = Intent(context, ReceiptDetailFragment::class.java)
                intent.putExtra("id", receipts.id)
                intent.putExtra("name", receipts.name)
                intent.putExtra("img", receipts.image)
                startActivity(intent)
            }
        }
    }
    private inner class ReceiptAdapter (var receiptList: List<Receipts>)
        : RecyclerView.Adapter<ReceiptHolder>()
    {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReceiptHolder
        {
            Log.d(LOG_TAG, "onCreateViewHolder called")
            val view = LayoutInflater.from(parent.context).inflate(R.layout.receipt_item, parent, false)
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