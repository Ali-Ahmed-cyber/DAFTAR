package com.daftar.app.data.converter

import androidx.room.TypeConverter
import java.math.BigDecimal

class RoomTypeConverters {
    @TypeConverter
    fun fromBigDecimal(value: BigDecimal?): String? = value?.toPlainString()

    @TypeConverter
    fun toBigDecimal(raw: String?): BigDecimal? =
        raw?.trim()?.takeIf { it.isNotEmpty() }?.let { BigDecimal(it) }
}
