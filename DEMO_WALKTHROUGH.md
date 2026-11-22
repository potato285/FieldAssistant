# Field Assistant — Phase 1 proof-of-concept video script

The v0.4 APK uses real on-device vision and Llama. This is the script for the Phase 1
submission; the final narrated competition video is still outstanding.

Opening narration and on-screen scope statement:

> “Field Assistant is our Phase 1 proof of concept. It runs crop-image classification
> and Llama advice offline on an ARM Android phone. The pretrained classifier has 54
> labels across 19 plants, but crop-specific advice currently covers only three
> conditions: potato early blight, potato late blight and tomato early blight.
> Other findings receive general observation and referral instructions.”

1. Open Field Assistant and select the correct crop. Add your own clear leaf photo
   using gallery or camera. There is no built-in sample-image button.
2. Show the possible finding and the instruction to confirm it with an agricultural
   officer. The model covers 54 labels across 19 plants, with crop-specific advice for
   only the three conditions stated above; it can be wrong or reject a supported crop.
   It does not measure severity or identify sapodilla/mealybug.
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

Closing narration and on-screen Phase 2 objectives:

> “Severity classification is required by the problem statement but is not implemented
> in Phase 1. In Phase 2 we plan to develop and validate it separately; prediction
> confidence is not damage severity. We also plan to expand crop-specific advice,
> improve field accuracy and pest coverage, and validate more phones. The current
> installer requires a debuggable APK and USB provisioning for Llama. This is a
> prototype installation method; production distribution is a Phase 2 objective.”

For an offline demonstration, download models/voices and store the photo locally first.
Only disconnect networking when the phone is not supplying the computer's connection.

Evidence and limitations: [docs/VERIFICATION.md](docs/VERIFICATION.md). The gallery
regression uses a real content URI through the same callback as the gallery, not
system picker taps. The app has no INTERNET permission. Earlier automated airplane-mode
checks passed, and the project owner reports successful manual offline use of the
current app (22 November 2026). Attribute the manual result separately from the logs.

For another phone, use the public [installation procedure](docs/PHONE_SETUP.md).
The source repository and pinned download/setup command replace the large ZIP as the
primary handoff. APKs are GitHub Release assets; model weights come from pinned
upstream URLs. All source/license attribution is included in THIRD_PARTY_NOTICES.md.
