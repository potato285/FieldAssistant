# Field Assistant v0.4 phone demonstration

The updated APK uses real on-device vision and Llama. Keep the test phone's mobile
data enabled: it supplies the laptop's internet. Do not repeat airplane-mode checks
on this shared connection. A final narrated competition video is still outstanding.

1. Open Field Assistant and select the correct crop. Add your own clear leaf photo
   using gallery or camera. There is no built-in sample-image button.
2. Show the possible finding and the instruction to confirm it with an agricultural
   officer. The model covers 54 labels across 19 plants; it can be wrong or reject a
   supported crop. It does not measure severity or identify sapodilla/mealybug.
3. Wait for “Your next steps are ready”. Llama selects and orders sourced guidance
   and attempts a separate English explanation. A failed call is explicitly marked
   as saved tips. In this development build, Developer details shows the engine,
   elapsed time, action IDs and Fallback value. Keep that section collapsed for farmers.
4. Show potato care, treatment options and prevention. Generic reminders appear in
   smaller text. Named fungicide options require a local officer/current label;
   the app supplies no dose, spray schedule or guaranteed cure.
5. Switch to Tamil and Telugu. These are saved translated instructions, not automatic
   translations of the English Llama explanation. Translation review is outstanding.
6. Tap Listen, Pause reading, Resume reading and Read from the start. Tap Stop.
   If unavailable on another phone, tap Set up offline voices and install the chosen
   language once. Playback is tested; ask a native speaker to assess pronunciation.
7. Choose Sapodilla to show the unsupported state. Open About this app & sources for
   model scope and source attribution.

Evidence and limitations: [docs/VERIFICATION.md](docs/VERIFICATION.md). The gallery
regression uses a real content URI through the same callback as the gallery, not
system picker taps. The current version has no INTERNET permission; its device tests
kept mobile data enabled. Earlier airplane-mode evidence is historical.

For another phone, use the public [installation procedure](docs/PHONE_SETUP.md).
The source repository and pinned download/setup command replace the large ZIP as the
primary handoff. APKs are GitHub Release assets; model weights come from pinned
upstream URLs. All source/license attribution is included in THIRD_PARTY_NOTICES.md.
