# Field Assistant — Phase 1 proof-of-concept technical report

Version 0.4.0, Bharat AI–SoC Challenge 2026–27. **Built with Llama.** This report
separates demonstrated runtime integration from unmeasured field accuracy.

**Phase 1 scope:** 54 classification labels across 19 plants, but **only three
conditions have crop-specific advice: potato early blight, potato late blight and
tomato early blight**. All other findings receive general observation/referral guidance.
**Severity classification is required by the problem statement but is not implemented.
It is explicitly a Phase 2 objective**, not a completed Phase 1 capability.

## System and scope

Native Kotlin/Jetpack Compose Android app. Farmer selects/corrects crop and supplies
a leaf photo through camera/gallery. Models and guidance run on the phone. No server,
API key or INTERNET permission. Supported device requirements and setup are in
[PHONE_SETUP.md](docs/PHONE_SETUP.md); only Nothing A001 has been tested.

`VisionEngine` is replaceable image inference; `ResultAggregator` checks support,
ambiguity and crop conflict; `GuidanceRepository` retrieves sourced local actions;
`AdvisoryEngine` runs Llama or reports a labeled local fallback. `FieldViewModel`
serializes native inference off the UI thread. Guidance and UI support three languages.

## Vision contract

Third-party **Plantulus Doctor / KumarjitC**, EfficientNetV2-B0, pinned revision
`ee2cb607a33fefa85ccc567988c98fc605fa36bd`. The team did not train this model.

- 11,910,620-byte `.tflite`, float16 weights; float32 input/output.
- 54 classes across 19 plants; exact order in `assets/vision/classes.json`.
- ImageDecoder applies image orientation; software sRGB decode bounds large inputs
  to 2048 pixels. Resize shortest side to 256, bilinear interpolation; center crop
  to 256 then 224, following the inspected upstream contract.
- Input `[1,224,224,3]`, RGB float32 **0–255**, without dividing by 255.
- Output `[1,54]`, softmax; real LiteRT 1.4.2 interpreter, CPU/XNNPACK, four threads.
- Model/hash/tensor/label checks run locally. Hashes are in `models/manifest.json`.

The prototype treats score <0.65 or top-two margin <0.15 as uncertain. These heuristics
are not calibrated or validated unknown-image detection. A high score is not a disease
probability, accuracy estimate or severity measure. Cross-crop conflicts can occur even
when the user's selected crop is correct. Unrelated photos can still score highly.

## Llama / ExecuTorch implementation

Pinned **Llama 3.2 1B Instruct SpinQuant INT4 EO8** community export, ExecuTorch **0.6.0**,
ARM CPU. The model and tokenizer live in private `files/llama/` storage and are checked
against pinned hashes. No Hailo or cloud path. The historical export/runtime API was
inspected rather than assuming that any `.pte` and runtime version are compatible.

The first generation chooses allowed sourced action IDs in a JSON object, including
expert referral. Validation rejects unknown/duplicate IDs and inappropriate format;
one corrective retry is allowed. When crop-specific care/prevention is available,
selection must include such an action. Generic observation reminders remain separately
visible. The early five-action limit was corrected to match eligible action counts.

A second call generates a short English explanation from selected nonchemical facts
for supported possible findings. `ExplanationGuard` screens format and restricted
certainty/treatment topics. It is **not semantic factual verification** and cannot
rule out hallucinations. Rejected explanations are omitted with a visible message.
Chemical instruction text is never freely authored by this generation path.

If model loading/generation/selection fails, saved guidance is clearly labeled as
fallback. Valid selection does not guarantee successful explanation generation; those
statuses are handled separately. Real phone tests demonstrate execution, not complete
competition validation or broad advisory correctness.

## Guidance, languages and speech

**Only three conditions have crop-specific advice:** tomato early blight, potato early
blight and potato late blight, with a fixed Tamil Nadu, India region. All other findings use general observation/referral
reminders. TNAU, UC IPM, PNW Extension and Manitoba source URLs/scope/dates are in JSON.
Potato fungicide names are conditional officer/label discussion points, with no dose
or schedule. No agronomist approval or current registration audit is claimed.

UI and action text are saved in English/Tamil/Telugu. The language toggle looks up
stored wording, not runtime translation. The generated explanation is English only.
Native-speaker and agronomic review remain incomplete.

OfflineSpeech selects installed voices that do not require a network. Text is split
into bounded sentence chunks, synthesized to private temporary WAV files, then played
using MediaPlayer. This enables real audio-position pause/resume, restart and stop.
Audio is discarded on normal completion/stop; speech pauses on background/audio-focus
loss/headphone disconnection. Local speech-engine data is downloaded separately once.
Acoustic quality is not established by automated playback/state tests.

## Measurements and evidence

| Check | Measured result | Scope |
| --- | --- | --- |
| v0.4 build | Debug and unsigned release compile; 19 unit tests; lint 0 errors/9 warnings | Host, not Android runtime |
| v0.4 potato photo | `potato_early_blight`, score 0.87629944 | Actual content-URI fixture, may overlap training |
| v0.4 Llama | 22,876 ms; real action selection and English explanation | One final gallery regression run |
| v0.4 speech | en/ta/te positions held 821/831/845 ms through pause; resume/restart passed | Installed local voices, physical phone |
| v0.4 installer | Existing model verified, APK updated, cold launch 706 ms | Previously provisioned Nothing A001 |
| Initial vision | Tomato early blight score 0.92736334; 80.47 ms first, 52.68–55.20 ms subsequent | Historical v0.1 fixture smoke test |
| Initial Llama | 12.768 s including hash/load, 16.14 tokens/s | Historical smaller advisory prompt |
| Initial memory | Process PSS 1,566,964 KiB with model loaded | One historical observation, not peak RAM requirement |
| Initial offline | Vision/Llama/English-Tamil synthesis passed with airplane mode and no default network | Historical v0.1 automated evidence |
| Current app offline | Project owner reports successful manual offline use | User-reported, 22 November 2026; separate from automated logs |

Exact current commands and sanitized raw outputs: [VERIFICATION.md](docs/VERIFICATION.md).
No benchmark average, field accuracy, per-class recall, power or thermal measurements
are inferred from these small smoke tests.

## Deployment and limits

ARM64 only; minimum Android 9. Tested on Nothing A001, Android 16, approximately 8 GB
RAM class, 4 KB pages. No 16 KB-page or lower-RAM compatibility claim. Model provisioning
uses `adb run-as` and requires a **debuggable APK**. This is the **Phase 1 prototype
installation method, not production distribution**. A production model importer and
release-signing process are Phase 2 objectives. No private signing key is committed.

## Phase 2 objectives

1. **Required severity classification: not implemented in Phase 1.** Establish severity
   labels and a measurable method, then validate against expert-labeled examples.
   Classifier confidence is not severity and will not substitute for this requirement.
2. Broaden crop-specific guidance beyond the current three conditions and review
   multilingual/regional content with native speakers and agronomists.
3. Evaluate independent field images, improve crop-mismatch/unknown handling, and add
   validated pest detection, including the intended sapodilla/mealybug demonstration.
4. Provide production signing and model setup without a debuggable APK or `run-as`.
5. Test more devices and lifecycle cases, memory/thermal/battery behavior and farmer
   usability.

The [Phase 1 demonstration script](DEMO_WALKTHROUGH.md) explicitly presents severity
as an unmet requirement planned for Phase 2. The narrated video recording is still
outstanding.

[Third-party attribution and licensing](THIRD_PARTY_NOTICES.md) distinguish all original
training/export work from this project's application and integration work.
