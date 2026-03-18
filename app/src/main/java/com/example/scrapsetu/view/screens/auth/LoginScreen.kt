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
fun LoginScreen(
    onBuyerLogin: () -> Unit,
    onSupplierLogin: () -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val userRole by viewModel.userRole.collectAsState()
    val authState by viewModel.authState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }

    fun validate(): Boolean {
        emailError = if (email.isBlank()) "Email required"
        else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) "Invalid email"
        else ""
        passwordError = if (password.isBlank()) "Password required"
        else if (password.length < 6) "Min 6 characters"
        else ""
        return emailError.isEmpty() && passwordError.isEmpty()
    }
    LaunchedEffect(authState) {
        if (authState is AuthState.Success) viewModel.loadUserRole()
    }

    LaunchedEffect(userRole) {
        userRole?.let { role ->
            if (role == "supplier") onSupplierLogin()
            else onBuyerLogin()
        }
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
            Text(
                "ScrapSetu",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Transforming Waste into Value",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )

                Spacer(modifier = Modifier.height(32.dp))

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
                Spacer(modifier = Modifier.height(24.dp))

                if (authState is AuthState.Error) {
                    Text(
                        text = (authState as AuthState.Error).message,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Button(
                    onClick = { if (validate()) viewModel.signIn(email, password) },
                    enabled = authState !is AuthState.Loading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (authState is AuthState.Loading) CircularProgressIndicator(
                        modifier = Modifier.size(
                            20.dp
                        )
                    )
                    else Text("Login")
                }

                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onNavigateToRegister) {
                    Text("Don't have an account? Register")
                }
            }
        }
    }
