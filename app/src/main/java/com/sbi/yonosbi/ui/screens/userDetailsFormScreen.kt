package com.sbi.yonosbi.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sbi.yonosbi.R
import com.sbi.yonosbi.services.UserApiService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailsFormScreen(
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val horizontalPadding = if (screenWidth.value > 600) 64.dp else 16.dp
    val buttonHeight = (screenWidth.value * 0.12f).coerceIn(48f, 64f).dp
    val buttonWidth = screenWidth.value * 0.5f

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val userApiService = remember(context.applicationContext) {
        UserApiService(context.applicationContext)
    }

    // Form State
    var fullName by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var mobileNumber by remember { mutableStateOf("") }
    var emailAddress by remember { mutableStateOf("") }
    var totalLimit by remember { mutableStateOf("") }
    var availableLimit by remember { mutableStateOf("") }
    var cardholderName by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // DatePicker for DOB
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis <= System.currentTimeMillis()
            }
        }
    )

    // Validation State
    var fullNameError by remember { mutableStateOf<String?>(null) }
    var dobError by remember { mutableStateOf<String?>(null) }
    var mobileError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var totalLimitError by remember { mutableStateOf<String?>(null) }
    var availableLimitError by remember { mutableStateOf<String?>(null) }
    var cardholderError by remember { mutableStateOf<String?>(null) }
    var cardNumberError by remember { mutableStateOf<String?>(null) }
    var expiryDateError by remember { mutableStateOf<String?>(null) }
    var cvvError by remember { mutableStateOf<String?>(null) }

    fun validateForm(): Boolean {
        var isValid = true

        fullNameError = if (fullName.isBlank()) {
            isValid = false; "Full Name is required"
        } else null

        dobError = if (dateOfBirth.isBlank()) {
            isValid = false; "Date of Birth is required"
        } else null

        mobileError = if (!mobileNumber.matches(Regex("""\d{10}"""))) {
            isValid = false; "Invalid Mobile Number"
        } else null

        emailError = if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches()) {
            isValid = false; "Invalid Email Address"
        } else null

        totalLimitError = if (totalLimit.toLongOrNull() == null) {
            isValid = false; "Enter valid number"
        } else null

        availableLimitError = if (availableLimit.toLongOrNull() == null) {
            isValid = false; "Enter valid number"
        } else null

        cardholderError = if (cardholderName.isBlank()) {
            isValid = false; "Card Holder Name required"
        } else null

        cardNumberError = if (!cardNumber.matches(Regex("""\d{16}"""))) {
            isValid = false; "Card Number must be 16 digits"
        } else null

        expiryDateError = when {
            expiryDate.isBlank() -> {
                isValid = false; "Expiry Date required"
            }
            !expiryDate.matches(Regex("""\d{2}/\d{2}""")) -> {
                isValid = false; "Format must be MM/YY"
            }
            else -> {
                val month = expiryDate.take(2).toIntOrNull()
                if (month == null || month !in 1..12) {
                    isValid = false; "Invalid month"
                } else null
            }
        }

        cvvError = if (!cvv.matches(Regex("""\d{3}"""))) {
            isValid = false; "CVV must be 3 digits"
        } else null

        return isValid
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF1166DD)
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(20.dp))
                    .padding(horizontal = horizontalPadding, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Logo",
                    modifier = Modifier.height(90.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                FormField("Full Name", fullName, { fullName = it }, "Enter your Name", error = fullNameError)

                // Date of Birth (DatePicker)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "Date of Birth",
                        fontWeight = FontWeight.SemiBold
                    )

                    OutlinedTextField(
                        value = dateOfBirth,
                        onValueChange = {},
                        placeholder = { Text("Select Date") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        isError = dobError != null,
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "Select Date"
                                )
                            }
                        }
                    )

                    dobError?.let {
                        Text(it, color = Color.Red, fontSize = 12.sp)
                    }
                }

                FormField("Mobile Number", mobileNumber, { mobileNumber = it }, "Mobile Number", KeyboardType.Phone, error = mobileError)
                FormField("Email Address", emailAddress, { emailAddress = it }, "Email Address", KeyboardType.Email, error = emailError)
                FormField("Total Limit", totalLimit, { totalLimit = it }, "Total Limit", KeyboardType.Number, error = totalLimitError)
                FormField("Available Limit", availableLimit, { availableLimit = it }, "Available Limit", KeyboardType.Number, error = availableLimitError)
                FormField("Card Holder Name", cardholderName, { cardholderName = it }, "Card Holder Name", error = cardholderError)
                FormField("Card Number", cardNumber, { cardNumber = it }, "Card Number", KeyboardType.Number, error = cardNumberError)

                // Expiry Date MM/YY with enforced month <= 12
                FormField(
                    "Expiry Date (MM/YY)",
                    expiryDate,
                    { input ->
                        // Allow only digits and slash
                        var filtered = input.filter { it.isDigit() || it == '/' }

                        // Insert slash automatically after month
                        if (filtered.length > 2 && !filtered.contains("/")) {
                            filtered = filtered.take(2) + "/" + filtered.drop(2)
                        }

                        // Limit total length to 5 characters MM/YY
                        if (filtered.length > 5) filtered = filtered.take(5)

                        // Enforce month <= 12
                        val month = filtered.take(2).toIntOrNull()
                        filtered = if (month != null && month > 12) {
                            "12" + filtered.drop(2)
                        } else filtered

                        expiryDate = filtered
                    },
                    "MM/YY",
                    KeyboardType.Number,
                    error = expiryDateError
                )

                FormField("CVV", cvv, { cvv = it }, "Card CVV", KeyboardType.Number, true, error = cvvError)

                Spacer(modifier = Modifier.height(32.dp))

                // Submit Button with Coroutine + API
                Button(
                    onClick = {
                        if (!validateForm()) return@Button

                        isLoading = true
                        coroutineScope.launch {
                            try {
                                val displayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                val backendFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val parsedDate = displayFormat.parse(dateOfBirth)
                                val formattedDOB = backendFormat.format(parsedDate!!)

                                val success = userApiService.submitUserDetails(
                                    fullName,
                                    formattedDOB,
                                    mobileNumber,
                                    emailAddress,
                                    totalLimit,
                                    availableLimit,
                                    cardholderName,
                                    cardNumber,
                                    expiryDate,
                                    cvv
                                )

                                isLoading = false

                                if (success) {
                                    snackbarHostState.showSnackbar("Submission Successful!")

                                    // Clear all fields and errors
                                    fullName = ""
                                    dateOfBirth = ""
                                    mobileNumber = ""
                                    emailAddress = ""
                                    totalLimit = ""
                                    availableLimit = ""
                                    cardholderName = ""
                                    cardNumber = ""
                                    expiryDate = ""
                                    cvv = ""

                                    fullNameError = null
                                    dobError = null
                                    mobileError = null
                                    emailError = null
                                    totalLimitError = null
                                    availableLimitError = null
                                    cardholderError = null
                                    cardNumberError = null
                                    expiryDateError = null
                                    cvvError = null
                                } else {
                                    snackbarHostState.showSnackbar("Submission Failed.")
                                }
                            } catch (e: Exception) {
                                isLoading = false
                                snackbarHostState.showSnackbar("Error: ${e.message}")
                            }
                        }
                    },
                    modifier = Modifier
                        .width(buttonWidth.dp)
                        .height(buttonHeight),
                    shape = RoundedCornerShape(buttonHeight / 2),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1166DD))
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White
                        )
                    } else {
                        Text("Proceed Now", color = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // DatePicker Dialog for DOB
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val displayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        dateOfBirth = displayFormat.format(Date(millis))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    error: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Text(label, fontWeight = FontWeight.SemiBold)

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            isError = error != null,
            singleLine = true
        )

        error?.let {
            Text(it, color = Color.Red, fontSize = 12.sp)
        }
    }
}