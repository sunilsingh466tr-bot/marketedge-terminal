package com.example.marketedge.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.marketedge.data.local.ThesisNoteEntity
import com.example.marketedge.model.ScreenerStock
import com.example.marketedge.ui.components.CandlestickChart
import com.example.marketedge.ui.components.RangeBar52W
import com.example.marketedge.ui.components.RsScoreBadge
import com.example.marketedge.ui.components.StockQuantStatistics
import com.example.marketedge.ui.components.VolumeSurgeBadge
import com.example.ui.theme.*

@Composable
fun StockDetailScreen(
    stock: ScreenerStock,
    savedThesis: ThesisNoteEntity?,
    onSaveThesis: (thesis: String, catalysts: String, target: String, risks: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current

    var thesisInput by remember(savedThesis) { mutableStateOf(savedThesis?.thesisText ?: "") }
    var catalystsInput by remember(savedThesis) { mutableStateOf(savedThesis?.catalysts ?: "") }
    var targetInput by remember(savedThesis) { mutableStateOf(savedThesis?.targetValuation ?: "") }
    var risksInput by remember(savedThesis) { mutableStateOf(savedThesis?.riskFactors ?: "") }
    var isEditingThesis by remember { mutableStateOf(savedThesis == null) }

    fun openBrowser(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open link: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBackground),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Navigation Top Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(TerminalCard)
                        .testTag("detail_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = CyanRS,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "${stock.symbol} TERMINAL",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary
                    )
                    Text(
                        text = "${stock.name} • ${stock.sector}",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
            }
        }

        // Header Price & RS Metrics
        item {
            StockHeaderCard(stock = stock)
        }

        // 52-Week Range Bar
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(TerminalSurface)
                    .border(1.dp, TerminalBorder, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                RangeBar52W(
                    low = stock.low52W,
                    high = stock.high52W,
                    current = stock.currentPrice
                )
            }
        }

        // Key Moving Averages Grid
        item {
            MovingAveragesGrid(stock = stock)
        }

        // Technical Candlestick + Volume + Relative Strength Chart
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TECHNICAL PRICE & RS PROFILE",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "Drag finger to inspect",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                CandlestickChart(candles = stock.candles)
            }
        }

        // Pattern Scanner Tags
        item {
            PatternScannerSection(patterns = stock.patterns)
        }

        // Quantitative Risk & Statistical Profile (Standard Deviation, Correlation & Beta)
        item {
            StockQuantStatistics(
                stats = stock.quantStats,
                currentPrice = stock.currentPrice,
                symbol = stock.symbol
            )
        }

        // Concall & Corporate Filings Hub
        item {
            ConcallHubSection(
                dossier = stock.concall,
                symbol = stock.symbol,
                onOpenLink = { openBrowser(it) }
            )
        }

        // Private Custom Investment Thesis & Notes Hub
        item {
            InvestmentThesisSection(
                symbol = stock.symbol,
                savedThesis = savedThesis,
                thesisInput = thesisInput,
                catalystsInput = catalystsInput,
                targetInput = targetInput,
                risksInput = risksInput,
                isEditing = isEditingThesis,
                onThesisChange = { thesisInput = it },
                onCatalystsChange = { catalystsInput = it },
                onTargetChange = { targetInput = it },
                onRisksChange = { risksInput = it },
                onToggleEdit = { isEditingThesis = !isEditingThesis },
                onSave = {
                    onSaveThesis(thesisInput, catalystsInput, targetInput, risksInput)
                    isEditingThesis = false
                    Toast.makeText(context, "Thesis saved for ${stock.symbol}!", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
private fun StockHeaderCard(stock: ScreenerStock) {
    val isPositive = stock.changePercent >= 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TerminalSurface)
            .border(1.dp, TerminalBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = "₹${String.format("%,.2f", stock.currentPrice)}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${if (isPositive) "+" else ""}${String.format("%.2f", stock.changePercent)}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isPositive) BullishGreen else BearishRed
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Dist from 52W High: ${String.format("%.1f", stock.distanceFrom52WHighPercent)}%",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (stock.isNear52WHigh) BullishGreen else TextSecondary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                RsScoreBadge(rsScore = stock.rsScore)
                Spacer(modifier = Modifier.height(6.dp))
                VolumeSurgeBadge(multiplier = stock.volumeMultiplier)
            }
        }
    }
}

@Composable
private fun MovingAveragesGrid(stock: ScreenerStock) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val p = stock.currentPrice
        MaCard(
            name = "21 EMA",
            value = stock.ema21,
            isAbove = p >= stock.ema21,
            diffPercent = ((p - stock.ema21) / stock.ema21) * 100,
            color = CyanRS,
            modifier = Modifier.weight(1f)
        )
        MaCard(
            name = "50 SMA",
            value = stock.sma50,
            isAbove = p >= stock.sma50,
            diffPercent = ((p - stock.sma50) / stock.sma50) * 100,
            color = AmberGold,
            modifier = Modifier.weight(1f)
        )
        MaCard(
            name = "200 SMA",
            value = stock.sma200,
            isAbove = p >= stock.sma200,
            diffPercent = ((p - stock.sma200) / stock.sma200) * 100,
            color = PurpleMA,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MaCard(
    name: String,
    value: Double,
    isAbove: Boolean,
    diffPercent: Double,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(TerminalSurface)
            .border(1.dp, TerminalBorder, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Text(text = name, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "₹${String.format("%,.0f", value)}",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "${if (diffPercent >= 0) "+" else ""}${String.format("%.1f", diffPercent)}% (${if (isAbove) "Above" else "Below"})",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = if (isAbove) BullishGreen else BearishRed
        )
    }
}

@Composable
private fun PatternScannerSection(patterns: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(TerminalSurface)
            .border(1.dp, TerminalBorder, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "AUTO-DETECTED TECHNICAL PATTERNS",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            patterns.forEach { pattern ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(TerminalCard)
                        .border(1.dp, CyanRS.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = pattern,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanRS
                    )
                }
            }
        }
    }
}

