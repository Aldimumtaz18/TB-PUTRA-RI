package com.example.ui.components

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsBluetooth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.StoreSettings
import com.example.data.local.TransactionEntity
import com.example.data.model.ReceiptPaperSize
import com.example.ui.theme.Amber100
import com.example.ui.theme.Amber600
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Emerald600
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.Navy900
import com.example.ui.theme.Orange100
import com.example.ui.theme.Orange400
import com.example.ui.theme.Orange500
import com.example.ui.theme.Orange600
import com.example.ui.theme.Red400
import com.example.ui.theme.Red600
import com.example.util.BluetoothPrinterHelper
import com.example.util.BluetoothPrintersState
import com.example.util.ReceiptHelper
import kotlinx.coroutines.launch

private enum class ThermalPrintMode {
  BLUETOOTH,
  SYSTEM,
}

@Composable
fun ThermalPrintDialog(
  transaction: TransactionEntity,
  settings: StoreSettings,
  isSuratJalan: Boolean,
  onSavePrintPreferences: (ReceiptPaperSize, String?, String?) -> Unit,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val isDark = isSystemInDarkTheme()

  var selectedSize by remember(settings.thermalPaperSize) { mutableStateOf(settings.thermalPaperSize) }
  var printMode by remember { mutableStateOf(ThermalPrintMode.BLUETOOTH) }
  var printersState by remember { mutableStateOf(BluetoothPrinterHelper.getPairedPrintersState(context)) }
  var selectedAddress by remember(settings.savedPrinterAddress) {
    mutableStateOf(settings.savedPrinterAddress)
  }
  var isPrintingBluetooth by remember { mutableStateOf(false) }
  var statusBanner by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

  val refreshPrinters = {
    printersState = BluetoothPrinterHelper.getPairedPrintersState(context)
    val ready = printersState as? BluetoothPrintersState.Ready
    if (ready != null && ready.devices.isNotEmpty()) {
      val stillExists = ready.devices.any { it.address == selectedAddress }
      if (!stillExists) {
        selectedAddress = ready.devices.first().address
      }
    }
  }

  val readyDevices = (printersState as? BluetoothPrintersState.Ready)?.devices.orEmpty()
  if (selectedAddress.isBlank() && readyDevices.isNotEmpty()) {
    selectedAddress = readyDevices.first().address
  }

  val permissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      refreshPrinters()
      if (!granted) {
        statusBanner =
          false to "Izin Bluetooth ditolak. Berikan izin Perangkat Sekitar agar printer bisa terhubung."
      }
    }

  val orangeBg = if (isDark) Orange500.copy(alpha = 0.18f) else Orange100
  val orangeFg = if (isDark) Orange400 else Orange600
  val emeraldBg = if (isDark) Emerald600.copy(alpha = 0.18f) else Emerald100
  val emeraldFg = if (isDark) Emerald400 else Emerald600
  val dangerFg = if (isDark) Red400 else Red600

  Dialog(
    onDismissRequest = { if (!isPrintingBluetooth) onDismiss() },
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
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
          ) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = orangeBg,
              modifier = Modifier.size(40.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.Print,
                  contentDescription = null,
                  tint = orangeFg,
                  modifier = Modifier.size(21.dp),
                )
              }
            }
            Column {
              Text(
                text = if (isSuratJalan) "Cetak Surat Jalan" else "Cetak Struk Thermal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = "Pilih ukuran kertas & metode cetak",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
          IconButton(
            onClick = { if (!isPrintingBluetooth) onDismiss() },
            modifier = Modifier.minimumInteractiveComponentSize().testTag("close_print_dialog"),
          ) {
            Icon(Icons.Default.Close, contentDescription = "Tutup dialog cetak")
          }
        }

        // Paper Size Selection with clean proportional width cue
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Ukuran Kertas",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            ReceiptPaperSize.entries.forEach { size ->
              val isSelected = selectedSize == size
              val tagName =
                if (size == ReceiptPaperSize.THERMAL_58MM) "paper_size_58mm" else "paper_size_80mm"
              val stripWidth = if (size == ReceiptPaperSize.THERMAL_58MM) 22.dp else 34.dp
              Surface(
                shape = RoundedCornerShape(14.dp),
                color =
                  if (isSelected) orangeBg
                  else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border =
                  BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) Orange600 else MaterialTheme.colorScheme.outlineVariant,
                  ),
                modifier =
                  Modifier.weight(1f)
                    .heightIn(min = 82.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(enabled = !isPrintingBluetooth) {
                      selectedSize = size
                      onSavePrintPreferences(size, null, null)
                    }
                    .testTag(tagName),
              ) {
                Column(
                  modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                  verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    // Mini visual paper-roll width indicator
                    Box(
                      modifier =
                        Modifier.width(stripWidth)
                          .height(10.dp)
                          .background(
                            color =
                              if (isSelected) Orange600
                              else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(3.dp),
                          ),
                    )
                    if (isSelected) {
                      Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Dipilih",
                        tint = Orange600,
                        modifier = Modifier.size(18.dp),
                      )
                    } else {
                      Spacer(modifier = Modifier.size(18.dp))
                    }
                  }
                  Text(
                    text = size.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) orangeFg else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                  )
                  Text(
                    text = size.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }
          }
        }

        // Clean Print Method Switcher
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Metode Cetak",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
          )
          SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
              selected = printMode == ThermalPrintMode.BLUETOOTH,
              onClick = { if (!isPrintingBluetooth) printMode = ThermalPrintMode.BLUETOOTH },
              shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
              icon = {
                Icon(
                  imageVector = Icons.Default.Bluetooth,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
              },
            ) {
              Text("Bluetooth", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            SegmentedButton(
              selected = printMode == ThermalPrintMode.SYSTEM,
              onClick = { if (!isPrintingBluetooth) printMode = ThermalPrintMode.SYSTEM },
              shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
              icon = {
                Icon(
                  imageVector = Icons.Default.Description,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
              },
            ) {
              Text("Sistem / PDF", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
          }
        }

        if (printMode == ThermalPrintMode.BLUETOOTH) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "Printer Bluetooth Terpasang",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
              )
              TextButton(
                onClick = refreshPrinters,
                enabled = !isPrintingBluetooth,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.minimumInteractiveComponentSize(),
              ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Segarkan", style = MaterialTheme.typography.labelMedium, maxLines = 1)
              }
              IconButton(
                onClick = {
                  runCatching { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
                },
                enabled = !isPrintingBluetooth,
                modifier = Modifier.minimumInteractiveComponentSize(),
              ) {
                Icon(
                  imageVector = Icons.Default.SettingsBluetooth,
                  contentDescription = "Pengaturan Bluetooth",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(19.dp),
                )
              }
            }

            when (val state = printersState) {
              BluetoothPrintersState.Unavailable -> {
                Surface(
                  shape = RoundedCornerShape(14.dp),
                  color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Text(
                    text = "Perangkat ini tidak mendukung Bluetooth. Silakan pilih metode Sistem / PDF di atas.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(14.dp),
                  )
                }
              }

              BluetoothPrintersState.PermissionRequired -> {
                Surface(
                  shape = RoundedCornerShape(14.dp),
                  color = if (isDark) Amber600.copy(alpha = 0.14f) else Amber100,
                  border = BorderStroke(1.dp, Amber600.copy(alpha = 0.3f)),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                  ) {
                    Text(
                      text = "Berikan izin Perangkat Sekitar (Bluetooth) untuk menampilkan printer thermal Anda.",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurface,
                    )
                    Button(
                      onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                          permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                        } else {
                          refreshPrinters()
                        }
                      },
                      shape = RoundedCornerShape(12.dp),
                      colors = ButtonDefaults.buttonColors(containerColor = Orange600, contentColor = Color.White),
                      modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                      Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text("Izinkan Akses Bluetooth", fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                  }
                }
              }

              BluetoothPrintersState.Disabled -> {
                Surface(
                  shape = RoundedCornerShape(14.dp),
                  color = if (isDark) Amber600.copy(alpha = 0.14f) else Amber100,
                  border = BorderStroke(1.dp, Amber600.copy(alpha = 0.3f)),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      Icon(
                        imageVector = Icons.Default.BluetoothDisabled,
                        contentDescription = null,
                        tint = Amber600,
                        modifier = Modifier.size(18.dp),
                      )
                      Text(
                        text = "Bluetooth sedang mati. Aktifkan Bluetooth terlebih dahulu lalu tekan Segarkan.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                      )
                    }
                    OutlinedButton(
                      onClick = {
                        runCatching { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
                      },
                      shape = RoundedCornerShape(12.dp),
                      modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                      Icon(Icons.Default.SettingsBluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text("Buka Pengaturan Bluetooth", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                  }
                }
              }

              is BluetoothPrintersState.Ready -> {
                if (state.devices.isEmpty()) {
                  Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                  ) {
                    Column(
                      modifier = Modifier.padding(14.dp),
                      verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                      Text(
                        text = "Belum ada printer Bluetooth yang terpasang. Pasangkan printer di Pengaturan HP lalu tekan Segarkan.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                      OutlinedButton(
                        onClick = {
                          runCatching { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                      ) {
                        Icon(Icons.Default.SettingsBluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pasangkan Printer", maxLines = 1)
                      }
                    }
                  }
                } else {
                  Column(
                    modifier =
                      Modifier.fillMaxWidth()
                        .heightIn(max = 172.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    state.devices.forEach { device ->
                      val chosen = selectedAddress == device.address
                      Surface(
                        shape = RoundedCornerShape(12.dp),
                        color =
                          if (chosen) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                          else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border =
                          BorderStroke(
                            width = if (chosen) 1.5.dp else 1.dp,
                            color =
                              if (chosen) MaterialTheme.colorScheme.primary
                              else MaterialTheme.colorScheme.outlineVariant,
                          ),
                        modifier =
                          Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = !isPrintingBluetooth) {
                              selectedAddress = device.address
                            },
                      ) {
                        Row(
                          modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                          verticalAlignment = Alignment.CenterVertically,
                        ) {
                          RadioButton(
                            selected = chosen,
                            onClick = { if (!isPrintingBluetooth) selectedAddress = device.address },
                          )
                          Spacer(modifier = Modifier.width(4.dp))
                          Column(modifier = Modifier.weight(1f)) {
                            Text(
                              text = device.name,
                              style = MaterialTheme.typography.bodySmall,
                              fontWeight = FontWeight.Bold,
                              maxLines = 1,
                              overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                              text = device.address,
                              fontFamily = JetBrainsMono,
                              style = MaterialTheme.typography.labelSmall,
                              color = MaterialTheme.colorScheme.onSurfaceVariant,
                              maxLines = 1,
                            )
                          }
                        }
                      }
                    }
                  }
                }
              }
            }

            if (statusBanner != null) {
              val (ok, msg) = statusBanner!!
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (ok) emeraldBg else dangerFg.copy(alpha = 0.12f),
                border =
                  BorderStroke(
                    1.dp,
                    if (ok) emeraldFg.copy(alpha = 0.35f) else dangerFg.copy(alpha = 0.35f),
                  ),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier.padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Icon(
                    imageVector = if (ok) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = if (ok) emeraldFg else dangerFg,
                    modifier = Modifier.size(16.dp),
                  )
                  Text(
                    text = msg,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (ok) emeraldFg else dangerFg,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                  )
                }
              }
            }

            if (printersState is BluetoothPrintersState.Ready && readyDevices.isNotEmpty()) {
              Button(
                onClick = {
                  val chosenDevice = readyDevices.firstOrNull { it.address == selectedAddress }
                  if (chosenDevice == null) {
                    statusBanner = false to "Pilih printer Bluetooth terlebih dahulu."
                    return@Button
                  }
                  isPrintingBluetooth = true
                  statusBanner = null
                  onSavePrintPreferences(selectedSize, chosenDevice.address, chosenDevice.name)
                  scope.launch {
                    val bytes =
                      ReceiptHelper.buildEscPosBytes(
                        tx = transaction,
                        settings = settings,
                        isSuratJalan = isSuratJalan,
                        paperSize = selectedSize,
                      )
                    val res = BluetoothPrinterHelper.printEscPos(context, chosenDevice.address, bytes)
                    isPrintingBluetooth = false
                    res
                      .onSuccess {
                        statusBanner =
                          true to "Berhasil dicetak ke ${chosenDevice.name} (${selectedSize.label})."
                      }
                      .onFailure { e ->
                        statusBanner =
                          false to (e.message ?: "Gagal mencetak ke printer Bluetooth.")
                      }
                  }
                },
                enabled = !isPrintingBluetooth && selectedAddress.isNotBlank(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                shape = RoundedCornerShape(14.dp),
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Orange600 else Navy900,
                    contentColor = Color.White,
                  ),
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("print_bluetooth_button"),
              ) {
                if (isPrintingBluetooth) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = Color.White,
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "Mengirim ke Printer...",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                  )
                } else {
                  Icon(
                    imageVector = Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "Cetak Bluetooth · ${selectedSize.label}",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }
          }
        } else {
          // System Print / PDF Mode
          Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Text(
                  text = "Cetak melalui layanan sistem Android",
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = "Mendukung printer Wi-Fi, USB, aplikasi layanan cetak (seperti RawBT), atau simpan sebagai file PDF ukuran ${selectedSize.label}.",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = orangeBg,
                ) {
                  Text(
                    text =
                      "${if (isSuratJalan) "Surat Jalan" else "Struk Thermal"} · ${selectedSize.label} (${selectedSize.charColumns} kolom)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = orangeFg,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                  )
                }
              }
            }

            Button(
              onClick = {
                onSavePrintPreferences(selectedSize, null, null)
                ReceiptHelper.printReceipt(
                  context = context,
                  tx = transaction,
                  settings = settings,
                  isSuratJalan = isSuratJalan,
                  paperSize = selectedSize,
                )
                onDismiss()
              },
              enabled = !isPrintingBluetooth,
              contentPadding = PaddingValues(horizontal = 16.dp),
              shape = RoundedCornerShape(14.dp),
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = if (isDark) Orange600 else Navy900,
                  contentColor = Color.White,
                ),
              modifier = Modifier.fillMaxWidth().height(50.dp).testTag("print_system_button"),
            ) {
              Icon(
                imageVector = Icons.Default.Print,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Cetak / Simpan PDF · ${selectedSize.label}",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }
    }
  }
}
