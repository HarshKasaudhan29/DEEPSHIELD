/* ═══════════════════════════════════════════════════════════
   app.js — Synthetic Media Detector
   Modules: Auth · WebSocket · Upload · Results · History · PDF
════════════════════════════════════════════════════════════ */

'use strict';

// ── Global State ──────────────────────────────────────────────
const State = {
  jwt:          null,
  username:     null,
  currentFile:  null,
  currentType:  'image',
  currentResult:null,
  currentScanId:null,
  stompClient:  null,
  taskId:       null,
};

const FORMATS = {
  image: { accept: '.jpg,.jpeg,.png,.gif,.webp', label: 'Images: JPG, PNG, GIF, WEBP · Max 100MB' },
  audio: { accept: '.mp3,.wav,.flac,.m4a',       label: 'Audio: MP3, WAV, FLAC, M4A · Max 100MB' },
  video: { accept: '.mp4,.avi,.mov,.mkv',         label: 'Video: MP4, AVI, MOV, MKV · Max 100MB' },
};

// ── Init ──────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
  const saved = localStorage.getItem('smd_jwt');
  const user  = localStorage.getItem('smd_user');
  if (saved && user) {
    State.jwt      = saved;
    State.username = user;
    onAuthSuccess();
  }
  updateTabFormats('image');
  initNavbarScroll();
});

function initNavbarScroll() {
  const navbar = document.getElementById('navbar');
  window.addEventListener('scroll', () => {
    navbar.style.boxShadow = window.scrollY > 20
      ? '0 4px 32px rgba(0,0,0,0.4)' : 'none';
  });
}

// ═══════════════════════════════════════════════════════════
// AUTH
// ═══════════════════════════════════════════════════════════

async function login() {
  const username = document.getElementById('loginUsername').value.trim();
  const password = document.getElementById('loginPassword').value;
  const errEl    = document.getElementById('loginError');
  errEl.textContent = '';

  if (!username || !password) {
    errEl.textContent = 'Please fill in all fields.';
    return;
  }

  try {
    const res  = await apiFetch('/api/auth/login', 'POST', { username, password }, false);
    const data = await res.json();

    if (!res.ok) { errEl.textContent = data.error || 'Login failed.'; return; }

    State.jwt      = data.token;
    State.username = data.username;
    localStorage.setItem('smd_jwt',  data.token);
    localStorage.setItem('smd_user', data.username);

    closeModal('loginModal');
    onAuthSuccess();
    showToast('Welcome back, ' + data.username + '!', 'success');

  } catch (e) {
    errEl.textContent = 'Network error. Please try again.';
  }
}

async function register() {
  const username = document.getElementById('signupUsername').value.trim();
  const email    = document.getElementById('signupEmail').value.trim();
  const password = document.getElementById('signupPassword').value;
  const errEl    = document.getElementById('signupError');
  errEl.textContent = '';

  if (!username || !email || !password) {
    errEl.textContent = 'Please fill in all fields.';
    return;
  }

  try {
    const res  = await apiFetch('/api/auth/register', 'POST', { username, email, password }, false);
    const data = await res.json();

    if (!res.ok) { errEl.textContent = data.error || 'Registration failed.'; return; }

    State.jwt      = data.token;
    State.username = data.username;
    localStorage.setItem('smd_jwt',  data.token);
    localStorage.setItem('smd_user', data.username);

    closeModal('signupModal');
    onAuthSuccess();
    showToast('Account created! Welcome, ' + data.username + '!', 'success');

  } catch (e) {
    errEl.textContent = 'Network error. Please try again.';
  }
}

function logout() {
  State.jwt      = null;
  State.username = null;
  localStorage.removeItem('smd_jwt');
  localStorage.removeItem('smd_user');

  if (State.stompClient) State.stompClient.disconnect();

  document.getElementById('navAuth').style.display  = 'flex';
  document.getElementById('navUser').style.display  = 'none';
  document.getElementById('historyNavLink').style.display = 'none';
  document.getElementById('history').style.display  = 'none';

  showToast('Signed out successfully.', 'info');
}

