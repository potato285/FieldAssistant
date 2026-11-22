package org.fieldassistant

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import org.fieldassistant.core.FindingStatus
import org.fieldassistant.ui.*
import java.io.File
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT))
        setContent { FieldApp() }
    }
}

private val Forest = Color(0xFF245B40)
private val Paper = Color(0xFFF5F6EF)
private val Ink = Color(0xFF203C2E)
private val Amber = Color(0xFFFFEAC2)

@Composable
fun FieldApp(vm: FieldViewModel = viewModel()) {
    val context = LocalContext.current
    val language = vm.language
    fun w(en: String, tamil: String) = Words.pick(language, en, tamil)
    var cropDialog by remember { mutableStateOf(false) }
    var details by rememberSaveable { mutableStateOf(false) }
    var debugDetails by rememberSaveable { mutableStateOf(false) }
    var pendingCamera by rememberSaveable { mutableStateOf<String?>(null) }
    var speechVersion by remember { mutableIntStateOf(0) }
    val speech = remember { OfflineSpeech(context) { speechVersion++ } }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) speech.pause()
            if (event == Lifecycle.Event.ON_RESUME) speechVersion++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer); speech.close() }
    }
    LaunchedEffect(language, vm.advisory, vm.busy, vm.crop) { speech.stop() }
    val canSpeak = remember(language, speechVersion) { speech.available(language) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) pendingCamera?.let { vm.loadPhoto(Uri.parse(it)) }
        pendingCamera = null
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.loadPhoto(uri)
    }
    val colors = lightColorScheme(primary = Forest, background = Paper, surface = Color.White,
        onBackground = Ink, onSurface = Ink, secondaryContainer = Color(0xFFE1ECDD))
    MaterialTheme(colorScheme = colors) {
        Surface(Modifier.fillMaxSize(), color = Paper) {
            Column(Modifier.safeDrawingPadding().fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(w("Field Assistant", "வயல் உதவியாளர்"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(w("A little clarity for your crops", "உங்கள் பயிருக்கான வழிகாட்டி"), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("en" to "English", "ta" to "தமிழ்", "te" to "తెలుగు").forEach { (code, label) ->
                        FilterChip(selected = language == code, onClick = { vm.selectLanguage(code) },
                            label = { Text(label) }, modifier = Modifier.testTag("language-$code"))
                    }
                }
                Surface(color = Color(0xFFE1ECDD), shape = RoundedCornerShape(16.dp)) {
                    Text(w("On your phone · Works without internet", "உங்கள் கைப்பேசியில் · இணையம் தேவையில்லை"),
                        Modifier.padding(horizontal = 16.dp, vertical = 12.dp), style = MaterialTheme.typography.labelLarge, color = Forest)
                }
                if (vm.bitmap == null) {
                    Text(w("Take a closer look\nat your crop.", "உங்கள் பயிரை\nஉற்றுப் பாருங்கள்."),
                        style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
                    Text(w("Photograph one leaf in good daylight. We’ll help you understand what to check next.",
                        "நல்ல வெளிச்சத்தில் ஒரு இலையைப் படம் எடுங்கள். அடுத்து என்ன பார்க்க வேண்டும் என்பதை அறிய உதவுவோம்."))
                }
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(w("1  Choose your crop", "1  பயிரைத் தேர்ந்தெடுக்கவும்"), fontWeight = FontWeight.Bold)
                        OutlinedButton(onClick = { cropDialog = true }, enabled = !vm.busy,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("crop-selector")) {
                            Text(vm.crop?.let { Words.crop(it, language) } ?: w("Not sure / choose a crop", "தெரியவில்லை / பயிரைத் தேர்ந்தெடுக்கவும்"))
                        }
                        Text(w("Not sure? You can choose the crop after adding a photo.", "தெரியவில்லையா? படம் சேர்த்த பிறகும் பயிரைத் தேர்வு செய்யலாம்."),
                            style = MaterialTheme.typography.bodySmall)
                        Text(w("2  Add a leaf photo", "2  இலைப் படத்தைச் சேர்க்கவும்"), fontWeight = FontWeight.Bold)
                        Button(onClick = {
                            try {
                                speech.stop()
                                val directory = File(context.cacheDir, "captures").apply { mkdirs() }
                                directory.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 86_400_000 }?.forEach { it.delete() }
                                val file = File.createTempFile("leaf-", ".jpg", directory)
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                                pendingCamera = uri.toString()
                                camera.launch(uri)
                            } catch (failure: Exception) { vm.showError(failure.message ?: "Camera unavailable") }
                        }, enabled = !vm.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("camera")) {
                            Text(w("Take a photo", "படம் எடுக்கவும்"), style = MaterialTheme.typography.titleMedium)
                        }
                        OutlinedButton(onClick = {
                            speech.stop()
                            try { gallery.launch("image/*") }
                            catch (failure: Exception) { vm.showError(failure.message ?: "Gallery unavailable") }
                        }, enabled = !vm.busy,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("gallery")) {
                            Text(w("Choose from gallery", "படத்தொகுப்பிலிருந்து தேர்வு செய்யவும்"))
                        }

                    }
                }
                vm.bitmap?.let { photo ->
                    Card(shape = RoundedCornerShape(24.dp)) {
                        Image(photo.asImageBitmap(), contentDescription = w("Selected leaf photograph", "தேர்ந்தெடுத்த இலைப் படம்"),
                            contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp)
                                .aspectRatio(1.3f).background(Color(0xFFE7EADD)))

                    }
                }
                if (vm.busy) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(if (vm.askingLlama) w("Preparing your next steps…", "அடுத்து செய்ய வேண்டியவை தயாராகின்றன…")
                        else w("Looking at the leaf on this phone…", "இந்தக் கைப்பேசியில் இலைப் படம் ஆய்வு செய்யப்படுகிறது…"))
                    if (vm.askingLlama) TextButton(onClick = { vm.stopLlama() }) { Text(w("Stop", "நிறுத்து")) }
                }
                vm.error?.let {
                    Surface(color = Amber, shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(if (vm.advisoryFailed) w("Your next steps aren’t ready yet", "அடுத்த படிகள் இன்னும் தயாராகவில்லை")
                                else w("We couldn’t complete that step", "இந்தச் செயலை முடிக்க முடியவில்லை"), fontWeight = FontWeight.Bold)
                            Text(if (vm.advisoryFailed) w("Your photo result is still shown. You can try preparing the advice again.",
                                "படத்தின் முடிவு கீழே உள்ளது. வழிகாட்டுதலை மீண்டும் தயாரிக்க முயற்சி செய்யலாம்.")
                                else w("Please try again, or choose another clear leaf photo.", "மீண்டும் முயற்சி செய்யுங்கள் அல்லது வேறு தெளிவான இலைப் படத்தைத் தேர்வு செய்யுங்கள்."))
                        }
                    }
                }
                vm.finding?.let { finding ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.testTag("result")) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(w("What the photo suggests", "படம் காட்டும் சாத்தியம்"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Surface(color = Amber, shape = RoundedCornerShape(12.dp)) {
                                Text(Words.status(finding.status, language), Modifier.padding(12.dp), fontWeight = FontWeight.Medium)
                            }
                            if (finding.status == FindingStatus.POSSIBLE_MATCH || finding.status == FindingStatus.NEEDS_CROP) {
                                finding.candidate?.let { top ->
                                    Text(Words.condition(top.label.condition, language), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                                    Text(Words.crop(top.label.crop, language))
                                }
                            }
                            Text(w("Some crop problems look alike. Ask an agricultural officer to confirm the cause before treatment.",
                                "சில பயிர் பிரச்சினைகள் ஒரே மாதிரி தோன்றும். சிகிச்சைக்கு முன் காரணத்தை வேளாண் அலுவலரிடம் உறுதி செய்யுங்கள்."))
                            if (finding.status == FindingStatus.UNSUPPORTED) Text(w(
                                "We cannot identify problems on this crop yet. Ask an agricultural officer to examine the plant.",
                                "இந்தப் பயிரின் பிரச்சினையை இப்போது கண்டறிய முடியாது. செடியை ஆய்வு செய்ய வேளாண் அலுவலரை அணுகவும்."))
                            if (finding.status == FindingStatus.CROP_CONFLICT) Text(w(
                                "The photo checker may be wrong. If your crop selection is correct, keep it and try a close, clear photo of one leaf in daylight.",
                                "பட ஆய்வு தவறாக இருக்கலாம். பயிரைச் சரியாகத் தேர்ந்தெடுத்திருந்தால் அதை மாற்றாமல், பகல் வெளிச்சத்தில் ஒரு இலையை அருகில் தெளிவாகப் படம் எடுங்கள்."))
                            if (finding.status == FindingStatus.CROP_CONFLICT || finding.status == FindingStatus.NEEDS_CROP) {
                                OutlinedButton(onClick = { cropDialog = true }, enabled = !vm.busy) {
                                    Text(w("Correct the crop", "பயிரைச் சரிசெய்யவும்"))
                                }
                            }
                            if (vm.result == null && vm.crop in vm.crops) {
                                OutlinedButton(onClick = { vm.reanalyze() }, enabled = !vm.busy) { Text(w("Analyze this photo", "இந்தப் படத்தை ஆய்வு செய்யவும்")) }
                            }
                        }
                    }
                }
                vm.advisory?.let { advice ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE8EFE2)), shape = RoundedCornerShape(24.dp)) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(w("What you can do next", "அடுத்து என்ன செய்யலாம்"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(if (advice.fallback) w("Saved tips · AI advice unavailable", "சேமித்த குறிப்புகள் · AI வழிகாட்டுதல் கிடைக்கவில்லை")
                                else w("Your next steps are ready", "அடுத்து செய்ய வேண்டியவை தயார்"),
                                modifier = Modifier.testTag("advice-status"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Text(w("Region: Tamil Nadu, India", "பகுதி: தமிழ்நாடு, இந்தியா"), style = MaterialTheme.typography.bodySmall)
                            if (language == "en" && advice.explanation != null) {
                                Text(w("Why these steps may help", "இந்தப் படிகள் ஏன் உதவலாம்"), fontWeight = FontWeight.Bold)
                                Text(advice.explanation, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag("llama-explanation"))
                                Text(w("AI-written explanation · the finding still needs confirmation.", "AI எழுதிய விளக்கம் · முடிவை இன்னும் உறுதி செய்ய வேண்டும்."), style = MaterialTheme.typography.bodySmall)
                            } else if (language != "en" && advice.explanation != null) {
                                Text(w("These translated steps are saved guidance. The AI explanation is available in English.", "இந்த மொழிபெயர்க்கப்பட்ட படிகள் சேமித்த வழிகாட்டுதல். AI விளக்கம் ஆங்கிலத்தில் உள்ளது."), style = MaterialTheme.typography.bodySmall)
                            } else if (advice.explanationIssue != null) {
                                Text(w("The explanation could not be prepared. Use the sourced steps below.", "விளக்கத்தைத் தயாரிக்க முடியவில்லை. கீழே உள்ள ஆதார வழிகாட்டுதலைப் பயன்படுத்துங்கள்."), style = MaterialTheme.typography.bodySmall)
                            }
                            val available = vm.finding?.let { vm.repository.allowedActions(it, "Tamil Nadu, India") }.orEmpty()
                            val mainActions = (advice.actions + available).distinctBy { it.id }.filter { it.section != "reminder" }
                            for ((section, heading, tamilHeading) in listOf(
                                Triple("care", "Care now", "இப்போது பராமரிக்க"),
                                Triple("treatment", "Treatment options", "சிகிச்சை வாய்ப்புகள்"),
                                Triple("prevention", "Prevent the next outbreak", "அடுத்த பாதிப்பைத் தடுக்க")
                            )) {
                                val actions = mainActions.filter { it.section == section }
                                if (actions.isNotEmpty()) {
                                    Text(w(heading, tamilHeading), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    actions.forEach { action ->
                                        Text("• " + action.text(language))
                                        Text(vm.repository.sources.firstOrNull { it.id == action.sourceId }?.title.orEmpty(), style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                            if (mainActions.isEmpty()) Text(w(
                                "We do not have specific treatment advice for this finding. Ask an agricultural officer to examine the plant.",
                                "இந்த முடிவுக்கான குறிப்பிட்ட சிகிச்சை வழிகாட்டுதல் இல்லை. செடியை ஆய்வு செய்ய வேளாண் அலுவலரை அணுகுங்கள்."))
                            HorizontalDivider()
                            Text(w("Keep in mind", "நினைவில் கொள்ளுங்கள்"), fontWeight = FontWeight.Bold)
                            vm.repository.reminders().forEach { Text("• " + it.text(language), style = MaterialTheme.typography.bodySmall) }
                            if (advice.fallback) {
                                Button(onClick = { vm.askLlama() }, enabled = !vm.busy && vm.llamaReady,
                                    modifier = Modifier.fillMaxWidth().testTag("retry-advice")) {
                                    Text(w("Try preparing advice again", "வழிகாட்டுதலை மீண்டும் தயாரிக்கவும்"))
                                }
                                if (!vm.llamaReady) Text(w("Advice is not set up on this phone yet. The saved tips above are still available.",
                                    "இந்தக் கைப்பேசியில் வழிகாட்டுதல் இன்னும் அமைக்கப்படவில்லை. மேலே உள்ள சேமித்த குறிப்புகளைப் பயன்படுத்தலாம்."))
                            }
                            val spoken = listOf(
                                w("Ask an agricultural officer to confirm the cause.", "காரணத்தை உறுதி செய்ய வேளாண் அலுவலரை அணுகுங்கள்."),
                                if (language == "en") advice.explanation.orEmpty() else "",
                                (mainActions + vm.repository.reminders()).joinToString("\n\n") { it.text(language) }
                            ).filter { it.isNotBlank() }.joinToString("\n\n")
                            val speechState = remember(speechVersion) { speech.state }
                            val activeSpeech = speechState in listOf(OfflineSpeech.State.PLAYING, OfflineSpeech.State.PREPARING, OfflineSpeech.State.PAUSED)
                            OutlinedButton(onClick = {
                                when (speechState) {
                                    OfflineSpeech.State.PAUSED -> speech.resume()
                                    OfflineSpeech.State.PLAYING, OfflineSpeech.State.PREPARING -> speech.pause()
                                    else -> if (!speech.speak(spoken, language)) vm.showError(w("Offline voice unavailable", "இணையமில்லா குரல் கிடைக்கவில்லை"))
                                }
                            }, enabled = canSpeak && !vm.busy, modifier = Modifier.fillMaxWidth().testTag("speech-main")) {
                                Text(when (speechState) {
                                    OfflineSpeech.State.PAUSED -> w("Resume reading", "தொடர்ந்து வாசிக்கவும்")
                                    OfflineSpeech.State.PLAYING, OfflineSpeech.State.PREPARING -> w("Pause reading", "வாசிப்பை இடைநிறுத்தவும்")
                                    else -> w("Listen to these steps", "இந்தப் படிகளைக் கேளுங்கள்")
                                })
                            }
                            if (activeSpeech) {
                                Text(if (speechState == OfflineSpeech.State.PREPARING) w("Preparing voice…", "குரலைத் தயாரிக்கிறது…")
                                    else if (speechState == OfflineSpeech.State.PAUSED) w("Reading paused", "வாசிப்பு இடைநிறுத்தப்பட்டது")
                                    else w("Reading aloud", "உரக்க வாசிக்கிறது"), modifier = Modifier.testTag("speech-status"), style = MaterialTheme.typography.bodySmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    TextButton(onClick = { speech.restart() }, modifier = Modifier.testTag("speech-restart")) { Text(w("Read from the start", "முதலிலிருந்து வாசிக்கவும்")) }
                                    TextButton(onClick = { speech.stop() }, modifier = Modifier.testTag("speech-stop")) { Text(w("Stop", "நிறுத்து")) }
                                }
                            }
                            if (speechState == OfflineSpeech.State.ERROR) Text(w("Reading failed. Try again or check the offline voice.", "வாசிக்க முடியவில்லை. மீண்டும் முயற்சிக்கவும் அல்லது இணையமில்லா குரலைச் சரிபார்க்கவும்."))
                            if (!canSpeak) Text(w("No installed offline voice for this language. Download it in Android Text-to-speech settings, then reopen this app.", "இந்த மொழிக்கு இணையமில்லா குரல் இல்லை. Android உரை-குரல் அமைப்பில் பதிவிறக்கி, செயலியை மீண்டும் திறக்கவும்."), style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = {
                                runCatching { context.startActivity(speech.voiceSetupIntent()) }
                                    .onFailure { vm.showError(w("Open Android Text-to-speech settings to install an offline voice.", "இணையமில்லா குரலை நிறுவ Android உரை-குரல் அமைப்பைத் திறக்கவும்.")) }
                            }, modifier = Modifier.testTag("speech-setup")) { Text(w("Set up offline voices", "இணையமில்லா குரல்களை அமைக்கவும்")) }
                        }
                    }
                }
                TextButton(onClick = { details = !details }, modifier = Modifier.fillMaxWidth().testTag("details")) {
                    Text(w("About this app & sources", "செயலி மற்றும் ஆதாரங்கள்") + if (details) " −" else " +")
                }
                if (details) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(w("Prototype • 54 leaf classes across 19 plants", "முன்மாதிரி • 19 தாவரங்களில் 54 இலை வகைகள்"), fontWeight = FontWeight.Bold)
                            Text("Built with Llama", style = MaterialTheme.typography.bodySmall)
                            Text(w("The model covers specific leaf conditions only. It does not identify every crop, pest, fruit or stem problem. Sapodilla/mealybug and severity are not supported.",
                                "குறிப்பிட்ட இலை அறிகுறிகள் மட்டுமே உள்ளன. எல்லாப் பயிர், பூச்சி, பழம், தண்டு பிரச்சினைகளையும் கண்டறியாது. சப்போட்டா/மாவுப்பூச்சி மற்றும் தீவிர அளவீடு ஆதரிக்கப்படவில்லை."))
                            Text(w("Supported labels", "ஆதரிக்கப்படும் வகைகள்"), fontWeight = FontWeight.Bold)
                            vm.labels.groupBy { it.crop }.forEach { (crop, labels) ->
                                Text(Words.crop(crop, language) + ": " + labels.joinToString(", ") { Words.condition(it.condition, language) }, style = MaterialTheme.typography.bodySmall)
                            }
                            Text("Sources / ஆதாரங்கள்", fontWeight = FontWeight.Bold)
                            vm.repository.sources.forEach { Text("${it.title}\n${it.url}", style = MaterialTheme.typography.bodySmall) }
                            Text("Model: KumarjitC/plantulus-doctor (MIT declared)\nLlama 3.2 Community License · Built with Llama when executed", style = MaterialTheme.typography.bodySmall)
                            Text(w("This is a trial version. Crop advice and translations still need expert review. Field accuracy has not been established.",
                                "இது சோதனைப் பதிப்பு. பயிர் வழிகாட்டுதலும் மொழிபெயர்ப்புகளும் நிபுணர் மதிப்பாய்வு பெற வேண்டும். வயல் சூழலில் துல்லியம் இன்னும் உறுதி செய்யப்படவில்லை."), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                if (BuildConfig.DEBUG) {
                    TextButton(onClick = { debugDetails = !debugDetails }, modifier = Modifier.fillMaxWidth().testTag("debug-toggle")) {
                        Text(if (debugDetails) "Hide developer details" else "Developer details (debug build)")
                    }
                    if (debugDetails) Card(modifier = Modifier.testTag("debug-panel")) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("DEBUG ONLY · hidden from release builds", fontWeight = FontWeight.Bold)
                            vm.result?.let { result ->
                                Text("${result.modelId}\nSHA256 ${result.modelSha256}\n${String.format(Locale.US, "%.1f ms", result.inferenceMs)} · CPU")
                                result.candidates.take(3).forEach { Text("${it.label.id}: ${String.format(Locale.US, "%.4f", it.score)}") }
                            }
                            Text("Finding: ${vm.finding?.status}; selected crop: ${vm.crop ?: "unconfirmed"}")
                            Text(vm.advisoryDebug.ifBlank { "No completed Llama request" })
                            vm.error?.let { Text("Diagnostic: $it") }
                            Text("Scores are uncalibrated, not severity or diagnostic certainty. Unknown images can score highly.")
                            TextButton(onClick = { vm.askLlama() }, enabled = !vm.busy && vm.finding != null,
                                modifier = Modifier.testTag("ask-llama")) { Text("Run Llama again") }
                            TextButton(onClick = { vm.refreshModelFiles() }) { Text("Check installed model files") }
                        }
                    }
                }
                Text(w("A second opinion for your crop. An expert makes the diagnosis.", "பயிருக்கான கூடுதல் வழிகாட்டி. நிபுணரே நோயை உறுதி செய்ய வேண்டும்."),
                    style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 16.dp))
            }
        }
        if (cropDialog) {
            AlertDialog(onDismissRequest = { cropDialog = false }, title = { Text(w("Which crop is this?", "இது என்ன பயிர்?")) },
                text = {
                    Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                        TextButton(onClick = { vm.selectCrop(null); cropDialog = false }, modifier = Modifier.fillMaxWidth()) {
                            Text(w("Not sure", "தெரியவில்லை"))
                        }
                        (vm.crops + listOf("Sapodilla", "Other")).forEach { value ->
                            TextButton(onClick = { vm.selectCrop(value); cropDialog = false }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                                Text(Words.crop(value, language) + if (value !in vm.crops) w(" • unsupported", " • ஆதரவு இல்லை") else "")
                            }
                        }
                    }
                }, confirmButton = { TextButton(onClick = { cropDialog = false }) { Text(w("Close", "மூடு")) } })
        }
    }
}
