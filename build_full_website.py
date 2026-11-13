import json

with open("generate_website.py") as f:
    code = f.read()

# Extract stocks
exec_globals = {}
exec(code, exec_globals)
stocks = exec_globals["stocks"]

results_data = [
    {
        "symbol": "DIXON",
        "name": "Dixon Technologies",
        "sector": "Electronics / EMS",
        "rev": "₹6,210 Cr", "revYoy": "+48.0%", "revQoq": "+22.4%",
        "pat": "₹390 Cr", "patYoy": "+72.5%", "patQoq": "+31.0%",
        "verdict": "STRONG_BEAT", "verdictLabel": "🚀 UPPER CIRCUIT (+20%)",
        "priceReaction": "+20.0%", "vol": "5.6x",
        "remark": "Landmark results driven by exponential smartphone manufacturing volumes. Stock locked in 20% Upper Circuit on monumental institutional demand; stage 2 breakout underway."
    },
    {
        "symbol": "KECINT",
        "name": "KEC International Ltd",
        "sector": "Power T&D / Infra",
        "rev": "₹5,340 Cr", "revYoy": "+19.5%", "revQoq": "+8.2%",
        "pat": "₹248 Cr", "patYoy": "+46.2%", "patQoq": "+18.1%",
        "verdict": "STRONG_BEAT", "verdictLabel": "⚡ STRONG BEAT",
        "priceReaction": "+14.8%", "vol": "4.8x",
        "remark": "Explosive operational beat with 46% PAT growth and 180 bps margin expansion. Aggressive institutional accumulation (4.8x 20DMA) drove a breakout above multi-month pivot as order book crossed ₹34,200 Cr."
    },
    {
        "symbol": "TRENT",
        "name": "Trent Ltd (Tata Retail)",
        "sector": "Retail / Consumer",
        "rev": "₹4,420 Cr", "revYoy": "+39.2%", "revQoq": "+12.1%",
        "pat": "₹485 Cr", "patYoy": "+44.8%", "patQoq": "+15.6%",
        "verdict": "STRONG_BEAT", "verdictLabel": "⚡ STRONG BEAT",
        "priceReaction": "+8.6%", "vol": "3.4x",
        "remark": "Zudio store count expansion (43 additions in Q2) continues without compromising store-level ROCE (>45%). Resilient institutional buying near all-time high."
    },
    {
        "symbol": "BEL",
        "name": "Bharat Electronics Ltd",
        "sector": "Defence",
        "rev": "₹4,890 Cr", "revYoy": "+16.4%", "revQoq": "+5.2%",
        "pat": "₹890 Cr", "patYoy": "+28.1%", "patQoq": "+11.4%",
        "verdict": "STRONG_BEAT", "verdictLabel": "⚡ STRONG BEAT",
        "priceReaction": "+6.4%", "vol": "2.9x",
        "remark": "Indigenization tailwinds driving record electronic warfare order conversion. Heavy delivery volumes noted during afternoon session with RS score of 92."
    },
    {
        "symbol": "COFORGE",
        "name": "Coforge Ltd",
        "sector": "IT Services",
        "rev": "₹3,280 Cr", "revYoy": "+18.2%", "revQoq": "+6.4%",
        "pat": "₹295 Cr", "patYoy": "+21.0%", "patQoq": "+8.5%",
        "verdict": "STRONG_BEAT", "verdictLabel": "⚡ STRONG BEAT",
        "priceReaction": "+5.2%", "vol": "2.4x",
        "remark": "Strongest execution across mid-tier IT. Order intake exceeded $500M for third consecutive quarter; RS line trending sharply upward."
    },
    {
        "symbol": "LT",
        "name": "Larsen & Toubro Ltd",
        "sector": "Capital Goods / EPC",
        "rev": "₹61,500 Cr", "revYoy": "+15.2%", "revQoq": "+7.1%",
        "pat": "₹3,980 Cr", "patYoy": "+17.8%", "patQoq": "+8.9%",
        "verdict": "STRONG_BEAT", "verdictLabel": "⚡ STRONG BEAT",
        "priceReaction": "+4.2%", "vol": "2.1x",
        "remark": "Core infrastructure pipeline strong across domestic transmission and Middle East offshore. Healthy volume support as stock tests 52-week highs."
    },
    {
        "symbol": "POLYCAB",
        "name": "Polycab India Ltd",
        "sector": "Cables & Infra",
        "rev": "₹4,980 Cr", "revYoy": "+14.1%", "revQoq": "+4.8%",
        "pat": "₹440 Cr", "patYoy": "+12.0%", "patQoq": "+3.2%",
        "verdict": "IN_LINE", "verdictLabel": "✅ IN-LINE",
        "priceReaction": "+1.8%", "vol": "1.2x",
        "remark": "In-line topline with stable raw material copper pass-through. Normal volume; stock is quietly consolidating above 50 SMA."
    },
    {
        "symbol": "TATAPOWER",
        "name": "Tata Power Company Ltd",
        "sector": "Power & Renewable",
        "rev": "₹16,200 Cr", "revYoy": "+11.8%", "revQoq": "+3.6%",
        "pat": "₹1,120 Cr", "patYoy": "+9.4%", "patQoq": "+2.5%",
        "verdict": "IN_LINE", "verdictLabel": "✅ IN-LINE",
        "priceReaction": "+2.1%", "vol": "1.6x",
        "remark": "Renewable solar module manufacturing ramp-up on track with 4.3 GW cell plant. Steady institutional accumulation seen near 21 EMA support."
    },
    {
        "symbol": "INFY",
        "name": "Infosys Ltd",
        "sector": "IT Services",
        "rev": "₹41,200 Cr", "revYoy": "+4.5%", "revQoq": "+1.2%",
        "pat": "₹6,540 Cr", "patYoy": "+3.8%", "patQoq": "+1.1%",
        "verdict": "IN_LINE", "verdictLabel": "✅ IN-LINE",
        "priceReaction": "-1.4%", "vol": "1.1x",
        "remark": "Met guidance but discretionary banking tech spend recovery remains gradual. Slight institutional profit-taking post disclosure."
    },
    {
        "symbol": "BAJAJCON",
        "name": "Bajaj Consumer Care",
        "sector": "FMCG / Personal Care",
        "rev": "₹238 Cr", "revYoy": "-4.2%", "revQoq": "-2.8%",
        "pat": "₹32 Cr", "patYoy": "-14.5%", "patQoq": "-9.2%",
        "verdict": "WEAK_MISS", "verdictLabel": "⚠️ WEAK MISS",
        "priceReaction": "-6.8%", "vol": "2.6x",
        "remark": "Rural demand sluggishness and competitive hair oil pricing compressed operating margins. High-volume institutional selling; price broke below 200 SMA."
    }
]

