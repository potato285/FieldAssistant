# Third-party models, data and software

Field Assistant integrates existing pretrained models. The project does **not** claim
their training, original datasets, architectures, quantization research or export work
as its own. Application integration, interface, routing, guidance presentation, local
speech, installation and testing are the work implemented in this repository.

## Vision model

- Author/publisher: **KumarjitC**, [Plantulus Doctor](https://huggingface.co/KumarjitC/plantulus-doctor).
- Artifact: EfficientNetV2-B0 TFLite, float16 weights, float32 input/output; 54 labels,
  19 plants; 11,910,620 bytes.
- Revision: `ee2cb607a33fefa85ccc567988c98fc605fa36bd`.
- SHA-256: `3f9b1ef6e7bcc603c6844429ae4ac28607cbfb48fd7b1f856362c824a29a3e65`.
- License: **MIT as declared by the publisher's model card**. Upstream provides no
  separate LICENSE file. The [preserved model card](app/src/main/assets/licenses/plantulus-model-card.md)
  and [notice](app/src/main/assets/licenses/NOTICE.txt) record this declaration.
- The publisher names PlantVillage, augmented PlantVillage and an indoor-plant dataset
  as training sources. Complete provenance/training rights have not been independently
  audited. No full training dataset is redistributed. Reported upstream accuracy is
  not our independent field accuracy.

Weights are downloaded from the pinned original source by `tools/download_models.py`;
metadata is committed. The packaged APK includes the vision model and its notices.
The exact preprocessing contract is described in [TECHNICAL_REPORT.md](TECHNICAL_REPORT.md).

## Llama model and ExecuTorch export

- Base family/author: **Meta Llama 3.2**, [1B Instruct](https://huggingface.co/meta-llama/Llama-3.2-1B-Instruct).
- Export publisher: **ExecuTorch community**, [Llama-3.2-1B-Instruct-SpinQuant_INT4_EO8-ET](https://huggingface.co/executorch-community/Llama-3.2-1B-Instruct-SpinQuant_INT4_EO8-ET).
- Pinned revision: `911221e0f49d3354cbd4697a9c09feee25cadec7`.
- `.pte`: 1,135,951,488 bytes;
  SHA-256 `8715cdba9e91f6bede00cc5f2d6b12397b95225da5630c4972a8da03001cda3b`.
- Tokenizer: 2,183,982 bytes;
  SHA-256 `82e9d31979e92ab929cd544440f129d9ecd797b69e327f80f17e1c50d5551b55`.
- Runtime: ExecuTorch Android 0.6.0; model/runtime pair tested together.
- License: [Llama 3.2 Community License](app/src/main/assets/licenses/LLAMA-3.2-LICENSE.txt)
  and [Acceptable Use Policy](app/src/main/assets/licenses/LLAMA-3.2-USE-POLICY.md).
  These are not the same as an MIT/Apache model license. Read the full terms before use.

**Built with Llama.** Llama 3.2 is licensed under the Llama 3.2 Community License,
Copyright © Meta Platforms, Inc. All Rights Reserved.

Model weights/tokenizer are not committed to Git. Setup downloads them from the original
pinned export and includes the agreement, policy and notice in the prepared installer.
No fine-tuning or model training was performed for this prototype.

## Test photographs

Tomato and potato early-blight photographs in `app/src/androidTest/assets/` are from
**PlantVillage**, Sharada P. Mohanty, David P. Hughes and Marcel Salathé / EPFL:
[repository](https://github.com/spMohanty/PlantVillage-Dataset),
[dataset author's card](https://huggingface.co/datasets/mohanty/PlantVillage).
The author card declares **CC BY-SA 3.0**. The original images and redistributed
adaptations retain that attribution/license. They may overlap training data and are
runtime smoke-test fixtures, not a held-out field evaluation. They are excluded from
the farmer APK. [Exact photo provenance](docs/verification/potato-photo-source.json)
and the [asset notice](app/src/main/assets/licenses/NOTICE.txt) are included.

Citation: Mohanty, Hughes, Salathé (2016), *Using Deep Learning for Image-Based Plant
Disease Detection*, Frontiers in Plant Science 7, doi:10.3389/fpls.2016.01419.

App screenshots containing those photographs retain the image's CC BY-SA attribution.

## Guidance and translations

Sources: **TNAU Agritech**, **UC IPM**, **PNW Extension** (Oregon State, Washington State,
University of Idaho), and **Manitoba Agriculture**. URLs, source scope and access dates
are stored in [guidance.json](app/src/main/assets/guidance.json). The app contains short
original summaries/prototype translations, not copied source articles, tables or images.
This is not an endorsement by those organizations or a current pesticide-registration
check. Named potato fungicides require a local officer and current product-label
confirmation; no chemical dose, mixing recipe or spray schedule is supplied.

## Libraries and setup tools

| Component | License / upstream |
| --- | --- |
| ExecuTorch | BSD-3-Clause; [license copy](app/src/main/assets/licenses/EXECUTORCH-LICENSE.txt), [upstream](https://github.com/pytorch/executorch) |
| LiteRT | Apache-2.0; [license copy](app/src/main/assets/licenses/LITERT-LICENSE.txt), [upstream](https://github.com/google-ai-edge/LiteRT) |
| AndroidX / Jetpack Compose | Apache-2.0; [AndroidX](https://android.googlesource.com/platform/frameworks/support/) |
| Kotlin | Apache-2.0; [JetBrains](https://github.com/JetBrains/kotlin) |
| Gradle wrapper | Apache-2.0; generated wrapper retains upstream notices; [Gradle](https://github.com/gradle/gradle) |
| Android Platform-Tools / SDK | Google's applicable SDK/tool terms; downloaded from [official Google source](https://developer.android.com/tools/releases/platform-tools), not committed |
| Phone speech engine / voice packs | Respective provider's terms; not redistributed in this repo or APK |

Dependency artifacts may have additional notices; consult their upstream distributions.
A separate license for Field Assistant's original application source has not yet been
selected. This notice records third-party terms; it does not relicense those materials.
