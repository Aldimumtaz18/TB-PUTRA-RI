package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.FinanceDateFilter
import com.example.data.model.FinanceDatePreset
import com.example.ui.theme.Amber400
import com.example.ui.theme.Amber600
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Emerald600
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.Orange400
import com.example.ui.theme.Orange500
import com.example.ui.theme.Orange600
import com.example.ui.theme.Red400
import com.example.ui.theme.Red600
import com.example.ui.viewmodel.StoreUiState
import com.example.util.FinanceReportData
import com.example.util.addDaysMillis
import com.example.util.formatMonthNameYear
import com.example.util.formatMonthShortName
import com.example.util.formatRupiah
import com.example.util.formatShortDateOnly
import com.example.util.formatShortDayMonth
import com.example.util.localDayStartToUtcPickerMillis
import com.example.util.monthDayRangeMillis
import com.example.util.startOfDayMillis
import com.example.util.startOfWeekMillis
import com.example.util.utcPickerMillisToLocalDayStart
import com.example.util.yearDayRangeMillis
import java.util.Calendar

enum class CustomPeriodGranularity(val label: String) {
  RENTANG("Rentang"),
  HARIAN("Tanggal"),
  MINGGUAN("Mingguan"),
  BULANAN("Bulanan"),
  TAHUNAN("Tahunan"),
}

private enum class ReportBaseChoice(val label: String) {
  SEMUA("Semua Waktu"),
  HARI_INI("Hari Ini"),
  KEMARIN("Kemarin"),
  CUSTOM("Custom"),
}