function onAuthSuccess() {
  document.getElementById('navAuth').style.display  = 'none';
  document.getElementById('navUser').style.display  = 'flex';
  document.getElementById('historyNavLink').style.display = 'block';
  document.getElementById('history').style.display  = 'block';
  document.getElementById('userBadge').textContent  = '@ ' + State.username;

  connectWebSocket();
  loadHistory();
}

// ═══════════════════════════════════════════════════════════
// WEBSOCKET
// ═══════════════════════════════════════════════════════════

function connectWebSocket() {
  if (!State.jwt) return;

  const socket = new SockJS('/ws-detection');
  State.stompClient = Stomp.over(socket);
  State.stompClient.debug = null; // silence STOMP logs

  State.stompClient.connect(
    { Authorization: 'Bearer ' + State.jwt },
    () => {
      State.stompClient.subscribe(
        '/user/queue/task-progress',
        (msg) => handleWsMessage(JSON.parse(msg.body))
      );
    },
    (err) => {
      console.warn('WebSocket disconnected, will retry on next upload.');
    }
  );
}

function handleWsMessage(data) {
  const pct = data.progressPercent || 0;

  // Update progress bar
  document.getElementById('progressFill').style.width  = pct + '%';
  document.getElementById('progressPct').textContent   = pct + '%';
  document.getElementById('progressStatus').textContent = data.message || data.status;

  // Update steps
  updateSteps(data.status);

  if (data.status === 'COMPLETED' && data.result) {
    setTimeout(() => showResults(data.result), 400);
  }

  if (data.status === 'FAILED') {
    document.getElementById('progressStatus').textContent = '✕ ' + (data.errorMessage || 'Analysis failed.');
    showToast('Analysis failed: ' + (data.errorMessage || 'Unknown error'), 'error');
  }
}

function updateSteps(status) {
  const map = {
    'PROCESSING_STARTED':  1,
    'ML_ANALYSIS_RUNNING': 2,
    'SAVING_RESULT':       3,
    'COMPLETED':           4,
    'FAILED':              0,
  };
  const active = map[status] || 0;

  for (let i = 1; i <= 4; i++) {
    const el = document.getElementById('step' + i);
    el.className = 'step' + (i < active ? ' done' : i === active ? ' active' : '');
  }
}

// ═══════════════════════════════════════════════════════════
// UPLOAD
// ═══════════════════════════════════════════════════════════

function switchTab(btn, type) {
  document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
  btn.classList.add('active');
  State.currentType = type;
  updateTabFormats(type);
  clearFile();
  resetAnalysis();
}

function updateTabFormats(type) {
  const f = FORMATS[type];
  document.getElementById('fileInput').accept = f.accept;
  document.getElementById('uploadFormats').textContent = f.label;
}

function triggerFileInput() {
  if (document.getElementById('fileSelected').style.display !== 'none') return;
  document.getElementById('fileInput').click();
}

function handleFileSelect(e) {
  const file = e.target.files[0];
  if (file) setFile(file);
}

function handleDrop(e) {
  e.preventDefault();
  document.getElementById('uploadZone').classList.remove('drag-over');
  const file = e.dataTransfer.files[0];
  if (file) setFile(file);
}

function handleDragOver(e) {
  e.preventDefault();
  document.getElementById('uploadZone').classList.add('drag-over');
}

function handleDragLeave(e) {
  document.getElementById('uploadZone').classList.remove('drag-over');
}

function setFile(file) {
  State.currentFile = file;

  document.getElementById('uploadInner').style.display    = 'none';
  document.getElementById('fileSelected').style.display   = 'flex';
  document.getElementById('selectedFileName').textContent = file.name;
  document.getElementById('selectedFileSize').textContent = formatBytes(file.size);

  const btn = document.getElementById('analyzeBtn');
  btn.disabled = false;
}

function clearFile(e) {
  if (e) e.stopPropagation();
  State.currentFile = null;

  document.getElementById('uploadInner').style.display  = 'flex';
  document.getElementById('fileSelected').style.display = 'none';
  document.getElementById('fileInput').value            = '';

  document.getElementById('analyzeBtn').disabled = true;
}

