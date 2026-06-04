"""Small dependency-free web UI for testing the offline translation pipeline.

The service uses Python's standard library so it can run before heavy offline
AI packages are installed. Later, the same request path can call NLLB, Argos
Translate, or CTranslate2 instead of the demo translation service.
"""

from __future__ import annotations

import json
import subprocess
import sys
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlparse


ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "python_lab"))
sys.path.insert(0, str(ROOT / "scripts"))

from app.translator_service import translate  # noqa: E402
from corpus_status import build_status  # noqa: E402


LANGUAGES = {
    "ko_kp": "조선말",
    "en": "English",
    "zh": "Chinese",
    "ru": "Russian",
}


class TranslatorRequestHandler(BaseHTTPRequestHandler):
    """Serve the test page and the local JSON translation endpoint."""

    server_version = "TranslatorWeb/0.1"

    def do_GET(self) -> None:  # noqa: N802 - stdlib method name
        path = urlparse(self.path).path
        if path in {"/", "/index.html"}:
            self._send_html(_render_index())
            return
        if path == "/health":
            self._send_json({"status": "ok", "offline": True})
            return
        if path == "/api/status":
            self._send_json(_project_status())
            return
        self._send_json({"error": "not found"}, HTTPStatus.NOT_FOUND)

    def do_POST(self) -> None:  # noqa: N802 - stdlib method name
        path = urlparse(self.path).path
        if path == "/api/translate":
            self._handle_translate()
            return
        if path == "/api/import-url":
            self._handle_import_url()
            return
        if path == "/api/import-url-pair":
            self._handle_import_url_pair()
            return
        if path == "/api/add-sentence-pair":
            self._handle_add_sentence_pair()
            return
        if path == "/api/rebuild":
            self._handle_rebuild()
            return
        self._send_json({"error": "not found"}, HTTPStatus.NOT_FOUND)

    def _handle_translate(self) -> None:
        try:
            payload = self._read_json()
            source = str(payload.get("source_language", "en"))
            target = str(payload.get("target_language", "ko_kp"))
            text = str(payload.get("text", ""))
            if source not in LANGUAGES or target not in LANGUAGES:
                raise ValueError("unsupported language")
            result = translate(text, source, target)
        except Exception as exc:
            self._send_json({"error": str(exc)}, HTTPStatus.BAD_REQUEST)
            return

        self._send_json(
            {
                "source_language": source,
                "target_language": target,
                "translated_text": result["translated_text"],
                "engine": result["engine"],
                "offline": result["offline"],
                "ko_kp_score": result.get("ko_kp_score"),
            }
        )

    def _handle_import_url(self) -> None:
        try:
            payload = self._read_json()
            url = str(payload.get("url", "")).strip()
            language = str(payload.get("language", "ko_kp")).strip()
            if not url:
                raise ValueError("url is required")
            if language not in LANGUAGES:
                raise ValueError("unsupported language")
            output = _run_command([sys.executable, "scripts/import_url_corpus.py", url, "--language", language])
        except Exception as exc:
            self._send_json({"error": str(exc)}, HTTPStatus.BAD_REQUEST)
            return
        self._send_json({"status": "imported", "output": output})

    def _handle_import_url_pair(self) -> None:
        try:
            payload = self._read_json()
            source_url = str(payload.get("source_url", "")).strip()
            target_url = str(payload.get("target_url", "")).strip()
            source_language = str(payload.get("source_language", "ko_kp")).strip()
            target_language = str(payload.get("target_language", "en")).strip()
            approve_exact_order = bool(payload.get("approve_exact_order", False))
            if not source_url or not target_url:
                raise ValueError("source_url and target_url are required")
            if source_language not in LANGUAGES or target_language not in LANGUAGES:
                raise ValueError("unsupported language")
            command = [
                sys.executable,
                "scripts/import_url_pair_corpus.py",
                "--source-url",
                source_url,
                "--target-url",
                target_url,
                "--source-language",
                source_language,
                "--target-language",
                target_language,
            ]
            if approve_exact_order:
                command.append("--approve-exact-order")
            output = _run_command(command)
        except Exception as exc:
            self._send_json({"error": str(exc)}, HTTPStatus.BAD_REQUEST)
            return
        self._send_json({"status": "pair_imported", "output": output})

    def _handle_add_sentence_pair(self) -> None:
        try:
            payload = self._read_json()
            source_language = str(payload.get("source_language", "ko_kp")).strip()
            target_language = str(payload.get("target_language", "en")).strip()
            source_text = str(payload.get("source_text", "")).strip()
            target_text = str(payload.get("target_text", "")).strip()
            if source_language not in LANGUAGES or target_language not in LANGUAGES:
                raise ValueError("unsupported language")
            if not source_text or not target_text:
                raise ValueError("source_text and target_text are required")
            output = _run_command(
                [
                    sys.executable,
                    "scripts/add_sentence_pair.py",
                    "--source-language",
                    source_language,
                    "--target-language",
                    target_language,
                    "--source-text",
                    source_text,
                    "--target-text",
                    target_text,
                ]
            )
        except Exception as exc:
            self._send_json({"error": str(exc)}, HTTPStatus.BAD_REQUEST)
            return
        self._send_json({"status": "sentence_pair_added", "output": output})

    def _handle_rebuild(self) -> None:
        try:
            output = _run_command([sys.executable, "scripts/bootstrap_data_pipeline.py"])
        except Exception as exc:
            self._send_json({"error": str(exc)}, HTTPStatus.BAD_REQUEST)
            return
        self._send_json({"status": "rebuilt", "output": output, "project": _project_status()})

    def log_message(self, format: str, *args: object) -> None:
        """Keep server logs compact and readable."""
        sys.stderr.write("%s - %s\n" % (self.address_string(), format % args))

    def _read_json(self) -> dict[str, object]:
        length = int(self.headers.get("Content-Length", "0"))
        raw = self.rfile.read(length).decode("utf-8")
        return json.loads(raw or "{}")

    def _send_html(self, body: str, status: HTTPStatus = HTTPStatus.OK) -> None:
        encoded = body.encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.send_header("Content-Length", str(len(encoded)))
        self.end_headers()
        self.wfile.write(encoded)

    def _send_json(self, body: dict[str, object], status: HTTPStatus = HTTPStatus.OK) -> None:
        encoded = json.dumps(body, ensure_ascii=False, indent=2).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(encoded)))
        self.end_headers()
        self.wfile.write(encoded)


