import json

with open("generate_website.py") as f:
    code = f.read()

exec_globals = {}
exec(code, exec_globals)
stocks = exec_globals["stocks"]

with open("build_full_website.py") as f:
    code2 = f.read()

exec_globals2 = {}
exec(code2, exec_globals2)
results_data = exec_globals2["results_data"]
bullets_data = exec_globals2["bullets_data"]

indices_data = [
    { "name": "NIFTY 50", "price": "24,989.75", "chg": "+112.40 (+0.45%)", "high": "26,277.35", "low": "21,281.45", "trend": "Resilient Bullish", "ma": "Price > 21 EMA > 50 SMA" },
    { "name": "NIFTY 500", "price": "23,489.20", "chg": "+32.25 (+0.14%)", "high": "24,144.20", "low": "20,385.65", "trend": "Consolidating Bullish", "ma": "Price > 50 SMA > 200 SMA" },
    { "name": "NIFTY BANK", "price": "52,180.40", "chg": "+240.50 (+0.46%)", "high": "54,467.35", "low": "43,230.10", "trend": "Mild Bullish", "ma": "Price > 21 EMA" },
    { "name": "NIFTY MIDCAP 150", "price": "21,240.80", "chg": "+185.30 (+0.88%)", "high": "22,150.00", "low": "16,400.00", "trend": "Strong Bullish", "ma": "All-Time High Momentum" },
    { "name": "NIFTY SMALLCAP 250", "price": "18,650.40", "chg": "+210.60 (+1.14%)", "high": "19,400.00", "low": "14,100.00", "trend": "Strong Bullish", "ma": "Stage 2 Expansion" },
    { "name": "NIFTY PSU BANK", "price": "7,120.60", "chg": "+145.20 (+2.08%)", "high": "7,540.00", "low": "4,850.00", "trend": "Sector Leader", "ma": "Heavy Institutional Inflow" },
    { "name": "NIFTY AUTO", "price": "26,450.00", "chg": "+190.00 (+0.72%)", "high": "27,800.00", "low": "18,200.00", "trend": "Mild Bullish", "ma": "Consolidating Above 50 SMA" },
    { "name": "NIFTY IT", "price": "42,150.00", "chg": "-280.40 (-0.66%)", "high": "44,520.00", "low": "33,200.00", "trend": "Consolidating", "ma": "Testing 50 SMA Support" }
]

earnings_data = [
    { "symbol": "TATAMOTORS", "name": "Tata Motors Ltd", "date": "Tomorrow (Tue)", "pat": "₹4,200 Cr", "vol": "HIGH", "move": "±6.2%", "focus": "JLR order book & UK EV margins (Q2 FY26)" },
    { "symbol": "KAYNES", "name": "Kaynes Technology", "date": "Tomorrow (Tue)", "pat": "₹68 Cr", "vol": "HIGH", "move": "±8.5%", "focus": "Sanand OSAT commercialization milestone" },
    { "symbol": "BEL", "name": "Bharat Electronics", "date": "In 2 Days (Wed)", "pat": "₹920 Cr", "vol": "MEDIUM", "move": "±4.2%", "focus": "QRSAM radar systems milestone billing" },
    { "symbol": "SBIN", "name": "State Bank of India", "date": "In 2 Days (Wed)", "pat": "₹17,500 Cr", "vol": "MEDIUM", "move": "±3.8%", "focus": "NIM defense & SME loan growth pace" },
    { "symbol": "SOLARINDS", "name": "Solar Industries", "date": "In 3 Days (Thu)", "pat": "₹280 Cr", "vol": "HIGH", "move": "±6.8%", "focus": "Pinaka rocket propellant delivery run-rate" }
]

stocks_json = json.dumps(stocks)
results_json = json.dumps(results_data)
bullets_json = json.dumps(bullets_data)
indices_json = json.dumps(indices_data)
earnings_json = json.dumps(earnings_data)

