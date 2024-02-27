package com.example.receiptsaver.db

import androidx.room.TypeConverter
import java.util.UUID

class MyTypeConverters
{
    @TypeConverter
    fun toUUID(uuid: String?): UUID? {
        return UUID.fromString(uuid)
    }

    @TypeConverter
    fun fromUUID(uuid: UUID?): String? {
        return uuid?.toString()
    }
}