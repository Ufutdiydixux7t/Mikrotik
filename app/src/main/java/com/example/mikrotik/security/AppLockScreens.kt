package com.example.mikrotik.security

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mikrotik.presentation.ui.components.AppBackground
import com.example.mikrotik.presentation.ui.localization.appText
import com.example.mikrotik.presentation.ui.theme.AppIcons
import com.example.mikrotik.ui.theme.AppPalette
import kotlinx.coroutines.delay

private data class PinUserDraft(
    val name: String,
    val pin: String = "",
    val confirmation: String = ""
)

@Composable
fun AppLockGateScreen(viewModel: AppLockViewModel) {
    val config by viewModel.config.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    val retryAfter by viewModel.retryAfterMillis.collectAsState()
    val verifying by viewModel.isVerifying.collectAsState()
    var selectedUser by remember { mutableIntStateOf(0) }
    var pin by remember { mutableStateOf("") }
    var remainingSeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(retryAfter) {
        remainingSeconds = ((retryAfter + 999L) / 1000L).toInt()
        while (remainingSeconds > 0) {
            delay(1_000L)
            remainingSeconds--
        }
    }
    LaunchedEffect(config.userNames.size) {
        if (selectedUser !in config.userNames.indices) selectedUser = 0
    }

    AppBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = 22.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = AppPalette.Card),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier.size(68.dp).clip(CircleShape).background(AppPalette.Mint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(AppIcons.Shield, null, tint = AppPalette.Teal, modifier = Modifier.size(34.dp))
                    }
                    Text(
                        appText("التطبيق مقفل", "App locked"),
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppPalette.TextNavy
                    )
                    Text(
                        appText("اختر المستخدم وأدخل رمز PIN للمتابعة.", "Choose a user and enter their PIN to continue."),
                        fontSize = 14.sp,
                        color = AppPalette.Muted,
                        textAlign = TextAlign.Center
                    )

                    if (config.userNames.size > 1) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                appText("المستخدم", "User"),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppPalette.TextNavy
                            )
                            config.userNames.forEachIndexed { index, name ->
                                UserChoiceRow(
                                    name = name,
                                    selected = selectedUser == index,
                                    onClick = { selectedUser = index; pin = "" }
                                )
                            }
                        }
                    } else if (config.userNames.isNotEmpty()) {
                        Text(
                            config.userNames.first(),
                            color = AppPalette.Teal,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    }

                    PinField(
                        value = pin,
                        onValueChange = { pin = normalizePinDigits(it).take(12); viewModel.clearError() },
                        label = appText("رمز PIN", "PIN"),
                        enabled = !verifying && remainingSeconds == 0
                    )
                    if (error != null) {
                        Text(
                            if (remainingSeconds > 0) appText("انتظر $remainingSeconds ثانية قبل المحاولة مجدداً.", "Wait $remainingSeconds seconds before trying again.") else localizeLockError(error.orEmpty()),
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    Button(
                        onClick = {
                            val submittedPin = pin
                            pin = ""
                            viewModel.unlock(selectedUser, submittedPin)
                        },
                        enabled = pin.length in 6..12 && !verifying && remainingSeconds == 0,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppPalette.Teal)
                    ) {
                        if (verifying) CircularProgressIndicator(modifier = Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        else Text(appText("فتح التطبيق", "Unlock app"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Text(
                        appText("رموز PIN غير المحفوظة لا يمكن استعادتها؛ يلزم إعادة ضبط التطبيق إذا نُسيت جميع الرموز.", "PINs are not recoverable. If all are forgotten, the app must be reset."),
                        color = AppPalette.Muted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun AppLockSettingsScreen(
    viewModel: AppLockViewModel,
    onBack: () -> Unit
) {
    val config by viewModel.config.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    val verifying by viewModel.isVerifying.collectAsState()
    val retryAfter by viewModel.retryAfterMillis.collectAsState()
    var remainingSeconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(retryAfter) {
        remainingSeconds = ((retryAfter + 999L) / 1000L).toInt()
        while (remainingSeconds > 0) {
            delay(1_000L)
            remainingSeconds--
        }
    }
    var authorized by remember { mutableStateOf(!config.enabled) }
    var selectedUser by remember { mutableIntStateOf(0) }
    var currentPin by remember { mutableStateOf("") }
    var userCount by remember(config.userNames) { mutableIntStateOf(if (config.userNames.size == 3) 3 else 1) }
    var policy by remember(config.policy) { mutableStateOf(config.policy) }
    var formError by remember { mutableStateOf<String?>(null) }
    var showDisableConfirmation by remember { mutableStateOf(false) }
    val nameRequiredMessage = appText("أدخل اسماً لكل مستخدم.", "Enter a name for every user.")
    val duplicateNamesMessage = appText("يجب أن تكون أسماء المستخدمين مختلفة.", "User names must be unique.")
    val pinLengthMessage = appText("يجب أن يتكون كل PIN من 6 إلى 12 رقماً.", "Each PIN must contain 6–12 digits.")
    val pinMismatchMessage = appText("تأكيد PIN لا يطابق الرمز.", "PIN confirmation does not match.")
    val duplicatePinsMessage = appText("استخدم PIN مختلفاً لكل مستخدم.", "Each user must have a different PIN.")
    val drafts = remember(config.userNames) {
        mutableStateListOf<PinUserDraft>().apply {
            val count = if (config.userNames.size == 3) 3 else 1
            repeat(count) { index -> add(PinUserDraft(config.userNames.getOrNull(index).orEmpty())) }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        BackToAppLockSettings(onBack)

        if (config.enabled && !authorized) {
            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    LockSectionTitle(appText("تأكيد الهوية", "Verify your identity"))
                    Text(
                        appText("أدخل PIN الحالي لتعديل المستخدمين أو إيقاف القفل.", "Enter the current PIN before changing users or disabling the lock."),
                        color = AppPalette.Muted,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                    UserPicker(config.userNames, selectedUser) { selectedUser = it; currentPin = ""; viewModel.clearError() }
                    PinField(
                        value = currentPin,
                        onValueChange = { currentPin = normalizePinDigits(it).take(12); viewModel.clearError() },
                        label = appText("رمز PIN الحالي", "Current PIN"),
                        enabled = !verifying && remainingSeconds == 0
                    )
                    error?.let { Text(localizeLockError(it), color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
                    Button(
                        onClick = {
                            val submitted = currentPin
                            currentPin = ""
                            viewModel.authorizeSettings(selectedUser, submitted) { if (it) authorized = true }
                        },
                        enabled = currentPin.length in 6..12 && !verifying && remainingSeconds == 0,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppPalette.Teal)
                    ) {
                        Text(appText("متابعة الإعدادات", "Continue to settings"), fontWeight = FontWeight.Bold)
                    }
                    if (remainingSeconds > 0) Text(
                        appText("انتظر $remainingSeconds ثانية قبل المحاولة مجدداً.", "Wait $remainingSeconds seconds before trying again."),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(44.dp).clip(CircleShape).background(AppPalette.Mint), contentAlignment = Alignment.Center) {
                            Icon(AppIcons.Shield, null, tint = AppPalette.Teal, modifier = Modifier.size(24.dp))
                        }
                        Column {
                            LockSectionTitle(appText("قفل التطبيق", "App lock"))
                            Text(
                                appText("PIN يُطلب قبل عرض لوحة التحكم والإعدادات.", "A PIN protects the dashboard and settings."),
                                color = AppPalette.Muted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LockSectionTitle(appText("عدد المستخدمين", "Number of users"))
                    Text(
                        appText("اختر مستخدماً واحداً أو ثلاثة. لكل مستخدم اسم ورمز PIN خاص به.", "Choose one or three users. Each user has a separate name and PIN."),
                        fontSize = 13.sp,
                        color = AppPalette.Muted,
                        lineHeight = 19.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        UserCountChoice(1, userCount == 1, Modifier.weight(1f)) { userCount = 1; resizeDrafts(drafts, 1); formError = null }
                        UserCountChoice(3, userCount == 3, Modifier.weight(1f)) { userCount = 3; resizeDrafts(drafts, 3); formError = null }
                    }
                }
            }

            drafts.forEachIndexed { index, draft ->
                SettingsCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        LockSectionTitle(
                            if (userCount == 1) appText("المستخدم", "User") else appText("المستخدم ${index + 1}", "User ${index + 1}")
                        )
                        OutlinedTextField(
                            value = draft.name,
                            onValueChange = { value -> drafts[index] = draft.copy(name = value.take(32)); formError = null },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(appText("اسم المستخدم", "User name")) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )
                        PinField(
                            value = draft.pin,
                            onValueChange = { value -> drafts[index] = draft.copy(pin = normalizePinDigits(value).take(12)); formError = null },
                            label = appText("PIN جديد (6 إلى 12 رقمًا)", "New PIN (6–12 digits)"),
                            enabled = !verifying
                        )
                        PinField(
                            value = draft.confirmation,
                            onValueChange = { value -> drafts[index] = draft.copy(confirmation = normalizePinDigits(value).take(12)); formError = null },
                            label = appText("تأكيد PIN", "Confirm PIN"),
                            enabled = !verifying
                        )
                    }
                }
            }

            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LockSectionTitle(appText("متى يُطلب PIN؟", "When should the PIN be required?"))
                    AppLockPolicy.entries.forEach { option ->
                        val selected = policy == option
                        Surface(
                            onClick = { policy = option },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) AppPalette.Mint else AppPalette.Card,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (selected) AppPalette.Teal else AppPalette.Border
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(selectedColor = AppPalette.Teal)
                                )
                                Spacer(Modifier.width(5.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        policyTitle(option),
                                        color = AppPalette.TextNavy,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        policyDescription(option),
                                        color = AppPalette.Muted,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            formError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 4.dp)) }
            error?.let { Text(localizeLockError(it), color = MaterialTheme.colorScheme.error, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 4.dp)) }
            Button(
                onClick = {
                    val names = drafts.map { it.name.trim() }
                    val pins = drafts.map { it.pin }
                    val duplicateNames = names.map { it.lowercase() }.distinct().size != names.size
                    val duplicatePins = pins.distinct().size != pins.size
                    formError = when {
                        names.any { it.isBlank() } -> nameRequiredMessage
                        duplicateNames -> duplicateNamesMessage
                        pins.any { it.length !in 6..12 } -> pinLengthMessage
                        drafts.indices.any { drafts[it].pin != drafts[it].confirmation } -> pinMismatchMessage
                        duplicatePins -> duplicatePinsMessage
                        else -> null
                    }
                    if (formError == null) {
                        viewModel.saveConfiguration(names, pins, policy) { saved ->
                            if (saved) onBack()
                        }
                    }
                },
                enabled = !verifying,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppPalette.Teal)
            ) {
                if (verifying) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                else Text(if (config.enabled) appText("تحديث المستخدمين ورموز PIN", "Update users and PINs") else appText("تفعيل القفل وحفظ PIN", "Enable lock and save PIN"), fontWeight = FontWeight.Bold)
            }

            if (config.enabled && authorized) {
                OutlinedButton(
                    onClick = {
                        viewModel.updatePolicy(policy) { updated -> if (updated) onBack() }
                    },
                    enabled = !verifying,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppPalette.Teal)
                ) {
                    Text(appText("حفظ توقيت القفل فقط", "Save lock timing only"), fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = { showDisableConfirmation = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(appText("إيقاف قفل التطبيق", "Disable app lock"), fontWeight = FontWeight.SemiBold) }
            }
            Text(
                appText("تُحفظ بصمة مملّحة مشتقة من PIN ولا يُحفظ الرمز نفسه. هذا قفل داخلي ولا يغني عن قفل الهاتف، ولا يحمي من شخص يملك Root أو يمسح بيانات التطبيق.", "Only a salted PIN verifier is stored, never the PIN itself. This is an in-app privacy lock, not a replacement for your phone lock; it cannot protect against someone with root access or the ability to clear app data."),
                color = AppPalette.Muted,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            )
        }
    }

    if (showDisableConfirmation) {
        AlertDialog(
            onDismissRequest = { showDisableConfirmation = false },
            title = { Text(appText("إيقاف القفل؟", "Disable app lock?")) },
            text = { Text(appText("لن يطلب التطبيق PIN عند فتحه حتى تفعّل القفل مجدداً.", "The app will no longer require a PIN until you enable the lock again.")) },
            confirmButton = {
                TextButton(onClick = {
                    showDisableConfirmation = false
                    viewModel.disableLock { }
                }) { Text(appText("إيقاف القفل", "Disable lock"), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDisableConfirmation = false }) { Text(appText("إلغاء", "Cancel")) }
            },
            containerColor = AppPalette.Card
        )
    }
}

@Composable
private fun UserPicker(names: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        names.forEachIndexed { index, name -> UserChoiceRow(name, selected == index) { onSelect(index) } }
    }
}

@Composable
private fun UserChoiceRow(name: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) AppPalette.Mint else AppPalette.Background,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) AppPalette.Teal else AppPalette.Border)
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = AppPalette.Teal))
            Spacer(Modifier.width(6.dp))
            Text(name, color = AppPalette.TextNavy, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
        }
    }
}

