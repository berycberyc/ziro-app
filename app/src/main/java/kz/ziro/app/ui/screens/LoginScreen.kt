package kz.ziro.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kz.ziro.app.data.AuthRepository
import kz.ziro.app.data.LoginResult

@Composable
fun LoginScreen(onLoginSuccess: (role: String) -> Unit) {
    val authRepository = remember { AuthRepository() }
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun handleLogin() {
        errorMessage = null
        loading = true
        scope.launch {
            when (val result = authRepository.login(email.trim(), password)) {
                is LoginResult.Success -> {
                    loading = false
                    onLoginSuccess(result.role)
                }
                is LoginResult.NotAllowed -> {
                    loading = false
                    errorMessage = "Бұл қосымшаға тек мұғалім/әкімші кіре алады."
                }
                is LoginResult.Error -> {
                    loading = false
                    errorMessage = "Қате шықты. Email/құпия сөзді тексеріңіз."
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Ziro", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            "Мұғалім / Әкімші кіру",
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Құпия сөз") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        )

        errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
        }

        Button(
            onClick = { handleLogin() },
            enabled = !loading && email.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                Text("Кіру")
            }
        }
    }
}