@Composable
fun CustomPeriodPickerDialog(
  initialFilter: FinanceDateFilter,
  onDismiss: () -> Unit,
  onApply: (FinanceDateFilter) -> Unit,
) {
  val now = remember { System.currentTimeMillis() }
  val today = remember(now) { startOfDayMillis(now) }
  val nowCal = remember(now) { Calendar.getInstance().apply { timeInMillis = now } }

  var granularity by remember {
    mutableStateOf(
      when {
        initialFilter.customLabel?.startsWith("Minggu", ignoreCase = true) == true ||
          initialFilter.preset == FinanceDatePreset.MINGGU_INI -> CustomPeriodGranularity.MINGGUAN
        initialFilter.customLabel?.startsWith("Bulan", ignoreCase = true) == true ||
          initialFilter.preset == FinanceDatePreset.BULAN_INI -> CustomPeriodGranularity.BULANAN
        initialFilter.customLabel?.startsWith("Tahun", ignoreCase = true) == true ||
          initialFilter.preset == FinanceDatePreset.TAHUN_INI -> CustomPeriodGranularity.TAHUNAN
        initialFilter.preset == FinanceDatePreset.TANGGAL_PILIHAN -> CustomPeriodGranularity.HARIAN
        else -> CustomPeriodGranularity.RENTANG
      }
    )
  }

  var selectedDayMillis by remember {
    mutableLongStateOf(initialFilter.customStartDayMillis ?: today)
  }
  var weekStartMillis by remember {
    mutableLongStateOf(startOfWeekMillis(initialFilter.customStartDayMillis ?: now))
  }
  var selectedYear by remember { mutableIntStateOf(nowCal.get(Calendar.YEAR)) }
  var selectedMonthZero by remember { mutableIntStateOf(nowCal.get(Calendar.MONTH)) }
  var rangeStartMillis by remember {
    mutableLongStateOf(initialFilter.customStartDayMillis ?: addDaysMillis(today, -6))
  }
  var rangeEndMillis by remember {
    mutableLongStateOf(initialFilter.customEndDayMillis ?: today)
  }

  val builtFilter =
    buildCustomFinanceFilter(
      granularity = granularity,
      selectedDayMillis = selectedDayMillis,
      weekStartMillis = weekStartMillis,
      selectedYear = selectedYear,
      selectedMonthZero = selectedMonthZero,
      rangeStartMillis = rangeStartMillis,
      rangeEndMillis = rangeEndMillis,
    )

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      modifier =
        Modifier.widthIn(max = 480.dp)
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 20.dp),
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Custom Periode",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "Pilih tanggal, mingguan, bulanan, tahunan, atau rentang",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Tutup")
          }
        }

        CustomGranularityControls(
          granularity = granularity,
          onGranularityChange = { granularity = it },
          selectedDayMillis = selectedDayMillis,
          onSelectedDayChange = { selectedDayMillis = it },
          weekStartMillis = weekStartMillis,
          onWeekStartChange = { weekStartMillis = it },
          selectedYear = selectedYear,
          onSelectedYearChange = { selectedYear = it },
          selectedMonthZero = selectedMonthZero,
          onSelectedMonthChange = { selectedMonthZero = it },
          rangeStartMillis = rangeStartMillis,
          rangeEndMillis = rangeEndMillis,
          onRangeChange = { s, e ->
            rangeStartMillis = s
            rangeEndMillis = e
          },
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(14.dp),
          ) {
            Text("Batal")
          }
          Button(
            onClick = { onApply(builtFilter) },
            modifier = Modifier.weight(1f).height(48.dp).testTag("apply_custom_period_btn"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Orange600, contentColor = Color.White),
          ) {
            Text("Terapkan", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
fun FinanceReportExportDialog(
  uiState: StoreUiState,
  onDismiss: () -> Unit,
  onPrintPdf: (FinanceReportData) -> Unit,
  onShareWhatsApp: (FinanceReportData) -> Unit,
  onShareOther: (FinanceReportData) -> Unit,
) {
  val isDark = isSystemInDarkTheme()
  val profitFg = if (isDark) Emerald400 else Emerald600
  val expenseFg = if (isDark) Red400 else Red600
  val kasbonFg = if (isDark) Amber400 else Amber600
  val accentFg = if (isDark) Orange400 else Orange600

  val now = remember { System.currentTimeMillis() }
  val today = remember(now) { startOfDayMillis(now) }
  val nowCal = remember(now) { Calendar.getInstance().apply { timeInMillis = now } }

  var baseChoice by remember {
    mutableStateOf(
      when (uiState.financeDateFilter.preset) {
        FinanceDatePreset.SEMUA -> ReportBaseChoice.SEMUA
        FinanceDatePreset.HARI_INI -> ReportBaseChoice.HARI_INI
        FinanceDatePreset.KEMARIN -> ReportBaseChoice.KEMARIN
        else -> ReportBaseChoice.CUSTOM
      }
    )
  }

  var granularity by remember {
    mutableStateOf(
      when {
        uiState.financeDateFilter.customLabel?.startsWith("Minggu", ignoreCase = true) == true ||
          uiState.financeDateFilter.preset == FinanceDatePreset.MINGGU_INI ->
          CustomPeriodGranularity.MINGGUAN
        uiState.financeDateFilter.customLabel?.startsWith("Bulan", ignoreCase = true) == true ||
          uiState.financeDateFilter.preset == FinanceDatePreset.BULAN_INI ->
          CustomPeriodGranularity.BULANAN
        uiState.financeDateFilter.customLabel?.startsWith("Tahun", ignoreCase = true) == true ||
          uiState.financeDateFilter.preset == FinanceDatePreset.TAHUN_INI ->
          CustomPeriodGranularity.TAHUNAN
        uiState.financeDateFilter.preset == FinanceDatePreset.TANGGAL_PILIHAN ->
          CustomPeriodGranularity.HARIAN
        else -> CustomPeriodGranularity.RENTANG
      }
    )
  }

  var selectedDayMillis by remember {
    mutableLongStateOf(uiState.financeDateFilter.customStartDayMillis ?: today)
  }
  var weekStartMillis by remember {
    mutableLongStateOf(startOfWeekMillis(uiState.financeDateFilter.customStartDayMillis ?: now))
  }
  var selectedYear by remember { mutableIntStateOf(nowCal.get(Calendar.YEAR)) }
  var selectedMonthZero by remember { mutableIntStateOf(nowCal.get(Calendar.MONTH)) }
  var rangeStartMillis by remember {
    mutableLongStateOf(uiState.financeDateFilter.customStartDayMillis ?: addDaysMillis(today, -6))
  }
  var rangeEndMillis by remember {
    mutableLongStateOf(uiState.financeDateFilter.customEndDayMillis ?: today)
  }

  val reportFilter =
    remember(
      baseChoice,
      granularity,
      selectedDayMillis,
      weekStartMillis,
      selectedYear,
      selectedMonthZero,
      rangeStartMillis,
      rangeEndMillis,
    ) {
      when (baseChoice) {
        ReportBaseChoice.SEMUA -> FinanceDateFilter(preset = FinanceDatePreset.SEMUA)
        ReportBaseChoice.HARI_INI -> FinanceDateFilter(preset = FinanceDatePreset.HARI_INI)
        ReportBaseChoice.KEMARIN -> FinanceDateFilter(preset = FinanceDatePreset.KEMARIN)
        ReportBaseChoice.CUSTOM ->
          buildCustomFinanceFilter(
            granularity = granularity,
            selectedDayMillis = selectedDayMillis,
            weekStartMillis = weekStartMillis,
            selectedYear = selectedYear,
            selectedMonthZero = selectedMonthZero,
            rangeStartMillis = rangeStartMillis,
            rangeEndMillis = rangeEndMillis,
          )
      }
    }

  val reportData =
    remember(
      reportFilter,
      uiState.transactions,
      uiState.expenses,
      uiState.products,
      uiState.settings,
      uiState.activeUserName,
    ) {
      uiState.buildReportDataForFilter(reportFilter)
    }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      modifier =
        Modifier.widthIn(max = 500.dp)
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 20.dp),
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
          ) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Orange600.copy(alpha = 0.14f),
              modifier = Modifier.size(40.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
              ) {
                Icon(
                  imageVector = Icons.Default.PictureAsPdf,
                  contentDescription = null,
                  tint = Orange600,
                  modifier = Modifier.size(20.dp),
                )
              }
            }
            Column {
              Text(
                text = "Cetak Laporan Keuangan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "Pilih periode waktu, lalu cetak PDF atau kirim ke WA",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Tutup dialog laporan")
          }
        }

        // Simplified 4 Period Choices: Semua Waktu, Hari Ini, Kemarin, Custom
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          ReportBaseChoice.entries.forEach { choice ->
            val selected = baseChoice == choice
            FilterChip(
              selected = selected,
              onClick = { baseChoice = choice },
              shape = RoundedCornerShape(999.dp),
              label = {
                Text(
                  text = choice.label,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                )
              },
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = Orange600,
                  selectedLabelColor = Color.White,
                  containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                  labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
              border =
                FilterChipDefaults.filterChipBorder(
                  enabled = true,
                  selected = selected,
                  borderColor = Color.Transparent,
                  selectedBorderColor = Orange600,
                ),
              modifier = Modifier.testTag("report_choice_${choice.name.lowercase()}"),
            )
          }
        }

        if (baseChoice == ReportBaseChoice.CUSTOM) {
          CustomGranularityControls(
            granularity = granularity,
            onGranularityChange = { granularity = it },
            selectedDayMillis = selectedDayMillis,
            onSelectedDayChange = { selectedDayMillis = it },
            weekStartMillis = weekStartMillis,
            onWeekStartChange = { weekStartMillis = it },
            selectedYear = selectedYear,
            onSelectedYearChange = { selectedYear = it },
            selectedMonthZero = selectedMonthZero,
            onSelectedMonthChange = { selectedMonthZero = it },
            rangeStartMillis = rangeStartMillis,
            rangeEndMillis = rangeEndMillis,
            onRangeChange = { s, e ->
              rangeStartMillis = s
              rangeEndMillis = e
            },
          )
        }

        // Live Preview Card
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "Ringkasan",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
              )
              Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (isDark) Orange500.copy(alpha = 0.2f) else Orange600.copy(alpha = 0.12f),
              ) {
                Text(
                  text = reportData.periodLabel,
                  style = MaterialTheme.typography.labelSmall,
                  color = accentFg,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
              }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ReportPreviewLine("Uang Masuk (${reportData.paidCount} nota)", formatRupiah(reportData.paidRevenue))
            Text(
              text =
                "• Tunai: ${formatRupiah(reportData.cashRevenue)}  |  Transfer/QRIS: ${formatRupiah(reportData.transferRevenue)}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ReportPreviewLine(
              "Uang Keluar (${reportData.expenseCount} catatan)",
              formatRupiah(reportData.totalExpenses),
              valueColor = expenseFg,
            )
            Text(
              text =
                "• Operasional: ${formatRupiah(reportData.operationalExpenses)}  |  Kulakan: ${formatRupiah(reportData.kulakanExpenses)}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ReportPreviewLine(
              "Sisa Uang Toko",
              formatRupiah(reportData.netCashFlow),
              bold = true,
              valueColor = if (reportData.netCashFlow >= 0) profitFg else expenseFg,
            )
            ReportPreviewLine(
              "Untung Kotor",
              formatRupiah(reportData.grossProfit),
              valueColor = MaterialTheme.colorScheme.onSurface,
            )
            ReportPreviewLine(
              "Untung Bersih",
              formatRupiah(reportData.netProfit),
              bold = true,
              valueColor = if (reportData.netProfit >= 0) profitFg else expenseFg,
            )
            ReportPreviewLine(
              "Kasbon Belum Lunas (${reportData.unpaidKasbonCount} bon)",
              formatRupiah(reportData.unpaidReceivables),
              valueColor = kasbonFg,
            )
            ReportPreviewLine(
              "Nilai Aset Stok Gudang",
              formatRupiah(reportData.inventoryCostValue),
              valueColor = MaterialTheme.colorScheme.onSurface,
            )
          }
        }

        // Actions: Cetak PDF or Kirim ke WA
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            OutlinedButton(
              onClick = {
                onDismiss()
                onPrintPdf(reportData)
              },
              modifier = Modifier.weight(1f).height(50.dp).testTag("dialog_print_pdf_btn"),
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(1.dp, accentFg),
            ) {
              Icon(
                Icons.Default.Print,
                contentDescription = null,
                tint = accentFg,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                "Cetak PDF",
                fontWeight = FontWeight.Bold,
                color = accentFg,
                maxLines = 1,
              )
            }

            Button(
              onClick = {
                onDismiss()
                onShareWhatsApp(reportData)
              },
              modifier = Modifier.weight(1f).height(50.dp).testTag("dialog_share_wa_btn"),
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(containerColor = Emerald600, contentColor = Color.White),
            ) {
              Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = null,
                modifier = Modifier.size(17.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("Kirim ke WA", fontWeight = FontWeight.Bold, maxLines = 1)
            }
          }

          TextButton(
            onClick = {
              onDismiss()
              onShareOther(reportData)
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 42.dp),
          ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Bagikan PDF ke Aplikasi Lain", style = MaterialTheme.typography.labelMedium)
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomGranularityControls(
  granularity: CustomPeriodGranularity,
  onGranularityChange: (CustomPeriodGranularity) -> Unit,
  selectedDayMillis: Long,
  onSelectedDayChange: (Long) -> Unit,
  weekStartMillis: Long,
  onWeekStartChange: (Long) -> Unit,
  selectedYear: Int,
  onSelectedYearChange: (Int) -> Unit,
  selectedMonthZero: Int,
  onSelectedMonthChange: (Int) -> Unit,
  rangeStartMillis: Long,
  rangeEndMillis: Long,
  onRangeChange: (Long, Long) -> Unit,
) {
  val isDark = isSystemInDarkTheme()
  val accentFg = if (isDark) Orange400 else Orange600

  var showDayPicker by remember { mutableStateOf(false) }
  var showWeekPicker by remember { mutableStateOf(false) }
  var showRangePicker by remember { mutableStateOf(false) }

  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        CustomPeriodGranularity.entries.forEach { item ->
          val selected = granularity == item
          Surface(
            shape = RoundedCornerShape(999.dp),
            color = if (selected) Orange600 else MaterialTheme.colorScheme.surface,
            border =
              BorderStroke(
                1.dp,
                if (selected) Orange600 else MaterialTheme.colorScheme.outlineVariant,
              ),
            modifier =
              Modifier.clip(RoundedCornerShape(999.dp)).clickable { onGranularityChange(item) },
          ) {
            Text(
              text = item.label,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
              color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            )
          }
        }
      }

      when (granularity) {
        CustomPeriodGranularity.RENTANG -> {
          val s = minOf(rangeStartMillis, rangeEndMillis)
          val e = maxOf(rangeStartMillis, rangeEndMillis)
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { showRangePicker = true },
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
              ) {
                Icon(
                  Icons.Default.DateRange,
                  contentDescription = null,
                  tint = accentFg,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "${formatShortDateOnly(s)}  —  ${formatShortDateOnly(e)}",
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.Bold,
                )
              }
              Text(
                text = "Pilih Tanggal",
                style = MaterialTheme.typography.labelMedium,
                color = accentFg,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }

        CustomPeriodGranularity.HARIAN -> {
          PeriodStepperBar(
            label = formatShortDateOnly(selectedDayMillis),
            onPrev = { onSelectedDayChange(addDaysMillis(selectedDayMillis, -1)) },
            onCenterClick = { showDayPicker = true },
            onNext = { onSelectedDayChange(addDaysMillis(selectedDayMillis, 1)) },
          )
        }

        CustomPeriodGranularity.MINGGUAN -> {
          val weekEnd = addDaysMillis(weekStartMillis, 6)
          PeriodStepperBar(
            label = "${formatShortDayMonth(weekStartMillis)} - ${formatShortDateOnly(weekEnd)}",
            onPrev = { onWeekStartChange(addDaysMillis(weekStartMillis, -7)) },
            onCenterClick = { showWeekPicker = true },
            onNext = { onWeekStartChange(addDaysMillis(weekStartMillis, 7)) },
          )
        }

        CustomPeriodGranularity.BULANAN -> {
          PeriodStepperBar(
            label = formatMonthNameYear(selectedYear, selectedMonthZero),
            onPrev = {
              if (selectedMonthZero == Calendar.JANUARY) {
                onSelectedMonthChange(Calendar.DECEMBER)
                onSelectedYearChange(selectedYear - 1)
              } else {
                onSelectedMonthChange(selectedMonthZero - 1)
              }
            },
            onCenterClick = null,
            onNext = {
              if (selectedMonthZero == Calendar.DECEMBER) {
                onSelectedMonthChange(Calendar.JANUARY)
                onSelectedYearChange(selectedYear + 1)
              } else {
                onSelectedMonthChange(selectedMonthZero + 1)
              }
            },
          )
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            for (m in Calendar.JANUARY..Calendar.DECEMBER) {
              val isSel = selectedMonthZero == m
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSel) Orange600 else MaterialTheme.colorScheme.surface,
                border =
                  BorderStroke(
                    1.dp,
                    if (isSel) Orange600 else MaterialTheme.colorScheme.outlineVariant,
                  ),
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onSelectedMonthChange(m) },
              ) {
                Text(
                  text = formatMonthShortName(m),
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                  color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
              }
            }
          }
        }

        CustomPeriodGranularity.TAHUNAN -> {
          PeriodStepperBar(
            label = "Tahun $selectedYear",
            onPrev = { onSelectedYearChange((selectedYear - 1).coerceAtLeast(2000)) },
            onCenterClick = null,
            onNext = { onSelectedYearChange((selectedYear + 1).coerceAtMost(2100)) },
          )
        }
      }
    }
  }

  if (showDayPicker) {
    val pickerState =
      rememberDatePickerState(
        initialSelectedDateMillis = localDayStartToUtcPickerMillis(selectedDayMillis)
      )
    DatePickerDialog(
      onDismissRequest = { showDayPicker = false },
      confirmButton = {
        Button(
          onClick = {
            pickerState.selectedDateMillis?.let { utc ->
              onSelectedDayChange(utcPickerMillisToLocalDayStart(utc))
            }
            showDayPicker = false
          }
        ) {
          Text("Pilih")
        }
      },
      dismissButton = { TextButton(onClick = { showDayPicker = false }) { Text("Batal") } },
    ) {
      DatePicker(state = pickerState)
    }
  }

  if (showWeekPicker) {
    val pickerState =
      rememberDatePickerState(
        initialSelectedDateMillis = localDayStartToUtcPickerMillis(weekStartMillis)
      )
    DatePickerDialog(
      onDismissRequest = { showWeekPicker = false },
      confirmButton = {
        Button(
          onClick = {
            pickerState.selectedDateMillis?.let { utc ->
              onWeekStartChange(startOfWeekMillis(utcPickerMillisToLocalDayStart(utc)))
            }
            showWeekPicker = false
          }
        ) {
          Text("Pilih Minggu")
        }
      },
      dismissButton = { TextButton(onClick = { showWeekPicker = false }) { Text("Batal") } },
    ) {
      DatePicker(state = pickerState)
    }
  }

  if (showRangePicker) {
    val s = minOf(rangeStartMillis, rangeEndMillis)
    val e = maxOf(rangeStartMillis, rangeEndMillis)
    val rangeState =
      rememberDateRangePickerState(
        initialSelectedStartDateMillis = localDayStartToUtcPickerMillis(s),
        initialSelectedEndDateMillis = localDayStartToUtcPickerMillis(e),
      )
    Dialog(
      onDismissRequest = { showRangePicker = false },
      properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
      Surface(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
          DateRangePicker(
            state = rangeState,
            modifier = Modifier.fillMaxWidth().height(460.dp),
          )
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.End,
          ) {
            TextButton(onClick = { showRangePicker = false }) { Text("Batal") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
              onClick = {
                val startUtc = rangeState.selectedStartDateMillis
                val endUtc = rangeState.selectedEndDateMillis ?: startUtc
                if (startUtc != null && endUtc != null) {
                  onRangeChange(
                    utcPickerMillisToLocalDayStart(startUtc),
                    utcPickerMillisToLocalDayStart(endUtc),
                  )
                }
                showRangePicker = false
              }
            ) {
              Text("Terapkan")
            }
          }
        }
      }
    }
  }
}

