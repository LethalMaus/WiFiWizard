package dev.jamescullimore.wifiwizard.ui

import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun ConnectionFailedDialog (
    onConnectViaSettings: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        icon = {
            Icon(Icons.Default.Warning, contentDescription = "Connection Failed")
        },
        title = {
            Text(text = "Connection Failed")
        },
        text = {
            Text(text = "Could not connect to the provided WiFi. Please make sure the password is correct and try again.")
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConnectViaSettings()
                }
            ) {
                Text("Connect via Settings")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text("OK")
            }
        }
    )
}

@Composable
fun ConnectionSuccessDialog (
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        icon = {
            Icon(Icons.Default.Check, contentDescription = "Connection Successful")
        },
        title = {
            Text(text = "Connection Successful")
        },
        text = {
            Text(text = "Your WiFi connection was successful.")
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text("OK")
            }
        }
    )
}

@Composable
fun ConnectionSuggestedDialog (
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        icon = {
            Icon(Icons.Default.List, contentDescription = "WiFI Suggested")
        },
        title = {
            Text(text = "WiFI Suggested")
        },
        text = {
            Text(text = "Your WiFi was suggested, check your settings to confirm if automatic connections will be made.")
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text("OK")
            }
        }
    )
}

@Composable
fun SelectConnectionTypeDialog (
    onConnect: () -> Unit,
    onSuggest: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        icon = {
            Icon(Icons.Default.List, contentDescription = "Select connection type")
        },
        title = {
            Text(text = "Select Connection Type")
        },
        text = {
            Text(text = "Would you like to connect directly or add it as a system suggestion?")
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConnect()
                    onDismissRequest()
                }
            ) {
                Text("Connect")
            }
        },
        dismissButton = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                TextButton(
                    onClick = {
                        onSuggest()
                        onDismissRequest()
                    }
                ) {
                    Text("Suggest")
                }
            } else {
                TextButton(
                    onClick = {
                        onDismissRequest()
                    }
                ) {
                    Text("Cancel")
                }
            }
        }
    )
}