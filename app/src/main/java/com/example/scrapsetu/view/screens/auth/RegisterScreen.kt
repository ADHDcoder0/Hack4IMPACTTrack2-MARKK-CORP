package com.example.scrapsetu.view.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.scrapsetu.vm.AuthState
import com.example.scrapsetu.vm.AuthViewModel

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("supplier") }

    var nameError by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    var locationError by remember { mutableStateOf("") }

    fun validate(): Boolean {
        nameError = if (name.isBlank()) "Name required" else ""
        emailError = if (email.isBlank()) "Email required"
        else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) "Invalid email"
        else ""
        passwordError = if (password.isBlank()) "Password required"
        else if (password.length < 6) "Min 6 characters"
        else ""
        locationError = if (location.isBlank()) "Location required" else ""
        return nameError.isEmpty() && emailError.isEmpty() &&
                passwordError.isEmpty() && locationError.isEmpty()
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) onRegisterSuccess()
    }
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Create Account", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameError = "" },
                label = { Text("Full Name") },
                isError = nameError.isNotEmpty(),
                supportingText = {
                    if (nameError.isNotEmpty()) Text(
                        nameError,
                        color = MaterialTheme.colorScheme.error
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; emailError = "" },
                label = { Text("Email") },
                isError = emailError.isNotEmpty(),
                supportingText = {
                    if (emailError.isNotEmpty()) Text(
                        emailError,
                        color = MaterialTheme.colorScheme.error
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; passwordError = "" },
                label = { Text("Password") },
                isError = passwordError.isNotEmpty(),
                supportingText = {
                    if (passwordError.isNotEmpty()) Text(
                        passwordError,
                        color = MaterialTheme.colorScheme.error
                    )
                },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = location,
                onValueChange = { location = it; locationError = "" },
                label = { Text("Location") },
                isError = locationError.isNotEmpty(),
                supportingText = {
                    if (locationError.isNotEmpty()) Text(
                        locationError,
                        color = MaterialTheme.colorScheme.error
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text("I am a:", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("supplier", "buyer").forEach { role ->
                    FilterChip(
                        selected = selectedRole == role,
                        onClick = { selectedRole = role },
                        label = { Text(role.replaceFirstChar { it.uppercase() }) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            if (authState is AuthState.Error) {
                Text(
                    text = (authState as AuthState.Error).message,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    if (validate()) viewModel.signUp(
                        email,
                        password,
                        name,
                        selectedRole,
                        location
                    )
                },
                enabled = authState !is AuthState.Loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (authState is AuthState.Loading) CircularProgressIndicator(
                    modifier = Modifier.size(
                        20.dp
                    )
                )
                else Text("Register")
            }

            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onNavigateToLogin) {
                Text("Already have an account? Login")
            }
        }
    }
}
