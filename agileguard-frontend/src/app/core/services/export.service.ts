import { Injectable } from '@angular/core';
import { SprintHealthReport, GapReport, GapFinding, WorkflowRun } from '../models';

/**
 * ExportService — produces downloadable artefacts from gap report data.
 *
 * Supported formats:
 *   • CSV   — one row per gap finding, opens in Excel / Sheets
 *   • JSON  — full structured report dump
 *   • PDF   — browser-native print-to-PDF of a generated HTML document
 *   • HTML  — standalone shareable report page (email-ready)
 */
@Injectable({ providedIn: 'root' })
export class ExportService {

  // ── CSV ────────────────────────────────────────────────────────────────────

  /**
   * Builds a CSV string from all gap findings in a sprint health report
   * and triggers a browser download.
   */
  exportGapsAsCsv(report: SprintHealthReport): void {
    const rows: string[][] = [];

    // Header row
    rows.push([
      'Issue Key', 'Issue Summary', 'Quality Score', 'Flagged',
      'Severity', 'Gap Type', 'Message', 'Suggested Action',
      'Critical Count', 'Error Count', 'Warning Count', 'Detected At'
    ]);

    // One row per finding; if a story has no findings, include it with blanks
    report.issueReports.forEach(r => {
      if (r.findings.length === 0) {
        rows.push([
          r.issueKey, this.esc(r.issueSummary), String(r.qualityScore),
          r.flagged ? 'YES' : 'NO',
          '', '', 'No gaps detected', '',
          '0', '0', '0', ''
        ]);
      } else {
        r.findings.forEach(f => {
          rows.push([
            r.issueKey, this.esc(r.issueSummary), String(r.qualityScore),
            r.flagged ? 'YES' : 'NO',
            f.severity, f.type,
            this.esc(f.message), this.esc(f.suggestedAction),
            String(r.criticalCount), String(r.errorCount), String(r.warningCount),
            f.detectedAt ? new Date(f.detectedAt).toLocaleString() : ''
          ]);
        });
      }
    });

    // Summary rows at the bottom
    rows.push([]);
    rows.push(['SUMMARY']);
    rows.push(['Project Key', report.projectKey]);
    rows.push(['Total Issues', String(report.totalIssues)]);
    rows.push(['Issues With Gaps', String(report.issuesWithGaps)]);
    rows.push(['Flagged Issues', String(report.flaggedIssues)]);
    rows.push(['Average Quality Score', String(Math.round(report.averageQualityScore))]);
    rows.push(['Overall Health Score', String(report.overallHealthScore)]);
    rows.push(['Generated At', new Date().toLocaleString()]);

    const csv = rows.map(r => r.join(',')).join('\r\n');
    this.download(
      'text/csv;charset=utf-8;',
      `AgileGuard_GapReport_${report.projectKey}_${this.dateTag()}.csv`,
      '\uFEFF' + csv   // BOM for Excel UTF-8 compatibility
    );
  }

  /**
   * Exports only the flagged / critical stories as a focused CSV.
   */
  exportFlaggedAsCsv(report: SprintHealthReport): void {
    const flagged: SprintHealthReport = {
      ...report,
      issueReports: report.issueReports.filter(r => r.flagged)
    };

    if (flagged.issueReports.length === 0) {
      alert('No flagged stories to export.');
      return;
    }

    const rows: string[][] = [];
    rows.push(['Issue Key', 'Summary', 'Score', 'Critical', 'Errors', 'Warnings', 'Top Issue', 'Suggested Action']);
    flagged.issueReports.forEach(r => {
      const topFinding = r.findings.find(f => f.severity === 'CRITICAL') ?? r.findings[0];
      rows.push([
        r.issueKey,
        this.esc(r.issueSummary),
        String(r.qualityScore),
        String(r.criticalCount),
        String(r.errorCount),
        String(r.warningCount),
        this.esc(topFinding?.message ?? ''),
        this.esc(topFinding?.suggestedAction ?? '')
      ]);
    });

    const csv = rows.map(r => r.join(',')).join('\r\n');
    this.download(
      'text/csv;charset=utf-8;',
      `AgileGuard_FlaggedStories_${report.projectKey}_${this.dateTag()}.csv`,
      '\uFEFF' + csv
    );
  }

