package com.suseoaa.locationspoofer.ui.screen

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suseoaa.locationspoofer.R
import com.suseoaa.locationspoofer.data.model.AppState
import com.suseoaa.locationspoofer.data.model.CatchLogCategory
import com.suseoaa.locationspoofer.data.model.CatchLogEntry
import com.suseoaa.locationspoofer.ui.theme.AccentBlue
import com.suseoaa.locationspoofer.ui.theme.AccentGreen
import com.suseoaa.locationspoofer.ui.theme.AccentOrange
import com.suseoaa.locationspoofer.ui.theme.AppColors
import com.suseoaa.locationspoofer.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CatchLogScreen(
    viewModel: MainViewModel,
    uiState: AppState,
    isDark: Boolean,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var searchQuery by remember { mutableStateOf("") }
    var showClearConfirm by remember { mutableStateOf(false) }

    // تحديث تلقائي للسجل عندما تكون شاشة سجل الصيد مفتوحة
    LaunchedEffect(uiState.catchLogAutoRefresh, uiState.enableCatchLog) {
        viewModel.refreshCatchLogs()
        while (uiState.catchLogAutoRefresh && uiState.enableCatchLog) {
            delay(2000L)
            viewModel.refreshCatchLogs()
        }
    }

    val filteredLogs = remember(uiState.catchLogs, uiState.selectedCatchLogCategory, searchQuery) {
        uiState.catchLogs.filter { entry ->
            val matchesPkg = entry.packageName.substringBefore(":") == "com.logistics.rider.hungerstation"
            val matchesCat = uiState.selectedCatchLogCategory == CatchLogCategory.ALL ||
                entry.category.equals(uiState.selectedCatchLogCategory.name, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                entry.title.contains(searchQuery, ignoreCase = true) ||
                entry.detail.contains(searchQuery, ignoreCase = true) ||
                (entry.lat != null && entry.lat.toString().contains(searchQuery)) ||
                (entry.lng != null && entry.lng.toString().contains(searchQuery))
            matchesPkg && matchesCat && matchesSearch
        }
    }

    val orderCount = remember(uiState.catchLogs) { uiState.catchLogs.count { it.category == "ORDER" } }
    val locCount = remember(uiState.catchLogs) { uiState.catchLogs.count { it.category == "LOCATION" } }
    val secCount = remember(uiState.catchLogs) { uiState.catchLogs.count { it.category == "SECURITY" } }
    val envCount = remember(uiState.catchLogs) { uiState.catchLogs.count { it.category == "ENV" } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // الشريط العلوي
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.topBarBackground(isDark))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.catch_log_title),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "com.logistics.rider.hungerstation",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = AccentGreen,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = { viewModel.refreshCatchLogs() }
            ) {
                Icon(
                    Icons.Rounded.Refresh,
                    contentDescription = stringResource(R.string.catch_log_refresh),
                    tint = AccentBlue
                )
            }

            IconButton(
                onClick = {
                    if (filteredLogs.isNotEmpty()) {
                        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
                        val textToCopy = filteredLogs.joinToString("\n") { item ->
                            val timeStr = fmt.format(Date(item.timestamp))
                            val coords = if (item.lat != null && item.lng != null) " [${item.lat}, ${item.lng}]" else ""
                            "[$timeStr] [${item.category}] (${item.packageName}) ${item.title} - ${item.detail}$coords (x${item.hitCount})"
                        }
                        clipboardManager.setText(AnnotatedString(textToCopy))
                        Toast.makeText(context, context.getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Icon(
                    Icons.Outlined.ContentCopy,
                    contentDescription = stringResource(R.string.copy),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            IconButton(
                onClick = { showClearConfirm = true }
            ) {
                Icon(
                    Icons.Rounded.DeleteSweep,
                    contentDescription = stringResource(R.string.catch_log_clear),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)

        // بطاقة حالة الحزمة المستهدفة ومفاتيح التحكم
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = AppColors.cardBackground(isDark)),
            border = BorderStroke(1.dp, AccentGreen.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (uiState.enableCatchLog) AccentGreen else MaterialTheme.colorScheme.outline)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.catch_log_exclusive_pkg),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = stringResource(R.string.catch_log_exclusive_desc),
                                fontSize = 11.sp,
                                color = AppColors.textSecondary(isDark)
                            )
                        }
                    }
                    Switch(
                        checked = uiState.enableCatchLog,
                        onCheckedChange = { viewModel.toggleEnableCatchLog() }
                    )
                }

                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(Modifier.height(8.dp))

                // إحصائيات سريعة
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatBadge(
                        label = stringResource(R.string.catch_cat_order),
                        count = orderCount,
                        color = AccentOrange
                    )
                    StatBadge(
                        label = stringResource(R.string.catch_cat_location),
                        count = locCount,
                        color = AccentBlue
                    )
                    StatBadge(
                        label = stringResource(R.string.catch_cat_security),
                        count = secCount,
                        color = Color(0xFFAB47BC)
                    )
                    StatBadge(
                        label = stringResource(R.string.catch_cat_env),
                        count = envCount,
                        color = AccentGreen
                    )
                }
            }
        }

        // شريط البحث والفلترة
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    stringResource(R.string.catch_log_search_hint),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            },
            leadingIcon = {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                    FilterChip(
                        selected = uiState.catchLogAutoRefresh,
                        onClick = { viewModel.toggleCatchLogAutoRefresh() },
                        label = {
                            Text(
                                stringResource(R.string.catch_log_live),
                                fontSize = 11.sp
                            )
                        },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (uiState.catchLogAutoRefresh) AccentGreen else MaterialTheme.colorScheme.outline)
                            )
                        }
                    )
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentBlue,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        )

        Spacer(Modifier.height(8.dp))

        // أزرار تصنيف السجل
        val categories = listOf(
            CatchLogCategory.ALL to stringResource(R.string.catch_cat_all),
            CatchLogCategory.ORDER to stringResource(R.string.catch_cat_order),
            CatchLogCategory.LOCATION to stringResource(R.string.catch_cat_location),
            CatchLogCategory.SECURITY to stringResource(R.string.catch_cat_security),
            CatchLogCategory.ENV to stringResource(R.string.catch_cat_env),
            CatchLogCategory.SYSTEM to stringResource(R.string.catch_cat_system)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { (cat, label) ->
                val isSelected = uiState.selectedCatchLogCategory == cat
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setCatchLogCategory(cat) },
                    label = { Text(label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentBlue.copy(alpha = 0.18f),
                        selectedLabelColor = AccentBlue
                    )
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // قائمة السجلات
        if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.Radar,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.catch_log_empty),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.catch_log_empty_hint),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(filteredLogs, key = { it.id }) { entry ->
                    CatchLogItemCard(
                        entry = entry,
                        isDark = isDark,
                        onCopy = {
                            val coordPart = if (entry.lat != null && entry.lng != null) " (${entry.lat}, ${entry.lng})" else ""
                            clipboardManager.setText(
                                AnnotatedString("[${entry.category}] ${entry.title}: ${entry.detail}$coordPart")
                            )
                            Toast.makeText(context, context.getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
                        },
                        onApplyCoordinates = { lat, lng ->
                            viewModel.updateLatitude(String.format(Locale.US, "%.6f", lat))
                            viewModel.updateLongitude(String.format(Locale.US, "%.6f", lng))
                            Toast.makeText(
                                context,
                                context.getString(R.string.catch_log_coords_applied),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(stringResource(R.string.catch_log_clear)) },
            text = { Text(stringResource(R.string.catch_log_clear_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearCatchLogs()
                        showClearConfirm = false
                    }
                ) {
                    Text(stringResource(R.string.clear_all), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun StatBadge(label: String, count: Int, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = color,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = count.toString(),
            fontSize = 12.sp,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CatchLogItemCard(
    entry: CatchLogEntry,
    isDark: Boolean,
    onCopy: () -> Unit,
    onApplyCoordinates: (Double, Double) -> Unit
) {
    val (catColor, catIcon, catLabel) = when (entry.category.uppercase()) {
        "ORDER" -> Triple(AccentOrange, Icons.Rounded.DeliveryDining, stringResource(R.string.catch_cat_order))
        "LOCATION" -> Triple(AccentBlue, Icons.Rounded.MyLocation, stringResource(R.string.catch_cat_location))
        "SECURITY" -> Triple(Color(0xFFAB47BC), Icons.Rounded.Shield, stringResource(R.string.catch_cat_security))
        "ENV" -> Triple(AccentGreen, Icons.Rounded.CellTower, stringResource(R.string.catch_cat_env))
        else -> Triple(Color(0xFF78909C), Icons.Rounded.Terminal, stringResource(R.string.catch_cat_system))
    }

    val timeStr = remember(entry.timestamp) {
        SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(entry.timestamp))
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.cardBackground(isDark)),
        border = BorderStroke(0.5.dp, catColor.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(catColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(catIcon, contentDescription = null, tint = catColor, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = catLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = catColor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(catColor.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
                Spacer(Modifier.width(8.dp))
                if (entry.hitCount > 1) {
                    Text(
                        text = "×${entry.hitCount}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentOrange,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(AccentOrange.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = timeStr,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = AppColors.textSecondary(isDark)
                )
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onCopy, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Outlined.ContentCopy,
                        contentDescription = stringResource(R.string.copy),
                        tint = AppColors.textSecondary(isDark),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = entry.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(3.dp))

            Text(
                text = entry.detail,
                fontSize = 12.sp,
                color = AppColors.textSecondary(isDark)
            )

            if (entry.lat != null && entry.lng != null && (entry.lat != 0.0 || entry.lng != 0.0)) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Place,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${String.format(Locale.US, "%.6f", entry.lat)}, ${String.format(Locale.US, "%.6f", entry.lng)}",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Text(
                        text = stringResource(R.string.catch_log_use_coords),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentBlue,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onApplyCoordinates(entry.lat, entry.lng) }
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
