package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.*
import com.example.ui.theme.FixiaTheme
import com.example.ui.viewmodels.FixiaViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: FixiaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.themePreference.collectAsState()

            FixiaTheme(themeMode = themeMode) {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "home"
                ) {
                    composable("home") {
                        HomeScreen(
                            viewModel = viewModel,
                            onNavigateToNewDiagnostic = { navController.navigate("new_diagnostic") },
                            onNavigateToHistory = { navController.navigate("history") },
                            onNavigateToSettings = { navController.navigate("settings") },
                            onNavigateToDetail = { id -> navController.navigate("detail/$id") },
                            onNavigateToTrackedProblems = { navController.navigate("history") },
                            onNavigateToKnowledgeStats = { navController.navigate("knowledge_stats") },
                            onNavigateToLiveAr = { navController.navigate("live_ar") },
                            onNavigateToEmergency = { navController.navigate("emergency") },
                            onNavigateToHouseDigitalTwin = { navController.navigate("house_digital_twin") }
                        )
                    }

                    composable("live_ar") {
                        LiveArDiagnosticScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onNavigateToNewDiagnostic = { navController.navigate("new_diagnostic") },
                            onNavigateToEmergency = { navController.navigate("emergency") }
                        )
                    }

                    composable("emergency") {
                        EmergencyModeScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("house_digital_twin") {
                        HouseDigitalTwinScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onNavigateToDiagnosticDetail = { id -> navController.navigate("detail/$id") }
                        )
                    }

                    composable("new_diagnostic") {
                        NewDiagnosticScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToCamera = { navController.navigate("camera") },
                            onNavigateToAudioRecorder = { navController.navigate("audio_recorder") },
                            onNavigateToSettings = { navController.navigate("settings") },
                            onAnalysisSuccess = { newId ->
                                navController.navigate("detail/$newId") {
                                    popUpTo("home")
                                }
                            }
                        )
                    }

                    composable("camera") {
                        CameraScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable("audio_recorder") {
                        AudioRecorderScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = "detail/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val id = backStackEntry.arguments?.getLong("id") ?: 0L
                        DiagnosticDetailScreen(
                            diagnosticId = id,
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToDiy = { diagnosticId -> navController.navigate("diy/$diagnosticId") },
                            onNavigateToPro = { diagnosticId -> navController.navigate("pro/$diagnosticId") },
                            onNavigateToChat = { diagnosticId -> navController.navigate("chat/$diagnosticId") },
                            onNavigateToTrackedTimeline = { suiviId -> navController.navigate("tracked_problem/$suiviId") }
                        )
                    }

                    composable(
                        route = "tracked_problem/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val suiviId = backStackEntry.arguments?.getLong("id") ?: 0L
                        TrackedProblemTimelineScreen(
                            suiviId = suiviId,
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToDetail = { diagId -> navController.navigate("detail/$diagId") },
                            onNavigateToNewControl = { sId ->
                                viewModel.selectedSuiviId.value = sId
                                navController.navigate("new_diagnostic")
                            }
                        )
                    }

                    composable(
                        route = "chat/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val diagId = backStackEntry.arguments?.getLong("id") ?: 0L
                        ClarificationChatScreen(
                            diagnosticId = diagId,
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable("knowledge_stats") {
                        PersonalKnowledgeStatsScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }


                    composable(
                        route = "diy/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val id = backStackEntry.arguments?.getLong("id") ?: 0L
                        DiyModeScreen(
                            diagnosticId = id,
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = "pro/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val id = backStackEntry.arguments?.getLong("id") ?: 0L
                        ProModeScreen(
                            diagnosticId = id,
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable("history") {
                        HistoryScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToDetail = { id -> navController.navigate("detail/$id") },
                            onNavigateToCompare = { id1, id2 -> navController.navigate("compare/$id1/$id2") }
                        )
                    }

                    composable(
                        route = "compare/{id1}/{id2}",
                        arguments = listOf(
                            navArgument("id1") { type = NavType.LongType },
                            navArgument("id2") { type = NavType.LongType }
                        )
                    ) { backStackEntry ->
                        val id1 = backStackEntry.arguments?.getLong("id1") ?: 0L
                        val id2 = backStackEntry.arguments?.getLong("id2") ?: 0L
                        CompareDiagnosticsScreen(
                            id1 = id1,
                            id2 = id2,
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable("settings") {
                        SettingsScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToAbout = { navController.navigate("about") }
                        )
                    }

                    composable("about") {
                        AboutScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
