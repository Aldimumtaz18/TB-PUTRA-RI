package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.StoreSettings
import com.example.data.repository.CloudSyncInfo
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Emerald600
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.Orange400
import com.example.ui.theme.Orange600
import com.example.ui.theme.Red100
import com.example.ui.theme.Red400
import com.example.ui.theme.Red600

private val STORE_OWNER_USERNAME_REGEX = Regex("^[a-zA-Z0-9_.\\-]{3,40}$")

@Composable
fun LoginScreen(
  settings: StoreSettings,
  cloudSync: CloudSyncInfo,
  googleEmail: String,
  googleDisplayName: String,
  loginError: String?,
  onClearError: () -> Unit,
  onLogin: (String, String) -> Unit,
  onJoinStoreByCode: (String) -> Unit,
  onCreateNewStore: (String, String, String, String, String, Boolean) -> Unit,
  onSwitchToOwnStore: () -> Unit,
  onRetrySync: () -> Unit,
  onGoogleSignOut: () -> Unit,
) {
  var usernameInput by remember { mutableStateOf("") }
  var passwordInput by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }
  var showJoinDialog by remember { mutableStateOf(false) }
  var showCreateStoreDialog by remember { mutableStateOf(false) }
  var joinCodeInput by remember { mutableStateOf("") }
  var copiedHint by remember { mutableStateOf(false) }

  val isDark = isSystemInDarkTheme()
  val emeraldBg = if (isDark) Emerald600.copy(alpha = 0.16f) else Emerald100
  val emeraldFg = if (isDark) Emerald400 else Emerald600
  val errorBg = if (isDark) Red400.copy(alpha = 0.16f) else Red100
  val errorFg = if (isDark) Red400 else Red600
  val accentFg = if (isDark) Orange400 else Orange600

  val focusManager = LocalFocusManager.current
  @Suppress("DEPRECATION")
  val clipboardManager = LocalClipboardManager.current

  BoxWithConstraints(
    modifier =
      Modifier.fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors =
              if (isDark) {
                listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.background)
              } else {
                listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surface)
              }
          )
        )
        .statusBarsPadding()
        .navigationBarsPadding()
        .imePadding(),
    contentAlignment = Alignment.Center,
  ) {
    val viewportHeight = maxHeight
    Column(
      modifier =
        Modifier.widthIn(max = 480.dp)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .heightIn(min = viewportHeight)
          .padding(horizontal = 20.dp, vertical = 22.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      // Clean Minimal Brand Header (no curved blocks or overlapping circles)
      Column(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Surface(
          modifier = Modifier.size(68.dp),
          shape = RoundedCornerShape(18.dp),
          color = Color.White,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          shadowElevation = 4.dp,
        ) {
          Image(
            painter = painterResource(id = R.drawable.tb_putra_ri_logo),
            contentDescription = "Logo ${settings.storeName.ifBlank { "TB PUTRA RI" }}",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().padding(6.dp),
          )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = settings.storeName.ifBlank { "TB PUTRA RI" },
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.5.sp,
          color = MaterialTheme.colorScheme.onBackground,
          textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "Kasir POS, Stok & Pembukuan Toko",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )
      }

      // Clean Login Card
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = "Masuk ke Aplikasi",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = "Masukkan username dan password Pemilik atau Karyawan.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          OutlinedTextField(
            value = usernameInput,
            onValueChange = {
              usernameInput = it
              if (loginError != null) onClearError()
            },
            label = { Text("Username") },
            placeholder = { Text("Contoh: owner atau kasir") },
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions =
              KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Next,
              ),
            keyboardActions =
              KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            modifier = Modifier.fillMaxWidth().testTag("login_username_input"),
          )

          OutlinedTextField(
            value = passwordInput,
            onValueChange = {
              passwordInput = it
              if (loginError != null) onClearError()
            },
            label = { Text("Password") },
            placeholder = { Text("Masukkan password") },
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            },
            trailingIcon = {
              IconButton(
                onClick = { passwordVisible = !passwordVisible },
                modifier = Modifier.minimumInteractiveComponentSize(),
              ) {
                Icon(
                  imageVector =
                    if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                  contentDescription =
                    if (passwordVisible) "Sembunyikan password" else "Lihat password",
                )
              }
            },
            visualTransformation =
              if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions =
              KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
              ),
            keyboardActions =
              KeyboardActions(
                onDone = {
                  focusManager.clearFocus()
                  onLogin(usernameInput, passwordInput)
                }
              ),
            modifier = Modifier.fillMaxWidth().testTag("login_password_input"),
          )

          AnimatedVisibility(visible = loginError != null) {
            Surface(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(12.dp),
              color = errorBg,
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  imageVector = Icons.Default.ErrorOutline,
                  contentDescription = null,
                  tint = errorFg,
                  modifier = Modifier.size(18.dp),
                )
                Text(
                  text = loginError.orEmpty(),
                  color = errorFg,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.SemiBold,
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(2.dp))

          Button(
            onClick = {
              focusManager.clearFocus()
              onLogin(usernameInput, passwordInput)
            },
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("login_submit_button"),
            shape = RoundedCornerShape(14.dp),
            colors =
              ButtonDefaults.buttonColors(
                containerColor = Orange600,
                contentColor = Color.White,
              ),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Login,
              contentDescription = null,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Masuk",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Minimal Cloud Status & Store Actions Card (Buka Toko / Gabung Toko / Keluar Google)
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f),
            ) {
              if (cloudSync.isSyncing) {
                CircularProgressIndicator(
                  modifier = Modifier.size(18.dp),
                  strokeWidth = 2.dp,
                  color = Orange600,
                )
              } else {
                Icon(
                  imageVector = Icons.Default.CloudDone,
                  contentDescription = null,
                  tint = if (cloudSync.cloudError == null) emeraldFg else errorFg,
                  modifier = Modifier.size(18.dp),
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text =
                    if (cloudSync.isSyncing) "Menyinkronkan Cloud..."
                    else "Cloud Aktif (${cloudSync.memberCount} akun)",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                if (googleEmail.isNotBlank()) {
                  Text(
                    text = googleEmail,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }

            if (cloudSync.joinCode.isNotBlank()) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = emeraldBg,
                border = BorderStroke(1.dp, emeraldFg.copy(alpha = 0.28f)),
                modifier =
                  Modifier.clip(RoundedCornerShape(8.dp)).clickable {
                    clipboardManager.setText(AnnotatedString(cloudSync.joinCode))
                    copiedHint = true
                  },
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                ) {
                  Text(
                    text = if (copiedHint) "Tersalin" else cloudSync.joinCode,
                    fontFamily = JetBrainsMono,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = emeraldFg,
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Salin Kode Toko",
                    tint = emeraldFg,
                    modifier = Modifier.size(12.dp),
                  )
                }
              }
            }
          }

          if (!cloudSync.cloudError.isNullOrBlank()) {
            Surface(shape = RoundedCornerShape(10.dp), color = errorBg, modifier = Modifier.fillMaxWidth()) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = cloudSync.cloudError,
                  style = MaterialTheme.typography.labelSmall,
                  color = errorFg,
                  modifier = Modifier.weight(1f),
                )
                IconButton(
                  onClick = onRetrySync,
                  modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                  Icon(Icons.Default.Refresh, contentDescription = "Coba sinkron ulang", tint = errorFg)
                }
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            OutlinedButton(
              onClick = { showCreateStoreDialog = true },
              modifier = Modifier.weight(1f).height(48.dp).testTag("open_create_store_button"),
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(1.dp, accentFg.copy(alpha = 0.45f)),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = accentFg),
              contentPadding = PaddingValues(horizontal = 10.dp),
            ) {
              Icon(Icons.Default.Store, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Buka Toko", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            OutlinedButton(
              onClick = {
                joinCodeInput = ""
                showJoinDialog = true
              },
              modifier = Modifier.weight(1f).height(48.dp).testTag("open_join_store_button"),
              shape = RoundedCornerShape(12.dp),
              contentPadding = PaddingValues(horizontal = 10.dp),
            ) {
              Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Gabung Toko", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1)
            }
          }

          TextButton(
            onClick = onGoogleSignOut,
            modifier = Modifier.fillMaxWidth().heightIn(min = 42.dp).testTag("google_signout_button"),
            shape = RoundedCornerShape(10.dp),
          ) {
            Icon(
              Icons.AutoMirrored.Filled.Logout,
              contentDescription = null,
              tint = errorFg,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              "Ganti / Keluar Akun Google",
              style = MaterialTheme.typography.labelMedium,
              color = errorFg,
              fontWeight = FontWeight.SemiBold,
            )
          }
        }
      }
    }
  }

  if (showJoinDialog) {
    AlertDialog(
      onDismissRequest = { showJoinDialog = false },
      title = { Text("Gabung Toko Dengan Kode") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Masukkan Kode Toko dari HP Pemilik agar data stok dan kasir langsung tersinkronisasi real-time.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          OutlinedTextField(
            value = joinCodeInput,
            onValueChange = { joinCodeInput = it.uppercase().filter { ch -> ch.isLetterOrDigit() }.take(12) },
            label = { Text("Kode Toko (contoh: TB48291A)") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("join_code_input"),
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onJoinStoreByCode(joinCodeInput)
            showJoinDialog = false
          },
          enabled = joinCodeInput.length >= 6,
          colors = ButtonDefaults.buttonColors(containerColor = Orange600, contentColor = Color.White),
        ) {
          Text("Gabung")
        }
      },
      dismissButton = { TextButton(onClick = { showJoinDialog = false }) { Text("Batal") } },
    )
  }

  if (showCreateStoreDialog) {
    CreateOrSwitchStoreDialog(
      isOwnerOfCurrentWorkspace = cloudSync.isOwnerOfWorkspace,
      defaultOwnerName = googleDisplayName.ifBlank { "Pemilik Toko" },
      onDismiss = { showCreateStoreDialog = false },
      onSwitchToOwnStore = {
        showCreateStoreDialog = false
        onSwitchToOwnStore()
      },
      onCreateNewStore = { ownerName, username, password, address, phone, includeSamples ->
        showCreateStoreDialog = false
        onCreateNewStore(ownerName, username, password, address, phone, includeSamples)
      },
    )
  }
}

