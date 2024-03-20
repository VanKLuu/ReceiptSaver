package com.example.receiptsaver
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

private const val LOG_TAG = "DashboardFragment"

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
        receiptRecyclerView.layoutManager = GridLayoutManager(context, 2)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        searchView.isIconified = true
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

    //
//    private fun openPdfPicker() {
//        val intent = Intent(Intent.ACTION_GET_CONTENT)
//        intent.type = "application/pdf"
//        startActivityForResult(intent, REQUEST_PDF_SELECTION)
//    }
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (requestCode == REQUEST_PDF_SELECTION && resultCode == Activity.RESULT_OK) {
//            data?.data?.let { uri ->
//                try {
//                    val inputStream: InputStream? =
//                        requireContext().contentResolver.openInputStream(uri)
//                    val pdfByteArray = inputStream?.readBytes()
//                    inputStream?.close()
//                    pdfByteArray?.let { savePdfToDatabase(it) }
//                } catch (e: Exception) {
//                    Log.e(LOG_TAG, "Error reading PDF file", e)
//                }
//            }
//        }
//    }
//
//    private fun savePdfToDatabase(pdfByteArray: ByteArray) {
//        Executors.newSingleThreadExecutor().execute {
//            val receipt = Receipts(
//                id = UUID.randomUUID(),
//                name = "PDF Receipt",
//                date = "2024-02-29",
//                totalAmount = 0.0,
//                image = pdfByteArray
//            )
//            dbRepo.addReceipt(receipt)
//        }
//    }
}

