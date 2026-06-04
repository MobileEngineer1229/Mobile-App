const form = document.querySelector("#chatForm");
const input = document.querySelector("#questionInput");
const messages = document.querySelector("#messages");
const statusText = document.querySelector("#statusText");
const hints = document.querySelectorAll(".hint");

async function checkHealth() {
  try {
    const response = await fetch("/api/health");
    const data = await response.json();
    statusText.textContent = data.ready
      ? `벡토르자료기지 준비됨. ${data.chunks}개 청크 색인.`
      : "벡토르자료기지가 준비되지 않았습니다. 색인 스크립트를 실행하세요.";
  } catch {
    statusText.textContent = "백엔드에 접속할 수 없습니다.";
  }
}

function addMessage(role, text, sources = []) {
  const article = document.createElement("article");
  article.className = `message ${role}`;

  const bubble = document.createElement("div");
  bubble.className = "bubble";
  bubble.textContent = text;

  if (sources.length) {
    const sourceList = document.createElement("div");
    sourceList.className = "sources";
    for (const source of sources) {
      const item = document.createElement("div");
      item.className = "source";
      const details = [
        `score ${source.score}`,
        source.rerank ? `rerank ${source.rerank}` : "",
        source.bm25 ? `bm25 ${source.bm25}` : "",
      ].filter(Boolean).join(" · ");
      item.innerHTML = `<strong>${escapeHtml(source.document)}</strong> · ${details}<br>${escapeHtml(source.preview)}`;
      sourceList.appendChild(item);
    }
    bubble.appendChild(sourceList);
  }

  article.appendChild(bubble);
  messages.appendChild(article);
  messages.scrollTop = messages.scrollHeight;
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

async function ask(question) {
  const clean = question.trim();
  if (!clean) return;

  addMessage("user", clean);
  input.value = "";
  input.disabled = true;
  form.querySelector("button").disabled = true;

  try {
    const response = await fetch("/api/chat", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ question: clean }),
    });
    const data = await response.json();
    if (!response.ok) {
      addMessage("assistant", data.error || "요청이 실패했습니다.");
      return;
    }
    addMessage("assistant", data.answer, data.sources || []);
  } catch {
    addMessage("assistant", "백엔드에 접속할 수 없습니다.");
  } finally {
    input.disabled = false;
    form.querySelector("button").disabled = false;
    input.focus();
  }
}

form.addEventListener("submit", (event) => {
  event.preventDefault();
  ask(input.value);
});

hints.forEach((button) => {
  button.addEventListener("click", () => ask(button.dataset.question || ""));
});

checkHealth();
