package com.example.scrapsetu.view.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsState()

    var currentStep by remember { mutableStateOf(1) }

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf("supplier") }

    var businessName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var selectedState by remember { mutableStateOf("") }

    var businessType by remember { mutableStateOf("") }
    var monthlyVolume by remember { mutableStateOf("") }
    val selectedCategories = remember { mutableStateListOf<String>() }

    var errorText by remember { mutableStateOf("") }

    val states = listOf(
        "Maharashtra", "Gujarat", "Karnataka", "Tamil Nadu", "Delhi", "Rajasthan",
        "Uttar Pradesh", "Madhya Pradesh", "Telangana", "West Bengal"
    )

    val supplierBusinessTypes = listOf("Trader", "Recycler", "Manufacturer", "Aggregator")
    val buyerBusinessTypes = listOf("Recycler", "Manufacturer", "Processor", "Aggregator")

    val supplierCategories = listOf("Plastic", "Metal", "Textile", "Paper", "Chemical", "E-Waste", "Other")
    val buyerCategories = listOf("Plastic", "Metal", "Textile", "Paper", "Chemical", "E-Waste", "Other")

    fun validateStep1(): Boolean {
        if (name.isBlank()) {
            errorText = "Name is required"
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            errorText = "Enter a valid email"
            return false
        }
        if (password.length < 8) {
            errorText = "Password must be at least 8 characters"
            return false
        }
        if (selectedRole.isBlank()) {
            errorText = "Select a role"
            return false
        }
        errorText = ""
        return true
    }

    fun validateStep2(): Boolean {
        if (businessName.isBlank()) {
            errorText = "Business name is required"
            return false
        }
        if (phone.length != 10 || phone.any { !it.isDigit() }) {
            errorText = "Phone must be exactly 10 digits"
            return false
        }
        if (selectedState.isBlank()) {
            errorText = "Please select state"
            return false
        }
        errorText = ""
        return true
    }

    fun validateStep3(): Boolean {
        if (businessType.isBlank()) {
            errorText = "Select business type"
            return false
        }
        if (selectedCategories.isEmpty()) {
            errorText = "Select at least one category"
            return false
        }
        val volume = monthlyVolume.toIntOrNull()
        if (volume == null || volume <= 0) {
            errorText = if (selectedRole == "supplier") {
                "Monthly volume must be greater than 0"
            } else {
                "Processing capacity must be greater than 0"
            }
            return false
        }
        errorText = ""
        return true
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) onRegisterSuccess()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Create Account",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            StepIndicator(currentStep = currentStep)
            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (currentStep) {
                        1 -> {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it; errorText = "" },
                                label = { Text("Name") },
                                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = fieldColors()
                            )
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it; errorText = "" },
                                label = { Text("Email") },
                                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = fieldColors()
                            )
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it; errorText = "" },
                                label = { Text("Password") },
                                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    TextButton(onClick = { showPassword = !showPassword }) {
                                        Text(if (showPassword) "Hide" else "Show")
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = fieldColors()
                            )

                            Text("Select Role", style = MaterialTheme.typography.titleSmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                FilterChip(
                                    selected = selectedRole == "supplier",
                                    onClick = { selectedRole = "supplier" },
                                    label = { Text("Supplier") },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = selectedRole == "buyer",
                                    onClick = { selectedRole = "buyer" },
                                    label = { Text("Buyer") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        2 -> {
                            OutlinedTextField(
                                value = businessName,
                                onValueChange = { businessName = it; errorText = "" },
                                label = { Text("Business Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = fieldColors()
                            )
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it.filter { ch -> ch.isDigit() }.take(10); errorText = "" },
                                label = { Text("Phone (10 digits)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = fieldColors()
                            )
                            OutlinedTextField(
                                value = city,
                                onValueChange = { city = it; errorText = "" },
                                label = { Text("City") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = fieldColors()
                            )
                            StateDropdown(
                                selectedState = selectedState,
                                states = states,
                                onSelected = {
                                    selectedState = it
                                    errorText = ""
                                }
                            )
                        }

                        3 -> {
                            val businessTypes = if (selectedRole == "supplier") supplierBusinessTypes else buyerBusinessTypes
                            val chips = if (selectedRole == "supplier") supplierCategories else buyerCategories

                            Text(
                                if (selectedRole == "supplier") "Supplier Profile" else "Buyer Profile",
                                style = MaterialTheme.typography.titleSmall
                            )

                            ChoiceChips(
                                title = "Business Type",
                                options = businessTypes,
                                selected = businessType,
                                onSelected = {
                                    businessType = it
                                    errorText = ""
                                }
                            )

                            MultiChoiceChips(
                                title = if (selectedRole == "supplier") "Waste Categories" else "Materials Accepted",
                                options = chips,
                                selected = selectedCategories,
                                onChanged = { errorText = "" }
                            )

                            OutlinedTextField(
                                value = monthlyVolume,
                                onValueChange = {
                                    monthlyVolume = it.filter { ch -> ch.isDigit() }
                                    errorText = ""
                                },
                                label = {
                                    Text(
                                        if (selectedRole == "supplier") {
                                            "Monthly Volume (kg)"
                                        } else {
                                            "Processing Capacity (kg)"
                                        }
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = fieldColors()
                            )
                        }
                    }

                    if (errorText.isNotBlank()) {
                        Text(errorText, color = MaterialTheme.colorScheme.error)
                    }

                    if (authState is AuthState.Error) {
                        Text((authState as AuthState.Error).message, color = MaterialTheme.colorScheme.error)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        if (currentStep > 1) {
                            TextButton(onClick = { currentStep -= 1 }, modifier = Modifier.weight(1f)) {
                                Text("Back")
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        Button(
                            onClick = {
                                when (currentStep) {
                                    1 -> if (validateStep1()) currentStep = 2
                                    2 -> if (validateStep2()) currentStep = 3
                                    3 -> {
                                        if (validateStep3()) {
                                            viewModel.signUp(
                                                email = email,
                                                password = password,
                                                name = name,
                                                role = selectedRole,
                                                location = listOf(city, selectedState).filter { it.isNotBlank() }.joinToString(", "),
                                                businessName = businessName,
                                                phone = phone,
                                                state = selectedState,
                                                businessType = businessType,
                                                wasteCategories = selectedCategories.toList(),
                                                monthlyVolume = monthlyVolume.toIntOrNull() ?: 0
                                            )
                                        }
                                    }
                                }
                            },
                            enabled = authState !is AuthState.Loading,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (authState is AuthState.Loading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Text(if (currentStep == 3) "Submit" else "Next")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Already have an account?")
                TextButton(onClick = onNavigateToLogin) {
                    Text("Login")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StepIndicator(currentStep: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { index ->
            val step = index + 1
            val active = step <= currentStep
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(8.dp)
                    .background(
                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(99.dp)
                    )
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun StateDropdown(
    selectedState: String,
    states: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selectedState,
            onValueChange = {},
            readOnly = true,
            label = { Text("State") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            colors = fieldColors()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            states.forEach { state ->
                val isSelected = selectedState == state
                DropdownMenuItem(
                    text = {
                        Text(
                            text = state,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    colors = MenuDefaults.itemColors(
                        textColor = MaterialTheme.colorScheme.onSurface,
                        leadingIconColor = MaterialTheme.colorScheme.primary,
                        trailingIconColor = MaterialTheme.colorScheme.primary,
                        disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.background(
                        if (isSelected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                        else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    ),
                    onClick = {
                        onSelected(state)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ChoiceChips(
    title: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    Text(title, style = MaterialTheme.typography.titleSmall)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        options.take(3).forEach { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelected(option) },
                label = { Text(option) }
            )
        }
    }
    if (options.size > 3) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            options.drop(3).forEach { option ->
                FilterChip(
                    selected = selected == option,
                    onClick = { onSelected(option) },
                    label = { Text(option) }
                )
            }
        }
    }
}

@Composable
private fun MultiChoiceChips(
    title: String,
    options: List<String>,
    selected: MutableList<String>,
    onChanged: () -> Unit
) {
    Text(title, style = MaterialTheme.typography.titleSmall)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.chunked(3).forEach { rowOptions ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                rowOptions.forEach { option ->
                    val isSelected = selected.contains(option)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (isSelected) selected.remove(option) else selected.add(option)
                            onChanged()
                        },
                        label = { Text(option) }
                    )
                }
            }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color.Transparent,
    unfocusedBorderColor = Color.Transparent,
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
)
