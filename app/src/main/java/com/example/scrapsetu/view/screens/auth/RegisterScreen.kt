package com.example.scrapsetu.view.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
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
    var showPassword by remember { mutableStateOf(false) }
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(36.dp))

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 8.dp,
                    modifier = Modifier.size(90.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "+",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Create Account",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Join the ScrapSetu ecosystem",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it; nameError = "" },
                            label = { Text("Full Name") },
                            isError = nameError.isNotEmpty(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = "Name") },
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (nameError.isNotEmpty()) {
                            Text(nameError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                        }

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it; emailError = "" },
                            label = { Text("Email Address") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            isError = emailError.isNotEmpty(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Filled.Email, contentDescription = "Email") },
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (emailError.isNotEmpty()) {
                            Text(emailError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                        }

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it; passwordError = "" },
                            label = { Text("Password") },
                            isError = passwordError.isNotEmpty(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = "Password") },
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = "Toggle password"
                                    )
                                }
                            },
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (passwordError.isNotEmpty()) {
                            Text(passwordError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                        }

                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it; locationError = "" },
                            label = { Text("Location") },
                            isError = locationError.isNotEmpty(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Filled.Place, contentDescription = "Location") },
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (locationError.isNotEmpty()) {
                            Text(locationError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                        }

                        Text(
                            text = "I am a",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FilterChip(
                                selected = selectedRole == "supplier",
                                onClick = { selectedRole = "supplier" },
                                label = { Text("Supplier") },
                                leadingIcon = { Icon(Icons.Filled.Business, contentDescription = null) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedRole == "buyer",
                                onClick = { selectedRole = "buyer" },
                                label = { Text("Buyer") },
                                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (authState is AuthState.Error) {
                            Text(
                                text = (authState as AuthState.Error).message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp),
                            shape = RoundedCornerShape(30.dp)
                        ) {
                            if (authState is AuthState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Create Account", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Already have an account?",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.82f)
                    )
                    TextButton(onClick = onNavigateToLogin) {
                        Text("Login")
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Box(
                    modifier = Modifier
                        .width(86.dp)
                        .height(4.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(2.dp)
                        )
                )

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
