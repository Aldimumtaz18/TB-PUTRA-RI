package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.ALL_CATEGORY_LABEL
import com.example.data.model.UserRole
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.Navy950
import com.example.ui.theme.Orange500
import com.example.ui.theme.Orange600

@Composable
fun StoreTopBar(
  userName: String,
  role: UserRole,
  onLogout: () -> Unit,
  modifier: Modifier = Modifier,
  storeName: String = "TB PUTRA RI",
  compact: Boolean = false,
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    color = Navy900,
    shadowElevation = 4.dp,
  ) {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .background(Brush.horizontalGradient(listOf(Navy950, Navy900, Navy800)))
          .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
          .padding(horizontal = 16.dp, vertical = if (compact) 6.dp else 12.dp)
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
          Surface(
            modifier = Modifier.size(if (compact) 36.dp else 44.dp),
            shape = RoundedCornerShape(if (compact) 10.dp else 12.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Orange500.copy(alpha = 0.45f)),
            shadowElevation = 2.dp,
          ) {
            Image(
              painter = painterResource(id = R.drawable.tb_putra_ri_logo),
              contentDescription = "Logo ${storeName.ifBlank { "TB PUTRA RI" }}",
              contentScale = ContentScale.Fit,
              modifier = Modifier.fillMaxSize().padding(3.dp),
            )
          }
          Spacer(modifier = Modifier.width(if (compact) 10.dp else 12.dp))
          Column(verticalArrangement = Arrangement.spacedBy(if (compact) 1.dp else 2.dp)) {
            Text(
              text = storeName.ifBlank { "TB PUTRA RI" },
              style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
              color = Color.White,
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.3.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color =
                  if (role == UserRole.OWNER) Orange500.copy(alpha = 0.25f)
                  else Color.White.copy(alpha = 0.16f),
              ) {
                Text(
                  text = role.badgeLabel,
                  style = MaterialTheme.typography.labelSmall,
                  color = if (role == UserRole.OWNER) Color(0xFFFFD8B5) else Color(0xFFDBEAFE),
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                )
              }
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = userName,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFCBD5E1),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Color.White.copy(alpha = 0.10f),
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
          modifier =
            Modifier.heightIn(min = if (compact) 38.dp else 42.dp)
              .clip(RoundedCornerShape(12.dp))
              .clickable(onClick = onLogout)
              .testTag("logout_button"),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
              Modifier.padding(
                horizontal = if (compact) 10.dp else 12.dp,
                vertical = if (compact) 7.dp else 9.dp,
              ),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Logout,
              contentDescription = "Keluar atau ganti akun",
              tint = Color(0xFFFDBA74),
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Ganti Akun",
              style = MaterialTheme.typography.labelMedium,
              color = Color(0xFFFDBA74),
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
    }
  }
}

@Composable
fun StoreSearchBar(
  query: String,
  onQueryChange: (String) -> Unit,
  placeholder: String,
  modifier: Modifier = Modifier,
  testTag: String = "search_input",
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isFocused by interactionSource.collectIsFocusedAsState()
  val borderColor =
    if (isFocused) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant

  Surface(
    modifier = modifier.fillMaxWidth().height(48.dp),
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(if (isFocused) 1.5.dp else 1.dp, borderColor),
  ) {
    Row(
      modifier = Modifier.fillMaxSize().padding(start = 12.dp, end = if (query.isNotEmpty()) 2.dp else 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.Default.Search,
        contentDescription = "Cari barang",
        tint = if (isFocused) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(18.dp),
      )
      Spacer(modifier = Modifier.width(8.dp))
      Box(
        modifier = Modifier.weight(1f),
        contentAlignment = Alignment.CenterStart,
      ) {
        BasicTextField(
          value = query,
          onValueChange = onQueryChange,
          singleLine = true,
          interactionSource = interactionSource,
          textStyle =
            MaterialTheme.typography.bodyMedium.copy(
              color = MaterialTheme.colorScheme.onSurface,
            ),
          cursorBrush = SolidColor(MaterialTheme.colorScheme.secondary),
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
          modifier = Modifier.fillMaxWidth().testTag(testTag),
          decorationBox = { innerTextField ->
            if (query.isEmpty()) {
              Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            innerTextField()
          },
        )
      }
      if (query.isNotEmpty()) {
        IconButton(
          onClick = { onQueryChange("") },
          modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Hapus pencarian",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }
  }
}

@Composable
fun CategoryFilterRow(
  categories: List<String>,
  selectedCategory: String,
  onSelectCategory: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val allOptions = listOf(ALL_CATEGORY_LABEL) + categories
  Row(
    modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    allOptions.forEach { category ->
      val selected = category.equals(selectedCategory, ignoreCase = true)
      FilterChip(
        selected = selected,
        onClick = { onSelectCategory(category) },
        shape = RoundedCornerShape(10.dp),
        label = {
          Text(
            text = category,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
          )
        },
        colors =
          FilterChipDefaults.filterChipColors(
            selectedContainerColor = Orange600,
            selectedLabelColor = Color.White,
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
          ),
        border =
          FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
            selectedBorderColor = Orange600,
          ),
        modifier = Modifier.testTag("category_chip_${category.lowercase().replace(" ", "_")}"),
      )
    }
  }
}

@Composable
fun MetricStatCard(
  title: String,
  valueText: String,
  subtitle: String,
  icon: ImageVector,
  accentColor: Color,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxSize().padding(12.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Medium,
          lineHeight = 15.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Box(
          modifier =
            Modifier.size(30.dp)
              .clip(RoundedCornerShape(9.dp))
              .background(accentColor.copy(alpha = 0.14f)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = icon,
            contentDescription = title,
            tint = accentColor,
            modifier = Modifier.size(17.dp),
          )
        }
      }
      Column {
        Spacer(modifier = Modifier.height(10.dp))
        val valueFontSize =
          when {
            valueText.length >= 14 -> 13.sp
            valueText.length >= 11 -> 14.sp
            else -> 16.sp
          }
        Text(
          text = valueText,
          fontFamily = JetBrainsMono,
          fontWeight = FontWeight.Bold,
          fontSize = valueFontSize,
          lineHeight = (valueFontSize.value + 4f).sp,
          color = accentColor,
          maxLines = 2,
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
          text = subtitle,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}
