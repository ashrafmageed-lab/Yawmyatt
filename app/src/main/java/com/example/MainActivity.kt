package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.ui.components.NavigationTab
import com.example.ui.components.YawmiyatiBottomNav
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DayScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.NewJournalEntryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.YawmiyatiViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: YawmiyatiViewModel by viewModels()
    private var triggerOpenNewEntry by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (intent?.getBooleanExtra("open_new_entry", false) == true) {
            triggerOpenNewEntry = true
        }

        setContent {
            MyApplicationTheme {
                val fontScale by viewModel.fontScale.collectAsState()
                val currentDensity = androidx.compose.ui.platform.LocalDensity.current
                val customDensity = androidx.compose.ui.unit.Density(
                    density = currentDensity.density,
                    fontScale = currentDensity.fontScale * fontScale
                )

                CompositionLocalProvider(
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                    androidx.compose.ui.platform.LocalDensity provides customDensity
                ) {
                    MainJournalApp(
                        viewModel = viewModel,
                        initialOpenNewEntry = triggerOpenNewEntry,
                        onConsumeOpenNewEntry = { triggerOpenNewEntry = false }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("open_new_entry", false)) {
            triggerOpenNewEntry = true
        }
    }
}

@Composable
fun MainJournalApp(
    viewModel: YawmiyatiViewModel,
    initialOpenNewEntry: Boolean = false,
    onConsumeOpenNewEntry: () -> Unit = {}
) {
    var currentTab by remember { mutableStateOf(NavigationTab.DASHBOARD) }
    var isWritingNewEntry by remember { mutableStateOf(initialOpenNewEntry) }

    LaunchedEffect(initialOpenNewEntry) {
        if (initialOpenNewEntry) {
            isWritingNewEntry = true
            onConsumeOpenNewEntry()
        }
    }

    if (isWritingNewEntry) {
        BackHandler { isWritingNewEntry = false }
        NewJournalEntryScreen(
            viewModel = viewModel,
            onDismiss = { isWritingNewEntry = false }
        )
    } else {
        // If on another tab, back press returns to Dashboard
        BackHandler(enabled = currentTab != NavigationTab.DASHBOARD) {
            currentTab = NavigationTab.DASHBOARD
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                YawmiyatiBottomNav(
                    currentTab = currentTab,
                    onTabSelected = { currentTab = it }
                )
            },
            floatingActionButton = {
                if (currentTab == NavigationTab.DASHBOARD || currentTab == NavigationTab.DAY) {
                    FloatingActionButton(
                        onClick = { isWritingNewEntry = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("fab_write_new_entry")
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = "تدوين يومية جديدة",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    NavigationTab.DASHBOARD -> DashboardScreen(
                        viewModel = viewModel,
                        onOpenNewJournalEntry = { isWritingNewEntry = true },
                        onNavigateToDay = { currentTab = NavigationTab.DAY },
                        onNavigateToLibrary = { currentTab = NavigationTab.LIBRARY },
                        onNavigateToHistory = { currentTab = NavigationTab.HISTORY },
                        onNavigateToSettings = { currentTab = NavigationTab.SETTINGS }
                    )
                    NavigationTab.DAY -> DayScreen(viewModel = viewModel)
                    NavigationTab.LIBRARY -> LibraryScreen(viewModel = viewModel)
                    NavigationTab.HISTORY -> HistoryScreen(
                        viewModel = viewModel,
                        onNavigateToDayWithDate = { date ->
                            viewModel.setSelectedDate(date)
                            currentTab = NavigationTab.DAY
                        }
                    )
                    NavigationTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