html_template = f'''<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>MarketEdge Terminal • Indian Equity Momentum & RS Screener</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;500;600;700;800&family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
  <style>
    :root {{
      --bg: #F8FAFC;
      --surface: #FFFFFF;
      --card: #F1F5F9;
      --card-hover: #E2E8F0;
      --border: #CBD5E1;
      --border-subtle: #E2E8F0;
      --text-primary: #0F172A;
      --text-secondary: #475569;
      --text-tertiary: #64748B;
      --cyan-rs: #008899;
      --cyan-rs-bg: rgba(0, 136, 153, 0.10);
      --green: #059669;
      --green-bg: rgba(5, 150, 105, 0.10);
      --red: #DC2626;
      --red-bg: rgba(220, 38, 38, 0.10);
      --amber: #D97706;
      --amber-bg: rgba(217, 119, 6, 0.10);
      --purple: #7C3AED;
      --purple-bg: rgba(124, 58, 237, 0.10);
      --font-sans: 'Plus Jakarta Sans', system-ui, -apple-system, sans-serif;
      --font-mono: 'JetBrains Mono', monospace;
    }}

    [data-theme="dark"] {{
      --bg: #0B0E14;
      --surface: #121824;
      --card: #182030;
      --card-hover: #1E293B;
      --border: #222F44;
      --border-subtle: #1E293B;
      --text-primary: #F1F5F9;
      --text-secondary: #94A3B8;
      --text-tertiary: #64748B;
      --cyan-rs: #00E5FF;
      --cyan-rs-bg: rgba(0, 229, 255, 0.12);
      --green: #00E676;
      --green-bg: rgba(0, 230, 118, 0.12);
      --red: #FF3B30;
      --red-bg: rgba(255, 59, 48, 0.12);
      --amber: #FFB300;
      --amber-bg: rgba(255, 179, 0, 0.12);
      --purple: #A855F7;
      --purple-bg: rgba(168, 85, 247, 0.12);
    }}

    * {{ box-sizing: border-box; margin: 0; padding: 0; }}
    body {{
      background-color: var(--bg);
      color: var(--text-primary);
      font-family: var(--font-sans);
      min-height: 100vh;
      display: flex;
      flex-direction: column;
      overflow-x: hidden;
      transition: background-color 0.2s, color 0.2s;
    }}

    /* TOP TERMINAL HEADER */
    header.terminal-header {{
      background-color: var(--surface);
      border-bottom: 1px solid var(--border);
      padding: 12px 28px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      position: sticky;
      top: 0;
      z-index: 100;
      box-shadow: 0 1px 3px rgba(0,0,0,0.04);
    }}

    .brand-group {{
      display: flex;
      align-items: center;
      gap: 12px;
    }}
    .status-dot {{
      width: 10px;
      height: 10px;
      border-radius: 50%;
      background-color: var(--green);
      box-shadow: 0 0 8px var(--green);
    }}
    .brand-title {{
      font-weight: 800;
      font-size: 17px;
      letter-spacing: 0.8px;
      text-transform: uppercase;
      color: var(--text-primary);
    }}
    .badge-laptop {{
      background: var(--cyan-rs-bg);
      color: var(--cyan-rs);
      font-size: 10px;
      font-weight: 700;
      padding: 3px 8px;
      border-radius: 4px;
      border: 1px solid var(--cyan-rs);
      letter-spacing: 0.5px;
    }}

    .ticker-bar {{
      display: flex;
      align-items: center;
      gap: 20px;
      font-family: var(--font-mono);
      font-size: 13px;
    }}
    .ticker-item {{
      display: flex;
      align-items: center;
      gap: 6px;
    }}
    .ticker-label {{
      color: var(--text-secondary);
      font-weight: 600;
    }}
    .ticker-val {{
      font-weight: 700;
    }}
    .up {{ color: var(--green); }}
    .down {{ color: var(--red); }}

    .header-actions {{
      display: flex;
      align-items: center;
      gap: 10px;
    }}
    .theme-btn, .action-btn {{
      background: var(--card);
      border: 1px solid var(--border);
      color: var(--text-primary);
      padding: 8px 14px;
      border-radius: 8px;
      cursor: pointer;
      font-size: 12px;
      font-weight: 600;
      display: flex;
      align-items: center;
      gap: 6px;
      transition: all 0.2s;
    }}
    .theme-btn:hover, .action-btn:hover {{
      background: var(--card-hover);
      border-color: var(--cyan-rs);
    }}

    /* SUB NAVIGATION BAR */
    nav.terminal-nav {{
      background: var(--surface);
      border-bottom: 1px solid var(--border);
      display: flex;
      gap: 8px;
      padding: 8px 28px;
      overflow-x: auto;
    }}
    .nav-tab {{
      background: transparent;
      border: none;
      padding: 8px 16px;
      border-radius: 6px;
      color: var(--text-secondary);
      font-weight: 600;
      font-size: 13px;
      cursor: pointer;
      transition: all 0.2s;
      white-space: nowrap;
    }}
    .nav-tab.active {{
      background: var(--cyan-rs-bg);
      color: var(--cyan-rs);
      border: 1px solid var(--cyan-rs);
    }}
    .nav-tab:hover:not(.active) {{
      color: var(--text-primary);
      background: var(--card);
    }}

    /* MAIN CONTAINER (DESKTOP & LAPTOP ENHANCED) */
    main.terminal-main {{
      flex: 1;
      padding: 24px 28px;
      max-width: 1600px;
      width: 100%;
      margin: 0 auto;
    }}

    .tab-content {{ display: none; }}
    .tab-content.active {{ display: block; }}

    /* MARKET PULSE LAYOUT */
    .pulse-grid {{
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 24px;
    }}
    @media (max-width: 1080px) {{
      .pulse-grid {{ grid-template-columns: 1fr; }}
      .ticker-bar {{ display: none; }}
    }}

    .panel {{
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: 12px;
      padding: 20px;
      box-shadow: 0 1px 4px rgba(0,0,0,0.03);
    }}
    .panel-header {{
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
    }}
    .panel-title {{
      font-size: 15px;
      font-weight: 800;
      letter-spacing: 0.5px;
      text-transform: uppercase;
      display: flex;
      align-items: center;
      gap: 8px;
    }}
    .panel-subtitle {{
      font-size: 12px;
      color: var(--text-secondary);
      margin-top: 2px;
    }}

    /* FII / DII KPI CARDS */
    .kpi-row {{
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 14px;
      margin-bottom: 16px;
    }}
    .kpi-card {{
      background: var(--card);
      border: 1px solid var(--border);
      border-radius: 8px;
      padding: 14px;
    }}
    .kpi-title {{ font-size: 11px; color: var(--text-secondary); text-transform: uppercase; font-weight: 600; }}
    .kpi-val {{ font-size: 19px; font-weight: 800; font-family: var(--font-mono); margin: 4px 0; }}
    .kpi-desc {{ font-size: 11px; color: var(--text-tertiary); }}

    /* MARKET BREADTH BAR */
    .breadth-container {{
      background: var(--card);
      border: 1px solid var(--border);
      border-radius: 8px;
      padding: 14px;
      margin-bottom: 16px;
    }}
    .breadth-track {{
      display: flex;
      height: 12px;
      border-radius: 6px;
      overflow: hidden;
      margin: 8px 0;
    }}
    .breadth-adv {{ background: var(--green); }}
    .breadth-dec {{ background: var(--red); }}
    .breadth-unc {{ background: var(--text-tertiary); }}

    /* FII DII BAR CHART */
    .chart-container {{
      background: var(--card);
      border: 1px solid var(--border);
      border-radius: 8px;
      padding: 16px;
      margin-top: 12px;
    }}
    .bar-row {{
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 10px;
    }}
    .bar-date {{
      font-family: var(--font-mono);
      font-size: 12px;
      color: var(--text-secondary);
      width: 50px;
    }}
    .bar-track {{
      flex: 1;
      height: 12px;
      background: var(--border-subtle);
      border-radius: 6px;
      overflow: hidden;
      display: flex;
    }}
    .bar-fill.dii {{
      background: var(--green);
      height: 100%;
      border-radius: 6px;
    }}
    .bar-num {{
      font-family: var(--font-mono);
      font-size: 12px;
      font-weight: 700;
      width: 90px;
      text-align: right;
    }}

    /* BULLET CARDS */
    .bullet-card {{
      background: var(--card);
      border: 1px solid var(--border);
      border-radius: 8px;
      padding: 14px;
      margin-bottom: 12px;
      transition: border-color 0.15s;
    }}
    .bullet-card:hover {{
      border-color: var(--cyan-rs);
    }}
    .bullet-tag {{
      font-size: 10px;
      font-weight: 700;
      text-transform: uppercase;
      padding: 3px 6px;
      border-radius: 4px;
      margin-bottom: 6px;
      display: inline-block;
    }}
    .tag-buyers {{ background: var(--green-bg); color: var(--green); border: 1px solid var(--green); }}
    .tag-sellers {{ background: var(--red-bg); color: var(--red); border: 1px solid var(--red); }}
    .tag-circuit {{ background: var(--amber-bg); color: var(--amber); border: 1px solid var(--amber); }}
    .tag-order {{ background: var(--cyan-rs-bg); color: var(--cyan-rs); border: 1px solid var(--cyan-rs); }}
    .tag-results {{ background: var(--cyan-rs-bg); color: var(--cyan-rs); border: 1px solid var(--cyan-rs); }}
    .tag-retail {{ background: var(--green-bg); color: var(--green); }}
    .tag-defence {{ background: var(--cyan-rs-bg); color: var(--cyan-rs); }}
    .tag-power {{ background: var(--amber-bg); color: var(--amber); }}
    .tag-indices {{ background: var(--green-bg); color: var(--green); }}
    .tag-macro {{ background: var(--card-hover); color: var(--text-secondary); }}
    .tag-breadth {{ background: var(--green-bg); color: var(--green); }}
    .tag-tech {{ background: var(--purple-bg); color: var(--purple); }}

    .bullet-title {{ font-weight: 700; font-size: 14px; margin-bottom: 4px; color: var(--text-primary); }}
    .bullet-desc {{ font-size: 12px; color: var(--text-secondary); line-height: 1.5; }}

    /* SCREENER CONTROLS */
    .screener-controls {{
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: 12px;
      padding: 16px 20px;
      margin-bottom: 20px;
      display: flex;
      flex-direction: column;
      gap: 12px;
      box-shadow: 0 1px 3px rgba(0,0,0,0.03);
    }}
    .search-row {{
      display: flex;
      gap: 12px;
      align-items: center;
      justify-content: space-between;
      flex-wrap: wrap;
    }}
    .screener-input {{
      flex: 1;
      min-width: 260px;
      background: var(--card);
      border: 1px solid var(--border);
      border-radius: 8px;
      padding: 10px 14px;
      color: var(--text-primary);
      font-family: var(--font-sans);
      font-size: 13px;
    }}
    .screener-input:focus {{
      outline: none;
      border-color: var(--cyan-rs);
    }}
    .screener-select {{
      background: var(--card);
      border: 1px solid var(--border);
      border-radius: 8px;
      padding: 10px 14px;
      color: var(--text-primary);
      font-family: var(--font-sans);
      font-size: 12px;
      font-weight: 600;
    }}
    .screener-select:focus {{
      outline: none;
      border-color: var(--cyan-rs);
    }}
    .view-toggle-group {{
      display: flex;
      gap: 6px;
      background: var(--card);
      padding: 4px;
      border-radius: 8px;
      border: 1px solid var(--border);
    }}
    .view-btn {{
      background: transparent;
      border: none;
      padding: 6px 12px;
      border-radius: 6px;
      font-size: 12px;
      font-weight: 600;
      color: var(--text-secondary);
      cursor: pointer;
      transition: all 0.15s;
    }}
    .view-btn.active {{
      background: var(--surface);
      color: var(--cyan-rs);
      box-shadow: 0 1px 2px rgba(0,0,0,0.08);
    }}

    .chip-row {{
      display: flex;
      gap: 8px;
      overflow-x: auto;
      padding-bottom: 4px;
    }}
    .chip {{
      background: var(--card);
      border: 1px solid var(--border);
      border-radius: 20px;
      padding: 6px 14px;
      font-size: 11px;
      font-weight: 600;
      cursor: pointer;
      white-space: nowrap;
      transition: all 0.2s;
      color: var(--text-secondary);
    }}
    .chip:hover {{
      color: var(--text-primary);
      border-color: var(--cyan-rs);
    }}
    .chip.active {{
      background: var(--cyan-rs-bg);
      border-color: var(--cyan-rs);
      color: var(--cyan-rs);
      font-weight: 700;
    }}

    /* SCREENER CARDS VIEW */
    .screener-cards {{
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(370px, 1fr));
      gap: 16px;
    }}
    .stock-card {{
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: 12px;
      padding: 18px;
      cursor: pointer;
      transition: transform 0.15s, border-color 0.15s, box-shadow 0.15s;
      display: flex;
      flex-direction: column;
      box-shadow: 0 1px 3px rgba(0,0,0,0.03);
    }}
    .stock-card:hover {{
      transform: translateY(-2px);
      border-color: var(--cyan-rs);
      box-shadow: 0 6px 16px rgba(0,0,0,0.06);
    }}
    .stock-card.leader {{
      border-color: rgba(5, 150, 105, 0.4);
    }}

    .card-top {{
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 12px;
    }}
    .ticker-name {{
      font-family: var(--font-mono);
      font-size: 16px;
      font-weight: 800;
      color: var(--text-primary);
    }}
    .co-name {{
      font-size: 12px;
      color: var(--text-secondary);
      margin-top: 2px;
    }}
    .sector-name {{
      font-size: 11px;
      color: var(--cyan-rs);
      font-weight: 600;
      margin-top: 1px;
    }}
    .cap-badge {{
      font-size: 9px;
      padding: 2px 6px;
      border-radius: 4px;
      background: var(--card);
      border: 1px solid var(--border);
      color: var(--text-secondary);
      margin-left: 6px;
      text-transform: uppercase;
      font-weight: 700;
    }}

    .rs-score-badge {{
      font-family: var(--font-mono);
      font-weight: 800;
      font-size: 13px;
      padding: 4px 10px;
      border-radius: 6px;
      display: inline-block;
    }}
    .rs-super {{ background: var(--green-bg); color: var(--green); border: 1px solid var(--green); }}
    .rs-high {{ background: var(--cyan-rs-bg); color: var(--cyan-rs); border: 1px solid var(--cyan-rs); }}
    .rs-neutral {{ background: var(--amber-bg); color: var(--amber); border: 1px solid var(--amber); }}

    .gauge-wrapper {{
      margin: 8px 0;
    }}
    .gauge-label-row {{
      display: flex;
      justify-content: space-between;
      font-size: 10px;
      color: var(--text-secondary);
      font-family: var(--font-mono);
      margin-bottom: 4px;
    }}
    .gauge-bar {{
      height: 6px;
      background: var(--border-subtle);
      border-radius: 3px;
      overflow: hidden;
    }}
    .gauge-fill {{
      height: 100%;
      background: linear-gradient(90deg, #0D9488, #008899, #059669);
      border-radius: 3px;
    }}

    .metrics-pill-row {{
      background: var(--card);
      border-radius: 8px;
      padding: 8px 12px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 11px;
      font-family: var(--font-mono);
      margin: 10px 0;
    }}
    .pattern-row {{
      display: flex;
      gap: 6px;
      flex-wrap: wrap;
      margin-top: auto;
      padding-top: 8px;
    }}
    .pattern-tag {{
      font-size: 9px;
      padding: 3px 6px;
      border-radius: 4px;
      background: var(--card);
      border: 1px solid var(--border);
      color: var(--text-secondary);
      font-weight: 500;
    }}

    /* SCREENER TABLE VIEW (LAPTOP DEDICATED) */
    .screener-table-container {{
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: 12px;
      overflow-x: auto;
      box-shadow: 0 1px 4px rgba(0,0,0,0.03);
      display: none;
    }}
    .screener-table {{
      width: 100%;
      border-collapse: collapse;
      text-align: left;
      font-size: 12px;
      white-space: nowrap;
    }}
    .screener-table th {{
      background: var(--card);
      padding: 12px 14px;
      font-weight: 700;
      color: var(--text-secondary);
      font-size: 11px;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      border-bottom: 1px solid var(--border);
    }}
    .screener-table td {{
      padding: 12px 14px;
      border-bottom: 1px solid var(--border-subtle);
      color: var(--text-primary);
    }}
    .screener-table tr:hover td {{
      background: var(--card-hover);
      cursor: pointer;
    }}
    .screener-table tr:last-child td {{
      border-bottom: none;
    }}

    /* RESULTS HUB CARDS & TABLE */
    .results-grid {{
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(420px, 1fr));
      gap: 18px;
    }}
    .result-card {{
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: 12px;
      padding: 18px;
      box-shadow: 0 1px 4px rgba(0,0,0,0.03);
      display: flex;
      flex-direction: column;
      gap: 10px;
    }}
    .result-card.upper {{
      border-color: rgba(217, 119, 6, 0.4);
      background: linear-gradient(180deg, var(--surface) 0%, var(--card) 100%);
    }}
    .verdict-tag {{
      font-size: 10px;
      font-weight: 800;
      padding: 4px 8px;
      border-radius: 4px;
      display: inline-block;
      letter-spacing: 0.5px;
    }}
    .verdict-strong {{ background: var(--green-bg); color: var(--green); border: 1px solid var(--green); }}
    .verdict-inline {{ background: var(--cyan-rs-bg); color: var(--cyan-rs); border: 1px solid var(--cyan-rs); }}
    .verdict-weak {{ background: var(--red-bg); color: var(--red); border: 1px solid var(--red); }}
    .verdict-upper {{ background: var(--amber-bg); color: var(--amber); border: 1px solid var(--amber); }}

    /* INTERACTIVE CANVAS CHART */
    .chart-box {{
      background: var(--card);
      border: 1px solid var(--border);
      border-radius: 8px;
      padding: 12px;
      margin: 12px 0;
      position: relative;
    }}
    .chart-legend {{
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-family: var(--font-mono);
      font-size: 11px;
      margin-bottom: 8px;
      flex-wrap: wrap;
      gap: 8px;
    }}
    .tf-btn {{
      background: var(--surface);
      border: 1px solid var(--border);
      color: var(--text-secondary);
      padding: 3px 8px;
      border-radius: 4px;
      font-size: 10px;
      font-weight: 700;
      cursor: pointer;
    }}
    .tf-btn.active {{
      background: var(--cyan-rs-bg);
      color: var(--cyan-rs);
      border-color: var(--cyan-rs);
    }}

    /* 52W RANGE BAR */
    .range52-bar {{
      height: 8px;
      background: var(--border-subtle);
      border-radius: 4px;
      position: relative;
      margin: 10px 0;
    }}
    .range52-fill {{
      height: 100%;
      background: linear-gradient(90deg, #DC2626, #D97706, #059669);
      border-radius: 4px;
    }}
    .range52-thumb {{
      position: absolute;
      top: -4px;
      width: 16px;
      height: 16px;
      border-radius: 50%;
      background: var(--cyan-rs);
      border: 2px solid var(--surface);
      box-shadow: 0 0 6px rgba(0,0,0,0.3);
      transform: translateX(-50%);
    }}

    /* STATS BADGE GRID */
    .stat-badge-grid {{
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(130px, 1fr));
      gap: 10px;
      margin: 12px 0;
    }}
    .stat-box {{
      background: var(--card);
      border: 1px solid var(--border);
      border-radius: 8px;
      padding: 10px;
      text-align: center;
    }}
    .stat-box-title {{
      font-size: 10px;
      color: var(--text-secondary);
      text-transform: uppercase;
      font-weight: 600;
    }}
    .stat-box-val {{
      font-family: var(--font-mono);
      font-weight: 800;
      font-size: 14px;
      margin-top: 4px;
      color: var(--text-primary);
    }}

    /* MODAL DOSSIER */
    .modal-overlay {{
      position: fixed;
      top: 0; left: 0; right: 0; bottom: 0;
      background: rgba(15, 23, 42, 0.65);
      backdrop-filter: blur(4px);
      display: none;
      justify-content: center;
      align-items: center;
      z-index: 999;
      padding: 20px;
    }}
    .modal-overlay.active {{ display: flex; }}
    .modal-content {{
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: 16px;
      width: 100%;
      max-width: 920px;
      max-height: 90vh;
      overflow-y: auto;
      padding: 28px;
      box-shadow: 0 20px 40px rgba(0,0,0,0.25);
    }}
    .modal-close {{
      background: var(--card);
      border: 1px solid var(--border);
      color: var(--text-primary);
      width: 32px;
      height: 32px;
      border-radius: 50%;
      cursor: pointer;
      float: right;
      font-weight: 700;
      display: flex;
      align-items: center;
      justify-content: center;
    }}
    .modal-close:hover {{
      background: var(--card-hover);
    }}

    /* FOOTER */
    footer.terminal-footer {{
      background: var(--surface);
      border-top: 1px solid var(--border);
      padding: 16px 28px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 11px;
      color: var(--text-secondary);
      margin-top: auto;
    }}
  </style>
</head>
<body data-theme="light">

  <!-- TOP TERMINAL HEADER -->
  <header class="terminal-header">
    <div class="brand-group">
      <div class="status-dot"></div>
      <div class="brand-title">MarketEdge Terminal</div>
      <span class="badge-laptop">FULL WEBSITE RUNTIME</span>
      <span style="font-size: 11px; color: var(--text-secondary); font-family: var(--font-mono);" id="header-date">LIVE</span>
    </div>

    <!-- NIFTY 50 & 500 TICKER -->
    <div class="ticker-bar">
      <div class="ticker-item">
        <span class="ticker-label">NIFTY 50:</span>
        <span class="ticker-val" id="nifty50-val">24,989.75</span>
        <span class="up" id="nifty50-chg">+112.40 (+0.45%)</span>
      </div>
      <div class="ticker-item">
        <span class="ticker-label">• N500:</span>
        <span class="ticker-val">23,489.20</span>
        <span class="up">+32.25 (+0.14%)</span>
      </div>
      <div class="ticker-item">
        <span class="ticker-label">• BANK NIFTY:</span>
        <span class="ticker-val">52,180.40</span>
        <span class="up">+240.50 (+0.46%)</span>
      </div>
    </div>

    <!-- ACTIONS -->
    <div class="header-actions">
      <button class="theme-btn" id="theme-toggle" onclick="toggleTheme()">🌙 Dark</button>
      <button class="theme-btn" onclick="exportScreenerCSV()">📥 Export Screener</button>
    </div>
  </header>

  <!-- NAVIGATION BAR -->
  <nav class="terminal-nav">
    <button class="nav-tab active" onclick="switchTab('pulse')">⚡ Market Pulse</button>
    <button class="nav-tab" onclick="switchTab('screener')">🎯 RS Momentum Screener</button>
    <button class="nav-tab" onclick="switchTab('results')">📊 Published Results (Q2 FY26)</button>
    <button class="nav-tab" onclick="switchTab('earnings')">📅 Earnings Radar</button>
    <button class="nav-tab" onclick="switchTab('indices')">📈 Indices Matrix</button>
    <button class="nav-tab" onclick="switchTab('theses')">📝 Investment Theses</button>
  </nav>

  <!-- MAIN TERMINAL CONTENT -->
  <main class="terminal-main">

    <!-- TAB 1: MARKET PULSE -->
    <section id="tab-pulse" class="tab-content active">
      <div class="pulse-grid">
        
        <!-- LEFT: INSTITUTIONAL FLOWS & MARKET BREADTH -->
        <div class="panel">
          <div class="panel-header">
            <div>
              <div class="panel-title">Institutional Cash Flows (FII & DII)</div>
              <div class="panel-subtitle">Daily Cash Market Activity & Cumulative Net Bias</div>
            </div>
            <span style="font-size: 11px; font-weight: 700; color: var(--green);">14D DII BUY STREAK</span>
          </div>

          <div class="kpi-row">
            <div class="kpi-card">
              <div class="kpi-title">FII Net Today (Foreign Selling)</div>
              <div class="kpi-val down">-₹1,840.50 Cr</div>
              <div class="kpi-desc">Absorbed by Domestic Liquidity</div>
            </div>
            <div class="kpi-card">
              <div class="kpi-title">DII Net Today (Domestic Buying)</div>
              <div class="kpi-val up">+₹2,460.20 Cr</div>
              <div class="kpi-desc">14th Consecutive Day Inflow</div>
            </div>
          </div>

          <!-- MARKET BREADTH GAUGE -->
          <div class="breadth-container">
            <div style="display: flex; justify-content: space-between; align-items: center; font-size: 11px;">
              <span style="font-weight: 700; text-transform: uppercase;">Nifty 500 Market Breadth (Adv/Dec)</span>
              <span style="color: var(--green); font-weight: 800;">1.94:1 Bullish Ratio</span>
            </div>
            <div class="breadth-track">
              <div class="breadth-adv" style="width: 63.6%;"></div>
              <div class="breadth-dec" style="width: 32.8%;"></div>
              <div class="breadth-unc" style="width: 3.6%;"></div>
            </div>
            <div style="display: flex; justify-content: space-between; font-size: 11px; font-family: var(--font-mono);">
              <span class="up">▲ 318 Advancing</span>
              <span style="color: var(--text-tertiary);">• 18 Unchanged</span>
              <span class="down">▼ 164 Declining</span>
            </div>
          </div>

          <!-- DII 5-DAY BAR CHART -->
          <div class="chart-container">
            <div style="font-size: 11px; color: var(--text-secondary); margin-bottom: 8px; font-weight: 600;">DII 5-DAY NET CASH BUYING (₹ CRORE)</div>
            <div id="fii-dii-bars"></div>
          </div>
        </div>

        <!-- RIGHT: 12 CURATED MARKET TAKEAWAYS -->
        <div class="panel">
          <div class="panel-header">
            <div>
              <div class="panel-title">Today's Market Pulse & Key Bullet Points</div>
              <div class="panel-subtitle">Institutional Net Flow, Big Corporate Results & Sector Movers</div>
            </div>
            <span style="font-size: 11px; font-weight: 700; color: var(--cyan-rs);">12 LIVE BULLETS</span>
          </div>

          <div id="bullets-list" style="max-height: 620px; overflow-y: auto; padding-right: 4px;">
            <!-- Bullets inserted dynamically -->
          </div>
        </div>

      </div>
    </section>

    <!-- TAB 2: RELATIVE STRENGTH SCREENER -->
    <section id="tab-screener" class="tab-content">
      <div class="screener-controls">
        
        <!-- SEARCH, SORT & VIEW TOGGLE (LAPTOP POWER USER) -->
        <div class="search-row">
          <input type="text" id="screener-search" class="screener-input" 
                 placeholder="Search Ticker Symbol (e.g. DIXON, TRENT, BEL, ZOMATO) or Sector..." 
                 oninput="filterScreener()">

          <select id="screener-sort" class="screener-select" onchange="filterScreener()">
            <option value="RS_DESC">Sort: RS Score (High to Low)</option>
            <option value="GAINERS">Sort: Top Gainers Today (%)</option>
            <option value="NEAR52">Sort: Closest to 52W High</option>
            <option value="VOL_SURGE">Sort: Volume Surge (vs 20DMA)</option>
            <option value="ALPHA_3M">Sort: 3M Alpha vs N500</option>
          </select>

          <div class="view-toggle-group">
            <button class="view-btn active" id="btn-view-cards" onclick="setViewMode('cards')">Cards View</button>
            <button class="view-btn" id="btn-view-table" onclick="setViewMode('table')">📊 Laptop Table View</button>
          </div>
        </div>

        <!-- PRESET CHIPS -->
        <div class="chip-row" id="preset-chips">
          <div class="chip active" onclick="setPreset('ALL', this)">All Stocks (21)</div>
          <div class="chip" onclick="setPreset('SUPER', this)">Super Momentum (RS≥95)</div>
          <div class="chip" onclick="setPreset('WEATHER', this)">All-Weather (RS>80)</div>
          <div class="chip" onclick="setPreset('NEAR52', this)">Within 5% 52W High</div>
          <div class="chip" onclick="setPreset('PIVOT', this)">Pocket Pivot (>2x Vol)</div>
          <div class="chip" onclick="setPreset('RESILIENT', this)">Resilient in Correction</div>
        </div>

        <!-- SECTORS CHIPS -->
        <div class="chip-row" id="sector-chips">
          <div class="chip active" onclick="setSector('All', this)">All Sectors</div>
          <div class="chip" onclick="setSector('Electronics', this)">Electronics / EMS</div>
          <div class="chip" onclick="setSector('Retail', this)">Retail / Consumer</div>
          <div class="chip" onclick="setSector('Defence', this)">Defence & Aerospace</div>
          <div class="chip" onclick="setSector('Power', this)">Power T&D / Infra</div>
          <div class="chip" onclick="setSector('Railways', this)">Railways</div>
          <div class="chip" onclick="setSector('Consumer Tech', this)">Consumer Tech</div>
          <div class="chip" onclick="setSector('IT Services', this)">IT Services</div>
          <div class="chip" onclick="setSector('Banking', this)">Banking / Financials</div>
        </div>

      </div>

      <!-- CARDS VIEW -->
      <div class="screener-cards" id="screener-cards-grid">
        <!-- Rendered by JavaScript -->
      </div>

      <!-- LAPTOP TABLE VIEW -->
      <div class="screener-table-container" id="screener-table-container">
        <table class="screener-table">
          <thead>
            <tr>
              <th>Symbol</th>
              <th>Company Name</th>
              <th>Sector</th>
              <th>Market Cap</th>
              <th>Price (₹)</th>
              <th>Chg %</th>
              <th>RS Score</th>
              <th>Dist 52W</th>
              <th>3M Alpha</th>
              <th>Vol 20D</th>
              <th>Volatility (SD)</th>
              <th>Correlation N500</th>
              <th>Concall Highlights (Q2 FY26)</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody id="screener-table-body">
            <!-- Rendered by JavaScript -->
          </tbody>
        </table>
      </div>
    </section>

    <!-- TAB 3: PUBLISHED RESULTS (Q2 FY26) -->
    <section id="tab-results" class="tab-content">
      <div class="screener-controls">
        <div class="search-row">
          <div>
            <h3 style="font-size: 15px; font-weight: 800; text-transform: uppercase;">Published Corporate Earnings Analysis (Q2 FY26)</h3>
            <p style="font-size: 12px; color: var(--text-secondary); margin-top: 2px;">Verdict, Revenue & PAT Growth, Price Momentum & Investor Reactions</p>
          </div>
          <div class="chip-row" id="verdict-filter-chips">
            <div class="chip active" onclick="filterResults('ALL', this)">All Results (10)</div>
            <div class="chip" onclick="filterResults('STRONG_BEAT', this)">⚡ Strong Beats</div>
            <div class="chip" onclick="filterResults('IN_LINE', this)">✅ In-Line</div>
            <div class="chip" onclick="filterResults('WEAK_MISS', this)">⚠️ Weak Misses</div>
          </div>
        </div>
      </div>

      <div class="results-grid" id="results-cards-grid">
        <!-- Rendered by JavaScript -->
      </div>
    </section>

    <!-- TAB 4: EARNINGS RADAR (Q2 FY26) -->
    <section id="tab-earnings" class="tab-content">
      <div class="panel">
        <div class="panel-header">
          <div>
            <div class="panel-title">Upcoming Week Earnings & Options Volatility Calendar</div>
            <div class="panel-subtitle">Official Filing Timeline, Implied Options Reaction & Key Focus Areas</div>
          </div>
          <span style="font-size: 11px; font-weight: 700; color: var(--cyan-rs);">CURRENT REPORTING TIMELINE</span>
        </div>
        <div id="earnings-list"></div>
      </div>
    </section>

    <!-- TAB 5: INDICES MATRIX -->
    <section id="tab-indices" class="tab-content">
      <div class="panel">
        <div class="panel-header">
          <div>
            <div class="panel-title">Broad & Sectoral Benchmark Matrix</div>
            <div class="panel-subtitle">Real-time Performance against Moving Averages and 52-Week Range</div>
          </div>
          <span style="font-size: 11px; font-weight: 700; color: var(--green);">BENCHMARK: NIFTY 500</span>
        </div>
        <div id="indices-grid" style="display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 16px;"></div>
      </div>
    </section>

    <!-- TAB 6: INVESTMENT THESES -->
    <section id="tab-theses" class="tab-content">
      <div class="panel">
        <div class="panel-header">
          <div>
            <div class="panel-title">Private Investment Thesis Journal</div>
            <div class="panel-subtitle">Stored privately on your device via Browser LocalStorage</div>
          </div>
          <span style="font-size: 11px; font-weight: 700; color: var(--amber);">PRIVATE & LOCAL</span>
        </div>
        
        <div style="display: flex; gap: 12px; margin-bottom: 20px; flex-wrap: wrap;">
          <input type="text" id="thesis-symbol" class="screener-input" placeholder="Stock Symbol (e.g. DIXON, TRENT)" style="max-width: 200px;">
          <input type="text" id="thesis-target" class="screener-input" placeholder="Target Price (₹)" style="max-width: 160px;">
          <input type="text" id="thesis-text" class="screener-input" placeholder="Core investment thesis, catalysts & conviction note...">
          <button class="theme-btn" onclick="saveThesis()" style="background: var(--cyan-rs-bg); color: var(--cyan-rs); border-color: var(--cyan-rs);">+ Add Note</button>
        </div>

        <div id="theses-cards-container"></div>
      </div>
    </section>

  </main>

  <!-- MODAL DOSSIER FOR STOCK DETAIL WITH INTERACTIVE CANVAS CHART -->
  <div class="modal-overlay" id="stock-modal" onclick="closeModal(event)">
    <div class="modal-content" onclick="event.stopPropagation()">
      <button class="modal-close" onclick="closeModalDirect()">✕</button>
      <div id="modal-body"></div>
    </div>
  </div>

  <!-- FOOTER -->
  <footer class="terminal-footer">
    <div>MarketEdge Terminal • Indian Equity Screener • Primary Benchmark: NIFTY 500 • Reporting Period: Q2 FY26</div>
    <div id="footer-time">Dynamic Official Market Sessions</div>
  </footer>

  <script>
    // DATA STORES
    const STOCKS = {stocks_json};
    const RESULTS = {results_json};
    const BULLETS = {bullets_json};
    const INDICES = {indices_json};
    const UPCOMING_EARNINGS = {earnings_json};

    let currentPreset = 'ALL';
    let currentSector = 'All';
    let currentResultFilter = 'ALL';
    let currentViewMode = 'cards';
    let activeStockCandles = [];
    let activeStockSymbol = '';
    let selectedTimeframe = '3M';

    // INITIALIZATION
    document.addEventListener("DOMContentLoaded", () => {{
      const now = new Date();
      const options = {{ day: '2-digit', month: 'short', year: 'numeric' }};
      const dateStr = now.toLocaleDateString('en-GB', options);
      document.getElementById("header-date").innerText = `${{dateStr}} • Market Active`;
      document.getElementById("footer-time").innerText = `${{dateStr}} • Real-time Data Sync`;

      renderFiiDii();
      renderBullets();
      renderScreener();
      renderResults();
      renderIndices();
      renderEarnings();
      renderTheses();
    }});

    // THEME TOGGLE (LIGHT THEME DEFAULT)
    function toggleTheme() {{
      const body = document.body;
      const btn = document.getElementById("theme-toggle");
      if (body.getAttribute("data-theme") === "dark") {{
        body.setAttribute("data-theme", "light");
        btn.innerText = "🌙 Dark";
      }} else {{
        body.setAttribute("data-theme", "dark");
        btn.innerText = "☀️ Light";
      }}
      if (activeStockSymbol) {{
        drawStockChart();
      }}
    }}

    // VIEW MODE (CARDS VS LAPTOP TABLE)
    function setViewMode(mode) {{
      currentViewMode = mode;
      const btnCards = document.getElementById("btn-view-cards");
      const btnTable = document.getElementById("btn-view-table");
      const grid = document.getElementById("screener-cards-grid");
      const table = document.getElementById("screener-table-container");

      if (mode === 'table') {{
        btnTable.classList.add("active");
        btnCards.classList.remove("active");
        grid.style.display = "none";
        table.style.display = "block";
      }} else {{
        btnCards.classList.add("active");
        btnTable.classList.remove("active");
        grid.style.display = "grid";
        table.style.display = "none";
      }}
    }}

    // TAB SWITCHER
    function switchTab(tabId) {{
      document.querySelectorAll(".nav-tab").forEach(tab => tab.classList.remove("active"));
      document.querySelectorAll(".tab-content").forEach(content => content.classList.remove("active"));
      
      const tabBtn = Array.from(document.querySelectorAll(".nav-tab")).find(btn => btn.getAttribute("onclick")?.includes(tabId));
      if (tabBtn) tabBtn.classList.add("active");
      
      const section = document.getElementById(`tab-${{tabId}}`);
      if (section) section.classList.add("active");
    }}

    // RENDER FII / DII
    function renderFiiDii() {{
      const container = document.getElementById("fii-dii-bars");
      const flows = [
        {{ date: "T-0", dii: 2460, fii: 1840 }},
        {{ date: "T-1", dii: 3180, fii: 2120 }},
        {{ date: "T-2", dii: 1240, fii: -890 }},
        {{ date: "T-3", dii: 4110, fii: 3450 }},
        {{ date: "T-4", dii: 2320, fii: 1280 }}
      ];

      let html = '';
      flows.forEach(f => {{
        const diiWidth = Math.min(100, (f.dii / 4500) * 100);
        html += `
          <div class="bar-row">
            <div class="bar-date">${{f.date}}</div>
            <div class="bar-track">
              <div class="bar-fill dii" style="width: ${{diiWidth}}%;"></div>
            </div>
            <div class="bar-num up">+₹${{f.dii}} Cr</div>
          </div>
        `;
      }});
      container.innerHTML = html;
    }}

    // RENDER 12 BULLETS
    function renderBullets() {{
      const container = document.getElementById("bullets-list");
      let html = '';
      BULLETS.forEach(b => {{
        html += `
          <div class="bullet-card">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px;">
              <span class="bullet-tag tag-${{b.tag}}">${{b.category}}</span>
            </div>
            <div class="bullet-title">${{b.title}}</div>
            <div class="bullet-desc">${{b.desc}}</div>
          </div>
        `;
      }});
      container.innerHTML = html;
    }}

    // RENDER SCREENER
    function renderScreener() {{
      const grid = document.getElementById("screener-cards-grid");
      const tableBody = document.getElementById("screener-table-body");
      const query = (document.getElementById("screener-search").value || "").toLowerCase();
      const sortBy = document.getElementById("screener-sort")?.value || "RS_DESC";

      let filtered = STOCKS.filter(s => {{
        const matchesQuery = s.symbol.toLowerCase().includes(query) || s.name.toLowerCase().includes(query) || s.sector.toLowerCase().includes(query);
        const matchesSector = currentSector === 'All' || s.sector.includes(currentSector);
        
        let matchesPreset = true;
        if (currentPreset === 'SUPER') matchesPreset = s.rs >= 95;
        if (currentPreset === 'WEATHER') matchesPreset = s.rs >= 80;
        if (currentPreset === 'NEAR52') matchesPreset = ((s.high52 - s.price)/s.high52) <= 0.05;
        if (currentPreset === 'PIVOT') matchesPreset = s.vol >= 3.0;
        if (currentPreset === 'RESILIENT') matchesPreset = (s.threeM - s.niftyReturn) >= 20.0;

        return matchesQuery && matchesSector && matchesPreset;
      }});

      // Sorting
      filtered.sort((a, b) => {{
        if (sortBy === 'RS_DESC') return b.rs - a.rs;
        if (sortBy === 'GAINERS') return b.chg - a.chg;
        if (sortBy === 'NEAR52') return ((b.price - b.high52)/b.high52) - ((a.price - a.high52)/a.high52);
        if (sortBy === 'VOL_SURGE') return b.vol - a.vol;
        if (sortBy === 'ALPHA_3M') return (b.threeM - b.niftyReturn) - (a.threeM - a.niftyReturn);
        return 0;
      }});

      if (filtered.length === 0) {{
        grid.innerHTML = `<div style="grid-column: 1/-1; text-align: center; padding: 40px; color: var(--text-secondary);">No matching stocks found. Try clearing filters.</div>`;
        tableBody.innerHTML = `<tr><td colspan="14" style="text-align: center; padding: 30px; color: var(--text-secondary);">No matching stocks found.</td></tr>`;
        return;
      }}

      let cardsHtml = '';
      let tableHtml = '';

      filtered.forEach(s => {{
        const alpha = (s.threeM - s.niftyReturn).toFixed(1);
        const rsClass = s.rs >= 95 ? 'rs-super' : s.rs >= 80 ? 'rs-high' : 'rs-neutral';
        const isLeader = s.rs >= 95;
        const dist52 = (((s.price - s.high52) / s.high52) * 100).toFixed(1);

        cardsHtml += `
          <div class="stock-card ${{isLeader ? 'leader' : ''}}" onclick="openModal('${{s.symbol}}')">
            <div class="card-top">
              <div>
                <span class="ticker-name">${{s.symbol}}</span>
                <span class="cap-badge">${{s.cap}}</span>
                <div class="co-name">${{s.name}}</div>
                <div class="sector-name">${{s.sector}}</div>
              </div>
              <div style="text-align: right;">
                <span class="rs-score-badge ${{rsClass}}">RS ${{s.rs}}</span>
                <div style="font-family: var(--font-mono); font-weight: 800; font-size: 15px; margin-top: 4px;">₹${{s.price.toLocaleString()}}</div>
                <div class="up" style="font-size: 11px; font-weight: 700;">+${{s.chg}}%</div>
              </div>
            </div>

            <div class="gauge-wrapper">
              <div class="gauge-label-row">
                <span>MOMENTUM SCORE</span>
                <span>${{s.rs}} / 99 Percentile</span>
              </div>
              <div class="gauge-bar">
                <div class="gauge-fill" style="width: ${{s.rs}}%;"></div>
              </div>
            </div>

            <div class="metrics-pill-row">
              <div>
                <span style="color: var(--text-tertiary); font-size: 9px; display: block;">3M ALPHA VS N500</span>
                <span class="up">+${{alpha}}% Alpha</span>
              </div>
              <div>
                <span style="color: var(--text-tertiary); font-size: 9px; display: block;">VOL SURGE</span>
                <span style="color: var(--amber); font-weight: 700;">${{s.vol}}x 20DMA</span>
              </div>
              <div>
                <span style="color: var(--text-tertiary); font-size: 9px; display: block;">ANNUAL VOL (SD)</span>
                <span style="font-weight: 700;">${{s.quant.sd}}%</span>
              </div>
            </div>

            <div class="pattern-row">
              ${{s.patterns.slice(0, 3).map(p => `<span class="pattern-tag">${{p}}</span>`).join('')}}
            </div>
          </div>
        `;

        tableHtml += `
          <tr onclick="openModal('${{s.symbol}}')">
            <td style="font-family: var(--font-mono); font-weight: 800; color: var(--cyan-rs);">${{s.symbol}}</td>
            <td style="font-weight: 600;">${{s.name}}</td>
            <td>${{s.sector}}</td>
            <td><span class="cap-badge">${{s.cap}}</span></td>
            <td style="font-family: var(--font-mono); font-weight: 700;">₹${{s.price.toLocaleString()}}</td>
            <td class="up" style="font-family: var(--font-mono); font-weight: 700;">+${{s.chg}}%</td>
            <td><span class="rs-score-badge ${{rsClass}}" style="padding: 2px 8px; font-size: 11px;">RS ${{s.rs}}</span></td>
            <td style="font-family: var(--font-mono); font-size: 11px; color: ${{dist52 >= -5 ? 'var(--green)' : 'var(--text-secondary)'}};">${{dist52}}%</td>
            <td class="up" style="font-family: var(--font-mono); font-weight: 700;">+${{alpha}}%</td>
            <td style="font-family: var(--font-mono); font-weight: 700; color: var(--amber);">${{s.vol}}x</td>
            <td style="font-family: var(--font-mono);">${{s.quant.sd}}%</td>
            <td style="font-family: var(--font-mono);">${{s.quant.corr}}</td>
            <td style="max-width: 280px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--text-secondary); font-size: 11px;">
              <strong>${{s.quarter}}:</strong> ${{s.takeaways[0]}}
            </td>
            <td>
              <button class="theme-btn" style="padding: 4px 8px; font-size: 10px;" onclick="event.stopPropagation(); openModal('${{s.symbol}}')">Dossier</button>
            </td>
          </tr>
        `;
      }});

      grid.innerHTML = cardsHtml;
      tableBody.innerHTML = tableHtml;
    }}

    function filterScreener() {{
      renderScreener();
    }}

    function setPreset(preset, el) {{
      currentPreset = preset;
      document.querySelectorAll("#preset-chips .chip").forEach(c => c.classList.remove("active"));
      el.classList.add("active");
      renderScreener();
    }}

    function setSector(sector, el) {{
      currentSector = sector;
      document.querySelectorAll("#sector-chips .chip").forEach(c => c.classList.remove("active"));
      el.classList.add("active");
      renderScreener();
    }}

    // EXPORT TO CSV
    function exportScreenerCSV() {{
      let csv = "Symbol,Company Name,Sector,Market Cap,Price (INR),Change (%),RS Score,Dist 52W (%),3M Return (%),3M Alpha (%),Vol Surge,Annual Volatility SD (%),Correlation N500,Beta\\n";
      STOCKS.forEach(s => {{
        const alpha = (s.threeM - s.niftyReturn).toFixed(1);
        const dist52 = (((s.price - s.high52) / s.high52) * 100).toFixed(1);
        csv += `"${{s.symbol}}","${{s.name}}","${{s.sector}}","${{s.cap}}",${{s.price}},${{s.chg}},${{s.rs}},${{dist52}},${{s.threeM}},${{alpha}},${{s.vol}},${{s.quant.sd}},${{s.quant.corr}},${{s.quant.beta}}\\n`;
      }});
      const blob = new Blob([csv], {{ type: 'text/csv;charset=utf-8;' }});
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.setAttribute("href", url);
      link.setAttribute("download", `MarketEdge_Nifty500_RS_Screener_${{new Date().toISOString().slice(0,10)}}.csv`);
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    }}

    // RENDER PUBLISHED RESULTS
    function renderResults() {{
      const grid = document.getElementById("results-cards-grid");
      const filtered = RESULTS.filter(r => {{
        if (currentResultFilter === 'ALL') return true;
        return r.verdict === currentResultFilter;
      }});

      let html = '';
      filtered.forEach(r => {{
        const isUpper = r.priceReaction.includes("20");
        const vClass = r.verdict === 'STRONG_BEAT' ? (isUpper ? 'verdict-upper' : 'verdict-strong') : r.verdict === 'IN_LINE' ? 'verdict-inline' : 'verdict-weak';

        html += `
          <div class="result-card ${{isUpper ? 'upper' : ''}}">
            <div style="display: flex; justify-content: space-between; align-items: flex-start;">
              <div>
                <span style="font-family: var(--font-mono); font-size: 16px; font-weight: 800;">${{r.symbol}}</span>
                <span style="font-size: 11px; color: var(--text-secondary); margin-left: 6px;">${{r.name}}</span>
                <div style="font-size: 11px; color: var(--cyan-rs); font-weight: 600;">${{r.sector}}</div>
              </div>
              <span class="verdict-tag ${{vClass}}">${{r.verdictLabel}}</span>
            </div>

            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px; background: var(--card); border-radius: 8px; padding: 10px; font-family: var(--font-mono); font-size: 11px;">
              <div>
                <span style="color: var(--text-tertiary); font-size: 9px; display: block;">REVENUE (Q2 FY26)</span>
                <strong>${{r.rev}}</strong> <span class="up" style="font-size: 10px;">${{r.revYoy}} YoY</span>
              </div>
              <div>
                <span style="color: var(--text-tertiary); font-size: 9px; display: block;">NET PROFIT / PAT</span>
                <strong>${{r.pat}}</strong> <span class="${{r.patYoy.startsWith('+') ? 'up' : 'down'}}" style="font-size: 10px;">${{r.patYoy}} YoY</span>
              </div>
            </div>

            <div style="display: flex; justify-content: space-between; align-items: center; background: var(--card); border-radius: 8px; padding: 8px 12px; font-size: 11px; font-family: var(--font-mono);">
              <span>Price Reaction: <strong class="${{r.priceReaction.startsWith('+') ? 'up' : 'down'}}">${{r.priceReaction}}</strong></span>
              <span>Volume: <strong style="color: var(--amber);">${{r.vol}} 20DMA</strong></span>
            </div>

            <div style="font-size: 12px; color: var(--text-secondary); line-height: 1.5; background: var(--surface); border: 1px solid var(--border); border-radius: 8px; padding: 10px;">
              <strong style="color: var(--text-primary); display: block; font-size: 11px; margin-bottom: 2px;">Market Momentum & Investor Reaction:</strong>
              ${{r.remark}}
            </div>
          </div>
        `;
      }});
      grid.innerHTML = html;
    }}

    function filterResults(filter, el) {{
      currentResultFilter = filter;
      document.querySelectorAll("#verdict-filter-chips .chip").forEach(c => c.classList.remove("active"));
      el.classList.add("active");
      renderResults();
    }}

    // CANDLESTICK CHART ENGINE
    function generateCandles(currentClose, ema21Target, sma50Target, sma200Target, baseRs) {{
      const count = 28;
      const list = [];
      const startPrice = currentClose * 0.85;
      let p = startPrice;

      for (let i = 0; i < count; i++) {{
        const progress = i / (count - 1);
        const dayTrend = (currentClose - startPrice) / count;
        const noise = (Math.random() - 0.45) * (currentClose * 0.02);
        const open = p;
        const close = (i === count - 1) ? currentClose : (p + dayTrend + noise);
        const high = Math.max(open, close) + Math.random() * (currentClose * 0.015);
        const low = Math.min(open, close) - Math.random() * (currentClose * 0.015);
        const vol = Math.floor(500000 + Math.random() * 1500000);
        const ema21 = ema21Target * (0.92 + 0.08 * progress);
        const sma50 = sma50Target * (0.90 + 0.10 * progress);
        const sma200 = sma200Target * (0.88 + 0.12 * progress);
        const rsVal = baseRs * (0.85 + 0.15 * progress);

        list.push({{
          date: `D-${{count - i}}`,
          open, high, low, close, volume: vol,
          ema21, sma50, sma200, rs: rsVal
        }});
        p = close;
      }}
      return list;
    }}

    function drawStockChart(inspectedIndex = null) {{
      const canvas = document.getElementById("candlestick-canvas");
      if (!canvas) return;
      const ctx = canvas.getContext("2d");
      const dpr = window.devicePixelRatio || 1;
      
      const width = canvas.parentElement.clientWidth - 24;
      const height = 280;
      canvas.width = width * dpr;
      canvas.height = height * dpr;
      canvas.style.width = width + "px";
      canvas.style.height = height + "px";
      ctx.scale(dpr, dpr);

      let candles = activeStockCandles;
      if (selectedTimeframe === '1M') candles = candles.slice(-12);
      else if (selectedTimeframe === '3M') candles = candles.slice(-20);

      const n = candles.length;
      if (n === 0) return;

      const isDark = document.body.getAttribute("data-theme") === "dark";
      const greenCol = isDark ? "#00E676" : "#059669";
      const redCol = isDark ? "#FF3B30" : "#DC2626";
      const cyanCol = isDark ? "#00E5FF" : "#0284C7";
      const amberCol = isDark ? "#FFB300" : "#D97706";
      const purpleCol = isDark ? "#A855F7" : "#7C3AED";
      const gridCol = isDark ? "rgba(255,255,255,0.06)" : "rgba(0,0,0,0.06)";
      const textCol = isDark ? "#94A3B8" : "#64748B";

      // Layout heights: Price (60%), Volume (18%), RS Line (22%)
      const priceH = height * 0.60;
      const volTop = priceH + 8;
      const volH = height * 0.16;
      const rsTop = volTop + volH + 8;
      const rsH = height - rsTop - 6;

      let minP = Infinity, maxP = -Infinity;
      let maxVol = 0;
      let minRs = Infinity, maxRs = -Infinity;

      candles.forEach(c => {{
        minP = Math.min(minP, c.low, c.sma200);
        maxP = Math.max(maxP, c.high);
        maxVol = Math.max(maxVol, c.volume);
        minRs = Math.min(minRs, c.rs);
        maxRs = Math.max(maxRs, c.rs);
      }});

      const pRange = (maxP - minP) * 1.08;
      const pZero = maxP + (pRange * 0.04);

      function getY(val) {{
        return ((pZero - val) / pRange) * priceH;
      }}
      function getVolY(vol) {{
        return volTop + volH - (vol / (maxVol || 1)) * volH;
      }}
      function getRsY(val) {{
        const rRange = (maxRs - minRs) || 1;
        return rsTop + rsH - ((val - minRs) / rRange) * rsH;
      }}

      // Background grid
      ctx.strokeStyle = gridCol;
      ctx.lineWidth = 1;
      for (let i = 0; i <= 4; i++) {{
        const gy = (priceH / 4) * i;
        ctx.beginPath(); ctx.moveTo(0, gy); ctx.lineTo(width, gy); ctx.stroke();
      }}

      const slotW = width / n;
      const bodyW = Math.max(4, slotW * 0.65);

      // 1. Draw Candlesticks & Volume
      candles.forEach((c, idx) => {{
        const cx = idx * slotW + slotW / 2;
        const isUp = c.close >= c.open;
        const color = isUp ? greenCol : redCol;

        // Volume bar
        const vy = getVolY(c.volume);
        ctx.fillStyle = color;
        ctx.globalAlpha = 0.35;
        ctx.fillRect(cx - bodyW / 2, vy, bodyW, (volTop + volH) - vy);
        ctx.globalAlpha = 1.0;

        // Wick
        const hy = getY(c.high);
        const ly = getY(c.low);
        ctx.strokeStyle = color;
        ctx.lineWidth = 1.2;
        ctx.beginPath(); ctx.moveTo(cx, hy); ctx.lineTo(cx, ly); ctx.stroke();

        // Body
        const oy = getY(c.open);
        const cy = getY(c.close);
        const topY = Math.min(oy, cy);
        const bH = Math.max(2, Math.abs(cy - oy));
        ctx.fillStyle = color;
        ctx.fillRect(cx - bodyW / 2, topY, bodyW, bH);
      }});

      // 2. Draw Moving Averages
      function drawLine(key, color, widthLine) {{
        ctx.strokeStyle = color;
        ctx.lineWidth = widthLine;
        ctx.beginPath();
        candles.forEach((c, idx) => {{
          const cx = idx * slotW + slotW / 2;
          const cy = getY(c[key]);
          if (idx === 0) ctx.moveTo(cx, cy);
          else ctx.lineTo(cx, cy);
        }});
        ctx.stroke();
      }}

      drawLine('ema21', cyanCol, 1.8);
      drawLine('sma50', amberCol, 1.6);
      drawLine('sma200', purpleCol, 1.4);

      // 3. Draw RS Line in bottom pane
      ctx.strokeStyle = cyanCol;
      ctx.lineWidth = 1.5;
      ctx.beginPath();
      candles.forEach((c, idx) => {{
        const cx = idx * slotW + slotW / 2;
        const ry = getRsY(c.rs);
        if (idx === 0) ctx.moveTo(cx, ry);
        else ctx.lineTo(cx, ry);
      }});
      ctx.stroke();

      // Panes label
      ctx.fillStyle = textCol;
      ctx.font = "9px 'JetBrains Mono', monospace";
      ctx.fillText("RS vs NIFTY 500", 6, rsTop + 10);
      ctx.fillText("VOL", 6, volTop + 10);

      // 4. Crosshair inspection
      if (inspectedIndex !== null && inspectedIndex >= 0 && inspectedIndex < n) {{
        const ic = candles[inspectedIndex];
        const ix = inspectedIndex * slotW + slotW / 2;
        
        ctx.strokeStyle = "rgba(0, 229, 255, 0.5)";
        ctx.setLineDash([3, 3]);
        ctx.beginPath(); ctx.moveTo(ix, 0); ctx.lineTo(ix, height); ctx.stroke();
        ctx.setLineDash([]);

        // Update Legend with inspected candle
        const legend = document.getElementById("chart-inspection-info");
        if (legend) {{
          legend.innerHTML = `
            <span>O: ₹${{ic.open.toFixed(1)}}</span>
            <span>H: ₹${{ic.high.toFixed(1)}}</span>
            <span>L: ₹${{ic.low.toFixed(1)}}</span>
            <span class="${{ic.close >= ic.open ? 'up' : 'down'}}">C: ₹${{ic.close.toFixed(1)}}</span>
            <span style="color: var(--cyan-rs)">RS: ${{ic.rs.toFixed(0)}}</span>
            <span style="color: var(--amber)">Vol: ${{(ic.volume/100000).toFixed(1)}}L</span>
          `;
        }}
      }}
    }}

    function setTimeframe(tf, el) {{
      selectedTimeframe = tf;
      document.querySelectorAll(".tf-btn").forEach(b => b.classList.remove("active"));
      el.classList.add("active");
      drawStockChart();
    }}

    // OPEN STOCK MODAL WITH INTERACTIVE CHART & AAYA STATS
    function openModal(symbol) {{
      const stock = STOCKS.find(s => s.symbol === symbol);
      if (!stock) return;

      activeStockSymbol = symbol;
      activeStockCandles = generateCandles(stock.price, stock.ema21, stock.sma50, stock.sma200, stock.rs);

      const body = document.getElementById("modal-body");
      const alpha = (stock.threeM - stock.niftyReturn).toFixed(1);
      const dist52 = (((stock.price - stock.high52) / stock.high52) * 100).toFixed(1);
      const rangePercent = Math.min(100, Math.max(0, ((stock.price - stock.low52) / (stock.high52 - stock.low52)) * 100));

      body.innerHTML = `
        <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 14px;">
          <div>
            <div style="display: flex; align-items: center; gap: 8px;">
              <h2 style="font-family: var(--font-mono); font-size: 26px; font-weight: 800; color: var(--text-primary);">${{stock.symbol}}</h2>
              <span class="rs-score-badge ${{stock.rs >= 95 ? 'rs-super' : 'rs-high'}}">RS ${{stock.rs}}</span>
              <span class="cap-badge">${{stock.cap}}</span>
            </div>
            <div style="font-size: 13px; color: var(--text-secondary); margin-top: 2px;">${{stock.name}} • ${{stock.sector}}</div>
          </div>
          <div style="text-align: right;">
            <div style="font-size: 24px; font-weight: 800; font-family: var(--font-mono);">₹${{stock.price.toLocaleString()}}</div>
            <div class="up" style="font-weight: 700; font-size: 13px;">+${{stock.chg}}% Today • ${{dist52}}% from 52W High</div>
          </div>
        </div>

        <!-- 52-WEEK RANGE BAR -->
        <div style="background: var(--card); border: 1px solid var(--border); border-radius: 8px; padding: 10px 14px; margin-bottom: 12px;">
          <div style="display: flex; justify-content: space-between; font-size: 11px; font-family: var(--font-mono); color: var(--text-secondary);">
            <span>52W Low: ₹${{stock.low52.toLocaleString()}}</span>
            <span style="color: var(--text-primary); font-weight: 700;">Current: ₹${{stock.price.toLocaleString()}} (${{rangePercent.toFixed(0)}}% of range)</span>
            <span>52W High: ₹${{stock.high52.toLocaleString()}}</span>
          </div>
          <div class="range52-bar">
            <div class="range52-fill" style="width: ${{rangePercent}}%;"></div>
            <div class="range52-thumb" style="left: ${{rangePercent}}%;"></div>
          </div>
        </div>

        <!-- INTERACTIVE CANDLESTICK CHART WITH MOVING AVERAGES -->
        <div class="chart-box">
          <div class="chart-legend">
            <div id="chart-inspection-info" style="display: flex; gap: 10px; font-size: 11px;">
              <span style="color: var(--cyan-rs)">21 EMA (Cyan)</span>
              <span style="color: var(--amber)">50 SMA (Amber)</span>
              <span style="color: var(--purple)">200 SMA (Purple)</span>
            </div>
            <div style="display: flex; gap: 4px;">
              <button class="tf-btn" onclick="setTimeframe('1M', this)">1M</button>
              <button class="tf-btn active" onclick="setTimeframe('3M', this)">3M</button>
              <button class="tf-btn" onclick="setTimeframe('6M', this)">6M</button>
            </div>
          </div>
          <canvas id="candlestick-canvas" style="display: block; cursor: crosshair;"></canvas>
        </div>

        <!-- AAYA.IN STYLE QUANTITATIVE STATISTICS MATRIX -->
        <div style="margin-bottom: 14px;">
          <h4 style="font-size: 11px; color: var(--cyan-rs); text-transform: uppercase; font-weight: 800; letter-spacing: 0.5px; margin-bottom: 6px;">Quantitative Risk & Alpha Statistics (Aaya.in Model)</h4>
          <div class="stat-badge-grid">
            <div class="stat-box">
              <div class="stat-box-title">Annualized Vol (SD)</div>
              <div class="stat-box-val">${{stock.quant.sd}}%</div>
            </div>
            <div class="stat-box">
              <div class="stat-box-title">Correlation vs N500</div>
              <div class="stat-box-val">${{stock.quant.corr}}</div>
            </div>
            <div class="stat-box">
              <div class="stat-box-title">Beta vs N500</div>
              <div class="stat-box-val">${{stock.quant.beta}}</div>
            </div>
            <div class="stat-box">
              <div class="stat-box-title">Jensen's Alpha</div>
              <div class="stat-box-val up">+${{stock.quant.alpha}}%</div>
            </div>
            <div class="stat-box">
              <div class="stat-box-title">Sharpe Ratio</div>
              <div class="stat-box-val up">${{stock.quant.sharpe}}</div>
            </div>
            <div class="stat-box">
              <div class="stat-box-title">Sortino Ratio</div>
              <div class="stat-box-val up">${{stock.quant.sortino}}</div>
            </div>
          </div>
        </div>

        <!-- TECHNICAL ANALYSIS STACK -->
        <div style="background: var(--card); border-radius: 8px; border: 1px solid var(--border); padding: 12px 14px; margin-bottom: 14px;">
          <h4 style="font-size: 11px; color: var(--cyan-rs); text-transform: uppercase; font-weight: 700; margin-bottom: 4px;">Technical Moving Average Alignment</h4>
          <p style="font-size: 12px; font-family: var(--font-mono); color: var(--text-primary);">${{stock.maStack}}</p>
        </div>

        <!-- Q2 FY26 CONCALL & MANAGEMENT HIGHLIGHTS -->
        <div style="background: var(--card); border-radius: 8px; border: 1px solid var(--border); padding: 14px; margin-bottom: 14px;">
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
            <h4 style="font-size: 11px; color: var(--green); text-transform: uppercase; font-weight: 800; letter-spacing: 0.5px;">Concall Key Highlights (${{stock.quarter}} • ${{stock.concallDate}})</h4>
            <span style="font-size: 9px; font-weight: 700; color: var(--green); background: var(--green-bg); padding: 2px 6px; border-radius: 4px;">AUDITED REPORT</span>
          </div>
          <ul style="padding-left: 18px; font-size: 12px; color: var(--text-secondary); line-height: 1.6;">
            ${{stock.takeaways.map(t => `<li style="margin-bottom: 4px;">${{t}}</li>`).join('')}}
          </ul>

          <div style="margin-top: 10px; padding-top: 8px; border-top: 1px solid var(--border-subtle);">
            <div style="font-size: 11px; font-weight: 700; color: var(--green);">MANAGEMENT GUIDANCE (FY26/FY27):</div>
            <div style="font-size: 11px; color: var(--text-primary); margin-top: 2px;">${{stock.guidance}}</div>
          </div>

          <div style="margin-top: 8px;">
            <div style="font-size: 11px; font-weight: 700; color: var(--amber);">CAPEX & EXPANSION PIPELINE:</div>
            <div style="font-size: 11px; color: var(--text-primary); margin-top: 2px;">${{stock.capex}}</div>
          </div>
        </div>

        <!-- DIRECT ONE-CLICK EXTERNAL LINKS -->
        <div style="display: flex; gap: 10px; margin-top: 16px; flex-wrap: wrap;">
          <a href="https://www.screener.in/company/${{stock.symbol}}/consolidated/" target="_blank" class="theme-btn" style="text-decoration:none;">📄 Open on Screener.in</a>
          <a href="https://www.nseindia.com/get-quotes/equity?symbol=${{stock.symbol}}" target="_blank" class="theme-btn" style="text-decoration:none;">🏛️ Official NSE Quote</a>
          <a href="https://www.bseindia.com/corporates/ann.html" target="_blank" class="theme-btn" style="text-decoration:none;">📑 BSE Filings & Transcripts</a>
        </div>
      `;

      document.getElementById("stock-modal").classList.add("active");

      // Initialize interactive canvas chart
      setTimeout(() => {{
        drawStockChart();
        const canvas = document.getElementById("candlestick-canvas");
        if (canvas) {{
          canvas.addEventListener("mousemove", (e) => {{
            const rect = canvas.getBoundingClientRect();
            const mouseX = e.clientX - rect.left;
            let candles = activeStockCandles;
            if (selectedTimeframe === '1M') candles = candles.slice(-12);
            else if (selectedTimeframe === '3M') candles = candles.slice(-20);
            const slotW = rect.width / candles.length;
            const idx = Math.floor(mouseX / slotW);
            drawStockChart(idx);
          }});
          canvas.addEventListener("mouseleave", () => {{
            drawStockChart(null);
          }});
        }}
      }}, 50);
    }}

    function closeModal(e) {{
      if (e.target.id === "stock-modal") closeModalDirect();
    }}
    function closeModalDirect() {{
      document.getElementById("stock-modal").classList.remove("active");
      activeStockSymbol = '';
    }}

    // INDICES MATRIX
    function renderIndices() {{
      const container = document.getElementById("indices-grid");
      let html = '';
      INDICES.forEach(idx => {{
        html += `
          <div class="kpi-card">
            <div class="kpi-title">${{idx.name}}</div>
            <div class="kpi-val">${{idx.price}}</div>
            <div class="${{idx.chg.startsWith('+') ? 'up' : 'down'}}" style="font-size: 12px; font-weight: 700; font-family: var(--font-mono);">${{idx.chg}}</div>
            <div style="font-size: 10px; color: var(--cyan-rs); margin-top: 6px;">52W Range: ${{idx.low}} - ${{idx.high}}</div>
            <div style="font-size: 10px; color: var(--text-tertiary); margin-top: 2px;">Trend: ${{idx.trend}}</div>
            <div style="font-size: 10px; color: var(--text-secondary); margin-top: 4px; font-family: var(--font-mono);">${{idx.ma}}</div>
          </div>
        `;
      }});
      container.innerHTML = html;
    }}

    // EARNINGS RADAR
    function renderEarnings() {{
      const container = document.getElementById("earnings-list");
      let html = '';
      UPCOMING_EARNINGS.forEach(e => {{
        html += `
          <div class="bullet-card">
            <div style="display: flex; justify-content: space-between; align-items: center;">
              <div>
                <span style="font-family: var(--font-mono); font-weight: 800; font-size: 15px;">${{e.symbol}}</span>
                <span style="font-size: 11px; color: var(--text-secondary); margin-left: 6px;">${{e.name}}</span>
              </div>
              <span style="color: var(--amber); font-weight: 700; font-size: 12px;">Vol: ${{e.vol}} (${{e.move}})</span>
            </div>
            <div style="font-size: 11px; color: var(--cyan-rs); margin: 4px 0;">Expected Date: ${{e.date}} • Estimated PAT: ${{e.pat}}</div>
            <div style="font-size: 12px; color: var(--text-secondary);">${{e.focus}}</div>
          </div>
        `;
      }});
      container.innerHTML = html;
    }}

    // THESES NOTES (LOCALSTORAGE)
    function saveThesis() {{
      const sym = document.getElementById("thesis-symbol").value.trim().toUpperCase();
      const target = document.getElementById("thesis-target").value.trim();
      const text = document.getElementById("thesis-text").value.trim();
      if (!sym || !text) return alert("Please enter symbol and thesis text.");

      const saved = JSON.parse(localStorage.getItem("marketedge_theses") || "[]");
      saved.unshift({{ symbol: sym, target, text, date: new Date().toLocaleDateString('en-GB') }});
      localStorage.setItem("marketedge_theses", JSON.stringify(saved));

      document.getElementById("thesis-symbol").value = "";
      document.getElementById("thesis-target").value = "";
      document.getElementById("thesis-text").value = "";
      renderTheses();
    }}

    function deleteThesis(idx) {{
      const saved = JSON.parse(localStorage.getItem("marketedge_theses") || "[]");
      saved.splice(idx, 1);
      localStorage.setItem("marketedge_theses", JSON.stringify(saved));
      renderTheses();
    }}

    function renderTheses() {{
      const container = document.getElementById("theses-cards-container");
      const saved = JSON.parse(localStorage.getItem("marketedge_theses") || "[]");
      if (saved.length === 0) {{
        container.innerHTML = `<div style="color: var(--text-tertiary); font-size: 12px; padding: 12px 0;">No investment notes saved yet. Add your first stock thesis above!</div>`;
        return;
      }}
      let html = '';
      saved.forEach((t, i) => {{
        html += `
          <div class="bullet-card">
            <div style="display: flex; justify-content: space-between; align-items: center;">
              <div>
                <span style="font-family: var(--font-mono); font-weight: 800; font-size: 15px; color: var(--cyan-rs);">${{t.symbol}}</span>
                <span style="font-size: 11px; color: var(--text-tertiary); margin-left: 8px;">${{t.date}}</span>
              </div>
              <button class="theme-btn" style="padding: 2px 8px; font-size: 10px;" onclick="deleteThesis(${{i}})">Delete</button>
            </div>
            ${{t.target ? `<div style="font-size: 11px; color: var(--green); font-weight: 700; margin: 4px 0;">Target Price: ₹${{t.target}}</div>` : ''}}
            <div style="font-size: 12px; color: var(--text-secondary); line-height: 1.5; margin-top: 4px;">${{t.text}}</div>
          </div>
        `;
      }});
      container.innerHTML = html;
    }}
  </script>
</body>
</html>'''

with open("web/index.html", "w") as f:
    f.write(html_template)

with open("app/src/main/assets/web/index.html", "w") as f:
    f.write(html_template)

print("web/index.html generated and synced to assets successfully!")
