package org.fieldassistant.ui

import org.fieldassistant.core.FindingStatus

object Words {
    fun pick(ta: Boolean, en: String, tamil: String) = if (ta) tamil else en
    private val crops = mapOf(
        "Aloe" to "கற்றாழை", "Apple" to "ஆப்பிள்", "Bell Pepper" to "குடைமிளகாய்",
        "Blueberry" to "புளூபெர்ரி", "Cactus" to "கள்ளிச்செடி", "Cherry" to "செர்ரி",
        "Corn (Maize)" to "மக்காச்சோளம்", "Grape" to "திராட்சை", "Money Plant" to "மணி பிளாண்ட்",
        "Orange" to "ஆரஞ்சு", "Peach" to "பீச்", "Potato" to "உருளைக்கிழங்கு",
        "Raspberry" to "ராஸ்பெர்ரி", "Snake Plant" to "ஸ்நேக் பிளாண்ட்", "Soybean" to "சோயா",
        "Spider Plant" to "ஸ்பைடர் பிளாண்ட்", "Squash" to "ஸ்குவாஷ்", "Strawberry" to "ஸ்ட்ராபெர்ரி",
        "Tomato" to "தக்காளி", "Sapodilla" to "சப்போட்டா", "Other" to "மற்ற பயிர்"
    )
    private val conditions = mapOf(
        "Anthracnose" to "ஆந்த்ராக்னோஸ்", "Bacterial Spot" to "பாக்டீரியா புள்ளி நோய்",
        "Bacterial Wilt Disease" to "பாக்டீரியா வாடல் நோய்", "Black Rot" to "கருப்பு அழுகல்",
        "Cedar Apple Rust" to "சீடார் ஆப்பிள் துரு நோய்", "Cercospora Leaf Spot (Gray Leaf Spot)" to "செர்கோஸ்போரா இலைப்புள்ளி",
        "Common Rust" to "பொதுவான துரு நோய்", "Dactylopius Opuntia (Cochineal)" to "கள்ளிச்செடி கொச்சினியல் பூச்சி",
        "Early Blight" to "முன்பருவ இலைக்கருகல்", "Esca (Black Measles)" to "எஸ்கா நோய்",
        "Fungal Leaf Spot" to "பூஞ்சை இலைப்புள்ளி", "Healthy" to "ஆரோக்கியமான இலை போன்ற தோற்றம்",
        "Huanglongbing (Citrus Greening)" to "சிட்ரஸ் பசுமை நோய்", "Late Blight" to "பின்பருவ இலைக்கருகல்",
        "Leaf Blight (Isariopsis Leaf Spot)" to "இலைக்கருகல் (இசாரியாப்சிஸ்)", "Leaf Mold" to "இலைப் பூஞ்சை",
        "Leaf Scorch" to "இலைத் தீய்வு", "Leaf Spot" to "இலைப்புள்ளி", "Leaf Tip Necrosis" to "இலை நுனி கருகல்",
        "Leaf Withering" to "இலை வாடல்", "Manganese Toxicity" to "மாங்கனீசு நச்சுத்தன்மை",
        "Mosaic Virus" to "தேமல் வைரஸ்", "Northern Leaf Blight" to "வடக்கு இலைக்கருகல்",
        "Powdery Mildew" to "சாம்பல் நோய்", "Rust" to "துரு நோய்", "Scab" to "சொறி நோய்",
        "Septoria Leaf Spot" to "செப்டோரியா இலைப்புள்ளி", "Spider Mites (Two-Spotted Spider Mite)" to "இருபுள்ளிச் சிலந்திப் பேன்",
        "Sunburn" to "வெயில் பாதிப்பு", "Target Spot" to "வட்டப்புள்ளி நோய்", "Yellow Leaf Curl Virus" to "மஞ்சள் இலைச்சுருள் வைரஸ்"
    )
    fun pick(language: String, en: String, tamil: String) = when (language) {
        "ta" -> tamil
        "te" -> Telugu.text[en] ?: en
        else -> en
    }
    fun crop(value: String, language: String) = when (language) { "ta" -> crops[value] ?: value; "te" -> Telugu.crops[value] ?: value; else -> value }
    fun condition(value: String, language: String) = when (language) { "ta" -> conditions[value] ?: value; "te" -> Telugu.conditions[value] ?: value; else -> condition(value, false) }
    fun status(value: FindingStatus, language: String): String {
        val en = status(value, false)
        return pick(language, en, status(value, true))
    }
    fun crop(value: String, ta: Boolean) = if (ta) crops[value] ?: value else value
    fun condition(value: String, ta: Boolean) = if (ta) conditions[value] ?: value else if (value == "Healthy") "Healthy-like appearance" else value
    fun status(value: FindingStatus, ta: Boolean) = when (value) {
        FindingStatus.POSSIBLE_MATCH -> pick(ta, "Possible match • confirm with an expert", "சாத்தியமான பொருத்தம் • நிபுணரிடம் உறுதி செய்யவும்")
        FindingStatus.UNCERTAIN -> pick(ta, "Uncertain • take another photo", "உறுதியாகத் தெரியவில்லை • மீண்டும் படம் எடுக்கவும்")
        FindingStatus.CROP_CONFLICT -> pick(ta, "We could not match this photo to your crop", "இந்தப் படத்தை உங்கள் பயிருடன் பொருத்த முடியவில்லை")
        FindingStatus.UNSUPPORTED -> pick(ta, "This crop is not supported yet", "இந்தப் பயிரை இப்போது கண்டறிய முடியாது")
        FindingStatus.NEEDS_CROP -> pick(ta, "Confirm the crop to interpret this result", "முடிவைப் புரிந்துகொள்ள பயிரைத் தேர்வு செய்யவும்")
    }
}
