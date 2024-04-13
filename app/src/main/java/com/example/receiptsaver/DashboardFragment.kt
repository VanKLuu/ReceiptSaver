package com.example.receiptsaver
import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.receiptsaver.db.MyDatabaseRepository
import com.example.receiptsaver.db.Receipts
import com.example.receiptsaver.MainActivity
import java.text.NumberFormat

private const val LOG_TAG = "DashboardFragment"
private const val REQUEST_IMAGE_SELECTION = 100

class DashboardFragment : Fragment() {

    private lateinit var uploadButton: Button
    private lateinit var searchView: SearchView
    private lateinit var receiptRecyclerView: RecyclerView
    private var adapter: ReceiptAdapter? = null
    private lateinit var dbRepo: MyDatabaseRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)
        dbRepo = MyDatabaseRepository(requireContext())
        uploadButton = view.findViewById(R.id.buttonUpload)
        searchView = view.findViewById(R.id.searchView)
        receiptRecyclerView = view.findViewById(R.id.recyclerViewReceipts) as RecyclerView
        adapter = ReceiptAdapter(emptyList())
        receiptRecyclerView.adapter = adapter
        receiptRecyclerView.layoutManager = GridLayoutManager(context, 1)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        searchView.isIconified = true
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) {
                    navigateToSearchFragment(query)
                    return true
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return true
            }
        })
        uploadButton.setOnClickListener {
            openImagePicker()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_IMAGE_SELECTION)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_SELECTION && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                try {
                    val inputStream = requireContext().contentResolver.openInputStream(uri)
                    val imageBitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    imageBitmap?.let { (activity as MainActivity).performOCR(it) }
                } catch (e: Exception) {
                    Log.e(LOG_TAG, "Error reading image file", e)
                }
            }
        }
    }

    private fun navigateToSearchFragment(query: String) {
        val fragmentTransaction = parentFragmentManager.beginTransaction()
        val searchFragment = SearchFragment.newInstance(query)
        fragmentTransaction.replace(R.id.fragment_container, searchFragment)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
    }

    override fun onStart() {
        super.onStart()
        updateUI()
    }

    private inner class ReceiptHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val receiptPhoto: ImageView = itemView.findViewById(R.id.receiptPhoto)
        private val storeName: TextView = itemView.findViewById(R.id.storeName)
        private val receiptDate: TextView = itemView.findViewById(R.id.receiptDate)
        private val totalAmount: TextView = itemView.findViewById(R.id.totalAmount)
        val currencyFormat = NumberFormat.getCurrencyInstance()

        fun bind(receipts: Receipts) {
            storeName.text = receipts.name
            receiptDate.text = receipts.date
            val formattedTotalAmount = currencyFormat.format(receipts.totalAmount)
            totalAmount.text = formattedTotalAmount.toString()
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

    private inner class ReceiptAdapter(var receiptList: List<Receipts>) :
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
        dbRepo.fetchAllReceipts().observe(viewLifecycleOwner) { receiptList ->
            adapter?.let {
                it.receiptList = receiptList
                it.notifyDataSetChanged()
            }
        }
    }

    companion object {
        fun newInstance(): DashboardFragment {
            return DashboardFragment()
        }
    }
}