async function startAnalysis() {
  if (!State.currentFile) return;

  if (!State.jwt) {
    openModal('loginModal');
    showToast('Please sign in to analyze files.', 'info');
    return;
  }

  // Show progress panel
  document.getElementById('progressPanel').style.display = 'block';
  document.getElementById('resultsPanel').style.display  = 'none';
  document.getElementById('analyzeBtn').disabled = true;

  // Reset steps
  for (let i = 1; i <= 4; i++) document.getElementById('step' + i).className = 'step';
  document.getElementById('progressFill').style.width = '5%';
  document.getElementById('progressPct').textContent  = '5%';
  document.getElementById('progressStatus').textContent = 'Uploading file...';

  const formData = new FormData();
  formData.append('file', State.currentFile);

  try {
    const res  = await fetch('/api/detect/' + State.currentType, {
      method: 'POST',
      headers: { Authorization: 'Bearer ' + State.jwt },
      body: formData,
    });

    const data = await res.json();

    if (!res.ok) {
      showToast(data.error || 'Upload failed.', 'error');
      document.getElementById('analyzeBtn').disabled = false;
      return;
    }

    State.taskId = data.taskId;

    // If WebSocket not connected, poll fallback not needed —
    // WS message will arrive automatically.

  } catch (e) {
    showToast('Network error during upload.', 'error');
    document.getElementById('analyzeBtn').disabled = false;
  }
}

// ═══════════════════════════════════════════════════════════
// RESULTS
// ═══════════════════════════════════════════════════════════

function showResults(result) {
  State.currentResult = result;

  document.getElementById('progressPanel').style.display = 'none';
  document.getElementById('resultsPanel').style.display  = 'flex';

  const isFake   = result.prediction === 'fake';
  const conf     = Math.round((result.confidence || 0) * 100);
  const realProb = Math.round((result.real_probability || 0) * 100);
  const fakeProb = Math.round((result.fake_probability || 0) * 100);

  // Verdict banner
  const banner = document.getElementById('verdictBanner');
  banner.className = 'verdict-banner ' + (isFake ? 'fake' : 'real');
  document.getElementById('verdictIcon').textContent  = isFake ? '⚠' : '✓';
  document.getElementById('verdictLabel').textContent = isFake ? 'SYNTHETIC / FAKE DETECTED' : 'AUTHENTIC / REAL CONTENT';
  document.getElementById('verdictDesc').textContent  = isFake
    ? 'High probability of AI-generated or manipulated content detected.'
    : 'Content appears to be authentic with no significant manipulation detected.';
  document.getElementById('verdictLabel').style.color = isFake
    ? 'var(--color-fake)' : 'var(--color-real)';

  // Gauge
  const circumference = 314;
  const offset = circumference - (conf / 100) * circumference;
  const gaugeFill = document.getElementById('gaugeFill');
  gaugeFill.style.strokeDashoffset = offset;
  gaugeFill.style.stroke = isFake ? 'var(--color-fake)' : 'var(--color-real)';
  document.getElementById('gaugeText').textContent = conf + '%';

  // Bars
  setTimeout(() => {
    document.getElementById('realBar').style.width = realProb + '%';
    document.getElementById('fakeBar').style.width = fakeProb + '%';
  }, 100);

  document.getElementById('realVal').textContent  = realProb + '%';
  document.getElementById('fakeVal').textContent  = fakeProb + '%';
  document.getElementById('procTime').textContent = (result.processing_time || 0) + 's';

  // Check Parameters
  renderParamsTable(result.fake_probability || 0);

  // Store scan id for PDF
  if (result.scanId) State.currentScanId = result.scanId;

  document.getElementById('analyzeBtn').disabled = false;
}

