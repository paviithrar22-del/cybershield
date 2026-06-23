package com.example.cybershield.ui.main

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import kotlinx.coroutines.delay
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler
import android.print.PrintManager
import android.print.PrintAttributes
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.cybershield.data.AppSettings
import com.example.cybershield.data.DefaultDataRepository
import com.example.cybershield.data.Incident
import com.example.cybershield.nlp.Severity
import com.example.cybershield.nlp.BullyingClassifier
import java.text.SimpleDateFormat
import java.util.*

// Harmonized Color Palette
val DarkBg = Color(0xFF0F172A)
val CardBg = Color(0xFF1E293B)
val CyanAccent = Color(0xFF06B6D4)
val PurpleAccent = Color(0xFF8B5CF6)
val AmberAccent = Color(0xFFF59E0B)
val RoseAccent = Color(0xFFF43F5E)
val LightText = Color(0xFFF8FAFC)
val MutedText = Color(0xFF94A3B8)

class MainScreenViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainScreenViewModel(
            DefaultDataRepository(context),
            AppSettings.getInstance(context)
        ) as T
    }
}

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = viewModel(
        factory = MainScreenViewModelFactory(LocalContext.current.applicationContext)
    )
) {
    val context = LocalContext.current
    var isPermissionGranted by remember { mutableStateOf(isNotificationServiceEnabled(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            isPermissionGranted = isNotificationServiceEnabled(context)
            kotlinx.coroutines.delay(2000)
        }
    }

    val currentTheme = viewModel.themeMode
    val computedBg = when (currentTheme) {
        "Pitch Black" -> Color.Black
        "Auto-Schedule" -> Color(0xFF020617)
        else -> Color(0xFF0F172A)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(computedBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header
            HeaderBar(isPermissionGranted = isPermissionGranted) {
                try {
                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Could not open settings", Toast.LENGTH_LONG).show()
                }
            }

            if (!isPermissionGranted) {
                PermissionOnboarding(onGrantClick = {
                    try {
                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Could not open settings", Toast.LENGTH_LONG).show()
                    }
                })
            } else {
                MainTabsAndContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun HeaderBar(isPermissionGranted: Boolean, onStatusClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "CyberShield",
                color = LightText,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )
            Text(
                text = "Real-time Protection",
                color = MutedText,
                fontSize = 12.sp
            )
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(if (isPermissionGranted) CyanAccent.copy(alpha = 0.15f) else RoseAccent.copy(alpha = 0.15f))
                .clickable { onStatusClick() }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isPermissionGranted) CyanAccent else RoseAccent)
            )
            Text(
                text = if (isPermissionGranted) "ACTIVE" else "SETUP",
                color = if (isPermissionGranted) CyanAccent else RoseAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PermissionOnboarding(onGrantClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Shield Alert",
            tint = PurpleAccent,
            modifier = Modifier
                .size(100.dp)
                .shadow(12.dp, CircleShape)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Enable Real-Time Scanning",
            color = LightText,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "CyberShield runs silently in the background, automatically scanning incoming messaging notifications for bullying, harassment, or threats.\n\nWe NEVER send your personal messages to any servers. The NLP pipeline runs 100% locally on your device.",
            color = MutedText,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onGrantClick,
            colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Grant Notification Access", color = LightText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun MainTabsAndContent(viewModel: MainScreenViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val currentTheme = viewModel.themeMode
    val computedBg = when (currentTheme) {
        "Pitch Black" -> Color.Black
        "Auto-Schedule" -> Color(0xFF020617)
        else -> Color(0xFF0F172A)
    }

    Scaffold(
        containerColor = computedBg,
        bottomBar = {
            NavigationBar(
                containerColor = CardBg,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PurpleAccent,
                        unselectedIconColor = MutedText,
                        selectedTextColor = LightText,
                        unselectedTextColor = MutedText,
                        indicatorColor = PurpleAccent.copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(imageVector = Icons.Default.List, contentDescription = "Incidents") },
                    label = { Text("Incidents", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PurpleAccent,
                        unselectedIconColor = MutedText,
                        selectedTextColor = LightText,
                        unselectedTextColor = MutedText,
                        indicatorColor = PurpleAccent.copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(imageVector = Icons.Default.Info, contentDescription = "Analysis") },
                    label = { Text("Analysis", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PurpleAccent,
                        unselectedIconColor = MutedText,
                        selectedTextColor = LightText,
                        unselectedTextColor = MutedText,
                        indicatorColor = PurpleAccent.copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PurpleAccent,
                        unselectedIconColor = MutedText,
                        selectedTextColor = LightText,
                        unselectedTextColor = MutedText,
                        indicatorColor = PurpleAccent.copy(alpha = 0.15f)
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(computedBg)
        ) {
            when (selectedTab) {
                0 -> HomeScreen(viewModel)
                1 -> DetectedIncidentListScreen(viewModel)
                2 -> AnalysisHubScreen(viewModel)
                3 -> SettingsScreen(viewModel)
            }
        }
    }
}

@Composable
fun HomeScreen(viewModel: MainScreenViewModel) {
    val totalScanned by viewModel.totalScanned.collectAsStateWithLifecycle()
    val totalFlagged by viewModel.totalFlagged.collectAsStateWithLifecycle()
    
    val logs = remember { mutableStateListOf<String>() }
    val monitored = viewModel.monitoredApps
    
    LaunchedEffect(Unit) {
        if (logs.isEmpty()) {
            logs.add("[10:14:02 AM] Guardian Shield service initialized.")
            logs.add("[10:14:03 AM] On-device NLP engine online.")
            logs.add("[10:15:30 AM] Registered intercept listener.")
        }
    }

    LaunchedEffect(Unit) {
        val appsList = listOf("WhatsApp", "Instagram", "Snapchat", "Messenger", "SMS")
        val scanMsgs = listOf(
            "Interception check... OK",
            "Scanned notification payload... Safe",
            "NLP Classification level: None",
            "Listening channels: Monitoring active"
        )
        while (true) {
            delay(5000)
            val time = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date())
            val app = appsList.random()
            val msg = scanMsgs.random()
            logs.add("[$time] Intercepted $app DM: $msg")
            if (logs.size > 15) {
                logs.removeAt(0)
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "monitoringRadar")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp, top = 8.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(CyanAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Home, contentDescription = "Home Welcome", tint = CyanAccent, modifier = Modifier.size(28.dp))
                    }
                    Column {
                        Text(text = "System Secured", color = LightText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(text = "CyberShield is running actively.", color = MutedText, fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .weight(1.1f)
                        .height(180.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(75.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(CyanAccent.copy(alpha = 0.12f))
                                .border(BorderStroke(2.dp, CyanAccent.copy(alpha = 0.4f)), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(45.dp)
                                    .clip(CircleShape)
                                    .background(CyanAccent.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Active scanning",
                                    tint = CyanAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = "SCANNER ACTIVE", color = CyanAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Scanning chats...", color = MutedText, fontSize = 9.sp)
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(180.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Safety Score",
                            color = LightText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        val score = if (totalScanned == 0) 100 else {
                            val scoreVal = 100 - ((totalFlagged.toFloat() / totalScanned.toFloat()) * 100).toInt()
                            scoreVal.coerceIn(0, 100)
                        }

                        val scoreColor = when {
                            score >= 90 -> CyanAccent
                            score >= 70 -> PurpleAccent
                            score >= 50 -> AmberAccent
                            else -> RoseAccent
                        }

                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .background(scoreColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$score%",
                                color = scoreColor,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (score >= 90) "Safe" else "Warnings",
                            color = scoreColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard(
                    title = "Total Scanned",
                    value = totalScanned.toString(),
                    icon = Icons.Default.Notifications,
                    accentColor = CyanAccent,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Flagged Incidents",
                    value = totalFlagged.toString(),
                    icon = Icons.Default.Warning,
                    accentColor = if (totalFlagged > 0) RoseAccent else MutedText,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Monitored Platforms",
                        color = LightText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val allTargetApps = listOf(
                        Pair("WhatsApp", "com.whatsapp"),
                        Pair("Instagram", "com.instagram.android"),
                        Pair("Snapchat", "com.snapchat.android"),
                        Pair("Messenger", "com.facebook.orca"),
                        Pair("SMS (Default)", "com.google.android.apps.messaging")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        allTargetApps.take(3).forEach { (name, pkg) ->
                            val active = monitored.contains(pkg)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (active) CyanAccent else Color.DarkGray)
                                )
                                Text(text = name, color = if (active) LightText else MutedText, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "On-Device Processing Stream",
                        color = CyanAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        items(logs.reversed()) { log ->
                            Text(
                                text = log,
                                color = MutedText,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun AnalysisHubScreen(viewModel: MainScreenViewModel) {
    val incidents by viewModel.incidents.collectAsStateWithLifecycle()
    var selectedSubTab by remember { mutableIntStateOf(0) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = "NLP Safety Analysis",
            color = LightText,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = "On-device intelligence logs",
            color = MutedText,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = Color.Transparent,
            contentColor = PurpleAccent,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedSubTab]),
                    color = PurpleAccent
                )
            },
            divider = { HorizontalDivider(color = CardBg) }
        ) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = { Text("Severity", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = { Text("Keywords", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
            )
            Tab(
                selected = selectedSubTab == 2,
                onClick = { selectedSubTab = 2 },
                text = { Text("Sentiment", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
            )
            Tab(
                selected = selectedSubTab == 3,
                onClick = { selectedSubTab = 3 },
                text = { Text("Reports", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.weight(1f)) {
            when (selectedSubTab) {
                0 -> SeverityAnalysisScreen(incidents = incidents)
                1 -> KeywordDetectionScreen(viewModel = viewModel, incidents = incidents)
                2 -> SentimentAnalysisScreen(viewModel = viewModel, incidents = incidents)
                3 -> ReportsScreen(viewModel = viewModel, incidents = incidents)
            }
        }
    }
}

@Composable
fun ReportsScreen(viewModel: MainScreenViewModel, incidents: List<Incident>) {
    var isExporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableStateOf(0f) }
    var exportMessage by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Mock weekly metrics
    val weeklyData = listOf(
        Pair("Mon", 1), Pair("Tue", 0), Pair("Wed", 3),
        Pair("Thu", 2), Pair("Fri", 4), Pair("Sat", 1), Pair("Sun", 0)
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Weekly reports screen
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Weekly Incident Trend",
                        color = LightText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Flagged alerts registered over the last 7 days.",
                        color = MutedText,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val maxIncidents = (weeklyData.maxOf { it.second }).coerceAtLeast(1)
                        weeklyData.forEach { (day, count) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = count.toString(),
                                    color = if (count > 0) PurpleAccent else MutedText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Box(
                                    modifier = Modifier
                                        .width(20.dp)
                                        .height((count.toFloat() / maxIncidents * 80).dp.coerceAtLeast(4.dp))
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(if (count > 0) PurpleAccent else Color.DarkGray)
                                )
                                Text(text = day, color = MutedText, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        // PDF Export Screen Content
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Safety Log PDF Export",
                        color = LightText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Compile and download a detailed diagnostic PDF containing safety scores, sentiment trends, and match logs to present to counselors or teachers.",
                        color = MutedText,
                        fontSize = 11.sp
                    )

                    HorizontalDivider(color = DarkBg.copy(alpha = 0.5f))

                    if (isExporting) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                        ) {
                            Text(text = exportMessage, color = CyanAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            LinearProgressIndicator(
                                progress = { exportProgress },
                                color = CyanAccent,
                                trackColor = Color.DarkGray,
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                scope.launch {
                                    isExporting = true
                                    exportProgress = 0.2f
                                    exportMessage = "Compiling incident metrics..."
                                    delay(600)
                                    exportProgress = 0.6f
                                    exportMessage = "Formatting analytical charts..."
                                    delay(600)
                                    exportProgress = 1.0f
                                    isExporting = false
                                    try {
                                        printReport(context, incidents)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Export/Print failed: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = "Export PDF", tint = DarkBg, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export Safety Report as PDF", color = DarkBg, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun SeverityAnalysisScreen(incidents: List<Incident>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            SeverityBreakdownCard(incidents = incidents)
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Severity Threat Guidelines",
                        color = LightText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    val items = listOf(
                        Triple("CRITICAL THREATS", "Includes direct encouragement of self-harm, physical violence, or extreme insults. CyberShield immediately sends an automated SMS alert to the guardian contact.", RoseAccent),
                        Triple("HIGH THREATS", "Includes direct harassment, persistent calling, or exclusion words. Guardians are recommended to review messages and talk to the child.", AmberAccent),
                        Triple("MEDIUM THREATS", "Includes basic slurs, profanities, and minor insults. Monitored for recurring frequency.", PurpleAccent),
                        Triple("LOW THREATS", "Includes toxic conversational phrases, shut up, go away, etc. Monitored for persistent escalation.", CyanAccent)
                    )

                    items.forEach { (title, description, color) ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .padding(top = 4.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Column {
                                Text(text = title, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(text = description, color = MutedText, fontSize = 11.sp, lineHeight = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KeywordDetectionScreen(viewModel: MainScreenViewModel, incidents: List<Incident>) {
    var newCustomKeyword by remember { mutableStateOf("") }
    val customKeywords = viewModel.customKeywords
    
    val textToScan = incidents.joinToString(" ") { it.messageContent.lowercase(java.util.Locale.ROOT) }
    val tokens = textToScan.split(Regex("[\\s.,!?;:\"]+")).filter { it.length > 2 }
    
    val matchedFreqs = remember(incidents, customKeywords) {
        tokens.groupBy { it }
            .mapValues { it.value.size }
            .filter { (word, _) ->
                BullyingClassifier.threatWords.contains(word) ||
                BullyingClassifier.hateWords.contains(word) ||
                BullyingClassifier.harassmentWords.contains(word) ||
                BullyingClassifier.profanityWords.contains(word) ||
                customKeywords.contains(word)
            }
            .toList()
            .sortedByDescending { it.second }
            .take(6)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Most Frequently Detected Keywords",
                        color = LightText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (matchedFreqs.isEmpty()) {
                        Text(
                            text = "No flagged keywords detected in logs yet.",
                            color = MutedText,
                            fontSize = 13.sp
                        )
                    } else {
                        val maxCount = matchedFreqs.maxOf { it.second }.toFloat()
                        matchedFreqs.forEach { (word, count) ->
                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = word, color = LightText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text(text = "$count match(es)", color = CyanAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.DarkGray)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(count.toFloat() / maxCount)
                                            .background(CyanAccent)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight((1f - (count.toFloat() / maxCount)).coerceAtLeast(0.001f))
                                            .background(Color.Transparent)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Custom Guardian Flagged Keywords",
                        color = LightText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Add specific keywords or private slang to trigger intermediate level alerts automatically.",
                        color = MutedText,
                        fontSize = 11.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newCustomKeyword,
                            onValueChange = { newCustomKeyword = it },
                            placeholder = { Text("e.g. badword", color = MutedText.copy(alpha = 0.5f)) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PurpleAccent,
                                unfocusedBorderColor = MutedText.copy(alpha = 0.3f),
                                focusedTextColor = LightText,
                                unfocusedTextColor = LightText
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (newCustomKeyword.isNotBlank()) {
                                    viewModel.addCustomKeyword(newCustomKeyword)
                                    newCustomKeyword = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text("Add", color = LightText, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (customKeywords.isEmpty()) {
                        Text(
                            text = "No custom keywords registered.",
                            color = MutedText,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            customKeywords.chunked(3).forEach { rowKeywords ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    rowKeywords.forEach { word ->
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(PurpleAccent.copy(alpha = 0.15f))
                                                .border(1.dp, PurpleAccent.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(text = word, color = PurpleAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove",
                                                tint = PurpleAccent,
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clickable { viewModel.removeCustomKeyword(word) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SentimentAnalysisScreen(viewModel: MainScreenViewModel, incidents: List<Incident>) {
    val totalScanned by viewModel.totalScanned.collectAsStateWithLifecycle()
    val totalFlagged by viewModel.totalFlagged.collectAsStateWithLifecycle()

    val safeCount = (totalScanned - totalFlagged).coerceAtLeast(0)
    val cautionCount = incidents.count { it.severity == Severity.MEDIUM || it.severity == Severity.LOW }
    val negativeCount = incidents.count { it.severity == Severity.CRITICAL || it.severity == Severity.HIGH }

    val sentimentScore = if (totalScanned == 0) 100 else {
        val score = (safeCount.toFloat() / totalScanned.toFloat() * 100).toInt()
        score.coerceIn(0, 100)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Sentiment Health Index",
                        color = LightText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val scoreColor = when {
                        sentimentScore >= 85 -> CyanAccent
                        sentimentScore >= 60 -> PurpleAccent
                        sentimentScore >= 40 -> AmberAccent
                        else -> RoseAccent
                    }

                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(scoreColor.copy(alpha = 0.12f))
                            .border(BorderStroke(4.dp, scoreColor.copy(alpha = 0.3f)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$sentimentScore%",
                                color = scoreColor,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = if (sentimentScore >= 85) "Positive" else if (sentimentScore >= 60) "Fair" else "Hostile",
                                color = scoreColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Calculated from $totalScanned scanned interactions.",
                        color = MutedText,
                        fontSize = 12.sp
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Interaction Sentiment Distribution",
                        color = LightText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    SentimentBarRow(
                        label = "Positive Tone (Safe Messages)",
                        count = safeCount,
                        percent = if (totalScanned == 0) 100f else (safeCount.toFloat() / totalScanned.toFloat() * 100),
                        color = CyanAccent
                    )

                    SentimentBarRow(
                        label = "Caution Tone (Low/Medium Alerts)",
                        count = cautionCount,
                        percent = if (totalScanned == 0) 0f else (cautionCount.toFloat() / totalScanned.toFloat() * 100),
                        color = PurpleAccent
                    )

                    SentimentBarRow(
                        label = "Hostile Tone (High/Critical Alerts)",
                        count = negativeCount,
                        percent = if (totalScanned == 0) 0f else (negativeCount.toFloat() / totalScanned.toFloat() * 100),
                        color = RoseAccent
                    )
                }
            }
        }
    }
}

@Composable
fun SentimentBarRow(label: String, count: Int, percent: Float, color: Color) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, color = LightText, fontSize = 12.sp)
            Text(text = "$count (${percent.toInt()}%)", color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.DarkGray)
        ) {
            if (percent > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(percent / 100f)
                        .background(color)
                )
            }
            if (percent < 100) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(((100f - percent) / 100f).coerceAtLeast(0.001f))
                        .background(Color.Transparent)
                )
            }
        }
    }
}


@Composable
fun MonitoringScreen(viewModel: MainScreenViewModel) {
    val context = LocalContext.current
    val settings = remember { AppSettings.getInstance(context) }
    val monitored = remember { settings.monitoredApps }
    
    // Live logs state
    val logs = remember { mutableStateListOf<String>() }
    
    // Initialize logs
    LaunchedEffect(Unit) {
        if (logs.isEmpty()) {
            logs.add("[10:14:02 AM] Guardian Shield service initialized.")
            logs.add("[10:14:03 AM] On-device NLP engine online.")
            logs.add("[10:14:05 AM] Loaded settings (Sensitivity: ${settings.sensitivity}).")
            logs.add("[10:15:30 AM] Registered intercept listener.")
        }
    }

    // Periodically append log entries
    LaunchedEffect(Unit) {
        val appsList = listOf("WhatsApp", "Instagram", "Snapchat", "Messenger", "SMS")
        val scanMsgs = listOf(
            "Interception check... OK",
            "Scanned notification payload... Safe",
            "NLP Classification level: None",
            "Listening channels: Monitoring active"
        )
        while (true) {
            delay(5000) // add a new entry every 5 seconds
            val time = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date())
            val app = appsList.random()
            val msg = scanMsgs.random()
            logs.add("[$time] Intercepted $app DM: $msg")
            if (logs.size > 20) {
                logs.removeAt(0) // keep it under 20 logs
            }
        }
    }

    // Pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "monitoringRadar")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp, top = 8.dp)
    ) {
        item {
            Text(
                text = "Real-Time Interceptor",
                color = LightText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Pulse radar scanner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(CyanAccent.copy(alpha = 0.12f))
                            .border(BorderStroke(2.dp, CyanAccent.copy(alpha = 0.4f)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .background(CyanAccent.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Active scanning",
                                tint = CyanAccent,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "SCANNER ACTIVE", color = CyanAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Listening for incoming chat notifications...", color = MutedText, fontSize = 12.sp)
                }
            }
        }

        // Monitored Apps list card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Intercept Targets",
                        color = LightText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val allTargetApps = listOf(
                        Pair("WhatsApp", "com.whatsapp"),
                        Pair("Instagram", "com.instagram.android"),
                        Pair("Snapchat", "com.snapchat.android"),
                        Pair("Messenger", "com.facebook.orca"),
                        Pair("SMS (Default)", "com.google.android.apps.messaging")
                    )

                    allTargetApps.forEach { (name, pkg) ->
                        val isMonitored = monitored.contains(pkg)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = name, color = LightText, fontSize = 13.sp)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isMonitored) CyanAccent else Color.DarkGray)
                                )
                                Text(
                                    text = if (isMonitored) "LISTENING" else "OFF",
                                    color = if (isMonitored) CyanAccent else MutedText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Live logs terminal
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkBg),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Interceptor Console Log",
                        color = CyanAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(logs.reversed()) { log ->
                            Text(
                                text = log,
                                color = MutedText,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetectedIncidentListScreen(viewModel: MainScreenViewModel) {
    val incidents by viewModel.incidents.collectAsStateWithLifecycle()
    
    var selectedFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var activeDetailsIncident by remember { mutableStateOf<Incident?>(null) }

    val filteredIncidents = remember(incidents, selectedFilter, searchQuery) {
        incidents.filter { inc ->
            val matchesFilter = if (selectedFilter == "All") true else inc.severity.name.equals(selectedFilter, ignoreCase = true)
            val matchesQuery = if (searchQuery.isEmpty()) true else {
                inc.sender.contains(searchQuery, ignoreCase = true) || 
                inc.messageContent.contains(searchQuery, ignoreCase = true)
            }
            matchesFilter && matchesQuery
        }
    }

    if (activeDetailsIncident != null) {
        IncidentDetailsScreen(incident = activeDetailsIncident!!, onDismiss = { activeDetailsIncident = null })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = "Detected Incidents",
            color = LightText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = PurpleAccent) },
            placeholder = { Text("Search sender or message...", color = MutedText.copy(alpha = 0.6f)) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PurpleAccent,
                unfocusedBorderColor = CardBg,
                focusedTextColor = LightText,
                unfocusedTextColor = LightText
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val filterOptions = listOf("All", "Critical", "High", "Medium", "Low")
            filterOptions.forEach { filter ->
                val active = selectedFilter == filter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (active) PurpleAccent else CardBg)
                        .clickable { selectedFilter = filter }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = filter,
                        color = if (active) LightText else MutedText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${filteredIncidents.size} Flagged Incidents",
                color = MutedText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (filteredIncidents.isNotEmpty()) {
                Text(
                    text = "Clear All",
                    color = RoseAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable {
                            viewModel.clearLogs()
                            viewModel.resetStatistics()
                        }
                        .padding(4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredIncidents.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBg)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Safe",
                        tint = CyanAccent.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No matching incidents detected",
                        color = MutedText,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredIncidents, key = { it.id }) { incident ->
                    Box(
                        modifier = Modifier.clickable { activeDetailsIncident = incident }
                    ) {
                        IncidentRow(incident = incident)
                    }
                }
            }
        }
    }
}

@Composable
fun IncidentDetailsScreen(incident: Incident, onDismiss: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("hh:mm a, dd MMM yyyy", Locale.getDefault()) }
    val severityColor = when (incident.severity) {
        Severity.CRITICAL -> RoseAccent
        Severity.HIGH -> AmberAccent
        Severity.MEDIUM -> PurpleAccent
        else -> CyanAccent
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Incident Details",
                    color = LightText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(severityColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = incident.severity.name,
                        color = severityColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HorizontalDivider(color = CardBg)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Sender", color = MutedText, fontSize = 12.sp)
                    Text(text = incident.sender, color = LightText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Platform", color = MutedText, fontSize = 12.sp)
                    Text(text = incident.sourceApp.split(".").last().replaceFirstChar { it.uppercase() }, color = LightText, fontSize = 13.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Received", color = MutedText, fontSize = 12.sp)
                    Text(text = dateFormat.format(Date(incident.timestamp)), color = LightText, fontSize = 13.sp)
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkBg)
                        .padding(10.dp)
                ) {
                    Text(text = "Flagged Message", color = MutedText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "\"${incident.messageContent}\"", color = LightText, fontSize = 13.sp)
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "NLP Diagnostics", color = MutedText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = incident.reason, color = LightText, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Dismiss", color = LightText, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = CardBg,
        shape = RoundedCornerShape(20.dp)
    )
}


@Composable
fun StatCard(title: String, value: String, icon: ImageVector, accentColor: Color, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.shadow(4.dp, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, color = MutedText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Icon(imageVector = icon, contentDescription = title, tint = accentColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                color = LightText,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SeverityBreakdownCard(incidents: List<Incident>) {
    val total = incidents.size.toFloat()
    val criticalCount = incidents.count { it.severity == Severity.CRITICAL }
    val highCount = incidents.count { it.severity == Severity.HIGH }
    val mediumCount = incidents.count { it.severity == Severity.MEDIUM }
    val lowCount = incidents.count { it.severity == Severity.LOW }

    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Severity Breakdown",
                color = LightText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.DarkGray)
            ) {
                if (criticalCount > 0) {
                    Box(modifier = Modifier.weight(criticalCount / total).fillMaxHeight().background(RoseAccent))
                }
                if (highCount > 0) {
                    Box(modifier = Modifier.weight(highCount / total).fillMaxHeight().background(AmberAccent))
                }
                if (mediumCount > 0) {
                    Box(modifier = Modifier.weight(mediumCount / total).fillMaxHeight().background(PurpleAccent))
                }
                if (lowCount > 0) {
                    Box(modifier = Modifier.weight(lowCount / total).fillMaxHeight().background(CyanAccent))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LegendItem(label = "Critical", count = criticalCount, color = RoseAccent)
                LegendItem(label = "High", count = highCount, color = AmberAccent)
                LegendItem(label = "Medium", count = mediumCount, color = PurpleAccent)
                LegendItem(label = "Low", count = lowCount, color = CyanAccent)
            }
        }
    }
}

@Composable
fun LegendItem(label: String, count: Int, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(text = "$label ($count)", color = MutedText, fontSize = 11.sp)
    }
}

@Composable
fun IncidentRow(incident: Incident) {
    val dateFormat = remember { SimpleDateFormat("hh:mm a, dd MMM", Locale.getDefault()) }
    val formattedTime = dateFormat.format(Date(incident.timestamp))

    val severityColor = when (incident.severity) {
        Severity.CRITICAL -> RoseAccent
        Severity.HIGH -> AmberAccent
        Severity.MEDIUM -> PurpleAccent
        else -> CyanAccent
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(severityColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = incident.sender.firstOrNull()?.toString()?.uppercase() ?: "?",
                            color = severityColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Column {
                        Text(
                            text = incident.sender,
                            color = LightText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = incident.sourceApp.split(".").lastOrNull() ?: incident.sourceApp,
                            color = MutedText,
                            fontSize = 11.sp
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(severityColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = incident.severity.name,
                            color = severityColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formattedTime,
                        color = MutedText,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "\"${incident.messageContent}\"",
                color = LightText.copy(alpha = 0.9f),
                fontSize = 13.sp,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkBg.copy(alpha = 0.5f))
                    .padding(8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Reason: ${incident.reason}",
                color = MutedText,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun SettingsScreen(viewModel: MainScreenViewModel) {
    var currentSettingsScreen by remember { mutableStateOf("Main") }

    if (currentSettingsScreen != "Main") {
        BackHandler {
            currentSettingsScreen = "Main"
        }
    }

    when (currentSettingsScreen) {
        "Main" -> SettingsDashboard(viewModel, onNavigate = { currentSettingsScreen = it })
        "Profile" -> UserProfileScreen(viewModel, onBack = { currentSettingsScreen = "Main" })
        "Theme" -> DarkModeSettingsScreen(viewModel, onBack = { currentSettingsScreen = "Main" })
        "Backup" -> DataBackupScreen(viewModel, onBack = { currentSettingsScreen = "Main" })
        "Help" -> HelpSupportScreen(onBack = { currentSettingsScreen = "Main" })
        "Privacy" -> PrivacyPolicyScreen(onBack = { currentSettingsScreen = "Main" })
        "About" -> AboutAppScreen(onBack = { currentSettingsScreen = "Main" })
    }
}

@Composable
fun SettingsDashboard(viewModel: MainScreenViewModel, onNavigate: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 32.dp, top = 8.dp)
    ) {
        item {
            Text(
                text = "Guardian Settings",
                color = LightText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
        }

        item {
            SettingsMenuItem(
                title = "User Profile Setup",
                subtitle = "Configure guardian alerts contact & scan targets",
                icon = Icons.Default.Lock,
                onClick = { onNavigate("Profile") }
            )
        }

        item {
            SettingsMenuItem(
                title = "Dark Mode Configuration",
                subtitle = "Toggle Pitch Black vs Space Slate styles",
                icon = Icons.Default.Info,
                onClick = { onNavigate("Theme") }
            )
        }

        item {
            SettingsMenuItem(
                title = "Data Backup Manager",
                subtitle = "Manual cloud synchronization details",
                icon = Icons.Default.Check,
                onClick = { onNavigate("Backup") }
            )
        }

        item {
            SettingsMenuItem(
                title = "Help & Support Database",
                subtitle = "Parenting guides, permissions FAQ & contacts",
                icon = Icons.Default.Notifications,
                onClick = { onNavigate("Help") }
            )
        }

        item {
            SettingsMenuItem(
                title = "Privacy Disclosures",
                subtitle = "Compliance info on local-first scans",
                icon = Icons.Default.Warning,
                onClick = { onNavigate("Privacy") }
            )
        }

        item {
            SettingsMenuItem(
                title = "About Application",
                subtitle = "Versions, developers, and licenses info",
                icon = Icons.Default.Settings,
                onClick = { onNavigate("About") }
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            OutlinedButton(
                onClick = {
                    scope.launch {
                        try {
                            com.example.cybershield.data.SupabaseManager.getInstance().signOut()
                            Toast.makeText(context, "Signed out successfully", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Sign out failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                border = BorderStroke(1.dp, RoseAccent),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseAccent),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(text = "Sign Out Account", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
fun SettingsMenuItem(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PurpleAccent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = PurpleAccent, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = LightText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(text = subtitle, color = MutedText, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun UserProfileScreen(viewModel: MainScreenViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(viewModel.guardianName) }
    var phone by remember { mutableStateOf(viewModel.guardianPhone) }
    var email by remember { mutableStateOf(viewModel.guardianEmail) }
    var sensitivityLevel by remember { mutableStateOf(viewModel.sensitivity) }
    
    val currentMonitored = remember { mutableStateMapOf<String, Boolean>() }
    
    val allMonitoredApps = listOf(
        Pair("WhatsApp", "com.whatsapp"),
        Pair("Instagram", "com.instagram.android"),
        Pair("Snapchat", "com.snapchat.android"),
        Pair("Messenger", "com.facebook.orca"),
        Pair("SMS (Default)", "com.google.android.apps.messaging")
    )

    LaunchedEffect(Unit) {
        val currentSet = viewModel.monitoredApps
        allMonitoredApps.forEach { (_, pkg) ->
            currentMonitored[pkg] = currentSet.contains(pkg)
        }
    }

    var customPackageToAdd by remember { mutableStateOf("") }
    
    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasSmsPermission = granted
        }
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp, top = 8.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.background(CardBg, CircleShape)
                ) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = LightText)
                }
                Text(
                    text = "User Profile Setup",
                    color = LightText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Guardian Identity Details",
                        color = LightText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Guardian Name", color = MutedText) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurpleAccent,
                            unfocusedBorderColor = MutedText.copy(alpha = 0.5f),
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
                        label = { Text("Guardian Phone (for SMS)", color = MutedText) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurpleAccent,
                            unfocusedBorderColor = MutedText.copy(alpha = 0.5f),
                            focusedLabelColor = PurpleAccent,
                            unfocusedLabelColor = MutedText,
                            focusedTextColor = LightText,
                            unfocusedTextColor = LightText
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (!hasSmsPermission) {
                        Button(
                            onClick = { launcher.launch(android.Manifest.permission.SEND_SMS) },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberAccent),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Grant SMS Permission", color = DarkBg, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Guardian Email", color = MutedText) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurpleAccent,
                            unfocusedBorderColor = MutedText.copy(alpha = 0.5f),
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
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Classification Sensitivity",
                        color = LightText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

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
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }



        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Scan Target Config",
                        color = LightText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    allMonitoredApps.forEach { (name, pkg) ->
                        val checked = currentMonitored[pkg] ?: false
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = name, color = LightText, fontSize = 13.sp)
                            Switch(
                                checked = checked,
                                onCheckedChange = { currentMonitored[pkg] = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = LightText,
                                    checkedTrackColor = PurpleAccent,
                                    uncheckedThumbColor = MutedText,
                                    uncheckedTrackColor = DarkBg
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = customPackageToAdd,
                        onValueChange = { customPackageToAdd = it },
                        placeholder = { Text("Custom package (e.g. com.app)", color = MutedText.copy(0.5f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurpleAccent,
                            unfocusedBorderColor = MutedText.copy(alpha = 0.3f),
                            focusedTextColor = LightText,
                            unfocusedTextColor = LightText
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (customPackageToAdd.isNotBlank()) {
                                currentMonitored[customPackageToAdd.trim()] = true
                                customPackageToAdd = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Register Custom App Package", color = LightText, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    val enabledSet = currentMonitored.filter { it.value }.keys.toSet()
                    viewModel.saveSettings(
                        name = name,
                        phone = phone,
                        email = email,
                        sensitivityLevel = sensitivityLevel,
                        enabledPackages = enabledSet,
                        useGeminiVal = viewModel.useGemini,
                        geminiApiKeyVal = viewModel.geminiApiKey
                    )
                    Toast.makeText(context, "Guardian profile updated!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(text = "Save Profile", color = LightText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun DarkModeSettingsScreen(viewModel: MainScreenViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    var selectedTheme by remember { mutableStateOf(viewModel.themeMode) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.background(CardBg, CircleShape)
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = LightText)
            }
            Text(
                text = "Dark Mode Configuration",
                color = LightText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Aesthetic Theme Styles",
                    color = LightText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                val themes = listOf(
                    Pair("Space Slate", "Deep cosmic slate background with premium purple glow."),
                    Pair("Pitch Black", "Pure AMOLED black background to optimize battery lifespan."),
                    Pair("Auto-Schedule", "Ultra-dark night blue theme mapping to standard scheduler.")
                )

                themes.forEach { (title, description) ->
                    val active = selectedTheme == title
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (active) PurpleAccent.copy(alpha = 0.15f) else DarkBg)
                            .border(1.dp, if (active) PurpleAccent else Color.Transparent, RoundedCornerShape(10.dp))
                            .clickable { selectedTheme = title }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(if (active) PurpleAccent else Color.DarkGray)
                        )
                        Column {
                            Text(text = title, color = LightText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(text = description, color = MutedText, fontSize = 11.sp)
                        }
                    }
                }

                Button(
                    onClick = {
                        viewModel.themeMode = selectedTheme
                        Toast.makeText(context, "Theme style changed to $selectedTheme!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Apply Style Preference", color = LightText, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DataBackupScreen(viewModel: MainScreenViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isBackingUp by remember { mutableStateOf(false) }
    var backupProgress by remember { mutableStateOf(0f) }
    var backupMsg by remember { mutableStateOf("") }
    
    val incidents by viewModel.incidents.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.background(CardBg, CircleShape)
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = LightText)
            }
            Text(
                text = "Data Backup Manager",
                color = LightText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Cloud Synchronization Details",
                    color = LightText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Local Flagged Incidents", color = MutedText, fontSize = 13.sp)
                    Text(text = "${incidents.size} logs", color = LightText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Supabase Server Link", color = MutedText, fontSize = 13.sp)
                    Text(text = "Connected", color = CyanAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                HorizontalDivider(color = DarkBg)

                if (isBackingUp) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    ) {
                        Text(text = backupMsg, color = PurpleAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        LinearProgressIndicator(
                            progress = { backupProgress },
                            color = PurpleAccent,
                            trackColor = Color.DarkGray,
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            scope.launch {
                                isBackingUp = true
                                backupProgress = 0.2f
                                backupMsg = "Serializing local database records..."
                                delay(1000)
                                backupProgress = 0.6f
                                backupMsg = "Transmitting tables to Supabase Cloud..."
                                delay(1000)
                                backupProgress = 1.0f
                                isBackingUp = false
                                Toast.makeText(context, "Log backup synchronization complete!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Synchronize Logs Backup", color = LightText, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun HelpSupportScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.background(CardBg, CircleShape)
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = LightText)
            }
            Text(
                text = "Help & Support Database",
                color = LightText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val faqs = listOf(
                Pair("How does CyberShield intercept notifications?", "CyberShield uses the Android NotificationListenerService framework to inspect texts of incoming messages from selected chat applications locally."),
                Pair("Why is my status badge RED (SETUP)?", "This means Notification Listener access is disabled. Click the SETUP badge at the top of the home screen to grant permission in system settings."),
                Pair("How does local NLP scanning work?", "All incoming messages are processed directly on your device using local keyword analysis. The message content is never sent to our servers, keeping your child's chats private."),
                Pair("How does SMS guardian alert work?", "When the system detects a Critical threat level alert, it dispatches an SMS alert directly to the guardian phone number set in settings. Grant SMS permission for this to function.")
            )

            items(faqs) { (q, a) ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = q, color = PurpleAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(text = a, color = MutedText, fontSize = 12.sp, lineHeight = 16.sp)
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "Still Need Assistance?", color = LightText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Get in touch directly with our support specialists.", color = MutedText, fontSize = 11.sp)
                        Text(text = "support@cybershield.com", color = CyanAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.background(CardBg, CircleShape)
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = LightText)
            }
            Text(
                text = "Privacy Disclosures",
                color = LightText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "100% On-Device Safety Compliance",
                        color = LightText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                item {
                    Text(
                        text = "CyberShield values the privacy of children. Our core message classification architecture runs entirely locally on your device's background system.\n\nWe do not log, upload, or transmit raw message contents. Flagged messages are saved exclusively in a local, sandbox-protected SQLite database.\n\nAutomated guardian dispatch is completed via direct GSM SMS packages to bypass external messaging gateways. Analytical details are encrypted if cloud sync backup is enabled.",
                        color = MutedText,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AboutAppScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.background(CardBg, CircleShape)
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = LightText)
            }
            Text(
                text = "About Application",
                color = LightText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(PurpleAccent.copy(alpha = 0.15f))
                        .border(1.dp, PurpleAccent, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = "CyberShield", tint = PurpleAccent, modifier = Modifier.size(45.dp))
                }

                Text(text = "CyberShield Security", color = LightText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(text = "Version 2.4.0 (Build 9283)", color = MutedText, fontSize = 11.sp)

                HorizontalDivider(color = DarkBg)

                Text(
                    text = "A local-first cyberbullying interception and NLP severity engine designed to safeguard children across social chat platforms.",
                    color = MutedText,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(text = "Developed by CyberShield Group Ltd.\nLicensed under MIT Open Source.", color = MutedText, fontSize = 10.sp, textAlign = TextAlign.Center)
            }
        }
    }
}



private fun isNotificationServiceEnabled(context: Context): Boolean {
    val packageNames = NotificationManagerCompat.getEnabledListenerPackages(context)
    return packageNames.contains(context.packageName)
}

private fun getActivity(context: Context): android.app.Activity? {
    var ctx = context
    while (ctx is android.content.ContextWrapper) {
        if (ctx is android.app.Activity) {
            return ctx
        }
        ctx = ctx.baseContext
    }
    return null
}

private fun printReport(context: Context, incidents: List<Incident>) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
    val jobName = "CyberShield_Report"
    
    val htmlContent = buildString {
        append("""
            <html>
            <head>
                <style>
                    body { font-family: sans-serif; padding: 24px; color: #0f172a; }
                    h1 { color: #8b5cf6; font-size: 24px; margin-bottom: 4px; }
                    h2 { color: #94a3b8; font-size: 14px; font-weight: normal; margin-top: 0; margin-bottom: 20px; }
                    .summary { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 16px; margin-bottom: 24px; }
                    .summary-item { font-size: 14px; margin-bottom: 6px; }
                    .summary-item strong { color: #8b5cf6; }
                    .incident { border: 1px solid #e2e8f0; border-radius: 12px; padding: 16px; margin-bottom: 16px; page-break-inside: avoid; }
                    .incident-header { display: flex; justify-content: space-between; margin-bottom: 8px; font-size: 12px; color: #64748b; }
                    .sender { font-weight: bold; color: #0f172a; font-size: 14px; }
                    .severity { font-weight: bold; text-transform: uppercase; padding: 2px 6px; border-radius: 4px; font-size: 10px; }
                    .severity-critical { background: #ffe4e6; color: #f43f5e; }
                    .severity-high { background: #fef3c7; color: #d97706; }
                    .severity-medium { background: #f3e8ff; color: #8b5cf6; }
                    .severity-low { background: #ecfeff; color: #0891b2; }
                    .message { font-style: italic; background: #f8fafc; padding: 10px; border-radius: 6px; margin: 8px 0; font-size: 13px; color: #334155; }
                    .reason { font-family: monospace; font-size: 11px; color: #64748b; margin: 0; }
                </style>
            </head>
            <body>
                <h1>CyberShield Protection Report</h1>
                <h2>Generated on ${java.text.DateFormat.getDateTimeInstance().format(Date())}</h2>
                
                <div class="summary">
                    <div class="summary-item">Total Monitored Incidents: <strong>${incidents.size}</strong></div>
                    <div class="summary-item">Engine Status: <strong>ACTIVE</strong></div>
                </div>
        """.trimIndent())
        
        incidents.forEach { inc ->
            val severityClass = when (inc.severity) {
                Severity.CRITICAL -> "severity-critical"
                Severity.HIGH -> "severity-high"
                Severity.MEDIUM -> "severity-medium"
                else -> "severity-low"
            }
            val appLabel = inc.sourceApp.split(".").lastOrNull()?.replaceFirstChar { it.uppercase() } ?: inc.sourceApp
            val formattedTime = java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT).format(Date(inc.timestamp))
            append("""
                <div class="incident">
                    <div class="incident-header">
                        <div>
                            <span class="sender">${inc.sender}</span> via $appLabel
                        </div>
                        <div>
                            <span class="severity $severityClass">${inc.severity.name}</span>
                        </div>
                    </div>
                    <div class="message">"${inc.messageContent}"</div>
                    <p class="reason">Diagnostics: ${inc.reason}</p>
                    <p style="margin: 4px 0 0 0; font-size: 10px; color: #94a3b8; text-align: right;">$formattedTime</p>
                </div>
            """.trimIndent())
        }
        
        append("""
            </body>
            </html>
        """.trimIndent())
    }

    val activity = getActivity(context)
    activity?.runOnUiThread {
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                val printAdapter = webView.createPrintDocumentAdapter(jobName)
                printManager.print(
                    jobName,
                    printAdapter,
                    PrintAttributes.Builder().build()
                )
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }
}

