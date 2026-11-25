package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ProductEntity
import com.example.data.local.TransactionEntity
import com.example.data.model.ALL_CATEGORY_LABEL
import com.example.data.model.CartItem
import com.example.data.model.CustomerType
import com.example.data.model.PaymentMethod
import com.example.data.model.UserRole
import com.example.ui.components.CategoryFilterRow
import com.example.ui.components.CustomerPurchaseHistorySection
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
import com.example.ui.theme.Orange600
import com.example.ui.theme.Red100
import com.example.ui.theme.Red400
import com.example.ui.theme.Red600
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.viewmodel.StoreUiState
import com.example.util.formatQty
import com.example.util.formatRupiah

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
  uiState: StoreUiState,
  onSearchChange: (String) -> Unit,
  onCategorySelect: (String) -> Unit,
  onAddToCart: (ProductEntity, Boolean) -> Unit,
  onUpdateCartQty: (Long, Boolean, Double) -> Unit,
  onUpdateCartItemPrice: (Long, Boolean, Long) -> Unit = { _, _, _ -> },
  onRemoveCartItem: (Long, Boolean) -> Unit,
  onClearCart: () -> Unit,
  onSelectCustomerType: (CustomerType) -> Unit,
  onUpdateManualDiscount: (Long) -> Unit,
  onProcessCheckout: (String, String, PaymentMethod, Long, PaymentMethod) -> Unit,
  onOpenReceipt: (TransactionEntity) -> Unit,
  modifier: Modifier = Modifier,
) {
  var showCheckoutSheet by remember { mutableStateOf(false) }
  var showHistorySheet by remember { mutableStateOf(false) }

  val filteredProducts =
    remember(uiState.products, uiState.posSearchQuery, uiState.posSelectedCategory) {
      uiState.products.filter { product ->
        val matchesCategory =
          uiState.posSelectedCategory == ALL_CATEGORY_LABEL ||
            product.category.equals(uiState.posSelectedCategory, ignoreCase = true)
        val matchesQuery =
          uiState.posSearchQuery.isBlank() ||
            product.name.contains(uiState.posSearchQuery, ignoreCase = true) ||
            product.category.contains(uiState.posSearchQuery, ignoreCase = true)
        matchesCategory && matchesQuery
      }
    }

  Box(modifier = modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        StoreSearchBar(
          query = uiState.posSearchQuery,
          onQueryChange = onSearchChange,
          placeholder = "Cari nama barang...",
          modifier = Modifier.weight(1f),
          testTag = "pos_search_input",
        )
        OutlinedButton(
          onClick = { showHistorySheet = true },
          modifier = Modifier.height(48.dp).testTag("pos_history_button"),
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          colors =
            ButtonDefaults.outlinedButtonColors(
              containerColor = MaterialTheme.colorScheme.surface,
              contentColor = MaterialTheme.colorScheme.onSurface,
            ),
          contentPadding = PaddingValues(horizontal = 12.dp),
        ) {
          Icon(
            imageVector = Icons.Default.History,
            contentDescription = "Riwayat pembelian pelanggan",
            tint = if (isSystemInDarkTheme()) Orange400 else Orange600,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(5.dp))
          Text(
            text = "Riwayat",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
          )
        }
      }
      Spacer(modifier = Modifier.height(8.dp))
      CategoryFilterRow(
        categories = uiState.categoryNames,
        selectedCategory = uiState.posSelectedCategory,
        onSelectCategory = onCategorySelect,
      )
      Spacer(modifier = Modifier.height(8.dp))

      if (filteredProducts.isEmpty()) {
        Box(
          modifier = Modifier.fillMaxSize().padding(bottom = 80.dp),
          contentAlignment = Alignment.Center,
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector = Icons.Default.Inventory2,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(56.dp),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = "Barang tidak ditemukan",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "Coba ubah kata kunci atau filter kategori",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      } else {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
          val columns = if (maxWidth >= 600.dp) 2 else 1
          LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding =
              PaddingValues(bottom = if (uiState.cart.isNotEmpty()) 96.dp else 20.dp),
          ) {
            items(filteredProducts, key = { it.id }) { product ->
              val inCartBaseQty =
                uiState.cart
                  .filter { it.productId == product.id && !it.isRetailUnit }
                  .sumOf { it.quantity }
              val inCartRetailQty =
                uiState.cart
                  .filter { it.productId == product.id && it.isRetailUnit }
                  .sumOf { it.quantity }
              PosProductCard(
                product = product,
                inCartBaseQty = inCartBaseQty,
                inCartRetailQty = inCartRetailQty,
                onAddBase = { onAddToCart(product, false) },
                onAddRetail = { onAddToCart(product, true) },
              )
            }
          }
        }
      }
    }

    // Sticky Cart Summary Bar
    AnimatedVisibility(
      visible = uiState.cart.isNotEmpty(),
      enter = slideInVertically(initialOffsetY = { it }),
      exit = slideOutVertically(targetOffsetY = { it }),
      modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
      val totalItemsCount = uiState.cart.size
      Surface(
        modifier =
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { showCheckoutSheet = true }
            .testTag("open_checkout_button"),
        shape = RoundedCornerShape(18.dp),
        color = Navy900,
        border = BorderStroke(1.dp, Orange600.copy(alpha = 0.45f)),
        shadowElevation = 10.dp,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
              modifier =
                Modifier.size(44.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .background(Orange600),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = "Keranjang belanja",
                tint = Color.White,
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text =
                  if (uiState.totalCombinedSavings > 0L) "$totalItemsCount Item • Hemat ${formatRupiah(uiState.totalCombinedSavings)}"
                  else "$totalItemsCount Item di Keranjang",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFCBD5E1),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = formatRupiah(uiState.cartTotal),
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
              )
            }
          }

          Spacer(modifier = Modifier.width(10.dp))

          Surface(
            shape = RoundedCornerShape(12.dp),
            color = Orange600,
          ) {
            Text(
              text = "Bayar →",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = Color.White,
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            )
          }
        }
      }
    }
  }

  if (showCheckoutSheet) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
      onDismissRequest = { showCheckoutSheet = false },
      sheetState = sheetState,
      containerColor = MaterialTheme.colorScheme.surface,
    ) {
      CheckoutBottomSheetContent(
        uiState = uiState,
        onUpdateCartQty = onUpdateCartQty,
        onUpdateCartItemPrice = onUpdateCartItemPrice,
        onRemoveCartItem = onRemoveCartItem,
        onClearCart = {
          onClearCart()
          showCheckoutSheet = false
        },
        onSelectCustomerType = onSelectCustomerType,
        onUpdateManualDiscount = onUpdateManualDiscount,
        onConfirm = { name, phone, method, cash, dpMethod ->
          onProcessCheckout(name, phone, method, cash, dpMethod)
          showCheckoutSheet = false
        },
      )
    }
  }

  if (showHistorySheet) {
    val historySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
      onDismissRequest = { showHistorySheet = false },
      sheetState = historySheetState,
      containerColor = MaterialTheme.colorScheme.background,
    ) {
      Column(modifier = Modifier.fillMaxWidth().heightIn(max = 640.dp)) {
        Text(
          text = "Riwayat Pembelian Pelanggan",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        CustomerPurchaseHistorySection(
          transactions = uiState.transactions,
          isOwner = uiState.settings.activeRole == UserRole.OWNER,
          onOpenReceipt = { tx ->
            showHistorySheet = false
            onOpenReceipt(tx)
          },
          onDeleteTransaction = null,
        )
      }
    }
  }
}