bullets_data = [
    { "category": "Net Buyers", "tag": "buyers", "title": "DIIs Pump ₹2,460.20 Cr into Equities (14-Day Inflow Streak)", "desc": "Domestic institutional funds absorbed foreign outflows, directing capital into capital goods, EMS, defense, and power transmission leaders." },
    { "category": "Net Sellers", "tag": "sellers", "title": "FIIs Net Sold ₹1,840.50 Cr in Cash Market", "desc": "Foreign institutional selling was concentrated in large-cap IT and private banks following US Treasury yield spikes, while mid-cap industrials saw FII buying." },
    { "category": "Upper Circuit", "tag": "circuit", "title": "Dixon Tech Locks in 20% Upper Circuit on Phenomenal Q2 Beat", "desc": "Smartphone manufacturing volume hit 8.5M units per quarter, driving 72.5% PAT surge. Stock experienced monumental institutional block delivery." },
    { "category": "Big Order", "tag": "order", "title": "L&T Hydrocarbon Secures ₹8,500 Cr GCC Offshore EPC Contract", "desc": "Major state-backed petroleum company in the Middle East awarded multi-field subsea gas development contract, boosting L&T's total order book to ₹5.1 lakh Cr." },
    { "category": "Big Results", "tag": "results", "title": "KEC International Surges 14.8% on Record 46% PAT Growth", "desc": "Global transmission line order wins and margin expansion to 6.8% triggered aggressive institutional breakout above multi-month pivot." },
    { "category": "Retail ATH", "tag": "retail", "title": "Trent Ltd Jumps 3.8% to Touch Fresh High of ₹7,840", "desc": "Zudio store count expanded to 559 stores across 164 cities with like-for-like sales up +12% YoY and ROCE exceeding 45%." },
    { "category": "Defence", "tag": "defence", "title": "BEL Bags ₹7,400 Cr Inflows in H1 FY26; Order Book at ₹74,595 Cr", "desc": "Electronic warfare systems and QRSAM radar deliveries contributed to stable 44.5% gross margins and institutional accumulation." },
    { "category": "Power Capex", "tag": "power", "title": "Suzlon Energy Net Cash Balance Sheet; Order Backlog at Record 5.1 GW", "desc": "NTPC Green and private C&I orders provide execution visibility through FY28 as company expands 3.X MW turbine blade capacity." },
    { "category": "Top Indices", "tag": "indices", "title": "Nifty PSU Bank (+2.08%) & Smallcap 250 (+1.14%) Outperform", "desc": "Broad market breadth favored domestic manufacturing and state-owned banks, while Nifty IT dipped -0.66% on modest discretionary spend outlook." },
    { "category": "Macro", "tag": "macro", "title": "Brent Crude Softens to $72.4/bbl; Margin Relief for Paints & OMCs", "desc": "Softening energy commodities support operating margin expansion across consumer paint makers, specialty chemicals, and transport utilities." },
    { "category": "Market Breadth", "tag": "breadth", "title": "Nifty 500 Market Breadth Bullish with 1.94:1 Advance-Decline Ratio", "desc": "318 stocks advanced vs 164 declines on Nifty 500, with 42 stocks touching fresh 52-week highs and only 4 hitting 52-week lows." },
    { "category": "Semiconductors", "tag": "tech", "title": "Kaynes Technology OSAT Facility Progresses under India Semi Mission", "desc": "Sanand OSAT semiconductor packaging plant on schedule with 70% state and central subsidies, securing multi-year global industrial contracts." }
]

print("Ready with data arrays.")
