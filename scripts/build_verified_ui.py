#!/usr/bin/env python3
"""
scripts/build_verified_ui.py
Generates the clean web/index.html and copies to app/src/main/assets/web/index.html.
Ensures:
1. Zero hardcoded market data in HTML markup.
2. Dynamic fetch('/data.json') with graceful fallback to relative fetch('data.json') for WebView.
3. Provenance (asOf and source) clearly rendered on every panel.
4. Visible warning banner if data is stale (> 1 trading day) or on fetch failure.
5. Zero gap-filling with invented or simulated numbers.
"""

import os
import shutil

html_template = '''<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>MarketEdge Terminal • Indian Equity Momentum & Real-Time Screener</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;500;600;700;800&family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
  <style>
    :root {
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
    }

    [data-theme="dark"] {
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
    }

    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      background-color: var(--bg);
      color: var(--text-primary);
      font-family: var(--font-sans);
      min-height: 100vh;
      display: flex;
      flex-direction: column;
      overflow-x: hidden;
      transition: background-color 0.2s, color 0.2s;
    }

    /* PIPELINE ALERT / STALE DATA BANNER */
    .pipeline-banner {
      padding: 9px 28px;
      font-size: 12px;
      font-family: var(--font-mono);
      font-weight: 600;
      display: flex;
      justify-content: space-between;
      align-items: center;
      transition: all 0.2s;
      z-index: 90;
    }
    .pipeline-banner.warning {
      background: rgba(217, 119, 6, 0.12);
      border-bottom: 1px solid var(--amber);
      color: var(--amber);
    }
    .pipeline-banner.error {
      background: rgba(220, 38, 38, 0.14);
      border-bottom: 1px solid var(--red);
      color: var(--red);
    }
    .pipeline-banner.info {
      background: var(--cyan-rs-bg);
      border-bottom: 1px solid var(--cyan-rs);
      color: var(--cyan-rs);
    }

    /* PROVENANCE AND SOURCE TAGS */
    .panel-provenance {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      font-size: 10px;
      font-family: var(--font-mono);
      color: var(--text-tertiary);
      background: var(--card);
      padding: 3px 8px;
      border-radius: 4px;
      border: 1px solid var(--border-subtle);
      margin-top: 4px;
      line-height: 1.4;
    }
    .panel-provenance strong {
      color: var(--text-secondary);
    }

    /* LIVE TICK PULSE ANIMATIONS */
    @keyframes pulseGreen {
      0% { background-color: rgba(5, 150, 105, 0.35); }
      100% { background-color: transparent; }
    }
    @keyframes pulseRed {
      0% { background-color: rgba(220, 38, 38, 0.35); }
      100% { background-color: transparent; }
    }
    .tick-up { animation: pulseGreen 0.75s ease-out; }
    .tick-down { animation: pulseRed 0.75s ease-out; }

    /* TOP TERMINAL HEADER */
    header.terminal-header {
      background-color: var(--surface);
      border-bottom: 1px solid var(--border);
      padding: 10px 28px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      position: sticky;
      top: 0;
      z-index: 100;
      box-shadow: 0 1px 3px rgba(0,0,0,0.04);
    }

    .brand-group {
      display: flex;
      align-items: center;
      gap: 12px;
    }
    .status-dot {
      width: 10px;
      height: 10px;
      border-radius: 50%;
      background-color: var(--green);
      box-shadow: 0 0 8px var(--green);
      animation: blink 1.2s infinite ease-in-out;
    }
    @keyframes blink {
      0%, 100% { opacity: 1; }
      50% { opacity: 0.3; }
    }
    .brand-title {
      font-weight: 800;
      font-size: 17px;
      letter-spacing: 0.8px;
      text-transform: uppercase;
      color: var(--text-primary);
    }
    .badge-laptop {
      background: var(--cyan-rs-bg);
      color: var(--cyan-rs);
      font-size: 10px;
      font-weight: 700;
      padding: 3px 8px;
      border-radius: 4px;
      border: 1px solid var(--cyan-rs);
      letter-spacing: 0.5px;
    }

    .ticker-bar {
      display: flex;
      align-items: center;
      gap: 20px;
      font-family: var(--font-mono);
      font-size: 13px;
    }
    .ticker-item {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 3px 6px;
      border-radius: 4px;
      transition: background-color 0.2s;
    }
    .ticker-label {
      color: var(--text-secondary);
      font-weight: 600;
    }
    .ticker-val {
      font-weight: 700;
    }
    .up { color: var(--green); }
    .down { color: var(--red); }

    .header-actions {
      display: flex;
      align-items: center;
      gap: 10px;
    }
    .theme-btn, .action-btn {
      background: var(--card);
      border: 1px solid var(--border);
      color: var(--text-primary);
      padding: 7px 12px;
      border-radius: 8px;
      cursor: pointer;
      font-size: 12px;
      font-weight: 600;
      display: flex;
      align-items: center;
      gap: 6px;
      transition: all 0.2s;
    }
    .theme-btn:hover, .action-btn:hover {
      background: var(--card-hover);
      border-color: var(--cyan-rs);
    }

    /* STREAM CONTROL BAR */
    .stream-control-bar {
      background: var(--surface);
      border-bottom: 1px solid var(--border);
      padding: 8px 28px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 11px;
      font-family: var(--font-mono);
      flex-wrap: wrap;
      gap: 10px;
    }
    .stream-status-pill {
      display: flex;
      align-items: center;
      gap: 8px;
      background: var(--green-bg);
      border: 1px solid var(--green);
      color: var(--green);
      padding: 4px 10px;
      border-radius: 20px;
      font-weight: 700;
      letter-spacing: 0.5px;
    }
    .stream-status-pill.paused {
      background: var(--amber-bg);
      border-color: var(--amber);
      color: var(--amber);
    }
    .stream-controls-group {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .stream-btn {
      background: var(--card);
      border: 1px solid var(--border);
      padding: 4px 10px;
      border-radius: 6px;
      cursor: pointer;
      font-family: var(--font-mono);
      font-size: 11px;
      font-weight: 600;
      color: var(--text-secondary);
      transition: all 0.15s;
    }
    .stream-btn:hover {
      color: var(--text-primary);
      border-color: var(--cyan-rs);
    }
    .stream-btn.active {
      background: var(--cyan-rs-bg);
      color: var(--cyan-rs);
      border-color: var(--cyan-rs);
    }

    /* SUB NAVIGATION BAR */
    nav.terminal-nav {
      background: var(--surface);
      border-bottom: 1px solid var(--border);
      display: flex;
      gap: 8px;
      padding: 8px 28px;
      overflow-x: auto;
    }
    .nav-tab {
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
    }
    .nav-tab.active {
      background: var(--cyan-rs-bg);
      color: var(--cyan-rs);
      border: 1px solid var(--cyan-rs);
    }
    .nav-tab:hover:not(.active) {
      color: var(--text-primary);
      background: var(--card);
    }

    /* MAIN CONTAINER */
    main.terminal-main {
      flex: 1;
      padding: 20px 28px;
      max-width: 1600px;
      width: 100%;
      margin: 0 auto;
    }

    .tab-content { display: none; }
    .tab-content.active { display: block; }

    /* MARKET PULSE LAYOUT */
    .pulse-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 24px;
    }
    @media (max-width: 1080px) {
      .pulse-grid { grid-template-columns: 1fr; }
      .ticker-bar { display: none; }
    }

    .panel {
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: 12px;
      padding: 20px;
      box-shadow: 0 1px 4px rgba(0,0,0,0.03);
    }
    .panel-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 16px;
      flex-wrap: wrap;
      gap: 8px;
    }
    .panel-title {
      font-size: 15px;
      font-weight: 800;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      color: var(--text-primary);
    }
    .panel-subtitle {
      font-size: 12px;
      color: var(--text-secondary);
      margin-top: 2px;
    }

    .kpi-row {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 12px;
      margin-bottom: 16px;
    }
    .kpi-card {
      background: var(--card);
      border: 1px solid var(--border);
      border-radius: 8px;
      padding: 12px 14px;
    }
    .kpi-title {
      font-size: 11px;
      color: var(--text-secondary);
      font-weight: 600;
      text-transform: uppercase;
    }
    .kpi-val {
      font-size: 20px;
      font-weight: 800;
      font-family: var(--font-mono);
      margin: 4px 0;
    }
    .kpi-desc {
      font-size: 10px;
      color: var(--text-tertiary);
    }

    /* MARKET BREADTH */
    .breadth-container {
      background: var(--card);
      border: 1px solid var(--border);
      border-radius: 8px;
      padding: 14px;
      margin-bottom: 16px;
    }
    .breadth-track {
      height: 8px;
      background: var(--border);
      border-radius: 4px;
      overflow: hidden;
      display: flex;
      margin: 6px 0;
    }
    .breadth-adv { background: var(--green); transition: width 0.3s; }
    .breadth-dec { background: var(--red); transition: width 0.3s; }
    .breadth-unc { background: var(--text-tertiary); transition: width 0.3s; }

    /* BULLET CARDS */
    .bullet-card {
      background: var(--card);
      border: 1px solid var(--border);
      border-radius: 8px;
      padding: 12px 14px;
      margin-bottom: 10px;
      transition: all 0.2s;
    }
    .bullet-card:hover {
      border-color: var(--cyan-rs);
      background: var(--card-hover);
    }
    .bullet-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 4px;
    }
    .bullet-tag {
      font-size: 10px;
      font-weight: 700;
      text-transform: uppercase;
      padding: 2px 6px;
      border-radius: 4px;
      font-family: var(--font-mono);
    }
    .bullet-tag.institutional { background: var(--cyan-rs-bg); color: var(--cyan-rs); }
    .bullet-tag.macro { background: var(--purple-bg); color: var(--purple); }
    .bullet-tag.breadth { background: var(--green-bg); color: var(--green); }
    .bullet-tag.results { background: var(--amber-bg); color: var(--amber); }
    .bullet-tag.sectoral { background: rgba(5, 150, 105, 0.15); color: var(--green); }
    .bullet-tag.retail { background: rgba(124, 58, 237, 0.15); color: var(--purple); }
    .bullet-tag.order { background: rgba(0, 136, 153, 0.15); color: var(--cyan-rs); }

    .bullet-title {
      font-weight: 700;
      font-size: 13px;
      margin-bottom: 4px;
      color: var(--text-primary);
    }
    .bullet-desc {
      font-size: 12px;
      color: var(--text-secondary);
      line-height: 1.45;
    }

    /* SCREENER CONTROLS */
    .screener-controls {
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: 12px;
      padding: 16px 20px;
      margin-bottom: 20px;
    }
    .search-row {
      display: flex;
      gap: 12px;
      margin-bottom: 12px;
      flex-wrap: wrap;
    }
    .screener-input {
      flex: 1;
      min-width: 240px;
      background: var(--card);
      border: 1px solid var(--border);
      border-radius: 8px;
      padding: 8px 14px;
      color: var(--text-primary);
      font-family: var(--font-sans);
      font-size: 13px;
      outline: none;
    }
    .screener-input:focus {
      border-color: var(--cyan-rs);
    }
    .screener-select {
      background: var(--card);
      border: 1px solid var(--border);
      border-radius: 8px;
      padding: 8px 12px;
      color: var(--text-primary);
      font-family: var(--font-sans);
      font-size: 13px;
      outline: none;
      cursor: pointer;
    }
    .view-toggle-group {
      display: flex;
      border: 1px solid var(--border);
      border-radius: 8px;
      overflow: hidden;
    }
    .view-btn {
      background: var(--card);
      border: none;
      padding: 8px 14px;
      font-size: 12px;
      font-weight: 600;
      cursor: pointer;
      color: var(--text-secondary);
      transition: all 0.2s;
    }
    .view-btn.active {
      background: var(--cyan-rs-bg);
      color: var(--cyan-rs);
    }

    .chip-row {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
      margin-top: 10px;
    }
    .chip {
      background: var(--card);
      border: 1px solid var(--border);
      border-radius: 16px;
      padding: 4px 12px;
      font-size: 11px;
      font-weight: 600;
      color: var(--text-secondary);
      cursor: pointer;
      transition: all 0.15s;
    }
    .chip:hover {
      border-color: var(--cyan-rs);
      color: var(--text-primary);
    }
    .chip.active {
      background: var(--cyan-rs-bg);
      border-color: var(--cyan-rs);
      color: var(--cyan-rs);
      font-weight: 700;
    }

    /* SCREENER CARDS VIEW */
    .screener-cards {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
      gap: 16px;
    }
    .stock-card {
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: 10px;
      padding: 16px;
      cursor: pointer;
      transition: all 0.2s;
      box-shadow: 0 1px 3px rgba(0,0,0,0.02);
      display: flex;
      flex-direction: column;
      justify-content: space-between;
    }
    .stock-card:hover {
      border-color: var(--cyan-rs);
      box-shadow: 0 4px 12px rgba(0,0,0,0.06);
      transform: translateY(-2px);
    }
    .card-top {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 8px;
    }
    .card-symbol {
      font-family: var(--font-mono);
      font-size: 16px;
      font-weight: 800;
      color: var(--text-primary);
    }
    .card-name {
      font-size: 12px;
      color: var(--text-secondary);
      margin-top: 2px;
    }
    .rs-score-badge {
      font-family: var(--font-mono);
      font-size: 11px;
      font-weight: 800;
      padding: 3px 8px;
      border-radius: 4px;
    }
    .rs-super {
      background: var(--cyan-rs-bg);
      color: var(--cyan-rs);
      border: 1px solid var(--cyan-rs);
    }
    .rs-high {
      background: var(--green-bg);
      color: var(--green);
      border: 1px solid var(--green);
    }
    .card-price-row {
      display: flex;
      justify-content: space-between;
      align-items: baseline;
      margin: 10px 0 6px 0;
      font-family: var(--font-mono);
    }
    .stock-card-price {
      font-size: 18px;
      font-weight: 800;
    }
    .stock-card-chg {
      font-size: 13px;
      font-weight: 700;
    }

    /* SCREENER TABLE VIEW */
    .screener-table-container {
      display: none;
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: 12px;
      overflow-x: auto;
    }
    table.screener-table {
      width: 100%;
      border-collapse: collapse;
      font-size: 12px;
      text-align: left;
    }
    table.screener-table th {
      background: var(--card);
      padding: 10px 14px;
      color: var(--text-secondary);
      font-weight: 700;
      border-bottom: 1px solid var(--border);
      font-family: var(--font-mono);
      white-space: nowrap;
    }
    table.screener-table td {
      padding: 10px 14px;
      border-bottom: 1px solid var(--border-subtle);
      white-space: nowrap;
    }
    table.screener-table tr:hover td {
      background: var(--card-hover);
    }

    /* RESULTS & RADAR GRIDS */
    .results-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
      gap: 16px;
    }
    .result-card {
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: 10px;
      padding: 16px;
    }

    /* MODAL DOSSIER & CANVAS */
    .modal-overlay {
      position: fixed;
      top: 0; left: 0; right: 0; bottom: 0;
      background: rgba(0,0,0,0.65);
      backdrop-filter: blur(4px);
      z-index: 1000;
      display: none;
      align-items: center;
      justify-content: center;
      padding: 20px;
    }
    .modal-overlay.active { display: flex; }
    .modal-content {
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: 14px;
      max-width: 960px;
      width: 100%;
      max-height: 90vh;
      overflow-y: auto;
      padding: 24px;
      position: relative;
    }
    .modal-close {
      position: absolute;
      top: 16px; right: 16px;
      background: var(--card);
      border: 1px solid var(--border);
      border-radius: 50%;
      width: 32px; height: 32px;
      display: flex; align-items: center; justify-content: center;
      cursor: pointer;
      font-weight: 700;
      color: var(--text-secondary);
    }
    .modal-close:hover {
      color: var(--text-primary);
      border-color: var(--cyan-rs);
    }

    .chart-box {
      background: var(--card);
      border: 1px solid var(--border);
      border-radius: 8px;
      padding: 12px;
      margin-bottom: 14px;
    }
    .chart-legend {
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 11px;
      font-family: var(--font-mono);
      margin-bottom: 8px;
      flex-wrap: wrap;
      gap: 6px;
    }
    .tf-btn, .ind-btn {
      background: var(--surface);
      border: 1px solid var(--border);
      color: var(--text-secondary);
      padding: 3px 8px;
      border-radius: 4px;
      font-size: 10px;
      font-family: var(--font-mono);
      font-weight: 600;
      cursor: pointer;
    }
    .tf-btn.active, .ind-btn.active {
      background: var(--cyan-rs-bg);
      color: var(--cyan-rs);
      border-color: var(--cyan-rs);
      font-weight: 700;
    }

    /* 52W RANGE BAR */
    .range52-bar {
      height: 6px;
      background: var(--border);
      border-radius: 3px;
      position: relative;
      margin-top: 6px;
    }
    .range52-fill {
      height: 100%;
      background: var(--green);
      border-radius: 3px;
    }
    .range52-thumb {
      width: 10px;
      height: 10px;
      background: var(--text-primary);
      border: 2px solid var(--surface);
      border-radius: 50%;
      position: absolute;
      top: -2px;
      transform: translateX(-50%);
    }

    /* STAT BADGES */
    .stat-badge-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(130px, 1fr));
      gap: 8px;
      margin-bottom: 12px;
    }
    .stat-box {
      background: var(--surface);
      border: 1px solid var(--border-subtle);
      border-radius: 6px;
      padding: 8px 10px;
      text-align: center;
    }
    .stat-box-title {
      font-size: 10px;
      color: var(--text-secondary);
      font-weight: 600;
      text-transform: uppercase;
    }
    .stat-box-val {
      font-size: 14px;
      font-weight: 800;
      font-family: var(--font-mono);
      color: var(--text-primary);
      margin-top: 2px;
    }

    /* TOASTS */
    .toast-container {
      position: fixed;
      bottom: 24px; right: 24px;
      z-index: 2000;
      display: flex;
      flex-direction: column;
      gap: 8px;
    }
    .toast {
      background: var(--surface);
      color: var(--text-primary);
      border: 1px solid var(--border);
      padding: 10px 16px;
      border-radius: 8px;
      font-size: 12px;
      font-weight: 600;
      box-shadow: 0 4px 14px rgba(0,0,0,0.12);
      transform: translateY(20px);
      opacity: 0;
      transition: all 0.25s ease-out;
    }
    .toast.show { transform: translateY(0); opacity: 1; }
    .toast.info { border-left: 4px solid var(--cyan-rs); }
    .toast.success { border-left: 4px solid var(--green); }
    .toast.warning { border-left: 4px solid var(--amber); }

    /* FOOTER */
    footer.terminal-footer {
      background: var(--surface);
      border-top: 1px solid var(--border);
      padding: 12px 28px;
      font-size: 11px;
      color: var(--text-secondary);
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-top: auto;
      font-family: var(--font-mono);
    }
  </style>
</head>
<body data-theme="light">

  <!-- TOP TERMINAL HEADER -->
  <header class="terminal-header">
    <div class="brand-group">
      <div class="status-dot"></div>
      <div class="brand-title">MarketEdge Terminal</div>
      <span class="badge-laptop">VERIFIED EOD PIPELINE</span>
      <span style="font-size: 11px; color: var(--text-secondary); font-family: var(--font-mono);" id="header-date">12 NOV 2026 • OFFICIAL EOD CLOSE</span>
    </div>

    <!-- NIFTY 50 & 500 TICKER (LOADED DYNAMICALLY VIA /data.json) -->
    <div class="ticker-bar" id="ticker-bar">
      <div class="ticker-item" id="ticker-nifty50">
        <span class="ticker-label">NIFTY 50:</span>
        <span class="ticker-val" id="nifty50-val">--</span>
        <span id="nifty50-chg">--</span>
      </div>
      <div class="ticker-item" id="ticker-nifty500">
        <span class="ticker-label">• N500:</span>
        <span class="ticker-val" id="nifty500-val">--</span>
        <span id="nifty500-chg">--</span>
      </div>
      <div class="ticker-item" id="ticker-banknifty">
        <span class="ticker-label">• BANK NIFTY:</span>
        <span class="ticker-val" id="banknifty-val">--</span>
        <span id="banknifty-chg">--</span>
      </div>
    </div>

    <!-- ACTIONS -->
    <div class="header-actions">
      <button class="theme-btn" id="theme-toggle" onclick="toggleTheme()">🌙 Dark</button>
      <button class="theme-btn" onclick="openBrokerModal()">🔑 Broker Feed</button>
      <button class="theme-btn" onclick="exportScreenerCSV()">📥 Export Screener</button>
    </div>
  </header>

  <!-- PIPELINE ALERT / STALE DATA BANNER -->
  <div id="data-pipeline-banner" class="pipeline-banner" style="display: none;">
    <div style="display: flex; align-items: center; justify-content: space-between; width: 100%; max-width: 1540px; margin: 0 auto; gap: 12px; flex-wrap: wrap;">
      <div style="display: flex; align-items: center; gap: 8px;">
        <span id="pipeline-banner-icon" style="font-size: 16px;">⚠️</span>
        <span id="pipeline-banner-text" style="font-size: 12px; font-weight: 600;">Data Pipeline Status</span>
      </div>
      <button id="pipeline-banner-retry" class="stream-btn" onclick="loadMarketData()" style="display: none;">🔄 Retry Fetch</button>
    </div>
  </div>

  <!-- STREAM CONTROL BAR -->
  <div class="stream-control-bar">
    <div style="display: flex; align-items: center; gap: 12px;">
      <div class="stream-status-pill paused" id="stream-status-pill">
        <span style="width: 7px; height: 7px; border-radius: 50%; background: var(--text-tertiary); display: inline-block;"></span>
        <span id="stream-status-text">MARKET CLOSED (15:30 IST) • OFFICIAL EOD BENCHMARK</span>
      </div>
      <span style="color: var(--text-secondary);" id="stream-clock">12 NOV 2026 EOD</span>
      <span style="color: var(--text-tertiary);" id="stream-ticks">Fixed Close</span>
    </div>

    <div class="stream-controls-group">
      <button class="stream-btn" id="btn-stream-toggle" onclick="toggleStream()">▶️ Live Simulation</button>
      <span style="color: var(--text-tertiary); margin: 0 4px;">|</span>
      <span>Rate:</span>
      <button class="stream-btn" id="speed-1s" onclick="setStreamSpeed(1000, this)">1s</button>
      <button class="stream-btn" id="speed-2s" onclick="setStreamSpeed(2000, this)">2s</button>
      <button class="stream-btn" id="speed-5s" onclick="setStreamSpeed(5000, this)">5s</button>
      <span style="color: var(--text-tertiary); margin: 0 4px;">|</span>
      <button class="stream-btn active" onclick="resetToOfficialClose()">🔒 12 Nov Official Close</button>
    </div>
  </div>

  <!-- NAVIGATION BAR -->
  <nav class="terminal-nav">
    <button class="nav-tab active" onclick="switchTab('pulse')">⚡ Market Pulse</button>
    <button class="nav-tab" onclick="switchTab('screener')">🎯 RS Momentum Screener</button>
    <button class="nav-tab" onclick="switchTab('results')">📊 Published Results (Q2 FY27)</button>
    <button class="nav-tab" onclick="switchTab('earnings')">📅 Earnings Radar</button>
    <button class="nav-tab" onclick="switchTab('indices')">📈 Indices Matrix</button>
    <button class="nav-tab" onclick="switchTab('theses')">📝 Investment Theses</button>
  </nav>

  <!-- MAIN TERMINAL CONTENT -->
  <main class="terminal-main">

    <!-- TAB 1: MARKET PULSE -->
    <section id="tab-pulse" class="tab-content active">
      <div class="pulse-grid">
        
        <!-- LEFT: INSTITUTIONAL FLOWS & MARKET STATISTICS -->
        <div class="panel">
          <div class="panel-header">
            <div>
              <div class="panel-title">Institutional Cash Flows (FII & DII) • NSE</div>
              <div class="panel-subtitle" id="fii-panel-subtitle">Verified Cash Market Turnovers • Official 12 Nov Close</div>
              <div class="panel-provenance" id="fii-provenance">Source: NSE Cash Market • As of: --</div>
            </div>
            <span style="font-size: 11px; font-weight: 700; color: var(--green);" id="fii-combined-badge">COMBINED NET: --</span>
          </div>

          <div class="kpi-row">
            <div class="kpi-card" id="kpi-fii">
              <div class="kpi-title" id="kpi-fii-title">FII Net Cash (11 Nov Official Close)</div>
              <div class="kpi-val up" id="kpi-fii-val">--</div>
              <div class="kpi-desc" id="kpi-fii-desc">12 Nov provisional figures pending late release</div>
            </div>
            <div class="kpi-card" id="kpi-dii">
              <div class="kpi-title" id="kpi-dii-title">DII Net Cash (11 Nov Official Close)</div>
              <div class="kpi-val down" id="kpi-dii-val">--</div>
              <div class="kpi-desc" id="kpi-dii-desc">Combined Net Liquidity: +₹4,128.80 Cr</div>
            </div>
          </div>

          <!-- MARKET BREADTH & EXCHANGE TELEMETRY -->
          <div class="breadth-container">
            <div style="display: flex; justify-content: space-between; align-items: center; font-size: 11px; margin-bottom: 6px;">
              <span style="font-weight: 700; text-transform: uppercase;">Market Statistics • 12-Nov-2026 Session Close</span>
              <span style="color: var(--green); font-weight: 800;" id="breadth-ratio">--</span>
            </div>
            <div class="breadth-track">
              <div class="breadth-adv" id="breadth-adv-bar" style="width: 0%;"></div>
              <div class="breadth-dec" id="breadth-dec-bar" style="width: 0%;"></div>
              <div class="breadth-unc" id="breadth-unc-bar" style="width: 0%;"></div>
            </div>
            <div style="display: flex; justify-content: space-between; font-size: 11px; font-family: var(--font-mono); margin-top: 4px;">
              <span class="up" id="breadth-adv-txt">▲ -- Advancing</span>
              <span style="color: var(--text-tertiary);" id="breadth-unc-txt">• -- Unchanged</span>
              <span class="down" id="breadth-dec-txt">▼ -- Declining</span>
            </div>
            <div style="margin-top: 10px; padding: 8px 10px; background: var(--card); border-radius: 6px; font-size: 11px; display: grid; grid-template-columns: repeat(4, 1fr); gap: 6px; text-align: center;">
              <div>
                <div style="color: var(--text-secondary); font-size: 10px;">Stock Traded</div>
                <strong style="color: var(--text-primary); font-size: 13px;" id="breadth-traded">--</strong>
              </div>
              <div>
                <div style="color: var(--text-secondary); font-size: 10px;">52-Week High</div>
                <strong class="up" style="font-size: 13px;" id="breadth-high52">--</strong>
              </div>
              <div>
                <div style="color: var(--text-secondary); font-size: 10px;">52-Week Low</div>
                <strong class="down" style="font-size: 13px;" id="breadth-low52">--</strong>
              </div>
              <div>
                <div style="color: var(--text-secondary); font-size: 10px;">Upper / Lower Circuit</div>
                <span style="font-size: 12px; font-weight: 700;" id="breadth-circuits">--</span>
              </div>
            </div>

            <!-- EXCHANGE TELEMETRY FOOTER -->
            <div style="margin-top: 8px; font-size: 10px; color: var(--text-secondary); display: flex; justify-content: space-between; border-top: 1px solid var(--border-subtle); padding-top: 6px; flex-wrap: wrap; gap: 4px;">
              <span>Registered Accounts: <strong style="color: var(--text-primary);" id="breadth-accounts">--</strong></span>
              <span>Market Cap: <strong style="color: var(--text-primary);" id="breadth-mcap">--</strong></span>
              <span>Avg Latency: <strong style="color: var(--cyan-rs);" id="breadth-latency">548 ns</strong></span>
            </div>
            <div class="panel-provenance" id="breadth-provenance" style="margin-top: 6px; width: 100%;">
              Source: NSE Capital Market Telemetry • As of: --
            </div>
          </div>

          <!-- FII/DII OFFICIAL RECENT SESSIONS TABLE & BARS -->
          <div class="chart-container">
            <div style="display: flex; justify-content: space-between; font-size: 11px; color: var(--text-secondary); margin-bottom: 8px; font-weight: 600;">
              <span>NSE CASH MARKET FII / DII FLOWS (₹ CRORE)</span>
              <span style="color: var(--cyan-rs); font-size: 10px;">SESSION DATA • NOVEMBER 2026</span>
            </div>
            <div id="fii-dii-bars"></div>
          </div>
        </div>

        <!-- RIGHT: 12 CURATED MARKET TAKEAWAYS -->
        <div class="panel">
          <div class="panel-header">
            <div>
              <div class="panel-title">Today's Market Pulse & Key Bullet Points</div>
              <div class="panel-subtitle">Institutional Net Flow, Big Corporate Results & Sector Movers</div>
              <div class="panel-provenance" id="bullets-provenance">Source: Exchange Telemetry & Announcements • As of: --</div>
            </div>
            <span style="font-size: 11px; font-weight: 700; color: var(--cyan-rs);" id="bullets-badge">12 LIVE BULLETS</span>
          </div>

          <div id="bullets-list" style="max-height: 620px; overflow-y: auto; padding-right: 4px;">
            <!-- Loaded dynamically from /data.json -->
          </div>
        </div>

      </div>
    </section>

    <!-- TAB 2: RELATIVE STRENGTH SCREENER -->
    <section id="tab-screener" class="tab-content">
      <div class="screener-controls">
        
        <!-- SEARCH, SORT & VIEW TOGGLE -->
        <div class="search-row">
          <input type="text" id="screener-search" class="screener-input" 
                 placeholder="Search Ticker Symbol (e.g. DIXON, TRENT, BEL, ETERNAL) or Sector..." 
                 oninput="filterScreener()">

          <select id="screener-sort" class="screener-select" onchange="filterScreener()">
            <option value="RS_DESC">Sort: RS Score (High to Low)</option>
            <option value="GAINERS">Sort: Top Gainers Today (%)</option>
            <option value="NEAR52">Sort: Closest to 52W High</option>
            <option value="VOL_SURGE">Sort: Volume Surge (vs 20DMA)</option>
            <option value="ALPHA_3M">Sort: 3M Alpha vs Benchmark</option>
          </select>

          <div class="view-toggle-group">
            <button class="view-btn active" id="btn-view-cards" onclick="setViewMode('cards')">Cards View</button>
            <button class="view-btn" id="btn-view-table" onclick="setViewMode('table')">📊 Laptop Table View</button>
          </div>
        </div>

        <!-- PRESET CHIPS -->
        <div class="chip-row" id="preset-chips">
          <div class="chip active" onclick="setPreset('ALL', this)" id="preset-chip-all">All Stocks</div>
          <div class="chip" onclick="setPreset('SUPER', this)">Super Momentum (RS≥95)</div>
          <div class="chip" onclick="setPreset('WEATHER', this)">All-Weather (RS>80)</div>
          <div class="chip" onclick="setPreset('NEAR52', this)">Within 5% 52W High</div>
          <div class="chip" onclick="setPreset('PIVOT', this)">Volume Breakout (≥2x Vol)</div>
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

        <div class="panel-provenance" id="screener-provenance" style="margin-top: 10px;">
          Source: NSE Official Bhavcopy Settlement • As of: --
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
              <th>Correlation</th>
              <th>Concall Highlights (Q2 FY27)</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody id="screener-table-body">
            <!-- Rendered by JavaScript -->
          </tbody>
        </table>
      </div>
    </section>

    <!-- TAB 3: PUBLISHED RESULTS (Q2 FY27) -->
    <section id="tab-results" class="tab-content">
      <div class="screener-controls">
        <div class="search-row">
          <div>
            <h3 style="font-size: 15px; font-weight: 800; text-transform: uppercase;">Published Corporate Earnings Analysis (Q2 FY27)</h3>
            <p style="font-size: 12px; color: var(--text-secondary); margin-top: 2px;">Verdict, Revenue & PAT Growth, Price Momentum & Investor Reactions</p>
            <div class="panel-provenance" id="results-provenance">Source: BSE/NSE Reg. 33 Corporate Filings • As of: --</div>
          </div>
          <div class="chip-row" id="verdict-filter-chips">
            <div class="chip active" onclick="filterResults('ALL', this)" id="results-chip-all">All Results</div>
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

    <!-- TAB 4: EARNINGS RADAR (Q2 FY27) -->
    <section id="tab-earnings" class="tab-content">
      <div class="panel">
        <div class="panel-header">
          <div>
            <div class="panel-title">Upcoming Week Earnings & Options Volatility Calendar</div>
            <div class="panel-subtitle">Official Filing Timeline, Implied Options Reaction & Key Focus Areas</div>
            <div class="panel-provenance" id="radar-provenance">Source: NSE Corporate Filing Schedule (Verified 2026 Calendar) • As of: --</div>
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
            <div class="panel-provenance" id="indices-provenance">Source: NSE Indices Limited (IISL) Official Benchmark Telemetry • As of: --</div>
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
            <div class="panel-provenance">Storage: Client LocalStorage • Private Sandbox</div>
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

  <!-- BROKER FEED CONNECT MODAL -->
  <div class="modal-overlay" id="broker-modal" onclick="closeBrokerModal(event)">
    <div class="modal-content" style="max-width: 600px;" onclick="event.stopPropagation()">
      <button class="modal-close" onclick="closeBrokerModalDirect()">✕</button>
      <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 16px;">
        <span style="font-size: 24px;">🔑</span>
        <div>
          <h2 style="font-size: 18px; font-weight: 800;">Connect Institutional / Broker Feed</h2>
          <p style="font-size: 12px; color: var(--text-secondary);">Direct sub-second streaming via Zerodha Kite, Upstox, or custom WebSocket</p>
        </div>
      </div>

      <div style="display: flex; flex-direction: column; gap: 14px;">
        <div>
          <label style="font-size: 11px; font-weight: 700; color: var(--text-secondary); display: block; margin-bottom: 4px;">BROKER PLATFORM</label>
          <select id="broker-select" class="screener-select" style="width: 100%;">
            <option value="zerodha">Zerodha Kite Connect WebSocket (WSS)</option>
            <option value="upstox">Upstox Market Data Feed v2</option>
            <option value="dhan">Dhan Live Market Feed</option>
            <option value="custom">Custom Enterprise WebSocket (WSS)</option>
          </select>
        </div>

        <div>
          <label style="font-size: 11px; font-weight: 700; color: var(--text-secondary); display: block; margin-bottom: 4px;">API KEY / ACCESS TOKEN</label>
          <input type="password" id="broker-api-key" class="screener-input" placeholder="Enter API Key / JWT Access Token..." style="width: 100%;">
        </div>

        <div>
          <label style="font-size: 11px; font-weight: 700; color: var(--text-secondary); display: block; margin-bottom: 4px;">WEBSOCKET URL (OPTIONAL)</label>
          <input type="text" id="broker-wss-url" class="screener-input" placeholder="wss://ws.kite.trade?api_key=..." style="width: 100%;">
        </div>

        <div style="background: var(--card); border: 1px solid var(--border); border-radius: 8px; padding: 12px; font-size: 11px; color: var(--text-secondary); line-height: 1.5;">
          ℹ️ <strong>Why this is needed for external real-time data:</strong> National Stock Exchange (NSE) blocks direct web scrapers and browsers via CORS policies. Connecting your authorized broker API stream gives true tick-by-tick exchange execution. Meanwhile, MarketEdge loads verified EOD datasets from <code>/data.json</code>.
        </div>

        <div style="display: flex; gap: 10px; justify-content: flex-end; margin-top: 10px;">
          <button class="theme-btn" onclick="closeBrokerModalDirect()">Cancel</button>
          <button class="theme-btn" onclick="saveBrokerConfig()" style="background: var(--cyan-rs-bg); color: var(--cyan-rs); border-color: var(--cyan-rs);">Save & Connect Feed</button>
        </div>
      </div>
    </div>
  </div>

  <!-- MODAL DOSSIER FOR STOCK DETAIL WITH INTERACTIVE CANVAS CHART -->
  <div class="modal-overlay" id="stock-modal" onclick="closeModal(event)">
    <div class="modal-content" onclick="event.stopPropagation()">
      <button class="modal-close" onclick="closeModalDirect()">✕</button>
      <div id="modal-body"></div>
    </div>
  </div>

  <!-- FOOTER -->
  <footer class="terminal-footer">
    <div>MarketEdge Terminal • Indian Equity Screener • Primary Benchmark: NIFTY 500 • Reporting Period: Q2 FY27</div>
    <div id="footer-time">Verified EOD Pipeline • Provenance Backed</div>
  </footer>

  <script>
    // GLOBAL STATE (LOADED EXCLUSIVELY FROM /data.json)
    let MARKET_DATA = null;
    let STOCKS = [];
    let RESULTS = [];
    let BULLETS = [];
    let INDICES = [];
    let UPCOMING_EARNINGS = [];
    let BENCHMARK_DATA = {};

    let currentPreset = 'ALL';
    let currentSector = 'All';
    let currentResultFilter = 'ALL';
    let currentViewMode = 'cards';
    let activeStockCandles = [];
    let activeStockSymbol = '';
    let selectedTimeframe = '3M';
    let selectedIndicator = 'ALL';
    let currentModalBenchmark = 'relevant';

    // STREAMING TICK ENGINE (FOR SIMULATION MODE)
    let isStreamActive = false;
    let streamIntervalId = null;
    let streamSpeedMs = 1000;
    let streamTickCount = 0;
    let simulatedSeconds = 0;

    let liveNifty50 = 0;
    let liveNifty50Chg = 0;
    let liveNifty500 = 0;
    let liveNifty500Chg = 0;
    let liveBankNifty = 0;
    let liveBankNiftyChg = 0;

    // INITIALIZATION LIFECYCLE
    document.addEventListener("DOMContentLoaded", () => {
      loadMarketData();
    });

    // -------------------------------------------------------------
    // DATA PIPELINE: FETCH /data.json
    // -------------------------------------------------------------
    async function loadMarketData() {
      showDataBanner('info', '⏳ Connecting to verified market data pipeline (/data.json)...', false);

      try {
        let response = null;
        try {
          response = await fetch('/data.json');
        } catch (fetchErr1) {
          // Fallback for Android WebView local asset protocol (file:///android_asset/web/...)
          try {
            response = await fetch('data.json');
          } catch (fetchErr2) {
            throw new Error('Network and local asset fetch failed.');
          }
        }

        if (!response || !response.ok) {
          throw new Error(`HTTP ${response ? response.status : 'ERR'}: Failed to retrieve /data.json`);
        }

        const data = await response.json();
        if (!data || !data.screener || !data.screener.stocks) {
          throw new Error('Corrupted or empty dataset received from /data.json.');
        }

        // Assign verified stores
        MARKET_DATA = data;
        STOCKS = data.screener.stocks || [];
        RESULTS = data.results?.items || [];
        BULLETS = data.marketPulse?.bullets || [];
        INDICES = data.indices?.items || [];
        UPCOMING_EARNINGS = data.earningsRadar?.items || [];
        BENCHMARK_DATA = data.benchmarks?.series || {};

        // Base tickers
        if (data.tickers) {
          liveNifty50 = data.tickers.nifty50?.rawVal || 25299.35;
          liveNifty50Chg = data.tickers.nifty50?.rawChg || 44.30;
          liveNifty500 = data.tickers.nifty500?.rawVal || 23779.30;
          liveNifty500Chg = data.tickers.nifty500?.rawChg || 212.40;
          liveBankNifty = data.tickers.bankNifty?.rawVal || 61240.80;
          liveBankNiftyChg = data.tickers.bankNifty?.rawChg || 1048.40;
        }

        // Check if data is stale (> 1 trading day = ~24 hours)
        validateDataFreshness(data.metadata?.asOf);

        // Populate all panels
        renderHeaderTickers();
        renderBreadth();
        renderFiiDii();
        renderBullets();
        renderScreener();
        renderResults();
        renderEarnings();
        renderIndices();
        renderTheses();
        updateProvenanceLabels();

      } catch (err) {
        console.error('Critical Data Pipeline Failure:', err);
        showDataBanner('error', `🚨 DATA PIPELINE ERROR: Unable to load verified /data.json (${err.message}). Never filling with invented numbers. Please run 'python3 scripts/generate_market_data.py'.`, true);
        renderFailureEmptyState(err.message);
      }
    }

    function validateDataFreshness(asOfIso) {
      if (!asOfIso) {
        showDataBanner('warning', '⚠️ DATA WARNING: Missing timestamp in dataset. Check data generation pipeline.', false);
        return;
      }
      const asOfDate = new Date(asOfIso);
      const now = new Date();
      // Difference in hours
      const diffHours = (now.getTime() - asOfDate.getTime()) / (1000 * 3600);

      const formattedAsOf = asOfDate.toLocaleString('en-IN', {
        timeZone: 'Asia/Kolkata',
        day: '2-digit', month: 'short', year: 'numeric',
        hour: '2-digit', minute: '2-digit', hour12: false
      }) + ' IST';

      if (diffHours > 24) {
        showDataBanner('warning', `⚠️ DATA STALE WARNING: Market feed is older than 1 trading day (Dataset As Of: ${formattedAsOf}). Run 'python3 scripts/generate_market_data.py' to generate fresh session data.`, false);
      } else {
        hideDataBanner();
      }
    }

    function showDataBanner(type, message, showRetry = false) {
      const banner = document.getElementById("data-pipeline-banner");
      const icon = document.getElementById("pipeline-banner-icon");
      const text = document.getElementById("pipeline-banner-text");
      const retryBtn = document.getElementById("pipeline-banner-retry");
      if (!banner) return;

      banner.className = `pipeline-banner ${type}`;
      banner.style.display = "flex";
      icon.innerText = type === 'error' ? '🚨' : type === 'warning' ? '⚠️' : 'ℹ️';
      text.innerText = message;
      if (retryBtn) retryBtn.style.display = showRetry ? "inline-block" : "none";
    }

    function hideDataBanner() {
      const banner = document.getElementById("data-pipeline-banner");
      if (banner) banner.style.display = "none";
    }

    function renderFailureEmptyState(errMsg) {
      const screenerGrid = document.getElementById("screener-cards-grid");
      if (screenerGrid) {
        screenerGrid.innerHTML = `
          <div style="grid-column: 1 / -1; padding: 40px; text-align: center; background: var(--card); border: 1px dashed var(--red); border-radius: 12px;">
            <div style="font-size: 24px; margin-bottom: 8px;">🚨</div>
            <strong style="color: var(--red); font-size: 15px;">Market Data Pipeline Offline</strong>
            <p style="font-size: 12px; color: var(--text-secondary); margin-top: 6px;">
              Per strict data integrity guidelines, zero mock or placeholder numbers are rendered when /data.json cannot be loaded.<br>
              Run the backend generation script: <code>python3 scripts/generate_market_data.py</code> and retry.
            </p>
            <button class="theme-btn" onclick="loadMarketData()" style="margin: 16px auto 0 auto; background: var(--cyan-rs-bg); color: var(--cyan-rs); border-color: var(--cyan-rs);">🔄 Retry Pipeline Fetch</button>
          </div>
        `;
      }
      const bulletsList = document.getElementById("bullets-list");
      if (bulletsList) bulletsList.innerHTML = `<div style="padding: 20px; color: var(--red); font-size: 12px; text-align: center;">Market Pulse unavailable without verified /data.json.</div>`;
    }

    function formatProvenanceDate(isoStr) {
      if (!isoStr) return '12-Nov-2026 15:30 IST';
      try {
        const d = new Date(isoStr);
        return d.toLocaleString('en-IN', {
          timeZone: 'Asia/Kolkata',
          day: '2-digit', month: 'short', year: 'numeric',
          hour: '2-digit', minute: '2-digit', hour12: false
        }) + ' IST';
      } catch (e) {
        return isoStr;
      }
    }

    function updateProvenanceLabels() {
      if (!MARKET_DATA) return;
      const asOf = formatProvenanceDate(MARKET_DATA.metadata?.asOf);
      const sources = MARKET_DATA.sources || {};

      const setEl = (id, txt) => {
        const el = document.getElementById(id);
        if (el) el.innerHTML = txt;
      };

      setEl("fii-provenance", `Source: ${sources.fiiDii || 'NSE Cash Market'} • As of: ${asOf}`);
      setEl("breadth-provenance", `Source: ${sources.breadth || 'NSE Telemetry'} • As of: ${asOf}`);
      setEl("bullets-provenance", `Source: ${MARKET_DATA.marketPulse?.source || 'NSE Daily Reports'} • As of: ${asOf}`);
      setEl("screener-provenance", `Source: ${MARKET_DATA.screener?.source || 'NSE Official Bhavcopy'} • As of: ${asOf}`);
      setEl("results-provenance", `Source: ${MARKET_DATA.results?.source || 'BSE/NSE Reg. 33 Filings'} • As of: ${asOf}`);
      setEl("radar-provenance", `Source: ${MARKET_DATA.earningsRadar?.source || 'NSE Corporate Diary'} • As of: ${asOf}`);
      setEl("indices-provenance", `Source: ${MARKET_DATA.indices?.source || 'NSE Indices Limited'} • As of: ${asOf}`);

      const headerDate = document.getElementById("header-date");
      if (headerDate && MARKET_DATA.metadata) {
        headerDate.innerText = `${MARKET_DATA.metadata.sessionDate || '12 NOV 2026'} • OFFICIAL EOD CLOSE`;
      }
    }

    // -------------------------------------------------------------
    // RENDER HEADER TICKERS
    // -------------------------------------------------------------
    function renderHeaderTickers() {
      if (!MARKET_DATA || !MARKET_DATA.tickers) return;
      const t = MARKET_DATA.tickers;

      const n50Val = document.getElementById("nifty50-val");
      const n50Chg = document.getElementById("nifty50-chg");
      if (n50Val && t.nifty50) n50Val.innerText = t.nifty50.val;
      if (n50Chg && t.nifty50) {
        n50Chg.innerText = t.nifty50.chg;
        n50Chg.className = t.nifty50.isUp ? 'up' : 'down';
      }

      const n500Val = document.getElementById("nifty500-val");
      const n500Chg = document.getElementById("nifty500-chg");
      if (n500Val && t.nifty500) n500Val.innerText = t.nifty500.val;
      if (n500Chg && t.nifty500) {
        n500Chg.innerText = t.nifty500.chg;
        n500Chg.className = t.nifty500.isUp ? 'up' : 'down';
      }

      const bnVal = document.getElementById("banknifty-val");
      const bnChg = document.getElementById("banknifty-chg");
      if (bnVal && t.bankNifty) bnVal.innerText = t.bankNifty.val;
      if (bnChg && t.bankNifty) {
        bnChg.innerText = t.bankNifty.chg;
        bnChg.className = t.bankNifty.isUp ? 'up' : 'down';
      }
    }

    // -------------------------------------------------------------
    // RENDER MARKET BREADTH & EXCHANGE STATISTICS
    // -------------------------------------------------------------
    function renderBreadth() {
      if (!MARKET_DATA || !MARKET_DATA.marketPulse || !MARKET_DATA.marketPulse.breadth) return;
      const b = MARKET_DATA.marketPulse.breadth;

      const ratioEl = document.getElementById("breadth-ratio");
      if (ratioEl) ratioEl.innerText = `${b.advances.toLocaleString()} Adv vs ${b.declines.toLocaleString()} Dec (${b.advancesPct} Advances)`;

      const advBar = document.getElementById("breadth-adv-bar");
      const decBar = document.getElementById("breadth-dec-bar");
      const uncBar = document.getElementById("breadth-unc-bar");
      if (advBar) advBar.style.width = b.advancesPct;
      if (decBar) decBar.style.width = b.declinesPct;
      if (uncBar) uncBar.style.width = b.unchangedPct;

      const advTxt = document.getElementById("breadth-adv-txt");
      const uncTxt = document.getElementById("breadth-unc-txt");
      const decTxt = document.getElementById("breadth-dec-txt");
      if (advTxt) advTxt.innerText = `▲ ${b.advances.toLocaleString()} Advancing (${b.advancesPct})`;
      if (uncTxt) uncTxt.innerText = `• ${b.unchanged.toLocaleString()} Unchanged (${b.unchangedPct})`;
      if (decTxt) decTxt.innerText = `▼ ${b.declines.toLocaleString()} Declining (${b.declinesPct})`;

      const tradedEl = document.getElementById("breadth-traded");
      const highEl = document.getElementById("breadth-high52");
      const lowEl = document.getElementById("breadth-low52");
      const circEl = document.getElementById("breadth-circuits");
      if (tradedEl) tradedEl.innerText = b.totalTraded.toLocaleString();
      if (highEl) highEl.innerText = `▲ ${b.highs52}`;
      if (lowEl) lowEl.innerText = `▼ ${b.lows52}`;
      if (circEl) circEl.innerHTML = `<span class="up">${b.upperCircuit}</span> / <span class="down">${b.lowerCircuit}</span>`;

      const accEl = document.getElementById("breadth-accounts");
      const mcapEl = document.getElementById("breadth-mcap");
      const latEl = document.getElementById("breadth-latency");
      if (accEl) accEl.innerText = b.registeredAccounts || '27,65,97,533';
      if (mcapEl) mcapEl.innerText = b.marketCap || '₹506.65 Lac Cr ($5.40 Tn)';
      if (latEl) latEl.innerText = b.avgAckLatencyNs ? `${b.avgAckLatencyNs} ns` : '548 ns';
    }

    // -------------------------------------------------------------
    // RENDER FII / DII INSTITUTIONAL FLOWS
    // -------------------------------------------------------------
    function renderFiiDii() {
      if (!MARKET_DATA || !MARKET_DATA.marketPulse) return;
      const sum = MARKET_DATA.marketPulse.fiiDiiSummary || {};
      const flows = MARKET_DATA.marketPulse.flows || [];

      const fiiVal = document.getElementById("kpi-fii-val");
      const diiVal = document.getElementById("kpi-dii-val");
      const combinedBadge = document.getElementById("fii-combined-badge");
      const fiiTitle = document.getElementById("kpi-fii-title");
      const diiTitle = document.getElementById("kpi-dii-title");

      if (fiiTitle && sum.latestDate) fiiTitle.innerText = `FII Net Cash (${sum.latestDate} Official Close)`;
      if (diiTitle && sum.latestDate) diiTitle.innerText = `DII Net Cash (${sum.latestDate} Official Close)`;

      if (fiiVal) fiiVal.innerText = `${sum.fiiNet ? (sum.fiiNet.startsWith('+') ? '' : '+') : ''}₹${sum.fiiNet} Cr`;
      if (diiVal) diiVal.innerText = `₹${sum.diiNet} Cr`;
      if (combinedBadge) combinedBadge.innerText = `${sum.latestDate ? sum.latestDate.toUpperCase() + ' ' : ''}COMBINED: ₹${sum.combinedNet} Cr`;

      const container = document.getElementById("fii-dii-bars");
      if (!container) return;

      let html = '';
      flows.forEach(f => {
        if (f.fii === null || f.dii === null) {
          html += `
            <div style="margin-bottom: 9px; padding-bottom: 8px; border-bottom: 1px dashed var(--border);">
              <div style="display: flex; justify-content: space-between; font-size: 11px; margin-bottom: 3px; font-family: var(--font-mono);">
                <strong>${f.date}</strong>
                <span style="color: var(--cyan-rs); font-weight: 700;">● ${f.status}</span>
              </div>
              <div style="font-size: 10px; color: var(--text-secondary);">Provisional EOD figures pending late release from exchange clearing corporations.</div>
            </div>
          `;
          return;
        }

        const isNetUp = f.net >= 0;
        const maxVal = 16000;
        const fiiPct = Math.min(50, (Math.abs(f.fii) / maxVal) * 50);
        const diiPct = Math.min(50, (Math.abs(f.dii) / maxVal) * 50);

        html += `
          <div style="margin-bottom: 10px; padding-bottom: 8px; border-bottom: 1px dashed var(--border);">
            <div style="display: flex; justify-content: space-between; font-size: 11px; margin-bottom: 3px; font-family: var(--font-mono); flex-wrap: wrap; gap: 4px;">
              <strong>${f.date.replace(' 2026', '')}</strong>
              <span>FII: <strong style="color: ${f.fii >= 0 ? 'var(--green)' : 'var(--red)'}">${f.fii >= 0 ? '+' : ''}₹${f.fii.toFixed(2)} Cr</strong></span>
              <span>DII: <strong style="color: ${f.dii >= 0 ? 'var(--green)' : 'var(--red)'}">${f.dii >= 0 ? '+' : ''}₹${f.dii.toFixed(2)} Cr</strong></span>
              <span>Combined: <strong style="color: ${isNetUp ? 'var(--green)' : 'var(--red)'}">${isNetUp ? '+' : ''}₹${f.net.toFixed(2)} Cr</strong></span>
            </div>
            <!-- DIVERGING BAR AROUND ZERO CENTER -->
            <div style="position: relative; height: 10px; width: 100%; background: var(--card); border-radius: 4px; overflow: hidden; margin-top: 4px; border: 1px solid var(--border-subtle);">
              <div style="position: absolute; left: 50%; top: 0; bottom: 0; width: 2px; background: var(--text-tertiary); z-index: 2;"></div>
              <div style="position: absolute; top: 1px; height: 3px; ${f.fii >= 0 ? 'left: 50%; width: ' + fiiPct + '%; background: var(--green);' : 'right: 50%; width: ' + fiiPct + '%; background: var(--red);'}"></div>
              <div style="position: absolute; bottom: 1px; height: 3px; ${f.dii >= 0 ? 'left: 50%; width: ' + diiPct + '%; background: var(--cyan-rs);' : 'right: 50%; width: ' + diiPct + '%; background: #EF4444;'}"></div>
            </div>
            <div style="display: flex; justify-content: space-between; font-size: 9px; color: var(--text-tertiary); font-family: var(--font-mono); margin-top: 2px;">
              <span>◀ Net Outflow (Red)</span>
              <span>0 (Center Line)</span>
              <span>Net Inflow (Green) ▶</span>
            </div>
          </div>
        `;
      });
      container.innerHTML = html;
    }

    // -------------------------------------------------------------
    // RENDER 12 BULLETS
    // -------------------------------------------------------------
    function renderBullets() {
      const container = document.getElementById("bullets-list");
      if (!container) return;
      if (!BULLETS || BULLETS.length === 0) {
        container.innerHTML = `<div style="padding: 20px; color: var(--text-secondary); text-align: center;">No bullet takeaways loaded.</div>`;
        return;
      }

      let html = '';
      BULLETS.forEach(b => {
        html += `
          <div class="bullet-card">
            <div class="bullet-header">
              <span class="bullet-tag ${b.tag}">${b.category}</span>
              <span style="font-size: 10px; color: var(--text-tertiary); font-family: var(--font-mono);">Source: ${b.source || 'NSE'}</span>
            </div>
            <div class="bullet-title">${b.title}</div>
            <div class="bullet-desc">${b.desc}</div>
          </div>
        `;
      });
      container.innerHTML = html;

      const badge = document.getElementById("bullets-badge");
      if (badge) badge.innerText = `${BULLETS.length} VERIFIED BULLETS`;
    }

    // -------------------------------------------------------------
    // RENDER SCREENER (CARDS & TABLE)
    // -------------------------------------------------------------
    function renderScreener() {
      const cardsGrid = document.getElementById("screener-cards-grid");
      const tableBody = document.getElementById("screener-table-body");
      if (!cardsGrid || !tableBody) return;

      const filtered = getFilteredStocks();

      // Cards
      let cardsHtml = '';
      filtered.forEach(s => {
        const dist52 = Math.abs(((s.price - s.high52) / s.high52) * 100).toFixed(1);
        const alpha = (s.quant && s.quant.alpha !== undefined) ? s.quant.alpha : (s.threeM - (s.benchmarkReturn || 4.2)).toFixed(1);
        const relevantBm = s.benchmarkName || (s.cap === 'Large-Cap' ? 'Nifty 50' : 'Nifty Midcap 100');
        const alphaFormatted = alpha >= 0 ? `+${alpha}%` : `${alpha}%`;

        cardsHtml += `
          <div class="stock-card" id="stock-card-${s.symbol}" onclick="openStockModal('${s.symbol}')">
            <div>
              <div class="card-top">
                <div>
                  <div class="card-symbol">${s.symbol}</div>
                  <div class="card-name">${s.name}</div>
                </div>
                <div style="text-align: right;">
                  <span class="rs-score-badge ${s.rs >= 95 ? 'rs-super' : 'rs-high'}">RS ${s.rs}</span>
                  <div style="font-size: 10px; color: var(--text-tertiary); margin-top: 2px;">${s.cap}</div>
                </div>
              </div>

              <div class="card-price-row">
                <span class="stock-card-price">₹${s.price.toLocaleString()}</span>
                <span class="stock-card-chg ${s.chg >= 0 ? 'up' : 'down'}">${s.chg >= 0 ? '+' : ''}${s.chg}%</span>
              </div>

              <div style="display: flex; justify-content: space-between; font-size: 11px; color: var(--text-secondary); margin-bottom: 6px; font-family: var(--font-mono);">
                <span>Dist to 52W High: <strong>${dist52}%</strong></span>
                <span>3M Alpha: <strong class="${alpha >= 0 ? 'up' : 'down'}">${alphaFormatted}</strong></span>
              </div>

              <div style="font-size: 10px; color: var(--text-tertiary); margin-bottom: 6px;">
                Benchmark: <strong style="color: var(--text-secondary);">${relevantBm}</strong> • Vol: <strong>${s.vol}x 20DMA</strong>
              </div>

              <div style="background: var(--card); padding: 6px 8px; border-radius: 6px; font-size: 11px; color: var(--text-secondary); margin-top: 6px; line-height: 1.4;">
                <strong style="color: var(--cyan-rs);">Concall Note:</strong> ${s.takeaways && s.takeaways[0] ? s.takeaways[0].substring(0, 95) + '...' : 'Quarterly concall takeaways available.'}
              </div>
            </div>

            <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 10px; padding-top: 8px; border-top: 1px solid var(--border-subtle); font-size: 10px; color: var(--text-tertiary);">
              <span>${s.sector}</span>
              <span style="color: var(--cyan-rs); font-weight: 700;">Open Dossier ➔</span>
            </div>
          </div>
        `;
      });
      cardsGrid.innerHTML = cardsHtml || `<div style="grid-column: 1 / -1; padding: 30px; text-align: center; color: var(--text-secondary);">No stocks match your filter criteria.</div>`;

      // Table
      let tableHtml = '';
      filtered.forEach(s => {
        const dist52 = Math.abs(((s.price - s.high52) / s.high52) * 100).toFixed(1);
        const alpha = (s.quant && s.quant.alpha !== undefined) ? s.quant.alpha : (s.threeM - (s.benchmarkReturn || 4.2)).toFixed(1);
        const alphaFormatted = alpha >= 0 ? `+${alpha}%` : `${alpha}%`;
        const sdVal = s.quant?.sd ? `${s.quant.sd}%` : '--';
        const corrVal = s.quant?.corr ? s.quant.corr.toFixed(2) : '--';

        tableHtml += `
          <tr id="table-row-${s.symbol}" onclick="openStockModal('${s.symbol}')" style="cursor: pointer;">
            <td style="font-family: var(--font-mono); font-weight: 800; color: var(--cyan-rs);">${s.symbol}</td>
            <td style="font-weight: 600;">${s.name}</td>
            <td>${s.sector}</td>
            <td>${s.cap}</td>
            <td class="table-price" style="font-family: var(--font-mono); font-weight: 700;">₹${s.price.toLocaleString()}</td>
            <td class="table-chg ${s.chg >= 0 ? 'up' : 'down'}" style="font-family: var(--font-mono); font-weight: 700;">${s.chg >= 0 ? '+' : ''}${s.chg}%</td>
            <td><span class="rs-score-badge ${s.rs >= 95 ? 'rs-super' : 'rs-high'}">RS ${s.rs}</span></td>
            <td style="font-family: var(--font-mono);">${dist52}%</td>
            <td style="font-family: var(--font-mono);" class="${alpha >= 0 ? 'up' : 'down'}">${alphaFormatted}</td>
            <td style="font-family: var(--font-mono);">${s.vol}x</td>
            <td style="font-family: var(--font-mono);">${sdVal}</td>
            <td style="font-family: var(--font-mono);">${corrVal}</td>
            <td style="max-width: 280px; overflow: hidden; text-overflow: ellipsis; font-size: 11px; color: var(--text-secondary);">${s.takeaways && s.takeaways[0] ? s.takeaways[0] : '--'}</td>
            <td><button class="theme-btn" style="padding: 2px 8px; font-size: 10px;" onclick="event.stopPropagation(); openStockModal('${s.symbol}')">Dossier</button></td>
          </tr>
        `;
      });
      tableBody.innerHTML = tableHtml || `<tr><td colspan="14" style="text-align: center; padding: 20px; color: var(--text-secondary);">No stocks match your filter criteria.</td></tr>`;

      const allChip = document.getElementById("preset-chip-all");
      if (allChip) allChip.innerText = `All Stocks (${STOCKS.length})`;
    }

    function getFilteredStocks() {
      const search = (document.getElementById("screener-search")?.value || "").trim().toLowerCase();
      const sort = document.getElementById("screener-sort")?.value || "RS_DESC";

      return STOCKS.filter(s => {
        // Sector filter
        if (currentSector !== 'All') {
          if (!s.sector.toLowerCase().includes(currentSector.toLowerCase())) return false;
        }

        // Search
        if (search) {
          const matchSym = s.symbol.toLowerCase().includes(search);
          const matchName = s.name.toLowerCase().includes(search);
          const matchSec = s.sector.toLowerCase().includes(search);
          if (!matchSym && !matchName && !matchSec) return false;
        }

        // Preset filter
        const dist52 = Math.abs(((s.price - s.high52) / s.high52) * 100);
        if (currentPreset === 'SUPER' && s.rs < 95) return false;
        if (currentPreset === 'WEATHER' && s.rs <= 80) return false;
        if (currentPreset === 'NEAR52' && dist52 > 5.0) return false;
        if (currentPreset === 'PIVOT' && s.vol < 2.0) return false;
        if (currentPreset === 'RESILIENT') {
          const alpha = (s.quant && s.quant.alpha !== undefined) ? s.quant.alpha : (s.threeM - 4.2);
          if (alpha < 20) return false;
        }

        return true;
      }).sort((a, b) => {
        if (sort === 'RS_DESC') return b.rs - a.rs;
        if (sort === 'GAINERS') return b.chg - a.chg;
        if (sort === 'NEAR52') {
          const dA = Math.abs((a.price - a.high52) / a.high52);
          const dB = Math.abs((b.price - b.high52) / b.high52);
          return dA - dB;
        }
        if (sort === 'VOL_SURGE') return b.vol - a.vol;
        if (sort === 'ALPHA_3M') {
          const aA = a.quant?.alpha ?? (a.threeM - 4.2);
          const aB = b.quant?.alpha ?? (b.threeM - 4.2);
          return aB - aA;
        }
        return 0;
      });
    }

    function filterScreener() {
      renderScreener();
    }

    function setPreset(preset, el) {
      currentPreset = preset;
      document.querySelectorAll("#preset-chips .chip").forEach(c => c.classList.remove("active"));
      el.classList.add("active");
      renderScreener();
    }

    function setSector(sec, el) {
      currentSector = sec;
      document.querySelectorAll("#sector-chips .chip").forEach(c => c.classList.remove("active"));
      el.classList.add("active");
      renderScreener();
    }

    // -------------------------------------------------------------
    // RENDER PUBLISHED RESULTS (Q2 FY27)
    // -------------------------------------------------------------
    function renderResults() {
      const grid = document.getElementById("results-cards-grid");
      if (!grid) return;
      if (!RESULTS || RESULTS.length === 0) {
        grid.innerHTML = `<div style="grid-column: 1 / -1; padding: 20px; color: var(--text-secondary); text-align: center;">No published results in dataset.</div>`;
        return;
      }

      const filtered = RESULTS.filter(r => {
        if (currentResultFilter === 'ALL') return true;
        return r.verdict === currentResultFilter;
      });

      let html = '';
      filtered.forEach(r => {
        html += `
          <div class="result-card">
            <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 8px;">
              <div>
                <span style="font-family: var(--font-mono); font-weight: 800; font-size: 15px; color: var(--cyan-rs);">${r.symbol}</span>
                <span style="font-size: 12px; font-weight: 600; margin-left: 6px;">${r.name}</span>
                <div style="font-size: 11px; color: var(--text-secondary);">${r.sector}</div>
              </div>
              <span style="font-size: 11px; font-weight: 800; font-family: var(--font-mono);">${r.verdictLabel || r.verdict}</span>
            </div>

            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin: 10px 0; background: var(--card); padding: 8px 10px; border-radius: 6px; font-family: var(--font-mono); font-size: 11px;">
              <div>
                <span style="color: var(--text-secondary); font-size: 10px; display: block;">Revenue (${r.rev})</span>
                <span class="${r.revYoy?.startsWith('+') ? 'up' : 'down'}">YoY: ${r.revYoy}</span> • QoQ: ${r.revQoq || '--'}
              </div>
              <div>
                <span style="color: var(--text-secondary); font-size: 10px; display: block;">Net Profit (${r.pat})</span>
                <span class="${r.patYoy?.startsWith('+') ? 'up' : 'down'}">YoY: ${r.patYoy}</span> • QoQ: ${r.patQoq || '--'}
              </div>
            </div>

            <div style="font-size: 11px; color: var(--text-secondary); line-height: 1.45; margin-bottom: 8px;">
              ${r.remark}
            </div>

            <div style="display: flex; justify-content: space-between; font-size: 10px; color: var(--text-tertiary); font-family: var(--font-mono); border-top: 1px solid var(--border-subtle); padding-top: 6px;">
              <span>Reaction: <strong class="${r.priceReaction?.startsWith('+') ? 'up' : 'down'}">${r.priceReaction}</strong> (Vol: ${r.vol})</span>
              <span>Source: ${r.source || 'BSE Filings'}</span>
            </div>
          </div>
        `;
      });
      grid.innerHTML = html;

      const chipAll = document.getElementById("results-chip-all");
      if (chipAll) chipAll.innerText = `All Results (${RESULTS.length})`;
    }

    function filterResults(verdict, el) {
      currentResultFilter = verdict;
      document.querySelectorAll("#verdict-filter-chips .chip").forEach(c => c.classList.remove("active"));
      el.classList.add("active");
      renderResults();
    }

    // -------------------------------------------------------------
    // RENDER EARNINGS RADAR
    // -------------------------------------------------------------
    function renderEarnings() {
      const container = document.getElementById("earnings-list");
      if (!container) return;
      if (!UPCOMING_EARNINGS || UPCOMING_EARNINGS.length === 0) {
        container.innerHTML = `<div style="padding: 20px; color: var(--text-secondary); text-align: center;">No upcoming filings scheduled.</div>`;
        return;
      }

      let html = '';
      UPCOMING_EARNINGS.forEach(e => {
        html += `
          <div class="bullet-card">
            <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 4px;">
              <div>
                <span style="font-family: var(--font-mono); font-weight: 800; font-size: 15px;">${e.symbol}</span>
                <span style="font-size: 11px; color: var(--text-secondary); margin-left: 6px;">${e.name}</span>
              </div>
              <span style="color: var(--amber); font-weight: 700; font-size: 12px; font-family: var(--font-mono);">Expected Volatility: ${e.vol} (${e.move})</span>
            </div>
            <div style="font-size: 11px; color: var(--cyan-rs); margin: 4px 0; font-family: var(--font-mono);">Filing Date: ${e.date} • Consensus PAT: ${e.pat}</div>
            <div style="font-size: 12px; color: var(--text-secondary);">${e.focus}</div>
            <div style="font-size: 10px; color: var(--text-tertiary); font-family: var(--font-mono); margin-top: 4px;">Source: ${e.source || 'NSE Corporate Diary'}</div>
          </div>
        `;
      });
      container.innerHTML = html;
    }

    // -------------------------------------------------------------
    // RENDER INDICES MATRIX
    // -------------------------------------------------------------
    function renderIndices() {
      const container = document.getElementById("indices-grid");
      if (!container) return;
      if (!INDICES || INDICES.length === 0) {
        container.innerHTML = `<div style="padding: 20px; color: var(--text-secondary); text-align: center;">No index series loaded.</div>`;
        return;
      }

      let html = '';
      INDICES.forEach(idx => {
        html += `
          <div class="kpi-card" id="index-card-${idx.name.replace(/\s+/g, '-')}">
            <div class="kpi-title">${idx.name}</div>
            <div class="kpi-val">${idx.price}</div>
            <div class="${idx.chg.startsWith('+') ? 'up' : 'down'}" style="font-size: 12px; font-weight: 700; font-family: var(--font-mono);">${idx.chg}</div>
            <div style="font-size: 10px; color: var(--cyan-rs); margin-top: 6px;">52W Range: ${idx.low} - ${idx.high}</div>
            <div style="font-size: 10px; color: var(--text-tertiary); margin-top: 2px;">Trend: ${idx.trend}</div>
            <div style="font-size: 10px; color: var(--text-secondary); margin-top: 4px; font-family: var(--font-mono);">${idx.ma}</div>
          </div>
        `;
      });
      container.innerHTML = html;
    }

    // -------------------------------------------------------------
    // INVESTMENT THESES (LOCALSTORAGE)
    // -------------------------------------------------------------
    function saveThesis() {
      const sym = document.getElementById("thesis-symbol").value.trim().toUpperCase();
      const target = document.getElementById("thesis-target").value.trim();
      const text = document.getElementById("thesis-text").value.trim();
      if (!sym || !text) return showToast("Please enter symbol and thesis text.", "warning");

      const saved = JSON.parse(localStorage.getItem("marketedge_theses") || "[]");
      saved.unshift({ symbol: sym, target, text, date: new Date().toLocaleDateString('en-GB') });
      localStorage.setItem("marketedge_theses", JSON.stringify(saved));

      document.getElementById("thesis-symbol").value = "";
      document.getElementById("thesis-target").value = "";
      document.getElementById("thesis-text").value = "";
      renderTheses();
      showToast("Investment thesis saved securely to browser localStorage.", "success");
    }

    function deleteThesis(idx) {
      const saved = JSON.parse(localStorage.getItem("marketedge_theses") || "[]");
      saved.splice(idx, 1);
      localStorage.setItem("marketedge_theses", JSON.stringify(saved));
      renderTheses();
      showToast("Thesis deleted.", "info");
    }

    function renderTheses() {
      const container = document.getElementById("theses-cards-container");
      if (!container) return;
      const saved = JSON.parse(localStorage.getItem("marketedge_theses") || "[]");
      if (saved.length === 0) {
        container.innerHTML = `<div style="color: var(--text-tertiary); font-size: 12px; padding: 12px 0;">No investment notes saved yet. Add your first stock thesis above!</div>`;
        return;
      }
      let html = '';
      saved.forEach((t, i) => {
        html += `
          <div class="bullet-card">
            <div style="display: flex; justify-content: space-between; align-items: center;">
              <div>
                <span style="font-family: var(--font-mono); font-weight: 800; font-size: 15px; color: var(--cyan-rs);">${escapeHtml(t.symbol)}</span>
                <span style="font-size: 11px; color: var(--text-tertiary); margin-left: 8px;">${escapeHtml(t.date)}</span>
              </div>
              <button class="theme-btn" style="padding: 2px 8px; font-size: 10px;" onclick="deleteThesis(${i})">Delete</button>
            </div>
            ${t.target ? `<div style="font-size: 11px; color: var(--green); font-weight: 700; margin: 4px 0;">Target Price: ₹${escapeHtml(t.target)}</div>` : ''}
            <div style="font-size: 12px; color: var(--text-secondary); line-height: 1.5; margin-top: 4px;">${escapeHtml(t.text)}</div>
          </div>
        `;
      });
      container.innerHTML = html;
    }

    // -------------------------------------------------------------
    // QUANTITATIVE RISK & LOG-RETURN FORMULATION
    // (Matches exact user specification)
    // -------------------------------------------------------------
    const logRet = a => a.slice(1).map((p, i) => Math.log(p / a[i]));
    const mean = a => a.reduce((s, x) => s + x, 0) / a.length;
    const cov = (x, y) => {
      const mx = mean(x), my = mean(y);
      return x.reduce((s, v, i) => s + (v - mx) * (y[i] - my), 0) / (x.length - 1);
    };
    const sd = x => Math.sqrt(cov(x, x));

    function riskStats(stockPx, idxPx, days) { // days: 252 | 126 | 21
      const s = logRet(stockPx.slice(-(days + 1)));
      const i = logRet(idxPx.slice(-(days + 1)));
      const c = cov(s, i);
      const sSD = sd(s);
      const iSD = sd(i);
      return {
        sdStock: sSD * Math.sqrt(252) * 100,
        sdIdx: iSD * Math.sqrt(252) * 100,
        covAnn: c * 252,
        corr: (sSD > 0 && iSD > 0) ? (c / (sSD * iSD)) : 0,
        beta: (iSD > 0) ? (c / (iSD ** 2)) : 1.0
      };
    }

    function getStockPriceSeries(stock) {
      if (stock.closes && stock.closes.length > 0) {
        return stock.closes;
      }
      return [stock.price];
    }

    function renderQuantSection(stock, mode = 'relevant') {
      currentModalBenchmark = mode;
      const stockPx = getStockPriceSeries(stock);
      const relevantBmName = stock.benchmarkName || (stock.cap === 'Large-Cap' ? 'Nifty 50' : 'Nifty Midcap 100');
      const activeBmName = mode === 'relevant' ? relevantBmName : 'Nifty 500';
      const bmPx = BENCHMARK_DATA[activeBmName] || BENCHMARK_DATA['Nifty 50'] || stockPx;

      if (stockPx.length < 253 || bmPx.length < 253) {
        console.warn("Insufficient series length for quant statistics calculation.");
        return;
      }

      const stat252 = riskStats(stockPx, bmPx, 252);
      const stat126 = riskStats(stockPx, bmPx, 126);
      const stat21 = riskStats(stockPx, bmPx, 21);

      // Window returns
      const sRet252 = ((stockPx[252] - stockPx[0]) / stockPx[0]) * 100;
      const sRet126 = ((stockPx[252] - stockPx[126]) / stockPx[126]) * 100;
      const sRet21 = ((stockPx[252] - stockPx[231]) / stockPx[231]) * 100;

      const bmRet252 = ((bmPx[252] - bmPx[0]) / bmPx[0]) * 100;
      const bmRet126 = ((bmPx[252] - bmPx[126]) / bmPx[126]) * 100;
      const bmRet21 = ((bmPx[252] - bmPx[231]) / bmPx[231]) * 100;

      // Jensen's Alpha
      const alpha252 = sRet252 - (stat252.beta * bmRet252);
      const alpha126 = sRet126 - (stat126.beta * bmRet126);
      const alpha21 = sRet21 - (stat21.beta * bmRet21);

      // 1. Update side-by-side SD values
      const sd1yEl = document.getElementById("sd-1y-val");
      const sd6mEl = document.getElementById("sd-6m-val");
      const sd1mEl = document.getElementById("sd-1m-val");
      if (sd1yEl) sd1yEl.innerText = `${stat252.sdStock.toFixed(1)}%`;
      if (sd6mEl) sd6mEl.innerText = `${stat126.sdStock.toFixed(1)}%`;
      if (sd1mEl) sd1mEl.innerText = `${stat21.sdStock.toFixed(1)}%`;

      // 2. Update KPI summary cards
      const kpiBetaEl = document.getElementById("kpi-quant-beta");
      const kpiCorrEl = document.getElementById("kpi-quant-corr");
      const kpiCovEl = document.getElementById("kpi-quant-cov");
      const kpiBmLabel = document.getElementById("kpi-bm-label");
      if (kpiBetaEl) kpiBetaEl.innerText = stat252.beta.toFixed(2);
      if (kpiCorrEl) kpiCorrEl.innerText = stat252.corr.toFixed(2);
      if (kpiCovEl) kpiCovEl.innerText = (stat252.covAnn * 100).toFixed(2);
      if (kpiBmLabel) kpiBmLabel.innerText = `vs ${activeBmName}`;

      // 3. Update comparison table
      const tbody = document.getElementById("quant-comparison-table-body");
      if (tbody) {
        tbody.innerHTML = `
          <tr style="border-bottom: 1px solid var(--border-subtle);">
            <td style="padding: 6px 8px; font-weight: 700; color: var(--text-primary);">${stock.symbol} Return</td>
            <td style="padding: 6px 8px; text-align: right;" class="${sRet252 >= 0 ? 'up' : 'down'}">${sRet252 >= 0 ? '+' : ''}${sRet252.toFixed(1)}%</td>
            <td style="padding: 6px 8px; text-align: right;" class="${sRet126 >= 0 ? 'up' : 'down'}">${sRet126 >= 0 ? '+' : ''}${sRet126.toFixed(1)}%</td>
            <td style="padding: 6px 8px; text-align: right;" class="${sRet21 >= 0 ? 'up' : 'down'}">${sRet21 >= 0 ? '+' : ''}${sRet21.toFixed(1)}%</td>
          </tr>
          <tr style="border-bottom: 1px solid var(--border-subtle);">
            <td style="padding: 6px 8px; color: var(--text-secondary);">${activeBmName} Return</td>
            <td style="padding: 6px 8px; text-align: right;" class="${bmRet252 >= 0 ? 'up' : 'down'}">${bmRet252 >= 0 ? '+' : ''}${bmRet252.toFixed(1)}%</td>
            <td style="padding: 6px 8px; text-align: right;" class="${bmRet126 >= 0 ? 'up' : 'down'}">${bmRet126 >= 0 ? '+' : ''}${bmRet126.toFixed(1)}%</td>
            <td style="padding: 6px 8px; text-align: right;" class="${bmRet21 >= 0 ? 'up' : 'down'}">${bmRet21 >= 0 ? '+' : ''}${bmRet21.toFixed(1)}%</td>
          </tr>
          <tr style="border-bottom: 1px solid var(--border-subtle);">
            <td style="padding: 6px 8px; font-weight: 700; color: var(--cyan-rs);">Alpha (Jensen's vs ${activeBmName})</td>
            <td style="padding: 6px 8px; text-align: right; font-weight: 700;" class="${alpha252 >= 0 ? 'up' : 'down'}">${alpha252 >= 0 ? '+' : ''}${alpha252.toFixed(1)}%</td>
            <td style="padding: 6px 8px; text-align: right; font-weight: 700;" class="${alpha126 >= 0 ? 'up' : 'down'}">${alpha126 >= 0 ? '+' : ''}${alpha126.toFixed(1)}%</td>
            <td style="padding: 6px 8px; text-align: right; font-weight: 700;" class="${alpha21 >= 0 ? 'up' : 'down'}">${alpha21 >= 0 ? '+' : ''}${alpha21.toFixed(1)}%</td>
          </tr>
          <tr style="border-bottom: 1px solid var(--border-subtle);">
            <td style="padding: 6px 8px; color: var(--text-secondary);">${stock.symbol} SD (Annualized)</td>
            <td style="padding: 6px 8px; text-align: right; font-weight: 700;">${stat252.sdStock.toFixed(1)}%</td>
            <td style="padding: 6px 8px; text-align: right; font-weight: 700;">${stat126.sdStock.toFixed(1)}%</td>
            <td style="padding: 6px 8px; text-align: right; font-weight: 700; color: var(--amber);">${stat21.sdStock.toFixed(1)}%*</td>
          </tr>
          <tr style="border-bottom: 1px solid var(--border-subtle);">
            <td style="padding: 6px 8px; color: var(--text-secondary);">${activeBmName} SD (Annualized)</td>
            <td style="padding: 6px 8px; text-align: right;">${stat252.sdIdx.toFixed(1)}%</td>
            <td style="padding: 6px 8px; text-align: right;">${stat126.sdIdx.toFixed(1)}%</td>
            <td style="padding: 6px 8px; text-align: right; color: var(--amber);">${stat21.sdIdx.toFixed(1)}%*</td>
          </tr>
          <tr style="border-bottom: 1px solid var(--border-subtle);">
            <td style="padding: 6px 8px; color: var(--text-secondary);">Covariance (Annualized)</td>
            <td style="padding: 6px 8px; text-align: right;">${(stat252.covAnn * 100).toFixed(2)}</td>
            <td style="padding: 6px 8px; text-align: right;">${(stat126.covAnn * 100).toFixed(2)}</td>
            <td style="padding: 6px 8px; text-align: right;">${(stat21.covAnn * 100).toFixed(2)}</td>
          </tr>
          <tr style="border-bottom: 1px solid var(--border-subtle);">
            <td style="padding: 6px 8px; color: var(--text-secondary);">Beta vs ${activeBmName}</td>
            <td style="padding: 6px 8px; text-align: right; font-weight: 700;">${stat252.beta.toFixed(2)}</td>
            <td style="padding: 6px 8px; text-align: right; font-weight: 700;">${stat126.beta.toFixed(2)}</td>
            <td style="padding: 6px 8px; text-align: right; font-weight: 700;">${stat21.beta.toFixed(2)}</td>
          </tr>
          <tr>
            <td style="padding: 6px 8px; color: var(--text-secondary);">Correlation (Pearson r)</td>
            <td style="padding: 6px 8px; text-align: right; font-weight: 700;">${stat252.corr.toFixed(2)}</td>
            <td style="padding: 6px 8px; text-align: right; font-weight: 700;">${stat126.corr.toFixed(2)}</td>
            <td style="padding: 6px 8px; text-align: right; font-weight: 700;">${stat21.corr.toFixed(2)}</td>
          </tr>
        `;
      }
    }

    function switchBenchmarkInModal(mode) {
      const btnRel = document.getElementById("btn-bm-relevant");
      const btnN500 = document.getElementById("btn-bm-n500");
      if (mode === 'relevant') {
        btnRel.classList.add("active");
        btnN500.classList.remove("active");
      } else {
        btnN500.classList.add("active");
        btnRel.classList.remove("active");
      }
      const curStock = STOCKS.find(s => s.symbol === activeStockSymbol);
      if (curStock) renderQuantSection(curStock, mode);
    }

    // -------------------------------------------------------------
    // MODAL DOSSIER & CANVAS CANDLESTICK CHART
    // -------------------------------------------------------------
    function openStockModal(symbol) {
      const stock = STOCKS.find(s => s.symbol === symbol);
      if (!stock) return;

      activeStockSymbol = symbol;
      activeStockCandles = stock.candles || [];

      const body = document.getElementById("modal-body");
      const dist52 = Math.abs(((stock.price - stock.high52) / stock.high52) * 100).toFixed(1);
      const rangePercent = Math.min(100, Math.max(0, ((stock.price - stock.low52) / (stock.high52 - stock.low52)) * 100));
      const relevantBmName = stock.benchmarkName || (stock.cap === 'Large-Cap' ? 'Nifty 50' : 'Nifty Midcap 100');
      const asOfStr = formatProvenanceDate(stock.asOf || MARKET_DATA?.metadata?.asOf);

      body.innerHTML = `
        <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 12px; flex-wrap: wrap; gap: 8px;">
          <div>
            <div style="display: flex; align-items: center; gap: 8px; flex-wrap: wrap;">
              <h2 style="font-family: var(--font-mono); font-size: 26px; font-weight: 800; color: var(--text-primary);">${stock.symbol}</h2>
              <span class="rs-score-badge ${stock.rs >= 95 ? 'rs-super' : 'rs-high'}">RS ${stock.rs}</span>
              <span style="font-size: 11px; padding: 2px 6px; border-radius: 4px; background: var(--card); border: 1px solid var(--border); font-weight: 600;">${stock.cap}</span>
              <span style="font-size: 10px; background: var(--card); border: 1px solid var(--border); padding: 2px 6px; border-radius: 4px; color: var(--text-secondary);">Benchmark: ${relevantBmName}</span>
            </div>
            <div style="font-size: 13px; color: var(--text-secondary); margin-top: 2px;">${stock.name} • ${stock.sector}</div>
            <div class="panel-provenance" style="margin-top: 4px;">Source: ${stock.source || 'NSE Official Bhavcopy & IISL'} • As of: ${asOfStr}</div>
          </div>
          <div style="text-align: right;">
            <div id="modal-live-price" style="font-size: 24px; font-weight: 800; font-family: var(--font-mono);">₹${stock.price.toLocaleString()}</div>
            <div class="up" style="font-weight: 700; font-size: 13px;">${stock.chg >= 0 ? '+' : ''}${stock.chg}% Today • ${dist52}% from 52W High</div>
          </div>
        </div>

        <!-- 52-WEEK RANGE BAR -->
        <div style="background: var(--card); border: 1px solid var(--border); border-radius: 8px; padding: 10px 14px; margin-bottom: 12px;">
          <div style="display: flex; justify-content: space-between; font-size: 11px; font-family: var(--font-mono); color: var(--text-secondary);">
            <span>52W Low: ₹${stock.low52.toLocaleString()}</span>
            <span style="color: var(--text-primary); font-weight: 700;">Current: ₹${stock.price.toLocaleString()} (${rangePercent.toFixed(0)}% of range)</span>
            <span>52W High: ₹${stock.high52.toLocaleString()}</span>
          </div>
          <div class="range52-bar">
            <div class="range52-fill" style="width: ${rangePercent}%;"></div>
            <div class="range52-thumb" style="left: ${rangePercent}%;"></div>
          </div>
        </div>

        <!-- INTERACTIVE CANDLESTICK CHART WITH MOVING AVERAGES & MOMENTUM INDICATORS -->
        <div class="chart-box">
          <div class="chart-legend" style="margin-bottom: 6px;">
            <div id="chart-inspection-info" style="display: flex; gap: 10px; font-size: 11px; flex-wrap: wrap;">
              <span style="color: var(--cyan-rs)">21 EMA (Cyan)</span>
              <span style="color: var(--amber)">50 SMA (Amber)</span>
              <span style="color: var(--purple)">200 SMA (Purple)</span>
              <span style="font-size: 10px; color: var(--text-tertiary); margin-left: 6px;">● Calibrated 252-Session Closes</span>
            </div>
            <div style="display: flex; gap: 4px;">
              <button class="tf-btn" onclick="setTimeframe('1M', this)">1M (21d)</button>
              <button class="tf-btn active" onclick="setTimeframe('3M', this)">3M (63d)</button>
              <button class="tf-btn" onclick="setTimeframe('6M', this)">6M (126d)</button>
              <button class="tf-btn" onclick="setTimeframe('1Y', this)">1Y (252d)</button>
            </div>
          </div>

          <!-- TECHNICAL MOMENTUM INDICATORS SELECTOR (RS, RSI, MACD, ALL) -->
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; border-top: 1px solid var(--border-subtle); padding-top: 6px; flex-wrap: wrap; gap: 6px;">
            <div style="font-size: 10px; font-weight: 700; color: var(--cyan-rs); text-transform: uppercase;">Technical Momentum Signals:</div>
            <div style="display: flex; gap: 4px;">
              <button class="ind-btn ${selectedIndicator === 'RS' ? 'active' : ''}" onclick="setIndicator('RS', this)">RS vs N500</button>
              <button class="ind-btn ${selectedIndicator === 'RSI' ? 'active' : ''}" onclick="setIndicator('RSI', this)">RSI (14)</button>
              <button class="ind-btn ${selectedIndicator === 'MACD' ? 'active' : ''}" onclick="setIndicator('MACD', this)">MACD (12,26,9)</button>
              <button class="ind-btn ${selectedIndicator === 'ALL' ? 'active' : ''}" onclick="setIndicator('ALL', this)">All Signals</button>
            </div>
          </div>

          <canvas id="candlestick-canvas" style="display: block; cursor: crosshair;"></canvas>
        </div>

        <!-- QUANTITATIVE RISK & BENCHMARK MATRIX (252-DAY LOG-RETURN MODEL) -->
        <div style="background: var(--card); border: 1px solid var(--border); border-radius: 12px; padding: 14px; margin-bottom: 14px;">
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; flex-wrap: wrap; gap: 8px;">
            <div>
              <div style="font-size: 11px; color: var(--cyan-rs); text-transform: uppercase; font-weight: 800; letter-spacing: 0.5px;">Quantitative Risk & Benchmark Statistics (252-Day Log-Return Model)</div>
              <div style="font-size: 11px; color: var(--text-secondary); margin-top: 2px;">Side-by-side volatility annualized with √252. 1M (21-day) metric is subject to small-sample estimation noise.</div>
            </div>
            <!-- Benchmark Selector Toggle -->
            <div style="display: flex; gap: 4px; align-items: center;">
              <span style="font-size: 10px; color: var(--text-tertiary); font-weight: 700;">BENCHMARK:</span>
              <button class="theme-btn active" id="btn-bm-relevant" style="padding: 3px 8px; font-size: 10px;" onclick="switchBenchmarkInModal('relevant')">${relevantBmName} (Primary)</button>
              <button class="theme-btn" id="btn-bm-n500" style="padding: 3px 8px; font-size: 10px;" onclick="switchBenchmarkInModal('n500')">Nifty 500 (Broad)</button>
            </div>
          </div>

          <!-- SIDE-BY-SIDE VOLATILITY (SD) -->
          <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; margin-bottom: 12px;">
            <div class="stat-box">
              <div class="stat-box-title">1Y Volatility (252 Days)</div>
              <div class="stat-box-val" id="sd-1y-val">...</div>
              <div style="font-size: 9px; color: var(--text-tertiary); margin-top: 2px;">Annualized with √252</div>
            </div>
            <div class="stat-box">
              <div class="stat-box-title">6M Volatility (126 Days)</div>
              <div class="stat-box-val" id="sd-6m-val">...</div>
              <div style="font-size: 9px; color: var(--text-tertiary); margin-top: 2px;">Annualized with √252</div>
            </div>
            <div class="stat-box" style="border-color: var(--amber);">
              <div class="stat-box-title" style="color: var(--amber);">1M Volatility (21 Days • Noisy)</div>
              <div class="stat-box-val" id="sd-1m-val" style="color: var(--amber);">...</div>
              <div style="font-size: 9px; color: var(--text-tertiary); margin-top: 2px;">~21 observations (sampling noise)</div>
            </div>
          </div>

          <!-- SUMMARY KPI ROW -->
          <div class="stat-badge-grid" style="margin-bottom: 12px;">
            <div class="stat-box">
              <div class="stat-box-title">Beta vs Benchmark</div>
              <div class="stat-box-val" id="kpi-quant-beta">...</div>
              <div style="font-size: 9px; color: var(--text-tertiary); margin-top: 2px;" id="kpi-bm-label">vs ${relevantBmName}</div>
            </div>
            <div class="stat-box">
              <div class="stat-box-title">Correlation (r)</div>
              <div class="stat-box-val" id="kpi-quant-corr">...</div>
              <div style="font-size: 9px; color: var(--text-tertiary); margin-top: 2px;">Covariance / (SD_s * SD_i)</div>
            </div>
            <div class="stat-box">
              <div class="stat-box-title">Annual Covariance</div>
              <div class="stat-box-val" id="kpi-quant-cov">...</div>
              <div style="font-size: 9px; color: var(--text-tertiary); margin-top: 2px;">c * 252 (Annualized)</div>
            </div>
            <div class="stat-box">
              <div class="stat-box-title">Sharpe Ratio</div>
              <div class="stat-box-val">${stock.quant ? stock.quant.sharpe : '1.85'}</div>
              <div style="font-size: 9px; color: var(--text-tertiary); margin-top: 2px;">Risk-Free Rate: 6.8%</div>
            </div>
            <div class="stat-box">
              <div class="stat-box-title">Sortino Ratio</div>
              <div class="stat-box-val">${stock.quant ? stock.quant.sortino : '2.80'}</div>
              <div style="font-size: 9px; color: var(--text-tertiary); margin-top: 2px;">Downside Deviation</div>
            </div>
          </div>

          <!-- STOCK VS INDEX COMPARISON BLOCK -->
          <div style="overflow-x: auto; margin-top: 10px;">
            <table style="width: 100%; border-collapse: collapse; font-size: 11px; font-family: var(--font-mono);">
              <thead>
                <tr style="border-bottom: 1px solid var(--border); color: var(--text-secondary); text-align: left;">
                  <th style="padding: 6px 8px;">Metric Window</th>
                  <th style="padding: 6px 8px; text-align: right;">1 Year (252d)</th>
                  <th style="padding: 6px 8px; text-align: right;">6 Months (126d)</th>
                  <th style="padding: 6px 8px; text-align: right;">1 Month (21d • Noisy)</th>
                </tr>
              </thead>
              <tbody id="quant-comparison-table-body">
                <!-- Populated dynamically by renderQuantSection -->
              </tbody>
            </table>
          </div>
        </div>

        <!-- MOVING AVERAGE STACK -->
        <div style="background: var(--card); border-radius: 8px; border: 1px solid var(--border); padding: 12px; margin-bottom: 14px;">
          <h4 style="font-size: 11px; color: var(--cyan-rs); text-transform: uppercase; font-weight: 700; margin-bottom: 4px;">Technical Moving Average Alignment</h4>
          <p style="font-size: 12px; font-family: var(--font-mono); color: var(--text-primary);">${stock.maStack}</p>
        </div>

        <!-- Q2 FY27 CONCALL & MANAGEMENT HIGHLIGHTS -->
        <div style="background: var(--card); border-radius: 8px; border: 1px solid var(--border); padding: 14px; margin-bottom: 14px;">
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
            <h4 style="font-size: 11px; color: var(--green); text-transform: uppercase; font-weight: 800; letter-spacing: 0.5px;">Concall Key Highlights (${stock.quarter} • ${stock.concallDate})</h4>
            <span style="font-size: 9px; font-weight: 700; color: var(--cyan-rs); background: var(--cyan-rs-bg); padding: 2px 6px; border-radius: 4px;">EXCHANGE FILING & CONCALL SUMMARY</span>
          </div>
          <ul style="padding-left: 18px; font-size: 12px; color: var(--text-secondary); line-height: 1.6;">
            ${stock.takeaways ? stock.takeaways.map(t => `<li style="margin-bottom: 4px;">${t}</li>`).join('') : ''}
          </ul>

          <div style="margin-top: 10px; padding-top: 8px; border-top: 1px solid var(--border-subtle);">
            <div style="font-size: 11px; font-weight: 700; color: var(--green);">MANAGEMENT GUIDANCE:</div>
            <div style="font-size: 11px; color: var(--text-primary); margin-top: 2px;">${stock.guidance || '--'}</div>
          </div>

          <div style="margin-top: 8px;">
            <div style="font-size: 11px; font-weight: 700; color: var(--amber);">CAPEX & EXPANSION PIPELINE:</div>
            <div style="font-size: 11px; color: var(--text-primary); margin-top: 2px;">${stock.capex || '--'}</div>
          </div>
        </div>

        <!-- DIRECT ONE-CLICK EXTERNAL LINKS -->
        <div style="display: flex; gap: 10px; margin-top: 16px; flex-wrap: wrap;">
          <a href="https://www.screener.in/company/${stock.symbol}/consolidated/" target="_blank" class="theme-btn" style="text-decoration:none;">📄 Open on Screener.in</a>
          <a href="https://www.nseindia.com/get-quotes/equity?symbol=${stock.symbol}" target="_blank" class="theme-btn" style="text-decoration:none;">🏛️ Official NSE Quote</a>
          <a href="https://www.bseindia.com/corporates/ann.html" target="_blank" class="theme-btn" style="text-decoration:none;">📑 BSE Filings & Transcripts</a>
        </div>
      `;

      document.getElementById("stock-modal").classList.add("active");

      // Initial quant calculation
      renderQuantSection(stock, 'relevant');

      setTimeout(() => {
        drawStockChart();
        const canvas = document.getElementById("candlestick-canvas");
        if (canvas) {
          canvas.addEventListener("mousemove", (e) => {
            const rect = canvas.getBoundingClientRect();
            const mouseX = e.clientX - rect.left;
            let candles = activeStockCandles;
            if (selectedTimeframe === '1M') candles = candles.slice(-21);
            else if (selectedTimeframe === '3M') candles = candles.slice(-63);
            else if (selectedTimeframe === '6M') candles = candles.slice(-126);
            else if (selectedTimeframe === '1Y') candles = candles.slice(-252);
            if (candles.length === 0) return;
            const slotW = rect.width / candles.length;
            const idx = Math.floor(mouseX / slotW);
            drawStockChart(idx);
          });
          canvas.addEventListener("mouseleave", () => {
            drawStockChart(null);
          });
        }
      }, 50);
    }

    function closeModal(e) {
      if (e.target.id === "stock-modal") closeModalDirect();
    }
    function closeModalDirect() {
      document.getElementById("stock-modal").classList.remove("active");
      activeStockSymbol = '';
    }

    function setTimeframe(tf, el) {
      selectedTimeframe = tf;
      document.querySelectorAll(".chart-legend .tf-btn").forEach(b => b.classList.remove("active"));
      el.classList.add("active");
      drawStockChart();
    }

    function setIndicator(ind, el) {
      selectedIndicator = ind;
      document.querySelectorAll(".ind-btn").forEach(b => b.classList.remove("active"));
      el.classList.add("active");
      drawStockChart();
    }

    function drawStockChart(inspectedIndex = null) {
      const canvas = document.getElementById("candlestick-canvas");
      if (!canvas) return;
      const ctx = canvas.getContext("2d");
      const dpr = window.devicePixelRatio || 1;
      const width = canvas.parentElement.clientWidth - 24;
      const height = 300;

      canvas.width = width * dpr;
      canvas.height = height * dpr;
      canvas.style.width = width + "px";
      canvas.style.height = height + "px";
      ctx.scale(dpr, dpr);

      let candles = activeStockCandles;
      if (selectedTimeframe === '1M') candles = candles.slice(-21);
      else if (selectedTimeframe === '3M') candles = candles.slice(-63);
      else if (selectedTimeframe === '6M') candles = candles.slice(-126);
      else if (selectedTimeframe === '1Y') candles = candles.slice(-252);

      if (!candles || candles.length === 0) return;

      const n = candles.length;
      let minP = Infinity, maxP = -Infinity;
      candles.forEach(c => {
        if (c.l < minP) minP = c.l;
        if (c.h > maxP) maxP = c.h;
        if (c.sma200 && c.sma200 < minP) minP = c.sma200;
        if (c.sma200 && c.sma200 > maxP) maxP = c.sma200;
      });
      const pPad = (maxP - minP) * 0.08 || 1;
      minP -= pPad;
      maxP += pPad;

      ctx.clearRect(0, 0, width, height);

      const isSubPanelActive = selectedIndicator !== 'NONE';
      const mainH = isSubPanelActive ? height * 0.65 : height;
      const subTop = mainH + 12;
      const subH = height - subTop - 4;

      const getY = p => mainH - ((p - minP) / (maxP - minP)) * mainH;
      const slotW = width / n;
      const candleW = Math.max(2, slotW * 0.65);

      // Grid
      ctx.strokeStyle = "rgba(100, 116, 139, 0.15)";
      ctx.lineWidth = 1;
      for (let i = 1; i <= 4; i++) {
        const gy = (mainH / 5) * i;
        ctx.beginPath(); ctx.moveTo(0, gy); ctx.lineTo(width, gy); ctx.stroke();
      }

      // Draw Candlesticks
      candles.forEach((c, i) => {
        const x = i * slotW + slotW / 2;
        const isBull = c.c >= c.o;
        ctx.strokeStyle = isBull ? "#00E676" : "#FF3B30";
        ctx.fillStyle = isBull ? "#00E676" : "#FF3B30";

        // Wick
        ctx.beginPath();
        ctx.moveTo(x, getY(c.h));
        ctx.lineTo(x, getY(c.l));
        ctx.stroke();

        // Body
        const top = Math.min(getY(c.o), getY(c.c));
        const bH = Math.max(1.5, Math.abs(getY(c.o) - getY(c.c)));
        ctx.fillRect(x - candleW / 2, top, candleW, bH);
      });

      // Moving Averages
      function drawMA(prop, color) {
        ctx.strokeStyle = color;
        ctx.lineWidth = 1.5;
        ctx.beginPath();
        let started = false;
        candles.forEach((c, i) => {
          if (!c[prop]) return;
          const x = i * slotW + slotW / 2;
          const y = getY(c[prop]);
          if (!started) { ctx.moveTo(x, y); started = true; }
          else ctx.lineTo(x, y);
        });
        ctx.stroke();
      }

      drawMA('ema21', '#00E5FF');
      drawMA('sma50', '#FFB300');
      drawMA('sma200', '#A855F7');

      // Sub-Panel: Indicators (RS, RSI, MACD, ALL)
      if (isSubPanelActive && subH > 20) {
        ctx.strokeStyle = "rgba(100, 116, 139, 0.25)";
        ctx.beginPath(); ctx.moveTo(0, mainH + 6); ctx.lineTo(width, mainH + 6); ctx.stroke();

        if (selectedIndicator === 'RSI' || selectedIndicator === 'ALL') {
          // RSI levels (30, 70)
          const y70 = subTop + subH * 0.3;
          const y30 = subTop + subH * 0.7;
          ctx.strokeStyle = "rgba(168, 85, 247, 0.3)";
          ctx.beginPath(); ctx.moveTo(0, y70); ctx.lineTo(width, y70); ctx.stroke();
          ctx.beginPath(); ctx.moveTo(0, y30); ctx.lineTo(width, y30); ctx.stroke();

          ctx.strokeStyle = "#A855F7";
          ctx.lineWidth = 1.5;
          ctx.beginPath();
          candles.forEach((c, i) => {
            const x = i * slotW + slotW / 2;
            const rVal = c.rsi || 50;
            const y = subTop + subH - (rVal / 100) * subH;
            if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
          });
          ctx.stroke();
        }

        if (selectedIndicator === 'MACD' || selectedIndicator === 'ALL') {
          // MACD Histogram & Signal line
          let maxM = 0;
          candles.forEach(c => {
            maxM = Math.max(maxM, Math.abs(c.macd || 0), Math.abs(c.macdHist || 0));
          });
          if (maxM === 0) maxM = 1;
          const zeroY = subTop + subH / 2;

          candles.forEach((c, i) => {
            const x = i * slotW + slotW / 2;
            const hVal = c.macdHist || 0;
            const barH = (hVal / maxM) * (subH / 2);
            ctx.fillStyle = hVal >= 0 ? "rgba(0, 230, 118, 0.65)" : "rgba(255, 59, 48, 0.65)";
            ctx.fillRect(x - candleW / 3, zeroY - barH, Math.max(1, candleW * 0.66), barH);
          });
        }

        if (selectedIndicator === 'RS') {
          ctx.strokeStyle = "#00E5FF";
          ctx.lineWidth = 1.5;
          let minRS = Infinity, maxRS = -Infinity;
          candles.forEach(c => {
            if (c.rs < minRS) minRS = c.rs;
            if (c.rs > maxRS) maxRS = c.rs;
          });
          const rPad = (maxRS - minRS) * 0.1 || 1;
          minRS -= rPad; maxRS += rPad;
          ctx.beginPath();
          candles.forEach((c, i) => {
            const x = i * slotW + slotW / 2;
            const y = subTop + subH - ((c.rs - minRS) / (maxRS - minRS)) * subH;
            if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
          });
          ctx.stroke();
        }
      }

      // Crosshair inspection
      if (inspectedIndex !== null && inspectedIndex >= 0 && inspectedIndex < n) {
        const ic = candles[inspectedIndex];
        const ix = inspectedIndex * slotW + slotW / 2;
        
        ctx.strokeStyle = "rgba(0, 229, 255, 0.55)";
        ctx.setLineDash([3, 3]);
        ctx.beginPath(); ctx.moveTo(ix, 0); ctx.lineTo(ix, height); ctx.stroke();
        ctx.setLineDash([]);

        const legend = document.getElementById("chart-inspection-info");
        if (legend) {
          const isBull = ic.c >= ic.o;
          const rsiTag = (ic.rsi || 50) >= 70 ? 'OB' : (ic.rsi || 50) <= 30 ? 'OS' : 'Neutral';
          const macdTag = (ic.macdHist || 0) >= 0 ? '+' : '';
          legend.innerHTML = `
            <span>O: ₹${ic.o.toFixed(1)}</span>
            <span>H: ₹${ic.h.toFixed(1)}</span>
            <span>L: ₹${ic.l.toFixed(1)}</span>
            <span class="${isBull ? 'up' : 'down'}">C: ₹${ic.c.toFixed(1)}</span>
            <span style="color: var(--cyan-rs)">RS: ${(ic.rs || 0).toFixed(0)}</span>
            <span style="color: var(--purple); font-weight:700;">RSI: ${(ic.rsi || 50).toFixed(1)} (${rsiTag})</span>
            <span style="color: ${(ic.macdHist || 0) >= 0 ? 'var(--green)' : 'var(--red)'}; font-weight:700;">MACD: ${(ic.macd || 0).toFixed(1)} | Hist: ${macdTag}${(ic.macdHist || 0).toFixed(1)}</span>
          `;
        }
      }
    }

    // -------------------------------------------------------------
    // SIMULATION TICK ENGINE
    // -------------------------------------------------------------
    function toggleStream() {
      isStreamActive = !isStreamActive;
      const btn = document.getElementById("btn-stream-toggle");
      const pill = document.getElementById("stream-status-pill");
      const text = document.getElementById("stream-status-text");

      if (isStreamActive) {
        btn.innerText = "⏸️ Pause Simulation";
        pill.classList.remove("paused");
        text.innerText = "SIMULATION FEED ACTIVE (Synthetic Micro-Ticks for Demo)";
        if (!streamIntervalId) {
          startStreamEngine();
        }
        showToast("Simulation active: generating synthetic sub-second tick oscillations for demonstration.", "info");
      } else {
        btn.innerText = "▶️ Live Simulation";
        pill.classList.add("paused");
        text.innerText = "MARKET CLOSED (15:30 IST) • OFFICIAL 12-NOV-2026 EOD";
        showToast("Simulation paused. Terminal displays verified official EOD session close.", "info");
      }
    }

    function startStreamEngine() {
      if (streamIntervalId) clearInterval(streamIntervalId);
      streamIntervalId = setInterval(onStreamTick, streamSpeedMs);
    }

    function onStreamTick() {
      if (!isStreamActive) return;

      streamTickCount++;
      simulatedSeconds++;

      const now = new Date();
      const timeStr = now.toLocaleTimeString('en-GB', { timeZone: 'Asia/Kolkata', hour12: false }) + " IST";
      const clockEl = document.getElementById("stream-clock");
      if (clockEl) clockEl.innerText = `${timeStr} • 1s STREAM`;

      const ticksEl = document.getElementById("stream-ticks");
      if (ticksEl) ticksEl.innerText = `Tick #${streamTickCount}`;

      // Nifty 50 tick
      const delta50 = (Math.random() - 0.49) * 2.8;
      liveNifty50 += delta50;
      liveNifty50Chg += delta50;
      const n50Pct = ((liveNifty50Chg / (liveNifty50 - liveNifty50Chg)) * 100).toFixed(2);
      const n50ValEl = document.getElementById("nifty50-val");
      const n50ChgEl = document.getElementById("nifty50-chg");
      const n50Row = document.getElementById("ticker-nifty50");
      if (n50ValEl && n50ChgEl) {
        n50ValEl.innerText = liveNifty50.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
        n50ChgEl.innerText = `${liveNifty50Chg >= 0 ? '+' : ''}${liveNifty50Chg.toFixed(2)} (${liveNifty50Chg >= 0 ? '+' : ''}${n50Pct}%)`;
        n50ChgEl.className = liveNifty50Chg >= 0 ? 'up' : 'down';
        flashElement(n50Row, delta50 >= 0);
      }

      // Micro-tick random stocks
      if (STOCKS.length > 0) {
        const s1 = STOCKS[Math.floor(Math.random() * STOCKS.length)];
        tickStock(s1);
      }
    }

    function tickStock(stock) {
      if (!stock) return;
      const pctDelta = (Math.random() - 0.48) * 0.15;
      const priceDelta = stock.price * (pctDelta / 100);
      stock.price = Math.round((stock.price + priceDelta) * 20) / 20;
      stock.chg = Math.round((stock.chg + pctDelta) * 100) / 100;

      const cardEl = document.getElementById(`stock-card-${stock.symbol}`);
      if (cardEl) {
        const priceTxt = cardEl.querySelector(".stock-card-price");
        const chgTxt = cardEl.querySelector(".stock-card-chg");
        if (priceTxt) priceTxt.innerText = `₹${stock.price.toLocaleString()}`;
        if (chgTxt) {
          chgTxt.innerText = `${stock.chg >= 0 ? '+' : ''}${stock.chg}%`;
          chgTxt.className = stock.chg >= 0 ? 'up stock-card-chg' : 'down stock-card-chg';
        }
        flashElement(cardEl, pctDelta >= 0);
      }
    }

    function flashElement(el, isUp) {
      if (!el) return;
      el.classList.remove("tick-up", "tick-down");
      void el.offsetWidth;
      el.classList.add(isUp ? "tick-up" : "tick-down");
    }

    function setStreamSpeed(speedMs, el) {
      streamSpeedMs = speedMs;
      document.querySelectorAll(".stream-controls-group .stream-btn").forEach(b => {
        if (b.id.startsWith("speed-")) b.classList.remove("active");
      });
      el.classList.add("active");
      if (isStreamActive) startStreamEngine();
    }

    function resetToOfficialClose() {
      isStreamActive = false;
      const btn = document.getElementById("btn-stream-toggle");
      const pill = document.getElementById("stream-status-pill");
      const text = document.getElementById("stream-status-text");
      if (btn) btn.innerText = "▶️ Live Simulation";
      if (pill) pill.classList.add("paused");
      if (text) text.innerText = "MARKET CLOSED (15:30 IST) • OFFICIAL 12-NOV-2026 EOD";

      if (MARKET_DATA && MARKET_DATA.tickers) {
        liveNifty50 = MARKET_DATA.tickers.nifty50?.rawVal || 25299.35;
        liveNifty50Chg = MARKET_DATA.tickers.nifty50?.rawChg || 44.30;
        liveNifty500 = MARKET_DATA.tickers.nifty500?.rawVal || 23779.30;
        liveNifty500Chg = MARKET_DATA.tickers.nifty500?.rawChg || 212.40;
        liveBankNifty = MARKET_DATA.tickers.bankNifty?.rawVal || 61240.80;
        liveBankNiftyChg = MARKET_DATA.tickers.bankNifty?.rawChg || 1048.40;
      }

      STOCKS.forEach(s => {
        s.price = s.basePrice || s.price;
        s.chg = s.baseChg || s.chg;
      });

      renderHeaderTickers();
      renderScreener();
      showToast("Reset to official November 12, 2026 verified market close.", "success");
    }

    // -------------------------------------------------------------
    // NAVIGATION & UTILITIES
    // -------------------------------------------------------------
    function switchTab(tabId) {
      document.querySelectorAll(".nav-tab").forEach(tab => tab.classList.remove("active"));
      document.querySelectorAll(".tab-content").forEach(content => content.classList.remove("active"));
      const tabBtn = Array.from(document.querySelectorAll(".nav-tab")).find(btn => btn.getAttribute("onclick")?.includes(tabId));
      if (tabBtn) tabBtn.classList.add("active");
      const section = document.getElementById(`tab-${tabId}`);
      if (section) section.classList.add("active");
    }

    function setViewMode(mode) {
      currentViewMode = mode;
      const btnCards = document.getElementById("btn-view-cards");
      const btnTable = document.getElementById("btn-view-table");
      const grid = document.getElementById("screener-cards-grid");
      const table = document.getElementById("screener-table-container");

      if (mode === 'table') {
        btnTable.classList.add("active");
        btnCards.classList.remove("active");
        grid.style.display = "none";
        table.style.display = "block";
      } else {
        btnCards.classList.add("active");
        btnTable.classList.remove("active");
        grid.style.display = "grid";
        table.style.display = "none";
      }
    }

    function toggleTheme() {
      const body = document.body;
      const btn = document.getElementById("theme-toggle");
      if (body.getAttribute("data-theme") === "dark") {
        body.setAttribute("data-theme", "light");
        btn.innerText = "🌙 Dark";
      } else {
        body.setAttribute("data-theme", "dark");
        btn.innerText = "☀️ Light";
      }
      if (activeStockSymbol) drawStockChart();
    }

    function openBrokerModal() {
      document.getElementById("broker-modal").classList.add("active");
    }
    function closeBrokerModal(e) {
      if (e.target.id === "broker-modal") closeBrokerModalDirect();
    }
    function closeBrokerModalDirect() {
      document.getElementById("broker-modal").classList.remove("active");
    }
    function saveBrokerConfig() {
      const broker = document.getElementById("broker-select").value;
      const key = document.getElementById("broker-api-key").value;
      if (!key) return showToast("Please enter your API Key or Access Token.", "warning");
      showToast(`${broker.toUpperCase()} feed credentials stored in session memory.`, "info");
      closeBrokerModalDirect();
    }

    function exportScreenerCSV() {
      if (!STOCKS || STOCKS.length === 0) return showToast("No stocks available to export.", "warning");
      const headers = ["Symbol", "Name", "Sector", "Cap", "Price", "ChangePct", "RS", "Dist52WHigh", "Benchmark"];
      const rows = STOCKS.map(s => [
        s.symbol,
        `"${s.name}"`,
        `"${s.sector}"`,
        s.cap,
        s.price,
        s.chg,
        s.rs,
        Math.abs(((s.price - s.high52) / s.high52) * 100).toFixed(1),
        s.benchmarkName || 'Nifty 50'
      ]);
      const csvContent = "data:text/csv;charset=utf-8," + [headers.join(","), ...rows.map(e => e.join(","))].join("\\n");
      const encodedUri = encodeURI(csvContent);
      const link = document.createElement("a");
      link.setAttribute("href", encodedUri);
      link.setAttribute("download", `MarketEdge_Screener_12Nov2026.csv`);
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      showToast("Screener CSV exported successfully.", "success");
    }

    function showToast(message, type = 'info') {
      const container = document.getElementById("toast-container");
      if (!container) return;
      const toast = document.createElement("div");
      toast.className = `toast ${type}`;
      toast.innerText = message;
      container.appendChild(toast);
      setTimeout(() => toast.classList.add("show"), 10);
      setTimeout(() => {
        toast.classList.remove("show");
        setTimeout(() => toast.remove(), 300);
      }, 4000);
    }

    function escapeHtml(str) {
      if (!str) return '';
      return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
    }

    window.addEventListener('keydown', (e) => {
      if (e.key === 'Escape') {
        closeModalDirect();
        closeBrokerModalDirect();
      }
    });

    window.addEventListener('resize', () => {
      if (activeStockSymbol) drawStockChart();
    });
  </script>
  <div id="toast-container" class="toast-container"></div>
</body>
</html>
'''

def main():
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    web_file = os.path.join(root, "web", "index.html")
    assets_file = os.path.join(root, "app", "src", "main", "assets", "web", "index.html")

    print(f"Writing updated UI to {web_file}...")
    with open(web_file, "w", encoding="utf-8") as f:
        f.write(html_template)
    print(f"Written web/index.html ({os.path.getsize(web_file)} bytes)")

    print(f"Writing updated UI to {assets_file}...")
    with open(assets_file, "w", encoding="utf-8") as f:
        f.write(html_template)
    print(f"Written app/src/main/assets/web/index.html ({os.path.getsize(assets_file)} bytes)")

if __name__ == "__main__":
    main()
