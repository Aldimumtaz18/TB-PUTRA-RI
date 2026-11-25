package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.AppTab
import com.example.data.model.UserRole
import com.example.data.repository.AuthRepository
import com.example.ui.components.ReceiptModal
import com.example.ui.components.StoreTopBar
import com.example.ui.screens.AccountSettingsScreen
import com.example.ui.screens.FinanceScreen
import com.example.ui.screens.GoogleSignInScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.PosScreen
import com.example.ui.screens.StockScreen
import com.example.ui.theme.Orange100
import com.example.ui.theme.Orange600
import com.example.ui.theme.TbPutraRiTheme
import com.example.ui.viewmodel.StoreUiState
import com.example.ui.viewmodel.StoreViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    val authRepository = AuthRepository(applicationContext)
    setContent {
      TbPutraRiTheme {
        val currentUser by authRepository.currentUserFlow.collectAsStateWithLifecycle(
          initialValue = authRepository.currentUser
        )
        var isCheckingSilentAuth by remember { mutableStateOf(currentUser == null) }
        var isSigningIn by remember { mutableStateOf(false) }
        var authError by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()
        val activityContext = LocalContext.current

        LaunchedEffect(Unit) {
          if (currentUser == null) {
            authRepository.attemptSilentSignIn()
          }
          isCheckingSilentAuth = false
        }

        if (isCheckingSilentAuth) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              CircularProgressIndicator(color = Orange600)
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = "Memeriksa sesi Google...",
                style = MaterialTheme.typography.bodyMedium,
              )
            }
          }
        } else if (currentUser == null) {
          GoogleSignInScreen(
            isLoading = isSigningIn,
            errorMessage = authError,
            onSignInClick = {
              scope.launch {
                isSigningIn = true
                authError = null
                authRepository.signInWithGoogle(activityContext).onFailure { e ->
                  authError = e.message ?: "Gagal masuk dengan akun Google."
                }
                isSigningIn = false
              }
            },
          )
        } else {
          val activeUser = currentUser!!
          key(activeUser.uid) {
            val viewModel: StoreViewModel =
              viewModel(
                key = activeUser.uid,
                factory =
                  StoreViewModel.factory(
                    application = application,
                    authUid = activeUser.uid,
                    displayName = activeUser.displayName.orEmpty(),
                    userEmail = activeUser.email.orEmpty(),
                  ),
              )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            TbPutraRiApp(
              uiState = uiState,
              viewModel = viewModel,
              onGoogleSignOut = {
                scope.launch {
                  authRepository.signOut {
                    viewModel.logout()
                  }
                }
              },
            )
          }
        }
      }
    }
  }
}