@Composable
private fun ConcallHubSection(
    dossier: com.example.marketedge.model.ConcallDossier,
    symbol: String,
    onOpenLink: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TerminalSurface)
            .border(1.dp, TerminalBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CONCALL & CORPORATE DOSSIER",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp,
                    color = TextPrimary
                )
                Text(
                    text = "Official Recordings, PPTs, Transcripts & Filings",
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = CyanRS,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quarter & Concall Date Badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(CyanRS.copy(alpha = 0.10f))
                .border(1.dp, CyanRS.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = dossier.latestQuarter,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanRS
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "•",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = dossier.concallDate,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
            }
            Text(
                text = "CORPORATE FILING",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = CyanRS
            )
        }

        if (dossier.keyTakeaways.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "MANAGEMENT HIGHLIGHTS & KEY TAKEAWAYS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                color = AmberGold
            )
            Spacer(modifier = Modifier.height(6.dp))
            dossier.keyTakeaways.forEach { takeaway ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "▸",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanRS,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = takeaway,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = TextPrimary
                    )
                }
            }
        }

        if (dossier.managementGuidance.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(TerminalCard)
                    .border(1.dp, TerminalBorder, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = BullishGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "MANAGEMENT GUIDANCE (FY26/FY27)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = BullishGreen
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = dossier.managementGuidance,
                    fontSize = 10.sp,
                    lineHeight = 15.sp,
                    color = TextPrimary
                )
            }
        }

        if (dossier.capexPlans.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(TerminalCard)
                    .border(1.dp, TerminalBorder, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PrecisionManufacturing,
                        contentDescription = null,
                        tint = AmberGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CAPEX & CAPACITY PIPELINE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberGold
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = dossier.capexPlans,
                    fontSize = 10.sp,
                    lineHeight = 15.sp,
                    color = TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Dossier Action Links
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DossierLinkButton(
                title = "Concall Audio",
                subtitle = "MP3 / Recording",
                icon = Icons.Default.Mic,
                onClick = { onOpenLink(dossier.audioUrl) },
                modifier = Modifier.weight(1f)
            )
            DossierLinkButton(
                title = "Transcript PDF",
                subtitle = "Official Minutes",
                icon = Icons.Default.Description,
                onClick = { onOpenLink(dossier.transcriptPdfUrl) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DossierLinkButton(
                title = "Investor PPT",
                subtitle = "Slide Deck",
                icon = Icons.Default.Slideshow,
                onClick = { onOpenLink(dossier.investorPptUrl) },
                modifier = Modifier.weight(1f)
            )
            DossierLinkButton(
                title = "Annual Report",
                subtitle = "NSE / BSE Filing",
                icon = Icons.Default.AccountBalance,
                onClick = { onOpenLink(dossier.annualReportUrl) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Direct 1-Click to Screener.in / NSE
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(TerminalCard)
                .clickable { onOpenLink(dossier.screenerInUrl) }
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Language, contentDescription = null, tint = CyanRS, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Open on Screener.in ($symbol)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
            Icon(Icons.Default.OpenInNew, contentDescription = null, tint = CyanRS, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun DossierLinkButton(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(TerminalCard)
            .border(1.dp, TerminalBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CyanRS,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(text = subtitle, fontSize = 9.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun InvestmentThesisSection(
    symbol: String,
    savedThesis: ThesisNoteEntity?,
    thesisInput: String,
    catalystsInput: String,
    targetInput: String,
    risksInput: String,
    isEditing: Boolean,
    onThesisChange: (String) -> Unit,
    onCatalystsChange: (String) -> Unit,
    onTargetChange: (String) -> Unit,
    onRisksChange: (String) -> Unit,
    onToggleEdit: () -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TerminalSurface)
            .border(1.dp, TerminalBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "MY INVESTMENT THESIS (PRIVATE)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp,
                    color = AmberGold
                )
                Text(
                    text = "Custom Research Notes & Catalysts (Persisted Locally)",
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }

            if (!isEditing && savedThesis != null) {
                IconButton(
                    onClick = onToggleEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Thesis", tint = CyanRS, modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (isEditing) {
            OutlinedTextField(
                value = thesisInput,
                onValueChange = onThesisChange,
                label = { Text("Core Investment Thesis", fontSize = 11.sp) },
                placeholder = { Text("e.g. Market leader in high-growth EMS, margin expansion from smartphone localization...", fontSize = 11.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("thesis_input_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanRS,
                    unfocusedBorderColor = TerminalBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = catalystsInput,
                onValueChange = onCatalystsChange,
                label = { Text("Upcoming Catalysts / Order Triggers", fontSize = 11.sp) },
                placeholder = { Text("e.g. PLI scheme incentives disbursement in Q3, new client ramp-up...", fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanRS,
                    unfocusedBorderColor = TerminalBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = targetInput,
                    onValueChange = onTargetChange,
                    label = { Text("Target / Valuation", fontSize = 11.sp) },
                    placeholder = { Text("e.g. ₹16,500 (35x FY28)", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanRS,
                        unfocusedBorderColor = TerminalBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
                OutlinedTextField(
                    value = risksInput,
                    onValueChange = onRisksChange,
                    label = { Text("Key Risks", fontSize = 11.sp) },
                    placeholder = { Text("e.g. Component supply crunch", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanRS,
                        unfocusedBorderColor = TerminalBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(containerColor = BullishGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_thesis_button")
            ) {
                Icon(Icons.Default.Save, contentDescription = null, tint = TerminalBackground, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Thesis & Notes", color = TerminalBackground, fontWeight = FontWeight.Bold)
            }
        } else {
            // View Saved Thesis
            if (savedThesis != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(TerminalCard)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "Core Thesis:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberGold
                    )
                    Text(
                        text = savedThesis.thesisText.ifBlank { "No thesis entered." },
                        fontSize = 12.sp,
                        color = TextPrimary,
                        lineHeight = 18.sp
                    )

                    if (savedThesis.catalysts.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Catalysts:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyanRS)
                        Text(text = savedThesis.catalysts, fontSize = 11.sp, color = TextSecondary)
                    }

                    if (savedThesis.targetValuation.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Target / Valuation:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BullishGreen)
                        Text(text = savedThesis.targetValuation, fontSize = 11.sp, color = TextSecondary)
                    }

                    if (savedThesis.riskFactors.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Risks:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BearishRed)
                        Text(text = savedThesis.riskFactors, fontSize = 11.sp, color = TextSecondary)
                    }
                }
            } else {
                Text(
                    text = "No saved research note for this stock yet. Click Edit to record your thesis.",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
    }
}
