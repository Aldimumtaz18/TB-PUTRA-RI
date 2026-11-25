package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.StoreSettings
import com.example.data.model.EmployeeAccount
import com.example.data.model.UserRole
import com.example.data.repository.CloudSyncInfo
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Emerald600
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.Navy100
import com.example.ui.theme.Navy900
import com.example.ui.theme.Orange100
import com.example.ui.theme.Orange400
import com.example.ui.theme.Orange500
import com.example.ui.theme.Orange600
import com.example.ui.theme.Red400
import com.example.ui.theme.Red600
import com.example.ui.theme.Sky400

private val USERNAME_REGEX = Regex("^[a-zA-Z0-9_.\\-]{3,40}$")

@Composable
fun AccountSettingsScreen(
  settings: StoreSettings,
  employees: List<EmployeeAccount>,
  activeEmployee: EmployeeAccount?,
  cloudSync: CloudSyncInfo,
  googleEmail: String,
  onSaveOwnerSettings: (String, String, String, String, String) -> Unit,
  onSaveCashierAccount: (String, String, String, String) -> Unit,
  onSaveEmployee: (String?, String, String, String, String) -> Unit,
  onDeleteEmployee: (EmployeeAccount) -> Unit,
  onResetDemoData: () -> Unit,
  onGoogleSignOut: () -> Unit,
  modifier: Modifier = Modifier,
) {
  if (settings.activeRole == UserRole.CASHIER) {
    CashierAccountSection(
      settings = settings,
      employees = employees,
      activeEmployee = activeEmployee,
      cloudSync = cloudSync,
      googleEmail = googleEmail,
      onSaveCashierAccount = onSaveCashierAccount,
      onGoogleSignOut = onGoogleSignOut,
      modifier = modifier,
    )
    return
  }
  var ownerName by remember(settings) { mutableStateOf(settings.ownerName) }
  var ownerUsername by remember(settings) { mutableStateOf(settings.ownerUsername) }
  var ownerPassword by remember(settings) { mutableStateOf(settings.ownerPassword) }
  var showOwnerPassword by remember { mutableStateOf(false) }

  var storeAddress by remember(settings) { mutableStateOf(settings.storeAddress) }
  var storePhone by remember(settings) { mutableStateOf(settings.storePhone) }
  var error by remember { mutableStateOf<String?>(null) }
  var showResetDialog by remember { mutableStateOf(false) }
  var copiedCode by remember { mutableStateOf(false) }

  // Employee management dialogs
  var showEmployeeDialog by remember { mutableStateOf(false) }
  var editingEmployee by remember { mutableStateOf<EmployeeAccount?>(null) }
  var employeePendingDelete by remember { mutableStateOf<EmployeeAccount?>(null) }

  val context = LocalContext.current
  @Suppress("DEPRECATION")
  val clipboardManager = LocalClipboardManager.current
  val isDark = isSystemInDarkTheme()
  val emeraldBg = if (isDark) Emerald600.copy(alpha = 0.16f) else Emerald100
  val emeraldBorder = if (isDark) Emerald400.copy(alpha = 0.35f) else Emerald600.copy(alpha = 0.25f)
  val emeraldFg = if (isDark) Emerald400 else Emerald600
  val orangeBg = if (isDark) Orange500.copy(alpha = 0.18f) else Orange100
  val orangeFg = if (isDark) Orange400 else Orange600
  val navyIconBg = if (isDark) Sky400.copy(alpha = 0.16f) else Navy100
  val navyIconFg = if (isDark) Sky400 else Navy900
  val dangerFg = if (isDark) Red400 else Red600

  Box(
    modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Cloud Sync Card
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
              color = emeraldBg,
              modifier = Modifier.size(38.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  Icons.Default.CloudDone,
                  contentDescription = null,
                  tint = emeraldFg,
                  modifier = Modifier.size(20.dp),
                )
              }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Sinkronisasi Cloud & Multi-HP",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text =
                  if (cloudSync.isOwnerOfWorkspace) "Pemilik Utama Cloud" else "Perangkat Anggota Cloud",
                style = MaterialTheme.typography.labelSmall,
                color = emeraldFg,
                fontWeight = FontWeight.SemiBold,
              )
            }
          }

          Text(
            text = "Akun Google: ${googleEmail.ifBlank { "Terhubung" }}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          Surface(
            shape = RoundedCornerShape(14.dp),
            color = emeraldBg,
            border = BorderStroke(1.dp, emeraldBorder),
            modifier = Modifier.fillMaxWidth(),
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
                Text(
                  text = "Kode Toko",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.SemiBold,
                  color = emeraldFg,
                )
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = if (isDark) Emerald600.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.8f),
                ) {
                  Text(
                    text = "${cloudSync.memberCount} akun terhubung",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = emeraldFg,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                  )
                }
              }

              Text(
                text = cloudSync.joinCode.ifBlank { "-" },
                fontFamily = JetBrainsMono,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.onSurface,
              )

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                OutlinedButton(
                  onClick = {
                    if (cloudSync.joinCode.isNotBlank()) {
                      clipboardManager.setText(AnnotatedString(cloudSync.joinCode))
                      copiedCode = true
                    }
                  },
                  shape = RoundedCornerShape(10.dp),
                  border = BorderStroke(1.dp, emeraldBorder),
                  colors = ButtonDefaults.outlinedButtonColors(contentColor = emeraldFg),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                  modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                ) {
                  Icon(Icons.Default.ContentCopy, contentDescription = "Salin", modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = if (copiedCode) "Tersalin" else "Salin Kode",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                  )
                }
                Button(
                  onClick = {
                    val msg =
                      "Gabung ke POS TB PUTRA RI secara real-time! Buka aplikasi -> Login Google -> tekan Gabung Toko -> masukkan Kode Toko: ${cloudSync.joinCode}"
                    runCatching {
                      context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/?text=${Uri.encode(msg)}"))
                      )
                    }
                  },
                  shape = RoundedCornerShape(10.dp),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                  modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = Emerald600, contentColor = Color.White),
                ) {
                  Icon(Icons.Default.Share, contentDescription = "Kirim WA", modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "Kirim WA",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                  )
                }
              }
            }
          }

          Text(
            text =
              "Tips: Login dengan akun Google yang sama di 2 HP langsung sinkron otomatis. Jika HP Karyawan memakai akun Google berbeda, pilih 'Gabung Toko' di halaman Login lalu masukkan Kode Toko di atas.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

    if (!cloudSync.isOwnerOfWorkspace) {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
      ) {
        Text(
          text = "Perangkat ini terhubung sebagai Anggota. Kelola karyawan, perubahan akun pemilik, identitas toko, dan reset data hanya bisa dilakukan dari akun Google Pemilik Utama.",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.padding(12.dp),
        )
      }
    }

    // Kelola Karyawan Section
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
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = orangeBg,
              modifier = Modifier.size(38.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  Icons.Default.Groups,
                  contentDescription = null,
                  tint = orangeFg,
                  modifier = Modifier.size(20.dp),
                )
              }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Kelola Karyawan (${employees.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = "Fitur Kelola Login Karyawan",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              editingEmployee = null
              showEmployeeDialog = true
            },
            enabled = cloudSync.isOwnerOfWorkspace,
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier.heightIn(min = 40.dp).testTag("add_employee_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Orange600, contentColor = Color.White),
          ) {
            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Tambah", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1)
          }
        }

        if (employees.isEmpty()) {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(16.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                Icons.Default.Groups,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp),
              )
              Text(
                text = "Belum ada akun karyawan",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
              )
              Text(
                text = "Tekan tombol Tambah di atas untuk membuat username & password karyawan.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            employees.forEach { emp ->
              Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.45f else 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Box(
                    modifier = Modifier.size(38.dp).clip(CircleShape).background(orangeBg),
                    contentAlignment = Alignment.Center,
                  ) {
                    Text(
                      text = emp.name.trim().firstOrNull()?.uppercase() ?: "K",
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold,
                      color = orangeFg,
                    )
                  }
                  Spacer(modifier = Modifier.width(12.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = emp.name,
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.Bold,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                      text =
                        if (emp.phone.isNotBlank()) "@${emp.username} • ${emp.phone}"
                        else "@${emp.username}",
                      fontFamily = JetBrainsMono,
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                  IconButton(
                    onClick = {
                      editingEmployee = emp
                      showEmployeeDialog = true
                    },
                    enabled = cloudSync.isOwnerOfWorkspace,
                    modifier =
                      Modifier.size(48.dp).testTag("edit_employee_${emp.id}"),
                  ) {
                    Icon(
                      imageVector = Icons.Default.Edit,
                      contentDescription = "Edit ${emp.name}",
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(20.dp),
                    )
                  }
                  IconButton(
                    onClick = { employeePendingDelete = emp },
                    enabled = cloudSync.isOwnerOfWorkspace,
                    modifier =
                      Modifier.size(48.dp).testTag("delete_employee_${emp.id}"),
                  ) {
                    Icon(
                      imageVector = Icons.Default.DeleteOutline,
                      contentDescription = "Hapus ${emp.name}",
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
    }

    // Owner Credentials Card
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = navyIconBg,
            modifier = Modifier.size(38.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                Icons.Default.AdminPanelSettings,
                contentDescription = null,
                tint = navyIconFg,
                modifier = Modifier.size(20.dp),
              )
            }
          }
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "Akun Pemilik (Owner)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
        }
        OutlinedTextField(
          value = ownerName,
          onValueChange = { ownerName = it },
          label = { Text("Nama Pemilik (Owner)") },
          singleLine = true,
          enabled = cloudSync.isOwnerOfWorkspace,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = ownerUsername,
          onValueChange = { ownerUsername = it.filter { ch -> ch.isLetterOrDigit() || ch in "._-" }.take(40) },
          label = { Text("Username Pemilik") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
          singleLine = true,
          enabled = cloudSync.isOwnerOfWorkspace,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().testTag("settings_owner_username_input"),
        )
        OutlinedTextField(
          value = ownerPassword,
          onValueChange = { ownerPassword = it.take(64) },
          label = { Text("Password Pemilik (min. 4 karakter)") },
          visualTransformation = if (showOwnerPassword) VisualTransformation.None else PasswordVisualTransformation(),
          trailingIcon = {
            IconButton(
              onClick = { showOwnerPassword = !showOwnerPassword },
              modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
              Icon(
                if (showOwnerPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = if (showOwnerPassword) "Sembunyikan" else "Lihat",
              )
            }
          },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
          singleLine = true,
          enabled = cloudSync.isOwnerOfWorkspace,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().testTag("settings_owner_password_input"),
        )
      }
    }

    // Store Receipt Identity Card
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = navyIconBg,
            modifier = Modifier.size(38.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                Icons.Default.Store,
                contentDescription = null,
                tint = navyIconFg,
                modifier = Modifier.size(20.dp),
              )
            }
          }
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "Identitas Struk TB PUTRA RI",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
        }
        OutlinedTextField(
          value = storeAddress,
          onValueChange = { storeAddress = it },
          label = { Text("Alamat Toko") },
          singleLine = true,
          enabled = cloudSync.isOwnerOfWorkspace,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = storePhone,
          onValueChange = { storePhone = it },
          label = { Text("No. Telepon / WhatsApp Toko") },
          singleLine = true,
          enabled = cloudSync.isOwnerOfWorkspace,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(),
        )
      }
    }

    if (error != null) {
      Text(
        text = error!!,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.labelMedium,
      )
    }

    Button(
      onClick = {
        val cleanOwnerUser = ownerUsername.trim()
        val clashesEmployee =
          employees.any { it.username.equals(cleanOwnerUser, ignoreCase = true) }
        when {
          ownerName.isBlank() ->
            error = "Nama Pemilik tidak boleh kosong."
          !USERNAME_REGEX.matches(cleanOwnerUser) ->
            error = "Username Pemilik minimal 3 karakter (huruf, angka, titik, garis bawah, atau -)."
          clashesEmployee ->
            error = "Username Pemilik tidak boleh sama dengan username karyawan."
          ownerPassword.length < 4 ->
            error = "Password Pemilik minimal 4 karakter."
          else -> {
            error = null
            onSaveOwnerSettings(
              cleanOwnerUser,
              ownerPassword,
              ownerName.trim(),
              storeAddress.trim(),
              storePhone.trim(),
            )
          }
        }
      },
      enabled = cloudSync.isOwnerOfWorkspace,
      modifier = Modifier.fillMaxWidth().height(52.dp).testTag("save_settings_button"),
      shape = RoundedCornerShape(14.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = if (isDark) Orange600 else Navy900,
        contentColor = Color.White,
      ),
    ) {
      Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
      Spacer(modifier = Modifier.width(8.dp))
      Text("Simpan Pengaturan Pemilik & Toko", fontWeight = FontWeight.Bold, color = Color.White)
    }

    OutlinedButton(
      onClick = { showResetDialog = true },
      enabled = cloudSync.isOwnerOfWorkspace,
      modifier = Modifier.fillMaxWidth().height(48.dp),
      shape = RoundedCornerShape(14.dp),
    ) {
      Icon(
        Icons.Default.RestartAlt,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text("Pulihkan Data Demo Awal", color = MaterialTheme.colorScheme.onSurfaceVariant)
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

  if (showEmployeeDialog) {
    EmployeeEditorDialog(
      existing = editingEmployee,
      ownerUsername = ownerUsername.trim(),
      existingEmployees = employees,
      onDismiss = {
        showEmployeeDialog = false
        editingEmployee = null
      },
      onSave = { name, username, password, phone ->
        onSaveEmployee(editingEmployee?.id, name, username, password, phone)
        showEmployeeDialog = false
        editingEmployee = null
      },
    )
  }

  employeePendingDelete?.let { target ->
    AlertDialog(
      onDismissRequest = { employeePendingDelete = null },
      title = { Text("Hapus Karyawan?") },
      text = {
        Text(
          "Akun '${target.name}' (@${target.username}) akan dihapus permanen dan tidak bisa digunakan untuk masuk lagi."
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onDeleteEmployee(target)
            employeePendingDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = Red600, contentColor = Color.White),
        ) {
          Text("Hapus")
        }
      },
      dismissButton = {
        TextButton(onClick = { employeePendingDelete = null }) { Text("Batal") }
      },
    )
  }

  if (showResetDialog) {
    AlertDialog(
      onDismissRequest = { showResetDialog = false },
      title = { Text("Pulihkan Data Demo?") },
      text = {
        Text(
          "Semua data barang, transaksi, dan pengeluaran di cloud saat ini akan dikembalikan ke 8 produk & contoh transaksi awal."
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onResetDemoData()
            showResetDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = Orange600, contentColor = Color.White),
        ) {
          Text("Pulihkan")
        }
      },
      dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text("Batal") } },
    )
  }
}

