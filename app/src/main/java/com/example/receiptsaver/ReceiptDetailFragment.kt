package com.example.receiptsaver

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import com.example.receiptsaver.db.MyDatabaseRepository
import com.github.chrisbanes.photoview.PhotoView
import com.github.chrisbanes.photoview.PhotoViewAttacher
import java.text.NumberFormat


private const val TAG = "ReceiptDetailFragment"
class ReceiptDetailFragment : Fragment() {

    private lateinit var receiptPhoto: PhotoView
    private lateinit var storeName: TextView
    private lateinit var totalAmount: TextView
    private lateinit var receiptDate: TextView
    private lateinit var receiptId: String
    private lateinit var dbRepo: MyDatabaseRepository
    val currencyFormat = NumberFormat.getCurrencyInstance()
    private lateinit var photoViewAttacher: PhotoViewAttacher

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_receipt_detail, container, false)
        receiptPhoto = view.findViewById(R.id.receiptPhoto)
        storeName = view.findViewById(R.id.storeName)
        totalAmount = view.findViewById(R.id.totalAmount)
        receiptDate = view.findViewById(R.id.receiptDate)
        dbRepo = MyDatabaseRepository(requireContext())
        arguments?.let { args ->
            receiptId = args.getString("id") ?: ""
        }
        photoViewAttacher = PhotoViewAttacher(receiptPhoto)
        val optionsButton: Button = view.findViewById(R.id.optionsButton)
        optionsButton.setOnClickListener {
            showOptionsPopupMenu(it)
        }
        loadReceiptFromDatabase()
        return view
    }
    private fun showOptionsPopupMenu(anchorView: View) {
        val popupMenu = PopupMenu(requireContext(), anchorView)
        popupMenu.menuInflater.inflate(R.menu.receipt_detail_menu, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_update -> {
                    updateReceipt()
                    true
                }
                R.id.action_delete -> {
                    deleteReceipt()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }
    private fun updateReceipt() {
        val receiptUpdateFragment = ReceiptUpdateFragment.newInstance(receiptId)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, receiptUpdateFragment)
            .addToBackStack(null) // Add to back stack to enable back navigation
            .commit()
    }

    private fun deleteReceipt() {
        dbRepo.removeReceipt(receiptId)
        val fragment = DashboardFragment.newInstance()
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
    private fun loadReceiptFromDatabase() {
        dbRepo.fetchReceiptByID(receiptId).observe(viewLifecycleOwner)  { receipt ->
            if (receipt != null) {
                // Bind receipt information to views
                storeName.text = receipt.name
                receiptDate.text = receipt.date
                val formattedTotalAmount = currencyFormat.format(receipt.totalAmount)
                totalAmount.text = "Total Amount: $formattedTotalAmount"

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
        fun newInstance(id: String): ReceiptDetailFragment {
            val fragment = ReceiptDetailFragment()
            val args = Bundle()
            args.putString("id", id)
            fragment.arguments = args
            return fragment
        }
    }
}