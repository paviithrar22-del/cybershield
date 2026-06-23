package com.example.cybershield.ui.auth

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.cybershield.ui.main.CardBg
import com.example.cybershield.ui.main.CyanAccent
import com.example.cybershield.ui.main.DarkBg
import com.example.cybershield.ui.main.LightText
import com.example.cybershield.ui.main.MutedText
import com.example.cybershield.ui.main.PurpleAccent
import com.example.cybershield.ui.main.RoseAccent
import kotlinx.coroutines.delay

private val DeepSlate = Color(0xFF0F172A)
private val GlowPurple = Color(0xFF312E81)
private val InputBg = Color(0xFF0B0F19)
private val GlassBg = Color(0xCC1E293B)

@Composable
fun PermissionSetupScreen(
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    
    var isNotificationAccessGranted by remember { mutableStateOf(isNotificationServiceEnabled(context)) }
    var isSmsAccessGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Periodically poll notification listener permission status
    LaunchedEffect(Unit) {
        while (true) {
            isNotificationAccessGranted = isNotificationServiceEnabled(context)
            delay(1500)
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            isSmsAccessGranted = granted
        }
    )

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

            // Title
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
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Permission config",
                            tint = CyanAccent,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Grant System Access",
                    color = LightText,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = (-0.5).sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "CyberShield requires system permissions to run its scanning engine and dispatch guardian alerts.",
                    color = MutedText,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            // Permissions list
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Permission 1: Notification Scan
                PermissionItem(
                    title = "Notification Scanning (Required)",
                    description = "Used to analyze incoming notifications and identify harassment. We never upload message logs.",
                    icon = Icons.Default.Notifications,
                    isGranted = isNotificationAccessGranted,
                    onGrantClick = {
                        try {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open notification settings", Toast.LENGTH_LONG).show()
                        }
                    }
                )

                // Permission 2: SMS Send
                PermissionItem(
                    title = "Automated SMS Alerts (Recommended)",
                    description = "Used to immediately send alert texts to your guardians when threats are detected.",
                    icon = Icons.Default.Settings,
                    isGranted = isSmsAccessGranted,
                    onGrantClick = {
                        launcher.launch(android.Manifest.permission.SEND_SMS)
                    }
                )
            }

            // Action
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
                        if (!isNotificationAccessGranted) {
                            Toast.makeText(context, "Notification scanning access is required to run CyberShield!", Toast.LENGTH_LONG).show()
                        } else {
                            onFinished()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PROCEED & CONTINUE",
                    color = LightText,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun PermissionItem(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    onGrantClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, if (isGranted) CyanAccent.copy(alpha = 0.2f) else Color.Transparent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PurpleAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = title, tint = PurpleAccent, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        text = title,
                        color = LightText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isGranted) CyanAccent.copy(alpha = 0.15f) else RoseAccent.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isGranted) "GRANTED" else "SETUP",
                        color = if (isGranted) CyanAccent else RoseAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = description,
                color = MutedText,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            if (!isGranted) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onGrantClick,
                    colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Grant Permission", color = LightText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

fun isNotificationServiceEnabled(context: Context): Boolean {
    val packageNames = NotificationManagerCompat.getEnabledListenerPackages(context)
    return packageNames.contains(context.packageName)
}
