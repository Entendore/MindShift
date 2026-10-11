package com.example.mindshift

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mindshift.data.AppDatabase
import com.example.mindshift.screens.*
import com.example.mindshift.ui.MindShiftTheme
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScreenSmokeTest {

    @get:Rule val compose = createComposeRule()
    private lateinit var dao: com.example.mindshift.data.WorksheetEntryDao

    @Before
    fun cleanDb() {
        dao = AppDatabase.get(
            ApplicationProvider.getApplicationContext<android.content.Context>()
        ).entryDao()
        runBlocking { dao.deleteAll(); dao.deleteAllMoods() }
    }

    @Test fun breathingPatternSwitchAndStartTogglesToPause() {
        compose.setContent { MindShiftTheme { BreathingScreen(onBack = {}) } }
        compose.onNodeWithText("Box 4-4-4-4").assertIsSelected()
        compose.onNodeWithText("Relax 4-7-8").performClick()
        compose.onNodeWithText("Box 4-4-4-4").assertIsNotSelected()
        compose.onNodeWithText("Start").performClick()
        compose.onNodeWithText("Pause").assertIsDisplayed()
        compose.onNodeWithText("Reset").performClick()
        compose.onNodeWithText("Start").assertIsDisplayed()
    }

    @Test fun ladderFormValidatesEmptyInput() {
        compose.setContent {
            MindShiftTheme {
                val nav = rememberNavController()
                NavHost(nav, "l") { composable("l") { LadderScreen(nav, entryId = null) } }
            }
        }
        compose.onNodeWithText("Start climbing").performClick()
        compose.onNodeWithText("Give the fear a name and at least one step.").assertIsDisplayed()
    }

    @Test fun worksheetSavesEntryToDatabase() {
        compose.setContent {
            MindShiftTheme {
                val nav = rememberNavController()
                NavHost(nav, "ws") { composable("ws") { WorksheetScreen(nav, id = "thought_record") } }
            }
        }
        compose.onNodeWithTag("field_situation").performTextInput("Skipped lunch, felt shaky")
        compose.onNodeWithTag("field_emotions").performTextInput("Anxious 60%")
        compose.onNodeWithTag("save_entry").performClick()
        compose.onNodeWithText("Saved to your journal").assertIsDisplayed()
        runBlocking {
            val saved = dao.entriesOnce().single()
            assertEquals("thought_record", saved.type)
            assertEquals("Skipped lunch, felt shaky", JSONObject(saved.contentJson).getString("situation"))
        }
    }

    @Test fun moodCheckInLogsAndShowsSuggestions() {
        compose.setContent {
            MindShiftTheme {
                val nav = rememberNavController()
                NavHost(nav, "home") { composable("home") { HomeScreen(nav) } }
            }
        }
        compose.onNodeWithText("😄").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Savor it").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Savor it").assertIsDisplayed()
        runBlocking { assertEquals(5, dao.moodsOnce().single().mood) }
    }
}
