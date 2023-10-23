package dev.jamescullimore.wifiwizard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.jamescullimore.wifiwizard.ui.theme.WiFiWizardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WiFiWizardTheme {
                WiFiWizardApp()
            }
        }
    }
}