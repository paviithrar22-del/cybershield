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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cybershield.data.AppSettings
import com.example.cybershield.data.SupabaseManager
import com.example.cybershield.ui.main.CyanAccent
import com.example.cybershield.ui.main.DarkBg
import com.example.cybershield.ui.main.LightText
import com.example.cybershield.ui.main.MutedText
import com.example.cybershield.ui.main.PurpleAccent
import com.example.cybershield.ui.main.RoseAccent
import kotlinx.coroutines.launch
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private val DeepSlate = Color(0xFF0F172A)
private val GlowPurple = Color(0xFF312E81)
private val InputBg = Color(0xFF0B0F19)
private val GlassBg = Color(0xCC1E293B)

@Composable
fun RegisterScreen(
    onLoginClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        GlowPurple,
                        DeepSlate,
                        DeepSlate
                    )
                )
            ),
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
            // Glowing Emblem
            item {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .shadow(16.dp, CircleShape)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(PurpleAccent.copy(alpha = 0.4f), Color.Transparent)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(GlassBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Shield Register",
                            tint = CyanAccent,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }

            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Create Account",
                        color = LightText,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Shield your device and alert guardians",
                        color = MutedText,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Glassmorphism Form Card
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
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Name", tint = PurpleAccent) },
                            label = { Text("Full Name", color = MutedText) },
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
                            value = phone,
                            onValueChange = { phone = it },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "Phone", tint = PurpleAccent) },
                            label = { Text("Phone Number", color = MutedText) },
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
                            value = email,
                            onValueChange = { email = it },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email", tint = PurpleAccent) },
                            label = { Text("Gmail Address", color = MutedText) },
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

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password", tint = PurpleAccent) },
                            label = { Text("Password", color = MutedText) },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
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
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Confirm Password", tint = PurpleAccent) },
                            label = { Text("Confirm Password", color = MutedText) },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
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

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                if (isLoading) {
                    CircularProgressIndicator(color = CyanAccent, modifier = Modifier.size(36.dp))
                } else {
                    // Gradient Submit Button
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
                                val emailTrim = email.trim()
                                val passTrim = password.trim()
                                val nameTrim = name.trim()
                                val phoneTrim = phone.trim()
                                val confirmTrim = confirmPassword.trim()

                                if (nameTrim.isEmpty() || phoneTrim.isEmpty() || emailTrim.isEmpty() || passTrim.isEmpty() || confirmTrim.isEmpty()) {
                                    Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                                    return@clickable
                                }
                                if (passTrim != confirmTrim) {
                                    Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                                    return@clickable
                                }
                                if (passTrim.length < 6) {
                                    Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                                    return@clickable
                                }
                                
                                isLoading = true
                                scope.launch {
                                    try {
                                        SupabaseManager.getInstance().client.auth.signUpWith(Email) {
                                            this.email = emailTrim
                                            this.password = passTrim
                                            this.data = buildJsonObject {
                                                put("name", nameTrim)
                                                put("phone", phoneTrim)
                                            }
                                        }
                                        AppSettings.getInstance(context).guardianName = nameTrim
                                        Toast.makeText(context, "Registration successful! Verification email sent.", Toast.LENGTH_LONG).show()
                                        onLoginClick()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error: ${e.localizedMessage ?: e.message}", Toast.LENGTH_LONG).show()
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "CREATE ACCOUNT",
                            color = LightText,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Already have an account? Log In",
                        color = CyanAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onLoginClick() }
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}
