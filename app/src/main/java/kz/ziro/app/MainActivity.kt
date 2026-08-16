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
import kz.ziro.app.ui.screens.CreateTestTypeScreen
import kz.ziro.app.ui.screens.HomeScreen
import kz.ziro.app.ui.screens.LoginScreen
import kz.ziro.app.ui.screens.TestTypesListScreen
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

private enum class Screen { LOGIN, HOME, TEST_TYPES_LIST, CREATE_TEST_TYPE }

@Composable
fun ZiroApp() {
    var screen by remember { mutableStateOf(Screen.LOGIN) }
    var userRole by remember { mutableStateOf("") }

    when (screen) {
        Screen.LOGIN -> LoginScreen(
            onLoginSuccess = { role ->
                userRole = role
                screen = Screen.HOME
            }
        )
        Screen.HOME -> HomeScreen(
            role = userRole,
            onOpenTestTypes = { screen = Screen.TEST_TYPES_LIST },
            onLoggedOut = { screen = Screen.LOGIN }
        )
        Screen.TEST_TYPES_LIST -> TestTypesListScreen(
            onBack = { screen = Screen.HOME },
            onCreateNew = { screen = Screen.CREATE_TEST_TYPE },
            onOpen = { /* details screen later */ }
        )
        Screen.CREATE_TEST_TYPE -> CreateTestTypeScreen(
            onBack = { screen = Screen.TEST_TYPES_LIST },
            onSaved = { screen = Screen.TEST_TYPES_LIST }
        )
    }
}