@Composable
private fun EmployeeEditorDialog(
  existing: EmployeeAccount?,
  ownerUsername: String,
  existingEmployees: List<EmployeeAccount>,
  onDismiss: () -> Unit,
  onSave: (String, String, String, String) -> Unit,
) {
  var name by remember(existing) { mutableStateOf(existing?.name.orEmpty()) }
  var phone by remember(existing) { mutableStateOf(existing?.phone.orEmpty()) }
  var username by remember(existing) { mutableStateOf(existing?.username.orEmpty()) }
  var password by remember(existing) { mutableStateOf(existing?.password.orEmpty()) }
  var showPassword by remember { mutableStateOf(false) }
  var error by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(
          if (existing == null) Icons.Default.PersonAdd else Icons.Default.Key,
          contentDescription = null,
          tint = Orange600,
        )
        Text(if (existing == null) "Tambah Karyawan" else "Edit Karyawan")
      }
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = name,
          onValueChange = {
            name = it.take(80)
            error = null
          },
          label = { Text("Nama Karyawan") },
          placeholder = { Text("Contoh: Budi Santoso") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("employee_name_input"),
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
          modifier = Modifier.fillMaxWidth().testTag("employee_phone_input"),
        )
        OutlinedTextField(
          value = username,
          onValueChange = {
            username = it.filter { ch -> ch.isLetterOrDigit() || ch in "._-" }.take(40)
            error = null
          },
          label = { Text("Username Login") },
          placeholder = { Text("Contoh: budi_kasir") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("employee_username_input"),
        )
        OutlinedTextField(
          value = password,
          onValueChange = {
            password = it.take(64)
            error = null
          },
          label = { Text("Password (min. 4 karakter)") },
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
          modifier = Modifier.fillMaxWidth().testTag("employee_password_input"),
        )
        if (error != null) {
          Text(
            text = error!!,
            color = Red600,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val cleanName = name.trim()
          val cleanUser = username.trim()
          val cleanPhone = phone.trim()
          val duplicateUsername =
            existingEmployees.any {
              it.id != existing?.id && it.username.equals(cleanUser, ignoreCase = true)
            }
          when {
            cleanName.isBlank() ->
              error = "Nama karyawan wajib diisi."
            !USERNAME_REGEX.matches(cleanUser) ->
              error = "Username minimal 3 karakter (huruf, angka, titik, _, atau -)."
            cleanUser.equals(ownerUsername, ignoreCase = true) ->
              error = "Username karyawan tidak boleh sama dengan username pemilik."
            duplicateUsername ->
              error = "Username '$cleanUser' sudah digunakan karyawan lain."
            password.length < 4 ->
              error = "Password minimal 4 karakter."
            else -> onSave(cleanName, cleanUser, password, cleanPhone)
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = Orange600, contentColor = Color.White),
        modifier = Modifier.testTag("save_employee_button"),
      ) {
        Text(if (existing == null) "Tambah" else "Simpan")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Batal") }
    },
  )
}
