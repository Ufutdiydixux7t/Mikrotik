package com.example.mikrotik.presentation.login

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mikrotik.presentation.ui.components.AppBackground
import com.example.mikrotik.presentation.ui.theme.AppIcons
import com.example.mikrotik.ui.theme.AppPalette

private val DeepGreenHeader = AppPalette.TextNavy
private val RichGreenButton = AppPalette.Teal
private val SeamlessCardBg = AppPalette.Card
private val WatermarkLineColor = AppPalette.Border

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = viewModel()
) {
    val serverUrl by viewModel.serverUrl.collectAsState()
    val serverIp by viewModel.serverIp.collectAsState()
    val serverPort by viewModel.serverPort.collectAsState()
    val isServerRunning by viewModel.isServerRunning.collectAsState()
    val healthResult by viewModel.healthTestResult.collectAsState()

    var voucherCode by remember { mutableStateOf("") }
    var loginResult by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }

    val context = LocalContext.current

    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Header
            Column(
                modifier = Modifier.padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = AppIcons.AntennaHotspot,
                    contentDescription = "Logo",
                    tint = RichGreenButton,
                    modifier = Modifier.size(52.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "AL RABDI NET",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepGreenHeader,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Server Info Box
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SeamlessCardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "حالة السيرفر: ${if (isServerRunning) "يعمل (Running)" else "متوقف"}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isServerRunning) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                            Text(
                                text = "المنفذ: $serverPort",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DeepGreenHeader
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "IP الهوتسبوت: $serverIp",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DeepGreenHeader
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "الرابط: $serverUrl",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = RichGreenButton
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Copy Link Button
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Captive Portal URL", serverUrl)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "تم نسخ الرابط بنجاح!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = RichGreenButton)
                        ) {
                            Text(text = "نسخ الرابط", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Center Login Card (Original Design Preserved)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, WatermarkLineColor, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SeamlessCardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = AppIcons.WifiSignal,
                        contentDescription = "Wifi",
                        tint = RichGreenButton,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "تسجيل الدخول",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepGreenHeader
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "أدخل البطاقة للاتصال بالإنترنت",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = voucherCode,
                        onValueChange = { voucherCode = it },
                        placeholder = {
                            Text(
                                text = "أدخل الكرت هنا",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                color = Color.Gray
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = AppIcons.CreditCard,
                                contentDescription = "Card",
                                tint = RichGreenButton
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RichGreenButton,
                            unfocusedBorderColor = WatermarkLineColor,
                            focusedContainerColor = Color(0xFFFAFCFB),
                            unfocusedContainerColor = Color(0xFFFAFCFB)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (voucherCode.isBlank()) {
                                loginResult = "يرجى إدخال كود الكرت."
                                isSuccess = false
                            } else if (voucherCode == "12345" || voucherCode.startsWith("MDI")) {
                                loginResult = "تم الاتصال بنجاح!\nالبيانات المتبقية: 4.8 GB | المدة: 29 يوم"
                                isSuccess = true
                            } else {
                                loginResult = "كود البطاقة غير صالح أو منتهي الصلاحية."
                                isSuccess = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RichGreenButton)
                    ) {
                        Text(
                            text = "اتصال",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    if (loginResult != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = loginResult ?: "",
                            color = if (isSuccess) Color(0xFF2E7D32) else Color(0xFFC62828),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = WatermarkLineColor, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Server Health Test Button (/health)
                    OutlinedButton(
                        onClick = { viewModel.testServerHealth() },
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RichGreenButton)
                    ) {
                        Text(
                            text = "اختبار صحة السيرفر (/health)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (healthResult != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = healthResult ?: "",
                            color = DeepGreenHeader,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Footer
            Column(
                modifier = Modifier.padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalDivider(
                    modifier = Modifier.width(120.dp).padding(bottom = 6.dp),
                    color = WatermarkLineColor,
                    thickness = 1.dp
                )
                Text(
                    text = "Powered by Hotspot",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}
