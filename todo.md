# Mobile & Web App Development Challenges

## App List

- [ ] 1. [Baby Grow App](./01-baby-grow-app.md)
- [ ] 2. [Learning Russian App](./02-learning-russian-app.md)
- [ ] 3. [Notion App](./03-notion-app.md)
- [ ] 4. [Tour App](./04-tour-app.md)
- [ ] 5. [Hotel Service Management App](./05-hotel-service-management-app.md)
- [ ] 6. [Agriculture Mobile App](./06-agriculture-mobile-app.md)
- [ ] 7. [Clinic Management App](./07-clinic-management-app.md)
- [ ] 8. [IoT Smart Home](./08-iot-smart-home.md)
- [ ] 9. [Girl's Health Management](./09-girls-health-management.md)
- [ ] 10. [Caroli Analyze (Calorie Tracker)](./10-caroli-analyze.md)
- [ ] 11. [Shop Assets Management App (RFID)](./11-shop-assets-management-rfid.md)
- [ ] 12. [Yoga App](./12-yoga-app.md)
- [ ] 13. [Health-Track (Detect System) - Using AI Model](./13-health-track-ai.md)
- [ ] 14. [AI Model Setup for HealthCare & Agriculture](./14-ai-model-healthcare-agriculture.md)
- [ ] 15. [Height Increase App](./15-height-increase-app.md)
- [ ] 16. [Women Health Management App](./16-women-health-management-app.md)
- [ ] 17. [AR/AI Hair Style App](./17-ar-ai-hair-style-app.md)
- [ ] 18. [Workout App](./18-workout-app.md)
- [ ] 19. [AI Assistant with DPRK Language (Generative AI from scratch)](./19-ai-assistant-dprk-language.md)
- [ ] 20. [World Cuisines App](./20-world-cuisines-app.md)
- [ ] 21. [AI Old Friends & Relatives Finder](./21-ai-old-friends-relatives-finder.md)

---

### 20. World Cuisines App

**Overview:** A comprehensive mobile app that takes users on a global culinary journey — exploring authentic recipes, cooking traditions, cultural stories, and ingredients from every corner of the world. Built for food lovers, home cooks, travelers, and culture enthusiasts who want to discover, learn, and recreate the world's most beloved dishes at home.

**Target Audience:** Home cooks, food enthusiasts, travelers, culinary students, expats reconnecting with home cuisine.

**Core Features:**

1. **World Cuisine Explorer** — Browse 195+ countries by continent/region with curated cuisine profiles, cultural background, history, and seasonal/festival food highlights.
2. **Recipe Discovery & Search** — 100,000+ authentic recipes with advanced filters: cuisine, dietary type (Vegan, Halal, Gluten-Free, etc.), cooking time, difficulty, available ingredients, and calorie range.
3. **Step-by-Step Cooking Mode** — Fullscreen hands-free mode with voice-guided instructions, per-step timers, ingredient checklist, embedded technique video clips, and Chef's Notes for regional authenticity.
4. **Cultural Food Stories** — Deep-dive articles, chef spotlights, short food documentary videos, dish origin facts, and traditional festival food guides.
5. **Ingredient Encyclopedia** — 5,000+ global ingredients with origin, flavor profile, substitutions, allergen info, and AR camera mode to identify fresh produce and spices.
6. **Meal Planning & Shopping List** — Weekly drag-and-drop planner, auto-generated shopping lists grouped by category, pantry tracker, budget estimator, and list sharing via WhatsApp/SMS.
7. **AI-Powered Recommendations** — Personalized recipe feed, "What to cook tonight?" AI assistant based on available ingredients and time, AI Flavor Pairing across global cuisines, and nutritional optimization suggestions.
8. **Nutritional Information** — Full macro/micro breakdown per recipe, daily intake comparison, healthier swap suggestions, and integration with Apple Health / Google Fit.
9. **Community & Social** — User-submitted recipes, dish photo sharing, ratings/reviews, "Cooked It" tracker, weekly cuisine challenges, and following favorite cooks/chefs.
10. **Offline Mode** — Download up to 50 recipes with full offline access; sync when back online.
11. **Language & Localization** — App UI in 20+ languages, original + translated dish names, pronunciation audio guide, and metric/imperial/traditional unit toggle.
12. **Cooking Classes & Tutorials** — Short-form video lessons by cuisine, beginner skill tracks, technique library, and live cooking events with guest chefs.
13. **Restaurant & Travel Integration** — "Eat Like a Local" city guides, nearby restaurant finder by cuisine, menu item scanner to find the home recipe, and travel food diary with location tags.
14. **Gamification & Achievements** — Cuisine Passport (stamps per country cooked), achievement badges, weekly streak system, friend leaderboard, and leveling system from Culinary Novice → World Chef.

**Tech Stack:** Flutter (mobile) · Node.js/NestJS (backend) · PostgreSQL + MongoDB · Elasticsearch (recipe search) · Python FastAPI (AI/ML) · AWS S3 (media) · Firebase Auth · Google ML Kit / ARCore (AR)

**Monetization:** Freemium (5,000 free recipes) + Premium subscription ($4.99/mo) + Cuisine packs ($2.99 one-time) + Affiliate ingredient links

**MVP Scope:** 30 countries, 500+ recipes, cooking mode, basic search/filters, favorites, shopping list, auth, nutritional info, offline download.

- [ ] 20. [World Cuisines App](./20-world-cuisines-app.md)

---

### 21. AI Old Friends & Relatives Finder

**Overview:** An AI-powered mobile app that helps elderly people reconnect with lost friends and relatives from decades past. Before the era of mobile phones and the internet, people lost touch easily — moving cities, changing jobs, or simply fading away. This app uses AI to intelligently match users based on shared schools, hometowns, family trees, and relationship networks, giving people a second chance to find those they once knew.

**Target Audience:** Elderly adults (50+), middle-aged people seeking childhood friends, diaspora communities, adoptees searching for birth families, anyone who lost contact before the digital age.

**Core Features:**

1. **Profile & Memory Builder** — Users input their life history: birthplace, elementary school name & graduation year, middle school name & graduation year, high school, university, workplaces, neighborhoods lived in, and time periods for each.
2. **Family & Relative Tree Input** — Build a partial family tree with known relatives (parents' names, siblings, aunts/uncles, cousins) including approximate birth years and last-known locations. AI fills in likely connections from the network.
3. **AI Smart Matching Engine** — Semantic AI matching across user profiles: same school + overlapping years = strong match signal. Fuzzy name matching handles name changes (marriage, transliteration) and spelling variants.
4. **School Alumni Network** — Search by school name, region, and graduation year range. Alumni confirm classmates from shared memory prompts ("Do you remember the teacher named…?", "Was your classroom on the 2nd floor?").
5. **AI-Powered Memory Prompts** — To verify authentic connections, AI generates era-specific memory prompts: local events, popular songs, school customs from that year. Only real classmates can answer correctly.
6. **Relationship Chain Discovery** — "Friend of a friend" graph traversal: if User A knows User B, and User B knows User C, the app surfaces C as a possible connection to A — like Six Degrees of Separation powered by AI.
7. **Voice & Photo Input** — Elderly users can describe people verbally ("my friend from 1972 with a scar on his chin who lived near the river market"). AI transcribes and extracts searchable attributes. Old photo upload with face-aging AI to match against current profile photos.
8. **Privacy-First Matching** — All matches are double opt-in: both parties must consent before contact details are shared. Users control visibility of their profile.
9. **Reunion Coordination** — Once connected, built-in chat, voice call, and group reunion planner with shared photo albums and memory timelines.
10. **Diaspora & Migration Support** — Special support for people who emigrated or were displaced: country-of-origin school records, refugee community networks, and multi-language name matching (romanization, Cyrillic, Hangul, etc.).
11. **AI Obituary & Memorial Mode** — Gracefully handle cases where the sought person has passed: memorial page with tributes from those who knew them, family notification with consent.
12. **Community Boards by Era & Region** — Open discussion boards organized by decade and region: "Pyongyang 1970s", "Seoul Mapo-gu 1980s", "Yanbian 1960s" — people post memories and others self-identify.

**Tech Stack:** Flutter (mobile) · Node.js/NestJS (backend) · PostgreSQL (profiles & relationships) · Neo4j (relationship graph) · Python FastAPI (AI/ML) · OpenAI / Claude API (semantic matching, memory prompts) · AWS Rekognition (face aging & matching) · Firebase Auth · ElasticSearch (fuzzy name search)

**AI Components:**
- **Semantic Name Matching** — Handles spelling variants, transliterations, and name changes across decades
- **Era-Aware Memory Verification** — LLM generates culturally accurate prompts by year and region to verify authentic connections
- **Face Age Progression** — Estimate current appearance from old photos using GAN-based age progression
- **Relationship Graph Inference** — Graph neural network to infer likely connections from partial family/social trees

**Monetization:** Free basic search + Premium ($3.99/mo) for unlimited matches, photo aging, and priority AI matching. One-time reunion group fee ($9.99) for group features.

**MVP Scope:** Profile creation with school/year input, basic AI name + school matching, opt-in contact reveal, simple chat, community boards by region/decade.

- [ ] 21. [AI Old Friends & Relatives Finder](./21-ai-old-friends-relatives-finder.md)
