'use client';

import { useState } from 'react';
import { useFetch } from '@/hooks/useFetch';
import { api } from '@/lib/api';
import { Recipe } from '@/types/api';
import PageHeader from '@/components/PageHeader';
import Modal from '@/components/Modal';
import Pagination from '@/components/Pagination';
import { usePagination } from '@/hooks/usePagination';

// ─── Constants ────────────────────────────────────────────────────────────────

const BABY_AGE_GROUPS = ['0-6', '7-11', '12-17', '18-24', '25-30', '31-36'];
const BABY_MEAL_SLOTS = ['breakfast', 'lunch', 'dinner', 'snack'];
const MUM_MEAL_SLOTS  = ['early_morning', 'breakfast', 'mid_morning', 'lunch', 'evening', 'dinner', 'bedtime'];
const RECIPE_TYPES    = ['puree', 'finger_food', 'snack', 'meal', 'drink', 'other'];

const slotLabel = (s: string) => s.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase());

// ─── Recipe Form Modal ────────────────────────────────────────────────────────

interface RecipeFormData {
  title: string;
  description: string;
  target: 'baby' | 'mum';
  meal_slot: string;
  baby_age_group: string;
  age_range_min_months: string;
  age_range_max_months: string;
  recipe_type: string;
  ingredients: string;   // newline-separated
  instructions: string;  // newline-separated
  prep_time_minutes: string;
  cooking_time_minutes: string;
  image_url: string;
  nutrition_info: string;
}

