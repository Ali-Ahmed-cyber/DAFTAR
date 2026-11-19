package com.daftar.app.core.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object MoneyMath {
    val ZERO: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
    val ONE: BigDecimal = BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP)
    val HUNDRED: BigDecimal = BigDecimal("100").setScale(4, RoundingMode.HALF_UP)

    fun of(value: String): BigDecimal =
        value.trim().takeIf { it.isNotEmpty() }?.let { BigDecimal(it) } ?: BigDecimal.ZERO

    fun scaleMoney(value: BigDecimal, decimals: Int = 2): BigDecimal =
        value.setScale(decimals, RoundingMode.HALF_UP)

    fun scaleRate(value: BigDecimal): BigDecimal =
        value.setScale(6, RoundingMode.HALF_UP)

    fun sumOf(values: Iterable<BigDecimal>): BigDecimal =
        values.fold(BigDecimal.ZERO) { acc, item -> acc.add(item) }

    fun isZero(value: BigDecimal, toleranceScale: Int = 2): Boolean =
        value.setScale(toleranceScale, RoundingMode.HALF_UP).compareTo(BigDecimal.ZERO) == 0

    fun equalsExact(a: BigDecimal, b: BigDecimal, decimals: Int = 2): Boolean =
        a.setScale(decimals, RoundingMode.HALF_UP)
            .compareTo(b.setScale(decimals, RoundingMode.HALF_UP)) == 0

    fun multiply(a: BigDecimal, b: BigDecimal, decimals: Int = 2): BigDecimal =
        a.multiply(b).setScale(decimals, RoundingMode.HALF_UP)

    fun divide(a: BigDecimal, b: BigDecimal, decimals: Int = 4): BigDecimal {
        require(b.compareTo(BigDecimal.ZERO) != 0) { "Division by zero in financial calculation" }
        return a.divide(b, decimals, RoundingMode.HALF_UP)
    }

    fun percentage(base: BigDecimal, ratePercent: BigDecimal, decimals: Int = 2): BigDecimal =
        base.multiply(ratePercent).divide(BigDecimal("100"), decimals, RoundingMode.HALF_UP)

    fun format(value: BigDecimal, decimals: Int = 2, currencyCode: String? = null): String {
        val symbols = DecimalFormatSymbols(Locale.US)
        val pattern = if (decimals > 0) "#,##0.${"0".repeat(decimals)}" else "#,##0"
        val df = DecimalFormat(pattern, symbols)
        val formatted = df.format(value.setScale(decimals, RoundingMode.HALF_UP))
        return if (currencyCode.isNullOrBlank()) formatted else "$formatted $currencyCode"
    }
}

fun BigDecimal.isPositive(): Boolean = this.compareTo(BigDecimal.ZERO) > 0
fun BigDecimal.isNegative(): Boolean = this.compareTo(BigDecimal.ZERO) < 0
fun BigDecimal.isZeroAmount(): Boolean = this.compareTo(BigDecimal.ZERO) == 0
