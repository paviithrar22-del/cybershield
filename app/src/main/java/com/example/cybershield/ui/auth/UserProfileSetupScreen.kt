package com.example.cybershield.ui.auth

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.text.input.KeyboardType
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
fun UserProfileSetupScreen(
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val settings = remember { AppSettings.getInstance(context) }

    var guardianName by remember { mutableStateOf(settings.guardianName) }
    var guardianPhone by remember { mutableStateOf(settings.guardianPhone) }
    var guardianEmail by remember { mutableStateOf(settings.guardianEmail) }
    var sensitivityLevel by remember { mutableStateOf(settings.sensitivity) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 24.dp)
        ) {
            // Emblem
            item {
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
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Profile Setup",
                            tint = CyanAccent,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }

            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Guardian & Protection",
                        color = LightText,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = (-0.5).sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Set up your guardian contact information to receive automated incident alerts.",
                        color = MutedText,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Glassmorphism configuration card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = GlassBg),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Brush.linearGradient(listOf(Color(0x33FFFFFF), Color(0x05FFFFFF)))),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(24.dp, RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Guardian Contact Info",
                            color = LightText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = guardianName,
                            onValueChange = { guardianName = it },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Guardian Name", tint = PurpleAccent) },
                            label = { Text("Guardian Name", color = MutedText) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PurpleAccent,
                                unfocusedBorderColor = MutedText.copy(alpha = 0.3f),
                                focusedLabelColor = PurpleAccent,
                                unfocusedLabelColor = MutedText,
                                focusedTextColor = LightText,
                                unfocusedTextColor = LightText
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = guardianPhone,
                            onValueChange = { guardianPhone = it },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "Guardian Phone", tint = PurpleAccent) },
                            label = { Text("Guardian Phone", color = MutedText) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PurpleAccent,
                                unfocusedBorderColor = MutedText.copy(alpha = 0.3f),
                                focusedLabelColor = PurpleAccent,
                                unfocusedLabelColor = MutedText,
                                focusedTextColor = LightText,
                                unfocusedTextColor = LightText
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = guardianEmail,
                            onValueChange = { guardianEmail = it },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Guardian Email", tint = PurpleAccent) },
                            label = { Text("Guardian Email", color = MutedText) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PurpleAccent,
                                unfocusedBorderColor = MutedText.copy(alpha = 0.3f),
                                focusedLabelColor = PurpleAccent,
                                unfocusedLabelColor = MutedText,
                                focusedTextColor = LightText,
                                unfocusedTextColor = LightText
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Sensitivity card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = GlassBg),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Brush.linearGradient(listOf(Color(0x33FFFFFF), Color(0x05FFFFFF)))),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Detection Sensitivity",
                            color = LightText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Choose how strictly messages are scanned. Medium is recommended.",
                            color = MutedText,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Low", "Medium", "High").forEach { level ->
                                val selected = sensitivityLevel == level
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (selected) PurpleAccent else DarkBg)
                                        .clickable { sensitivityLevel = level }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = level,
                                        color = if (selected) LightText else MutedText,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Finish Action
            item {
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
                            val nameTrim = guardianName.trim()
                            val phoneTrim = guardianPhone.trim()
                            val emailTrim = guardianEmail.trim()

                            if (nameTrim.isEmpty() || phoneTrim.isEmpty() || emailTrim.isEmpty()) {
                                Toast.makeText(context, "Please configure all guardian contact info", Toast.LENGTH_SHORT).show()
                                return@clickable
                            }

                            // Save settings
                            settings.guardianName = nameTrim
                            settings.guardianPhone = phoneTrim
                            settings.guardianEmail = emailTrim
                            settings.sensitivity = sensitivityLevel
                            
                            Toast.makeText(context, "Setup Completed successfully!", Toast.LENGTH_SHORT).show()
                            onFinished()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "FINISH SETUP",
                        color = LightText,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}
