package com.example.mindshift

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mindshift.screens.GlossaryScreen
import com.example.mindshift.ui.MindShiftTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GlossaryUiTest {

    @get:Rule val compose = createComposeRule()

    private fun setContent() {
        compose.setContent {
            MindShiftTheme {
                val nav = rememberNavController()
                NavHost(navController = nav, startDestination = "glossary") {
                    composable("glossary") { GlossaryScreen(nav) }
                    composable("worksheet/{id}") { }
                }
            }
        }
    }

    @Test fun termsAreListedAndCategoryFilterWorks() {
        setContent()
        compose.onNodeWithText("5-4-3-2-1 grounding").assertIsDisplayed()
        compose.onNodeWithTag("cat_CBT").performClick()
        compose.onNodeWithText("Automatic thought").assertIsDisplayed()
        compose.onNodeWithText("Wise Mind").assertDoesNotExist()
        compose.onNodeWithTag("cat_all").performClick()
        compose.onNodeWithText("Wise Mind").assertExists()
    }

    @Test fun searchFiltersToMatchingTerms() {
        setContent()
        compose.onNodeWithTag("glossary_search").performTextInput("dear")
        compose.onNodeWithText("DEAR MAN").assertIsDisplayed()
        compose.onNodeWithText("Radical acceptance").assertDoesNotExist()
    }

    @Test fun expandingShowsDefinitionAndTryItLink() {
        setContent()
        compose.onNodeWithText("Automatic thought").performClick()
        compose.onNodeWithText("Try it → Thought Record").assertIsDisplayed()
    }
}