private fun buildCustomFinanceFilter(
  granularity: CustomPeriodGranularity,
  selectedDayMillis: Long,
  weekStartMillis: Long,
  selectedYear: Int,
  selectedMonthZero: Int,
  rangeStartMillis: Long,
  rangeEndMillis: Long,
): FinanceDateFilter =
  when (granularity) {
    CustomPeriodGranularity.HARIAN ->
      FinanceDateFilter(
        preset = FinanceDatePreset.TANGGAL_PILIHAN,
        customStartDayMillis = selectedDayMillis,
        customLabel = "Tanggal • ${formatShortDateOnly(selectedDayMillis)}",
      )
    CustomPeriodGranularity.MINGGUAN -> {
      val end = addDaysMillis(weekStartMillis, 6)
      FinanceDateFilter(
        preset = FinanceDatePreset.RENTANG_TANGGAL,
        customStartDayMillis = weekStartMillis,
        customEndDayMillis = end,
        customLabel = "Mingguan • ${formatShortDayMonth(weekStartMillis)} - ${formatShortDateOnly(end)}",
      )
    }
    CustomPeriodGranularity.BULANAN -> {
      val (start, end) = monthDayRangeMillis(selectedYear, selectedMonthZero)
      FinanceDateFilter(
        preset = FinanceDatePreset.RENTANG_TANGGAL,
        customStartDayMillis = start,
        customEndDayMillis = end,
        customLabel = "Bulanan • ${formatMonthNameYear(selectedYear, selectedMonthZero)}",
      )
    }
    CustomPeriodGranularity.TAHUNAN -> {
      val (start, end) = yearDayRangeMillis(selectedYear)
      FinanceDateFilter(
        preset = FinanceDatePreset.RENTANG_TANGGAL,
        customStartDayMillis = start,
        customEndDayMillis = end,
        customLabel = "Tahunan • $selectedYear",
      )
    }
    CustomPeriodGranularity.RENTANG -> {
      val s = minOf(rangeStartMillis, rangeEndMillis)
      val e = maxOf(rangeStartMillis, rangeEndMillis)
      FinanceDateFilter(
        preset = FinanceDatePreset.RENTANG_TANGGAL,
        customStartDayMillis = s,
        customEndDayMillis = e,
        customLabel =
          if (s == e) formatShortDateOnly(s)
          else "${formatShortDateOnly(s)} - ${formatShortDateOnly(e)}",
      )
    }
  }

