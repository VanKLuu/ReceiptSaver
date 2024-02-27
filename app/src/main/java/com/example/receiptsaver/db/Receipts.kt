package com.example.receiptsaver.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "RECEIPTS")
data class Receipts(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "id")
    var id: UUID = UUID.randomUUID(),

    @ColumnInfo(name = "name") //name of store
    var name: String = "",

    @ColumnInfo(name = "date")
    var date: String = "",

    @ColumnInfo(name = "totalAmount")
    var totalAmount: Double = 0.0,

    @ColumnInfo(name = "image")
    var image: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Receipts

        if (image != null) {
            if (other.image == null) return false
            if (!image.contentEquals(other.image)) return false
        } else if (other.image != null) return false

        return true
    }

    override fun hashCode(): Int {
        return image?.contentHashCode() ?: 0
    }
}

