package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Orange600
import com.example.ui.theme.Red100
import com.example.ui.theme.Red400
import com.example.ui.theme.Red600

@Composable
fun GoogleSignInScreen(
  isLoading: Boolean,
  errorMessage: String?,
  onSignInClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val isDark = isSystemInDarkTheme()
  val cloudAccent = if (isDark) Emerald400 else Emerald600
  val errorBg = if (isDark) Red400.copy(alpha = 0.16f) else Red100
  val errorFg = if (isDark) Red400 else Red600

  BoxWithConstraints(
    modifier =
      modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors =
              if (isDark) {
                listOf(
                  MaterialTheme.colorScheme.surface,
                  MaterialTheme.colorScheme.background,
                )
              } else {
                listOf(
                  MaterialTheme.colorScheme.background,
                  MaterialTheme.colorScheme.surface,
                )
              }
          )
        )
        .statusBarsPadding()
        .navigationBarsPadding(),
    contentAlignment = Alignment.Center,
  ) {
    val viewportHeight = maxHeight
    Column(
      modifier =
        Modifier.widthIn(max = 480.dp)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .heightIn(min = viewportHeight)
          .padding(horizontal = 24.dp, vertical = 28.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Spacer(modifier = Modifier.height(16.dp))

      // Centered Minimal Brand Hero
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Surface(
          modifier = Modifier.size(88.dp),
          shape = RoundedCornerShape(24.dp),
          color = Color.White,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          shadowElevation = 6.dp,
        ) {
          Image(
            painter = painterResource(id = R.drawable.tb_putra_ri_logo),
            contentDescription = "Logo TB PUTRA RI",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().padding(8.dp),
          )
        }

        Spacer(modifier = Modifier.height(22.dp))

        Text(
          text = "TB PUTRA RI",
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.6.sp,
          color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = "Kasir, Stok & Pembukuan Toko",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
          shape = RoundedCornerShape(999.dp),
          color = cloudAccent.copy(alpha = 0.12f),
          border = BorderStroke(1.dp, cloudAccent.copy(alpha = 0.25f)),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Default.CloudDone,
              contentDescription = null,
              tint = cloudAccent,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Sinkronisasi Cloud Real-Time",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
              color = cloudAccent,
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text =
            "Masuk dengan akun Google untuk mencadangkan data toko dan menyinkronkan perangkat Pemilik maupun Kasir secara otomatis.",
          style = MaterialTheme.typography.bodySmall,
          lineHeight = 20.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(horizontal = 12.dp),
        )
      }

      // Bottom Thumb-Zone Action Area
      Column(
        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        if (!errorMessage.isNullOrBlank()) {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = errorBg,
          ) {
            Text(
              text = errorMessage,
              style = MaterialTheme.typography.bodySmall,
              color = errorFg,
              fontWeight = FontWeight.SemiBold,
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            )
          }
        }

        Button(
          onClick = onSignInClick,
          enabled = !isLoading,
          modifier = Modifier.fillMaxWidth().height(54.dp).testTag("google_signin_button"),
          shape = RoundedCornerShape(16.dp),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = Orange600,
              contentColor = Color.White,
            ),
        ) {
          if (isLoading) {
            CircularProgressIndicator(
              modifier = Modifier.size(20.dp),
              color = Color.White,
              strokeWidth = 2.5.dp,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Menghubungkan Akun...",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = Color.White,
            )
          } else {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Login,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Masuk dengan Akun Google",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = Color.White,
            )
          }
        }

        Text(
          text = "Data stok, kasbon, dan transaksi tersimpan aman di cloud",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
          textAlign = TextAlign.Center,
        )
      }
    }
  }
}