@Composable
private fun PeriodStepperBar(
  label: String,
  onPrev: () -> Unit,
  onCenterClick: (() -> Unit)?,
  onNext: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      IconButton(onClick = onPrev, modifier = Modifier.size(44.dp)) {
        Icon(
          Icons.AutoMirrored.Filled.KeyboardArrowLeft,
          contentDescription = "Periode sebelumnya",
        )
      }
      Row(
        modifier =
          Modifier.weight(1f)
            .clip(RoundedCornerShape(8.dp))
            .then(
              if (onCenterClick != null) Modifier.clickable(onClick = onCenterClick)
              else Modifier
            )
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (onCenterClick != null) {
          Icon(
            Icons.Default.CalendarToday,
            contentDescription = null,
            tint = Orange600,
            modifier = Modifier.size(14.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
          text = label,
          style = MaterialTheme.typography.bodySmall,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      IconButton(onClick = onNext, modifier = Modifier.size(44.dp)) {
        Icon(
          Icons.AutoMirrored.Filled.KeyboardArrowRight,
          contentDescription = "Periode berikutnya",
        )
      }
    }
  }
}

@Composable
private fun ReportPreviewLine(
  label: String,
  value: String,
  bold: Boolean = false,
  valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.weight(1f),
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = value,
      fontFamily = JetBrainsMono,
      style = MaterialTheme.typography.bodySmall,
      fontWeight = FontWeight.Bold,
      color = valueColor,
    )
  }
}
