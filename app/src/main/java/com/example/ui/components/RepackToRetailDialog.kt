package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ProductEntity
import com.example.data.model.retailUnitsPerBase
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Emerald600
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.Orange100
import com.example.ui.theme.Orange400
import com.example.ui.theme.Orange500
import com.example.ui.theme.Orange600
import com.example.ui.theme.Red400
import com.example.ui.theme.Red600
import com.example.util.formatQty

@Composable
fun RepackToRetailDialog(
  product: ProductEntity,
  maxBaseAvailable: Double = product.stockQuantity,
  onDismiss: () -> Unit,
  onConfirm: (Double) -> Unit,
) {
  val isDark = isSystemInDarkTheme()
  val orangeBg = if (isDark) Orange500.copy(alpha = 0.18f) else Orange100
  val orangeFg = if (isDark) Orange400 else Orange600
  val emeraldBg = if (isDark) Emerald600.copy(alpha = 0.18f) else Emerald100
  val emeraldFg = if (isDark) Emerald400 else Emerald600
  val dangerFg = if (isDark) Red400 else Red600

  val retailUnit = product.retailUnitName?.takeIf { it.isNotBlank() } ?: "Eceran"
  val unitsPerBase = remember(product.retailConversionFactor) {
    retailUnitsPerBase(product.retailConversionFactor)
  }
  val defaultQty = if (maxBaseAvailable >= 1.0) "1" else formatQty(maxBaseAvailable.coerceAtLeast(0.0))
  var qtyText by remember(product.id) { mutableStateOf(defaultQty) }
  var error by remember { mutableStateOf<String?>(null) }

  val parsedQty = qtyText.replace(',', '.').toDoubleOrNull() ?: 0.0
  val isValidQty = parsedQty > 0.0 && parsedQty <= maxBaseAvailable + 1e-6
  val addedRetail =
    if (isValidQty) kotlin.math.round(parsedQty * unitsPerBase * 100.0) / 100.0 else 0.0
  val remainingBaseAfter =
    if (isValidQty) kotlin.math.round((product.stockQuantity - parsedQty).coerceAtLeast(0.0) * 1000.0) / 1000.0
    else product.stockQuantity
  val totalRetailAfter =
    if (isValidQty) kotlin.math.round((product.retailStockQuantity + addedRetail) * 1000.0) / 1000.0
    else product.retailStockQuantity

  val presets = listOf(0.5, 1.0, 2.0, 5.0, 10.0).filter { it <= maxBaseAvailable + 1e-6 }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = orangeBg,
          modifier = Modifier.size(38.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.SwapHoriz,
              contentDescription = null,
              tint = orangeFg,
              modifier = Modifier.size(21.dp),
            )
          }
        }
        Column {
          Text(
            text = "Ecer / Buka Stok",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = product.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        // Real-time current dual-pool stock summary
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Stok ${product.baseUnit}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = "${formatQty(product.stockQuantity)} ${product.baseUnit}",
                fontFamily = JetBrainsMono,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
              )
            }
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForward,
              contentDescription = null,
              tint = orangeFg,
              modifier = Modifier.padding(horizontal = 8.dp).size(18.dp),
            )
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
              Text(
                text = "Stok Eceran",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = "${formatQty(product.retailStockQuantity)} $retailUnit",
                fontFamily = JetBrainsMono,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = emeraldFg,
              )
            }
          }
        }

        Surface(
          shape = RoundedCornerShape(10.dp),
          color = orangeBg,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = "Konversi: 1 ${product.baseUnit} = ${formatQty(unitsPerBase)} $retailUnit",
            fontFamily = JetBrainsMono,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = orangeFg,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
          )
        }

        if (presets.isNotEmpty()) {
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            presets.forEach { preset ->
              val isSelected = kotlin.math.abs(parsedQty - preset) <= 1e-6
              FilterChip(
                selected = isSelected,
                onClick = {
                  qtyText = formatQty(preset)
                  error = null
                },
                label = {
                  Text(
                    text = "${formatQty(preset)} ${product.baseUnit}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                  )
                },
                colors =
                  FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Orange600,
                    selectedLabelColor = Color.White,
                  ),
              )
            }
          }
        }

        OutlinedTextField(
          value = qtyText,
          onValueChange = {
            qtyText = it
            error = null
          },
          label = { Text("Jumlah ${product.baseUnit} yang diecer") },
          supportingText = {
            Text("Maksimal tersedia: ${formatQty(maxBaseAvailable)} ${product.baseUnit}")
          },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().testTag("repack_qty_input"),
        )

        if (isValidQty) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = emeraldBg,
            border = BorderStroke(1.dp, emeraldFg.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Text(
                text = "Hasil Setelah Diecer:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = emeraldFg,
              )
              Text(
                text =
                  "• Stok ${product.baseUnit}: ${formatQty(product.stockQuantity)} → ${formatQty(remainingBaseAfter)} ${product.baseUnit}",
                fontFamily = JetBrainsMono,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text =
                  "• Stok $retailUnit: ${formatQty(product.retailStockQuantity)} → ${formatQty(totalRetailAfter)} $retailUnit (+${formatQty(addedRetail)})",
                fontFamily = JetBrainsMono,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = emeraldFg,
              )
            }
          }
        }

        if (error != null) {
          Text(
            text = error!!,
            color = dangerFg,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          when {
            parsedQty <= 0.0 -> error = "Masukkan jumlah ${product.baseUnit} lebih dari 0."
            parsedQty > maxBaseAvailable + 1e-6 ->
              error = "Jumlah melebihi sisa stok ${product.baseUnit} yang tersedia."
            else -> onConfirm(parsedQty)
          }
        },
        enabled = maxBaseAvailable > 0.0,
        colors = ButtonDefaults.buttonColors(containerColor = Orange600, contentColor = Color.White),
        modifier = Modifier.testTag("confirm_repack_button"),
      ) {
        Text("Pecah ke Eceran", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Batal") }
    },
  )
}
