import { useState, useEffect, useCallback } from "react";
import { isAdmin } from "../services/authService";
import { fetchReport, importCsv } from "../services/reportService";
import "./Reports.css";

const MAX_ADMIN_COMMENT = 500;

// ── CSV export (frontend-side) ──────────────────────────────────────────────
function exportCsv(rows, from, to) {
  const cols = [
    "Вработен", "Оддел", "Денови присутни",
    "Работни часови", "Доцнења", "Одобрени отсуства",
  ];
  const lines = [
    `Период;${from} – ${to}`,
    cols.join(";"),
    ...rows.map((r) =>
      [
        r.employeeName,
        r.department ?? "—",
        r.daysPresent,
        r.workedHours.toFixed(2),
        r.lateCount,
        r.leaveRequestCount,
      ].join(";")
    ),
  ];
  const blob = new Blob(["\ufeff" + lines.join("\n")], {
    type: "text/csv;charset=utf-8",
  });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `izvestaj_${from}_${to}.csv`;
  a.click();
  URL.revokeObjectURL(url);
}

// ── PDF export (frontend-side, no library needed) ──────────────────────────
function exportPdf(rows, from, to) {
  const rowsHtml = rows
    .map(
      (r) => `
      <tr>
        <td>${r.employeeName}</td>
        <td>${r.department ?? "—"}</td>
        <td>${r.daysPresent}</td>
        <td>${r.workedHours.toFixed(2)}</td>
        <td>${r.lateCount}</td>
        <td>${r.leaveRequestCount}</td>
      </tr>`
    )
    .join("");

  const html = `<!DOCTYPE html>
<html lang="mk">
<head>
  <meta charset="UTF-8"/>
  <title>Извештај ${from} – ${to}</title>
  <style>
    body { font-family: Arial, sans-serif; padding: 24px; }
    h2 { margin-bottom: 4px; }
    p { margin: 0 0 16px; color: #555; }
    table { width: 100%; border-collapse: collapse; }
    th, td { border: 1px solid #ccc; padding: 8px 10px; text-align: left; }
    th { background: #f0f0f0; }
  </style>
</head>
<body>
  <h2>Извештај за присуство</h2>
  <p>Период: ${from} – ${to}</p>
  <table>
    <thead>
      <tr>
        <th>Вработен</th><th>Оддел</th><th>Денови присутни</th>
        <th>Работни часови</th><th>Доцнења</th><th>Одобрени отсуства</th>
      </tr>
    </thead>
    <tbody>${rowsHtml}</tbody>
  </table>
</body>
</html>`;

  const win = window.open("", "_blank");
  win.document.write(html);
  win.document.close();
  win.focus();
  win.print();
  win.close();
}

