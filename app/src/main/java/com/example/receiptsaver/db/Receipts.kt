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
    var image: ByteArray? = null,

    @ColumnInfo(name = "thumbnail")
    var thumbnail: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Receipts

        if (id != other.id) return false
        if (name != other.name) return false
        if (date != other.date) return false
        if (totalAmount != other.totalAmount) return false
        if (image != null) {
            if (other.image == null) return false
            if (!image.contentEquals(other.image)) return false
        } else if (other.image != null) return false
        if (thumbnail != null) {
            if (other.thumbnail == null) return false
            if (!thumbnail.contentEquals(other.thumbnail)) return false
        } else if (other.thumbnail != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + date.hashCode()
        result = 31 * result + totalAmount.hashCode()
        result = 31 * result + (image?.contentHashCode() ?: 0)
        result = 31 * result + (thumbnail?.contentHashCode() ?: 0)
        return result
    }
}


