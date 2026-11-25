package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val indoLocale = Locale("id", "ID")
private val currencyFormat =
  DecimalFormat("#,###", DecimalFormatSymbols(indoLocale).apply { groupingSeparator = '.' })
private val qtyFormat =
  DecimalFormat(
    "0.##",
    DecimalFormatSymbols(indoLocale).apply {
      decimalSeparator = ','
      groupingSeparator = '.'
    },
  )

fun formatRupiah(amount: Long): String {
  val prefix = if (amount < 0) "-Rp " else "Rp "
  return prefix + currencyFormat.format(kotlin.math.abs(amount))
}

fun formatQty(qty: Double): String {
  val rounded = kotlin.math.round(qty * 100.0) / 100.0
  return qtyFormat.format(rounded)
}

fun formatDateTime(timestamp: Long): String {
  val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", indoLocale)
  return sdf.format(Date(timestamp))
}

fun formatDateShort(timestamp: Long): String {
  val sdf = SimpleDateFormat("dd/MM/yy HH:mm", indoLocale)
  return sdf.format(Date(timestamp))
}

fun startOfDayMillis(timestamp: Long = System.currentTimeMillis()): Long {
  return Calendar.getInstance().apply {
    timeInMillis = timestamp
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
  }.timeInMillis
}

fun addDaysMillis(dayStartMillis: Long, days: Int): Long {
  return Calendar.getInstance().apply {
    timeInMillis = dayStartMillis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
    add(Calendar.DAY_OF_MONTH, days)
  }.timeInMillis
}

fun startOfMonthMillis(timestamp: Long = System.currentTimeMillis()): Long {
  return Calendar.getInstance().apply {
    timeInMillis = timestamp
    set(Calendar.DAY_OF_MONTH, 1)
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
  }.timeInMillis
}

fun endOfMonthExclusiveMillis(timestamp: Long = System.currentTimeMillis()): Long {
  return Calendar.getInstance().apply {
    timeInMillis = startOfMonthMillis(timestamp)
    add(Calendar.MONTH, 1)
  }.timeInMillis
}

fun startOfWeekMillis(timestamp: Long = System.currentTimeMillis()): Long {
  return Calendar.getInstance().apply {
    timeInMillis = startOfDayMillis(timestamp)
    firstDayOfWeek = Calendar.MONDAY
    val dayOfWeek = get(Calendar.DAY_OF_WEEK)
    val diffFromMonday = (dayOfWeek - Calendar.MONDAY + 7) % 7
    add(Calendar.DAY_OF_MONTH, -diffFromMonday)
  }.timeInMillis
}

fun endOfWeekExclusiveMillis(timestamp: Long = System.currentTimeMillis()): Long {
  return addDaysMillis(startOfWeekMillis(timestamp), 7)
}

fun startOfYearMillis(timestamp: Long = System.currentTimeMillis()): Long {
  return Calendar.getInstance().apply {
    timeInMillis = timestamp
    set(Calendar.MONTH, Calendar.JANUARY)
    set(Calendar.DAY_OF_MONTH, 1)
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
  }.timeInMillis
}

fun endOfYearExclusiveMillis(timestamp: Long = System.currentTimeMillis()): Long {
  return Calendar.getInstance().apply {
    timeInMillis = startOfYearMillis(timestamp)
    add(Calendar.YEAR, 1)
  }.timeInMillis
}

fun monthDayRangeMillis(year: Int, monthZeroBased: Int): Pair<Long, Long> {
  val startCal = Calendar.getInstance().apply {
    clear()
    set(year, monthZeroBased, 1, 0, 0, 0)
    set(Calendar.MILLISECOND, 0)
  }
  val lastDay = startCal.getActualMaximum(Calendar.DAY_OF_MONTH)
  val endCal = Calendar.getInstance().apply {
    clear()
    set(year, monthZeroBased, lastDay, 0, 0, 0)
    set(Calendar.MILLISECOND, 0)
  }
  return startCal.timeInMillis to endCal.timeInMillis
}

fun yearDayRangeMillis(year: Int): Pair<Long, Long> {
  val startCal = Calendar.getInstance().apply {
    clear()
    set(year, Calendar.JANUARY, 1, 0, 0, 0)
    set(Calendar.MILLISECOND, 0)
  }
  val endCal = Calendar.getInstance().apply {
    clear()
    set(year, Calendar.DECEMBER, 31, 0, 0, 0)
    set(Calendar.MILLISECOND, 0)
  }
  return startCal.timeInMillis to endCal.timeInMillis
}

fun formatShortDateOnly(timestamp: Long): String {
  return SimpleDateFormat("dd MMM yyyy", indoLocale).format(Date(timestamp))
}

fun formatMonthYear(timestamp: Long): String {
  return SimpleDateFormat("MMMM yyyy", indoLocale).format(Date(timestamp))
}

fun formatYearOnly(timestamp: Long): String {
  return SimpleDateFormat("yyyy", indoLocale).format(Date(timestamp))
}

fun formatShortDayMonth(timestamp: Long): String {
  return SimpleDateFormat("dd MMM", indoLocale).format(Date(timestamp))
}

fun formatMonthNameYear(year: Int, monthZeroBased: Int): String {
  val cal = Calendar.getInstance().apply {
    clear()
    set(year, monthZeroBased, 1, 0, 0, 0)
  }
  return SimpleDateFormat("MMMM yyyy", indoLocale).format(cal.time)
}

fun formatMonthShortName(monthZeroBased: Int): String {
  val cal = Calendar.getInstance().apply {
    clear()
    set(2026, monthZeroBased, 1, 0, 0, 0)
  }
  return SimpleDateFormat("MMM", indoLocale).format(cal.time)
}

fun utcPickerMillisToLocalDayStart(utcMillis: Long): Long {
  val utcCal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
    timeInMillis = utcMillis
  }
  return Calendar.getInstance().apply {
    clear()
    set(
      utcCal.get(Calendar.YEAR),
      utcCal.get(Calendar.MONTH),
      utcCal.get(Calendar.DAY_OF_MONTH),
      0,
      0,
      0,
    )
    set(Calendar.MILLISECOND, 0)
  }.timeInMillis
}

fun localDayStartToUtcPickerMillis(localDayStart: Long): Long {
  val localCal = Calendar.getInstance().apply { timeInMillis = localDayStart }
  return Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
    clear()
    set(
      localCal.get(Calendar.YEAR),
      localCal.get(Calendar.MONTH),
      localCal.get(Calendar.DAY_OF_MONTH),
      0,
      0,
      0,
    )
    set(Calendar.MILLISECOND, 0)
  }.timeInMillis
}

fun formatDayLabel(dayStartMillis: Long): String {
  val today = startOfDayMillis()
  val yesterday = addDaysMillis(today, -1)
  val tomorrow = addDaysMillis(today, 1)
  val dateStr = SimpleDateFormat("dd MMM yyyy", indoLocale).format(Date(dayStartMillis))
  return when (dayStartMillis) {
    today -> "Hari Ini • $dateStr"
    yesterday -> "Kemarin • $dateStr"
    tomorrow -> "Besok • $dateStr"
    else -> dateStr
  }
}
