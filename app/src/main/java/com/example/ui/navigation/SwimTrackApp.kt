package com.example.ui.navigation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.domain.model.AppLanguage
import com.example.domain.model.Stroke
import com.example.domain.model.SwimDistance
import com.example.domain.model.ThemePreference
import com.example.ui.SwimTrackViewModel
import com.example.ui.components.NewPbCelebrationDialog
import com.example.ui.screens.AddRecordScreen
import com.example.ui.screens.CompetitionDetailsScreen
import com.example.ui.screens.CompetitionsScreen
import com.example.ui.screens.EventDetailsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.RecordsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatisticsScreen
import com.example.ui.theme.SwimTrackTheme

private data class BottomNavDestination(
    val route: String,
    val labelEn: String,
    val labelAr: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    fun localizedLabel(language: AppLanguage): String =
        if (language == AppLanguage.ARABIC) labelAr else labelEn
}

private val mainDestinations = listOf(
    BottomNavDestination(
        route = SwimRoutes.HOME,
        labelEn = "Home",
        labelAr = "الرئيسية",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_item_home"
    ),
    BottomNavDestination(
        route = SwimRoutes.RECORDS,
        labelEn = "Records",
        labelAr = "السجلات",
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History,
        testTag = "nav_item_records"
    ),
    BottomNavDestination(
        route = SwimRoutes.ADD_RECORD,
        labelEn = "Add",
        labelAr = "إضافة",
        selectedIcon = Icons.Filled.AddCircle,
        unselectedIcon = Icons.Outlined.AddCircleOutline,
        testTag = "nav_item_add"
    ),
    BottomNavDestination(
        route = SwimRoutes.COMPETITIONS,
        labelEn = "Competitions",
        labelAr = "البطولات",
        selectedIcon = Icons.Filled.EmojiEvents,
        unselectedIcon = Icons.Outlined.EmojiEvents,
        testTag = "nav_item_competitions"
    ),
    BottomNavDestination(
        route = SwimRoutes.STATISTICS,
        labelEn = "Statistics",
        labelAr = "الإحصائيات",
        selectedIcon = Icons.Filled.Analytics,
        unselectedIcon = Icons.Outlined.Analytics,
        testTag = "nav_item_statistics"
    )
)

