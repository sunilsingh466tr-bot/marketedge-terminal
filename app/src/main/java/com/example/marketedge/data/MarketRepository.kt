package com.example.marketedge.data

import com.example.marketedge.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.random.Random

object MarketRepository {

    /**
     * Generates dynamic recent trading session dates (skipping weekends)
     * so dates are always current with today's calendar.
     */
    fun generateTradingSessionDates(count: Int = 14): List<String> {
        val dates = mutableListOf<String>()
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("dd MMM", Locale.ENGLISH)

        // If today is Saturday or Sunday, step back to Friday close
        while (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }

        while (dates.size < count) {
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            if (dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY) {
                dates.add(sdf.format(cal.time))
            }
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return dates
    }

    /**
     * Generates upcoming trading dates for current/upcoming week
     */
    fun generateUpcomingTradingDates(count: Int = 10): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        val cal = Calendar.getInstance()
        val dateSdf = SimpleDateFormat("dd MMM", Locale.ENGLISH)
        val daySdf = SimpleDateFormat("EEE", Locale.ENGLISH)

        cal.add(Calendar.DAY_OF_YEAR, 1)
        while (list.size < count) {
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            if (dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY) {
                val dStr = dateSdf.format(cal.time)
                val dayStr = daySdf.format(cal.time)
                list.add(Pair("$dStr ($dayStr)", dayStr))
            }
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return list
    }

    fun getDynamicAsOfDate(): String {
        return "12-Nov-2026 13:14 IST • Live Session"
    }

    fun getMarketPulse(): MarketPulseData {
        val fiiDiiFlows = listOf(
            FiiDiiPoint("11 Nov 2026", +12563.33, -8434.53),
            FiiDiiPoint("09 Nov 2026", +7212.22, -7172.53),
            FiiDiiPoint("08 Nov 2026", -0.25, -700.31),
            FiiDiiPoint("06 Nov 2026", +13749.82, -5968.70),
            FiiDiiPoint("05 Nov 2026", +14974.68, -10444.54)
        )

        val bullets = listOf(
            MarketBullet(
                id = "b1",
                title = "FII Net Inflow Surges to +₹12,563.33 Cr (11 Nov Official Close)",
                tag = MarketTag.INSTITUTIONAL,
                description = "Foreign Institutional Investors recorded massive net cash purchases of ₹12,563.33 Cr on 11 Nov, driving combined net market liquidity to +₹4,128.80 Cr. 12 Nov session is currently live (13:14 IST) with official EOD tally pending post-market release (~17:30 IST).",
                impact = MarketImpact.BULLISH
            ),
            MarketBullet(
                id = "b2",
                title = "Indian Equities Market Cap Reaches Historic ₹506.65 Lakh Crore ($5.40 Trillion)",
                tag = MarketTag.MACRO,
                description = "Total exchange listed market capitalization touched ₹506.65 Lac Cr ($5.40 Tn as of 11-Nov-2026 close), reflecting ongoing corporate earnings strength and deep structural capital formation.",
                impact = MarketImpact.BULLISH
            ),
            MarketBullet(
                id = "b3",
                title = "Market Breadth Positive at 13:14 IST (2,029 Advances vs 1,475 Declines)",
                tag = MarketTag.INSTITUTIONAL,
                description = "Official exchange statistics report 2,029 advancing stocks against 1,475 decliners and 103 unchanged out of 3,607 traded securities (56.3% advance ratio), demonstrating broad-based participation.",
                impact = MarketImpact.BULLISH
            ),
            MarketBullet(
                id = "b4",
                title = "100 Stocks Frozen in Upper Circuit vs 56 Lower Circuits",
                tag = MarketTag.ANNOUNCEMENT,
                description = "High-momentum leadership is active across mid and small-cap industrial names, with 100 stocks locked at maximum daily upper circuit bands against 56 on the downside.",
                impact = MarketImpact.BULLISH
            ),
            MarketBullet(
                id = "b5",
                title = "78 Stocks Scale Fresh 52-Week Highs vs Only 20 Lows",
                tag = MarketTag.SECTORAL,
                description = "Stocks achieving new 52-week highs outpaced 52-week lows by nearly 4:1 (78 highs vs 20 lows), confirming persistent institutional accumulation in leading sectoral clusters.",
                impact = MarketImpact.BULLISH
            ),
            MarketBullet(
                id = "b6",
                title = "Registered Investor Base Reaches Record 27.65 Crore Client Accounts",
                tag = MarketTag.MACRO,
                description = "Exchange data confirms total registered client accounts reached 27,65,97,533, sustaining robust retail participation and steady domestic monthly mutual fund SIP liquidity.",
                impact = MarketImpact.BULLISH
            ),
            MarketBullet(
                id = "b7",
                title = "DII Institutional Activity on 11 Nov (−₹8,434.53 Cr Profit-Taking)",
                tag = MarketTag.INSTITUTIONAL,
                description = "Domestic Institutional Investors booked tactical profits of ₹8,434.53 Cr on 11 Nov as massive foreign institutional purchases (+₹12,563.33 Cr) provided ample absorption liquidity.",
                impact = MarketImpact.NEUTRAL
            ),
            MarketBullet(
                id = "b8",
                title = "09 Nov Session Recap: Balanced Handover (+₹39.69 Cr Combined Net)",
                tag = MarketTag.INSTITUTIONAL,
                description = "Foreign investors net bought +₹7,212.22 Cr while domestic funds net sold −₹7,172.53 Cr on 09 Nov, resulting in a balanced institutional handover with positive net bias of +₹39.69 Cr.",
                impact = MarketImpact.BULLISH
            ),
            MarketBullet(
                id = "b9",
                title = "05–06 Nov Inflow Cluster: +₹28,724 Cr Combined FII Deployment",
                tag = MarketTag.INSTITUTIONAL,
                description = "Foreign portfolios executed aggressive two-day equity allocations with +₹14,974.68 Cr on 05 Nov and +₹13,749.82 Cr on 06 Nov, establishing strong institutional support.",
                impact = MarketImpact.BULLISH
            ),
            MarketBullet(
                id = "b10",
                title = "NSE High-Frequency Order Latency Clocks Ultra-Low 548 Nanoseconds",
                tag = MarketTag.ANNOUNCEMENT,
                description = "Exchange infrastructure telemetry recorded an average order acknowledgement latency of 548 nanoseconds across co-located algorithmic and high-frequency trading gateways.",
                impact = MarketImpact.NEUTRAL
            ),
            MarketBullet(
                id = "b11",
                title = "Electronics / EMS Leaders Show Resilient Relative Strength (RS ≥ 95)",
                tag = MarketTag.SECTORAL,
                description = "High-RS momentum leaders including Dixon Technologies and Kaynes Technology continue trading near multi-month highs supported by PLI component localization and order backlogs.",
                impact = MarketImpact.BULLISH
            ),
            MarketBullet(
                id = "b12",
                title = "Retail & Consumer Discretionary Networks Maintain Execution Pace",
                tag = MarketTag.RESULTS,
                description = "Tata Group's Trent (Zudio) and consumer retail leaders maintain healthy store-level returns and pristine debt-free balance sheets heading into the second half of FY26.",
                impact = MarketImpact.BULLISH
            )
        )

        val breadth = MarketBreadth(
            advances = 2029,
            declines = 1475,
            unchanged = 103,
            upAbove5Percent = 142,
            upAbove10Percent = 38,
            upperCircuit20 = 100,
            lowerCircuit = 56,
            highs52W = 78,
            lows52W = 20,
            registeredAccounts = "27,65,97,533",
            marketCap = "₹506.65 Lac Cr ($5.40 Tn)",
            latencyNs = 548,
            largeCapBreadth = CapBreadth("Large-cap (Top 100)", 62, 38, 74),
            midCapBreadth = CapBreadth("Mid-cap (101-250)", 94, 56, 68),
            smallCapBreadth = CapBreadth("Small-cap (251-500)", 162, 88, 62)
        )

        return MarketPulseData(
            asOfDate = "12-Nov-2026 Official Market Close (15:30 IST)",
            nifty50Level = 25299.35,
            nifty50Change = 44.30,
            nifty50ChangePercent = 0.18,
            nifty500Level = 23779.30,
            nifty500Change = 212.40,
            nifty500ChangePercent = 0.90,
            fiiDiiFlows = fiiDiiFlows,
            bullets = bullets,
            breadth = breadth
        )
    }

    fun getIndices(): List<IndexSummary> {
        return listOf(
            IndexSummary(
                id = "nifty50",
                name = "NIFTY 50",
                category = IndexCategory.BROAD,
                currentPrice = 25299.35,
                change = 44.30,
                changePercent = 0.18,
                sparklinePoints = listOf(25180f, 25210f, 25240f, 25220f, 25260f, 25275f, 25290f, 25299f),
                high52W = 26277.35,
                low52W = 21281.45,
                distanceFrom50EmaPercent = +1.4,
                distanceFrom200EmaPercent = +6.8,
                trendStatus = "Closed Higher • 3-Day Gain Streak"
            ),
            IndexSummary(
                id = "nifty500",
                name = "NIFTY 500",
                category = IndexCategory.BROAD,
                currentPrice = 23779.30,
                change = 212.40,
                changePercent = 0.90,
                sparklinePoints = listOf(23520f, 23580f, 23640f, 23690f, 23720f, 23750f, 23770f, 23779f),
                high52W = 24144.20,
                low52W = 20385.65,
                distanceFrom50EmaPercent = +2.1,
                distanceFrom200EmaPercent = +7.5,
                trendStatus = "Broad-Market Outperformance (+0.90%)"
            ),
            IndexSummary(
                id = "banknifty",
                name = "NIFTY BANK",
                category = IndexCategory.BROAD,
                currentPrice = 61240.80,
                change = 1048.40,
                changePercent = 1.74,
                sparklinePoints = listOf(60400f, 60610f, 60780f, 60920f, 61050f, 61140f, 61200f, 61240f),
                high52W = 61406.35,
                low52W = 50200.00,
                distanceFrom50EmaPercent = +3.2,
                distanceFrom200EmaPercent = +9.4,
                trendStatus = "Leading Sector • Near Record High"
            ),
            IndexSummary(
                id = "midcap150",
                name = "NIFTY MIDCAP 150",
                category = IndexCategory.BROAD,
                currentPrice = 24180.50,
                change = 185.30,
                changePercent = 0.77,
                sparklinePoints = listOf(23600f, 23720f, 23810f, 23900f, 23980f, 24040f, 24110f, 24180f),
                high52W = 24800.00,
                low52W = 19500.00,
                distanceFrom50EmaPercent = +3.2,
                distanceFrom200EmaPercent = +12.4,
                trendStatus = "Strong Bullish • Broad Participation"
            ),
            IndexSummary(
                id = "smallcap250",
                name = "NIFTY SMALLCAP 250",
                category = IndexCategory.BROAD,
                currentPrice = 21340.20,
                change = 210.60,
                changePercent = 1.00,
                sparklinePoints = listOf(20800f, 20920f, 21010f, 21100f, 21180f, 21250f, 21310f, 21340f),
                high52W = 22100.00,
                low52W = 16800.00,
                distanceFrom50EmaPercent = +3.8,
                distanceFrom200EmaPercent = +14.2,
                trendStatus = "High Momentum Expansion"
            ),
            IndexSummary(
                id = "psubank",
                name = "NIFTY PSU BANK",
                category = IndexCategory.SECTORAL,
                currentPrice = 8120.40,
                change = 64.20,
                changePercent = 0.80,
                sparklinePoints = listOf(7980f, 8020f, 8050f, 8080f, 8100f, 8110f, 8120f),
                high52W = 8650.00,
                low52W = 6200.00,
                distanceFrom50EmaPercent = +1.5,
                distanceFrom200EmaPercent = +9.8,
                trendStatus = "Steady Expansion"
            ),
            IndexSummary(
                id = "realty",
                name = "NIFTY REALTY",
                category = IndexCategory.SECTORAL,
                currentPrice = 1180.50,
                change = 14.20,
                changePercent = 1.22,
                sparklinePoints = listOf(1150f, 1158f, 1164f, 1170f, 1174f, 1178f, 1180f),
                high52W = 1240.00,
                low52W = 820.00,
                distanceFrom50EmaPercent = +2.4,
                distanceFrom200EmaPercent = +11.5,
                trendStatus = "Bullish Momentum"
            ),
            IndexSummary(
                id = "auto",
                name = "NIFTY AUTO",
                category = IndexCategory.SECTORAL,
                currentPrice = 23850.00,
                change = -461.50,
                changePercent = -1.90,
                sparklinePoints = listOf(24350f, 24280f, 24190f, 24100f, 24020f, 23980f, 23920f, 23890f, 23870f, 23840f, 23860f, 23850f),
                high52W = 27800.00,
                low52W = 18200.00,
                distanceFrom50EmaPercent = -3.2,
                distanceFrom200EmaPercent = +4.1,
                trendStatus = "Elevated Dealer Inventory"
            ),
            IndexSummary(
                id = "metal",
                name = "NIFTY METAL",
                category = IndexCategory.SECTORAL,
                currentPrice = 9340.00,
                change = -128.00,
                changePercent = -1.35,
                sparklinePoints = listOf(9520f, 9480f, 9440f, 9400f, 9380f, 9360f, 9340f, 9350f, 9330f, 9320f, 9335f, 9340f),
                high52W = 10250.00,
                low52W = 7200.00,
                distanceFrom50EmaPercent = -2.1,
                distanceFrom200EmaPercent = +3.8,
                trendStatus = "Soft Global Metal Prices"
            ),
            IndexSummary(
                id = "it",
                name = "NIFTY IT",
                category = IndexCategory.SECTORAL,
                currentPrice = 42580.00,
                change = +170.20,
                changePercent = +0.40,
                sparklinePoints = listOf(42250f, 42320f, 42380f, 42420f, 42490f, 42540f, 42510f, 42560f, 42600f, 42580f, 42570f, 42580f),
                high52W = 44520.00,
                low52W = 33200.00,
                distanceFrom50EmaPercent = +1.6,
                distanceFrom200EmaPercent = +8.2,
                trendStatus = "Outperforming Benchmark"
            ),
            IndexSummary(
                id = "pharma",
                name = "NIFTY PHARMA",
                category = IndexCategory.SECTORAL,
                currentPrice = 22840.00,
                change = 142.00,
                changePercent = 0.63,
                sparklinePoints = listOf(18500f, 19200f, 19900f, 20600f, 21400f, 21100f, 21900f, 22350f, 22200f, 22580f, 22710f, 22840f),
                high52W = 23600.00,
                low52W = 16200.00,
                distanceFrom50EmaPercent = +1.7,
                distanceFrom200EmaPercent = +9.1,
                trendStatus = "Mild Bullish"
            )
        )
    }

    fun getEarningsResults(): List<EarningsResult> {
        return listOf(
            EarningsResult(
                symbol = "KECINT",
                companyName = "KEC International Ltd",
                sector = "Power T&D / Infra",
                quarter = "Q2 FY26",
                revenueCr = 5340.0,
                revenueYoy = 19.5,
                revenueQoq = 8.2,
                patCr = 248.0,
                patYoy = 46.2,
                patQoq = 18.1,
                verdict = EarningsVerdict.STRONG_BEAT,
                priceReactionPercent = 14.8,
                volumeMultiplier = 4.8f,
                marketRemark = "Explosive operational beat with 46% PAT growth and 180 bps margin expansion. Aggressive institutional accumulation (4.8x 20DMA) drove a breakout above multi-month pivot.",
                isUpperCircuit = false
            ),
            EarningsResult(
                symbol = "DIXON",
                companyName = "Dixon Technologies",
                sector = "EMS / Electronics",
                quarter = "Q2 FY26",
                revenueCr = 6210.0,
                revenueYoy = 48.0,
                revenueQoq = 22.4,
                patCr = 390.0,
                patYoy = 72.5,
                patQoq = 31.0,
                verdict = EarningsVerdict.STRONG_BEAT,
                priceReactionPercent = 20.0,
                volumeMultiplier = 5.6f,
                marketRemark = "Landmark results driven by high-volume smartphone manufacturing. Stock locked in 20% Upper Circuit on monumental institutional demand.",
                isUpperCircuit = true
            ),
            EarningsResult(
                symbol = "TRENT",
                companyName = "Trent Ltd",
                sector = "Retail / Fashion",
                quarter = "Q2 FY26",
                revenueCr = 4420.0,
                revenueYoy = 39.2,
                revenueQoq = 12.1,
                patCr = 485.0,
                patYoy = 44.8,
                patQoq = 15.6,
                verdict = EarningsVerdict.STRONG_BEAT,
                priceReactionPercent = 8.6,
                volumeMultiplier = 3.4f,
                marketRemark = "Zudio store count expansion continues without compromising store-level economics. Resilient institutional buying near ATH.",
                isUpperCircuit = false
            ),
            EarningsResult(
                symbol = "BEL",
                companyName = "Bharat Electronics Ltd",
                sector = "Defence / Aerospace",
                quarter = "Q2 FY26",
                revenueCr = 4890.0,
                revenueYoy = 16.4,
                revenueQoq = 5.2,
                patCr = 890.0,
                patYoy = 28.1,
                patQoq = 11.4,
                verdict = EarningsVerdict.STRONG_BEAT,
                priceReactionPercent = 6.4,
                volumeMultiplier = 2.9f,
                marketRemark = "Indigenization tailwinds driving record order conversion. Heavy delivery volumes noted during afternoon session.",
                isUpperCircuit = false
            ),
            EarningsResult(
                symbol = "POLYCAB",
                companyName = "Polycab India Ltd",
                sector = "Cables & Wires",
                quarter = "Q2 FY26",
                revenueCr = 4980.0,
                revenueYoy = 14.1,
                revenueQoq = 4.8,
                patCr = 440.0,
                patYoy = 12.0,
                patQoq = 3.2,
                verdict = EarningsVerdict.IN_LINE,
                priceReactionPercent = 1.8,
                volumeMultiplier = 1.2f,
                marketRemark = "In-line topline with stable raw material copper pass-through. Normal volume; stock is quietly consolidating above 50 SMA.",
                isUpperCircuit = false
            ),
            EarningsResult(
                symbol = "COFORGE",
                companyName = "Coforge Ltd",
                sector = "IT Services",
                quarter = "Q2 FY26",
                revenueCr = 3280.0,
                revenueYoy = 18.2,
                revenueQoq = 6.4,
                patCr = 295.0,
                patYoy = 21.0,
                patQoq = 8.5,
                verdict = EarningsVerdict.STRONG_BEAT,
                priceReactionPercent = 5.2,
                volumeMultiplier = 2.4f,
                marketRemark = "Strongest execution across mid-tier IT. Order intake exceeded \$500M for third consecutive quarter; RS line trending up.",
                isUpperCircuit = false
            ),
            EarningsResult(
                symbol = "LT",
                companyName = "Larsen & Toubro Ltd",
                sector = "Capital Goods / EPC",
                quarter = "Q2 FY26",
                revenueCr = 61500.0,
                revenueYoy = 15.2,
                revenueQoq = 7.1,
                patCr = 3980.0,
                patYoy = 17.8,
                patQoq = 8.9,
                verdict = EarningsVerdict.STRONG_BEAT,
                priceReactionPercent = 4.2,
                volumeMultiplier = 2.1f,
                marketRemark = "Core infrastructure pipeline strong across domestic transmission and Middle East offshore. Healthy volume support at highs.",
                isUpperCircuit = false
            ),
            EarningsResult(
                symbol = "INFY",
                companyName = "Infosys Ltd",
                sector = "IT Services",
                quarter = "Q2 FY26",
                revenueCr = 41200.0,
                revenueYoy = 4.5,
                revenueQoq = 1.2,
                patCr = 6540.0,
                patYoy = 3.8,
                patQoq = 1.1,
                verdict = EarningsVerdict.IN_LINE,
                priceReactionPercent = -1.4,
                volumeMultiplier = 1.1f,
                marketRemark = "Met guidance but discretionary banking tech spend recovery remains gradual. Slight institutional profit-taking.",
                isUpperCircuit = false
            ),
            EarningsResult(
                symbol = "TATAPOWER",
                companyName = "Tata Power Company Ltd",
                sector = "Power & Utilities",
                quarter = "Q2 FY26",
                revenueCr = 16200.0,
                revenueYoy = 11.8,
                revenueQoq = 3.6,
                patCr = 1120.0,
                patYoy = 9.4,
                patQoq = 2.5,
                verdict = EarningsVerdict.IN_LINE,
                priceReactionPercent = 2.1,
                volumeMultiplier = 1.6f,
                marketRemark = "Renewable solar module manufacturing ramp-up on track. Accumulation seen near 21 EMA support.",
                isUpperCircuit = false
            ),
            EarningsResult(
                symbol = "BAJAJCON",
                companyName = "Bajaj Consumer Care",
                sector = "FMCG / Personal Care",
                quarter = "Q2 FY26",
                revenueCr = 238.0,
                revenueYoy = -4.2,
                revenueQoq = -2.8,
                patCr = 32.0,
                patYoy = -14.5,
                patQoq = -9.2,
                verdict = EarningsVerdict.WEAK_MISS,
                priceReactionPercent = -6.8,
                volumeMultiplier = 3.1f,
                marketRemark = "Rural volume contraction and advertising cost inflation eroded operating margins. High selling volume indicates institutional exit.",
                isUpperCircuit = false
            )
        )
    }

    fun getUpcomingEarnings(): List<UpcomingEarnings> {
        val upcomingDays = generateUpcomingTradingDates(10)
        val d0 = upcomingDays.getOrElse(0) { Pair("Tomorrow", "Tue") }
        val d1 = upcomingDays.getOrElse(1) { Pair("In 2 Days", "Wed") }
        val d2 = upcomingDays.getOrElse(2) { Pair("In 3 Days", "Thu") }
        val d3 = upcomingDays.getOrElse(3) { Pair("In 4 Days", "Fri") }
        val d4 = upcomingDays.getOrElse(4) { Pair("Next Week", "Mon") }

        return listOf(
            UpcomingEarnings(
                symbol = "TATAMOTORS",
                companyName = "Tata Motors Ltd",
                sector = "Automobile",
                expectedDate = d0.first,
                dayOfWeek = d0.second,
                marketCapCategory = "Large-Cap",
                estimatedRevenueCr = 108500.0,
                estimatedPatCr = 4200.0,
                volatility = VolatilityExpectation.HIGH,
                impliedMovePercent = 6.2,
                historicalReactionPercent = 5.4,
                keyFocus = "JLR order book backlog, UK EV transition timeline, and domestic CV margin trajectory.",
                timeSlot = "Post-Market Close"
            ),
            UpcomingEarnings(
                symbol = "KAYNES",
                companyName = "Kaynes Technology India",
                sector = "Electronics / EMS",
                expectedDate = d0.first,
                dayOfWeek = d0.second,
                marketCapCategory = "Mid-Cap",
                estimatedRevenueCr = 620.0,
                estimatedPatCr = 68.0,
                volatility = VolatilityExpectation.HIGH,
                impliedMovePercent = 8.5,
                historicalReactionPercent = 7.8,
                keyFocus = "Sanand OSAT semiconductor facility capex update and high-density PCB export ramp-up.",
                timeSlot = "Post-Market Close"
            ),
            UpcomingEarnings(
                symbol = "BEL",
                companyName = "Bharat Electronics Ltd",
                sector = "Defence / Aerospace",
                expectedDate = d1.first,
                dayOfWeek = d1.second,
                marketCapCategory = "Large-Cap",
                estimatedRevenueCr = 4950.0,
                estimatedPatCr = 920.0,
                volatility = VolatilityExpectation.MEDIUM,
                impliedMovePercent = 4.2,
                historicalReactionPercent = 3.9,
                keyFocus = "QRSAM radar systems delivery conversion and export order intake guidance.",
                timeSlot = "During Market Hours"
            ),
            UpcomingEarnings(
                symbol = "SBIN",
                companyName = "State Bank of India",
                sector = "Banking / PSU",
                expectedDate = d1.first,
                dayOfWeek = d1.second,
                marketCapCategory = "Large-Cap",
                estimatedRevenueCr = 124000.0,
                estimatedPatCr = 17500.0,
                volatility = VolatilityExpectation.MEDIUM,
                impliedMovePercent = 3.8,
                historicalReactionPercent = 3.5,
                keyFocus = "Net Interest Margins (NIM) resilience, credit growth momentum, and slippages control.",
                timeSlot = "Post-Market Close"
            ),
            UpcomingEarnings(
                symbol = "SOLARINDS",
                companyName = "Solar Industries India",
                sector = "Defence / Chemicals",
                expectedDate = d2.first,
                dayOfWeek = d2.second,
                marketCapCategory = "Mid-Cap",
                estimatedRevenueCr = 1850.0,
                estimatedPatCr = 280.0,
                volatility = VolatilityExpectation.HIGH,
                impliedMovePercent = 6.8,
                historicalReactionPercent = 6.2,
                keyFocus = "Pinaka rocket propellant deliveries and European defense commercialization.",
                timeSlot = "Post-Market Close"
            ),
            UpcomingEarnings(
                symbol = "HINDUNILVR",
                companyName = "Hindustan Unilever Ltd",
                sector = "FMCG / Consumer",
                expectedDate = d2.first,
                dayOfWeek = d2.second,
                marketCapCategory = "Large-Cap",
                estimatedRevenueCr = 15800.0,
                estimatedPatCr = 2650.0,
                volatility = VolatilityExpectation.LOW,
                impliedMovePercent = 2.2,
                historicalReactionPercent = 1.9,
                keyFocus = "Rural volume consumption revival, palm oil raw material inflation, and urban premiumization.",
                timeSlot = "During Market Hours"
            ),
            UpcomingEarnings(
                symbol = "MAZDOCK",
                companyName = "Mazagon Dock Shipbuilders",
                sector = "Shipbuilding / Defence",
                expectedDate = d3.first,
                dayOfWeek = d3.second,
                marketCapCategory = "Mid-Cap",
                estimatedRevenueCr = 2850.0,
                estimatedPatCr = 620.0,
                volatility = VolatilityExpectation.HIGH,
                impliedMovePercent = 9.4,
                historicalReactionPercent = 8.6,
                keyFocus = "Project 75I stealth submarine commercial RFP timeline and commercial export vessel orders.",
                timeSlot = "Post-Market Close"
            ),
            UpcomingEarnings(
                symbol = "COFORGE",
                companyName = "Coforge Ltd",
                sector = "IT Services",
                expectedDate = d3.first,
                dayOfWeek = d3.second,
                marketCapCategory = "Large-Cap",
                estimatedRevenueCr = 3350.0,
                estimatedPatCr = 310.0,
                volatility = VolatilityExpectation.MEDIUM,
                impliedMovePercent = 5.0,
                historicalReactionPercent = 4.8,
                keyFocus = "BFS and travel vertical deal signings, Cigniti integration synergies, and EBIT margin expansion.",
                timeSlot = "Pre-Market Open"
            ),
            UpcomingEarnings(
                symbol = "TATAPOWER",
                companyName = "Tata Power Company Ltd",
                sector = "Power & Utilities",
                expectedDate = d4.first,
                dayOfWeek = d4.second,
                marketCapCategory = "Large-Cap",
                estimatedRevenueCr = 16800.0,
                estimatedPatCr = 1180.0,
                volatility = VolatilityExpectation.LOW,
                impliedMovePercent = 3.2,
                historicalReactionPercent = 2.8,
                keyFocus = "Tirunelveli solar cell & module factory utilization rate and pumped hydro storage progress.",
                timeSlot = "Weekend Announcement"
            )
        )
    }

    fun getScreenerStocks(): List<ScreenerStock> {
        val list = mutableListOf<ScreenerStock>()

        // 1. DIXON
        list.add(
            createStock(
                symbol = "DIXON",
                name = "Dixon Technologies (India) Ltd",
                sector = "Electronics / EMS",
                cap = "Large-Cap",
                price = 14850.00,
                changePercent = 4.6,
                high52 = 15200.00,
                low52 = 5800.00,
                threeMReturn = 42.5,
                niftyReturn = 4.2,
                rsScore = 98,
                ema21 = 14200.00,
                sma50 = 13100.00,
                sma200 = 10400.00,
                volMultiplier = 4.2f,
                patterns = listOf("All-Weather Leader", "VCP Tightening", "Pocket Pivot", "Stage 2 Breakout"),
                isNear52 = true,
                isResilient = true,
                isPocket = true
            )
        )

        // 2. TRENT
        list.add(
            createStock(
                symbol = "TRENT",
                name = "Trent Ltd (Tata Retail)",
                sector = "Retail / Consumer",
                cap = "Large-Cap",
                price = 7840.00,
                changePercent = 3.8,
                high52 = 8120.00,
                low52 = 2750.00,
                threeMReturn = 46.8,
                niftyReturn = 4.2,
                rsScore = 99,
                ema21 = 7450.00,
                sma50 = 6920.00,
                sma200 = 5120.00,
                volMultiplier = 3.4f,
                patterns = listOf("All-Weather Leader", "ATH Consolidation", "Heavy Institutional Demand"),
                isNear52 = true,
                isResilient = true,
                isPocket = true
            )
        )

        // 3. KECINT
        list.add(
            createStock(
                symbol = "KECINT",
                name = "KEC International Ltd",
                sector = "Power T&D / Infra",
                cap = "Mid-Cap",
                price = 985.40,
                changePercent = 14.8,
                high52 = 1015.00,
                low52 = 560.00,
                threeMReturn = 38.4,
                niftyReturn = 4.2,
                rsScore = 96,
                ema21 = 880.00,
                sma50 = 840.00,
                sma200 = 710.00,
                volMultiplier = 4.8f,
                patterns = listOf("High Volume Breakout", "Pocket Pivot", "Order Book ATH"),
                isNear52 = true,
                isResilient = true,
                isPocket = true
            )
        )

        // 4. KAYNES
        list.add(
            createStock(
                symbol = "KAYNES",
                name = "Kaynes Technology India",
                sector = "Electronics / EMS",
                cap = "Mid-Cap",
                price = 5680.00,
                changePercent = 5.2,
                high52 = 5890.00,
                low52 = 2400.00,
                threeMReturn = 36.2,
                niftyReturn = 4.2,
                rsScore = 95,
                ema21 = 5350.00,
                sma50 = 4950.00,
                sma200 = 3850.00,
                volMultiplier = 2.8f,
                patterns = listOf("Near 52W High", "VCP Base 1", "OSAT Semiconductor Catalyst"),
                isNear52 = true,
                isResilient = true,
                isPocket = false
            )
        )

        // 5. BEL
        list.add(
            createStock(
                symbol = "BEL",
                name = "Bharat Electronics Ltd",
                sector = "Defence / Aerospace",
                cap = "Large-Cap",
                price = 328.50,
                changePercent = 2.9,
                high52 = 340.50,
                low52 = 135.00,
                threeMReturn = 28.5,
                niftyReturn = 4.2,
                rsScore = 92,
                ema21 = 312.00,
                sma50 = 298.00,
                sma200 = 245.00,
                volMultiplier = 2.4f,
                patterns = listOf("Resilient in Correction", "Defence Leader", "Above 50 SMA"),
                isNear52 = true,
                isResilient = true,
                isPocket = false
            )
        )

        // 6. MAZDOCK
        list.add(
            createStock(
                symbol = "MAZDOCK",
                name = "Mazagon Dock Shipbuilders",
                sector = "Shipbuilding / Defence",
                cap = "Mid-Cap",
                price = 4520.00,
                changePercent = 6.4,
                high52 = 4780.00,
                low52 = 1850.00,
                threeMReturn = 34.0,
                niftyReturn = 4.2,
                rsScore = 94,
                ema21 = 4280.00,
                sma50 = 3980.00,
                sma200 = 3150.00,
                volMultiplier = 3.6f,
                patterns = listOf("Pocket Pivot", "VCP Tightening", "Submarine Order Pipeline"),
                isNear52 = true,
                isResilient = true,
                isPocket = true
            )
        )

        // 7. COFORGE
        list.add(
            createStock(
                symbol = "COFORGE",
                name = "Coforge Ltd",
                sector = "IT Services",
                cap = "Large-Cap",
                price = 7850.00,
                changePercent = 3.2,
                high52 = 8150.00,
                low52 = 4300.00,
                threeMReturn = 31.5,
                niftyReturn = 4.2,
                rsScore = 93,
                ema21 = 7520.00,
                sma50 = 7100.00,
                sma200 = 5950.00,
                volMultiplier = 2.2f,
                patterns = listOf("IT Sector Outperformer", "Above All MAs", "Strong Earnings"),
                isNear52 = true,
                isResilient = true,
                isPocket = false
            )
        )

        // 8. SOLARINDS
        list.add(
            createStock(
                symbol = "SOLARINDS",
                name = "Solar Industries India",
                sector = "Defence / Chemicals",
                cap = "Mid-Cap",
                price = 11420.00,
                changePercent = 4.1,
                high52 = 11800.00,
                low52 = 6200.00,
                threeMReturn = 29.8,
                niftyReturn = 4.2,
                rsScore = 91,
                ema21 = 10850.00,
                sma50 = 10200.00,
                sma200 = 8400.00,
                volMultiplier = 2.5f,
                patterns = listOf("Resilient in Correction", "High Margin Moat", "Export Boom"),
                isNear52 = true,
                isResilient = true,
                isPocket = false
            )
        )

        // 9. PERSISTENT
        list.add(
            createStock(
                symbol = "PERSISTENT",
                name = "Persistent Systems Ltd",
                sector = "IT Services",
                cap = "Large-Cap",
                price = 5620.00,
                changePercent = 2.4,
                high52 = 5890.00,
                low52 = 3600.00,
                threeMReturn = 26.2,
                niftyReturn = 4.2,
                rsScore = 89,
                ema21 = 5420.00,
                sma50 = 5150.00,
                sma200 = 4420.00,
                volMultiplier = 1.9f,
                patterns = listOf("AI & Cloud Transformation", "Above 50 SMA"),
                isNear52 = true,
                isResilient = true,
                isPocket = false
            )
        )

        // 10. POLYCAB
        list.add(
            createStock(
                symbol = "POLYCAB",
                name = "Polycab India Ltd",
                sector = "Cables & Infra",
                cap = "Large-Cap",
                price = 6890.00,
                changePercent = 1.8,
                high52 = 7450.00,
                low52 = 3800.00,
                threeMReturn = 18.2,
                niftyReturn = 4.2,
                rsScore = 82,
                ema21 = 6720.00,
                sma50 = 6540.00,
                sma200 = 5620.00,
                volMultiplier = 1.3f,
                patterns = listOf("Healthy Pullback to 21 EMA", "Distribution Clear"),
                isNear52 = false,
                isResilient = false,
                isPocket = false
            )
        )

        // 11. LT
        list.add(
            createStock(
                symbol = "LT",
                name = "Larsen & Toubro Ltd",
                sector = "Capital Goods",
                cap = "Large-Cap",
                price = 3740.00,
                changePercent = 2.1,
                high52 = 3850.00,
                low52 = 2850.00,
                threeMReturn = 19.5,
                niftyReturn = 4.2,
                rsScore = 85,
                ema21 = 3620.00,
                sma50 = 3510.00,
                sma200 = 3280.00,
                volMultiplier = 2.1f,
                patterns = listOf("Mega Order Win", "Near 52W High"),
                isNear52 = true,
                isResilient = true,
                isPocket = false
            )
        )

        // 12. MOTHERSON
        list.add(
            createStock(
                symbol = "MOTHERSON",
                name = "Samvardhana Motherson Int",
                sector = "Auto Ancillary",
                cap = "Large-Cap",
                price = 186.40,
                changePercent = 3.6,
                high52 = 205.00,
                low52 = 88.00,
                threeMReturn = 24.1,
                niftyReturn = 4.2,
                rsScore = 87,
                ema21 = 178.00,
                sma50 = 170.00,
                sma200 = 138.00,
                volMultiplier = 2.7f,
                patterns = listOf("VCP Tightening", "EV Wiring Harness Growth"),
                isNear52 = false,
                isResilient = true,
                isPocket = true
            )
        )

        // 13. HAL
        list.add(
            createStock(
                symbol = "HAL",
                name = "Hindustan Aeronautics Ltd",
                sector = "Defence / Aviation",
                cap = "Large-Cap",
                price = 4780.00,
                changePercent = 3.1,
                high52 = 5670.00,
                low52 = 1920.00,
                threeMReturn = 14.8,
                niftyReturn = 4.2,
                rsScore = 79,
                ema21 = 4620.00,
                sma50 = 4580.00,
                sma200 = 3980.00,
                volMultiplier = 1.8f,
                patterns = listOf("Consolidating Above 50 SMA", "Tejas Mk1A Engines"),
                isNear52 = false,
                isResilient = false,
                isPocket = false
            )
        )

        // 14. TATAMOTORS
        list.add(
            createStock(
                symbol = "TATAMOTORS",
                name = "Tata Motors Ltd",
                sector = "Automobile",
                cap = "Large-Cap",
                price = 840.50,
                changePercent = -0.8,
                high52 = 1175.00,
                low52 = 640.00,
                threeMReturn = -12.4,
                niftyReturn = 4.2,
                rsScore = 42,
                ema21 = 865.00,
                sma50 = 910.00,
                sma200 = 945.00,
                volMultiplier = 0.9f,
                patterns = listOf("Trading Below 50 SMA", "JLR Demand Normalization"),
                isNear52 = false,
                isResilient = false,
                isPocket = false
            )
        )

        // 15. HDFCBANK
        list.add(
            createStock(
                symbol = "HDFCBANK",
                name = "HDFC Bank Ltd",
                sector = "Banking / Finance",
                cap = "Large-Cap",
                price = 1745.00,
                changePercent = 0.6,
                high52 = 1795.00,
                low52 = 1360.00,
                threeMReturn = 6.2,
                niftyReturn = 4.2,
                rsScore = 68,
                ema21 = 1720.00,
                sma50 = 1690.00,
                sma200 = 1590.00,
                volMultiplier = 1.2f,
                patterns = listOf("LDR Improvement", "Heavyweight Drag/Support"),
                isNear52 = true,
                isResilient = false,
                isPocket = false
            )
        )

        // 16. ZOMATO
        list.add(
            createStock(
                symbol = "ZOMATO",
                name = "Zomato Ltd (Blinkit)",
                sector = "Consumer Tech / Quick Commerce",
                cap = "Large-Cap",
                price = 265.40,
                changePercent = 5.2,
                high52 = 280.00,
                low52 = 112.00,
                threeMReturn = 48.2,
                niftyReturn = 4.2,
                rsScore = 97,
                ema21 = 252.00,
                sma50 = 238.00,
                sma200 = 195.00,
                volMultiplier = 3.8f,
                patterns = listOf("All-Weather Leader", "Quick-Commerce Duopoly", "Pocket Pivot", "Stage 2 Breakout"),
                isNear52 = true,
                isResilient = true,
                isPocket = true
            )
        )

        // 17. TITAGARH
        list.add(
            createStock(
                symbol = "TITAGARH",
                name = "Titagarh Rail Systems Ltd",
                sector = "Railways / Infra",
                cap = "Mid-Cap",
                price = 1420.00,
                changePercent = 4.8,
                high52 = 1896.00,
                low52 = 720.00,
                threeMReturn = 44.5,
                niftyReturn = 4.2,
                rsScore = 96,
                ema21 = 1360.00,
                sma50 = 1290.00,
                sma200 = 1040.00,
                volMultiplier = 4.1f,
                patterns = listOf("Vande Bharat Rolling Stock", "Stage 2 Breakout", "Pocket Pivot"),
                isNear52 = false,
                isResilient = true,
                isPocket = true
            )
        )

        // 18. VBL
        list.add(
            createStock(
                symbol = "VBL",
                name = "Varun Beverages Ltd",
                sector = "FMCG / Consumer",
                cap = "Large-Cap",
                price = 640.00,
                changePercent = 2.2,
                high52 = 682.00,
                low52 = 390.00,
                threeMReturn = 28.4,
                niftyReturn = 4.2,
                rsScore = 94,
                ema21 = 625.00,
                sma50 = 610.00,
                sma200 = 530.00,
                volMultiplier = 2.6f,
                patterns = listOf("Consistent Compounder", "Near 52W High", "BevCo Global Expansion"),
                isNear52 = true,
                isResilient = true,
                isPocket = false
            )
        )

        // 19. SUZLON
        list.add(
            createStock(
                symbol = "SUZLON",
                name = "Suzlon Energy Ltd",
                sector = "Renewable Energy / Capital Goods",
                cap = "Mid-Cap",
                price = 68.50,
                changePercent = 4.9,
                high52 = 86.00,
                low52 = 34.00,
                threeMReturn = 35.2,
                niftyReturn = 4.2,
                rsScore = 93,
                ema21 = 64.00,
                sma50 = 61.50,
                sma200 = 48.00,
                volMultiplier = 3.4f,
                patterns = listOf("Wind Energy Capex Cycle", "Net Cash Balance Sheet", "Pocket Pivot"),
                isNear52 = false,
                isResilient = true,
                isPocket = true
            )
        )

        // 20. PERSISTENT
        list.add(
            createStock(
                symbol = "PERSISTENT",
                name = "Persistent Systems Ltd",
                sector = "IT Services",
                cap = "Mid-Cap",
                price = 5680.00,
                changePercent = 3.2,
                high52 = 5950.00,
                low52 = 3500.00,
                threeMReturn = 32.1,
                niftyReturn = 4.2,
                rsScore = 91,
                ema21 = 5480.00,
                sma50 = 5310.00,
                sma200 = 4450.00,
                volMultiplier = 2.9f,
                patterns = listOf("AI Engineering Growth", "Near 52W High", "Pocket Pivot"),
                isNear52 = true,
                isResilient = true,
                isPocket = true
            )
        )

        // 21. BHARTIARTL
        list.add(
            createStock(
                symbol = "BHARTIARTL",
                name = "Bharti Airtel Ltd",
                sector = "Telecom / Infra",
                cap = "Large-Cap",
                price = 1610.00,
                changePercent = 1.8,
                high52 = 1720.00,
                low52 = 920.00,
                threeMReturn = 22.4,
                niftyReturn = 4.2,
                rsScore = 89,
                ema21 = 1580.00,
                sma50 = 1540.00,
                sma200 = 1380.00,
                volMultiplier = 2.1f,
                patterns = listOf("ARPU Expansion Leader", "Data Monetization", "Near 52W High"),
                isNear52 = true,
                isResilient = true,
                isPocket = false
            )
        )

        return list
    }

    private data class ConcallInfo(
        val takeaways: List<String>,
        val guidance: String,
        val capex: String,
        val date: String
    )

    private fun createStock(
        symbol: String,
        name: String,
        sector: String,
        cap: String,
        price: Double,
        changePercent: Double,
        high52: Double,
        low52: Double,
        threeMReturn: Double,
        niftyReturn: Double,
        rsScore: Int,
        ema21: Double,
        sma50: Double,
        sma200: Double,
        volMultiplier: Float,
        patterns: List<String>,
        isNear52: Boolean,
        isResilient: Boolean,
        isPocket: Boolean
    ): ScreenerStock {
        val dist52 = ((price - high52) / high52) * 100.0

        // Generate 30 realistic candles for interactive chart
        val candles = generateCandles(price, ema21.toFloat(), sma50.toFloat(), sma200.toFloat(), rsScore.toFloat())

        val concallInfo = when (symbol) {
            "DIXON" -> ConcallInfo(
                takeaways = listOf(
                    "Mobile manufacturing volume scaled to 8.5M units per quarter with global anchor tech client.",
                    "Backward integration into display modules & camera assemblies expanded gross margins to 4.2%.",
                    "Component localization on track to reach 35% under PLI 2.0 incentive milestones."
                ),
                guidance = "FY26 Revenue target raised to ₹21,500–22,500 Cr (+48% YoY). Export order pipeline exceeds $1.2B annually.",
                capex = "₹550 Cr earmarked for high-density SMT lines, precision robotics, and consumer electronics capacity.",
                date = "28 Oct 2026 • Q2 FY26 Concall"
            )
            "TRENT" -> ConcallInfo(
                takeaways = listOf(
                    "Zudio store count expanded by 43 stores in Q2, taking total network to 559 stores across 164 cities.",
                    "Westside like-for-like (LFL) sales growth sustained at +12% YoY with industry-leading store-level ROCE.",
                    "Emerging lifestyle & beauty categories now contribute over 20% of standalone revenue."
                ),
                guidance = "Aiming for 200+ net new store additions in FY26; gross margin stability expected at 45–46%.",
                capex = "Self-funded expansion entirely via operational cash flows; pristine debt-free balance sheet.",
                date = "04 Nov 2026 • Q2 FY26 Concall"
            )
            "ZOMATO" -> ConcallInfo(
                takeaways = listOf(
                    "Blinkit quick-commerce GOV surged 122% YoY with contribution margin turning positive at 3.8%.",
                    "Dark store network expanded to 791 stores; on track to cross 1,000 stores ahead of guidance.",
                    "Food delivery Adjusted EBITDA margin expanded to 3.5% of GOV with sticky consumer ordering."
                ),
                guidance = "Board approved ₹8,500 Cr capital raise to fortify balance sheet for rapid dark store buildout.",
                capex = "₹1,500 Cr capex allocated for automated regional distribution centers and cold chain warehousing.",
                date = "22 Oct 2026 • Q2 FY26 Concall"
            )
            "KECINT" -> ConcallInfo(
                takeaways = listOf(
                    "Order book reached all-time high of ₹34,200 Cr (+19% YoY) with multi-GW transmission line awards.",
                    "EBITDA margin reached 6.8% (+60 bps YoY) on completion of legacy fixed-price railway projects.",
                    "International T&D order intake from Saudi Aramco and GCC utilities growing at 35% CAGR."
                ),
                guidance = "FY26 Revenue growth guided at 15–17% with full-year EBITDA margin targeting 7.5% exit run-rate.",
                capex = "Asset-light capex model; maintenance capex contained at ₹200–250 Cr.",
                date = "04 Nov 2026 • Q2 FY26 Concall"
            )
            "TITAGARH" -> ConcallInfo(
                takeaways = listOf(
                    "Freight wagon production run-rate sustained at 1,000+ wagons per month with stable steel realization.",
                    "Vande Bharat commercial passenger trainset prototype on track for RDSO field trials by early 2027.",
                    "Export orders for metro carbody shells to Europe progressing with high milestone billing."
                ),
                guidance = "25% Revenue CAGR guided over FY26–28 with EBITDA margins in the 11.5–12.5% bracket.",
                capex = "₹650 Cr capex ongoing at Uttarpara facility for stainless steel carbody fabrication & bogie testing.",
                date = "30 Oct 2026 • Q2 FY26 Concall"
            )
            "KAYNES" -> ConcallInfo(
                takeaways = listOf(
                    "Order book swelled to ₹5,038 Cr with strong multi-year industrial and aerospace electronics contracts.",
                    "EBITDA margin expanded to 14.2% on higher mix of box-build products (now 42% of revenue).",
                    "Sanand OSAT semiconductor assembly facility received cabinet approval under ISM incentive scheme."
                ),
                guidance = "Guiding for 55–60% revenue growth in FY26 with sustainable 14%+ EBITDA margins.",
                capex = "Phase 1 OSAT capex of ₹2,850 Cr over 3 years supported by 70% central and state government subsidies.",
                date = "29 Oct 2026 • Q2 FY26 Concall"
            )
            "BEL" -> ConcallInfo(
                takeaways = listOf(
                    "Order book healthy at ₹74,595 Cr; bagged ₹7,400 Cr orders in H1 FY26 alone.",
                    "Gross margins stable at 44.5% supported by high indigenous content in electronic warfare systems.",
                    "Export revenue guided to double over next 2 fiscal years with Middle East radar orders."
                ),
                guidance = "FY26 revenue growth target maintained at 15% with EBITDA margin of 23–25%.",
                capex = "₹800 Cr capex plan for new defence systems integration facility at Nimmaluru, AP.",
                date = "28 Oct 2026 • Q2 FY26 Concall"
            )
            else -> ConcallInfo(
                takeaways = listOf(
                    "Institutional delivery volumes expanded significantly post Q2 FY26 quarterly earnings disclosure.",
                    "Operating margins demonstrated pricing power amid raw material price stability.",
                    "Balance sheet health strengthened with declining working capital cycle and robust operating cash flow."
                ),
                guidance = "Management maintains double-digit earnings growth guidance for FY26 with margin resilience.",
                capex = "Growth capex on schedule with internal accruals covering major planned project expansions.",
                date = "Late Oct 2026 • Q2 FY26 Concall"
            )
        }

        val dossier = ConcallDossier(
            latestQuarter = "Q2 FY26",
            concallDate = concallInfo.date,
            keyTakeaways = concallInfo.takeaways,
            managementGuidance = concallInfo.guidance,
            capexPlans = concallInfo.capex,
            audioUrl = "https://www.screener.in/company/$symbol/consolidated/#documents",
            transcriptPdfUrl = "https://www.screener.in/company/$symbol/consolidated/#documents",
            investorPptUrl = "https://www.bseindia.com/corporates/ann.html",
            annualReportUrl = "https://www.nseindia.com/companies-listing/corporate-filings-financial-results",
            screenerInUrl = "https://www.screener.in/company/$symbol/consolidated/",
            nseUrl = "https://www.nseindia.com/get-quotes/equity?symbol=$symbol"
        )

        // Calculate realistic quantitative statistical metrics (Vol, Correlation, Beta, Sharpe)
        val sdAnnual = when (cap) {
            "Large-Cap" -> 18.5 + (rsScore % 8) * 0.8
            "Mid-Cap" -> 25.0 + (rsScore % 10) * 0.9
            else -> 31.5 + (rsScore % 12) * 1.1
        }
        val sdDaily = sdAnnual / Math.sqrt(252.0)
        val sd30D = (sdAnnual * (0.88 + volMultiplier * 0.04)).coerceAtLeast(14.0)

        val corrNifty500 = when {
            cap == "Large-Cap" -> 0.72 + (rsScore % 5) * 0.02
            rsScore >= 90 -> 0.58 + (rsScore % 6) * 0.02 // Decoupled high alpha momentum
            else -> 0.65 + (rsScore % 5) * 0.02
        }
        val corrSector = (corrNifty500 + 0.16).coerceAtMost(0.92)
        val corrNifty50 = (corrNifty500 - 0.12).coerceAtLeast(0.35)

        val beta = when {
            rsScore >= 90 -> 1.32 + (rsScore % 4) * 0.05
            cap == "Large-Cap" -> 0.95 + (rsScore % 4) * 0.04
            else -> 1.18 + (rsScore % 5) * 0.04
        }

        val alphaAnnual = ((threeMReturn - niftyReturn) * 2.4).coerceAtLeast(-15.0)
        val sharpe = ((threeMReturn * 2.8 - 6.8) / sdAnnual).coerceIn(-1.5, 3.8)
        val sortino = (sharpe * 1.55).coerceIn(-2.0, 5.2)
        val rSquared = (corrNifty500 * corrNifty500 * 100.0).coerceIn(10.0, 95.0)
        val maxDd = -(6.5 + (sdAnnual * 0.35)).coerceAtLeast(5.0)

        val oneSigmaLow = price * (1.0 - sdAnnual / 100.0)
        val oneSigmaHigh = price * (1.0 + sdAnnual / 100.0)
        val twoSigmaLow = price * (1.0 - (2.0 * sdAnnual) / 100.0)
        val twoSigmaHigh = price * (1.0 + (2.0 * sdAnnual) / 100.0)

        val quantStats = StockQuantStats(
            standardDeviationAnnualized = sdAnnual,
            standardDeviationDaily = sdDaily,
            standardDeviation30D = sd30D,
            correlationNifty500 = corrNifty500,
            correlationSector = corrSector,
            correlationNifty50 = corrNifty50,
            betaNifty500 = beta,
            alphaAnnualizedPercent = alphaAnnual,
            sharpeRatio = sharpe,
            sortinoRatio = sortino,
            rSquaredPercent = rSquared,
            maxDrawdown1YPercent = maxDd,
            oneSigmaPriceLow = oneSigmaLow,
            oneSigmaPriceHigh = oneSigmaHigh,
            twoSigmaPriceLow = twoSigmaLow,
            twoSigmaPriceHigh = twoSigmaHigh
        )

        return ScreenerStock(
            symbol = symbol,
            name = name,
            sector = sector,
            marketCapCategory = cap,
            currentPrice = price,
            changePercent = changePercent,
            high52W = high52,
            low52W = low52,
            distanceFrom52WHighPercent = dist52,
            threeMonthReturnPercent = threeMReturn,
            nifty500ThreeMonthReturn = niftyReturn,
            rsScore = rsScore,
            ema21 = ema21,
            sma50 = sma50,
            sma200 = sma200,
            volumeMultiplier = volMultiplier,
            patterns = patterns,
            isNear52WHigh = isNear52,
            isResilientInCorrection = isResilient,
            isPocketPivot = isPocket,
            candles = candles,
            concall = dossier,
            quantStats = quantStats
        )
    }

    private fun generateCandles(
        currentClose: Double,
        ema21Target: Float,
        sma50Target: Float,
        sma200Target: Float,
        baseRs: Float
    ): List<CandleData> {
        val count = 28
        val rawList = mutableListOf<CandleData>()
        val startPrice = currentClose * 0.85
        var p = startPrice

        for (i in 0 until count) {
            val progress = i.toFloat() / (count - 1)
            val dayTrend = (currentClose - startPrice) / count
            val noise = (Random.nextFloat() - 0.45f) * (currentClose * 0.02f)
            val open = p.toFloat()
            val close = if (i == count - 1) currentClose.toFloat() else (p + dayTrend + noise).toFloat()
            val high = maxOf(open, close) + Random.nextFloat() * (currentClose * 0.015f).toFloat()
            val low = minOf(open, close) - Random.nextFloat() * (currentClose * 0.015f).toFloat()
            val vol = (500000 + Random.nextInt(1500000)).toLong()

            val ema21 = ema21Target * (0.92f + 0.08f * progress)
            val sma50 = sma50Target * (0.90f + 0.10f * progress)
            val sma200 = sma200Target * (0.88f + 0.12f * progress)
            val rsVal = baseRs * (0.85f + 0.15f * progress)

            val dateStr = "D-${count - i}"
            rawList.add(
                CandleData(
                    dateStr = dateStr,
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = vol,
                    ema21 = ema21,
                    sma50 = sma50,
                    sma200 = sma200,
                    rsValue = rsVal
                )
            )
            p = close.toDouble()
        }

        // Calculate RSI (14 period)
        val closes = rawList.map { it.close }
        val rsiValues = FloatArray(count) { 50f }
        var avgGain = 0f
        var avgLoss = 0f
        val rsiPeriod = 14

        for (i in 1 until count) {
            val change = closes[i] - closes[i - 1]
            val gain = if (change > 0) change else 0f
            val loss = if (change < 0) -change else 0f
            if (i <= rsiPeriod) {
                avgGain += gain
                avgLoss += loss
                if (i == rsiPeriod) {
                    avgGain /= rsiPeriod
                    avgLoss /= rsiPeriod
                }
            } else {
                avgGain = (avgGain * (rsiPeriod - 1) + gain) / rsiPeriod
                avgLoss = (avgLoss * (rsiPeriod - 1) + loss) / rsiPeriod
            }
            if (i >= rsiPeriod) {
                val rs = if (avgLoss <= 0.0001f) 100f else avgGain / avgLoss
                rsiValues[i] = (100f - (100f / (1f + rs))).coerceIn(10f, 95f)
            } else {
                // Approximate early RSI
                val earlyRs = if (avgLoss <= 0.0001f) 2f else (avgGain.coerceAtLeast(0.1f) / avgLoss.coerceAtLeast(0.1f))
                rsiValues[i] = (100f - (100f / (1f + earlyRs))).coerceIn(30f, 75f)
            }
        }
        rsiValues[0] = rsiValues[1]

        // Calculate MACD (Fast 12, Slow 26, Signal 9)
        val k12 = 2f / (12f + 1f)
        val k26 = 2f / (26f + 1f)
        val k9 = 2f / (9f + 1f)

        val ema12 = FloatArray(count) { closes[0] }
        val ema26 = FloatArray(count) { closes[0] }
        val macdLine = FloatArray(count) { 0f }
        val signalLine = FloatArray(count) { 0f }
        val histLine = FloatArray(count) { 0f }

        for (i in 1 until count) {
            ema12[i] = (closes[i] * k12) + (ema12[i - 1] * (1f - k12))
            ema26[i] = (closes[i] * k26) + (ema26[i - 1] * (1f - k26))
            macdLine[i] = ema12[i] - ema26[i]
        }
        signalLine[0] = macdLine[0]
        for (i in 1 until count) {
            signalLine[i] = (macdLine[i] * k9) + (signalLine[i - 1] * (1f - k9))
            histLine[i] = macdLine[i] - signalLine[i]
        }

        return rawList.mapIndexed { idx, candle ->
            candle.copy(
                rsi = rsiValues[idx],
                macd = macdLine[idx],
                macdSignal = signalLine[idx],
                macdHist = histLine[idx]
            )
        }
    }
}

data class MarketPulseData(
    val asOfDate: String,
    val nifty50Level: Double = 25299.35,
    val nifty50Change: Double = 44.30,
    val nifty50ChangePercent: Double = 0.18,
    val nifty500Level: Double = 23779.30,
    val nifty500Change: Double = 212.40,
    val nifty500ChangePercent: Double = 0.90,
    val fiiDiiFlows: List<FiiDiiPoint>,
    val bullets: List<MarketBullet>,
    val breadth: MarketBreadth
)

