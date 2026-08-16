package kz.ziro.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kz.ziro.app.ui.screens.HomeScreen
import kz.ziro.app.ui.screens.LoginScreen
import kz.ziro.app.ui.theme.ZiroTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ZiroTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ZiroApp()
                }
            }
        }
    }
}

@Composable
fun ZiroApp() {
    var userRole by remember { mutableStateOf<String?>(null) }

    if (userRole == null) {
        LoginScreen(onLoginSuccess = { role -> userRole = role })
    } else {
        HomeScreen(role = userRole!!, onLoggedOut = { userRole = null })
    }
}
