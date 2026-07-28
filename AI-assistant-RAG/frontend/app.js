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
      ? `Vector data base is ready. ${data.chunks}dog chunk index.`
      : "Vector data base is not ready. Run the index script.";
  } catch {
    statusText.textContent = "Unable to connect to backend.";
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
      addMessage("assistant", data.error || "request failed.");
      return;
    }
    addMessage("assistant", data.answer, data.sources || []);
  } catch {
    addMessage("assistant", "Unable to connect to backend.");
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