// ── Main component ─────────────────────────────────────────────────────────
export default function Reports() {
  const admin = isAdmin();

  const today = new Date().toISOString().slice(0, 10);
  const firstOfMonth = today.slice(0, 8) + "01";

  const [from, setFrom] = useState(firstOfMonth);
  const [to, setTo] = useState(today);
  const [department, setDepartment] = useState("all");

  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const [importFile, setImportFile] = useState(null);
  const [importMsg, setImportMsg] = useState(null);
  const [importError, setImportError] = useState(null);
  const [importing, setImporting] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await fetchReport({ from, to, department });
      setRows(Array.isArray(data) ? data : []);
    } catch (e) {
      setError(e.message);
      setRows([]);
    } finally {
      setLoading(false);
    }
  }, [from, to, department]);

  useEffect(() => {
    load();
  }, [load]);

  async function handleImport() {
    if (!importFile) return;
    setImporting(true);
    setImportMsg(null);
    setImportError(null);
    try {
      const msg = await importCsv(importFile);
      setImportMsg(msg);
      setImportFile(null);
      await load();
    } catch (e) {
      setImportError(e.message);
    } finally {
      setImporting(false);
    }
  }

  function fmtDate(iso) {
    if (!iso) return "";
    const [y, m, d] = iso.split("-");
    return `${d}.${m}.${y}`;
  }

  return (
    <div className="reports-page">
      <section className="reports__shell" aria-labelledby="reports-main-title">
        <h2 id="reports-main-title" className="reports__titlebar">
          Извештаи и статистики
        </h2>

        <div className="reports__body">
          {/* ── Filters ── */}
          <div className="reports__filters">
            <div className="reports__field">
              <label htmlFor="reports-from">Од датум</label>
              <input
                id="reports-from"
                type="date"
                value={from}
                onChange={(e) => setFrom(e.target.value)}
              />
            </div>
            <div className="reports__field">
              <label htmlFor="reports-to">До датум</label>
              <input
                id="reports-to"
                type="date"
                value={to}
                onChange={(e) => setTo(e.target.value)}
              />
            </div>

            {admin && (
              <div className="reports__field reports__field--grow">
                <label htmlFor="reports-dept">Оддел</label>
                <select
                  id="reports-dept"
                  value={department}
                  onChange={(e) => setDepartment(e.target.value)}
                >
                  <option value="all">Сите</option>
                  <option value="Финансии">Финансии</option>
                  <option value="ИТ">ИТ</option>
                  <option value="Менаџер">Менаџер</option>
                </select>
              </div>
            )}

            <div className="reports__field reports__field--btn">
              <span className="reports__label-spacer" aria-hidden>&nbsp;</span>
              <button
                type="button"
                className="reports__btn-csv"
                onClick={() => exportCsv(rows, fmtDate(from), fmtDate(to))}
                disabled={rows.length === 0}
              >
                Преземи CSV
              </button>
              <button
                type="button"
                className="reports__btn-pdf"
                onClick={() => exportPdf(rows, fmtDate(from), fmtDate(to))}
                disabled={rows.length === 0}
              >
                Преземи PDF
              </button>
            </div>
          </div>

          {/* ── Table ── */}
          <div className="reports__table-card">
            <h3 className="reports__table-head">Податоци за вработени</h3>

            {loading && <p className="reports__loading">Вчитување…</p>}
            {error && <p className="reports__error">{error}</p>}

            {!loading && !error && (
              <div className="reports__table-scroll">
                <table className="reports__table">
                  <thead>
                    <tr>
                      <th>Вработен</th>
                      {admin && <th>Оддел</th>}
                      <th>Денови присутни</th>
                      <th>Работни часови</th>
                      <th>Доцнења</th>
                      <th>Одобрени отсуства</th>
                    </tr>
                  </thead>
                  <tbody>
                    {rows.length === 0 ? (
                      <tr>
                        <td colSpan={admin ? 6 : 5} className="reports__empty">
                          Нема податоци за избраните филтри.
                        </td>
                      </tr>
                    ) : (
                      rows.map((r) => (
                        <tr key={r.employeeId}>
                          <td className="reports__cell-name">{r.employeeName}</td>
                          {admin && <td>{r.department ?? "—"}</td>}
                          <td>{r.daysPresent}</td>
                          <td className="reports__cell-mono">
                            {r.workedHours.toFixed(2)} ч
                          </td>
                          <td>{r.lateCount}</td>
                          <td>{r.leaveRequestCount}</td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          {/* ── CSV Import (admin only) ── */}
          {admin && (
            <div className="reports__import-card">
              <h3 className="reports__table-head">Увоз на податоци (CSV)</h3>
              <p className="reports__import-hint">
                Очекуван формат (со точка-запирка):{" "}
                <code>employeeId;checkIn;checkOut;status;workedHours</code>
                <br />
                Пример: <code>1;2026-04-01T08:00;2026-04-01T16:00;PRESENT;8.0</code>
              </p>
              <div className="reports__import-row">
                <input
                  type="file"
                  accept=".csv"
                  onChange={(e) => setImportFile(e.target.files?.[0] ?? null)}
                />
                <button
                  type="button"
                  className="reports__btn-import"
                  onClick={handleImport}
                  disabled={!importFile || importing}
                >
                  {importing ? "Увозување…" : "Увези"}
                </button>
              </div>
              {importMsg && <p className="reports__import-ok">{importMsg}</p>}
              {importError && <p className="reports__error">{importError}</p>}
            </div>
          )}
        </div>
      </section>
    </div>
  );
}