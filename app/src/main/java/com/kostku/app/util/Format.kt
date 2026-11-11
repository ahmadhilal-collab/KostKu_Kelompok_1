package com.kostku.app.util

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Fmt {
    val localeId: Locale = Locale("id", "ID")
    private val zone: ZoneId get() = ZoneId.systemDefault()
    private val longDate = DateTimeFormatter.ofPattern("d MMMM yyyy", localeId)
    private val shortDate = DateTimeFormatter.ofPattern("d MMMM", localeId)
    private val monthYear = DateTimeFormatter.ofPattern("MMMM yyyy", localeId)

    fun rupiah(amount: Long): String = "Rp" + NumberFormat.getInstance(localeId).format(amount)
    fun rupiahSpaced(amount: Long): String = "Rp " + NumberFormat.getInstance(localeId).format(amount)

    fun localDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
    fun startOfDayMillis(date: LocalDate): Long = date.atStartOfDay(zone).toInstant().toEpochMilli()

    fun date(millis: Long): String = localDate(millis).format(longDate)
    fun date(date: LocalDate): String = date.format(longDate)
    fun shortDate(millis: Long): String = localDate(millis).format(shortDate)
    fun month(year: Int, month: Int): String = YearMonth.of(year, month).format(monthYear)

    /** 800000 -> "Delapan Ratus Ribu Rupiah" */
    fun terbilang(n: Long): String {
        if (n <= 0L) return "Nol Rupiah"
        val words = words(n).trim().split(" ").filter { it.isNotEmpty() }
        return words.joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } } + " Rupiah"
    }

    private val units = listOf(
        "", "satu", "dua", "tiga", "empat", "lima", "enam", "tujuh", "delapan", "sembilan", "sepuluh", "sebelas"
    )

    private fun rest(n: Long): String = if (n > 0) " " + words(n) else ""

    private fun words(n: Long): String = when {
        n < 12 -> units[n.toInt()]
        n < 20 -> words(n - 10) + " belas"
        n < 100 -> words(n / 10) + " puluh" + rest(n % 10)
        n < 200 -> "seratus" + rest(n - 100)
        n < 1_000 -> words(n / 100) + " ratus" + rest(n % 100)
        n < 2_000 -> "seribu" + rest(n - 1_000)
        n < 1_000_000 -> words(n / 1_000) + " ribu" + rest(n % 1_000)
        n < 1_000_000_000 -> words(n / 1_000_000) + " juta" + rest(n % 1_000_000)
        else -> words(n / 1_000_000_000) + " miliar" + rest(n % 1_000_000_000)
    }
}
