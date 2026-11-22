# Model label coverage

The pinned Plantulus Doctor model has 54 output classes across 19 crops/plants.
This is the model's declared label set, not measured field accuracy. Healthy-only
plants have no disease class coverage. Crop selection is supplied/corrected by the
user. Cactus cochineal and tomato spider-mite labels do not establish detection of
sapodilla mealybug or a general pest detector.

| Crop / plant | Conditions |
|---|---|
| Aloe | Anthracnose, Healthy, Leaf Spot, Rust, Sunburn |
| Apple | Scab, Black Rot, Cedar Apple Rust, Healthy |
| Bell Pepper | Bacterial Spot, Healthy |
| Blueberry | Healthy |
| Cactus | Dactylopius Opuntia (Cochineal), Healthy |
| Cherry | Healthy, Powdery Mildew |
| Corn (Maize) | Cercospora Leaf Spot (Gray Leaf Spot), Common Rust, Healthy, Northern Leaf Blight |
| Grape | Black Rot, Esca (Black Measles), Healthy, Leaf Blight (Isariopsis Leaf Spot) |
| Money Plant | Bacterial Wilt Disease, Healthy, Manganese Toxicity |
| Orange | Huanglongbing (Citrus Greening) |
| Peach | Bacterial Spot, Healthy |
| Potato | Early Blight, Healthy, Late Blight |
| Raspberry | Healthy |
| Snake Plant | Anthracnose, Healthy, Leaf Withering |
| Soybean | Healthy |
| Spider Plant | Fungal Leaf Spot, Healthy, Leaf Tip Necrosis |
| Squash | Powdery Mildew |
| Strawberry | Healthy, Leaf Scorch |
| Tomato | Bacterial Spot, Early Blight, Healthy, Late Blight, Leaf Mold, Septoria Leaf Spot, Spider Mites (Two-Spotted Spider Mite), Target Spot, Mosaic Virus, Yellow Leaf Curl Virus |

Machine-readable ordered labels: [classes.json](../app/src/main/assets/vision/classes.json).
Detailed label metadata: [class_info.json](../app/src/main/assets/vision/class_info.json).

## Guidance coverage is narrower than classification coverage

- Tomato early blight: sourced practical care/prevention.
- Potato early and late blight: expanded care, prevention, treatment discussion.
- Other supported findings: general observation and agricultural-officer referral.
- Unsupported, conflicting or uncertain findings: no specific treatment claim.

Guidance currently uses Tamil Nadu, India as a fixed region. Selecting Telugu does
not switch treatment references to Andhra Pradesh or Telangana. New crops, diseases,
regions and language content require separately reviewed sources and tests.