@Composable
private fun UserCountChoice(count: Int, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(13.dp),
        color = if (selected) AppPalette.Mint else AppPalette.Background,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) AppPalette.Teal else AppPalette.Border)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                if (count == 1) appText("مستخدم واحد", "One user") else appText("3 مستخدمين", "3 users"),
                color = AppPalette.TextNavy,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun PinField(value: String, onValueChange: (String) -> Unit, label: String, enabled: Boolean = true) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        shape = RoundedCornerShape(14.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, AppPalette.Border, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppPalette.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), content = content)
    }
}

@Composable
private fun LockSectionTitle(text: String) {
    Text(text, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AppPalette.TextNavy)
}

@Composable
private fun BackToAppLockSettings(onBack: () -> Unit) {
    OutlinedButton(onClick = onBack, shape = RoundedCornerShape(12.dp)) {
        Text(appText("العودة إلى الإعدادات", "Back to settings"), color = AppPalette.Teal)
    }
}

private fun resizeDrafts(drafts: MutableList<PinUserDraft>, size: Int) {
    while (drafts.size > size) drafts.removeAt(drafts.lastIndex)
    while (drafts.size < size) drafts.add(PinUserDraft(""))
}

private fun normalizePinDigits(input: String): String = buildString {
    input.forEach { digit ->
        when (digit) {
            in '0'..'9' -> append(digit)
            in '٠'..'٩' -> append(('0'.code + (digit - '٠')).toChar())
            in '۰'..'۹' -> append(('0'.code + (digit - '۰')).toChar())
        }
    }
}

