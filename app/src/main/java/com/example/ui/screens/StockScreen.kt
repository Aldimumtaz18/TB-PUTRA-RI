package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import com.example.data.model.ALL_CATEGORY_LABEL
import com.example.data.model.CategoryItem
import com.example.data.model.UserRole
import com.example.data.model.conversionFactorFromUnitsPerBase
import com.example.data.model.retailUnitsPerBase
import com.example.ui.components.CategoryFilterRow
import com.example.ui.components.RepackToRetailDialog
import com.example.ui.components.StoreSearchBar
import com.example.ui.theme.Amber100
import com.example.ui.theme.Amber400
import com.example.ui.theme.Amber600
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Emerald600
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.Navy100
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.Orange100
import com.example.ui.theme.Orange400
import com.example.ui.theme.Orange500
import com.example.ui.theme.Orange600
import com.example.ui.theme.Red100
import com.example.ui.theme.Red400
import com.example.ui.theme.Red600
import com.example.ui.viewmodel.StoreUiState
import com.example.util.formatQty
import com.example.util.formatRupiah

@Composable
fun StockScreen(
  uiState: StoreUiState,
  onSearchChange: (String) -> Unit,
  onCategorySelect: (String) -> Unit,
  onSaveProduct: (ProductEntity) -> Unit,
  onDeleteProduct: (Long) -> Unit,
  onRestockProduct: (ProductEntity, Double, Long, Boolean, Boolean) -> Unit,
  onRepackToRetail: (ProductEntity, Double) -> Unit = { _, _ -> },
  onSaveCategory: (CategoryItem?, String) -> Unit,
  onDeleteCategory: (CategoryItem, String?) -> Unit,
  modifier: Modifier = Modifier,
) {
  val isOwner = uiState.settings.activeRole == UserRole.OWNER
  var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
  var showAddDialog by remember { mutableStateOf(false) }
  var showCategoryManager by remember { mutableStateOf(false) }
  var restockingProduct by remember { mutableStateOf<ProductEntity?>(null) }
  var repackingProduct by remember { mutableStateOf<ProductEntity?>(null) }
  var deletingProduct by remember { mutableStateOf<ProductEntity?>(null) }

  val filteredProducts =
    remember(uiState.products, uiState.stockSearchQuery, uiState.stockSelectedCategory) {
      uiState.products.filter { product ->
        val matchesCategory =
          uiState.stockSelectedCategory == ALL_CATEGORY_LABEL ||
            product.category.equals(uiState.stockSelectedCategory, ignoreCase = true)
        val matchesQuery =
          uiState.stockSearchQuery.isBlank() ||
            product.name.contains(uiState.stockSearchQuery, ignoreCase = true) ||
            product.category.contains(uiState.stockSearchQuery, ignoreCase = true)
        matchesCategory && matchesQuery
      }
    }

  val isDark = isSystemInDarkTheme()
  val emeraldBg = if (isDark) Emerald400.copy(alpha = 0.14f) else Emerald100
  val emeraldFg = if (isDark) Emerald400 else Emerald600
  val redBg = if (isDark) Red400.copy(alpha = 0.14f) else Red100
  val redFg = if (isDark) Red400 else Red600
  val orangeBg = if (isDark) Orange400.copy(alpha = 0.14f) else Orange100
  val orangeFg = if (isDark) Orange400 else Orange600
  val amberBg = if (isDark) Amber400.copy(alpha = 0.14f) else Amber100
  val amberFg = if (isDark) Amber400 else Amber600

  @Composable
  fun StockBannerCard() {
    if (!isOwner) {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Mode Kasir: Anda dapat melihat detail stok & memecah stok utama ke eceran.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
          )
        }
      }
    } else if (uiState.lowStockCount > 0) {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        border = BorderStroke(1.dp, redFg.copy(alpha = 0.35f)),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "${uiState.lowStockCount} barang stok di bawah batas minimum. Segera kulakan!",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
  }

  Box(modifier = modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
      Spacer(modifier = Modifier.height(10.dp))

      if (isOwner) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          StoreSearchBar(
            query = uiState.stockSearchQuery,
            onQueryChange = onSearchChange,
            placeholder = "Cari nama barang...",
            modifier = Modifier.weight(1f),
            testTag = "stock_search_input",
          )
          OutlinedButton(
            onClick = { showCategoryManager = true },
            modifier = Modifier.height(48.dp).testTag("manage_categories_button"),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            colors =
              ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
              ),
            contentPadding = PaddingValues(horizontal = 12.dp),
          ) {
            Icon(
              imageVector = Icons.Default.Category,
              contentDescription = "Kelola kategori barang",
              tint = Orange600,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text = "Kategori",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
            )
          }
        }
      } else {
        StoreSearchBar(
          query = uiState.stockSearchQuery,
          onQueryChange = onSearchChange,
          placeholder = "Cari nama barang...",
          testTag = "stock_search_input",
        )
      }
      Spacer(modifier = Modifier.height(8.dp))
      CategoryFilterRow(
        categories = uiState.categoryNames,
        selectedCategory = uiState.stockSelectedCategory,
        onSelectCategory = onCategorySelect,
      )
      Spacer(modifier = Modifier.height(8.dp))

      if (filteredProducts.isEmpty()) {
        if (!isOwner || uiState.lowStockCount > 0) {
          StockBannerCard()
          Spacer(modifier = Modifier.height(8.dp))
        }
        Box(
          modifier = Modifier.fillMaxSize().padding(bottom = 80.dp),
          contentAlignment = Alignment.Center,
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector = Icons.Default.Warning,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(48.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Barang stok tidak ditemukan",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "Coba ubah pencarian atau kategori produk.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      } else {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
          val columns = if (maxWidth >= 660.dp) 2 else 1
          LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = if (isOwner) 88.dp else 20.dp),
          ) {
            if (!isOwner || uiState.lowStockCount > 0) {
              item(key = "stock_banner", span = { GridItemSpan(maxLineSpan) }) {
                StockBannerCard()
              }
            }
            items(filteredProducts, key = { it.id }) { product ->
              val isLow = product.stockQuantity < product.minStock
              val hasRetail = !product.retailUnitName.isNullOrBlank() && product.retailSellPrice > 0L
              val baseStockBg = if (isLow) redBg else emeraldBg
              val baseStockFg = if (isLow) redFg else emeraldFg
              val retailEmpty = product.retailStockQuantity <= 0.0001
              val retailStockBg = if (retailEmpty) amberBg else orangeBg
              val retailStockFg = if (retailEmpty) amberFg else orangeFg

              Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border =
                  BorderStroke(
                    1.dp,
                    if (isLow) redFg.copy(alpha = 0.55f)
                    else MaterialTheme.colorScheme.outlineVariant,
                  ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
              ) {
                Column(
                  modifier = Modifier.fillMaxWidth().padding(14.dp),
                  verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  // Header: category pill + optional low-stock badge (never crammed with stock pills)
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Surface(
                      shape = RoundedCornerShape(8.dp),
                      color =
                        if (isDark) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        else Navy100,
                      modifier = Modifier.weight(1f, fill = false),
                    ) {
                      Text(
                        text = product.category,
                        style = MaterialTheme.typography.labelSmall,
                        color =
                          if (isDark) MaterialTheme.colorScheme.onPrimaryContainer else Navy900,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                      )
                    }

                    if (isLow) {
                      Spacer(modifier = Modifier.width(8.dp))
                      Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = redBg,
                        border = BorderStroke(1.dp, redFg.copy(alpha = 0.35f)),
                      ) {
                        Row(
                          verticalAlignment = Alignment.CenterVertically,
                          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                          Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Stok tipis",
                            tint = redFg,
                            modifier = Modifier.size(12.dp),
                          )
                          Spacer(modifier = Modifier.width(4.dp))
                          Text(
                            text = "Stok Tipis",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = redFg,
                          )
                        }
                      }
                    }
                  }

                  Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                  )

                  // Real-time stock boxes: crisp 2-column split when retail is enabled
                  if (hasRetail) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = baseStockBg,
                        border = BorderStroke(1.dp, baseStockFg.copy(alpha = 0.3f)),
                      ) {
                        Column(
                          modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                          verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                          Text(
                            text = "Stok Utama",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                          )
                          Text(
                            text = "${formatQty(product.stockQuantity)} ${product.baseUnit}",
                            fontFamily = JetBrainsMono,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = baseStockFg,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                          )
                        }
                      }

                      Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = retailStockBg,
                        border = BorderStroke(1.dp, retailStockFg.copy(alpha = 0.3f)),
                      ) {
                        Column(
                          modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                          verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                          Text(
                            text = "Stok Eceran",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                          )
                          Text(
                            text = "${formatQty(product.retailStockQuantity)} ${product.retailUnitName}",
                            fontFamily = JetBrainsMono,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = retailStockFg,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                          )
                        }
                      }
                    }
                  } else {
                    Surface(
                      modifier = Modifier.fillMaxWidth(),
                      shape = RoundedCornerShape(10.dp),
                      color = baseStockBg,
                      border = BorderStroke(1.dp, baseStockFg.copy(alpha = 0.3f)),
                    ) {
                      Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                      ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                          Text(
                            text = "Stok Tersedia",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                          )
                          Text(
                            text = "${formatQty(product.stockQuantity)} ${product.baseUnit}",
                            fontFamily = JetBrainsMono,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = baseStockFg,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                          )
                        }
                        Text(
                          text = "Min. ${formatQty(product.minStock)} ${product.baseUnit}",
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                      }
                    }
                  }

                  // Clean aligned price, conversion, and minimum stock summary
                  Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                  ) {
                    Column(
                      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                      verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                      val minBase = product.effectiveMinBaseSellPrice()
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                      ) {
                        Text(
                          text = "Harga Standar (${product.baseUnit})",
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                          text = formatRupiah(product.sellPricePerBaseUnit),
                          fontFamily = JetBrainsMono,
                          style = MaterialTheme.typography.bodyMedium,
                          fontWeight = FontWeight.Bold,
                          color = MaterialTheme.colorScheme.onSurface,
                        )
                      }
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                      ) {
                        Text(
                          text = "Harga Minimal (${product.baseUnit})",
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                          text =
                            if (minBase < product.sellPricePerBaseUnit) formatRupiah(minBase)
                            else "Harga Pas",
                          fontFamily = JetBrainsMono,
                          style = MaterialTheme.typography.labelMedium,
                          fontWeight = FontWeight.SemiBold,
                          color =
                            if (minBase < product.sellPricePerBaseUnit) emeraldFg
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                      }

                      if (hasRetail) {
                        val perBase = retailUnitsPerBase(product.retailConversionFactor)
                        val minRetail = product.effectiveMinRetailSellPrice()
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically,
                        ) {
                          Text(
                            text = "Ecer Standar (${product.retailUnitName})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                          )
                          Text(
                            text = formatRupiah(product.retailSellPrice),
                            fontFamily = JetBrainsMono,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = orangeFg,
                          )
                        }
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically,
                        ) {
                          Text(
                            text = "Ecer Minimal (${product.retailUnitName})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                          )
                          Text(
                            text =
                              if (minRetail < product.retailSellPrice) formatRupiah(minRetail)
                              else "Harga Pas",
                            fontFamily = JetBrainsMono,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color =
                              if (minRetail < product.retailSellPrice) emeraldFg
                              else MaterialTheme.colorScheme.onSurfaceVariant,
                          )
                        }
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically,
                        ) {
                          Text(
                            text = "Konversi Ecer",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                          )
                          Text(
                            text = "1 ${product.baseUnit} = ${formatQty(perBase)} ${product.retailUnitName}",
                            fontFamily = JetBrainsMono,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                          )
                        }
                      }

                      HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                      )

                      if (isOwner) {
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically,
                        ) {
                          Text(
                            text = "Modal: ${formatRupiah(product.costPricePerBaseUnit)}",
                            fontFamily = JetBrainsMono,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                          )
                          Text(
                            text = "Min: ${formatQty(product.minStock)} ${product.baseUnit}",
                            fontFamily = JetBrainsMono,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isLow) redFg else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isLow) FontWeight.Bold else FontWeight.Normal,
                          )
                        }
                      } else {
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically,
                        ) {
                          Text(
                            text = "Batas minimum stok",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                          )
                          Text(
                            text =
                              "${formatQty(product.minStock)} ${product.baseUnit}" +
                                if (isLow) " • Perlu Kulakan" else "",
                            fontFamily = JetBrainsMono,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isLow) redFg else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isLow) FontWeight.Bold else FontWeight.Normal,
                          )
                        }
                      }
                    }
                  }

                  if (hasRetail) {
                    val canRepack = product.stockQuantity > 0.0
                    OutlinedButton(
                      onClick = { repackingProduct = product },
                      enabled = canRepack,
                      shape = RoundedCornerShape(12.dp),
                      border =
                        BorderStroke(
                          1.dp,
                          if (canRepack) orangeFg.copy(alpha = 0.7f)
                          else MaterialTheme.colorScheme.outlineVariant,
                        ),
                      colors =
                        ButtonDefaults.outlinedButtonColors(
                          containerColor = if (canRepack) orangeBg.copy(alpha = 0.5f) else Color.Transparent,
                          contentColor = orangeFg,
                        ),
                      modifier =
                        Modifier.fillMaxWidth()
                          .heightIn(min = 48.dp)
                          .testTag("repack_btn_${product.id}"),
                    ) {
                      Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        text = "Ecer Stok (${product.baseUnit} → ${product.retailUnitName})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                    }
                  }

                  if (isOwner) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Button(
                        onClick = { restockingProduct = product },
                        modifier =
                          Modifier.weight(1f).height(48.dp).testTag("restock_btn_${product.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors =
                          ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Navy800 else Navy900,
                            contentColor = Color.White,
                          ),
                      ) {
                        Icon(
                          Icons.Default.AddShoppingCart,
                          contentDescription = null,
                          modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Kulakan / Restok", style = MaterialTheme.typography.labelMedium)
                      }

                      OutlinedButton(
                        onClick = { editingProduct = product },
                        modifier = Modifier.height(48.dp).testTag("edit_product_${product.id}"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                      ) {
                        Icon(
                          Icons.Default.Edit,
                          contentDescription = "Edit barang",
                          modifier = Modifier.size(18.dp),
                        )
                      }

                      IconButton(
                        onClick = { deletingProduct = product },
                        modifier = Modifier.size(48.dp).testTag("delete_product_${product.id}"),
                      ) {
                        Icon(
                          Icons.Default.Delete,
                          contentDescription = "Hapus barang",
                          tint = redFg,
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    if (isOwner) {
      FloatingActionButton(
        onClick = { showAddDialog = true },
        containerColor = Orange600,
        contentColor = Color.White,
        modifier =
          Modifier.align(Alignment.BottomEnd)
            .padding(16.dp)
            .testTag("add_product_fab"),
      ) {
        Icon(Icons.Default.Add, contentDescription = "Tambah barang baru")
      }
    }
  }

  if (showCategoryManager && isOwner) {
    CategoryManagementDialog(
      categories = uiState.categories,
      products = uiState.products,
      onDismiss = { showCategoryManager = false },
      onSaveCategory = onSaveCategory,
      onDeleteCategory = onDeleteCategory,
    )
  }

  if (showAddDialog || editingProduct != null) {
    ProductFormDialog(
      existing = editingProduct,
      categoryOptions = uiState.categoryNames,
      onQuickAddCategory = { newCat -> onSaveCategory(null, newCat) },
      onDismiss = {
        showAddDialog = false
        editingProduct = null
      },
      onSave = { product ->
        onSaveProduct(product)
        showAddDialog = false
        editingProduct = null
      },
    )
  }

  restockingProduct?.let { target ->
    RestockDialog(
      product = target,
      onDismiss = { restockingProduct = null },
      onConfirm = { addedQty, totalCost, deductCash, updateCostPrice ->
        onRestockProduct(target, addedQty, totalCost, deductCash, updateCostPrice)
        restockingProduct = null
      },
    )
  }

  repackingProduct?.let { target ->
    val liveProduct = uiState.products.find { it.id == target.id } ?: target
    RepackToRetailDialog(
      product = liveProduct,
      onDismiss = { repackingProduct = null },
      onConfirm = { baseQty ->
        onRepackToRetail(liveProduct, baseQty)
        repackingProduct = null
      },
    )
  }

  deletingProduct?.let { target ->
    AlertDialog(
      onDismissRequest = { deletingProduct = null },
      title = { Text("Hapus Barang?") },
      text = { Text("Yakin ingin menghapus '${target.name}' dari daftar stok?") },
      confirmButton = {
        Button(
          onClick = {
            onDeleteProduct(target.id)
            deletingProduct = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = Red600),
        ) {
          Text("Hapus")
        }
      },
      dismissButton = {
        TextButton(onClick = { deletingProduct = null }) { Text("Batal") }
      },
    )
  }
}

@Composable
private fun ProductFormDialog(
  existing: ProductEntity?,
  categoryOptions: List<String>,
  onQuickAddCategory: (String) -> Unit,
  onDismiss: () -> Unit,
  onSave: (ProductEntity) -> Unit,
) {
  val categories =
    remember(categoryOptions, existing) {
      val base = categoryOptions.toMutableList()
      val existingCat = existing?.category?.trim().orEmpty()
      if (existingCat.isNotBlank() && base.none { it.equals(existingCat, ignoreCase = true) }) {
        base.add(existingCat)
      }
      base.ifEmpty { listOf("Umum") }
    }
  var name by remember { mutableStateOf(existing?.name ?: "") }
  var category by remember { mutableStateOf(existing?.category ?: categories.first()) }
  var showQuickCategoryInput by remember { mutableStateOf(false) }
  var quickCategoryText by remember { mutableStateOf("") }
  var baseUnit by remember { mutableStateOf(existing?.baseUnit ?: "Sak") }
  var stockText by remember {
    mutableStateOf(if (existing != null) formatQty(existing.stockQuantity) else "0")
  }
  var minStockText by remember {
    mutableStateOf(if (existing != null) formatQty(existing.minStock) else "5")
  }
  var costText by remember { mutableStateOf(existing?.costPricePerBaseUnit?.toString() ?: "") }
  var sellText by remember { mutableStateOf(existing?.sellPricePerBaseUnit?.toString() ?: "") }
  var minSellText by remember {
    mutableStateOf(
      if (existing != null && existing.effectiveMinBaseSellPrice() > 0L) {
        existing.effectiveMinBaseSellPrice().toString()
      } else {
        ""
      }
    )
  }
  var enableRetail by remember { mutableStateOf(!existing?.retailUnitName.isNullOrBlank()) }
  var retailUnit by remember { mutableStateOf(existing?.retailUnitName ?: "") }
  var unitsPerBaseText by remember {
    mutableStateOf(
      if (existing != null && !existing.retailUnitName.isNullOrBlank()) {
        formatQty(retailUnitsPerBase(existing.retailConversionFactor))
      } else {
        "50"
      }
    )
  }
  var retailStockText by remember {
    mutableStateOf(if (existing != null) formatQty(existing.retailStockQuantity) else "0")
  }
  var retailPriceText by remember {
    mutableStateOf(
      if (existing != null && existing.retailSellPrice > 0L) existing.retailSellPrice.toString()
      else ""
    )
  }
  var minRetailPriceText by remember {
    mutableStateOf(
      if (existing != null && existing.retailSellPrice > 0L && existing.effectiveMinRetailSellPrice() > 0L) {
        existing.effectiveMinRetailSellPrice().toString()
      } else {
        ""
      }
    )
  }
  var error by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (existing == null) "Tambah Barang Baru" else "Edit Barang") },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Nama Barang") },
          singleLine = true,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("Kategori", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
          TextButton(
            onClick = { showQuickCategoryInput = !showQuickCategoryInput },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
          ) {
            Text(
              text = if (showQuickCategoryInput) "Tutup" else "+ Kategori Baru",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = Orange600,
            )
          }
        }
        if (showQuickCategoryInput) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            OutlinedTextField(
              value = quickCategoryText,
              onValueChange = { quickCategoryText = it.take(60) },
              label = { Text("Nama kategori baru") },
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
            Button(
              onClick = {
                val clean = quickCategoryText.trim()
                if (clean.isNotBlank()) {
                  onQuickAddCategory(clean)
                  category = clean
                  quickCategoryText = ""
                  showQuickCategoryInput = false
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = Orange600, contentColor = Color.White),
              modifier = Modifier.height(48.dp),
            ) {
              Text("Buat")
            }
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          categories.forEach { catName ->
            val isSelected = category.equals(catName, ignoreCase = true)
            FilterChip(
              selected = isSelected,
              onClick = { category = catName },
              label = { Text(catName, style = MaterialTheme.typography.labelSmall) },
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = Orange600,
                  selectedLabelColor = Color.White,
                ),
            )
          }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = baseUnit,
            onValueChange = { baseUnit = it },
            label = { Text("Satuan Utama") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = stockText,
            onValueChange = { stockText = it },
            label = { Text("Stok Awal") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = minStockText,
            onValueChange = { minStockText = it },
            label = { Text("Min. Stok") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = costText,
            onValueChange = { costText = it.filter { c -> c.isDigit() } },
            label = { Text("Modal (HPP)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = sellText,
            onValueChange = { sellText = it.filter { c -> c.isDigit() } },
            label = { Text("Harga Standar (Rp)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("product_sell_standard_input"),
          )
          OutlinedTextField(
            value = minSellText,
            onValueChange = { minSellText = it.filter { c -> c.isDigit() } },
            label = { Text("Harga Minimal (Rp)") },
            placeholder = { Text(sellText.ifBlank { "Harga pas" }) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("product_sell_min_input"),
          )
        }
        Text(
          text = "Kosongkan harga minimal jika harga pas (tidak bisa ditawar per barang).",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text("Aktifkan Satuan Eceran", style = MaterialTheme.typography.labelLarge)
          Switch(checked = enableRetail, onCheckedChange = { enableRetail = it })
        }

        if (enableRetail) {
          val baseLabel = baseUnit.trim().ifBlank { "Utama" }
          val retailLabel = retailUnit.trim().ifBlank { "Eceran" }
          val parsedUnitsPreview = unitsPerBaseText.replace(',', '.').toDoubleOrNull() ?: 0.0
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = retailUnit,
              onValueChange = { retailUnit = it },
              label = { Text("Satuan (Kg/m)") },
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
              value = unitsPerBaseText,
              onValueChange = { unitsPerBaseText = it },
              label = { Text("Isi per 1 $baseLabel") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
          }
          OutlinedTextField(
            value = retailStockText,
            onValueChange = { retailStockText = it },
            label = { Text("Stok Awal $retailLabel") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = retailPriceText,
              onValueChange = { retailPriceText = it.filter { c -> c.isDigit() } },
              label = { Text("Ecer Standar (Rp)") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.weight(1f).testTag("product_retail_standard_input"),
            )
            OutlinedTextField(
              value = minRetailPriceText,
              onValueChange = { minRetailPriceText = it.filter { c -> c.isDigit() } },
              label = { Text("Ecer Minimal (Rp)") },
              placeholder = { Text(retailPriceText.ifBlank { "Harga pas" }) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.weight(1f).testTag("product_retail_min_input"),
            )
          }
          if (parsedUnitsPreview > 0.0) {
            Text(
              text = "Konversi: 1 $baseLabel = ${formatQty(parsedUnitsPreview)} $retailLabel",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.secondary,
              fontWeight = FontWeight.SemiBold,
            )
          }
        }

        if (error != null) {
          Text(error!!, color = Red600, style = MaterialTheme.typography.labelSmall)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val stock = stockText.replace(',', '.').toDoubleOrNull()
          val minStock = minStockText.replace(',', '.').toDoubleOrNull()
          val cost = costText.toLongOrNull()
          val sell = sellText.toLongOrNull()
          val minSell = if (minSellText.isBlank()) sell else minSellText.toLongOrNull()
          val unitsPerBase = unitsPerBaseText.replace(',', '.').toDoubleOrNull() ?: 0.0
          val retailStock = retailStockText.replace(',', '.').toDoubleOrNull() ?: 0.0
          val retailSell = retailPriceText.toLongOrNull() ?: 0L
          val minRetailSell =
            if (minRetailPriceText.isBlank()) retailSell else (minRetailPriceText.toLongOrNull() ?: -1L)

          when {
            name.isBlank() || baseUnit.isBlank() -> error = "Nama dan satuan utama wajib diisi"
            stock == null || stock < 0.0 || minStock == null || cost == null || sell == null || sell <= 0L ->
              error = "Periksa angka stok, modal, dan harga jual standar"
            minSell == null || minSell <= 0L || minSell > sell ->
              error = "Harga minimal satuan utama harus antara Rp 1 s/d ${formatRupiah(sell)}"
            enableRetail && (retailUnit.isBlank() || retailSell <= 0L || unitsPerBase <= 0.0 || retailStock < 0.0) ->
              error = "Lengkapi satuan eceran, isi per satuan utama, stok eceran, & harga ecer standar"
            enableRetail && (minRetailSell <= 0L || minRetailSell > retailSell) ->
              error = "Harga minimal eceran harus antara Rp 1 s/d ${formatRupiah(retailSell)}"
            else -> {
              onSave(
                ProductEntity(
                  id = existing?.id ?: 0L,
                  name = name.trim(),
                  category = category,
                  baseUnit = baseUnit.trim(),
                  stockQuantity = stock,
                  minStock = minStock,
                  costPricePerBaseUnit = cost,
                  sellPricePerBaseUnit = sell,
                  minSellPricePerBaseUnit = minSell,
                  retailUnitName = if (enableRetail) retailUnit.trim() else null,
                  retailConversionFactor =
                    if (enableRetail) conversionFactorFromUnitsPerBase(unitsPerBase) else 1.0,
                  retailSellPrice = if (enableRetail) retailSell else 0L,
                  minRetailSellPrice = if (enableRetail) minRetailSell else 0L,
                  retailStockQuantity = if (enableRetail) retailStock else 0.0,
                  updatedAt = System.currentTimeMillis(),
                )
              )
            }
          }
        },
        modifier = Modifier.testTag("save_product_button"),
      ) {
        Text("Simpan")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
  )
}

@Composable
private fun RestockDialog(
  product: ProductEntity,
  onDismiss: () -> Unit,
  onConfirm: (Double, Long, Boolean, Boolean) -> Unit,
) {
  var qtyText by remember { mutableStateOf("10") }
  var deductFromCash by remember { mutableStateOf(true) }
  var updateCostPrice by remember { mutableStateOf(false) }
  var costText by remember {
    mutableStateOf((10 * product.costPricePerBaseUnit).toString())
  }

  val parsedQty = qtyText.replace(',', '.').toDoubleOrNull() ?: 0.0
  val parsedCost = costText.toLongOrNull() ?: 0L
  val previewNewAvgCost =
    if (parsedQty > 0.0 && parsedCost > 0L) {
      val existingStock = product.stockQuantity.coerceAtLeast(0.0)
      val totalStock = existingStock + parsedQty
      val totalValue = existingStock * product.costPricePerBaseUnit.coerceAtLeast(0L).toDouble() + parsedCost.toDouble()
      if (totalStock > 0.0) kotlin.math.round(totalValue / totalStock).toLong().coerceAtLeast(0L)
      else product.costPricePerBaseUnit
    } else {
      product.costPricePerBaseUnit
    }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Kulakan / Restok Barang") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text =
            "${product.name}\nStok saat ini: ${formatQty(product.stockQuantity)} ${product.baseUnit} • Modal saat ini: ${formatRupiah(product.costPricePerBaseUnit)}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
          value = qtyText,
          onValueChange = { next ->
            qtyText = next
            val q = next.replace(',', '.').toDoubleOrNull()
            if (q != null) {
              costText = kotlin.math.round(q * product.costPricePerBaseUnit).toLong().toString()
            }
          },
          label = { Text("Tambah Stok (${product.baseUnit})") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = costText,
          onValueChange = { costText = it.filter { c -> c.isDigit() } },
          label = { Text("Total Harga Beli / Kulakan (Rp)") },
          prefix = { Text("Rp ") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(checked = deductFromCash, onCheckedChange = { deductFromCash = it })
          Text(
            text = "Catat sebagai uang keluar kulakan (Potong kas)",
            style = MaterialTheme.typography.bodySmall,
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(checked = updateCostPrice, onCheckedChange = { updateCostPrice = it })
          Text(
            text = "Perbarui harga modal barang (rata-rata stok)",
            style = MaterialTheme.typography.bodySmall,
          )
        }
        if (updateCostPrice && parsedQty > 0.0 && parsedCost > 0L) {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = Orange100,
          ) {
            Text(
              text =
                "Modal baru (${product.baseUnit}): ${formatRupiah(product.costPricePerBaseUnit)} → ${formatRupiah(previewNewAvgCost)}",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = Orange600,
              modifier = Modifier.padding(10.dp),
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (parsedQty > 0.0) {
            onConfirm(parsedQty, parsedCost, deductFromCash, updateCostPrice)
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = Navy900, contentColor = Color.White),
      ) {
        Text("Simpan Restok")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
  )
}

@Composable
private fun CategoryManagementDialog(
  categories: List<CategoryItem>,
  products: List<ProductEntity>,
  onDismiss: () -> Unit,
  onSaveCategory: (CategoryItem?, String) -> Unit,
  onDeleteCategory: (CategoryItem, String?) -> Unit,
) {
  val isDark = isSystemInDarkTheme()
  val dangerFg = if (isDark) Red400 else Red600
  val orangeBg = if (isDark) Orange500.copy(alpha = 0.18f) else Orange100
  val orangeFg = if (isDark) Orange400 else Orange600

  var newCategoryInput by remember { mutableStateOf("") }
  var addError by remember { mutableStateOf<String?>(null) }
  var editingCategory by remember { mutableStateOf<CategoryItem?>(null) }
  var deletingCategory by remember { mutableStateOf<CategoryItem?>(null) }

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
          modifier = Modifier.size(36.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.Category,
              contentDescription = null,
              tint = orangeFg,
              modifier = Modifier.size(20.dp),
            )
          }
        }
        Column {
          Text(
            text = "Kelola Kategori (${categories.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "Tambah, ubah nama, atau hapus kategori",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedTextField(
            value = newCategoryInput,
            onValueChange = {
              newCategoryInput = it.take(60)
              addError = null
            },
            label = { Text("Kategori Baru") },
            placeholder = { Text("Misal: Keramik, Listrik") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f).testTag("add_category_input"),
          )
          Button(
            onClick = {
              val clean = newCategoryInput.trim()
              when {
                clean.isBlank() -> addError = "Nama kategori wajib diisi."
                clean.equals(ALL_CATEGORY_LABEL, ignoreCase = true) ->
                  addError = "Nama '$ALL_CATEGORY_LABEL' tidak dapat digunakan."
                categories.any { it.name.equals(clean, ignoreCase = true) } ->
                  addError = "Kategori '$clean' sudah ada."
                else -> {
                  onSaveCategory(null, clean)
                  newCategoryInput = ""
                  addError = null
                }
              }
            },
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Orange600, contentColor = Color.White),
            modifier = Modifier.height(52.dp).testTag("add_category_button"),
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Tambah", fontWeight = FontWeight.Bold)
          }
        }

        if (addError != null) {
          Text(
            text = addError!!,
            color = dangerFg,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
          )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        if (categories.isEmpty()) {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
          ) {
            Text(
              text = "Belum ada kategori barang. Tambahkan kategori pertama di atas.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(14.dp),
            )
          }
        } else {
          Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            categories.forEach { cat ->
              val itemCount = products.count { it.category.equals(cat.name, ignoreCase = true) }
              Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.45f else 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
              ) {
                Row(
                  modifier =
                    Modifier.fillMaxWidth()
                      .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = cat.name,
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.Bold,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                      text = "$itemCount barang",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  IconButton(
                    onClick = { editingCategory = cat },
                    modifier = Modifier.size(48.dp).testTag("edit_category_${cat.id}"),
                  ) {
                    Icon(
                      imageVector = Icons.Default.Edit,
                      contentDescription = "Edit kategori ${cat.name}",
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(20.dp),
                    )
                  }
                  IconButton(
                    onClick = { deletingCategory = cat },
                    modifier = Modifier.size(48.dp).testTag("delete_category_${cat.id}"),
                  ) {
                    Icon(
                      imageVector = Icons.Default.DeleteOutline,
                      contentDescription = "Hapus kategori ${cat.name}",
                      tint = dangerFg,
                      modifier = Modifier.size(20.dp),
                    )
                  }
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Tutup", fontWeight = FontWeight.Bold)
      }
    },
  )

  editingCategory?.let { target ->
    var renameText by remember(target) { mutableStateOf(target.name) }
    var renameError by remember { mutableStateOf<String?>(null) }
    AlertDialog(
      onDismissRequest = { editingCategory = null },
      title = { Text("Ubah Nama Kategori") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = renameText,
            onValueChange = {
              renameText = it.take(60)
              renameError = null
            },
            label = { Text("Nama Kategori") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("rename_category_input"),
          )
          Text(
            text = "Semua barang di kategori ini akan otomatis diperbarui.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          if (renameError != null) {
            Text(
              text = renameError!!,
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
            val clean = renameText.trim()
            when {
              clean.isBlank() -> renameError = "Nama kategori tidak boleh kosong."
              clean.equals(ALL_CATEGORY_LABEL, ignoreCase = true) ->
                renameError = "Nama '$ALL_CATEGORY_LABEL' tidak dapat digunakan."
              categories.any { it.id != target.id && it.name.equals(clean, ignoreCase = true) } ->
                renameError = "Nama kategori '$clean' sudah digunakan."
              else -> {
                onSaveCategory(target, clean)
                editingCategory = null
              }
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = Orange600, contentColor = Color.White),
          modifier = Modifier.testTag("confirm_rename_category_button"),
        ) {
          Text("Simpan")
        }
      },
      dismissButton = {
        TextButton(onClick = { editingCategory = null }) { Text("Batal") }
      },
    )
  }

  deletingCategory?.let { target ->
    val usedCount = products.count { it.category.equals(target.name, ignoreCase = true) }
    val otherCategories = categories.filter { it.id != target.id }.map { it.name }
    var reassignTarget by remember(target, otherCategories) {
      mutableStateOf(otherCategories.firstOrNull())
    }
    val canDelete = usedCount == 0 || otherCategories.isNotEmpty()

    AlertDialog(
      onDismissRequest = { deletingCategory = null },
      title = { Text("Hapus Kategori?") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          when {
            usedCount == 0 -> {
              Text("Yakin ingin menghapus kategori '${target.name}'?")
            }
            otherCategories.isEmpty() -> {
              Text(
                text =
                  "Kategori '${target.name}' masih digunakan oleh $usedCount barang. Tambahkan kategori pengganti terlebih dahulu sebelum menghapus kategori terakhir ini.",
                color = dangerFg,
                style = MaterialTheme.typography.bodySmall,
              )
            }
            else -> {
              Text(
                text =
                  "Ada $usedCount barang dalam kategori '${target.name}'. Pilih kategori tujuan untuk memindahkan barang tersebut:",
                style = MaterialTheme.typography.bodySmall,
              )
              Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                otherCategories.forEach { alt ->
                  FilterChip(
                    selected = reassignTarget == alt,
                    onClick = { reassignTarget = alt },
                    label = { Text(alt, style = MaterialTheme.typography.labelSmall) },
                    colors =
                      FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Orange600,
                        selectedLabelColor = Color.White,
                      ),
                  )
                }
              }
            }
          }
        }
      },
      confirmButton = {
        if (canDelete) {
          Button(
            onClick = {
              onDeleteCategory(target, reassignTarget)
              deletingCategory = null
            },
            colors = ButtonDefaults.buttonColors(containerColor = Red600, contentColor = Color.White),
            modifier = Modifier.testTag("confirm_delete_category_button"),
          ) {
            Text("Hapus")
          }
        }
      },
      dismissButton = {
        TextButton(onClick = { deletingCategory = null }) {
          Text(if (canDelete) "Batal" else "Tutup")
        }
      },
    )
  }
}
