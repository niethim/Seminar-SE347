/**
 * Flash-Voucher Claim & Clearing Demo
 * Pure Vanilla JavaScript Client
 */

(function () {
  'use strict';

  // --- Configuration & State ---
  const DEFAULT_API_BASE = 'http://localhost:8080/be/api';
  const VOUCHER_CODE = 'FLASH50';
  const STOCK_REFRESH_INTERVAL_MS = 5000;
  const POLLING_INTERVAL_MS = 1000;
  const MAX_POLL_ATTEMPTS = 15;

  let apiBase = localStorage.getItem('voucher_api_base') || DEFAULT_API_BASE;
  if (apiBase.includes('/demo/api')) {
    apiBase = DEFAULT_API_BASE;
    localStorage.setItem('voucher_api_base', apiBase);
  }
  let stockTimerId = null;
  let activePollingAbort = null;

  // --- DOM Elements ---
  const elements = {
    // Header & Config
    backendStatusDot: document.getElementById('backend-status-dot'),
    backendStatusText: document.getElementById('backend-status-text'),
    toggleSettingsBtn: document.getElementById('toggle-settings-btn'),
    settingsPanel: document.getElementById('settings-panel'),
    apiBaseInput: document.getElementById('api-base-input'),
    saveApiBaseBtn: document.getElementById('save-api-base-btn'),
    resetApiBaseBtn: document.getElementById('reset-api-base-btn'),

    // Stock Monitor
    voucherCodeDisplay: document.getElementById('voucher-code-display'),
    voucherTotalDisplay: document.getElementById('voucher-total-display'),
    voucherAvailableDisplay: document.getElementById('voucher-available-display'),
    stockProgressBar: document.getElementById('stock-progress-bar'),
    stockPercentageLabel: document.getElementById('stock-percentage-label'),
    refreshStockBtn: document.getElementById('refresh-stock-btn'),
    stockErrorBox: document.getElementById('stock-error-box'),

    // Stock Demo Controls & Concurrency Panel
    resetStockBtn: document.getElementById('reset-stock-btn'),
    resetSpinner: document.getElementById('reset-spinner'),
    resetFeedback: document.getElementById('reset-feedback'),
    btnAutofire: document.getElementById('btn-autofire'),
    autofireSpinner: document.getElementById('autofire-spinner'),
    autofireResultsPanel: document.getElementById('autofire-results-panel'),
    btnCloseAutofire: document.getElementById('btn-close-autofire'),
    autofireTotal: document.getElementById('autofire-total'),
    autofireSuccess: document.getElementById('autofire-success'),
    autofireRejected: document.getElementById('autofire-rejected'),
    autofireDuration: document.getElementById('autofire-duration'),
    autofireProgressFill: document.getElementById('autofire-progress-fill'),
    autofireStockBadge: document.getElementById('autofire-stock-badge'),
    autofireStatusNote: document.getElementById('autofire-status-note'),

    // Claim Form
    claimForm: document.getElementById('claim-form'),
    userIdInput: document.getElementById('user-id-input'),
    btnGenerateUser: document.getElementById('btn-generate-user'),
    simulateFraudCheckbox: document.getElementById('simulate-fraud-checkbox'),
    btnClaimVoucher: document.getElementById('btn-claim-voucher'),
    claimSpinner: document.getElementById('claim-spinner'),

    // Claim Result Banner
    claimResultBox: document.getElementById('claim-result-box'),
    claimStatusBadge: document.getElementById('claim-status-badge'),
    claimHttpCode: document.getElementById('claim-http-code'),
    claimResultMessage: document.getElementById('claim-result-message'),
    claimResultCode: document.getElementById('claim-result-code'),
    claimResultTrace: document.getElementById('claim-result-trace'),
    claimCodeRow: document.getElementById('claim-code-row'),

    // Status Tracker
    statusTrackerBox: document.getElementById('status-tracker-box'),
    stepClaimed: document.getElementById('step-claimed'),
    stepIssued: document.getElementById('step-issued'),
    stepIssuedCircle: document.getElementById('step-issued-circle'),
    stepIssuedLabel: document.getElementById('step-issued-label'),
    stepLine: document.getElementById('step-line'),
    trackerStatusText: document.getElementById('tracker-status-text'),
    pollingSpinner: document.getElementById('polling-spinner'),

    // Batch Reconciliation
    btnTriggerBatch: document.getElementById('btn-trigger-batch'),
    btnRefreshBatch: document.getElementById('btn-refresh-batch'),
    batchSpinner: document.getElementById('batch-spinner'),
    batchAlert: document.getElementById('batch-alert'),
    batchExecutionCard: document.getElementById('batch-execution-card'),
    batchExecId: document.getElementById('batch-exec-id'),
    batchExecStatus: document.getElementById('batch-exec-status'),
    batchRecordsBody: document.getElementById('batch-records-body'),

    // Live Audit
    btnRefreshAudit: document.getElementById('btn-refresh-audit'),
    auditLogsBody: document.getElementById('audit-logs-body'),
  };

  // --- Utility Functions ---

  /** Format Basic Auth Header */
  function getBasicAuthHeader(user, pass) {
    const encoded = btoa(`${user || ''}:${pass || ''}`);
    return `Basic ${encoded}`;
  }

  /** Safe parse JSON or fallback to text */
  async function parseResponsePayload(res) {
    const contentType = res.headers.get('content-type') || '';
    if (contentType.includes('application/json')) {
      try {
        return await res.json();
      } catch (e) {
        return { error: 'Invalid JSON response from server' };
      }
    }
    const text = await res.text();
    return text ? { message: text } : {};
  }

  /** Update Backend Connection Indicator */
  function setBackendStatus(state, message) {
    elements.backendStatusDot.className = `status-dot ${state}`;
    elements.backendStatusText.textContent = message;
  }

  /** Generate random user ID (e.g. user_4812) */
  function generateRandomUserId() {
    const rand = Math.floor(1000 + Math.random() * 9000);
    return `user_${rand}`;
  }

  // --- API Functions ---

  /**
   * Fetch voucher details and current stock
   * GET /api/vouchers/{code}
   */
  async function fetchVoucher(code) {
    try {
      const res = await fetch(`${apiBase}/vouchers/${encodeURIComponent(code)}`, {
        method: 'GET',
        headers: { 'Accept': 'application/json' }
      });

      if (!res.ok) {
        const errorData = await parseResponsePayload(res);
        throw new Error(errorData.message || errorData.error || `HTTP ${res.status} ${res.statusText}`);
      }

      const data = await res.json();
      setBackendStatus('online', 'Backend Connected');
      elements.stockErrorBox.classList.add('hidden');
      return data;
    } catch (err) {
      setBackendStatus('offline', 'Backend Offline / Error');
      elements.stockErrorBox.textContent = `Failed to fetch stock: ${err.message}`;
      elements.stockErrorBox.classList.remove('hidden');
      throw err;
    }
  }

  /**
   * Claim Voucher
   * POST /api/vouchers/{code}/claim
   */
  async function claimVoucher(code, userId, simulateFraud) {
    const payload = {
      userId: userId.trim(),
      simulateFraud: Boolean(simulateFraud)
    };

    const res = await fetch(`${apiBase}/vouchers/${encodeURIComponent(code)}/claim`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      },
      body: JSON.stringify(payload)
    });

    const body = await parseResponsePayload(res);
    return {
      status: res.status,
      statusText: res.statusText,
      ok: res.ok,
      data: body
    };
  }

  /**
   * Poll Claim Status until ISSUED or FAILED
   * GET /api/vouchers/{code}/claims/{claimCode}
   */
  async function pollClaimStatus(code, claimCode, onUpdate) {
    let attempts = 0;

    // Reset previous polling if any
    if (activePollingAbort) {
      activePollingAbort.aborted = true;
    }
    const currentPolling = { aborted: false };
    activePollingAbort = currentPolling;

    while (attempts < MAX_POLL_ATTEMPTS) {
      if (currentPolling.aborted) return;
      attempts++;

      try {
        const res = await fetch(`${apiBase}/vouchers/${encodeURIComponent(code)}/claims/${encodeURIComponent(claimCode)}`, {
          method: 'GET',
          headers: { 'Accept': 'application/json' }
        });

        if (res.ok) {
          const claim = await res.json();
          onUpdate(claim, false);

          if (claim.status === 'ISSUED' || claim.status === 'FAILED') {
            return claim;
          }
        } else {
          onUpdate({ status: 'UNKNOWN', message: `HTTP ${res.status}` }, false);
        }
      } catch (err) {
        onUpdate({ status: 'POLL_ERROR', message: err.message }, false);
      }

      await new Promise(resolve => setTimeout(resolve, POLLING_INTERVAL_MS));
    }

    onUpdate({ status: 'TIMEOUT', message: 'Issuance polling timed out' }, true);
  }

  /**
   * Trigger Jakarta Batch Reconciliation Job
   * POST /api/batch/reconciliation
   */
  async function triggerBatchReconciliation() {
    const res = await fetch(`${apiBase}/batch/reconciliation`, {
      method: 'POST',
      headers: {
        'Accept': 'application/json'
      }
    });

    const body = await parseResponsePayload(res);
    return {
      status: res.status,
      ok: res.ok,
      data: body
    };
  }

  /**
   * Fetch Batch Records
   * GET /api/batch/records
   */
  async function fetchBatchRecords() {
    try {
      const res = await fetch(`${apiBase}/batch/records`, {
        method: 'GET',
        headers: {
          'Accept': 'application/json'
        }
      });

      if (!res.ok) return null;
      return await res.json();
    } catch (e) {
      return null;
    }
  }

  /**
   * Fetch Audit Logs
   * GET /api/audit-logs
   */
  async function fetchAuditLogs() {
    const res = await fetch(`${apiBase}/audit-logs`, {
      method: 'GET',
      headers: { 'Accept': 'application/json' }
    });

    if (!res.ok) {
      const body = await parseResponsePayload(res);
      throw new Error(body.message || body.error || `HTTP ${res.status}`);
    }

    return await res.json();
  }

  // --- UI Renderers & Updaters ---

  /** Render Stock Data to UI */
  async function updateStockView() {
    try {
      const voucher = await fetchVoucher(VOUCHER_CODE);
      const total = voucher.totalStock ?? voucher.total_stock ?? 0;
      const available = voucher.availableStock ?? voucher.available_stock ?? 0;

      elements.voucherCodeDisplay.textContent = voucher.code || VOUCHER_CODE;
      elements.voucherTotalDisplay.textContent = total;
      elements.voucherAvailableDisplay.textContent = available;

      const pct = total > 0 ? Math.max(0, Math.min(100, Math.round((available / total) * 100))) : 0;
      elements.stockPercentageLabel.textContent = `${pct}% (${available}/${total})`;
      elements.stockProgressBar.style.width = `${pct}%`;

      if (pct <= 20) {
        elements.stockProgressBar.classList.add('low');
      } else {
        elements.stockProgressBar.classList.remove('low');
      }
    } catch (err) {
      // Handled in fetchVoucher
    }
  }

  /** Render Claim Response Banner */
  function renderClaimResponse(result) {
    elements.claimResultBox.classList.remove('hidden');
    elements.claimHttpCode.textContent = `HTTP ${result.status}`;

    const data = result.data || {};
    const message = data.message || (result.ok ? 'Voucher claim accepted for asynchronous processing.' : 'Claim request failed.');
    const claimCode = data.claimCode || data.claim_code || '';
    const traceId = data.traceId || data.trace_id || '--';

    elements.claimResultMessage.textContent = message;
    elements.claimResultTrace.textContent = traceId;

    if (claimCode) {
      elements.claimCodeRow.classList.remove('hidden');
      elements.claimResultCode.textContent = claimCode;
    } else {
      elements.claimCodeRow.classList.add('hidden');
    }

    // Set Status Badge styling
    elements.claimStatusBadge.className = 'badge';
    if (result.status === 202) {
      elements.claimStatusBadge.classList.add('badge-claimed');
      elements.claimStatusBadge.textContent = 'CLAIMED (202 ACCEPTED)';
    } else if (result.status === 409) {
      elements.claimStatusBadge.classList.add('badge-danger');
      elements.claimStatusBadge.textContent = 'CONFLICT (409)';
    } else if (result.status === 400) {
      elements.claimStatusBadge.classList.add('badge-danger');
      elements.claimStatusBadge.textContent = 'BAD REQUEST / FRAUD (400)';
    } else if (result.status === 429) {
      elements.claimStatusBadge.classList.add('badge-warning');
      elements.claimStatusBadge.textContent = 'RATE LIMITED (429)';
    } else {
      elements.claimStatusBadge.classList.add('badge-danger');
      elements.claimStatusBadge.textContent = `ERROR (${result.status})`;
    }
  }

  /** Setup and Start Claim Polling Tracker */
  function startClaimTracker(claimCode) {
    elements.statusTrackerBox.classList.remove('hidden');
    elements.pollingSpinner.classList.remove('hidden');

    // Reset steps
    elements.stepClaimed.className = 'step-item active';
    elements.stepIssued.className = 'step-item';
    elements.stepIssuedCircle.textContent = '2';
    elements.stepIssuedLabel.textContent = 'ISSUED (Async Queue)';
    elements.stepLine.className = 'step-line';
    elements.trackerStatusText.textContent = 'Voucher claimed! ActiveMQ MDB is processing issuance...';

    pollClaimStatus(VOUCHER_CODE, claimCode, (claim, isTimeout) => {
      if (isTimeout) {
        elements.pollingSpinner.classList.add('hidden');
        elements.trackerStatusText.textContent = 'Timed out waiting for issuance completion.';
        return;
      }

      if (claim.status === 'ISSUED') {
        elements.pollingSpinner.classList.add('hidden');
        elements.stepClaimed.className = 'step-item completed';
        elements.stepLine.className = 'step-line completed';
        elements.stepIssued.className = 'step-item completed';
        elements.stepIssuedCircle.textContent = '✓';
        elements.trackerStatusText.innerHTML = '<strong class="text-success">Issuance Complete!</strong> Voucher marked as ISSUED by MDB.';
        
        // Refresh stock to ensure UI sync
        updateStockView();
      } else if (claim.status === 'FAILED') {
        elements.pollingSpinner.classList.add('hidden');
        elements.stepIssued.className = 'step-item failed';
        elements.stepIssuedCircle.textContent = '✕';
        elements.trackerStatusText.innerHTML = '<strong class="text-danger">Issuance Failed!</strong> Check server logs.';
      } else {
        elements.trackerStatusText.textContent = `Current status: ${claim.status || 'PROCESSING'}...`;
      }
    });
  }

  /** Render Audit Logs Table */
  function renderAuditLogs(logs) {
    if (!logs || !Array.isArray(logs) || logs.length === 0) {
      elements.auditLogsBody.innerHTML = '<tr><td colspan="5" class="text-center text-muted">No audit logs recorded yet.</td></tr>';
      return;
    }

    elements.auditLogsBody.innerHTML = logs.map(log => {
      const time = log.timestamp ? new Date(log.timestamp).toLocaleTimeString() : '--';
      const action = log.action || '--';
      const principal = log.principalName || log.principal_name || 'anonymous';
      const execTime = log.executionTimeMs ?? log.execution_time_ms ?? '--';
      const trace = log.traceId || log.trace_id || '--';

      return `
        <tr>
          <td>${time}</td>
          <td><strong>${action}</strong></td>
          <td><code>${principal}</code></td>
          <td>${execTime} ms</td>
          <td><code>${trace}</code></td>
        </tr>
      `;
    }).join('');
  }

  /** Render Batch Clearing Records */
  function renderBatchRecords(records) {
    if (!records || !Array.isArray(records) || records.length === 0) {
      elements.batchRecordsBody.innerHTML = '<tr><td colspan="5" class="text-center text-muted">No clearing records found.</td></tr>';
      return;
    }

    elements.batchRecordsBody.innerHTML = records.map(rec => {
      const jobId = rec.batchJobId || rec.batch_job_id || rec.id || '--';
      const date = rec.settlementDate || rec.settlement_date || '--';
      const claims = rec.totalClaims ?? rec.total_claims ?? 0;
      const amount = rec.totalClearedAmount ?? rec.total_cleared_amount ?? '0.00';
      const time = rec.createdAt || rec.created_at ? new Date(rec.createdAt || rec.created_at).toLocaleString() : '--';

      return `
        <tr>
          <td><code>${jobId}</code></td>
          <td>${date}</td>
          <td>${claims}</td>
          <td>$${Number(amount).toFixed(2)}</td>
          <td>${time}</td>
        </tr>
      `;
    }).join('');
  }

  /**
   * Reset Voucher Inventory Database
   * POST /api/vouchers/{code}/init?stock={stock}
   */
  async function resetDatabase(stock = 50) {
    if (!elements.resetStockBtn) return;
    elements.resetStockBtn.disabled = true;
    if (elements.btnAutofire) elements.btnAutofire.disabled = true;
    if (elements.resetSpinner) elements.resetSpinner.classList.remove('hidden');

    if (elements.resetFeedback) {
      elements.resetFeedback.className = 'callout callout-success';
      elements.resetFeedback.textContent = `Đang reset kho về ${stock} vé...`;
      elements.resetFeedback.classList.remove('hidden');
    }

    try {
      const res = await fetch(`${apiBase}/vouchers/${encodeURIComponent(VOUCHER_CODE)}/init?stock=${stock}`, {
        method: 'POST',
        headers: {
          'Accept': 'application/json'
        }
      });

      if (!res.ok) {
        const errorData = await parseResponsePayload(res);
        throw new Error(errorData.message || errorData.error || `HTTP ${res.status}`);
      }

      const data = await res.json();
      const newStock = data.availableStock ?? data.available_stock ?? stock;

      if (elements.resetFeedback) {
        elements.resetFeedback.className = 'callout callout-success';
        elements.resetFeedback.textContent = `Đã reset kho thành công về ${newStock} vé!`;
      }

      // Clear claim inputs & results
      if (elements.claimResultBox) elements.claimResultBox.classList.add('hidden');
      if (elements.statusTrackerBox) elements.statusTrackerBox.classList.add('hidden');
      if (elements.userIdInput) elements.userIdInput.value = generateRandomUserId();

      // Refresh stock view
      await updateStockView();

      // Refresh audit logs
      fetchAuditLogs()
        .then(renderAuditLogs)
        .catch(() => {});

      // Auto-hide alert after 5s
      setTimeout(() => {
        if (elements.resetFeedback) elements.resetFeedback.classList.add('hidden');
      }, 5000);
    } catch (err) {
      if (elements.resetFeedback) {
        elements.resetFeedback.className = 'callout callout-error';
        elements.resetFeedback.textContent = `Lỗi reset kho: ${err.message}`;
      }
    } finally {
      elements.resetStockBtn.disabled = false;
      if (elements.btnAutofire) elements.btnAutofire.disabled = false;
      if (elements.resetSpinner) elements.resetSpinner.classList.add('hidden');
    }
  }

  /**
   * Auto-Fire Concurrency Demo
   * Sends concurrent claim requests to demonstrate row-level locking & contention
   * @param {number} count Total requests to fire (default 30)
   * @param {number} concurrency Concurrency limit (default 10)
   */
  async function runAutoFireDemo(count = 30, concurrency = 10) {
    if (!elements.btnAutofire) return;

    elements.btnAutofire.disabled = true;
    if (elements.resetStockBtn) elements.resetStockBtn.disabled = true;
    if (elements.autofireSpinner) elements.autofireSpinner.classList.remove('hidden');

    if (elements.autofireResultsPanel) elements.autofireResultsPanel.classList.remove('hidden');
    if (elements.autofireTotal) elements.autofireTotal.textContent = count;
    if (elements.autofireSuccess) elements.autofireSuccess.textContent = '0';
    if (elements.autofireRejected) elements.autofireRejected.textContent = '0';
    if (elements.autofireDuration) elements.autofireDuration.textContent = '0 ms';
    if (elements.autofireProgressFill) elements.autofireProgressFill.style.width = '0%';
    if (elements.autofireStatusNote) elements.autofireStatusNote.textContent = `Đang đồng loạt bắn ${count} requests (concurrency: ${concurrency})...`;
    if (elements.autofireStockBadge) {
      elements.autofireStockBadge.textContent = 'Đang đồng bộ...';
      elements.autofireStockBadge.className = 'status-chip';
    }

    const batchRunId = Date.now().toString().slice(-4);
    let completed = 0;
    let successCount = 0;
    let rejectedCount = 0;
    let otherCount = 0;

    const startTime = performance.now();

    // Prepare distinct tasks
    const tasks = Array.from({ length: count }, (_, i) => ({
      userId: `auto_user_${batchRunId}_${i + 1}`
    }));

    let taskIndex = 0;
    async function worker() {
      while (taskIndex < tasks.length) {
        const curTask = tasks[taskIndex++];
        try {
          const res = await claimVoucher(VOUCHER_CODE, curTask.userId, false);
          completed++;
          if (res.status === 202) {
            successCount++;
          } else if (res.status === 409) {
            rejectedCount++;
          } else {
            otherCount++;
          }
        } catch (err) {
          completed++;
          otherCount++;
        } finally {
          const pct = Math.round((completed / count) * 100);
          if (elements.autofireProgressFill) elements.autofireProgressFill.style.width = `${pct}%`;
          if (elements.autofireSuccess) elements.autofireSuccess.textContent = successCount;
          if (elements.autofireRejected) elements.autofireRejected.textContent = rejectedCount;
          if (elements.autofireStatusNote) elements.autofireStatusNote.textContent = `Đang xử lý: ${completed}/${count} requests...`;
        }
      }
    }

    // Launch worker pool
    const workers = Array.from({ length: Math.min(concurrency, count) }, () => worker());
    await Promise.all(workers);

    const duration = Math.round(performance.now() - startTime);
    elements.autofireDuration.textContent = `${duration} ms`;

    // Refresh stock view and audit stream
    await updateStockView();
    fetchAuditLogs()
      .then(renderAuditLogs)
      .catch(() => {});

    // Update real-time stock badge & status note
    const available = elements.voucherAvailableDisplay.textContent;
    elements.autofireStockBadge.textContent = `Tồn kho sau bắn tải: ${available} vé`;
    elements.autofireStockBadge.className = available === '0' ? 'badge badge-danger' : 'badge badge-success';

    let summary = `⚡ Thành công: ${successCount} (202 Accepted) | 🚫 Từ chối: ${rejectedCount} (409 Conflict) | ⏱️ Thời gian: ${duration} ms`;
    if (otherCount > 0) {
      summary += ` | ⚠️ Lỗi khác: ${otherCount}`;
    }
    elements.autofireStatusNote.textContent = summary;

    elements.btnAutofire.disabled = false;
    if (elements.resetStockBtn) elements.resetStockBtn.disabled = false;
    if (elements.autofireSpinner) elements.autofireSpinner.classList.add('hidden');
  }

  // --- Event Listeners & Handlers ---

  // Claim Form Submission
  elements.claimForm.addEventListener('submit', async (e) => {
    e.preventDefault();

    const userId = elements.userIdInput.value.trim();
    if (!userId) return;

    const simulateFraud = elements.simulateFraudCheckbox.checked;

    // UI loading state
    elements.btnClaimVoucher.disabled = true;
    elements.claimSpinner.classList.remove('hidden');
    elements.statusTrackerBox.classList.add('hidden');

    try {
      const result = await claimVoucher(VOUCHER_CODE, userId, simulateFraud);
      renderClaimResponse(result);

      // Refresh stock immediately
      updateStockView();

      // If accepted (202), start polling for MDB issuance
      const claimCode = result.data?.claimCode || result.data?.claim_code;
      if (result.status === 202 && claimCode) {
        startClaimTracker(claimCode);
      }
    } catch (err) {
      renderClaimResponse({
        status: 0,
        ok: false,
        data: { message: `Network/CORS error: ${err.message}` }
      });
    } finally {
      elements.btnClaimVoucher.disabled = false;
      elements.claimSpinner.classList.add('hidden');
    }
  });

  // Generate User ID Button
  elements.btnGenerateUser.addEventListener('click', () => {
    elements.userIdInput.value = generateRandomUserId();
    elements.userIdInput.focus();
  });

  // Refresh Stock Button
  elements.refreshStockBtn.addEventListener('click', () => {
    updateStockView();
  });

  // Reset Database Button
  if (elements.resetStockBtn) {
    elements.resetStockBtn.addEventListener('click', () => {
      resetDatabase(50);
    });
  }

  // Auto-Fire Concurrency Demo Button
  if (elements.btnAutofire) {
    elements.btnAutofire.addEventListener('click', () => {
      runAutoFireDemo(30, 10);
    });
  }

  // Close Auto-Fire Results Panel
  if (elements.btnCloseAutofire) {
    elements.btnCloseAutofire.addEventListener('click', () => {
      elements.autofireResultsPanel.classList.add('hidden');
    });
  }

  // Trigger Batch Reconciliation
  elements.btnTriggerBatch.addEventListener('click', async () => {
    elements.btnTriggerBatch.disabled = true;
    elements.batchSpinner.classList.remove('hidden');
    elements.batchAlert.className = 'callout hidden';

    try {
      const res = await triggerBatchReconciliation();
      if (res.ok) {
        elements.batchAlert.className = 'callout callout-success';
        elements.batchAlert.textContent = `Batch Job submitted successfully! (HTTP ${res.status})`;

        const execId = res.data?.executionId || res.data?.execution_id || 'active';
        elements.batchExecutionCard.classList.remove('hidden');
        elements.batchExecId.textContent = execId;
        elements.batchExecStatus.textContent = res.data?.status || res.data?.batchStatus || 'STARTING';

        // Load clearing records after short delay
        setTimeout(async () => {
          const records = await fetchBatchRecords();
          if (records) renderBatchRecords(records);
        }, 1500);
      } else {
        elements.batchAlert.className = 'callout callout-error';
        elements.batchAlert.textContent = `Failed (HTTP ${res.status}): ${res.data?.message || 'Server error'}`;
      }
    } catch (err) {
      elements.batchAlert.className = 'callout callout-error';
      elements.batchAlert.textContent = `Request error: ${err.message}`;
    } finally {
      elements.btnTriggerBatch.disabled = false;
      elements.batchSpinner.classList.add('hidden');
    }
  });

  // Refresh Batch Records
  if (elements.btnRefreshBatch) {
    elements.btnRefreshBatch.addEventListener('click', async () => {
      elements.btnRefreshBatch.disabled = true;
      try {
        const records = await fetchBatchRecords();
        if (records) renderBatchRecords(records);
      } finally {
        elements.btnRefreshBatch.disabled = false;
      }
    });
  }

  // Refresh Audit Logs
  elements.btnRefreshAudit.addEventListener('click', async () => {
    elements.btnRefreshAudit.disabled = true;
    try {
      const logs = await fetchAuditLogs();
      renderAuditLogs(logs);
    } catch (err) {
      elements.auditLogsBody.innerHTML = `<tr><td colspan="5" class="text-center text-danger">Failed to fetch audit logs: ${err.message}</td></tr>`;
    } finally {
      elements.btnRefreshAudit.disabled = false;
    }
  });

  // Settings Panel Toggle
  elements.toggleSettingsBtn.addEventListener('click', () => {
    elements.settingsPanel.classList.toggle('hidden');
  });

  // Save Custom API Base
  elements.saveApiBaseBtn.addEventListener('click', () => {
    let inputVal = elements.apiBaseInput.value.trim();
    if (!inputVal) inputVal = DEFAULT_API_BASE;
    // Strip trailing slashes
    inputVal = inputVal.replace(/\/+$/, '');
    apiBase = inputVal;
    localStorage.setItem('voucher_api_base', apiBase);
    elements.settingsPanel.classList.add('hidden');

    setBackendStatus('checking', 'Reconnecting...');
    updateStockView();
  });

  // Reset API Base
  elements.resetApiBaseBtn.addEventListener('click', () => {
    apiBase = DEFAULT_API_BASE;
    elements.apiBaseInput.value = DEFAULT_API_BASE;
    localStorage.removeItem('voucher_api_base');
    elements.settingsPanel.classList.add('hidden');

    setBackendStatus('checking', 'Reconnecting...');
    updateStockView();
  });

  // --- Initialization ---
  function init() {
    elements.apiBaseInput.value = apiBase;
    elements.userIdInput.value = generateRandomUserId();

    // Initial fetch
    setBackendStatus('checking', 'Connecting to backend...');
    updateStockView();

    // Start auto-refresh interval for stock
    stockTimerId = setInterval(updateStockView, STOCK_REFRESH_INTERVAL_MS);

    // Initial audit log fetch attempt (non-blocking)
    fetchAuditLogs()
      .then(renderAuditLogs)
      .catch(() => {});

    // Initial batch clearing records fetch attempt (non-blocking)
    fetchBatchRecords()
      .then(records => {
        if (records) renderBatchRecords(records);
      })
      .catch(() => {});
  }

  // Run on DOM ready
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
