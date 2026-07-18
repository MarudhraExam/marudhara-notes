package com.example.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.store.SessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

enum class AuthMode {
    SIGN_IN, SIGN_UP, FORGOT_PASSWORD
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    sessionManager: SessionManager,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentMode by remember { mutableStateOf(AuthMode.SIGN_IN) }

    // Login Form State
    var mobileNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }

    // Registration (Sign Up) Form State
    var regName by remember { mutableStateOf("") }
    var regMobile by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regConfirmPassword by remember { mutableStateOf("") }
    var isRegPasswordVisible by remember { mutableStateOf(false) }

    // Forgot Password Form State
    var fpName by remember { mutableStateOf("") }
    var fpMobile by remember { mutableStateOf("") }
    var fpPassword by remember { mutableStateOf("") }
    var fpConfirmPassword by remember { mutableStateOf("") }
    var isFpPasswordVisible by remember { mutableStateOf(false) }

    // Loading & Error States
    var isAuthenticating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // Prepopulate saved mobile number/password if any on startup
    LaunchedEffect(key1 = true) {
        val savedMobile = sessionManager.mobileNumberFlow.firstOrNull() ?: ""
        val savedPass = sessionManager.savedPasswordFlow.firstOrNull() ?: ""
        val isRemembered = sessionManager.rememberMeFlow.firstOrNull() ?: true
        if (isRemembered && savedMobile.isNotEmpty()) {
            mobileNumber = savedMobile
            password = savedPass
            rememberMe = true
        }
    }

    // NATIVE SIGN IN ACTION
    val handleSignIn = {
        val trimmedMobile = mobileNumber.trim()
        val trimmedPassword = password.trim()

        if (trimmedMobile.length < 10) {
            errorMessage = "कृपया एक वैध 10-अंकों का मोबाइल नंबर दर्ज करें।"
        } else if (trimmedPassword.isEmpty()) {
            errorMessage = "कृपया अपना पासवर्ड दर्ज करें।"
        } else {
            errorMessage = null
            successMessage = null
            isAuthenticating = true

            // Synthetic stable email format used by Marudhara Exam: mobile@mockstudent.marudharaexam.in
            val email = "$trimmedMobile@mockstudent.marudharaexam.in"
            val auth = FirebaseAuth.getInstance()

            auth.signInWithEmailAndPassword(email, trimmedPassword)
                .addOnSuccessListener { authResult ->
                    val uid = authResult.user?.uid
                    if (uid != null) {
                        // Retrieve the verified profile data directly from production Firestore users/{uid}
                        FirebaseFirestore.getInstance().collection("users").document(uid).get()
                            .addOnSuccessListener { doc ->
                                val name = doc.getString("name") ?: "प्रिय विद्यार्थी"
                                val mobile = doc.getString("mobile") ?: trimmedMobile

                                coroutineScope.launch {
                                    sessionManager.saveSession(
                                        mobile = mobile,
                                        name = name,
                                        rememberMe = rememberMe,
                                        password = trimmedPassword
                                    )
                                    isAuthenticating = false
                                    onLoginSuccess()
                                }
                            }
                            .addOnFailureListener {
                                // Fallback if Firestore fetch fails but auth succeeded
                                coroutineScope.launch {
                                    sessionManager.saveSession(
                                        mobile = trimmedMobile,
                                        name = "प्रिय विद्यार्थी",
                                        rememberMe = rememberMe,
                                        password = trimmedPassword
                                    )
                                    isAuthenticating = false
                                    onLoginSuccess()
                                }
                            }
                    } else {
                        isAuthenticating = false
                        errorMessage = "त्रुटि: प्रमाणीकरण विफल रहा।"
                    }
                }
                .addOnFailureListener { exception ->
                    isAuthenticating = false
                    errorMessage = when (exception) {
                        is FirebaseAuthInvalidUserException -> "यह मोबाइल नंबर पंजीकृत नहीं है।"
                        is FirebaseAuthInvalidCredentialsException -> "पासवर्ड गलत है। कृपया पुनः प्रयास करें।"
                        else -> "लॉगिन विफल: ${exception.localizedMessage ?: "नेटवर्क समस्या, पुनः प्रयास करें"}"
                    }
                }
        }
    }

    // NATIVE SIGN UP ACTION
    val handleSignUp = {
        val trimmedName = regName.trim()
        val trimmedMobile = regMobile.trim()
        val trimmedPassword = regPassword.trim()
        val trimmedConfirmPassword = regConfirmPassword.trim()

        if (trimmedName.isEmpty()) {
            errorMessage = "कृपया अपना पूरा नाम दर्ज करें।"
        } else if (trimmedMobile.length < 10) {
            errorMessage = "कृपया वैध 10-अंकों का मोबाइल नंबर दर्ज करें।"
        } else if (trimmedPassword.length < 6) {
            errorMessage = "पासवर्ड कम से कम 6 अक्षरों का होना चाहिए।"
        } else if (trimmedPassword != trimmedConfirmPassword) {
            errorMessage = "पासवर्ड मेल नहीं खाते हैं।"
        } else {
            errorMessage = null
            successMessage = null
            isAuthenticating = true

            val email = "$trimmedMobile@mockstudent.marudharaexam.in"
            val auth = FirebaseAuth.getInstance()
            val db = FirebaseFirestore.getInstance()

            auth.createUserWithEmailAndPassword(email, trimmedPassword)
                .addOnSuccessListener { authResult ->
                    val user = authResult.user
                    if (user != null) {
                        // Set up standard production Firestore document in users/{uid}
                        val userProfile = hashMapOf(
                            "uid" to user.uid,
                            "name" to trimmedName,
                            "mobile" to trimmedMobile,
                            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                            "lastLogin" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                        )

                        db.collection("users").document(user.uid)
                            .set(userProfile, com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener {
                                coroutineScope.launch {
                                    sessionManager.saveSession(
                                        mobile = trimmedMobile,
                                        name = trimmedName,
                                        rememberMe = rememberMe,
                                        password = trimmedPassword
                                    )
                                    isAuthenticating = false
                                    onLoginSuccess()
                                }
                            }
                            .addOnFailureListener {
                                // Fallback to let user log in even if profile writing fails
                                coroutineScope.launch {
                                    sessionManager.saveSession(
                                        mobile = trimmedMobile,
                                        name = trimmedName,
                                        rememberMe = rememberMe,
                                        password = trimmedPassword
                                    )
                                    isAuthenticating = false
                                    onLoginSuccess()
                                }
                            }
                    } else {
                        isAuthenticating = false
                        errorMessage = "रजिस्ट्रेशन विफल रहा।"
                    }
                }
                .addOnFailureListener { exception ->
                    isAuthenticating = false
                    errorMessage = when (exception) {
                        is FirebaseAuthUserCollisionException -> "यह मोबाइल नंबर पहले से ही पंजीकृत है।"
                        else -> "रजिस्ट्रेशन विफल: ${exception.localizedMessage ?: "नेटवर्क समस्या"}"
                    }
                }
        }
    }

    // NATIVE FORGOT PASSWORD ACTION
    val handleForgotPassword = {
        val trimmedName = fpName.trim()
        val trimmedMobile = fpMobile.trim()
        val trimmedPassword = fpPassword.trim()
        val trimmedConfirmPassword = fpConfirmPassword.trim()

        if (trimmedName.isEmpty()) {
            errorMessage = "कृपया अपना पंजीकृत पूरा नाम दर्ज करें।"
        } else if (trimmedMobile.length < 10) {
            errorMessage = "कृपया वैध 10-अंकों का मोबाइल नंबर दर्ज करें।"
        } else if (trimmedPassword.length < 6) {
            errorMessage = "पासवर्ड कम से कम 6 अक्षरों का होना चाहिए।"
        } else if (trimmedPassword != trimmedConfirmPassword) {
            errorMessage = "पासवर्ड मेल नहीं खाते हैं।"
        } else {
            errorMessage = null
            successMessage = null
            isAuthenticating = true

            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val client = OkHttpClient()
                    val payload = JSONObject().apply {
                        put("name", trimmedName)
                        put("mobile", trimmedMobile)
                        put("newPassword", trimmedPassword)
                    }

                    val requestBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                    val request = Request.Builder()
                        .url("https://marudhara-payment-api.jmdseller2025.workers.dev/api/reset-password")
                        .post(requestBody)
                        .build()

                    client.newCall(request).execute().use { response ->
                        val responseBody = response.body?.string() ?: ""
                        if (response.isSuccessful) {
                            withContext(Dispatchers.Main) {
                                isAuthenticating = false
                                errorMessage = null
                                successMessage = "पासवर्ड सफलतापूर्वक बदल दिया गया है! कृपया नए पासवर्ड के साथ लॉगिन करें।"
                                // Reset fp states and switch to sign in
                                fpName = ""
                                fpMobile = ""
                                fpPassword = ""
                                fpConfirmPassword = ""
                                mobileNumber = trimmedMobile
                                password = ""
                                currentMode = AuthMode.SIGN_IN
                            }
                        } else {
                            val errorResponseMsg = try {
                                val errJson = JSONObject(responseBody)
                                errJson.optJSONObject("error")?.optString("message")
                                    ?: errJson.optString("message")
                            } catch (e: Exception) {
                                null
                            } ?: "विवरण मेल नहीं खाते हैं। कृपया अपना पंजीकृत नाम और मोबाइल सही दर्ज करें।"

                            withContext(Dispatchers.Main) {
                                isAuthenticating = false
                                errorMessage = errorResponseMsg
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        isAuthenticating = false
                        errorMessage = "संजाल त्रुटि: कृपया इंटरनेट कनेक्शन की जांच करें।"
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Upper background brand gradient curve
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.35f)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                        )
                    ),
                    shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                )
        )

        // Scrollable Card Container
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Branding Section
            Text(
                text = "मरुधरा एग्जाम",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 30.sp,
                    color = Color.White
                )
            )
            Text(
                text = "MARUDHARA EXAM",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Main Native Authentication Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header titles depending on Mode
                    val titleText = when (currentMode) {
                        AuthMode.SIGN_IN -> "लॉगिन करें (Sign In)"
                        AuthMode.SIGN_UP -> "नया अकाउंट (Register)"
                        AuthMode.FORGOT_PASSWORD -> "पासवर्ड बदलें (Reset)"
                    }
                    val subtitleText = when (currentMode) {
                        AuthMode.SIGN_IN -> "अपने पंजीकृत मोबाइल नंबर और पासवर्ड का उपयोग करें।"
                        AuthMode.SIGN_UP -> "मरुधरा एग्जाम पर अपना नया विद्यार्थी अकाउंट बनाएं।"
                        AuthMode.FORGOT_PASSWORD -> "अपने पंजीकृत नाम और मोबाइल नंबर से पासवर्ड बदलें।"
                    }

                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(top = 4.dp, bottom = 24.dp)
                    )

                    // Error Notification
                    if (errorMessage != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Text(
                                text = errorMessage!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Start
                            )
                        }
                    }

                    // Success Notification
                    if (successMessage != null) {
                        Surface(
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Text(
                                text = successMessage!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF047857),
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Start
                            )
                        }
                    }

                    // FORM FIELDS CONDITIONAL RENDER
                    when (currentMode) {
                        AuthMode.SIGN_IN -> {
                            // Mobile Number Input
                            OutlinedTextField(
                                value = mobileNumber,
                                onValueChange = { if (it.length <= 10) mobileNumber = it },
                                label = { Text("मोबाइल नंबर (Mobile Number)") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = "फ़ोन",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Password Input
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("पासवर्ड (Password)") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "पासवर्ड",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                        Icon(
                                            imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "पासवर्ड दिखाएं/छुपाएं"
                                        )
                                    }
                                },
                                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Action Links Row (Remember Me and Forgot Password)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { rememberMe = !rememberMe }
                                ) {
                                    Checkbox(
                                        checked = rememberMe,
                                        onCheckedChange = { rememberMe = it },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                    Text(
                                        text = "याद रखें",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }

                                Text(
                                    text = "पासवर्ड भूल गए?",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    ),
                                    modifier = Modifier.clickable {
                                        errorMessage = null
                                        successMessage = null
                                        currentMode = AuthMode.FORGOT_PASSWORD
                                    }
                                )
                            }
                        }

                        AuthMode.SIGN_UP -> {
                            // Full Name Input
                            OutlinedTextField(
                                value = regName,
                                onValueChange = { regName = it },
                                label = { Text("पूरा नाम (Full Name)") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "नाम",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Mobile Number Input
                            OutlinedTextField(
                                value = regMobile,
                                onValueChange = { if (it.length <= 10) regMobile = it },
                                label = { Text("मोबाइल नंबर (Mobile Number)") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = "फ़ोन",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Password Input
                            OutlinedTextField(
                                value = regPassword,
                                onValueChange = { regPassword = it },
                                label = { Text("पासवर्ड (Password)") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "पासवर्ड",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = { isRegPasswordVisible = !isRegPasswordVisible }) {
                                        Icon(
                                            imageVector = if (isRegPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "पासवर्ड दिखाएं/छुपाएं"
                                        )
                                    }
                                },
                                visualTransformation = if (isRegPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Confirm Password Input
                            OutlinedTextField(
                                value = regConfirmPassword,
                                onValueChange = { regConfirmPassword = it },
                                label = { Text("पासवर्ड पुनः दर्ज करें (Confirm Password)") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "पासवर्ड पुनः दर्ज करें",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        AuthMode.FORGOT_PASSWORD -> {
                            // Full Name Input
                            OutlinedTextField(
                                value = fpName,
                                onValueChange = { fpName = it },
                                label = { Text("पंजीकृत पूरा नाम (Registered Full Name)") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "नाम",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Mobile Number Input
                            OutlinedTextField(
                                value = fpMobile,
                                onValueChange = { if (it.length <= 10) fpMobile = it },
                                label = { Text("पंजीकृत मोबाइल (Registered Mobile)") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = "फ़ोन",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // New Password Input
                            OutlinedTextField(
                                value = fpPassword,
                                onValueChange = { fpPassword = it },
                                label = { Text("नया पासवर्ड (New Password)") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "नया पासवर्ड",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = { isFpPasswordVisible = !isFpPasswordVisible }) {
                                        Icon(
                                            imageVector = if (isFpPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "पासवर्ड दिखाएं/छुपाएं"
                                        )
                                    }
                                },
                                visualTransformation = if (isFpPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Confirm New Password Input
                            OutlinedTextField(
                                value = fpConfirmPassword,
                                onValueChange = { fpConfirmPassword = it },
                                label = { Text("नया पासवर्ड पुनः दर्ज करें (Confirm Password)") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "पासवर्ड पुनः दर्ज करें",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // ACTION BUTTON
                    Button(
                        onClick = {
                            when (currentMode) {
                                AuthMode.SIGN_IN -> handleSignIn()
                                AuthMode.SIGN_UP -> handleSignUp()
                                AuthMode.FORGOT_PASSWORD -> handleForgotPassword()
                            }
                        },
                        enabled = !isAuthenticating,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        if (isAuthenticating) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            val buttonText = when (currentMode) {
                                AuthMode.SIGN_IN -> "सुरक्षित लॉगिन (Login)"
                                AuthMode.SIGN_UP -> "नया अकाउंट बनाएं (Register)"
                                AuthMode.FORGOT_PASSWORD -> "पासवर्ड बदलें (Reset)"
                            }
                            Text(
                                text = buttonText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer Switch Mode section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val footerPreText = when (currentMode) {
                    AuthMode.SIGN_IN -> "नया छात्र अकाउंट बनाना है? "
                    AuthMode.SIGN_UP -> "पहले से अकाउंट बना हुआ है? "
                    AuthMode.FORGOT_PASSWORD -> "वापस लॉगिन स्क्रीन पर जाएं? "
                }
                val footerActionText = when (currentMode) {
                    AuthMode.SIGN_IN -> "यहाँ रजिस्टर करें"
                    AuthMode.SIGN_UP -> "यहाँ लॉगिन करें"
                    AuthMode.FORGOT_PASSWORD -> "यहाँ क्लिक करें"
                }

                Text(
                    text = footerPreText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Text(
                    text = footerActionText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier.clickable {
                        errorMessage = null
                        successMessage = null
                        currentMode = when (currentMode) {
                            AuthMode.SIGN_IN -> AuthMode.SIGN_UP
                            AuthMode.SIGN_UP -> AuthMode.SIGN_IN
                            AuthMode.FORGOT_PASSWORD -> AuthMode.SIGN_IN
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Brand Disclaimer
            Text(
                text = "आधिकारिक मरुधरा एग्जाम Companion App\n100% सुरक्षित एवं प्रमाणित प्रमाणीकरण",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                ),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

suspend fun <T> kotlinx.coroutines.flow.Flow<T>.firstOrNull(): T? {
    var result: T? = null
    try {
        result = this.first()
    } catch (e: Exception) {
        // Ignored
    }
    return result
}
