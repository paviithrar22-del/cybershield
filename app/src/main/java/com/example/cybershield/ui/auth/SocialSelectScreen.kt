package com.example.cybershield.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cybershield.data.AppSettings
import com.example.cybershield.ui.main.CardBg
import com.example.cybershield.ui.main.CyanAccent
import com.example.cybershield.ui.main.DarkBg
import com.example.cybershield.ui.main.LightText
import com.example.cybershield.ui.main.MutedText
import com.example.cybershield.ui.main.PurpleAccent

private val DeepSlate = Color(0xFF0F172A)
private val GlowPurple = Color(0xFF312E81)
private val InputBg = Color(0xFF0B0F19)
private val GlassBg = Color(0xCC1E293B)

@Composable
fun SocialSelectScreen(
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val settings = remember { AppSettings.getInstance(context) }
    
    val allMonitoredApps = remember {
        listOf(
            Pair("WhatsApp", "com.whatsapp"),
            Pair("Instagram", "com.instagram.android"),
            Pair("Snapchat", "com.snapchat.android"),
            Pair("Messenger", "com.facebook.orca"),
            Pair("SMS (Default)", "com.google.android.apps.messaging")
        )
    }

    val selectedApps = remember {
        mutableStateMapOf<String, Boolean>().apply {
            val currentSet = settings.monitoredApps
            allMonitoredApps.forEach { (_, pkg) ->
                put(pkg, currentSet.contains(pkg))
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Title section
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(PurpleAccent.copy(alpha = 0.3f), Color.Transparent)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(CardBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Social select",
                            tint = CyanAccent,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Monitored Applications",
                    color = LightText,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = (-0.5).sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Select which platforms CyberShield should scan for incoming notifications and warnings.",
                    color = MutedText,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // List selection
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(allMonitoredApps) { (appName, pkgName) ->
                    val checked = selectedApps[pkgName] ?: false
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CardBg)
                            .clickable { selectedApps[pkgName] = !checked }
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = appName,
                                color = LightText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = pkgName,
                                color = MutedText,
                                fontSize = 12.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (checked) PurpleAccent else DarkBg)
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        if (checked) PurpleAccent else MutedText.copy(alpha = 0.5f)
                                    ),
                                    RoundedCornerShape(6.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (checked) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Checked",
                                    tint = LightText,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Save Actions
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(PurpleAccent, CyanAccent)
                        )
                    )
                    .clickable {
                        val activePackages = selectedApps.filter { it.value }.keys.toSet()
                        settings.monitoredApps = activePackages
                        onFinished()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SAVE & CONTINUE",
                    color = LightText,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
