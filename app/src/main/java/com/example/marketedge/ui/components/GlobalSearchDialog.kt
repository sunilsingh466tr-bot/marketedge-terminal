package com.example.marketedge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.marketedge.model.ScreenerStock
import com.example.ui.theme.*

@Composable
fun GlobalSearchDialog(
    query: String,
    results: List<ScreenerStock>,
    allStocks: List<ScreenerStock>,
    onQueryChange: (String) -> Unit,
    onStockSelect: (ScreenerStock) -> Unit,
    onDismiss: () -> Unit
) {
    val displayList = if (query.isBlank()) allStocks.take(8) else results

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, CyanRS.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
            color = TerminalSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SEARCH NIFTY 500 TERMINAL",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Input Field
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = CyanRS)
                    },
                    placeholder = { Text("Search ticker, company or sector...", fontSize = 12.sp, color = TextTertiary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("global_search_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanRS,
                        unfocusedBorderColor = TerminalBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (query.isBlank()) "Quick Access / Momentum Leaders:" else "Matches (${displayList.size}):",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (displayList.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No matching companies found.", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                    } else {
                        items(displayList, key = { it.symbol }) { stock ->
                            SearchStockRow(
                                stock = stock,
                                onClick = { onStockSelect(stock) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchStockRow(
    stock: ScreenerStock,
    onClick: () -> Unit
) {
    val isPositive = stock.changePercent >= 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(TerminalCard)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stock.symbol,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stock.sector,
                    fontSize = 9.sp,
                    color = CyanRS
                )
            }
            Text(
                text = stock.name,
                fontSize = 10.sp,
                color = TextSecondary,
                maxLines = 1
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            RsScoreBadge(rsScore = stock.rsScore)
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${String.format("%,.1f", stock.currentPrice)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary
                )
                Text(
                    text = "${if (isPositive) "+" else ""}${String.format("%.1f", stock.changePercent)}%",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (isPositive) BullishGreen else BearishRed
                )
            }
        }
    }
}