@Composable
fun TbPutraRiApp(
  uiState: StoreUiState,
  viewModel: StoreViewModel,
  onGoogleSignOut: () -> Unit = {},
) {
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(uiState.statusMessage) {
    uiState.statusMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      viewModel.consumeStatusMessage()
    }
  }

  val activeRole = uiState.settings.activeRole
  if (activeRole == null) {
    LoginScreen(
      settings = uiState.settings,
      cloudSync = uiState.cloudSync,
      googleEmail = uiState.googleUserEmail,
      googleDisplayName = uiState.googleDisplayName,
      loginError = uiState.loginError,
      onClearError = viewModel::clearLoginError,
      onLogin = viewModel::attemptLogin,
      onJoinStoreByCode = viewModel::joinStoreByCode,
      onCreateNewStore = viewModel::createNewStore,
      onSwitchToOwnStore = viewModel::switchToOwnStore,
      onRetrySync = viewModel::retryCloudSync,
      onGoogleSignOut = onGoogleSignOut,
    )
    return
  }

  if (uiState.activeTab != AppTab.POS) {
    BackHandler { viewModel.selectTab(AppTab.POS) }
  }

  val visibleTabs =
    remember {
      listOf(AppTab.POS, AppTab.STOCK, AppTab.FINANCE, AppTab.ACCOUNT)
    }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useRail = maxWidth >= 720.dp
    val isCompactHeight = maxHeight < 520.dp

    Scaffold(
      modifier = Modifier.fillMaxSize(),
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      snackbarHost = {
        SnackbarHost(hostState = snackbarHostState) { data ->
          Snackbar(
            snackbarData = data,
            shape = RoundedCornerShape(14.dp),
            containerColor = com.example.ui.theme.Navy900,
            contentColor = Color.White,
          )
        }
      },
      topBar = {
        StoreTopBar(
          userName = uiState.activeUserName,
          role = activeRole,
          onLogout = viewModel::logout,
          storeName = uiState.settings.storeName,
          compact = isCompactHeight,
        )
      },
      bottomBar = {
        if (!useRail) {
          Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
            NavigationBar(
              containerColor = MaterialTheme.colorScheme.surface,
              tonalElevation = 0.dp,
              windowInsets = WindowInsets.navigationBars,
            ) {
              visibleTabs.forEach { tab ->
                val selected = uiState.activeTab == tab
                val label = tabLabelForRole(tab, activeRole)
                NavigationBarItem(
                  selected = selected,
                  onClick = { viewModel.selectTab(tab) },
                  icon = { TabIconWithBadge(tab, selected, uiState) },
                  label = {
                    Text(
                      text = label,
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                      maxLines = 1,
                    )
                  },
                  colors =
                    NavigationBarItemDefaults.colors(
                      selectedIconColor = Orange600,
                      selectedTextColor = MaterialTheme.colorScheme.onSurface,
                      unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                      unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                      indicatorColor = Orange100,
                    ),
                  modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}"),
                )
              }
            }
          }
        }
      },
    ) { innerPadding ->
      val bodyInsets =
        if (useRail) {
          WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
        } else {
          WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
        }
      Row(
        modifier =
          Modifier.fillMaxSize()
            .padding(innerPadding)
            .windowInsetsPadding(bodyInsets),
      ) {
        if (useRail) {
          NavigationRail(
            modifier = Modifier.fillMaxHeight().width(if (isCompactHeight) 74.dp else 80.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            windowInsets = WindowInsets(0, 0, 0, 0),
          ) {
            Column(
              modifier =
                Modifier.fillMaxHeight()
                  .verticalScroll(rememberScrollState())
                  .padding(vertical = if (isCompactHeight) 4.dp else 10.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement =
                Arrangement.spacedBy(if (isCompactHeight) 0.dp else 4.dp),
            ) {
              visibleTabs.forEach { tab ->
                val selected = uiState.activeTab == tab
                val label = tabLabelForRole(tab, activeRole)
                NavigationRailItem(
                  selected = selected,
                  onClick = { viewModel.selectTab(tab) },
                  icon = { TabIconWithBadge(tab, selected, uiState) },
                  label = {
                    Text(
                      text = label,
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                      maxLines = 1,
                    )
                  },
                  colors =
                    NavigationRailItemDefaults.colors(
                      selectedIconColor = Orange600,
                      selectedTextColor = MaterialTheme.colorScheme.onSurface,
                      unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                      unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                      indicatorColor = Orange100,
                    ),
                  modifier = Modifier.testTag("rail_tab_${tab.name.lowercase()}"),
                )
              }
            }
          }
          VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
        }

        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
          when (uiState.activeTab) {
            AppTab.POS ->
              PosScreen(
                uiState = uiState,
                onSearchChange = viewModel::updatePosSearch,
                onCategorySelect = viewModel::selectPosCategory,
                onAddToCart = { product, retail -> viewModel.addToCart(product, retail) },
                onUpdateCartQty = viewModel::updateCartItemQuantity,
                onUpdateCartItemPrice = viewModel::updateCartItemUnitPrice,
                onRemoveCartItem = viewModel::removeCartItem,
                onClearCart = viewModel::clearCart,
                onSelectCustomerType = viewModel::selectCustomerType,
                onUpdateManualDiscount = viewModel::updateManualDiscount,
                onProcessCheckout = viewModel::processCheckout,
                onOpenReceipt = viewModel::openReceipt,
              )
            AppTab.STOCK ->
              StockScreen(
                uiState = uiState,
                onSearchChange = viewModel::updateStockSearch,
                onCategorySelect = viewModel::selectStockCategory,
                onSaveProduct = viewModel::saveProduct,
                onDeleteProduct = viewModel::deleteProduct,
                onRestockProduct = viewModel::restockProduct,
                onRepackToRetail = viewModel::repackToRetail,
                onSaveCategory = viewModel::saveCategory,
                onDeleteCategory = viewModel::deleteCategory,
              )
            AppTab.FINANCE ->
              FinanceScreen(
                uiState = uiState,
                onSelectDateFilter = viewModel::selectFinanceDateFilter,
                onAddExpense = viewModel::addExpense,
                onDeleteExpense = viewModel::deleteExpense,
                onPayKasbon = viewModel::payKasbon,
                onOpenReceipt = viewModel::openReceipt,
                onDeleteTransaction = viewModel::deleteTransaction,
              )
            AppTab.ACCOUNT ->
              AccountSettingsScreen(
                settings = uiState.settings,
                employees = uiState.employees,
                activeEmployee = uiState.activeEmployee,
                cloudSync = uiState.cloudSync,
                googleEmail = uiState.googleUserEmail,
                onSaveOwnerSettings = viewModel::updateOwnerAccountSettings,
                onSaveCashierAccount = viewModel::updateOwnCashierAccount,
                onSaveEmployee = viewModel::saveEmployee,
                onDeleteEmployee = viewModel::deleteEmployee,
                onResetDemoData = viewModel::resetDemoData,
                onGoogleSignOut = onGoogleSignOut,
              )
          }
        }
      }
    }
  }

  uiState.lastCompletedTransaction?.let { tx ->
    ReceiptModal(
      transaction = tx,
      settings = uiState.settings,
      onSavePrintPreferences = viewModel::saveThermalPrintPreferences,
      onDismiss = viewModel::dismissReceipt,
    )
  }
}

private fun tabLabelForRole(tab: AppTab, role: UserRole): String =
  when (tab) {
    AppTab.POS -> "Kasir"
    AppTab.STOCK -> if (role == UserRole.OWNER) "Stok" else "Cek Stok"
    AppTab.FINANCE -> "Keuangan"
    AppTab.ACCOUNT -> "Akun"
  }

@Composable
private fun TabIconWithBadge(tab: AppTab, selected: Boolean, uiState: StoreUiState) {
  val icon =
    when (tab) {
      AppTab.POS -> if (selected) Icons.Filled.PointOfSale else Icons.Outlined.PointOfSale
      AppTab.STOCK -> if (selected) Icons.Filled.Inventory2 else Icons.Outlined.Inventory2
      AppTab.FINANCE ->
        if (selected) Icons.Filled.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet
      AppTab.ACCOUNT -> if (selected) Icons.Filled.ManageAccounts else Icons.Outlined.ManageAccounts
    }

  val badgeCount =
    when (tab) {
      AppTab.STOCK -> uiState.lowStockCount
      AppTab.FINANCE -> uiState.unpaidKasbonList.size
      else -> 0
    }

  if (badgeCount > 0) {
    BadgedBox(
      badge = {
        Badge(containerColor = Orange600, contentColor = Color.White) {
          Text(badgeCount.toString())
        }
      }
    ) {
      Icon(icon, contentDescription = tab.label)
    }
  } else {
    Icon(icon, contentDescription = tab.label)
  }
}