  /**
   * Exports GitHub Actions workflow runs as CSV.
   */
  exportWorkflowRunsAsCsv(runs: WorkflowRun[], projectKey: string): void {
    const rows: string[][] = [];
    rows.push(['Run ID', 'Workflow', 'Branch', 'Status', 'Conclusion', 'Triggered By', 'JIRA Issue', 'Duration (s)', 'Started At']);
    runs.forEach(r => {
      rows.push([
        String(r.id), this.esc(r.workflowName), this.esc(r.headBranch),
        r.status, r.conclusion ?? '',
        r.triggeredBy ?? '', r.jiraIssueKey ?? '',
        String(r.durationSeconds),
        r.startedAt ? new Date(r.startedAt).toLocaleString() : ''
      ]);
    });
    const csv = rows.map(r => r.join(',')).join('\r\n');
    this.download(
      'text/csv;charset=utf-8;',
      `AgileGuard_CICDRuns_${projectKey}_${this.dateTag()}.csv`,
      '\uFEFF' + csv
    );
  }

  // ── JSON ───────────────────────────────────────────────────────────────────

  /** Exports the full SprintHealthReport as a formatted JSON file. */
  exportAsJson(report: SprintHealthReport): void {
    const payload = {
      exportedAt: new Date().toISOString(),
      exportedBy: 'AgileGuard Platform',
      report
    };
    this.download(
      'application/json',
      `AgileGuard_GapReport_${report.projectKey}_${this.dateTag()}.json`,
      JSON.stringify(payload, null, 2)
    );
  }

  // ── PDF (print) ────────────────────────────────────────────────────────────

  /**
   * Opens a new browser window containing a formatted HTML gap report,
   * then triggers the print dialog so the user can save as PDF.
   * No external library required.
   */
  exportAsPdf(report: SprintHealthReport, runs: WorkflowRun[] = []): void {
    const html = this.buildHtmlReport(report, runs);
    const win = window.open('', '_blank', 'width=1100,height=800');
    if (!win) {
      alert('Please allow pop-ups for this site to download the PDF report.');
      return;
    }
    win.document.open();
    win.document.write(html);
    win.document.close();
    // Give styles a moment to render before opening print dialog
    setTimeout(() => {
      win.focus();
      win.print();
    }, 600);
  }

  // ── HTML standalone ────────────────────────────────────────────────────────

  /** Downloads a self-contained HTML report file that can be emailed or shared. */
  exportAsHtml(report: SprintHealthReport, runs: WorkflowRun[] = []): void {
    const html = this.buildHtmlReport(report, runs);
    this.download(
      'text/html;charset=utf-8;',
      `AgileGuard_GapReport_${report.projectKey}_${this.dateTag()}.html`,
      html
    );
  }

  // ── HTML report builder ────────────────────────────────────────────────────