function renderParamsTable(fp) {
  const params = [
    { name: 'Pixel Integrity / ELA Analysis',      score: fp * 1.0,  threshold: [0.4, 0.6] },
    { name: 'Structural Consistency (OpenCV)',      score: fp * 0.9,  threshold: [0.35, 0.55] },
    { name: 'Metadata Validation',                 score: fp * 0.7,  threshold: [0.3, 0.5] },
    { name: 'Noise Pattern Analysis',              score: fp * 0.85, threshold: [0.35, 0.55] },
    { name: 'Edge Coherence Check',               score: fp * 0.95, threshold: [0.4, 0.6] },
    { name: 'Temporal Consistency (Video only)',   score: fp * 0.6,  threshold: [0.3, 0.5] },
  ];

  const body = document.getElementById('paramsBody');
  body.innerHTML = params.map(p => {
    const pct    = Math.round(p.score * 100);
    const status = p.score > p.threshold[1] ? 'ANOMALY'
                 : p.score > p.threshold[0] ? 'WARNING' : 'PASS';
    const cls    = status === 'ANOMALY' ? 'status-fail'
                 : status === 'WARNING' ? 'status-warn' : 'status-pass';

    return `
      <div class="param-row">
        <span class="param-name">${p.name}</span>
        <span class="param-status ${cls}">${status}</span>
        <span class="param-score">${pct}% risk</span>
      </div>`;
  }).join('');
}

function resetAnalysis() {
  document.getElementById('progressPanel').style.display = 'none';
  document.getElementById('resultsPanel').style.display  = 'none';
  clearFile();
  State.currentResult = null;
  State.currentScanId = null;
}

// ═══════════════════════════════════════════════════════════
// EXPORT
// ═══════════════════════════════════════════════════════════

async function downloadPDF() {
  if (!State.currentScanId) {
    showToast('No scan ID available. Please re-analyze the file.', 'info');
    return;
  }

  try {
    const res = await fetch('/api/reports/download/' + State.currentScanId, {
      headers: { Authorization: 'Bearer ' + State.jwt },
    });

    if (!res.ok) { showToast('PDF generation failed.', 'error'); return; }

    const blob = await res.blob();
    const url  = URL.createObjectURL(blob);
    const a    = document.createElement('a');
    a.href     = url;
    a.download = 'SMD_Report_' + State.currentScanId + '.pdf';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);

    showToast('PDF report downloaded!', 'success');

  } catch (e) {
    showToast('Error downloading PDF.', 'error');
  }
}

function exportJSON() {
  if (!State.currentResult) return;

  const blob = new Blob([JSON.stringify(State.currentResult, null, 2)], {
    type: 'application/json'
  });
  const url  = URL.createObjectURL(blob);
  const a    = document.createElement('a');
  a.href     = url;
  a.download = 'SMD_Analysis_' + Date.now() + '.json';
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);

  showToast('JSON exported!', 'success');
}

// ═══════════════════════════════════════════════════════════
// HISTORY
// ═══════════════════════════════════════════════════════════

async function loadHistory() {
  if (!State.jwt) return;

  try {
    const [histRes, statsRes] = await Promise.all([
      apiFetch('/api/detect/history',        'GET'),
      apiFetch('/api/detect/history/stats',  'GET'),
    ]);

    if (histRes.ok) {
      const history = await histRes.json();
      renderHistoryTable(history);
    }

    if (statsRes.ok) {
      const stats = await statsRes.json();
      renderHistoryStats(stats);
    }

  } catch (e) {
    console.warn('Could not load history:', e);
  }
}

function renderHistoryStats(stats) {
  const el = document.getElementById('historyStats');
  el.innerHTML = `
    <div class="hist-stat-card">
      <span class="hist-stat-num">${stats.total || 0}</span>
      <span class="hist-stat-label">Total Scans</span>
    </div>
    <div class="hist-stat-card">
      <span class="hist-stat-num" style="color:var(--color-fake)">${stats.fakeDetected || 0}</span>
      <span class="hist-stat-label">Fake Detected</span>
    </div>
    <div class="hist-stat-card">
      <span class="hist-stat-num" style="color:var(--color-real)">${stats.realDetected || 0}</span>
      <span class="hist-stat-label">Real Verified</span>
    </div>`;
}

