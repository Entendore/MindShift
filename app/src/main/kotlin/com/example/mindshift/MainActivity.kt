package com.example.mindshift

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.mindshift.data.AppDatabase
import com.example.mindshift.data.SettingsRepository
import com.example.mindshift.data.TextSize
import com.example.mindshift.data.Worksheets
import com.example.mindshift.reminders.ItemReminderScheduler
import com.example.mindshift.screens.*
import com.example.mindshift.ui.LockOverlay
import com.example.mindshift.ui.MindShiftTheme
import com.example.mindshift.ui.ThemeMode
import com.example.mindshift.widgets.NextUpWidget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private val pendingRoute = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingRoute.value = intent?.getStringExtra("navigate_to")
        val settings = SettingsRepository.get(applicationContext)
        setContent {
            val mode by settings.theme.collectAsState(initial = ThemeMode.SYSTEM)
            val textSize by settings.textSize.collectAsState(initial = TextSize.NORMAL)
            val lockEnabled by remember { settings.lockEnabled.map { it as Boolean? } }
                .collectAsState(initial = null)
            val onboarded by remember { settings.onboarded.map { it as Boolean? } }
                .collectAsState(initial = null)
            val route by pendingRoute.collectAsState()
            val scope = rememberCoroutineScope()
            var unlocked by remember { mutableStateOf(false) }
            var backgrounded by remember { mutableStateOf(false) }

            DisposableEffect(Unit) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_STOP -> backgrounded = true
                        Lifecycle.Event.ON_START ->
                            if (backgrounded) { unlocked = false; backgrounded = false }
                        else -> Unit
                    }
                }
                lifecycle.addObserver(observer)
                onDispose { lifecycle.removeObserver(observer) }
            }

            MindShiftTheme(mode) {
                val base = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(base.density, base.fontScale * textSize.multiplier)
                ) {
                    when {
                        onboarded == null ->
                            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                        onboarded == false ->
                            OnboardingScreen(onDone = { scope.launch { settings.setOnboarded(true) } })
                        else -> {
                            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                                AppRoot(
                                    pendingRoute = route,
                                    onRouteConsumed = { pendingRoute.value = null }
                                )
                                if (lockEnabled == true && !unlocked) {
                                    LockOverlay(
                                        onUnlock = { unlocked = true },
                                        onDisableLock = { scope.launch { settings.setLock(false) } }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingRoute.value = intent.getStringExtra("navigate_to")
    }
}

private data class TabItem(val route: String, val label: String, val icon: ImageVector)

@Composable
fun AppRoot(pendingRoute: String? = null, onRouteConsumed: () -> Unit = {}) {
    val nav = rememberNavController()
    val context = LocalContext.current

    // Refresh schedules for all enabled reminders each launch (idempotent; REPLACE policy)
    LaunchedEffect(Unit) {
        val dao = AppDatabase.get(context).entryDao()
        dao.enabledReminders().forEach { ItemReminderScheduler.schedule(context, it) }
        NextUpWidget().updateAll(context)
    }

    // Deep link from reminder notifications
    LaunchedEffect(pendingRoute) {
        if (pendingRoute != null) {
            val allowed = pendingRoute.startsWith("worksheet/") ||
                pendingRoute.startsWith("glossary") ||
                pendingRoute in setOf(
                    "coping", "breathing", "timer", "diary", "help", "learn",
                    "weekly", "insights", "reminders", "search", "settings",
                    "ladders", "ladder", "leaves"
                )
            if (allowed) nav.navigate(pendingRoute) { launchSingleTop = true }
            onRouteConsumed()
        }
    }

    val tabs = listOf(
        TabItem("home", "Home", Icons.Rounded.Home),
        TabItem("cbt", "CBT", Icons.Rounded.Psychology),
        TabItem("dbt", "DBT", Icons.Rounded.SelfImprovement),
        TabItem("mindfulness", "Calm", Icons.Rounded.Spa),
        TabItem("saved", "Journal", Icons.Rounded.Bookmarks)
    )
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (tabs.any { it.route == currentRoute }) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = nav,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") { HomeScreen(nav) }

            composable("cbt") {
                CategoryScreen(
                    nav = nav,
                    title = "CBT",
                    blurb = "Catch, check, and change the thoughts that drive how you feel.",
                    worksheets = Worksheets.cbt,
                    header = { DistortionsReference() }
                )
            }

            composable("dbt") {
                CategoryScreen(
                    nav = nav,
                    title = "DBT",
                    blurb = "Skills for distress, emotions, and relationships.",
                    worksheets = Worksheets.dbt,
                    header = {
                        Card(Modifier.fillMaxWidth().clickable { nav.navigate("diary") }) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("📔", fontSize = 28.sp)
                                Spacer(Modifier.width(14.dp))
                                Column {
                                    Text("Today's diary card", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "Rate emotions, distress, and skills used",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                )
            }

            composable("mindfulness") {
                CategoryScreen(
                    nav = nav,
                    title = "Mindfulness",
                    blurb = "Return to the present — on purpose, without judgment.",
                    worksheets = Worksheets.mindfulness,
                    header = {
                        MindfulnessTips()
                        Spacer(Modifier.height(10.dp))
                        Card(Modifier.fillMaxWidth().clickable { nav.navigate("breathing") }) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("🌬️", fontSize = 28.sp)
                                Spacer(Modifier.width(14.dp))
                                Column {
                                    Text("Breathing exercises", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "Box, 4-7-8, and focus patterns",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Card(Modifier.fillMaxWidth().clickable { nav.navigate("leaves") }) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("🍃", fontSize = 28.sp)
                                Spacer(Modifier.width(14.dp))
                                Column {
                                    Text("Leaves on a Stream", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "A guided defusion practice",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                )
            }

            composable("saved") { SavedScreen(nav) }
            composable("breathing") { BreathingScreen(onBack = { nav.popBackStack() }) }
            composable("timer") { MeditationTimerScreen(onBack = { nav.popBackStack() }) }
            composable("diary") { DiaryCardScreen(nav) }
            composable("search") { SearchScreen(nav) }
            composable("help") { HelpScreen(onBack = { nav.popBackStack() }) }
            composable("settings") { SettingsScreen(onBack = { nav.popBackStack() }) }
            composable("insights") { InsightsScreen(onBack = { nav.popBackStack() }) }
            composable("coping") { CopingCardsScreen(nav) }
            composable("learn") { LearnScreen(nav) }
            composable("weekly") { WeeklyReflectionScreen(nav) }
            composable("reminders") { RemindersScreen(nav) }
            composable("leaves") { LeavesScreen(onBack = { nav.popBackStack() }) }
            composable("ladders") { ExposureLadderScreen(nav) }
            composable("ladder") { LadderScreen(nav, entryId = null) }
            composable("ladder/{entryId}") { back ->
                LadderScreen(nav, back.arguments?.getString("entryId")?.toLongOrNull())
            }
            composable(
                "glossary?focus={focus}",
                arguments = listOf(navArgument("focus") { defaultValue = ""; nullable = true })
            ) { back ->
                GlossaryScreen(nav, focus = back.arguments?.getString("focus").orEmpty())
            }
            composable("edit/{entryId}") { back ->
                EditScreen(nav, back.arguments?.getString("entryId")?.toLongOrNull())
            }
            composable("worksheet/{id}") { entry ->
                WorksheetScreen(nav = nav, id = entry.arguments?.getString("id").orEmpty())
            }
        }
    }
}