def run(host: str = "127.0.0.1", port: int = 8765) -> None:
    """Start the local offline web service."""
    server = ThreadingHTTPServer((host, port), TranslatorRequestHandler)
    print(f"Translator web UI: http://{host}:{port}")
    print("Press Ctrl+C to stop.")
    server.serve_forever()


def _run_command(command: list[str]) -> str:
    result = subprocess.run(
        command,
        cwd=ROOT,
        check=False,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        timeout=120,
    )
    output = (result.stdout + "\n" + result.stderr).strip()
    if result.returncode != 0:
        raise RuntimeError(output or f"command failed: {' '.join(command)}")
    return output


def _project_status() -> dict[str, object]:
    return build_status()


def _render_index() -> str:
    language_options = "\n".join(f'<option value="{code}">{label}</option>' for code, label in LANGUAGES.items())
    return f"""<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Translator</title>
  <style>
    :root {{
      color-scheme: light;
      --ink: #1b1f24;
      --muted: #667085;
      --line: #d7dde5;
      --accent: #1f6f5b;
      --red: #d1495b;
      --blue: #2f80ed;
      --gold: #f2a541;
      --violet: #7c3aed;
      --surface: #f6f8fa;
      --panel: #ffffff;
    }}
    * {{ box-sizing: border-box; }}
    body {{
      margin: 0;
      font-family: Arial, sans-serif;
      color: var(--ink);
      background: var(--surface);
    }}
    main {{
      width: min(1180px, calc(100vw - 32px));
      margin: 32px auto;
    }}
    h1 {{
      margin: 0 0 16px;
      font-size: 28px;
      letter-spacing: 0;
    }}
    .toolbar {{
      display: grid;
      grid-template-columns: 1fr auto 1fr;
      gap: 12px;
      align-items: center;
      margin-bottom: 12px;
    }}
    .tabs {{
      display: flex;
      gap: 8px;
      margin: 18px 0;
      border-bottom: 1px solid var(--line);
    }}
    .tab-button {{
      width: auto;
      min-width: 120px;
      margin: 0;
      border: 0;
      border-radius: 6px 6px 0 0;
      background: transparent;
      color: var(--muted);
    }}
    .tab-button.active {{
      background: white;
      color: var(--ink);
      border: 1px solid var(--line);
      border-bottom-color: white;
    }}
    .tab-panel {{
      display: none;
    }}
    .tab-panel.active {{
      display: block;
    }}
    .section {{
      margin: 20px 0;
      padding-top: 4px;
    }}
    .grid-2 {{
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 12px;
    }}
    .status-dashboard {{
      display: grid;
      gap: 14px;
      margin-top: 14px;
    }}
    .metric-grid {{
      display: grid;
      grid-template-columns: repeat(4, minmax(0, 1fr));
      gap: 12px;
    }}
    .metric-card, .chart-panel, .readiness-card {{
      border: 1px solid var(--line);
      border-radius: 8px;
      background: var(--panel);
      box-shadow: 0 8px 24px rgba(27, 31, 36, 0.05);
    }}
    .metric-card {{
      min-height: 104px;
      padding: 14px;
      display: flex;
      flex-direction: column;
      justify-content: space-between;
    }}
    .metric-label {{
      color: var(--muted);
      font-size: 13px;
    }}
    .metric-value {{
      font-size: 30px;
      font-weight: 700;
      line-height: 1;
    }}
    .metric-note {{
      color: var(--muted);
      font-size: 12px;
    }}
    .chart-grid {{
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 12px;
    }}
    .chart-panel {{
      min-height: 230px;
      padding: 16px;
    }}
    .chart-title {{
      margin: 0 0 12px;
      font-size: 16px;
    }}
    .donut-layout {{
      display: grid;
      grid-template-columns: 148px 1fr;
      gap: 16px;
      align-items: center;
    }}
    .donut {{
      width: 148px;
      aspect-ratio: 1;
      border-radius: 50%;
      background: #e5e7eb;
      position: relative;
    }}
    .donut::after {{
      content: "";
      position: absolute;
      inset: 28px;
      border-radius: 50%;
      background: var(--panel);
      box-shadow: inset 0 0 0 1px var(--line);
    }}
    .donut-total {{
      position: absolute;
      inset: 0;
      display: grid;
      place-items: center;
      text-align: center;
      z-index: 1;
      font-size: 20px;
      font-weight: 700;
      pointer-events: none;
    }}
    .donut-total span {{
      display: block;
      color: var(--muted);
      font-size: 11px;
      font-weight: 400;
      margin-top: 4px;
    }}
    .legend {{
      display: grid;
      gap: 8px;
      min-width: 0;
    }}
    .legend-row {{
      display: grid;
      grid-template-columns: 12px minmax(0, 1fr) auto;
      gap: 8px;
      align-items: center;
      font-size: 13px;
    }}
    .legend-dot {{
      width: 12px;
      height: 12px;
      border-radius: 50%;
    }}
    .legend-label {{
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }}
    .readiness-grid {{
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 12px;
    }}
    .readiness-card {{
      padding: 14px;
    }}
    .readiness-head {{
      display: flex;
      justify-content: space-between;
      gap: 12px;
      margin-bottom: 10px;
      font-weight: 700;
    }}
    .progress {{
      height: 12px;
      border-radius: 999px;
      overflow: hidden;
      background: #e8edf2;
    }}
    .progress-fill {{
      height: 100%;
      width: 0;
      border-radius: inherit;
      background: linear-gradient(90deg, var(--accent), var(--blue));
    }}
    .status-actions {{
      display: flex;
      justify-content: space-between;
      gap: 12px;
      align-items: center;
      margin-top: 10px;
    }}
    .status-actions button {{
      width: auto;
      min-width: 150px;
      margin: 0;
    }}
    .check-row {{
      display: flex;
      gap: 8px;
      align-items: center;
      margin-top: 8px;
    }}
    select, textarea, button, input {{
      width: 100%;
      border: 1px solid var(--line);
      border-radius: 6px;
      font: inherit;
    }}
    select, button, input {{
      height: 42px;
      padding: 0 12px;
      background: white;
    }}
    textarea {{
      min-height: 180px;
      padding: 12px;
      resize: vertical;
      background: white;
    }}
    button {{
      margin-top: 12px;
      border-color: var(--accent);
      background: var(--accent);
      color: white;
      cursor: pointer;
    }}
    pre {{
      min-height: 120px;
      padding: 12px;
      border: 1px solid var(--line);
      border-radius: 6px;
      white-space: pre-wrap;
      background: white;
    }}
    .muted {{ color: var(--muted); }}
    @media (max-width: 720px) {{
      .toolbar, .grid-2, .chart-grid, .readiness-grid, .donut-layout {{
        grid-template-columns: 1fr;
      }}
      .metric-grid {{
        grid-template-columns: repeat(2, minmax(0, 1fr));
      }}
      .donut {{
        width: min(180px, 100%);
        justify-self: center;
      }}
    }}
  </style>
</head>
<body>
  <main>
    <h1>Translator</h1>

    <div class="tabs">
      <button class="tab-button active" data-tab="translator">번역기</button>
      <button class="tab-button" data-tab="training">자료모집/학습</button>
    </div>

    <section id="translator-panel" class="tab-panel active">
      <div class="toolbar">
        <select id="source">{language_options}</select>
        <span class="muted">to</span>
        <select id="target">{language_options}</select>
      </div>
      <textarea id="text" spellcheck="false">Korean</textarea>
      <button id="translate">Translate offline</button>
      <p class="muted" id="engine">Engine: demo-memory-postedit</p>
      <pre id="result"></pre>
    </section>

    <section id="training-panel" class="tab-panel">
      <div class="section">
        <h1>Raw 자료모집</h1>
        <div class="toolbar">
          <select id="url-language">{language_options}</select>
          <span class="muted">URL</span>
          <input id="url" placeholder="언어별 원문 URL">
        </div>
        <button id="import-url">Raw URL 수집</button>
      </div>

      <div class="section">
        <h1>쌍방향 번역자료</h1>
        <div class="grid-2">
          <div>
            <select id="pair-source-language">{language_options}</select>
            <input id="source-url" placeholder="조선말 URL">
          </div>
          <div>
            <select id="pair-target-language">{language_options}</select>
            <input id="target-url" placeholder="영어 URL">
          </div>
        </div>
        <label class="check-row muted">
          <input id="approve-exact-order" type="checkbox" style="width:auto;height:auto;">
          문장순서가 정확히 같으면 곧바로 승인자료로 넣기
        </label>
        <button id="import-url-pair">URL쌍 수집</button>
      </div>

      <div class="section">
        <h1>문장 대 문장</h1>
        <div class="grid-2">
          <div>
            <select id="sentence-source-language">{language_options}</select>
            <textarea id="sentence-source-text" spellcheck="false" placeholder="원문 문장"></textarea>
          </div>
          <div>
            <select id="sentence-target-language">{language_options}</select>
            <textarea id="sentence-target-text" spellcheck="false" placeholder="번역 문장"></textarea>
          </div>
        </div>
        <button id="add-sentence-pair">문장쌍 추가</button>
      </div>

      <div class="section">
        <h1>학습</h1>
        <button id="rebuild">학습시키기</button>
        <div id="status-dashboard" class="status-dashboard"></div>
        <div class="status-actions">
          <span class="muted" id="status-updated">-</span>
          <button id="toggle-status-log" type="button">JSON 보기</button>
        </div>
        <pre id="status-log" hidden></pre>
      </div>
    </section>
  </main>
  <script>
    document.querySelectorAll('.tab-button').forEach((button) => {{
      button.addEventListener('click', () => {{
        document.querySelectorAll('.tab-button').forEach((item) => item.classList.remove('active'));
        document.querySelectorAll('.tab-panel').forEach((item) => item.classList.remove('active'));
        button.classList.add('active');
        document.querySelector(`#${{button.dataset.tab}}-panel`).classList.add('active');
        refreshStatus();
      }});
    }});

    const source = document.querySelector('#source');
    const target = document.querySelector('#target');
    const text = document.querySelector('#text');
    const result = document.querySelector('#result');
    const engine = document.querySelector('#engine');
    const status = document.querySelector('#status-log');
    const statusDashboard = document.querySelector('#status-dashboard');
    const statusUpdated = document.querySelector('#status-updated');
    const palette = ['#1f6f5b', '#d1495b', '#2f80ed', '#f2a541', '#7c3aed', '#6b7280'];
    source.value = 'en';
    target.value = 'ko_kp';
    document.querySelector('#url-language').value = 'ko_kp';
    document.querySelector('#pair-source-language').value = 'ko_kp';
    document.querySelector('#pair-target-language').value = 'en';
    document.querySelector('#sentence-source-language').value = 'ko_kp';
    document.querySelector('#sentence-target-language').value = 'en';

    document.querySelector('#translate').addEventListener('click', async () => {{
      result.textContent = 'Working...';
      const response = await fetch('/api/translate', {{
        method: 'POST',
        headers: {{ 'Content-Type': 'application/json' }},
        body: JSON.stringify({{
          source_language: source.value,
          target_language: target.value,
          text: text.value
        }})
      }});
      const data = await response.json();
      if (!response.ok) {{
        result.textContent = data.error || 'Translation failed';
        return;
      }}
      engine.textContent = `Engine: ${{data.engine}}`;
      const score = data.ko_kp_score === null || data.ko_kp_score === undefined ? '' : `\\nko_kp_score: ${{data.ko_kp_score}}`;
      result.textContent = data.translated_text + score;
    }});

    document.querySelector('#import-url').addEventListener('click', async () => {{
      status.textContent = 'Importing URL...';
      const response = await fetch('/api/import-url', {{
        method: 'POST',
        headers: {{ 'Content-Type': 'application/json' }},
        body: JSON.stringify({{
          url: document.querySelector('#url').value,
          language: document.querySelector('#url-language').value
        }})
      }});
      const data = await response.json();
      status.textContent = JSON.stringify(data, null, 2);
      if (response.ok) {{
        document.querySelector('#url').value = '';
        await refreshStatus();
      }}
    }});

    document.querySelector('#rebuild').addEventListener('click', async () => {{
      status.textContent = 'Rebuilding corpus and training local models...';
      const response = await fetch('/api/rebuild', {{ method: 'POST' }});
      const data = await response.json();
      status.textContent = JSON.stringify(data, null, 2);
      if (response.ok && data.project) {{
        renderStatus(data.project);
      }}
    }});

    document.querySelector('#import-url-pair').addEventListener('click', async () => {{
      status.textContent = 'Importing paired URLs...';
      const response = await fetch('/api/import-url-pair', {{
        method: 'POST',
        headers: {{ 'Content-Type': 'application/json' }},
        body: JSON.stringify({{
          source_url: document.querySelector('#source-url').value,
          target_url: document.querySelector('#target-url').value,
          source_language: document.querySelector('#pair-source-language').value,
          target_language: document.querySelector('#pair-target-language').value,
          approve_exact_order: document.querySelector('#approve-exact-order').checked
        }})
      }});
      const data = await response.json();
      status.textContent = JSON.stringify(data, null, 2);
      if (response.ok) {{
        document.querySelector('#source-url').value = '';
        document.querySelector('#target-url').value = '';
        document.querySelector('#approve-exact-order').checked = false;
        await refreshStatus();
      }}
    }});

    document.querySelector('#add-sentence-pair').addEventListener('click', async () => {{
      status.textContent = 'Adding sentence pair...';
      const response = await fetch('/api/add-sentence-pair', {{
        method: 'POST',
        headers: {{ 'Content-Type': 'application/json' }},
        body: JSON.stringify({{
          source_language: document.querySelector('#sentence-source-language').value,
          target_language: document.querySelector('#sentence-target-language').value,
          source_text: document.querySelector('#sentence-source-text').value,
          target_text: document.querySelector('#sentence-target-text').value
        }})
      }});
      const data = await response.json();
      status.textContent = JSON.stringify(data, null, 2);
      if (response.ok) {{
        document.querySelector('#sentence-source-text').value = '';
        document.querySelector('#sentence-target-text').value = '';
        await refreshStatus();
      }}
    }});

    async function refreshStatus() {{
      const response = await fetch('/api/status');
      const data = await response.json();
      status.textContent = formatStatus(data);
      renderStatus(data);
    }}

    document.querySelector('#toggle-status-log').addEventListener('click', () => {{
      const isHidden = status.hasAttribute('hidden');
      if (isHidden) {{
        status.removeAttribute('hidden');
        document.querySelector('#toggle-status-log').textContent = 'JSON 숨기기';
      }} else {{
        status.setAttribute('hidden', '');
        document.querySelector('#toggle-status-log').textContent = 'JSON 보기';
      }}
    }});

    function renderStatus(data) {{
      const readiness = data.training_readiness || {{}};
      statusUpdated.textContent = `상태 갱신: ${{new Date().toLocaleTimeString()}}`;
      statusDashboard.innerHTML = [
        renderMetricCards(data),
        '<div class="chart-grid">',
        renderDonutPanel('Raw 문장 언어별', objectEntries(data.raw_sentences_by_language), '문장'),
        renderDonutPanel('URL쌍 수집상태', [
          ['후보', data.url_pair_candidate_files || 0],
          ['승인', data.url_pair_approved_files || 0]
        ], '파일'),
        renderDonutPanel('문장쌍 방향별', objectEntries(data.parallel_sentences_by_direction), '쌍'),
        renderDonutPanel('단어후보 언어별', objectEntries(data.term_candidates_by_language), '개'),
        renderDonutPanel('성구후보 언어별', objectEntries(data.phrase_candidates_by_language), '개'),
        renderDonutPanel('쌍방향 성구 방향별', objectEntries(data.parallel_phrase_candidates_by_direction), '쌍'),
        '</div>',
        renderReadiness(readiness)
      ].join('');
    }}

    function renderMetricCards(data) {{
      const cards = [
        ['Raw 파일', data.raw_text_files || 0, '원문 URL/파일'],
        ['URL쌍 후보', data.url_pair_candidate_files || 0, '검토 대기'],
        ['URL쌍 승인', data.url_pair_approved_files || 0, '학습에 반영'],
        ['번역메모리', data.translation_memory_entries || 0, '즉시 검색 가능']
      ];
      return `<div class="metric-grid">${{cards.map(([label, value, note]) => `
        <div class="metric-card">
          <div class="metric-label">${{escapeHtml(label)}}</div>
          <div class="metric-value">${{formatNumber(value)}}</div>
          <div class="metric-note">${{escapeHtml(note)}}</div>
        </div>
      `).join('')}}</div>`;
    }}

    function renderDonutPanel(title, rawEntries, totalLabel) {{
      const entries = rawEntries
        .map(([label, value]) => [String(label), Number(value) || 0])
        .filter(([, value]) => value > 0);
      const total = entries.reduce((sum, [, value]) => sum + value, 0);
      const shownEntries = total > 0 ? entries : [['자료없음', 1]];
      const gradient = total > 0 ? conicGradient(entries) : '#e5e7eb';
      return `
        <section class="chart-panel">
          <h2 class="chart-title">${{escapeHtml(title)}}</h2>
          <div class="donut-layout">
            <div class="donut" style="background: ${{gradient}}">
              <div class="donut-total">${{formatNumber(total)}}<span>${{escapeHtml(totalLabel)}}</span></div>
            </div>
            <div class="legend">
              ${{shownEntries.map(([label, value], index) => renderLegendRow(label, total > 0 ? value : 0, total, palette[index % palette.length])).join('')}}
            </div>
          </div>
        </section>
      `;
    }}

    function renderLegendRow(label, value, total, color) {{
      const percent = total > 0 ? Math.round((value / total) * 100) : 0;
      return `
        <div class="legend-row">
          <span class="legend-dot" style="background:${{color}}"></span>
          <span class="legend-label" title="${{escapeHtml(label)}}">${{escapeHtml(label)}}</span>
          <strong>${{formatNumber(value)}} · ${{percent}}%</strong>
        </div>
      `;
    }}

    function renderReadiness(readiness) {{
      const entries = Object.entries(readiness);
      if (!entries.length) {{
        return '<div class="readiness-card"><strong>학습준비</strong><p class="muted">아직 문장쌍이 없습니다.</p></div>';
      }}
      return `<div>
        <h2 class="chart-title">학습준비 진행도</h2>
        <div class="readiness-grid">${{entries.map(([direction, item]) => {{
          const count = Number(item.sentence_pairs) || 0;
          const target = Number(item.next_target) || count || 1;
          const percentValue = Math.min(100, (count / target) * 100);
          const percent = percentValue > 0 && percentValue < 1 ? percentValue.toFixed(2) : Math.round(percentValue);
          return `
            <div class="readiness-card">
              <div class="readiness-head">
                <span>${{escapeHtml(direction)}}</span>
                <span>${{percent}}%</span>
              </div>
              <div class="progress"><div class="progress-fill" style="width:${{percentValue}}%"></div></div>
              <p class="muted">${{formatNumber(count)}} / ${{formatNumber(target)}} · ${{escapeHtml(item.stage || '')}}</p>
            </div>
          `;
        }}).join('')}}</div>
      </div>`;
    }}

    function objectEntries(value) {{
      return Object.entries(value || {{}});
    }}

    function conicGradient(entries) {{
      const total = entries.reduce((sum, [, value]) => sum + value, 0);
      let cursor = 0;
      const stops = entries.map(([, value], index) => {{
        const start = cursor;
        const end = cursor + (value / total) * 100;
        cursor = end;
        const color = palette[index % palette.length];
        return `${{color}} ${{start.toFixed(2)}}% ${{end.toFixed(2)}}%`;
      }});
      return `conic-gradient(${{stops.join(', ')}})`;
    }}

    function formatNumber(value) {{
      return new Intl.NumberFormat().format(Number(value) || 0);
    }}

    function escapeHtml(value) {{
      return String(value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
    }}

    function formatStatus(data) {{
      return [
        '현재 자료상태',
        `raw files: ${{data.raw_text_files}}`,
        `URL쌍 후보파일: ${{data.url_pair_candidate_files}}`,
        `URL쌍 승인파일: ${{data.url_pair_approved_files}}`,
        `번역메모리: ${{data.translation_memory_entries}}`,
        '',
        '언어별 raw 문장수',
        JSON.stringify(data.raw_sentences_by_language, null, 2),
        '',
        '언어별 쌍방향 문장쌍',
        JSON.stringify(data.parallel_sentences_by_direction, null, 2),
        '',
        '언어별 단어 후보수',
        JSON.stringify(data.term_candidates_by_language, null, 2),
        '',
        '언어별 성구 후보수',
        JSON.stringify(data.phrase_candidates_by_language, null, 2),
        '',
        '쌍방향 성구 후보수',
        JSON.stringify(data.parallel_phrase_candidates_by_direction, null, 2),
        '',
        '학습준비 단계',
        JSON.stringify(data.training_readiness, null, 2)
      ].join('\\n');
    }}
    refreshStatus();
  </script>
</body>
</html>"""
