package com.example.marketedge.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.marketedge.model.IndexCategory
import com.example.marketedge.model.IndexSummary
import com.example.marketedge.ui.components.SparklineCanvas
import com.example.ui.theme.*

@Composable
fun IndexMatrixScreen(
    indices: List<IndexSummary>,
    selectedCategory: IndexCategory?,
    onCategorySelect: (IndexCategory?) -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredIndices = remember(indices, selectedCategory) {
        if (selectedCategory == null) indices
        else indices.filter { it.category == selectedCategory }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBackground),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text(
                    text = "INDEX MATRIX & MOMENTUM",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = TextPrimary
                )
                Text(
                    text = "52-Week Trajectories & Key Moving Average Distances",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Category Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryFilterChip(
                        label = "All (${indices.size})",
                        isSelected = selectedCategory == null,
                        onClick = { onCategorySelect(null) },
                        testTag = "index_cat_all"
                    )
                    CategoryFilterChip(
                        label = "Broad Market",
                        isSelected = selectedCategory == IndexCategory.BROAD,
                        onClick = { onCategorySelect(IndexCategory.BROAD) },
                        testTag = "index_cat_broad"
                    )
                    CategoryFilterChip(
                        label = "Sectoral Indices",
                        isSelected = selectedCategory == IndexCategory.SECTORAL,
                        onClick = { onCategorySelect(IndexCategory.SECTORAL) },
                        testTag = "index_cat_sectoral"
                    )
                }
            }
        }

        items(filteredIndices, key = { it.id }) { indexItem ->
            IndexCard(indexItem = indexItem)
        }
    }
}

@Composable
private fun IndexCard(
    indexItem: IndexSummary,
    modifier: Modifier = Modifier
) {
    val isPositive = indexItem.changePercent >= 0
    val trendColor = if (isPositive) BullishGreen else BearishRed

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TerminalSurface)
            .border(1.dp, TerminalBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = indexItem.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
                Text(
                    text = indexItem.category.label,
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = String.format("%,.2f", indexItem.currentPrice),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${if (isPositive) "+" else ""}${String.format("%.2f", indexItem.change)} (${if (isPositive) "+" else ""}${String.format("%.2f", indexItem.changePercent)}%)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = trendColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 52-Week Sparkline Chart
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(TerminalCard)
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "52W Low: ${String.format("%,.0f", indexItem.low52W)}",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextTertiary
                )
                Text(
                    text = "52W Sparkline",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanRS
                )
                Text(
                    text = "52W High: ${String.format("%,.0f", indexItem.high52W)}",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextTertiary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            SparklineCanvas(
                points = indexItem.sparklinePoints,
                isPositive = isPositive,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(45.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Moving Average Distances Row (50 EMA & 200 EMA)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EmaDistancePill(
                label = "Dist. from 50 EMA",
                distance = indexItem.distanceFrom50EmaPercent,
                modifier = Modifier.weight(1f)
            )
            EmaDistancePill(
                label = "Dist. from 200 EMA",
                distance = indexItem.distanceFrom200EmaPercent,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun EmaDistancePill(
    label: String,
    distance: Double,
    modifier: Modifier = Modifier
) {
    val isAbove = distance >= 0
    val textColor = if (isAbove) BullishGreen else BearishRed

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(TerminalCard)
            .border(1.dp, TerminalBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            color = TextSecondary
        )
        Text(
            text = "${if (isAbove) "+" else ""}${String.format("%.1f", distance)}% (${if (isAbove) "Above" else "Below"})",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = textColor
        )
    }
}

@Composable
private fun CategoryFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) CyanRSDark else TerminalCard)
            .border(
                1.dp,
                if (isSelected) CyanRS else TerminalBorder,
                RoundedCornerShape(20.dp)
            )
            .testTag(testTag)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) CyanRS else TextSecondary
        )
    }
}