@Composable
private fun policyTitle(policy: AppLockPolicy): String = when (policy) {
    AppLockPolicy.ON_SCREEN_LOCK -> appText("عند إطفاء شاشة الهاتف", "When the phone screen turns off")
    AppLockPolicy.ON_APP_EXIT -> appText("مباشرة بعد مغادرة التطبيق", "Immediately after leaving the app")
    AppLockPolicy.AFTER_ONE_MINUTE -> appText("بعد دقيقة في الخلفية", "After 1 minute in the background")
    AppLockPolicy.AFTER_FIVE_MINUTES -> appText("بعد 5 دقائق في الخلفية", "After 5 minutes in the background")
}

@Composable
private fun policyDescription(policy: AppLockPolicy): String = when (policy) {
    AppLockPolicy.ON_SCREEN_LOCK -> appText("يُقفل عند إطفاء الشاشة حتى لو كان التطبيق بالخلفية.", "Locks when the screen turns off, even if the app is in the background.")
    AppLockPolicy.ON_APP_EXIT -> appText("يطلب PIN كلما غادرت التطبيق ثم عدت إليه.", "Requires a PIN whenever you leave and return.")
    AppLockPolicy.AFTER_ONE_MINUTE -> appText("يُقفل إذا بقي التطبيق بالخلفية دقيقة أو أكثر.", "Locks if the app stays in the background for at least one minute.")
    AppLockPolicy.AFTER_FIVE_MINUTES -> appText("يُقفل إذا بقي التطبيق بالخلفية خمس دقائق أو أكثر.", "Locks if the app stays in the background for at least five minutes.")
}

@Composable
private fun localizeLockError(message: String): String = when (message) {
    "رمز PIN غير صحيح. تحقق من المستخدم والرمز." -> appText(message, "Incorrect PIN. Check the selected user and code.")
    "محاولات كثيرة. حاول مجدداً بعد قليل." -> appText(message, "Too many attempts. Please try again later.")
    "تعذر حفظ الإعدادات؛ تحقق من الأسماء ورموز PIN المختلفة." -> appText(message, "Could not save. Check that names and PINs are valid and unique.")
    "تعذر إيقاف القفل؛ أعد المحاولة." -> appText(message, "Could not disable the app lock. Please try again.")
    "تعذر حفظ سياسة القفل؛ أعد المحاولة." -> appText(message, "Could not save the lock timing. Please try again.")
    else -> message
}