  private buildHtmlReport(report: SprintHealthReport, runs: WorkflowRun[]): string {
    const scoreColor = (s: number) => s >= 75 ? '#16a34a' : s >= 50 ? '#d97706' : '#dc2626';
    const sevColor   = (s: string) => ({ CRITICAL: '#dc2626', ERROR: '#d97706', WARNING: '#b45309', INFO: '#0ea5e9' }[s] ?? '#64748b');
    const sevBg      = (s: string) => ({ CRITICAL: '#fee2e2', ERROR: '#fef3c7', WARNING: '#fffbeb', INFO: '#e0f2fe' }[s] ?? '#f1f5f9');
    const badge      = (txt: string, bg: string, fg: string) =>
      `<span style="display:inline-block;padding:2px 10px;border-radius:9999px;font-size:11px;font-weight:700;background:${bg};color:${fg}">${txt}</span>`;

    const healthPct = report.overallHealthScore;
    const healthFill = scoreColor(healthPct);

    const now = new Date().toLocaleString('en-IN', { dateStyle: 'full', timeStyle: 'short' });

    // ── stat cards ─────────────────────────────────────────────────────
    const stats = [
      { v: healthPct + '%', l: 'Health Score', c: scoreColor(healthPct) },
      { v: String(report.totalIssues), l: 'Total Issues', c: '#1e2761' },
      { v: String(report.issuesWithGaps), l: 'Issues With Gaps', c: '#d97706' },
      { v: String(report.flaggedIssues), l: '🚨 Flagged', c: '#dc2626' },
      { v: Math.round(report.averageQualityScore) + '', l: 'Avg Quality Score', c: scoreColor(report.averageQualityScore) },
    ];

    const statCards = stats.map(s => `
      <div style="background:#fff;border:1px solid #e2e8f0;border-radius:12px;padding:20px 24px;text-align:center;min-width:140px;flex:1">
        <div style="font-size:32px;font-weight:800;color:${s.c}">${s.v}</div>
        <div style="font-size:12px;color:#64748b;margin-top:4px">${s.l}</div>
      </div>`).join('');

    // ── gap findings table rows ────────────────────────────────────────
    const issueRows = report.issueReports.map(r => {
      const scoreC = scoreColor(r.qualityScore);
      const flagBadge = r.flagged ? badge('🚨 FLAGGED', '#fee2e2', '#991b1b') : badge('✅ OK', '#dcfce7', '#166534');

      const findingRows = r.findings.length === 0
        ? `<tr><td colspan="4" style="color:#64748b;font-style:italic;padding:8px 12px">No gaps detected</td></tr>`
        : r.findings.map(f => `
          <tr style="border-top:1px solid #f1f5f9">
            <td style="padding:8px 12px">
              <span style="display:inline-block;padding:2px 8px;border-radius:4px;font-size:11px;font-weight:700;background:${sevBg(f.severity)};color:${sevColor(f.severity)}">${f.severity}</span>
            </td>
            <td style="padding:8px 12px;font-size:13px;color:#1e293b">${f.type?.replace(/_/g, ' ') ?? ''}</td>
            <td style="padding:8px 12px;font-size:13px;color:#334155">${f.message}</td>
            <td style="padding:8px 12px;font-size:12px;color:#64748b">${f.suggestedAction}</td>
          </tr>`).join('');

      return `
        <div style="margin-bottom:20px;border:1px solid ${r.flagged ? '#fca5a5' : '#e2e8f0'};border-radius:12px;overflow:hidden;background:${r.flagged ? '#fff5f5' : '#fff'}">
          <div style="display:flex;align-items:center;justify-content:space-between;padding:14px 20px;background:${r.flagged ? '#fff1f2' : '#f8fafc'};border-bottom:1px solid #e2e8f0">
            <div style="display:flex;align-items:center;gap:14px">
              <span style="font-weight:700;font-size:15px;color:#1e2761">${r.issueKey}</span>
              <span style="font-size:13px;color:#475569">${r.issueSummary}</span>
              ${flagBadge}
            </div>
            <div style="display:flex;align-items:center;gap:10px">
              <span style="font-size:11px;color:#64748b">${badge(r.criticalCount + ' CRIT', '#fee2e2', '#991b1b')} ${badge(r.errorCount + ' ERR', '#fef3c7', '#92400e')} ${badge(r.warningCount + ' WARN', '#f1f5f9', '#475569')}</span>
              <div style="width:44px;height:44px;border-radius:50%;display:flex;align-items:center;justify-content:center;border:3px solid ${scoreC};font-weight:800;font-size:14px;color:${scoreC}">${r.qualityScore}</div>
            </div>
          </div>
          <table style="width:100%;border-collapse:collapse;font-size:13px">
            <thead>
              <tr style="background:#f8fafc">
                <th style="text-align:left;padding:8px 12px;font-size:11px;color:#64748b;font-weight:600;text-transform:uppercase;letter-spacing:0.05em;width:90px">Severity</th>
                <th style="text-align:left;padding:8px 12px;font-size:11px;color:#64748b;font-weight:600;text-transform:uppercase;letter-spacing:0.05em;width:180px">Gap Type</th>
                <th style="text-align:left;padding:8px 12px;font-size:11px;color:#64748b;font-weight:600;text-transform:uppercase;letter-spacing:0.05em">Message</th>
                <th style="text-align:left;padding:8px 12px;font-size:11px;color:#64748b;font-weight:600;text-transform:uppercase;letter-spacing:0.05em;width:240px">Suggested Action</th>
              </tr>
            </thead>
            <tbody>${findingRows}</tbody>
          </table>
        </div>`;
    }).join('');

    // ── aging stories ──────────────────────────────────────────────────
    const agingSection = report.agingIssueKeys?.length > 0 ? `
      <div style="background:#fff5f5;border:1px solid #fca5a5;border-radius:12px;padding:18px 22px;margin-bottom:28px">
        <div style="font-weight:700;font-size:15px;color:#991b1b;margin-bottom:10px">⏰ Aging Stories — Require Immediate Attention</div>
        <div style="display:flex;flex-wrap:wrap;gap:8px">
          ${report.agingIssueKeys.map(k =>
            `<span style="display:inline-block;padding:4px 12px;border-radius:6px;font-size:13px;font-weight:600;background:#fee2e2;color:#991b1b">${k}</span>`
          ).join('')}
        </div>
        <div style="font-size:12px;color:#b91c1c;margin-top:10px">These stories have been in the same JIRA status beyond the configured aging threshold. Review in your next standup.</div>
      </div>` : '';

    // ── CI/CD runs table ───────────────────────────────────────────────
    const ciSection = runs.length > 0 ? `
      <h2 style="font-size:18px;font-weight:700;color:#1e2761;margin:36px 0 14px">🔧 CI/CD Pipeline Runs</h2>
      <div style="background:#fff;border:1px solid #e2e8f0;border-radius:12px;overflow:hidden">
        <table style="width:100%;border-collapse:collapse;font-size:13px">
          <thead>
            <tr style="background:#f8fafc;border-bottom:1px solid #e2e8f0">
              <th style="text-align:left;padding:10px 16px;font-size:11px;color:#64748b;font-weight:600;text-transform:uppercase;letter-spacing:0.05em">Workflow</th>
              <th style="text-align:left;padding:10px 16px;font-size:11px;color:#64748b;font-weight:600;text-transform:uppercase;letter-spacing:0.05em">Branch</th>
              <th style="text-align:left;padding:10px 16px;font-size:11px;color:#64748b;font-weight:600;text-transform:uppercase;letter-spacing:0.05em">Status</th>
              <th style="text-align:left;padding:10px 16px;font-size:11px;color:#64748b;font-weight:600;text-transform:uppercase;letter-spacing:0.05em">Triggered By</th>
              <th style="text-align:left;padding:10px 16px;font-size:11px;color:#64748b;font-weight:600;text-transform:uppercase;letter-spacing:0.05em">JIRA Issue</th>
              <th style="text-align:left;padding:10px 16px;font-size:11px;color:#64748b;font-weight:600;text-transform:uppercase;letter-spacing:0.05em">Duration</th>
            </tr>
          </thead>
          <tbody>
            ${runs.map((r, i) => {
              const conc = r.conclusion ?? r.status;
              const cBg  = conc === 'success' ? '#dcfce7' : conc === 'failure' ? '#fee2e2' : '#f1f5f9';
              const cFg  = conc === 'success' ? '#166534' : conc === 'failure' ? '#991b1b' : '#475569';
              return `
              <tr style="border-top:1px solid #f1f5f9;background:${i % 2 === 0 ? '#fff' : '#f8fafc'}">
                <td style="padding:10px 16px;font-weight:500;color:#1e293b">${r.workflowName}</td>
                <td style="padding:10px 16px;font-size:12px;color:#475569;font-family:monospace">${r.headBranch}</td>
                <td style="padding:10px 16px"><span style="padding:2px 10px;border-radius:9999px;font-size:11px;font-weight:700;background:${cBg};color:${cFg}">${conc}</span></td>
                <td style="padding:10px 16px;font-size:12px;color:#475569">${r.triggeredBy ?? '—'}</td>
                <td style="padding:10px 16px"><span style="font-size:11px;background:#ede9fe;color:#5b21b6;padding:2px 8px;border-radius:4px;font-weight:600">${r.jiraIssueKey || '—'}</span></td>
                <td style="padding:10px 16px;font-size:12px;color:#475569">${r.durationSeconds}s</td>
              </tr>`;
            }).join('')}
          </tbody>
        </table>
      </div>` : '';

    // ── health bar ─────────────────────────────────────────────────────
    const healthBar = `
      <div style="background:#e2e8f0;border-radius:9999px;height:10px;width:100%;margin:8px 0 4px">
        <div style="height:10px;border-radius:9999px;width:${healthPct}%;background:${healthFill};transition:width 0.5s"></div>
      </div>
      <div style="font-size:11px;color:#64748b">Overall health: ${healthPct}/100</div>`;

    // ── full HTML document ─────────────────────────────────────────────
    return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>AgileGuard Gap Report — ${report.projectKey}</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
           background: #f8fafc; color: #1e293b; line-height: 1.5; }
    @media print {
      body { background: #fff; }
      .no-print { display: none !important; }
      .page-break { page-break-before: always; }
      @page { margin: 1.5cm; size: A4; }
    }
  </style>
</head>
<body>
  <div style="max-width:1100px;margin:0 auto;padding:32px 24px">

    <!-- Header -->
    <div style="background:linear-gradient(135deg,#1e2761,#283a8e);border-radius:16px;padding:32px 36px;margin-bottom:28px;color:#fff">
      <div style="display:flex;align-items:flex-start;justify-content:space-between;flex-wrap:wrap;gap:16px">
        <div>
          <div style="font-size:13px;font-weight:600;color:#cadcfc;letter-spacing:0.08em;text-transform:uppercase;margin-bottom:6px">🛡️ AgileGuard Platform</div>
          <h1 style="font-size:28px;font-weight:800;margin-bottom:6px">Sprint Gap Analysis Report</h1>
          <div style="font-size:15px;color:#cadcfc">Project: <strong style="color:#f4b942">${report.projectKey}</strong> &nbsp;•&nbsp; Generated: ${now}</div>
        </div>
        <div style="text-align:right">
          <div style="font-size:42px;font-weight:900;color:${healthFill}">${healthPct}</div>
          <div style="font-size:12px;color:#cadcfc;margin-top:-4px">Health Score</div>
          <div style="width:120px;background:rgba(255,255,255,0.2);border-radius:9999px;height:6px;margin-top:6px">
            <div style="height:6px;border-radius:9999px;width:${healthPct}%;background:${healthFill}"></div>
          </div>
        </div>
      </div>
    </div>

    <!-- Stats -->
    <div style="display:flex;gap:14px;flex-wrap:wrap;margin-bottom:28px">
      ${statCards}
    </div>

    ${agingSection}

    <!-- Gap Findings -->
    <h2 style="font-size:18px;font-weight:700;color:#1e2761;margin-bottom:14px">📋 Issue-Level Gap Analysis</h2>
    ${issueRows}

    ${ciSection}

    <!-- Footer -->
    <div style="margin-top:40px;padding-top:18px;border-top:1px solid #e2e8f0;display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:12px">
      <div style="font-size:12px;color:#94a3b8">AgileGuard SDLC Governance Platform • Exported ${now}</div>
      <div style="font-size:12px;color:#94a3b8">Java 21 · Spring Boot · Angular 17 · Google Gemini AI</div>
    </div>

  </div>
</body>
</html>`;
  }

  // ── Utilities ──────────────────────────────────────────────────────────────

  /** Wraps text in double quotes and escapes internal quotes for CSV safety. */
  private esc(val: string | undefined | null): string {
    if (!val) return '';
    return '"' + val.replace(/"/g, '""') + '"';
  }

  /** Returns a compact date-time tag suitable for file names. */
  private dateTag(): string {
    const d = new Date();
    return `${d.getFullYear()}${String(d.getMonth() + 1).padStart(2, '0')}${String(d.getDate()).padStart(2, '0')}_${String(d.getHours()).padStart(2, '0')}${String(d.getMinutes()).padStart(2, '0')}`;
  }

  /** Creates a temporary anchor element, clicks it to trigger the download, then removes it. */
  private download(mime: string, filename: string, content: string): void {
    const blob = new Blob([content], { type: mime });
    const url  = URL.createObjectURL(blob);
    const a    = document.createElement('a');
    a.href     = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }
}
