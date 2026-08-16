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
import kz.ziro.app.data.TestType
import kz.ziro.app.ui.screens.AnswerSheetScreen
import kz.ziro.app.ui.screens.CaptureAnswerSheetScreen
import kz.ziro.app.ui.screens.CreateTestTypeScreen
import kz.ziro.app.ui.screens.HomeScreen
import kz.ziro.app.ui.screens.LoginScreen
import kz.ziro.app.ui.screens.QrScanScreen
import kz.ziro.app.ui.screens.ScanResultScreen
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

private enum class Screen {
    LOGIN, HOME, TEST_TYPES_LIST, CREATE_TEST_TYPE, ANSWER_SHEET,
    QR_SCAN, SCAN_RESULT, CAPTURE_SHEET
}

@Composable
fun ZiroApp() {
    var screen by remember { mutableStateOf(Screen.LOGIN) }
    var userRole by remember { mutableStateOf("") }
    var selectedTestType by remember { mutableStateOf<TestType?>(null) }
    var lastScanValue by remember { mutableStateOf("") }

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
            onOpenScan = { screen = Screen.QR_SCAN },
            onLoggedOut = { screen = Screen.LOGIN }
        )
        Screen.TEST_TYPES_LIST -> TestTypesListScreen(
            onBack = { screen = Screen.HOME },
            onCreateNew = { screen = Screen.CREATE_TEST_TYPE },
            onOpen = { tt ->
                selectedTestType = tt
                screen = Screen.ANSWER_SHEET
            }
        )
        Screen.CREATE_TEST_TYPE -> CreateTestTypeScreen(
            onBack = { screen = Screen.TEST_TYPES_LIST },
            onSaved = { screen = Screen.TEST_TYPES_LIST }
        )
        Screen.ANSWER_SHEET -> selectedTestType?.let { tt ->
            AnswerSheetScreen(
                testType = tt,
                onBack = { screen = Screen.TEST_TYPES_LIST },
                onScanSheet = { screen = Screen.CAPTURE_SHEET }
            )
        }
        Screen.CAPTURE_SHEET -> selectedTestType?.let { tt ->
            CaptureAnswerSheetScreen(
                testType = tt,
                onBack = { screen = Screen.ANSWER_SHEET }
            )
        }
        Screen.QR_SCAN -> QrScanScreen(
            onBack = { screen = Screen.HOME },
            onScanned = { value ->
                lastScanValue = value
                screen = Screen.SCAN_RESULT
            }
        )
        Screen.SCAN_RESULT -> ScanResultScreen(
            rawValue = lastScanValue,
            onScanAgain = { screen = Screen.QR_SCAN },
            onBack = { screen = Screen.HOME }
        )
    }
}