@Composable
fun SwimTrackApp(
    viewModel: SwimTrackViewModel,
    navController: NavHostController = rememberNavController()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val allRecords by viewModel.allRecords.collectAsStateWithLifecycle()
    val filteredRecords by viewModel.filteredRecords.collectAsStateWithLifecycle()
    val recordsFilter by viewModel.recordsFilter.collectAsStateWithLifecycle()
    val personalBests by viewModel.personalBests.collectAsStateWithLifecycle()
    val competitions by viewModel.competitionsWithResults.collectAsStateWithLifecycle()
    val statsFilter by viewModel.statsFilter.collectAsStateWithLifecycle()
    val currentEventStatistics by viewModel.currentEventStatistics.collectAsStateWithLifecycle()
    val newPbCelebration by viewModel.newPbCelebration.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val isDarkTheme = when (settings.themePreference) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbarMessage()
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val navigateToTopLevel: (String) -> Unit = { targetRoute ->
        if (targetRoute == SwimRoutes.HOME) {
            val popped = navController.popBackStack(SwimRoutes.HOME, inclusive = false)
            if (!popped) {
                navController.navigate(SwimRoutes.HOME) {
                    launchSingleTop = true
                }
            }
        } else {
            navController.navigate(targetRoute) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = false
                }
                launchSingleTop = true
                restoreState = false
            }
        }
    }

    val configuration = LocalConfiguration.current
    val useNavigationRail = configuration.screenWidthDp >= 600

    SwimTrackTheme(
        darkTheme = isDarkTheme,
        language = settings.language
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.systemBars,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.testTag("bottom_navigation_bar")
                    ) {
                        mainDestinations.forEach { dest ->
                            val isSelected = currentRoute?.startsWith(dest.route) == true
                            val labelText = dest.localizedLabel(settings.language)
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { navigateToTopLevel(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = labelText
                                    )
                                },
                                label = {
                                    Text(
                                        text = labelText,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.5.sp,
                                            letterSpacing = if (settings.language.isRtl) 0.sp else (-0.25).sp
                                        ),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Visible
                                    )
                                },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.testTag("side_navigation_rail")
                    ) {
                        mainDestinations.forEach { dest ->
                            val isSelected = currentRoute?.startsWith(dest.route) == true
                            val labelText = dest.localizedLabel(settings.language)
                            NavigationRailItem(
                                selected = isSelected,
                                onClick = { navigateToTopLevel(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = labelText
                                    )
                                },
                                label = { Text(labelText) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = SwimRoutes.HOME,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(SwimRoutes.HOME) {
                        HomeScreen(
                            allRecords = allRecords,
                            personalBests = personalBests,
                            competitions = competitions,
                            settings = settings,
                            onQuickAddClick = { navigateToTopLevel(SwimRoutes.ADD_RECORD) },
                            onViewAllRecordsClick = { navigateToTopLevel(SwimRoutes.RECORDS) },
                            onEventClick = { distance, stroke ->
                                navController.navigate(SwimRoutes.eventDetailsRoute(distance, stroke))
                            },
                            onEditRecordClick = { recordId ->
                                navController.navigate(SwimRoutes.editRecordRoute(recordId))
                            },
                            onDeleteRecordClick = { recordId ->
                                viewModel.deleteRecord(recordId)
                            },
                            onSettingsClick = {
                                navController.navigate(SwimRoutes.SETTINGS)
                            },
                            onLoadDemoDataClick = {
                                viewModel.loadDemoData()
                            }
                        )
                    }

                    composable(SwimRoutes.RECORDS) {
                        RecordsScreen(
                            records = filteredRecords,
                            totalUnfilteredCount = allRecords.size,
                            filterState = recordsFilter,
                            settings = settings,
                            onStrokeFilterChange = viewModel::updateRecordsStrokeFilter,
                            onDistanceFilterChange = viewModel::updateRecordsDistanceFilter,
                            onSessionFilterChange = viewModel::updateRecordsSessionFilter,
                            onPoolFilterChange = viewModel::updateRecordsPoolFilter,
                            onDateRangeChange = viewModel::updateRecordsDateRange,
                            onSearchQueryChange = viewModel::updateRecordsSearchQuery,
                            onClearFilters = viewModel::clearRecordsFilters,
                            onAddRecordClick = { navigateToTopLevel(SwimRoutes.ADD_RECORD) },
                            onOpenEventDetails = { distance, stroke ->
                                navController.navigate(SwimRoutes.eventDetailsRoute(distance, stroke))
                            },
                            onEditRecordClick = { recordId ->
                                navController.navigate(SwimRoutes.editRecordRoute(recordId))
                            },
                            onDeleteRecordClick = { recordId ->
                                viewModel.deleteRecord(recordId)
                            }
                        )
                    }

                    composable(
                        route = SwimRoutes.EDIT_RECORD,
                        arguments = listOf(
                            navArgument("recordId") {
                                type = NavType.LongType
                                defaultValue = -1L
                            }
                        )
                    ) { backStackEntry ->
                        val recordId = backStackEntry.arguments?.getLong("recordId") ?: -1L
                        val existingRecord = remember(allRecords, recordId) {
                            if (recordId > 0L) allRecords.firstOrNull { it.id == recordId } else null
                        }

                        AddRecordScreen(
                            existingRecord = existingRecord,
                            competitions = competitions,
                            settings = settings,
                            onSaveRecord = { record ->
                                viewModel.saveRecord(record) {
                                    if (recordId > 0L) {
                                        navController.popBackStack()
                                    } else {
                                        navigateToTopLevel(SwimRoutes.RECORDS)
                                    }
                                }
                            },
                            onCancel = {
                                if (!navController.popBackStack()) {
                                    navigateToTopLevel(SwimRoutes.HOME)
                                }
                            }
                        )
                    }

                    composable(SwimRoutes.COMPETITIONS) {
                        CompetitionsScreen(
                            competitions = competitions,
                            onOpenCompetitionDetails = { compId ->
                                navController.navigate(SwimRoutes.competitionDetailsRoute(compId))
                            },
                            onSaveCompetition = { comp, onSaved ->
                                viewModel.saveCompetition(comp, onSaved)
                            },
                            onDeleteCompetition = { compId ->
                                viewModel.deleteCompetition(compId)
                            }
                        )
                    }

                    composable(
                        route = SwimRoutes.COMPETITION_DETAILS,
                        arguments = listOf(
                            navArgument("competitionId") { type = NavType.LongType }
                        )
                    ) { backStackEntry ->
                        val compId = backStackEntry.arguments?.getLong("competitionId") ?: -1L
                        val compWithResults = remember(competitions, compId) {
                            competitions.firstOrNull { it.competition.id == compId }
                        }
                        CompetitionDetailsScreen(
                            competitionWithResults = compWithResults,
                            settings = settings,
                            onBack = { navController.popBackStack() },
                            onSaveResult = { result, compDate ->
                                viewModel.saveCompetitionResult(result, compDate)
                            },
                            onDeleteResult = { result ->
                                viewModel.deleteCompetitionResult(result)
                            },
                            onOpenEventDetails = { distance, stroke ->
                                navController.navigate(SwimRoutes.eventDetailsRoute(distance, stroke))
                            }
                        )
                    }

                    composable(SwimRoutes.STATISTICS) {
                        StatisticsScreen(
                            allRecords = allRecords,
                            personalBests = personalBests,
                            competitions = competitions,
                            filterState = statsFilter,
                            eventStatistics = currentEventStatistics,
                            settings = settings,
                            onSelectEvent = viewModel::selectStatsEvent,
                            onSessionFilterChange = viewModel::updateStatsSessionFilter,
                            onPoolFilterChange = viewModel::updateStatsPoolFilter,
                            onChartRangeChange = viewModel::updateStatsChartRange,
                            onOpenEventDetails = { distance, stroke ->
                                navController.navigate(SwimRoutes.eventDetailsRoute(distance, stroke))
                            },
                            onAddRecordClick = { navigateToTopLevel(SwimRoutes.ADD_RECORD) }
                        )
                    }

                    composable(
                        route = SwimRoutes.EVENT_DETAILS,
                        arguments = listOf(
                            navArgument("distance") { type = NavType.StringType },
                            navArgument("stroke") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val distStr = backStackEntry.arguments?.getString("distance") ?: SwimDistance.D50M.name
                        val strokeStr = backStackEntry.arguments?.getString("stroke") ?: Stroke.FREESTYLE.name
                        val distance = SwimDistance.fromString(distStr)
                        val stroke = Stroke.fromString(strokeStr)

                        EventDetailsScreen(
                            distance = distance,
                            stroke = stroke,
                            allRecords = allRecords,
                            settings = settings,
                            onBack = { navController.popBackStack() },
                            onEditRecordClick = { recordId ->
                                navController.navigate(SwimRoutes.editRecordRoute(recordId))
                            },
                            onDeleteRecordClick = { recordId ->
                                viewModel.deleteRecord(recordId)
                            }
                        )
                    }

                    composable(SwimRoutes.SETTINGS) {
                        SettingsScreen(
                            settings = settings,
                            onBack = { navController.popBackStack() },
                            onUpdateLanguage = viewModel::updateLanguage,
                            onUpdateTheme = viewModel::updateThemePreference,
                            onUpdateTimeFormat = viewModel::updateTimeFormatPreference,
                            onUpdateDefaultPool = viewModel::updateDefaultPoolLength,
                            onUpdateDefaultSession = viewModel::updateDefaultSessionType,
                            onUpdateCompetitionOnlyPb = viewModel::updateCompetitionOnlyPb,
                            onExportJsonSuspend = { viewModel.exportJsonString() },
                            onImportJson = viewModel::importJsonString,
                            onLoadDemoData = viewModel::loadDemoData,
                            onDeleteAllData = viewModel::deleteAllData,
                            onShowMessage = viewModel::showMessage
                        )
                    }
                }
            }
        }

        newPbCelebration?.let { celebration ->
            NewPbCelebrationDialog(
                celebration = celebration,
                timeFormat = settings.timeFormatPreference,
                onDismiss = viewModel::dismissPbCelebration
            )
        }
    }
}