function RecipeModal({
  initial,
  defaultTarget,
  defaultAgeGroup,
  defaultSlot,
  onClose,
  onSaved,
}: {
  initial?: Recipe;
  defaultTarget: 'baby' | 'mum';
  defaultAgeGroup?: string;
  defaultSlot?: string;
  onClose: () => void;
  onSaved: () => void;
}) {
  const [form, setForm] = useState<RecipeFormData>({
    title: initial?.title ?? '',
    description: initial?.description ?? '',
    target: initial?.target ?? defaultTarget,
    meal_slot: initial?.meal_slot ?? defaultSlot ?? (defaultTarget === 'baby' ? 'breakfast' : 'early_morning'),
    baby_age_group: initial?.baby_age_group ?? defaultAgeGroup ?? '7-11',
    age_range_min_months: String(initial?.age_range_min_months ?? 7),
    age_range_max_months: String(initial?.age_range_max_months ?? 11),
    recipe_type: initial?.recipe_type ?? 'puree',
    ingredients: Array.isArray(initial?.ingredients) ? initial.ingredients.join('\n') : '',
    instructions: Array.isArray(initial?.instructions) ? initial.instructions.join('\n') : '',
    prep_time_minutes: String(initial?.prep_time_minutes ?? ''),
    cooking_time_minutes: String(initial?.cooking_time_minutes ?? ''),
    image_url: initial?.image_url ?? '',
    nutrition_info: initial?.nutrition_info ? JSON.stringify(initial.nutrition_info, null, 2) : '',
  });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const set = (k: keyof RecipeFormData, v: string) => setForm((p) => ({ ...p, [k]: v }));

  const mealSlots = form.target === 'baby' ? BABY_MEAL_SLOTS : MUM_MEAL_SLOTS;

  async function submit(e: React.SyntheticEvent) {
    e.preventDefault();
    if (!form.title.trim()) { setError('Title is required.'); return; }
    let nutritionInfo: Record<string, unknown> | null = null;
    if (form.nutrition_info.trim()) {
      try {
        nutritionInfo = JSON.parse(form.nutrition_info);
      } catch {
        setError('Nutrition info must be valid JSON.');
        return;
      }
    }
    setSaving(true);
    setError('');
    try {
      const payload: Record<string, unknown> = {
        title: form.title.trim(),
        description: form.description || null,
        target: form.target,
        meal_slot: form.meal_slot || null,
        baby_age_group: form.target === 'baby' ? (form.baby_age_group || null) : null,
        age_range_min_months: Number(form.age_range_min_months) || 0,
        age_range_max_months: Number(form.age_range_max_months) || 36,
        recipe_type: form.recipe_type,
        ingredients: form.ingredients.split('\n').map((s) => s.trim()).filter(Boolean),
        instructions: form.instructions.split('\n').map((s) => s.trim()).filter(Boolean),
        prep_time_minutes: form.prep_time_minutes ? Number(form.prep_time_minutes) : null,
        cooking_time_minutes: form.cooking_time_minutes ? Number(form.cooking_time_minutes) : null,
        image_url: form.image_url || null,
        nutrition_info: nutritionInfo,
        language: 'en',
      };
      if (initial?.id) {
        await api.put(`/recipes/${initial.id}`, payload);
      } else {
        await api.post('/recipes', payload);
      }
      onSaved();
      onClose();
    } catch (err: any) {
      setError(err?.response?.data?.error?.message ?? err?.response?.data?.error ?? 'Failed to save.');
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal onClose={onClose}>
      <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between sticky top-0 bg-white z-10">
        <h2 className="text-base font-semibold text-slate-800">
          {initial ? 'Edit Recipe' : 'Add Recipe'}
        </h2>
        <button onClick={onClose} className="text-slate-400 hover:text-slate-600 text-xl leading-none">&times;</button>
      </div>

        <form onSubmit={submit} className="p-6 space-y-4">
          {error && <p className="text-sm text-red-600 bg-red-50 rounded-lg px-3 py-2">{error}</p>}

          {/* Target + meal slot row */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1">For *</label>
              <select
                value={form.target}
                onChange={(e) => {
                  const t = e.target.value as 'baby' | 'mum';
                  const slots = t === 'baby' ? BABY_MEAL_SLOTS : MUM_MEAL_SLOTS;
                  setForm((p) => ({ ...p, target: t, meal_slot: slots[0] }));
                }}
                className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-purple-300"
              >
                <option value="baby">Baby</option>
                <option value="mum">Mum</option>
              </select>
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1">Meal Slot *</label>
              <select
                value={form.meal_slot}
                onChange={(e) => set('meal_slot', e.target.value)}
                className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-purple-300"
              >
                {mealSlots.map((s) => (
                  <option key={s} value={s}>{slotLabel(s)}</option>
                ))}
              </select>
            </div>
          </div>

          {/* Baby age group (only when target=baby) */}
          {form.target === 'baby' && (
            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1">Age Group</label>
              <div className="flex flex-wrap gap-2">
                {BABY_AGE_GROUPS.map((ag) => (
                  <button
                    key={ag}
                    type="button"
                    onClick={() => {
                      const [min, max] = ag.split('-').map(Number);
                      setForm((p) => ({ ...p, baby_age_group: ag, age_range_min_months: String(min), age_range_max_months: String(max) }));
                    }}
                    className={`px-3 py-1 rounded-lg text-xs font-medium border transition-colors ${
                      form.baby_age_group === ag
                        ? 'bg-purple-600 text-white border-purple-600'
                        : 'bg-white text-slate-600 border-slate-200 hover:bg-slate-50'
                    }`}
                  >
                    {ag}m
                  </button>
                ))}
              </div>
            </div>
          )}

          <div>
            <label className="block text-xs font-medium text-slate-600 mb-1">Title *</label>
            <input
              value={form.title}
              onChange={(e) => set('title', e.target.value)}
              placeholder="Recipe name…"
              className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-purple-300"
            />
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-600 mb-1">Description</label>
            <textarea
              value={form.description}
              onChange={(e) => set('description', e.target.value)}
              rows={2}
              className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-purple-300 resize-none"
            />
          </div>

          {/* Type + times row */}
          <div className="grid grid-cols-3 gap-4">
            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1">Type</label>
              <select
                value={form.recipe_type}
                onChange={(e) => set('recipe_type', e.target.value)}
                className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-purple-300"
              >
                {RECIPE_TYPES.map((t) => <option key={t} value={t}>{slotLabel(t)}</option>)}
              </select>
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1">Prep (min)</label>
              <input
                type="number" min={0}
                value={form.prep_time_minutes}
                onChange={(e) => set('prep_time_minutes', e.target.value)}
                className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-purple-300"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1">Cook (min)</label>
              <input
                type="number" min={0}
                value={form.cooking_time_minutes}
                onChange={(e) => set('cooking_time_minutes', e.target.value)}
                className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-purple-300"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-600 mb-1">Ingredients (one per line)</label>
            <textarea
              value={form.ingredients}
              onChange={(e) => set('ingredients', e.target.value)}
              rows={4}
              placeholder={"100g sweet potato\n50ml breast milk"}
              className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm font-mono focus:outline-none focus:ring-2 focus:ring-purple-300 resize-none"
            />
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-600 mb-1">Instructions (one per line)</label>
            <textarea
              value={form.instructions}
              onChange={(e) => set('instructions', e.target.value)}
              rows={4}
              placeholder={"Peel and chop the potato\nSteam for 15 minutes"}
              className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm font-mono focus:outline-none focus:ring-2 focus:ring-purple-300 resize-none"
            />
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-600 mb-1">Image URL</label>
            <input
              value={form.image_url}
              onChange={(e) => set('image_url', e.target.value)}
              placeholder="https://…"
              className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-purple-300"
            />
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-600 mb-1">Nutrition Info JSON</label>
            <textarea
              value={form.nutrition_info}
              onChange={(e) => set('nutrition_info', e.target.value)}
              rows={6}
              placeholder={'{\n  "nutrients": ["Iron", "Vitamin C"],\n  "safety_notes": []\n}'}
              className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm font-mono focus:outline-none focus:ring-2 focus:ring-purple-300 resize-none"
            />
          </div>

          <div className="flex justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-sm text-slate-600 bg-slate-100 rounded-xl hover:bg-slate-200 transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={saving}
              className="px-5 py-2 text-sm font-medium text-white bg-purple-600 rounded-xl hover:bg-purple-700 disabled:opacity-50 transition-colors"
            >
              {saving ? 'Saving…' : initial ? 'Save Changes' : 'Add Recipe'}
            </button>
          </div>
        </form>
    </Modal>
  );
}

// ─── Recipe List ──────────────────────────────────────────────────────────────

function stringList(value: unknown): string[] {
  return Array.isArray(value) ? value.map((item) => String(item)).filter(Boolean) : [];
}

function RecipeDetailModal({
  recipe,
  onClose,
  onEdit,
}: {
  recipe: Recipe;
  onClose: () => void;
  onEdit: () => void;
}) {
  const nutrients = stringList(recipe.nutrition_info?.nutrients);
  const safetyNotes = stringList(recipe.nutrition_info?.safety_notes);
  const source = typeof recipe.nutrition_info?.source === 'string' ? recipe.nutrition_info.source : undefined;

  return (
    <Modal onClose={onClose} maxWidth="max-w-3xl">
      <div className="px-6 py-4 border-b border-slate-100 flex items-start justify-between gap-4 sticky top-0 bg-white z-10">
        <div>
          <div className="flex items-center gap-2 flex-wrap">
            <h2 className="text-base font-semibold text-slate-800">{recipe.title}</h2>
            <span className="px-2 py-0.5 rounded-md text-[11px] font-medium bg-slate-100 text-slate-600 capitalize">
              {recipe.target ?? 'baby'}
            </span>
            <span className="px-2 py-0.5 rounded-md text-[11px] font-medium bg-purple-50 text-purple-700">
              {recipe.baby_age_group ? `${recipe.baby_age_group}m` : `${recipe.age_range_min_months}-${recipe.age_range_max_months}m`}
            </span>
            {source && (
              <span className="px-2 py-0.5 rounded-md text-[11px] font-medium bg-orange-50 text-orange-700">
                UI seed
              </span>
            )}
          </div>
          <p className="text-xs text-slate-500 mt-1">
            {slotLabel(recipe.meal_slot ?? 'meal')} · {slotLabel(recipe.recipe_type ?? 'meal')}
          </p>
        </div>
        <button onClick={onClose} className="text-slate-400 hover:text-slate-600 text-xl leading-none">&times;</button>
      </div>

      <div className="p-6 space-y-6">
        {recipe.image_url && (
          <img src={recipe.image_url} alt={recipe.title} className="w-full h-56 object-cover rounded-xl border border-slate-100" />
        )}

        <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
          <div className="rounded-xl border border-slate-100 p-3">
            <p className="text-[11px] uppercase tracking-wide text-slate-400">Prep</p>
            <p className="text-sm font-semibold text-slate-800">{recipe.prep_time_minutes ?? 0} min</p>
          </div>
          <div className="rounded-xl border border-slate-100 p-3">
            <p className="text-[11px] uppercase tracking-wide text-slate-400">Cook</p>
            <p className="text-sm font-semibold text-slate-800">{recipe.cooking_time_minutes ?? 0} min</p>
          </div>
          <div className="rounded-xl border border-slate-100 p-3">
            <p className="text-[11px] uppercase tracking-wide text-slate-400">Ingredients</p>
            <p className="text-sm font-semibold text-slate-800">{recipe.ingredients?.length ?? 0}</p>
          </div>
          <div className="rounded-xl border border-slate-100 p-3">
            <p className="text-[11px] uppercase tracking-wide text-slate-400">Steps</p>
            <p className="text-sm font-semibold text-slate-800">{recipe.instructions?.length ?? 0}</p>
          </div>
        </div>

        <div>
          <h3 className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-2">Description</h3>
          <p className="text-sm text-slate-700 leading-6">{recipe.description || 'No description yet.'}</p>
        </div>

        <div className="grid md:grid-cols-2 gap-5">
          <div>
            <h3 className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-2">Ingredients</h3>
            {recipe.ingredients?.length ? (
              <ol className="space-y-2">
                {recipe.ingredients.map((item, index) => (
                  <li key={`${item}-${index}`} className="text-sm text-slate-700 leading-5 flex gap-2">
                    <span className="text-slate-400">{index + 1}.</span>
                    <span>{item}</span>
                  </li>
                ))}
              </ol>
            ) : (
              <p className="text-sm text-slate-400">No ingredients listed.</p>
            )}
          </div>

          <div>
            <h3 className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-2">Instructions</h3>
            {recipe.instructions?.length ? (
              <ol className="space-y-2">
                {recipe.instructions.map((item, index) => (
                  <li key={`${item}-${index}`} className="text-sm text-slate-700 leading-5 flex gap-2">
                    <span className="text-slate-400">{index + 1}.</span>
                    <span>{item}</span>
                  </li>
                ))}
              </ol>
            ) : (
              <p className="text-sm text-slate-400">No instructions listed.</p>
            )}
          </div>
        </div>

        <div>
          <h3 className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-2">Nutrients</h3>
          <div className="flex flex-wrap gap-2">
            {(nutrients.length ? nutrients : ['No nutrients listed']).map((item) => (
              <span key={item} className="px-2 py-1 rounded-md text-xs bg-green-50 text-green-700">{item}</span>
            ))}
          </div>
        </div>

        {safetyNotes.length > 0 && (
          <div>
            <h3 className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-2">Safety Notes</h3>
            <ul className="space-y-2">
              {safetyNotes.map((note, index) => (
                <li key={`${note}-${index}`} className="text-sm text-slate-700 leading-5">{note}</li>
              ))}
            </ul>
          </div>
        )}

        {recipe.nutrition_info && (
          <details className="rounded-xl border border-slate-100">
            <summary className="cursor-pointer px-4 py-3 text-sm font-medium text-slate-700">Raw nutrition JSON</summary>
            <pre className="px-4 pb-4 text-xs text-slate-600 overflow-x-auto">{JSON.stringify(recipe.nutrition_info, null, 2)}</pre>
          </details>
        )}

        <div className="flex justify-end gap-3 pt-2">
          <button onClick={onClose} className="px-4 py-2 text-sm text-slate-600 bg-slate-100 rounded-xl hover:bg-slate-200 transition-colors">
            Close
          </button>
          <button onClick={onEdit} className="px-5 py-2 text-sm font-medium text-white bg-purple-600 rounded-xl hover:bg-purple-700 transition-colors">
            Edit Recipe
          </button>
        </div>
      </div>
    </Modal>
  );
}

function RecipeList({
  recipes,
  loading,
  onView,
  onEdit,
  onDelete,
  deleting,
}: {
  recipes: Recipe[];
  loading: boolean;
  onView: (r: Recipe) => void;
  onEdit: (r: Recipe) => void;
  onDelete: (r: Recipe) => void;
  deleting: number | null;
}) {
  if (loading) return <div className="flex items-center justify-center h-48 text-slate-400 text-sm">Loading…</div>;
  if (!recipes.length) return <div className="flex items-center justify-center h-48 text-slate-400 text-sm">No recipes yet</div>;

  return (
    <div className="divide-y divide-slate-50">
      {recipes.map((r) => (
        <div key={r.id} className="px-5 py-3 flex items-start gap-3 hover:bg-slate-50 transition-colors group">
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 flex-wrap">
              <span className="text-sm font-medium text-slate-800">{r.title}</span>
              <span className="px-1.5 py-0.5 rounded text-[10px] font-medium bg-slate-100 text-slate-500 capitalize">
                {r.recipe_type?.replace('_', ' ')}
              </span>
              {(r.prep_time_minutes || r.cooking_time_minutes) && (
                <span className="text-[10px] text-slate-400">
                  {(r.prep_time_minutes ?? 0) + (r.cooking_time_minutes ?? 0)} min
                </span>
              )}
            </div>
            {r.description && (
              <p className="text-xs text-slate-500 mt-0.5 line-clamp-1">{r.description}</p>
            )}
          </div>
          <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity shrink-0">
            <button
              onClick={() => onView(r)}
              className="px-2.5 py-1 text-xs text-slate-600 bg-slate-100 rounded-lg hover:bg-slate-200 transition-colors"
            >
              Details
            </button>
            <button
              onClick={() => onEdit(r)}
              className="px-2.5 py-1 text-xs text-blue-600 bg-blue-50 rounded-lg hover:bg-blue-100 transition-colors"
            >
              Edit
            </button>
            <button
              onClick={() => onDelete(r)}
              disabled={deleting === r.id}
              className="px-2.5 py-1 text-xs text-red-600 bg-red-50 rounded-lg hover:bg-red-100 disabled:opacity-50 transition-colors"
            >
              {deleting === r.id ? '…' : 'Delete'}
            </button>
          </div>
        </div>
      ))}
    </div>
  );
}

// ─── Baby Tab ─────────────────────────────────────────────────────────────────

function BabyTab() {
  const [ageGroup, setAgeGroup] = useState('7-11');
  const [mealSlot, setMealSlot] = useState<string>('All');
  const [modal, setModal] = useState<{ open: boolean; recipe?: Recipe }>({ open: false });
  const [detail, setDetail] = useState<Recipe | null>(null);
  const [deleting, setDeleting] = useState<number | null>(null);

  const url = `/recipes/baby/age-group/${ageGroup}${mealSlot !== 'All' ? `?meal_slot=${mealSlot}` : ''}`;
  const { data: recipesRaw, loading, refetch } = useFetch<Recipe[]>(url);
  const recipes = recipesRaw ?? [];
  const { paged, page, totalPages, setPage } = usePagination(recipes);

  async function handleDelete(r: Recipe) {
    if (!confirm(`Delete "${r.title}"?`)) return;
    setDeleting(r.id);
    try { await api.delete(`/recipes/${r.id}`); refetch(); } finally { setDeleting(null); }
  }

  return (
    <div>
      {/* Age group row */}
      <div className="px-5 py-3 border-b border-slate-100 flex flex-wrap items-center gap-2">
        <span className="text-xs font-medium text-slate-500 mr-1">Age Group:</span>
        {BABY_AGE_GROUPS.map((ag) => (
          <button
            key={ag}
            onClick={() => setAgeGroup(ag)}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
              ageGroup === ag ? 'bg-purple-600 text-white' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
            }`}
          >
            {ag}m
          </button>
        ))}
      </div>

      {/* Meal slot row */}
      <div className="px-5 py-2.5 border-b border-slate-100 flex flex-wrap items-center gap-2">
        <span className="text-xs font-medium text-slate-500 mr-1">Meal Slot:</span>
        {['All', ...BABY_MEAL_SLOTS].map((s) => (
          <button
            key={s}
            onClick={() => setMealSlot(s)}
            className={`px-2.5 py-1 rounded-lg text-xs font-medium transition-colors capitalize ${
              mealSlot === s ? 'bg-orange-500 text-white' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
            }`}
          >
            {s}
          </button>
        ))}
        <div className="ml-auto">
          <button
            onClick={() => setModal({ open: true })}
            className="px-3 py-1.5 text-xs font-medium text-white bg-purple-600 rounded-lg hover:bg-purple-700 transition-colors"
          >
            + Add Recipe
          </button>
        </div>
      </div>

      <RecipeList
        recipes={paged}
        loading={loading}
        onView={setDetail}
        onEdit={(r) => setModal({ open: true, recipe: r })}
        onDelete={handleDelete}
        deleting={deleting}
      />
      <Pagination page={page} totalPages={totalPages} onPage={setPage} />

      <div className="px-5 py-2.5 text-xs text-slate-400 border-t border-slate-50">
        {recipes.length} recipes · age group {ageGroup}m{mealSlot !== 'All' ? ` · ${mealSlot}` : ''}
      </div>

      {modal.open && (
        <RecipeModal
          initial={modal.recipe}
          defaultTarget="baby"
          defaultAgeGroup={ageGroup}
          defaultSlot={mealSlot !== 'All' ? mealSlot : 'breakfast'}
          onClose={() => setModal({ open: false })}
          onSaved={refetch}
        />
      )}
      {detail && (
        <RecipeDetailModal
          recipe={detail}
          onClose={() => setDetail(null)}
          onEdit={() => {
            setModal({ open: true, recipe: detail });
            setDetail(null);
          }}
        />
      )}
    </div>
  );
}

// ─── Mum Tab ──────────────────────────────────────────────────────────────────

function MumTab() {
  const [mealSlot, setMealSlot] = useState<string>('All');
  const [modal, setModal] = useState<{ open: boolean; recipe?: Recipe }>({ open: false });
  const [detail, setDetail] = useState<Recipe | null>(null);
  const [deleting, setDeleting] = useState<number | null>(null);

  const url = mealSlot === 'All' ? '/recipes/mum' : `/recipes/mum/slot/${mealSlot}`;
  const { data: recipesRaw, loading, refetch } = useFetch<Recipe[]>(url);
  const recipes = recipesRaw ?? [];
  const { paged, page, totalPages, setPage } = usePagination(recipes);

  async function handleDelete(r: Recipe) {
    if (!confirm(`Delete "${r.title}"?`)) return;
    setDeleting(r.id);
    try { await api.delete(`/recipes/${r.id}`); refetch(); } finally { setDeleting(null); }
  }

  return (
    <div>
      {/* Meal slot row */}
      <div className="px-5 py-3 border-b border-slate-100 flex flex-wrap items-center gap-2">
        <span className="text-xs font-medium text-slate-500 mr-1">Meal Slot:</span>
        {['All', ...MUM_MEAL_SLOTS].map((s) => (
          <button
            key={s}
            onClick={() => setMealSlot(s)}
            className={`px-2.5 py-1.5 rounded-lg text-xs font-medium transition-colors capitalize ${
              mealSlot === s ? 'bg-pink-500 text-white' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
            }`}
          >
            {slotLabel(s)}
          </button>
        ))}
        <div className="ml-auto">
          <button
            onClick={() => setModal({ open: true })}
            className="px-3 py-1.5 text-xs font-medium text-white bg-purple-600 rounded-lg hover:bg-purple-700 transition-colors"
          >
            + Add Recipe
          </button>
        </div>
      </div>

      <RecipeList
        recipes={paged}
        loading={loading}
        onView={setDetail}
        onEdit={(r) => setModal({ open: true, recipe: r })}
        onDelete={handleDelete}
        deleting={deleting}
      />
      <Pagination page={page} totalPages={totalPages} onPage={setPage} />

      <div className="px-5 py-2.5 text-xs text-slate-400 border-t border-slate-50">
        {recipes.length} recipes{mealSlot !== 'All' ? ` · ${slotLabel(mealSlot)}` : ''}
      </div>

      {modal.open && (
        <RecipeModal
          initial={modal.recipe}
          defaultTarget="mum"
          defaultSlot={mealSlot !== 'All' ? mealSlot : 'early_morning'}
          onClose={() => setModal({ open: false })}
          onSaved={refetch}
        />
      )}
      {detail && (
        <RecipeDetailModal
          recipe={detail}
          onClose={() => setDetail(null)}
          onEdit={() => {
            setModal({ open: true, recipe: detail });
            setDetail(null);
          }}
        />
      )}
    </div>
  );
}

// ─── Main Page ────────────────────────────────────────────────────────────────

export default function RecipesPage() {
  const [tab, setTab] = useState<'baby' | 'mum'>('baby');

  return (
    <div className="p-8">
      <PageHeader title="Recipes" subtitle="Manage baby and mum meal plans" />

      <div className="bg-white rounded-2xl shadow-sm border border-slate-100">
        {/* Tab bar */}
        <div className="px-5 pt-4 flex gap-1 border-b border-slate-100">
          {(['baby', 'mum'] as const).map((t) => (
            <button
              key={t}
              onClick={() => setTab(t)}
              className={`px-5 py-2.5 rounded-t-lg text-sm font-medium transition-colors capitalize border-b-2 -mb-px ${
                tab === t
                  ? 'text-purple-700 border-purple-600 bg-purple-50/50'
                  : 'text-slate-500 border-transparent hover:text-slate-700'
              }`}
            >
              {t === 'baby' ? '👶 Baby' : '👩 Mum'}
            </button>
          ))}
        </div>

        {tab === 'baby' ? <BabyTab /> : <MumTab />}
      </div>
    </div>
  );
}
