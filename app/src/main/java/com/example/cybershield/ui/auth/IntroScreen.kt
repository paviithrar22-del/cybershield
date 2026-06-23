package com.example.cybershield.ui.auth

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

data class IntroSlide(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color
)

@Composable
fun IntroScreen(
    onFinished: () -> Unit
) {
    val slides = remember {
        listOf(
            IntroSlide(
                title = "Real-time Guardian",
                description = "CyberShield runs silently in the background, automatically scanning incoming notifications on popular messaging services to filter out bullying and harassment.",
                icon = Icons.Default.Notifications,
                accentColor = CyanAccent
            ),
            IntroSlide(
                title = "Privacy First",
                description = "Your personal conversations are kept safe. All message scans and NLP classifications are processed 100% locally on your device. No cloud uploads.",
                icon = Icons.Default.Lock,
                accentColor = PurpleAccent
            ),
            IntroSlide(
                title = "Instant Alert System",
                description = "Upon detecting severe threats or direct harassment, CyberShield instantly dispatches automated alerts to your specified guardian contact.",
                icon = Icons.Default.Send,
                accentColor = CyanAccent
            )
        )
    }

    var currentSlideIndex by remember { mutableIntStateOf(0) }
    val slide = slides[currentSlideIndex]

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
            // Top Navigation / Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentSlideIndex > 0) {
                    IconButton(onClick = { currentSlideIndex-- }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = LightText)
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }
                
                Text(
                    text = "${currentSlideIndex + 1} of ${slides.size}",
                    color = MutedText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Skip",
                    color = CyanAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onFinished() }
                        .padding(8.dp)
                )
            }

            // Slide Content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 32.dp)
            ) {
                // Pulsing Icon Frame
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(slide.accentColor.copy(alpha = 0.25f), Color.Transparent)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(GlassBg)
                            .shadow(12.dp, RoundedCornerShape(28.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = slide.icon,
                            contentDescription = slide.title,
                            tint = slide.accentColor,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                Text(
                    text = slide.title,
                    color = LightText,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = (-0.5).sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = slide.description,
                    color = MutedText,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Footer Actions
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Dots Indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    slides.forEachIndexed { index, _ ->
                        val active = index == currentSlideIndex
                        Box(
                            modifier = Modifier
                                .size(if (active) 20.dp else 8.dp, 8.dp)
                                .clip(CircleShape)
                                .background(if (active) slide.accentColor else MutedText.copy(alpha = 0.4f))
                        )
                    }
                }

                // Next / Get Started button
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
                            if (currentSlideIndex < slides.size - 1) {
                                currentSlideIndex++
                            } else {
                                onFinished()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (currentSlideIndex == slides.size - 1) "GET STARTED" else "NEXT STEP",
                            color = LightText,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            letterSpacing = 1.sp
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Next",
                            tint = LightText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
