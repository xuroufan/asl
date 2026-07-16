package com.hackfuture.trading

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class NavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun appShowsMarketScreenByDefault() {
        composeTestRule.setContent {
            AppNavigation()
        }

        // 验证行情页面默认显示
        composeTestRule.onNodeWithText("行情").assertIsDisplayed()
    }
}
