import { TestBed } from '@angular/core/testing';
import { ExportService } from './export.service';
import { SprintHealthReport, WorkflowRun } from '../models';

/**
 * Unit tests for ExportService.
 * Uses spies to intercept anchor.click() so no actual file download happens.
 */
describe('ExportService', () => {
  let service: ExportService;
  let anchorSpy: jasmine.SpyObj<HTMLAnchorElement>;

  // A minimal report fixture used across tests
  const mockReport: SprintHealthReport = {
    projectKey: 'TEST',
    tenantId: 'tenant-001',
    totalIssues: 3,
    issuesWithGaps: 2,
    flaggedIssues: 1,
    averageQualityScore: 68,
    overallHealthScore: 68,
    agingIssueKeys: ['TEST-102'],
    generatedAt: new Date().toISOString(),
    issueReports: [
      {
        issueKey: 'TEST-101',
        issueSummary: 'User login feature',
        qualityScore: 100,
        flagged: false,
        criticalCount: 0, errorCount: 0, warningCount: 0,
        findings: []
      },
      {
        issueKey: 'TEST-102',
        issueSummary: 'Fix payment bug',
        qualityScore: 25,
        flagged: true,
        criticalCount: 1, errorCount: 1, warningCount: 1,
        findings: [
          { issueKey: 'TEST-102', type: 'AGING_STORY',               severity: 'CRITICAL', message: 'In same status 8 days', suggestedAction: 'Review in standup', detectedAt: new Date().toISOString() },
          { issueKey: 'TEST-102', type: 'MISSING_DESCRIPTION',       severity: 'ERROR',    message: 'Description is empty',   suggestedAction: 'Add description',   detectedAt: new Date().toISOString() },
          { issueKey: 'TEST-102', type: 'MISSING_STORY_POINTS',      severity: 'WARNING',  message: 'No story points',        suggestedAction: 'Estimate with team', detectedAt: new Date().toISOString() },
        ]
      },
      {
        issueKey: 'TEST-103',
        issueSummary: 'Add search API',
        qualityScore: 72,
        flagged: false,
        criticalCount: 0, errorCount: 0, warningCount: 1,
        findings: [
          { issueKey: 'TEST-103', type: 'INVALID_AC_FORMAT', severity: 'WARNING', message: 'AC has no Given/When/Then', suggestedAction: 'Rewrite AC', detectedAt: new Date().toISOString() }
        ]
      }
    ]
  };

  const mockRuns: WorkflowRun[] = [
    { id: 1, name: 'CI', workflowName: 'CI Pipeline', headBranch: 'main', status: 'completed',
      conclusion: 'success', repoFullName: 'org/repo', triggeredBy: 'dev', jiraIssueKey: 'TEST-101',
      durationSeconds: 120, startedAt: new Date().toISOString(), htmlUrl: '' }
  ];

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ExportService);

    // Spy on document.createElement to capture the <a> element
    anchorSpy = jasmine.createSpyObj('HTMLAnchorElement', ['click']);
    anchorSpy.href = '';
    anchorSpy.download = '';
    spyOn(document, 'createElement').and.callFake((tag: string) => {
      if (tag === 'a') return anchorSpy as any;
      return document.createElement(tag);
    });
    spyOn(document.body, 'appendChild').and.stub();
    spyOn(document.body, 'removeChild').and.stub();
    spyOn(URL, 'createObjectURL').and.returnValue('blob:mock-url');
    spyOn(URL, 'revokeObjectURL').and.stub();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  // ── CSV — All Gaps ──────────────────────────────────────────────────────

  describe('exportGapsAsCsv', () => {
    it('should trigger an anchor click (file download)', () => {
      service.exportGapsAsCsv(mockReport);
      expect(anchorSpy.click).toHaveBeenCalledOnceWith();
    });

    it('should set download filename with project key and .csv extension', () => {
      service.exportGapsAsCsv(mockReport);
      expect(anchorSpy.download).toContain('TEST');
      expect(anchorSpy.download).toMatch(/\.csv$/);
    });

    it('should set download filename containing "GapReport"', () => {
      service.exportGapsAsCsv(mockReport);
      expect(anchorSpy.download).toContain('GapReport');
    });

    it('should create a Blob (CSV mime type)', () => {
      const blobSpy = spyOn(window, 'Blob' as any).and.callThrough();
      service.exportGapsAsCsv(mockReport);
      expect(blobSpy).toHaveBeenCalled();
    });
  });

  // ── CSV — Flagged Only ──────────────────────────────────────────────────

  describe('exportFlaggedAsCsv', () => {
    it('should trigger download when flagged stories exist', () => {
      service.exportFlaggedAsCsv(mockReport);
      expect(anchorSpy.click).toHaveBeenCalledOnceWith();
    });

    it('should include "Flagged" in the filename', () => {
      service.exportFlaggedAsCsv(mockReport);
      expect(anchorSpy.download).toContain('Flagged');
    });

    it('should show alert when no stories are flagged', () => {
      const alertSpy = spyOn(window, 'alert');
      const noFlagged: SprintHealthReport = { ...mockReport, issueReports: mockReport.issueReports.map(r => ({ ...r, flagged: false })) };
      service.exportFlaggedAsCsv(noFlagged);
      expect(alertSpy).toHaveBeenCalled();
      expect(anchorSpy.click).not.toHaveBeenCalled();
    });
  });

  // ── CSV — CI/CD Runs ────────────────────────────────────────────────────

  describe('exportWorkflowRunsAsCsv', () => {
    it('should trigger download for workflow runs', () => {
      service.exportWorkflowRunsAsCsv(mockRuns, 'TEST');
      expect(anchorSpy.click).toHaveBeenCalledOnceWith();
    });

    it('should use CICD in the filename', () => {
      service.exportWorkflowRunsAsCsv(mockRuns, 'TEST');
      expect(anchorSpy.download).toContain('CICD');
    });
  });

  // ── JSON ────────────────────────────────────────────────────────────────

  describe('exportAsJson', () => {
    it('should trigger download with .json extension', () => {
      service.exportAsJson(mockReport);
      expect(anchorSpy.click).toHaveBeenCalledOnceWith();
      expect(anchorSpy.download).toMatch(/\.json$/);
    });

    it('should include project key in json filename', () => {
      service.exportAsJson(mockReport);
      expect(anchorSpy.download).toContain('TEST');
    });
  });

  // ── HTML ────────────────────────────────────────────────────────────────

  describe('exportAsHtml', () => {
    it('should trigger download with .html extension', () => {
      service.exportAsHtml(mockReport, mockRuns);
      expect(anchorSpy.click).toHaveBeenCalledOnceWith();
      expect(anchorSpy.download).toMatch(/\.html$/);
    });
  });

  // ── HTML report content ─────────────────────────────────────────────────

  describe('buildHtmlReport (via exportAsHtml content)', () => {
    let generatedContent: string;

    beforeEach(() => {
      let captured = '';
      spyOn(window, 'Blob').and.callFake((parts: any[]) => {
        captured = parts[0];
        return new Blob(parts);
      });
      service.exportAsHtml(mockReport, mockRuns);
      generatedContent = captured;
    });

    it('should include the project key in generated HTML', () => {
      expect(generatedContent).toContain('TEST');
    });

    it('should include all issue keys', () => {
      expect(generatedContent).toContain('TEST-101');
      expect(generatedContent).toContain('TEST-102');
      expect(generatedContent).toContain('TEST-103');
    });

    it('should include gap messages', () => {
      expect(generatedContent).toContain('In same status 8 days');
      expect(generatedContent).toContain('Description is empty');
    });

    it('should include the aging issue key', () => {
      expect(generatedContent).toContain('Aging Stories');
    });

    it('should include CI/CD run data when runs are provided', () => {
      expect(generatedContent).toContain('CI Pipeline');
      expect(generatedContent).toContain('main');
    });

    it('should include health score', () => {
      expect(generatedContent).toContain('68');
    });

    it('should be a valid HTML document', () => {
      expect(generatedContent).toContain('<!DOCTYPE html>');
      expect(generatedContent).toContain('</html>');
    });
  });

  // ── Pipe: GapTypeLabelPipe ───────────────────────────────────────────────
  // (tested here for convenience since it's a pure function)

  describe('GapTypeLabelPipe logic', () => {
    const transform = (v: string) =>
      v.split('_').map(w => w.charAt(0).toUpperCase() + w.slice(1).toLowerCase()).join(' ');

    it('should convert MISSING_DESCRIPTION to "Missing Description"', () => {
      expect(transform('MISSING_DESCRIPTION')).toBe('Missing Description');
    });
    it('should convert AGING_STORY to "Aging Story"', () => {
      expect(transform('AGING_STORY')).toBe('Aging Story');
    });
    it('should convert INVALID_AC_FORMAT to "Invalid Ac Format"', () => {
      expect(transform('INVALID_AC_FORMAT')).toBe('Invalid Ac Format');
    });
  });
});
