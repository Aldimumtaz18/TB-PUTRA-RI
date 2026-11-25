package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.StoreSettings
import com.example.data.model.EmployeeAccount
import com.example.data.repository.CloudSyncInfo
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

private val CASHIER_USERNAME_REGEX = Regex("^[a-zA-Z0-9_.\\-]{3,40}$")

@Composable
fun CashierAccountSection(
  settings: StoreSettings,
  employees: List<EmployeeAccount>,
  activeEmployee: EmployeeAccount?,
  cloudSync: CloudSyncInfo,
  googleEmail: String,
  onSaveCashierAccount: (String, String, String, String) -> Unit,
  onGoogleSignOut: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val initialName =
    activeEmployee?.name?.takeIf { it.isNotBlank() }
      ?: settings.activeEmployeeName.ifBlank { settings.cashierName }
  val initialPhone = activeEmployee?.phone.orEmpty()
  val initialUsername = activeEmployee?.username ?: settings.cashierUsername
  val initialPassword = activeEmployee?.password ?: settings.cashierPassword

  var name by remember(activeEmployee?.id, initialName) { mutableStateOf(initialName) }
  var phone by remember(activeEmployee?.id, initialPhone) { mutableStateOf(initialPhone) }
  var username by remember(activeEmployee?.id, initialUsername) { mutableStateOf(initialUsername) }
  var password by remember(activeEmployee?.id, initialPassword) { mutableStateOf(initialPassword) }
  var showPassword by remember { mutableStateOf(false) }
  var error by remember { mutableStateOf<String?>(null) }

  val isDark = isSystemInDarkTheme()
  val emeraldBg = if (isDark) Emerald600.copy(alpha = 0.16f) else Emerald100
  val emeraldFg = if (isDark) Emerald400 else Emerald600
  val orangeBg = if (isDark) Orange500.copy(alpha = 0.18f) else Orange100
  val orangeFg = if (isDark) Orange400 else Orange600
  val dangerFg = if (isDark) Red400 else Red600

  val targetId = activeEmployee?.id ?: settings.activeEmployeeId
  val displayInitial = name.trim().firstOrNull()?.uppercase() ?: "K"

  Box(
    modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Cashier Profile & Sync Summary Card
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Box(
              modifier = Modifier.size(48.dp).clip(CircleShape).background(orangeBg),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = displayInitial,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = orangeFg,
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = name.trim().ifBlank { initialName },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = "@${username.trim().ifBlank { initialUsername }}",
                fontFamily = JetBrainsMono,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = emeraldBg,
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Icon(
                  Icons.Default.CloudDone,
                  contentDescription = null,
                  tint = emeraldFg,
                  modifier = Modifier.size(14.dp),
                )
                Text(
                  text = "Kasir Aktif",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = emeraldFg,
                )
              }
            }
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

          SummaryKeyValueRow(label = "Toko", value = settings.storeName)
          SummaryKeyValueRow(
            label = "Status Cloud",
            value = if (cloudSync.isOwnerOfWorkspace) "Pemilik Utama Cloud" else "Perangkat Anggota Cloud",
            valueColor = emeraldFg,
          )
          SummaryKeyValueRow(label = "No. Tlp / WA", value = phone.trim().ifBlank { "-" })
          SummaryKeyValueRow(label = "Akun Google", value = googleEmail.ifBlank { "Terhubung" })
        }
      }

      // Self-Service Profile & Login Credentials Card
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = orangeBg,
              modifier = Modifier.size(38.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  Icons.Default.Key,
                  contentDescription = null,
                  tint = orangeFg,
                  modifier = Modifier.size(20.dp),
                )
              }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Edit Profil & Akun Login",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "Ubah nama tampilan kasir, username, dan password login Anda sendiri.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          OutlinedTextField(
            value = name,
            onValueChange = {
              name = it.take(80)
              error = null
            },
            label = { Text("Nama Lengkap (Profil)") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("cashier_profile_name_input"),
          )

          OutlinedTextField(
            value = phone,
            onValueChange = {
              phone = it.filter { ch -> ch.isDigit() || ch in "+- ()" }.take(40)
              error = null
            },
            label = { Text("No. Telepon / WhatsApp") },
            placeholder = { Text("Contoh: 0812-3456-7890") },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("cashier_phone_input"),
          )

          OutlinedTextField(
            value = username,
            onValueChange = {
              username = it.filter { ch -> ch.isLetterOrDigit() || ch in "._-" }.take(40)
              error = null
            },
            label = { Text("Username Login") },
            supportingText = {
              Text("Minimal 3 karakter (huruf, angka, titik, _, atau -)")
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("cashier_username_input"),
          )

          OutlinedTextField(
            value = password,
            onValueChange = {
              password = it.take(64)
              error = null
            },
            label = { Text("Password Login (min. 4 karakter)") },
            visualTransformation =
              if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
              IconButton(
                onClick = { showPassword = !showPassword },
                modifier = Modifier.minimumInteractiveComponentSize(),
              ) {
                Icon(
                  imageVector =
                    if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                  contentDescription = if (showPassword) "Sembunyikan" else "Lihat",
                )
              }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("cashier_password_input"),
          )

          if (error != null) {
            Text(
              text = error!!,
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
            )
          }

          Button(
            onClick = {
              val cleanName = name.trim()
              val cleanUser = username.trim()
              val clashesOwner = cleanUser.equals(settings.ownerUsername, ignoreCase = true)
              val clashesEmployee =
                employees.any {
                  it.id != targetId && it.username.equals(cleanUser, ignoreCase = true)
                }
              when {
                cleanName.isBlank() ->
                  error = "Nama profil tidak boleh kosong."
                !CASHIER_USERNAME_REGEX.matches(cleanUser) ->
                  error = "Username minimal 3 karakter (huruf, angka, titik, _, atau -)."
                clashesOwner ->
                  error = "Username tidak boleh sama dengan username pemilik."
                clashesEmployee ->
                  error = "Username '$cleanUser' sudah digunakan karyawan lain."
                password.length < 4 ->
                  error = "Password minimal 4 karakter."
                else -> {
                  error = null
                  onSaveCashierAccount(cleanName, cleanUser, password, phone.trim())
                }
              }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("save_cashier_account_button"),
            shape = RoundedCornerShape(14.dp),
            colors =
              ButtonDefaults.buttonColors(
                containerColor = if (isDark) Orange600 else Navy900,
                contentColor = Color.White,
              ),
          ) {
            Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Simpan Perubahan Akun", fontWeight = FontWeight.Bold, color = Color.White)
          }
        }
      }

      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
      ) {
        Text(
          text =
            "Pengaturan toko, akun pemilik, kelola karyawan lain, dan pemulihan data hanya dapat diakses melalui login Pemilik.",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.padding(12.dp),
        )
      }

      OutlinedButton(
        onClick = onGoogleSignOut,
        border = BorderStroke(1.dp, dangerFg.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("settings_google_signout_button"),
        shape = RoundedCornerShape(14.dp),
      ) {
        Icon(
          Icons.AutoMirrored.Filled.Logout,
          contentDescription = null,
          tint = dangerFg,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Keluar Akun Google", color = dangerFg, fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
private fun SummaryKeyValueRow(
  label: String,
  value: String,
  valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.width(12.dp))
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall,
      fontWeight = FontWeight.SemiBold,
      color = valueColor,
      textAlign = TextAlign.End,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f),
    )
  }
}
