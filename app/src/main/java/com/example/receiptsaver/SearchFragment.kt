package com.example.receiptsaver

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.receiptsaver.db.MyDatabaseRepository
import com.example.receiptsaver.db.Receipts

private const val LOG_TAG = "SearchFragment"

class SearchFragment : Fragment() {

    private lateinit var receiptRecyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private var adapter: SearchAdapter? = null
    private lateinit var dbRepo: MyDatabaseRepository
    private lateinit var searchQuery: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            searchQuery = it.getString(SEARCH_QUERY_KEY, "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)
        dbRepo = MyDatabaseRepository(requireContext())
        searchView = view.findViewById(R.id.searchView)
        searchView.setQuery(searchQuery, false)
        receiptRecyclerView = view.findViewById(R.id.recyclerViewReceipts) as RecyclerView
        adapter = SearchAdapter(emptyList())
        receiptRecyclerView.adapter = adapter
        receiptRecyclerView.layoutManager = GridLayoutManager(context, 2)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        searchView.isIconified = false
        searchView.clearFocus()
        searchView.setQuery(searchQuery, false)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) {
                    searchQuery = query
                    updateUI()
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return true
            }
        })
    }


    override fun onStart() {
        super.onStart()
        updateUI()
    }

    private inner class ReceiptHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val receiptPhoto: ImageView = itemView.findViewById(R.id.receiptPhoto)
        private val storeName: TextView = itemView.findViewById(R.id.storeName)
        private val totalAmount: TextView = itemView.findViewById(R.id.totalAmount)

        fun bind(receipts: Receipts) {
            storeName.text = receipts.name
            totalAmount.text = receipts.totalAmount.toString()
            val imageData = receipts.thumbnail
            if (imageData != null) {
                val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
                receiptPhoto.setImageBitmap(bitmap)
            } else {
                receiptPhoto.setImageResource(R.drawable.receipt_image)
            }

            itemView.setOnClickListener {
                try {
                    val fragment = ReceiptDetailFragment.newInstance(receipts.id.toString())
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit()
                } catch (e: Exception) {
                    Log.e(LOG_TAG, "Error navigating to detail fragment", e)
                }
            }
        }
    }

    private inner class SearchAdapter(var receiptList: List<Receipts>) :
        RecyclerView.Adapter<ReceiptHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReceiptHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.receipt_item, parent, false)
            return ReceiptHolder(view)
        }

        override fun onBindViewHolder(holder: ReceiptHolder, position: Int) {
            val receiptData = receiptList[position]
            holder.bind(receiptData)
        }

        override fun getItemCount() = receiptList.size
    }

    private fun updateUI() {
        dbRepo.fetchReceiptsByName(searchQuery).observe(viewLifecycleOwner) { receiptList ->
            adapter?.let {
                it.receiptList = receiptList
                it.notifyDataSetChanged()
            }
        }
    }

    companion object {
        private const val SEARCH_QUERY_KEY = "search_query"

        fun newInstance(query: String): SearchFragment {
            val fragment = SearchFragment()
            val args = Bundle().apply {
                putString(SEARCH_QUERY_KEY, query)
            }
            fragment.arguments = args
            return fragment
        }
    }
}