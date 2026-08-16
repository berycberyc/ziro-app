package kz.ziro.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.ziro.app.data.Registration
import kz.ziro.app.data.RegistrationRepository
import kz.ziro.app.data.TestSession
import kz.ziro.app.data.TestType
import kz.ziro.app.data.TestTypeRepository
import kz.ziro.app.omr.SheetQrData
import kz.ziro.app.ui.screens.AnswerSheetScreen
import kz.ziro.app.ui.screens.CaptureAnswerSheetScreen
import kz.ziro.app.ui.screens.CreateTestTypeScreen
import kz.ziro.app.ui.screens.GenerateSheetsScreen
import kz.ziro.app.ui.screens.HomeScreen
import kz.ziro.app.ui.screens.LoginScreen
import kz.ziro.app.ui.screens.QrScanScreen
import kz.ziro.app.ui.screens.ScanResultScreen
import kz.ziro.app.ui.screens.SessionPickerScreen
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
    QR_SCAN, SCAN_RESULT, CAPTURE_SHEET, SESSION_PICKER, GENERATE_SHEETS
}

@Composable
fun ZiroApp() {
    var screen by remember { mutableStateOf(Screen.LOGIN) }
    var userRole by remember { mutableStateOf("") }
    var selectedTestType by remember { mutableStateOf<TestType?>(null) }
    var selectedRegistration by remember { mutableStateOf<Registration?>(null) }
    var selectedSession by remember { mutableStateOf<TestSession?>(null) }
    var lastScanValue by remember { mutableStateOf("") }
    var scanLookupStatus by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val registrationRepo = remember { RegistrationRepository() }
    val testTypeRepo = remember { TestTypeRepository() }

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
            onOpenGenerateSheets = { screen = Screen.SESSION_PICKER },
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
                onScanSheet = { screen = Screen.QR_SCAN }
            )
        }
        Screen.CAPTURE_SHEET -> selectedTestType?.let { tt ->
            CaptureAnswerSheetScreen(
                testType = tt,
                registration = selectedRegistration,
                onBack = { screen = Screen.HOME }
            )
        }
        Screen.QR_SCAN -> QrScanScreen(
            onBack = { screen = Screen.HOME },
            onScanned = { value ->
                lastScanValue = value
                val regId = SheetQrData.decode(value)
                if (regId == null) {
                    screen = Screen.SCAN_RESULT
                } else {
                    scanLookupStatus = "Тіркеу деректері ізделуде..."
                    scope.launch {
                        val registration = registrationRepo.getById(regId)
                        if (registration == null) {
                            scanLookupStatus = "Тіркеу табылмады (ID: $regId)"
                            screen = Screen.SCAN_RESULT
                            return@launch
                        }
                        val testType = testTypeRepo.getById(registration.test_type_id)
                        if (testType == null) {
                            scanLookupStatus = "Тест түрі табылмады"
                            screen = Screen.SCAN_RESULT
                            return@launch
                        }
                        selectedRegistration = registration
                        selectedTestType = testType
                        screen = Screen.CAPTURE_SHEET
                    }
                }
            }
        )
        Screen.SCAN_RESULT -> ScanResultScreen(
            rawValue = if (scanLookupStatus.isNotEmpty()) "$lastScanValue\n\n$scanLookupStatus" else lastScanValue,
            onScanAgain = {
                scanLookupStatus = ""
                screen = Screen.QR_SCAN
            },
            onBack = { screen = Screen.HOME }
        )
        Screen.SESSION_PICKER -> SessionPickerScreen(
            onBack = { screen = Screen.HOME },
            onSelect = { session ->
                selectedSession = session
                screen = Screen.GENERATE_SHEETS
            }
        )
        Screen.GENERATE_SHEETS -> selectedSession?.let { s ->
            GenerateSheetsScreen(
                session = s,
                onBack = { screen = Screen.SESSION_PICKER }
            )
        }
    }
}