@Composable
private fun CreateOrSwitchStoreDialog(
  isOwnerOfCurrentWorkspace: Boolean,
  defaultOwnerName: String,
  onDismiss: () -> Unit,
  onSwitchToOwnStore: () -> Unit,
  onCreateNewStore: (String, String, String, String, String, Boolean) -> Unit,
) {
  var ownerName by remember { mutableStateOf(defaultOwnerName) }
  var ownerUsername by remember { mutableStateOf("owner") }
  var ownerPassword by remember { mutableStateOf("") }
  var showPassword by remember { mutableStateOf(false) }
  var storeAddress by remember { mutableStateOf("") }
  var storePhone by remember { mutableStateOf("") }
  var includeSampleProducts by remember { mutableStateOf(false) }
  var error by remember { mutableStateOf<String?>(null) }

  val isDark = isSystemInDarkTheme()
  val errorFg = if (isDark) Red400 else Red600

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Icon(Icons.Default.Store, contentDescription = null, tint = Orange600)
        Text("Buka Toko TB PUTRA RI", fontWeight = FontWeight.Bold)
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (!isOwnerOfCurrentWorkspace) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Text(
                text = "Akun Google Anda sedang terhubung ke toko lain.",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
              OutlinedButton(
                onClick = onSwitchToOwnStore,
                modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
                shape = RoundedCornerShape(10.dp),
              ) {
                Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Kembali ke Toko Pribadi Saya", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
              }
            }
          }
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }

        Text(
          text = "Daftar dan buka toko TB PUTRA RI langsung untuk akun Google ini tanpa perlu keluar ke email lain.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
          value = ownerName,
          onValueChange = {
            ownerName = it.take(80)
            error = null
          },
          label = { Text("Nama Pemilik") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().testTag("new_store_owner_name_input"),
        )
        OutlinedTextField(
          value = ownerUsername,
          onValueChange = {
            ownerUsername = it.take(40)
            error = null
          },
          label = { Text("Username Pemilik") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().testTag("new_store_username_input"),
        )
        OutlinedTextField(
          value = ownerPassword,
          onValueChange = {
            ownerPassword = it.take(64)
            error = null
          },
          label = { Text("Password Pemilik (min. 4 karakter)") },
          visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
          trailingIcon = {
            IconButton(
              onClick = { showPassword = !showPassword },
              modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
              Icon(
                if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = if (showPassword) "Sembunyikan" else "Lihat",
              )
            }
          },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().testTag("new_store_password_input"),
        )
        OutlinedTextField(
          value = storeAddress,
          onValueChange = { storeAddress = it.take(200) },
          label = { Text("Alamat Toko (Opsional)") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = storePhone,
          onValueChange = { storePhone = it.take(40) },
          label = { Text("No. HP / WhatsApp Toko (Opsional)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(),
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
              text = "Isi contoh barang bangunan",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
            )
            Text(
              text = "Kosongkan jika ingin input stok sendiri",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Switch(
            checked = includeSampleProducts,
            onCheckedChange = { includeSampleProducts = it },
          )
        }

        if (error != null) {
          Text(
            text = error!!,
            color = errorFg,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val cleanUser = ownerUsername.trim()
          when {
            ownerName.isBlank() -> error = "Nama Pemilik wajib diisi."
            !STORE_OWNER_USERNAME_REGEX.matches(cleanUser) ->
              error = "Username minimal 3 karakter (huruf, angka, ., _, atau -)."
            ownerPassword.length < 4 -> error = "Password minimal 4 karakter."
            else ->
              onCreateNewStore(
                ownerName.trim(),
                cleanUser,
                ownerPassword,
                storeAddress.trim(),
                storePhone.trim(),
                includeSampleProducts,
              )
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = Orange600, contentColor = Color.White),
        modifier = Modifier.testTag("confirm_create_store_button"),
      ) {
        Text("Buka Toko", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Batal") }
    },
  )
}
