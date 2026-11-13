/**
 * lib/nseCalendar.js
 * NSE Equity Trading Calendar & IST Date Utilities.
 * Skips weekends (Sat/Sun) and official NSE holidays when checking latest session & staleness.
 */

const IST_OFFSET_MS = 5.5 * 60 * 60 * 1000;

// Official NSE Equity Trading Holidays (2025-2026)
const NSE_HOLIDAYS = new Set([
  // 2025
  '2025-02-26', '2025-03-14', '2025-03-31', '2025-04-10', '2025-04-14',
  '2025-04-18', '2025-05-01', '2025-08-15', '2025-08-27', '2025-10-02',
  '2025-10-21', '2025-10-22', '2025-11-05', '2025-12-25',
  // 2026 (including verified Oct 20 Dussehra & Nov 10 Balipratipada)
  '2026-01-26', '2026-03-03', '2026-03-26', '2026-03-31', '2026-04-03',
  '2026-04-14', '2026-05-01', '2026-05-28', '2026-06-26', '2026-09-14',
  '2026-10-02', '2026-10-20', '2026-11-10', '2026-11-24', '2026-12-25'
]);

function getISTComponents(date = new Date()) {
  const ist = new Date(date.getTime() + IST_OFFSET_MS);
  const year = ist.getUTCFullYear();
  const month = String(ist.getUTCMonth() + 1).padStart(2, '0');
  const day = String(ist.getUTCDate()).padStart(2, '0');
  const hours = ist.getUTCHours();
  const minutes = ist.getUTCMinutes();
  const seconds = ist.getUTCSeconds();
  const dayOfWeek = ist.getUTCDay(); // 0=Sun, 6=Sat
  return {
    dateStr: `${year}-${month}-${day}`,
    hours,
    minutes,
    seconds,
    dayOfWeek
  };
}

function toISTDateStr(date) {
  return getISTComponents(new Date(date)).dateStr;
}

function formatISTDateTime(dateInput) {
  const d = new Date(dateInput);
  if (Number.isNaN(d.getTime())) return String(dateInput || '--');
  const ist = new Date(d.getTime() + IST_OFFSET_MS);
  const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
  const day = String(ist.getUTCDate()).padStart(2, '0');
  const mon = months[ist.getUTCMonth()];
  const year = ist.getUTCFullYear();
  const hh = String(ist.getUTCHours()).padStart(2, '0');
  const mm = String(ist.getUTCMinutes()).padStart(2, '0');
  return `${day} ${mon} ${year}, ${hh}:${mm} IST`;
}

function isTradingDay(dateStr) {
  const [y, m, d] = dateStr.split('-').map(Number);
  const utcDate = new Date(Date.UTC(y, m - 1, d));
  const dow = utcDate.getUTCDay();
  if (dow === 0 || dow === 6) return false;
  if (NSE_HOLIDAYS.has(dateStr)) return false;
  return true;
}

function prevCalendarDay(dateStr) {
  const [y, m, d] = dateStr.split('-').map(Number);
  const dt = new Date(Date.UTC(y, m - 1, d - 1));
  const yy = dt.getUTCFullYear();
  const mm = String(dt.getUTCMonth() + 1).padStart(2, '0');
  const dd = String(dt.getUTCDate()).padStart(2, '0');
  return `${yy}-${mm}-${dd}`;
}

function nextCalendarDay(dateStr) {
  const [y, m, d] = dateStr.split('-').map(Number);
  const dt = new Date(Date.UTC(y, m - 1, d + 1));
  const yy = dt.getUTCFullYear();
  const mm = String(dt.getUTCMonth() + 1).padStart(2, '0');
  const dd = String(dt.getUTCDate()).padStart(2, '0');
  return `${yy}-${mm}-${dd}`;
}

/**
 * Determines the latest expected COMPLETED NSE EOD trading session date (YYYY-MM-DD in IST).
 * NSE equity session closes at 15:30 IST; we consider EOD settled by 15:40 IST.
 */
function getLatestCompletedSessionDate(now = new Date()) {
  const { dateStr, hours, minutes } = getISTComponents(now);
  const afterClose = hours > 15 || (hours === 15 && minutes >= 40);

  let cursor = dateStr;
  if (isTradingDay(cursor) && afterClose) {
    return cursor;
  }

  // Step backward until a trading day is found
  cursor = prevCalendarDay(cursor);
  for (let i = 0; i < 30; i++) {
    if (isTradingDay(cursor)) return cursor;
    cursor = prevCalendarDay(cursor);
  }
  return cursor;
}

/**
 * Counts how many trading days (laterDateStr is strictly after earlierDateStr) separate two dates.
 * Returns 0 if earlierDateStr >= laterDateStr.
 */
function countTradingDaysBetween(earlierDateStr, laterDateStr) {
  if (!earlierDateStr || !laterDateStr) return 999;
  if (earlierDateStr >= laterDateStr) return 0;

  let count = 0;
  let cursor = nextCalendarDay(earlierDateStr);
  let guard = 0;
  while (cursor <= laterDateStr && guard < 400) {
    if (isTradingDay(cursor)) count++;
    cursor = nextCalendarDay(cursor);
    guard++;
  }
  return count;
}

function sessionCloseISO(dateStr) {
  return `${dateStr}T15:30:00+05:30`;
}

module.exports = {
  NSE_HOLIDAYS,
  getISTComponents,
  toISTDateStr,
  formatISTDateTime,
  isTradingDay,
  getLatestCompletedSessionDate,
  countTradingDaysBetween,
  sessionCloseISO
};
