const form = document.querySelector("#profileForm");
const statusEl = document.querySelector("#status");
const resultEl = document.querySelector("#result");
const emptyEl = document.querySelector("#emptyState");

let latestRoutine = null;

async function health() {
  try {
    const res = await fetch("/api/health");
    const data = await res.json();
    statusEl.textContent = data.database_ready
      ? `Database ready. Feedback records: ${data.feedback.count}.`
      : "Database is not ready. Run scripts/ingest_datasets.py.";
  } catch {
    statusEl.textContent = "Backend is not reachable.";
  }
}

function formPayload() {
  const data = new FormData(form);
  const payload = {};
  for (const [key, value] of data.entries()) {
    payload[key] = value;
  }
  return payload;
}

form.addEventListener("submit", async (event) => {
  event.preventDefault();
  const button = form.querySelector("button");
  button.disabled = true;
  button.textContent = "Creating...";
  try {
    const res = await fetch("/api/routine", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(formPayload()),
    });
    const data = await res.json();
    if (!res.ok) {
      renderError(data.error || "Failed to create routine.");
      return;
    }
    latestRoutine = data;
    renderRoutine(data);
  } catch {
    renderError("Backend is not reachable.");
  } finally {
    button.disabled = false;
    button.textContent = "Create routine";
  }
});

function renderError(message) {
  emptyEl.classList.add("hidden");
  resultEl.classList.remove("hidden");
  resultEl.innerHTML = `<div class="hero"><h2>Could not create routine</h2><p>${escapeHtml(message)}</p></div>`;
}

function renderRoutine(data) {
  emptyEl.classList.add("hidden");
  resultEl.classList.remove("hidden");
  const p = data.profile_summary;
  const plan = data.plan;
  resultEl.innerHTML = `
    <section class="hero">
      <h2>${escapeHtml(plan.name)}</h2>
      <p>${escapeHtml(plan.summary)}</p>
    </section>

    <section class="metrics">
      <div class="metric"><span>BMI</span><strong>${p.bmi}</strong></div>
      <div class="metric"><span>Plan code</span><strong>${plan.code}</strong></div>
      <div class="metric"><span>Intensity</span><strong>${escapeHtml(plan.intensity)}</strong></div>
      <div class="metric"><span>Cycle</span><strong>${plan.cycle_weeks} weeks</strong></div>
      <div class="metric"><span>Training</span><strong>${plan.days_per_week}/week</strong></div>
      <div class="metric"><span>Available</span><strong>${plan.available_days_per_week}/week</strong></div>
      <div class="metric"><span>Deload</span><strong>Week ${plan.deload_week || "-"}</strong></div>
    </section>

    <section class="card notes">
      <h3>Predicted training cycle</h3>
      <p>${escapeHtml(data.cycle?.reason || "")}</p>
    </section>

    <section class="week">
      ${(data.cycle?.schedule || [{ week: 1, focus: "Week 1", days: data.week || [] }]).map(renderWeek).join("")}
    </section>

    <section class="card notes">
      <h3>Progression</h3>
      <ul>${data.progression.map((x) => `<li>${escapeHtml(x)}</li>`).join("")}</ul>
    </section>

    <section class="card notes">
      <h3>Safety notes</h3>
      <ul>${data.safety_notes.map((x) => `<li>${escapeHtml(x)}</li>`).join("")}</ul>
    </section>

    <section class="card evidence">
      <h3>Dataset evidence</h3>
      <ul>
        <li>Similar cases used: ${data.dataset_evidence.similar_profile_cases.length}</li>
        <li>Activity matches: ${data.dataset_evidence.activity_dataset_matches.map((x) => `${x.activity} (${x.source_samples})`).join(", ")}</li>
        <li>Saved feedback records: ${data.dataset_evidence.feedback.count}</li>
      </ul>
    </section>

    <section class="card">
      <h3>Feedback for future tuning</h3>
      <form id="feedbackForm" class="feedback">
        <input name="rating" type="number" min="1" max="5" placeholder="1-5 rating" required />
        <select name="difficulty">
          <option>Too easy</option>
          <option selected>Good</option>
          <option>Too hard</option>
        </select>
        <input name="notes" type="text" placeholder="What happened during this routine?" />
        <button type="submit">Save</button>
      </form>
    </section>
  `;
  document.querySelector("#feedbackForm").addEventListener("submit", submitFeedback);
}

function renderDay(day) {
  return `
    <article class="day-card">
      <h3>Day ${day.day}: ${escapeHtml(day.title)} · ${day.duration_min} min</h3>
      <p>Estimated calories: ${day.estimated_calories} · Target effort: ${escapeHtml(day.target_effort)}</p>
      <div class="parts">
        ${day.structure.map((part) => `
          <div class="part">
            <strong>${escapeHtml(part.part)} · ${part.minutes} min</strong><br>
            ${escapeHtml(part.details)}
          </div>
        `).join("")}
      </div>
    </article>
  `;
}

function renderWeek(week) {
  return `
    <section class="card week-card">
      <h3>Week ${week.week}: ${escapeHtml(week.focus)}</h3>
      <div class="parts">
        ${(week.days || []).map(renderDay).join("")}
      </div>
    </section>
  `;
}

async function submitFeedback(event) {
  event.preventDefault();
  const data = new FormData(event.currentTarget);
  const payload = {
    plan_id: latestRoutine?.plan_id,
    rating: Number(data.get("rating")),
    difficulty: data.get("difficulty"),
    completed: true,
    notes: data.get("notes"),
    profile: latestRoutine?.profile_summary,
    routine: latestRoutine,
  };
  const res = await fetch("/api/feedback", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  const body = await res.json();
  alert(body.message || body.error || "Saved");
  health();
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

health();
