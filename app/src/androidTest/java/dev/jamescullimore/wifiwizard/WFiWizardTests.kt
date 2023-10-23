package dev.jamescullimore.wifiwizard

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test

class WFiWizardTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @get:Rule
    var permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.CHANGE_NETWORK_STATE,
        Manifest.permission.CHANGE_WIFI_STATE
    )

    @Test
    fun testManualConnection() {
        composeTestRule.onNodeWithText("Enter manually")
            .performClick()

        composeTestRule.onNodeWithText("Enter the SSID & password")
            .assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("SSID TextField")
            .performTextInput(BuildConfig.SSID)

        composeTestRule.onNodeWithContentDescription("Password TextField")
            .performTextInput(BuildConfig.PASS)

        composeTestRule.onNodeWithText("Connect")
            .performClick()
    }
}