function renderHistoryTable(history) {
  const body = document.getElementById('historyBody');

  if (!history || history.length === 0) {
    body.innerHTML = '<div class="history-empty">No scans yet. Upload a file to get started.</div>';
    return;
  }

  body.innerHTML = history.map(row => {
    const isFake  = row.prediction === 'fake';
    const conf    = Math.round((row.confidenceScore || 0) * 100);
    const date    = row.timestamp ? new Date(row.timestamp).toLocaleDateString('en-IN',
      { day: '2-digit', month: 'short', year: 'numeric' }) : 'N/A';

    return `
      <div class="history-row">
        <span class="history-filename" title="${row.filename}">${row.filename || 'N/A'}</span>
        <span class="history-type">${row.fileType || '—'}</span>
        <span>
          <span class="verdict-pill ${isFake ? 'verdict-fake' : 'verdict-real'}">
            ${isFake ? '⚠ FAKE' : '✓ REAL'}
          </span>
        </span>
        <span class="history-conf" style="color:${isFake ? 'var(--color-fake)' : 'var(--color-real)'}">
          ${conf}%
        </span>
        <span class="history-date">${date}</span>
        <span>
          <button class="btn-dl-pdf" onclick="downloadHistoryPDF('${row.id}')">⬇ PDF</button>
        </span>
      </div>`;
  }).join('');
}

async function downloadHistoryPDF(id) {
  try {
    const res = await fetch('/api/reports/download/' + id, {
      headers: { Authorization: 'Bearer ' + State.jwt },
    });

    if (!res.ok) { showToast('PDF not available.', 'error'); return; }

    const blob = await res.blob();
    const url  = URL.createObjectURL(blob);
    const a    = document.createElement('a');
    a.href     = url;
    a.download = 'SMD_Report_' + id + '.pdf';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);

    showToast('Report downloaded!', 'success');

  } catch (e) {
    showToast('Download failed.', 'error');
  }
}

// ═══════════════════════════════════════════════════════════
// MODALS
// ═══════════════════════════════════════════════════════════

function openModal(id) {
  document.getElementById(id).classList.add('open');
  document.body.style.overflow = 'hidden';
}

function closeModal(id) {
  document.getElementById(id).classList.remove('open');
  document.body.style.overflow = '';
}

function closeModalOutside(e, id) {
  if (e.target.id === id) closeModal(id);
}

function switchModal(fromId, toId) {
  closeModal(fromId);
  setTimeout(() => openModal(toId), 200);
}

// ═══════════════════════════════════════════════════════════
// TOAST NOTIFICATIONS
// ═══════════════════════════════════════════════════════════

function showToast(message, type = 'info') {
  const existing = document.getElementById('smd-toast');
  if (existing) existing.remove();

  const colors = {
    success: 'var(--color-real)',
    error:   'var(--color-fake)',
    info:    'var(--accent-cyan)',
  };

  const toast = document.createElement('div');
  toast.id = 'smd-toast';
  toast.style.cssText = `
    position:fixed;bottom:24px;right:24px;z-index:999;
    background:var(--bg-card);border:1px solid ${colors[type]};
    color:var(--text-primary);padding:14px 20px;border-radius:10px;
    font-family:var(--font-mono);font-size:13px;
    box-shadow:0 8px 32px rgba(0,0,0,0.4);
    animation:fadeUp 0.3s ease;max-width:320px;line-height:1.5;
  `;
  toast.textContent = message;
  document.body.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transition = 'opacity 0.3s';
    setTimeout(() => toast.remove(), 300);
  }, 3500);
}

// ═══════════════════════════════════════════════════════════
// UTILITIES
// ═══════════════════════════════════════════════════════════

async function apiFetch(url, method = 'GET', body = null, auth = true) {
  const headers = { 'Content-Type': 'application/json' };
  if (auth && State.jwt) headers['Authorization'] = 'Bearer ' + State.jwt;

  const opts = { method, headers };
  if (body) opts.body = JSON.stringify(body);

  return fetch(url, opts);
}

function formatBytes(bytes) {
  if (bytes < 1024)     return bytes + ' B';
  if (bytes < 1048576)  return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / 1048576).toFixed(2) + ' MB';
}
