package com.example.marketedge.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NoteAlt
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
import com.example.marketedge.data.local.ThesisNoteEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SavedThesesScreen(
    theses: List<ThesisNoteEntity>,
    onSelectStock: (String) -> Unit,
    onDeleteThesis: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

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
                    text = "MY RESEARCH THESES & DOSSIERS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = AmberGold
                )
                Text(
                    text = "Personal conviction notes, upcoming catalysts, and price targets",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${theses.size} Active Stock Research Notes",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = CyanRS
                )
            }
        }

        if (theses.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.NoteAlt,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No saved stock research notes yet.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Open any stock from the RS Screener or Search to write your conviction thesis.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            items(theses, key = { it.symbol }) { thesis ->
                ThesisCard(
                    thesis = thesis,
                    formattedDate = dateFormat.format(Date(thesis.updatedAt)),
                    onClick = { onSelectStock(thesis.symbol) },
                    onDelete = { onDeleteThesis(thesis.symbol) }
                )
            }
        }
    }
}

@Composable
private fun ThesisCard(
    thesis: ThesisNoteEntity,
    formattedDate: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TerminalSurface)
            .border(1.dp, TerminalBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
            .testTag("saved_thesis_${thesis.symbol}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = thesis.symbol,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary
                )
                Text(
                    text = thesis.companyName,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Text(
                    text = "Saved: $formattedDate",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextTertiary
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = BearishRed.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(TerminalCard)
                .padding(8.dp)
        ) {
            Text(
                text = thesis.thesisText.ifBlank { "No detailed text." },
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = TextPrimary,
                maxLines = 3
            )
        }

        if (thesis.catalysts.isNotBlank() || thesis.targetValuation.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (thesis.catalysts.isNotBlank()) {
                    Text(
                        text = "Trigger: ${thesis.catalysts}",
                        fontSize = 10.sp,
                        color = CyanRS,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (thesis.targetValuation.isNotBlank()) {
                    Text(
                        text = "Target: ${thesis.targetValuation}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = BullishGreen
                    )
                }
            }
        }
    }
}