@Composable
private fun PosProductCard(
  product: ProductEntity,
  inCartBaseQty: Double,
  inCartRetailQty: Double,
  onAddBase: () -> Unit,
  onAddRetail: () -> Unit,
) {
  val isDark = isSystemInDarkTheme()
  val emeraldBg = if (isDark) Emerald400.copy(alpha = 0.14f) else Emerald100
  val emeraldFg = if (isDark) Emerald400 else Emerald600
  val redBg = if (isDark) Red400.copy(alpha = 0.14f) else Red100
  val redFg = if (isDark) Red400 else Red600
  val orangeBg = if (isDark) Orange400.copy(alpha = 0.14f) else Orange100
  val orangeFg = if (isDark) Orange400 else Orange600
  val amberBg = if (isDark) Amber400.copy(alpha = 0.14f) else Amber100
  val amberFg = if (isDark) Amber400 else Amber600

  val remainingBaseStock = (product.stockQuantity - inCartBaseQty).coerceAtLeast(0.0)
  val remainingRetailStock = (product.retailStockQuantity - inCartRetailQty).coerceAtLeast(0.0)
  val isLowStock = product.stockQuantity < product.minStock
  val isOutOfBaseStock = remainingBaseStock <= 0.0001
  val hasRetail = !product.retailUnitName.isNullOrBlank() && product.retailSellPrice > 0L
  val isRetailEmpty = hasRetail && remainingRetailStock < 1.0 - 1e-6
  val hasAnythingInCart = inCartBaseQty > 0.0 || inCartRetailQty > 0.0

  val baseStockBg = if (isLowStock || isOutOfBaseStock) redBg else emeraldBg
  val baseStockFg = if (isLowStock || isOutOfBaseStock) redFg else emeraldFg
  val retailStockBg = if (isRetailEmpty) amberBg else orangeBg
  val retailStockFg = if (isRetailEmpty) amberFg else orangeFg

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border =
      BorderStroke(
        width = if (hasAnythingInCart) 1.5.dp else 1.dp,
        color = if (hasAnythingInCart) orangeFg else MaterialTheme.colorScheme.outlineVariant,
      ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      // Header row: category chip + optional status badge (never squashes stock numbers)
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
            color = if (isDark) MaterialTheme.colorScheme.onPrimaryContainer else Navy900,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }

        if (isOutOfBaseStock || isLowStock) {
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
                contentDescription = if (isOutOfBaseStock) "Stok habis" else "Stok tipis",
                tint = redFg,
                modifier = Modifier.size(12.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = if (isOutOfBaseStock) "Habis" else "Stok Tipis",
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

      // Dedicated real-time stock display (2 equal boxes when retail is active)
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
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
              verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
              Text(
                text = "Stok Utama",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = "${formatQty(remainingBaseStock)} ${product.baseUnit}",
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
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
              verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
              Text(
                text = "Stok Eceran",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = "${formatQty(remainingRetailStock)} ${product.retailUnitName}",
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
            Text(
              text = "Stok Tersedia",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = "${formatQty(remainingBaseStock)} ${product.baseUnit}",
              fontFamily = JetBrainsMono,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = baseStockFg,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }

      if (hasAnythingInCart) {
        val cartParts = buildList {
          if (inCartBaseQty > 0.0) add("${formatQty(inCartBaseQty)} ${product.baseUnit}")
          if (inCartRetailQty > 0.0) add("${formatQty(inCartRetailQty)} ${product.retailUnitName ?: "Eceran"}")
        }
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(8.dp),
          color = orangeBg,
          border = BorderStroke(1.dp, orangeFg.copy(alpha = 0.35f)),
        ) {
          Text(
            text = "Di keranjang: ${cartParts.joinToString(" + ")}",
            style = MaterialTheme.typography.labelSmall,
            color = orangeFg,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
          )
        }
      }

      // Symmetrical multi-unit add-to-cart buttons (large 52dp+ touch targets)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Button(
          onClick = onAddBase,
          enabled = remainingBaseStock >= 1.0 - 1e-6,
          modifier =
            Modifier.weight(1f)
              .heightIn(min = 54.dp)
              .testTag("add_base_${product.id}"),
          shape = RoundedCornerShape(12.dp),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = if (isDark) Navy800 else Navy900,
              contentColor = Color.White,
            ),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "+ 1 ${product.baseUnit}",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = formatRupiah(product.sellPricePerBaseUnit),
              fontFamily = JetBrainsMono,
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFFFDBA74),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        if (hasRetail) {
          val retailAvailable = remainingRetailStock >= 1.0 - 1e-6
          OutlinedButton(
            onClick = onAddRetail,
            enabled = retailAvailable,
            modifier =
              Modifier.weight(1f)
                .heightIn(min = 54.dp)
                .testTag("add_retail_${product.id}"),
            shape = RoundedCornerShape(12.dp),
            border =
              BorderStroke(
                1.5.dp,
                if (retailAvailable) orangeFg
                else MaterialTheme.colorScheme.outlineVariant,
              ),
            colors =
              ButtonDefaults.outlinedButtonColors(
                containerColor = if (retailAvailable) orangeBg.copy(alpha = 0.35f) else Color.Transparent,
                contentColor = orangeFg,
              ),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "+ 1 ${product.retailUnitName}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text =
                  if (retailAvailable) formatRupiah(product.retailSellPrice)
                  else "Eceran habis",
                fontFamily = JetBrainsMono,
                style = MaterialTheme.typography.labelSmall,
                color = if (retailAvailable) MaterialTheme.colorScheme.onSurfaceVariant else amberFg,
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

@Composable
private fun CheckoutBottomSheetContent(
  uiState: StoreUiState,
  onUpdateCartQty: (Long, Boolean, Double) -> Unit,
  onUpdateCartItemPrice: (Long, Boolean, Long) -> Unit,
  onRemoveCartItem: (Long, Boolean) -> Unit,
  onClearCart: () -> Unit,
  onSelectCustomerType: (CustomerType) -> Unit,
  onUpdateManualDiscount: (Long) -> Unit,
  onConfirm: (String, String, PaymentMethod, Long, PaymentMethod) -> Unit,
) {
  var paymentMethod by remember { mutableStateOf(PaymentMethod.TUNAI) }
  var dpPaymentMethod by remember { mutableStateOf(PaymentMethod.TUNAI) }
  var dpInputText by remember { mutableStateOf("") }
  var customerName by remember { mutableStateOf("") }
  var customerPhone by remember { mutableStateOf("") }
  var discountInputText by remember {
    mutableStateOf(
      if (uiState.manualDiscountAmount > 0L) uiState.manualDiscountAmount.toString() else ""
    )
  }
  var cashInputText by remember(uiState.cartTotal) { mutableStateOf(uiState.cartTotal.toString()) }
  var validationError by remember { mutableStateOf<String?>(null) }
  var editingQtyItem by remember { mutableStateOf<CartItem?>(null) }
  var bargainingItem by remember { mutableStateOf<CartItem?>(null) }

  LaunchedEffect(uiState.customerType) {
    if (uiState.customerType != CustomerType.PELANGGAN) {
      discountInputText = ""
    }
  }

  val rawDiscountAmount = discountInputText.filter { it.isDigit() }.toLongOrNull() ?: 0L
  val cashAmount = cashInputText.filter { it.isDigit() }.toLongOrNull() ?: 0L
  val dpAmount = dpInputText.filter { it.isDigit() }.toLongOrNull() ?: 0L
  val changeAmount = (cashAmount - uiState.cartTotal).coerceAtLeast(0L)

  val isDark = isSystemInDarkTheme()
  val emeraldBg = if (isDark) Emerald400.copy(alpha = 0.14f) else Emerald100
  val emeraldFg = if (isDark) Emerald400 else Emerald600
  val redBg = if (isDark) Red400.copy(alpha = 0.14f) else Red100
  val redFg = if (isDark) Red400 else Red600
  val orangeBg = if (isDark) Orange400.copy(alpha = 0.14f) else Orange100
  val orangeFg = if (isDark) Orange400 else Orange600
  val amberBg = if (isDark) Amber400.copy(alpha = 0.14f) else Amber100
  val amberFg = if (isDark) Amber400 else Amber600

  Column(
    modifier =
      Modifier.fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 18.dp)
        .padding(bottom = 28.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Column(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Rincian Keranjang & Bayar",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
        )
        TextButton(onClick = onClearCart) {
          Text("Kosongkan", color = redFg, fontWeight = FontWeight.Bold)
        }
      }

    Spacer(modifier = Modifier.height(8.dp))

    // Cart items with per-item bargain negotiation
    uiState.cart.forEach { item ->
      val minUnitPrice = item.effectiveMinUnitPrice
      val canBargainItem = minUnitPrice < item.standardUnitPrice
      val isBargained = item.unitBargainDiscount > 0L
      Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        border =
          BorderStroke(
            1.dp,
            if (isBargained) emeraldFg.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.outlineVariant,
          ),
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 8.dp, end = 8.dp, bottom = 10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
          ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = item.productName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
              if (isBargained) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Text(
                    text = formatRupiah(item.standardUnitPrice),
                    fontFamily = JetBrainsMono,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textDecoration = TextDecoration.LineThrough,
                  )
                  Text(
                    text = "${formatRupiah(item.unitPrice)} / ${item.unitName}",
                    fontFamily = JetBrainsMono,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = emeraldFg,
                  )
                }
                Text(
                  text = "Hemat tawar item: -${formatRupiah(item.lineBargainDiscount)}",
                  style = MaterialTheme.typography.labelSmall,
                  color = emeraldFg,
                  fontWeight = FontWeight.SemiBold,
                )
              } else {
                Text(
                  text = "${formatRupiah(item.unitPrice)} / ${item.unitName}",
                  fontFamily = JetBrainsMono,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              if (item.isRetailUnit) {
                Text(
                  text = "Potong stok eceran (${item.unitName})",
                  style = MaterialTheme.typography.labelSmall,
                  color = orangeFg,
                  fontWeight = FontWeight.SemiBold,
                )
              }
            }

            IconButton(
              onClick = { onRemoveCartItem(item.productId, item.isRetailUnit) },
              modifier = Modifier.size(48.dp),
            ) {
              Icon(
                Icons.Default.Delete,
                contentDescription = "Hapus barang",
                tint = redFg,
                modifier = Modifier.size(20.dp),
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth().padding(end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(
                onClick = { onUpdateCartQty(item.productId, item.isRetailUnit, item.quantity - 1.0) },
                modifier = Modifier.size(48.dp),
              ) {
                Icon(Icons.Default.Remove, contentDescription = "Kurangi jumlah")
              }
              Surface(
                modifier =
                  Modifier.clip(RoundedCornerShape(8.dp))
                    .clickable { editingQtyItem = item },
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
              ) {
                Text(
                  text = "${formatQty(item.quantity)} ${item.unitName}",
                  fontFamily = JetBrainsMono,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                )
              }
              IconButton(
                onClick = { onUpdateCartQty(item.productId, item.isRetailUnit, item.quantity + 1.0) },
                modifier = Modifier.size(48.dp),
              ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah jumlah")
              }
            }

            Column(horizontalAlignment = Alignment.End) {
              if (isBargained) {
                Text(
                  text = formatRupiah(item.standardSubtotal),
                  fontFamily = JetBrainsMono,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  textDecoration = TextDecoration.LineThrough,
                )
              }
              Text(
                text = formatRupiah(item.subtotal),
                fontFamily = JetBrainsMono,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isBargained) emeraldFg else MaterialTheme.colorScheme.primary,
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
          Spacer(modifier = Modifier.height(6.dp))

          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text =
                if (canBargainItem) "Min. tawar: ${formatRupiah(minUnitPrice)}/${item.unitName}"
                else "Harga pas (${formatRupiah(item.standardUnitPrice)})",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.weight(1f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            if (canBargainItem) {
              Spacer(modifier = Modifier.width(8.dp))
              OutlinedButton(
                onClick = { bargainingItem = item },
                shape = RoundedCornerShape(10.dp),
                border =
                  BorderStroke(
                    1.dp,
                    if (isBargained) emeraldFg else orangeFg.copy(alpha = 0.75f),
                  ),
                colors =
                  ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isBargained) emeraldBg else orangeBg.copy(alpha = 0.45f),
                    contentColor = if (isBargained) emeraldFg else orangeFg,
                  ),
                contentPadding = PaddingValues(horizontal = 12.dp),
                modifier =
                  Modifier.heightIn(min = 48.dp)
                    .testTag("bargain_item_${item.productId}_${item.isRetailUnit}"),
              ) {
                Icon(
                  imageVector = if (isBargained) Icons.Default.Edit else Icons.Default.LocalOffer,
                  contentDescription = "Tawar harga ${item.productName}",
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = if (isBargained) "Ubah Tawar" else "Tawar Barang",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Customer Category & Quick Customer Picker
    Text(
      text = "Kategori & Pilihan Pelanggan",
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.Bold,
    )
    Spacer(modifier = Modifier.height(6.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      CustomerType.entries.forEach { type ->
        val selected = uiState.customerType == type
        Surface(
          modifier =
            Modifier.weight(1f)
              .clip(RoundedCornerShape(12.dp))
              .clickable { onSelectCustomerType(type) }
              .testTag("customer_type_${type.name.lowercase()}"),
          shape = RoundedCornerShape(12.dp),
          color =
            if (selected) (if (isDark) Navy800 else Navy900)
            else MaterialTheme.colorScheme.background,
          border =
            BorderStroke(
              width = if (selected) 1.5.dp else 1.dp,
              color = if (selected) Orange600 else MaterialTheme.colorScheme.outline,
            ),
        ) {
          Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Text(
              text = type.label,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = type.shortLabel,
              style = MaterialTheme.typography.labelSmall,
              color =
                if (selected) Color(0xFFFDBA74)
                else if (type.discountRate > 0.0) emeraldFg
                else MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }

    if (uiState.customerType == CustomerType.PELANGGAN) {
      Spacer(modifier = Modifier.height(12.dp))
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Potongan Harga Keseluruhan (Tawar Total)",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.weight(1f),
            )
            Text(
              text = "Nominal Rp",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            val discountPresets =
              listOf(
                "Normal" to 0L,
                "-5rb" to 5_000L,
                "-10rb" to 10_000L,
                "-20rb" to 20_000L,
                "-50rb" to 50_000L,
              )
            discountPresets.forEach { (presetLabel, presetValue) ->
              val clampedPreset = presetValue.coerceAtMost(uiState.cartSubtotal)
              val isPresetSelected =
                if (presetValue == 0L) uiState.manualDiscountAmount == 0L
                else uiState.manualDiscountAmount == clampedPreset && clampedPreset > 0L
              FilterChip(
                selected = isPresetSelected,
                onClick = {
                  discountInputText = if (clampedPreset > 0L) clampedPreset.toString() else ""
                  onUpdateManualDiscount(clampedPreset)
                  validationError = null
                },
                enabled = presetValue == 0L || uiState.cartSubtotal >= presetValue,
                label = {
                  Text(
                    text = presetLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                  )
                },
                colors =
                  FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Emerald600,
                    selectedLabelColor = Color.White,
                  ),
              )
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = discountInputText,
            onValueChange = { input ->
              val digits = input.filter { ch -> ch.isDigit() }.take(10)
              discountInputText = digits
              val parsed = digits.toLongOrNull() ?: 0L
              onUpdateManualDiscount(parsed)
              validationError = null
            },
            label = { Text("Potongan Harga Keseluruhan (Rp)") },
            prefix = { Text("Rp ") },
            placeholder = { Text("0") },
            trailingIcon = {
              if (discountInputText.isNotEmpty()) {
                IconButton(
                  onClick = {
                    discountInputText = ""
                    onUpdateManualDiscount(0L)
                  },
                  modifier = Modifier.testTag("clear_manual_discount_button"),
                ) {
                  Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Hapus potongan harga",
                  )
                }
              }
            },
            isError = rawDiscountAmount > uiState.cartSubtotal,
            supportingText =
              when {
                rawDiscountAmount > uiState.cartSubtotal -> {
                  {
                    Text(
                      "Maksimal potongan sebesar subtotal (${formatRupiah(uiState.cartSubtotal)})."
                    )
                  }
                }
                uiState.cartDiscount > 0L -> {
                  {
                    Text(
                      "Harga akhir setelah potongan: ${formatRupiah(uiState.cartTotal)}",
                      color = Emerald600,
                      fontWeight = FontWeight.SemiBold,
                    )
                  }
                }
                else -> null
              },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("checkout_manual_discount_input"),
            shape = RoundedCornerShape(12.dp),
          )
        }
      }
    }

    if (uiState.savedCustomers.isNotEmpty()) {
      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = "Pilih Pelanggan Tersimpan",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(6.dp))
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        uiState.savedCustomers.forEach { saved ->
          val isPicked = customerName.equals(saved.name, ignoreCase = true)
          FilterChip(
            selected = isPicked,
            onClick = {
              customerName = saved.name
              if (saved.phone.isNotBlank()) customerPhone = saved.phone
              onSelectCustomerType(saved.customerType)
              validationError = null
            },
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
            },
            label = {
              Text(
                text = saved.name,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
              )
            },
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = Orange600,
                selectedLabelColor = Color.White,
                selectedLeadingIconColor = Color.White,
              ),
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Payment Method
    Text(
      text = "Metode Pembayaran",
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.Bold,
    )
    Spacer(modifier = Modifier.height(6.dp))
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
      PaymentMethod.entries.forEachIndexed { idx, method ->
        SegmentedButton(
          selected = paymentMethod == method,
          onClick = {
            paymentMethod = method
            validationError = null
          },
          shape = SegmentedButtonDefaults.itemShape(idx, PaymentMethod.entries.size),
          modifier = Modifier.testTag("payment_method_${method.name.lowercase()}"),
        ) {
          Text(text = method.label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Customer Name & Phone (Mandatory for Kasbon, optional for Tunai/QRIS)
    OutlinedTextField(
      value = customerName,
      onValueChange = {
        customerName = it
        validationError = null
      },
      label = {
        Text(
          if (paymentMethod == PaymentMethod.KASBON) "Nama Pelanggan / Mandor (Wajib)*"
          else "Nama Pelanggan (Opsional)"
        )
      },
      singleLine = true,
      modifier = Modifier.fillMaxWidth().testTag("checkout_customer_name"),
      shape = RoundedCornerShape(12.dp),
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
      value = customerPhone,
      onValueChange = {
        customerPhone = it
        validationError = null
      },
      label = {
        Text(
          if (paymentMethod == PaymentMethod.KASBON) "No. HP / WhatsApp (Wajib)*"
          else "No. WhatsApp untuk Struk (Opsional)"
        )
      },
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
      singleLine = true,
      modifier = Modifier.fillMaxWidth().testTag("checkout_customer_phone"),
      shape = RoundedCornerShape(12.dp),
    )

    if (paymentMethod == PaymentMethod.TUNAI) {
      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = "Kalkulator Kembalian Tunai",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
      )
      Spacer(modifier = Modifier.height(6.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        val presets =
          listOf(
            "Pas" to uiState.cartTotal,
            "50rb" to 50_000L,
            "100rb" to 100_000L,
            "200rb" to 200_000L,
            "500rb" to 500_000L,
          )
        presets.forEach { (label, amount) ->
          Surface(
            modifier =
              Modifier.weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .clickable { cashInputText = amount.toString() },
            shape = RoundedCornerShape(10.dp),
            color = orangeBg,
            border = BorderStroke(1.dp, orangeFg.copy(alpha = 0.35f)),
          ) {
            Text(
              text = label,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = orangeFg,
              modifier = Modifier.padding(vertical = 8.dp),
              textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
      OutlinedTextField(
        value = cashInputText,
        onValueChange = {
          cashInputText = it.filter { ch -> ch.isDigit() }
          validationError = null
        },
        label = { Text("Uang Diterima (Rp)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("checkout_cash_input"),
        shape = RoundedCornerShape(12.dp),
      )

      Spacer(modifier = Modifier.height(8.dp))
      val enoughCash = cashAmount >= uiState.cartTotal
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (enoughCash) emeraldBg else redBg,
        border = BorderStroke(1.dp, (if (enoughCash) emeraldFg else redFg).copy(alpha = 0.35f)),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = if (enoughCash) "Kembalian" else "Uang Kurang",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (enoughCash) emeraldFg else redFg,
          )
          Text(
            text =
              if (enoughCash) formatRupiah(changeAmount)
              else formatRupiah(uiState.cartTotal - cashAmount),
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall,
            color = if (enoughCash) emeraldFg else redFg,
          )
        }
      }
    } else if (paymentMethod == PaymentMethod.KASBON) {
      Spacer(modifier = Modifier.height(10.dp))
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = "DP / Uang Muka Kasbon (Opsional)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
          )
          OutlinedTextField(
            value = dpInputText,
            onValueChange = {
              dpInputText = it.filter { ch -> ch.isDigit() }.take(12)
              validationError = null
            },
            label = { Text("DP / Uang Muka (Rp)") },
            prefix = { Text("Rp ") },
            placeholder = { Text("0 (Full Kasbon)") },
            trailingIcon = {
              if (dpInputText.isNotEmpty()) {
                IconButton(onClick = { dpInputText = "" }) {
                  Icon(Icons.Default.Close, contentDescription = "Hapus nominal DP")
                }
              }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("checkout_kasbon_dp_input"),
            shape = RoundedCornerShape(12.dp),
          )
          if (dpAmount > 0L) {
            Text(
              text = "DP Diterima Lewat",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
              val dpMethods = listOf(PaymentMethod.TUNAI, PaymentMethod.TRANSFER_QRIS)
              dpMethods.forEachIndexed { idx, method ->
                SegmentedButton(
                  selected = dpPaymentMethod == method,
                  onClick = { dpPaymentMethod = method },
                  shape = SegmentedButtonDefaults.itemShape(idx, dpMethods.size),
                ) {
                  Text(text = method.label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
              }
            }
            val remainingKasbon = (uiState.cartTotal - dpAmount).coerceAtLeast(0L)
            Surface(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(10.dp),
              color = if (dpAmount < uiState.cartTotal) amberBg else redBg,
              border =
                BorderStroke(
                  1.dp,
                  (if (dpAmount < uiState.cartTotal) amberFg else redFg).copy(alpha = 0.35f),
                ),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = if (dpAmount < uiState.cartTotal) "Sisa Kasbon" else "DP melebihi total",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = if (dpAmount < uiState.cartTotal) amberFg else redFg,
                )
                Text(
                  text = formatRupiah(remainingKasbon),
                  fontFamily = JetBrainsMono,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = if (dpAmount < uiState.cartTotal) amberFg else redFg,
                )
              }
            }
          } else {
            Surface(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(10.dp),
              color = amberBg,
              border = BorderStroke(1.dp, amberFg.copy(alpha = 0.35f)),
            ) {
              Text(
                text = "Transaksi akan dicatat ke Daftar Kasbon hingga dilunasi oleh Pemilik.",
                style = MaterialTheme.typography.bodySmall,
                color = amberFg,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(10.dp),
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(10.dp))

    if (uiState.totalItemBargainDiscount > 0L) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
          "Subtotal Harga Standar",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          formatRupiah(uiState.cartStandardSubtotal),
          fontFamily = JetBrainsMono,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
          "Tawar Per Barang",
          style = MaterialTheme.typography.bodySmall,
          color = emeraldFg,
          fontWeight = FontWeight.SemiBold,
        )
        Text(
          "-${formatRupiah(uiState.totalItemBargainDiscount)}",
          fontFamily = JetBrainsMono,
          style = MaterialTheme.typography.bodySmall,
          color = emeraldFg,
          fontWeight = FontWeight.SemiBold,
        )
      }
      Spacer(modifier = Modifier.height(2.dp))
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text(
        "Subtotal",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        formatRupiah(uiState.cartSubtotal),
        fontFamily = JetBrainsMono,
        style = MaterialTheme.typography.bodyMedium,
      )
    }
    if (uiState.cartDiscount > 0) {
      val discountLabel =
        if (uiState.customerType.discountRate > 0.0) {
          val discPct = (uiState.customerType.discountRate * 100).toInt()
          "Diskon ${uiState.customerType.label} ($discPct%)"
        } else {
          "Potongan Keseluruhan"
        }
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
          text = discountLabel,
          style = MaterialTheme.typography.bodyMedium,
          color = emeraldFg,
        )
        Text(
          "-${formatRupiah(uiState.cartDiscount)}",
          fontFamily = JetBrainsMono,
          style = MaterialTheme.typography.bodyMedium,
          color = emeraldFg,
        )
      }
    }
    Spacer(modifier = Modifier.height(4.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text("TOTAL BAYAR", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
      Text(
        formatRupiah(uiState.cartTotal),
        fontFamily = JetBrainsMono,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }

    if (validationError != null) {
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = validationError!!,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
      )
    }

    Spacer(modifier = Modifier.height(14.dp))

    Button(
      onClick = {
        when {
          paymentMethod == PaymentMethod.KASBON && customerName.isBlank() -> {
            validationError = "Nama pelanggan wajib diisi untuk Tempo / Kasbon."
          }
          paymentMethod == PaymentMethod.KASBON && customerPhone.isBlank() -> {
            validationError = "Nomor HP / WhatsApp wajib diisi untuk Tempo / Kasbon."
          }
          paymentMethod == PaymentMethod.TUNAI && cashAmount < uiState.cartTotal -> {
            validationError = "Uang tunai yang diterima kurang dari total belanja."
          }
          paymentMethod == PaymentMethod.KASBON && uiState.cartTotal > 0L && dpAmount >= uiState.cartTotal -> {
            validationError = "DP sama dengan atau melebihi total belanja. Pilih metode Tunai atau Transfer/QRIS."
          }
          else -> {
            onConfirm(
              customerName,
              customerPhone,
              paymentMethod,
              if (paymentMethod == PaymentMethod.KASBON) dpAmount else cashAmount,
              dpPaymentMethod,
            )
          }
        }
      },
      modifier = Modifier.fillMaxWidth().height(54.dp).testTag("confirm_checkout_button"),
      shape = RoundedCornerShape(14.dp),
      colors = ButtonDefaults.buttonColors(containerColor = Orange600),
    ) {
      Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = "Proses & Cetak Struk",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = Color.White,
      )
    }
    }
  }

  editingQtyItem?.let { target ->
    var qtyText by remember(target) { mutableStateOf(target.quantity.toString()) }
    AlertDialog(
      onDismissRequest = { editingQtyItem = null },
      title = { Text("Ubah Jumlah (${target.unitName})") },
      text = {
        Column {
          Text(target.productName, style = MaterialTheme.typography.bodySmall, color = Slate500)
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = qtyText,
            onValueChange = { qtyText = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            label = { Text("Jumlah (${target.unitName})") },
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val parsed = qtyText.replace(',', '.').toDoubleOrNull()
            if (parsed != null) {
              onUpdateCartQty(target.productId, target.isRetailUnit, parsed)
            }
            editingQtyItem = null
          }
        ) {
          Text("Simpan")
        }
      },
      dismissButton = {
        TextButton(onClick = { editingQtyItem = null }) { Text("Batal") }
      },
    )
  }

  bargainingItem?.let { target ->
    val minPrice = target.effectiveMinUnitPrice
    val stdPrice = target.standardUnitPrice
    var unitPriceText by remember(target) { mutableStateOf(target.unitPrice.toString()) }
    val parsedUnitPrice = unitPriceText.filter { it.isDigit() }.toLongOrNull()
    val priceError =
      when {
        parsedUnitPrice == null -> "Masukkan harga tawar per ${target.unitName}."
        parsedUnitPrice < minPrice ->
          "Tidak boleh di bawah harga minimal (${formatRupiah(minPrice)})."
        parsedUnitPrice > stdPrice ->
          "Maksimal sebesar harga standar (${formatRupiah(stdPrice)})."
        else -> null
      }
    val previewUnitPrice = (parsedUnitPrice ?: target.unitPrice).coerceIn(minPrice, stdPrice)
    val previewLineSubtotal = kotlin.math.round(target.quantity * previewUnitPrice).toLong()
    val previewLineSavings = (target.standardSubtotal - previewLineSubtotal).coerceAtLeast(0L)

    val quickCandidates =
      buildList {
        add("Standar" to stdPrice)
        listOf(500L, 1_000L, 2_000L, 5_000L, 10_000L).forEach { step ->
          val candidate = stdPrice - step
          if (candidate in minPrice until stdPrice) {
            val shortLabel =
              if (step >= 1_000L) "-${step / 1_000L}rb" else "-$step"
            add(shortLabel to candidate)
          }
        }
        if (minPrice < stdPrice) {
          add("Min" to minPrice)
        }
      }.distinctBy { it.second }

    AlertDialog(
      onDismissRequest = { bargainingItem = null },
      title = { Text("Tawar Harga Barang") },
      text = {
        Column(
          modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = "${target.productName} (${formatQty(target.quantity)} ${target.unitName})",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
          )
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(10.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = "Harga Standar / ${target.unitName}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                  text = formatRupiah(stdPrice),
                  fontFamily = JetBrainsMono,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.SemiBold,
                )
              }
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = "Harga Jual Minimal",
                  style = MaterialTheme.typography.labelSmall,
                  color = emeraldFg,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = formatRupiah(minPrice),
                  fontFamily = JetBrainsMono,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = emeraldFg,
                )
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            quickCandidates.forEach { (chipLabel, chipPrice) ->
              FilterChip(
                selected = parsedUnitPrice == chipPrice,
                onClick = { unitPriceText = chipPrice.toString() },
                label = {
                  Text(
                    text = chipLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                  )
                },
                colors =
                  FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Emerald600,
                    selectedLabelColor = Color.White,
                  ),
              )
            }
          }

          OutlinedTextField(
            value = unitPriceText,
            onValueChange = { input -> unitPriceText = input.filter { it.isDigit() }.take(10) },
            label = { Text("Harga Tawar / ${target.unitName} (Rp)") },
            prefix = { Text("Rp ") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            isError = priceError != null,
            supportingText =
              if (priceError != null) {
                { Text(priceError) }
              } else {
                {
                  Text(
                    text =
                      if (previewLineSavings > 0L)
                        "Subtotal: ${formatRupiah(previewLineSubtotal)} (Hemat ${formatRupiah(previewLineSavings)})"
                      else "Subtotal: ${formatRupiah(previewLineSubtotal)}",
                    color = if (previewLineSavings > 0L) emeraldFg else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                  )
                }
              },
            modifier = Modifier.fillMaxWidth().testTag("bargain_unit_price_input"),
            shape = RoundedCornerShape(12.dp),
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (parsedUnitPrice != null && priceError == null) {
              onUpdateCartItemPrice(target.productId, target.isRetailUnit, parsedUnitPrice)
              bargainingItem = null
            }
          },
          enabled = priceError == null,
          colors = ButtonDefaults.buttonColors(containerColor = Orange600, contentColor = Color.White),
          modifier = Modifier.testTag("confirm_bargain_item_button"),
        ) {
          Text("Terapkan")
        }
      },
      dismissButton = {
        TextButton(onClick = { bargainingItem = null }) { Text("Batal") }
      },
    )
  }
}
