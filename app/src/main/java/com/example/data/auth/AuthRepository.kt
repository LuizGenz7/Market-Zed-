package com.example.data.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.SystemClock
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.IOException

data class AuthUserState(
    val isFirebaseConfigured: Boolean = false,
    val uid: String? = null,
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val isEmailVerified: Boolean = false,
    val isAdminClaim: Boolean = false,
    val role: String? = null,
    val providerIds: Set<String> = emptySet()
) {
    val isSignedIn: Boolean get() = !uid.isNullOrBlank()
    val userScope: String get() = uid?.takeIf { it.isNotBlank() } ?: "guest"
}

sealed class AuthTokenResult {
    data class Token(val jwt: String) : AuthTokenResult()
    data object NoUser : AuthTokenResult()
    data class OfflineOrTransient(val message: String) : AuthTokenResult()
    data class SessionInvalidated(val message: String) : AuthTokenResult()
}

sealed class SessionValidationResult {
    data object Valid : SessionValidationResult()
    data object NoSession : SessionValidationResult()
    data class OfflineCachedValid(val uid: String) : SessionValidationResult()
    data class Invalidated(val reason: String) : SessionValidationResult()
}

class AuthRepository(context: Context) {
    private val appContext = context.applicationContext
    private val secureSessionStore = SecureSessionStore(appContext)
    private val lock = Any()
    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val initDeferred = CompletableDeferred<FirebaseAuth?>()

    @Volatile
    private var initialized = false

    @Volatile
    private var firebaseConfigured = false

    @Volatile
    private var cachedAuth: FirebaseAuth? = null

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        _authState.value = mapUser(user)
        if (user != null) {
            recordSessionIfMissing(user)
            fetchClaimsAsync(user)
        }
    }

    private val _authState = MutableStateFlow(AuthUserState())
    val authState: StateFlow<AuthUserState> = _authState.asStateFlow()

    init {
        cleanupScope.launch {
            ensureInitialized()
        }
    }

    private suspend fun awaitInitialized(): FirebaseAuth? {
        if (initialized) return cachedAuth
        ensureInitialized()
        return initDeferred.await()
    }

    private fun ensureInitialized(): FirebaseAuth? {
        if (initialized) return cachedAuth
        synchronized(lock) {
            if (initialized) return cachedAuth
            val configured = try {
                FirebaseApp.getApps(appContext).isNotEmpty() || FirebaseApp.initializeApp(appContext) != null
            } catch (_: Throwable) {
                false
            }
            firebaseConfigured = configured
            val instance = if (configured) {
                try {
                    FirebaseAuth.getInstance()
                } catch (_: Throwable) {
                    null
                }
            } else {
                null
            }
            cachedAuth = instance
            initialized = true
            if (instance != null) {
                try {
                    val user = instance.currentUser
                    _authState.value = mapUser(user)
                    if (user != null) {
                        recordSessionIfMissing(user)
                        fetchClaimsAsync(user)
                    }
                    instance.addAuthStateListener(authStateListener)
                } catch (_: Throwable) {
                }
            } else {
                _authState.value = mapUser(null)
            }
            if (!initDeferred.isCompleted) {
                initDeferred.complete(cachedAuth)
            }
            return cachedAuth
        }
    }

    private fun recordSessionIfMissing(user: FirebaseUser, explicitSignIn: Boolean = false) {
        val now = System.currentTimeMillis()
        val existing = secureSessionStore.read(user.uid)
        val firebaseLastSignIn = user.metadata?.lastSignInTimestamp?.takeIf { it > 0L }
        val signInTime = when {
            explicitSignIn -> firebaseLastSignIn ?: now
            existing != null && existing.lastSignInAtMillis > 0L -> existing.lastSignInAtMillis
            else -> firebaseLastSignIn ?: now
        }
        val provider = user.providerData
            .firstOrNull { it.providerId != "firebase" }
            ?.providerId
            ?: existing?.providerId
            ?: "firebase"
        secureSessionStore.save(
            PersistedSessionMetadata(
                uid = user.uid,
                providerId = provider,
                lastSignInAtMillis = signInTime,
                lastVerifiedAtMillis = now
            )
        )
    }

    private fun mapUser(
        user: FirebaseUser?,
        isAdminClaim: Boolean = false,
        role: String? = null
    ): AuthUserState {
        val providers = user?.providerData
            ?.mapNotNull { it.providerId.takeIf { id -> id.isNotBlank() && id != "firebase" } }
            ?.toSet()
            .orEmpty()
        return AuthUserState(
            isFirebaseConfigured = firebaseConfigured && cachedAuth != null,
            uid = user?.uid,
            email = user?.email,
            displayName = user?.displayName,
            photoUrl = user?.photoUrl?.toString(),
            isEmailVerified = user?.isEmailVerified == true,
            isAdminClaim = isAdminClaim,
            role = role,
            providerIds = providers
        )
    }

    private fun fetchClaimsAsync(user: FirebaseUser, forceRefresh: Boolean = false) {
        cleanupScope.launch {
            val updated = resolveUserWithClaims(user, forceRefresh)
            if (_authState.value.uid == user.uid) {
                _authState.value = updated
            }
        }
    }

    private suspend fun resolveUserWithClaims(
        user: FirebaseUser?,
        forceRefresh: Boolean = false
    ): AuthUserState {
        if (user == null) return mapUser(null)
        return try {
            val tokenResult = user.getIdToken(forceRefresh).await()
            val claims = tokenResult.claims
            val (isAdminClaim, role) = extractAdminFromClaims(claims)
            mapUser(user, isAdminClaim = isAdminClaim, role = role)
        } catch (_: Throwable) {
            mapUser(user)
        }
    }

    suspend fun refreshClaims(forceRefresh: Boolean = true): AuthUserState {
        val user = awaitInitialized()?.currentUser
        val updated = resolveUserWithClaims(user, forceRefresh)
        _authState.value = updated
        return updated
    }

    suspend fun validateAndRefreshSession(forceReload: Boolean = false): SessionValidationResult {
        val firebaseAuth = awaitInitialized() ?: return SessionValidationResult.NoSession
        val user = firebaseAuth.currentUser
        if (user == null) {
            secureSessionStore.clear()
            return SessionValidationResult.NoSession
        }

        val existingMeta = secureSessionStore.read(user.uid)
        val now = System.currentTimeMillis()
        if (secureSessionStore.isSessionExpiredByAge(existingMeta, now)) {
            signOutLocalOnly(firebaseAuth)
            return SessionValidationResult.Invalidated(
                "Your 30-day MarketZed sign-in session expired. Please sign in again."
            )
        }

        return try {
            if (forceReload) {
                user.reload().await()
            }
            val tokenResult = user.getIdToken(forceReload).await()
            val (isAdminClaim, role) = extractAdminFromClaims(tokenResult.claims)
            recordSessionIfMissing(user, explicitSignIn = false)
            _authState.value = mapUser(firebaseAuth.currentUser ?: user, isAdminClaim, role)
            SessionValidationResult.Valid
        } catch (e: Throwable) {
            when {
                isTransientNetworkError(e) -> SessionValidationResult.OfflineCachedValid(user.uid)
                isPermanentInvalidSessionError(e) -> {
                    signOutLocalOnly(firebaseAuth)
                    SessionValidationResult.Invalidated(friendlySessionInvalidMessage(e))
                }
                else -> SessionValidationResult.OfflineCachedValid(user.uid)
            }
        }
    }

    suspend fun getIdTokenResult(forceRefresh: Boolean = false): AuthTokenResult {
        val firebaseAuth = awaitInitialized() ?: return AuthTokenResult.NoUser
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            secureSessionStore.clear()
            return AuthTokenResult.NoUser
        }
        val meta = secureSessionStore.read(currentUser.uid)
        if (secureSessionStore.isSessionExpiredByAge(meta)) {
            signOutLocalOnly(firebaseAuth)
            return AuthTokenResult.SessionInvalidated(
                "Your 30-day MarketZed sign-in session expired. Please sign in again."
            )
        }
        return try {
            val token = currentUser.getIdToken(forceRefresh).await().token
            if (token.isNullOrBlank()) {
                AuthTokenResult.SessionInvalidated("Your sign-in session is no longer valid. Please sign in again.")
            } else {
                recordSessionIfMissing(currentUser, explicitSignIn = false)
                AuthTokenResult.Token(token)
            }
        } catch (e: Throwable) {
            when {
                isTransientNetworkError(e) -> AuthTokenResult.OfflineOrTransient(
                    "Unable to verify your session while offline. Check your connection and retry."
                )
                isPermanentInvalidSessionError(e) -> {
                    signOutLocalOnly(firebaseAuth)
                    AuthTokenResult.SessionInvalidated(friendlySessionInvalidMessage(e))
                }
                else -> AuthTokenResult.OfflineOrTransient(
                    e.localizedMessage ?: "Temporary authentication network issue. Please try again."
                )
            }
        }
    }

    suspend fun getIdToken(forceRefresh: Boolean = false): String? {
        return when (val res = getIdTokenResult(forceRefresh)) {
            is AuthTokenResult.Token -> res.jwt
            else -> null
        }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<AuthUserState> {
        val firebaseAuth = awaitInitialized() ?: return Result.failure(
            IllegalStateException("Firebase configuration (google-services.json) is not bundled yet for this build.")
        )
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email.trim(), password).await()
            result.user?.let { recordSessionIfMissing(it, explicitSignIn = true) }
            val mapped = resolveUserWithClaims(result.user)
            _authState.value = mapped
            Result.success(mapped)
        } catch (e: Exception) {
            Result.failure(Exception(friendlyAuthError(e)))
        }
    }

    suspend fun registerWithEmail(fullName: String, email: String, password: String): Result<AuthUserState> {
        val firebaseAuth = awaitInitialized() ?: return Result.failure(
            IllegalStateException("Firebase configuration (google-services.json) is not bundled yet for this build.")
        )
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email.trim(), password).await()
            val user = result.user
            if (user != null) {
                val updates = UserProfileChangeRequest.Builder()
                    .setDisplayName(fullName.trim())
                    .build()
                user.updateProfile(updates).await()
                recordSessionIfMissing(user, explicitSignIn = true)
                try {
                    user.sendEmailVerification().await()
                } catch (_: Exception) {
                    // Verification email failure shouldn't break created account state
                }
            }
            val mapped = resolveUserWithClaims(firebaseAuth.currentUser)
            _authState.value = mapped
            Result.success(mapped)
        } catch (e: Exception) {
            Result.failure(Exception(friendlyAuthError(e)))
        }
    }

    suspend fun sendEmailVerification(): Result<Unit> {
        val user = awaitInitialized()?.currentUser ?: return Result.failure(IllegalStateException("Sign in first."))
        return try {
            user.sendEmailVerification().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(friendlyAuthError(e)))
        }
    }

    suspend fun refreshUser(): AuthUserState {
        val firebaseAuth = awaitInitialized()
        val user = firebaseAuth?.currentUser
        if (user != null) {
            try {
                user.reload().await()
            } catch (e: Throwable) {
                if (isPermanentInvalidSessionError(e)) {
                    signOutLocalOnly(firebaseAuth)
                    return _authState.value
                }
            }
        }
        val mapped = resolveUserWithClaims(firebaseAuth?.currentUser, forceRefresh = true)
        _authState.value = mapped
        return mapped
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        val firebaseAuth = awaitInitialized() ?: return Result.failure(
            IllegalStateException("Firebase configuration (google-services.json) is not bundled yet for this build.")
        )
        return try {
            firebaseAuth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(friendlyAuthError(e)))
        }
    }

    suspend fun signInWithGoogle(activityContext: Context, webClientId: String): Result<AuthUserState> {
        val firebaseAuth = awaitInitialized() ?: return Result.failure(
            IllegalStateException("Firebase configuration (google-services.json) is not bundled yet for this build.")
        )
        val resolvedActivity = activityContext.findActivity()
            ?: return Result.failure(IllegalStateException("Unable to start Google Sign-In without an active screen."))

        if (webClientId.isNotBlank()) {
            val credentialManager = CredentialManager.create(resolvedActivity)
            val startMs = SystemClock.elapsedRealtime()
            try {
                val credResponse = requestGoogleButtonCredential(credentialManager, resolvedActivity, webClientId)
                val cred = credResponse.credential
                if (cred is CustomCredential &&
                    cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleCred = GoogleIdTokenCredential.createFrom(cred.data)
                    val firebaseCred = GoogleAuthProvider.getCredential(googleCred.idToken, null)
                    val authResult = firebaseAuth.signInWithCredential(firebaseCred).await()
                    authResult.user?.let { recordSessionIfMissing(it, explicitSignIn = true) }
                    val mapped = resolveUserWithClaims(authResult.user)
                    _authState.value = mapped
                    return Result.success(mapped)
                }
            } catch (e: Exception) {
                val elapsedMs = SystemClock.elapsedRealtime() - startMs
                if (!shouldFallbackToFirebaseGoogleOAuth(e, elapsedMs)) {
                    return Result.failure(Exception(friendlyAuthError(e)))
                }
            }
        }
        return signInWithGoogleWebOAuth(firebaseAuth, resolvedActivity)
    }

    suspend fun linkGoogleAccount(activityContext: Context, webClientId: String): Result<AuthUserState> {
        val firebaseAuth = awaitInitialized() ?: return Result.failure(
            IllegalStateException("Firebase configuration is unavailable.")
        )
        val currentUser = firebaseAuth.currentUser
            ?: return Result.failure(IllegalStateException("Sign in with your existing account first."))
        val resolvedActivity = activityContext.findActivity()
            ?: return Result.failure(IllegalStateException("Unable to link Google without an active screen."))
        if (webClientId.isBlank()) {
            return Result.failure(IllegalStateException("Google OAuth client ID is missing in build configuration."))
        }
        return try {
            val credentialManager = CredentialManager.create(resolvedActivity)
            val response = requestGoogleButtonCredential(credentialManager, resolvedActivity, webClientId)
            val cred = response.credential
            if (cred is CustomCredential &&
                cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleCred = GoogleIdTokenCredential.createFrom(cred.data)
                val firebaseCred = GoogleAuthProvider.getCredential(googleCred.idToken, null)
                val linkRes = currentUser.linkWithCredential(firebaseCred).await()
                linkRes.user?.let { recordSessionIfMissing(it, explicitSignIn = false) }
                val mapped = resolveUserWithClaims(linkRes.user)
                _authState.value = mapped
                Result.success(mapped)
            } else {
                Result.failure(IllegalStateException("Unsupported credential returned for Google linking."))
            }
        } catch (e: Exception) {
            Result.failure(Exception(friendlyAuthError(e)))
        }
    }

    private suspend fun signInWithGoogleWebOAuth(
        firebaseAuth: FirebaseAuth,
        activity: Activity
    ): Result<AuthUserState> {
        return try {
            val pending = firebaseAuth.pendingAuthResult
            val authResult = if (pending != null) {
                pending.await()
            } else {
                val provider = OAuthProvider.newBuilder("google.com")
                    .addCustomParameter("prompt", "select_account")
                    .build()
                firebaseAuth.startActivityForSignInWithProvider(activity, provider).await()
            }
            authResult.user?.let { recordSessionIfMissing(it, explicitSignIn = true) }
            val mapped = resolveUserWithClaims(authResult.user)
            _authState.value = mapped
            Result.success(mapped)
        } catch (e: Exception) {
            Result.failure(Exception(friendlyAuthError(e)))
        }
    }

    private suspend fun requestGoogleButtonCredential(
        credentialManager: CredentialManager,
        activity: Activity,
        webClientId: String
    ): GetCredentialResponse {
        val buttonOption = GetSignInWithGoogleOption.Builder(serverClientId = webClientId)
            .build()
        val buttonRequest = GetCredentialRequest.Builder()
            .addCredentialOption(buttonOption)
            .build()

        val buttonStart = SystemClock.elapsedRealtime()
        return try {
            credentialManager.getCredential(
                context = activity,
                request = buttonRequest
            )
        } catch (primaryError: Exception) {
            val buttonElapsedMs = SystemClock.elapsedRealtime() - buttonStart
            val shouldTryGoogleIdOption = primaryError is NoCredentialException ||
                (primaryError is GetCredentialCancellationException && buttonElapsedMs < INSTANT_CANCEL_THRESHOLD_MS)
            if (!shouldTryGoogleIdOption) {
                throw primaryError
            }
            val fallbackOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .setServerClientId(webClientId)
                .build()
            val fallbackRequest = GetCredentialRequest.Builder()
                .addCredentialOption(fallbackOption)
                .build()
            try {
                credentialManager.getCredential(
                    context = activity,
                    request = fallbackRequest
                )
            } catch (_: Exception) {
                throw primaryError
            }
        }
    }

    private fun signOutLocalOnly(firebaseAuth: FirebaseAuth) {
        try {
            firebaseAuth.signOut()
        } catch (_: Throwable) {
        }
        secureSessionStore.clear()
        _authState.value = mapUser(null)
    }

    fun signOut() {
        try {
            cachedAuth?.signOut()
        } catch (_: Throwable) {
        }
        secureSessionStore.clear()
        cleanupScope.launch {
            try {
                CredentialManager.create(appContext)
                    .clearCredentialState(ClearCredentialStateRequest())
            } catch (_: Throwable) {
            }
        }
        _authState.value = mapUser(null)
    }

    internal companion object {
        internal const val INSTANT_CANCEL_THRESHOLD_MS = 1_500L

        internal fun isTransientNetworkError(e: Throwable): Boolean {
            return e is FirebaseNetworkException ||
                e is IOException ||
                e.cause is IOException ||
                e.localizedMessage.orEmpty().contains("network", ignoreCase = true)
        }

        internal fun isPermanentInvalidSessionError(e: Throwable): Boolean {
            if (e is FirebaseAuthInvalidUserException || e is FirebaseAuthInvalidCredentialsException) {
                return true
            }
            val code = (e as? FirebaseAuthException)?.errorCode?.uppercase().orEmpty()
            return code in setOf(
                "ERROR_USER_DISABLED",
                "ERROR_USER_NOT_FOUND",
                "ERROR_USER_TOKEN_EXPIRED",
                "ERROR_INVALID_USER_TOKEN"
            )
        }

        internal fun friendlySessionInvalidMessage(e: Throwable): String {
            val code = (e as? FirebaseAuthException)?.errorCode?.uppercase().orEmpty()
            return when (code) {
                "ERROR_USER_DISABLED" -> "This MarketZed account has been disabled. Contact support if this is unexpected."
                "ERROR_USER_NOT_FOUND" -> "Your account record could not be found. Please sign in or create an account."
                else -> "Your MarketZed sign-in session expired or was revoked. Please sign in again."
            }
        }

        internal fun shouldFallbackToFirebaseGoogleOAuth(e: Throwable, elapsedMs: Long): Boolean {
            return when (e) {
                is NoCredentialException,
                is GetCredentialProviderConfigurationException,
                is GetCredentialUnsupportedException -> true
                is GetCredentialCancellationException -> elapsedMs < INSTANT_CANCEL_THRESHOLD_MS
                is GetCredentialException -> {
                    val detail = e.localizedMessage.orEmpty()
                    detail.contains("28444", ignoreCase = true) ||
                        detail.contains("DEVELOPER_ERROR", ignoreCase = true) ||
                        detail.contains("not whitelisted", ignoreCase = true) ||
                        detail.contains("16:", ignoreCase = true) ||
                        detail.contains("reauth", ignoreCase = true) ||
                        elapsedMs < INSTANT_CANCEL_THRESHOLD_MS
                }
                else -> false
            }
        }

        internal fun extractAdminFromClaims(claims: Map<String, Any?>): Pair<Boolean, String?> {
            val rawRole = (claims["role"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
            val adminBool = (claims["admin"] as? Boolean == true) ||
                (claims["isAdmin"] as? Boolean == true)
            val roleIsAdmin = rawRole != null && rawRole.lowercase() in setOf("admin", "super_admin", "superadmin")
            val rolesListIsAdmin = (claims["roles"] as? Collection<*>)?.any {
                it?.toString()?.trim()?.lowercase() in setOf("admin", "super_admin", "superadmin")
            } == true
            val isAdmin = adminBool || roleIsAdmin || rolesListIsAdmin
            val resolvedRole = rawRole ?: if (isAdmin) "admin" else null
            return isAdmin to resolvedRole
        }

        internal tailrec fun Context.findActivity(): Activity? = when (this) {
            is Activity -> this
            is ContextWrapper -> baseContext?.findActivity()
            else -> null
        }

        internal fun friendlyAuthError(e: Throwable): String {
            return when (e) {
                is FirebaseAuthUserCollisionException ->
                    "An account already exists with this email using a different sign-in method (such as Email & Password). Sign in with that method first, then link Google from Account Settings."
                is GetCredentialCancellationException ->
                    "Google Sign-In was cancelled."
                is NoCredentialException ->
                    "No Google account is available on this device. Add a Google account in device Settings > Passwords & accounts, ensure Google Play Services is active, or sign in with email and password."
                is GetCredentialProviderConfigurationException ->
                    "Google Sign-In is unavailable on this device (Play Services credential provider missing). Please sign in with email and password."
                is GetCredentialUnsupportedException ->
                    "Google Sign-In via Credential Manager is not supported on this device. Please sign in with email and password."
                is GetCredentialInterruptedException ->
                    "Google Sign-In was interrupted. Please try again."
                is GoogleIdTokenParsingException ->
                    "Received an invalid Google Sign-In token. Please try again."
                is GetCredentialException -> {
                    val detail = e.localizedMessage.orEmpty()
                    if (detail.contains("28444", ignoreCase = true) ||
                        detail.contains("DEVELOPER_ERROR", ignoreCase = true) ||
                        detail.contains("not whitelisted", ignoreCase = true)
                    ) {
                        "Google Sign-In is not configured for this build's SHA-1 fingerprint yet. Verify SHA-1 in Firebase Project Settings or sign in with email and password."
                    } else if (detail.isNotBlank()) {
                        "Google Sign-In failed: $detail"
                    } else {
                        "Google Sign-In failed. Please try again or use email and password."
                    }
                }
                else -> {
                    val msg = e.localizedMessage ?: "Authentication error"
                    when {
                        msg.contains("password is invalid", ignoreCase = true) ||
                            msg.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ||
                            msg.contains("no user record", ignoreCase = true) ->
                            "Invalid email or password. Please check your details and try again."
                        msg.contains("certificate hash", ignoreCase = true) ||
                            msg.contains("package name", ignoreCase = true) ||
                            msg.contains("sha1", ignoreCase = true) ||
                            msg.contains("sign_in_failed", ignoreCase = true) ->
                            "Google Sign-In configuration error: package name or certificate hash mismatch in Firebase Console. Please verify SHA-1 and package name (com.techgu.marketzed) in Firebase Project Settings or sign in with email and password."
                        msg.contains("already in use", ignoreCase = true) ->
                            "An account with this email already exists. Try signing in instead."
                        msg.contains("too many requests", ignoreCase = true) ->
                            "Too many attempts. Please wait a few minutes and try again."
                        msg.contains("network", ignoreCase = true) ->
                            "Network error. Check your internet connection and retry."
                        msg.contains("disabled", ignoreCase = true) ->
                            "This account has been disabled."
                        msg.contains("cancelled", ignoreCase = true) || msg.contains("canceled", ignoreCase = true) ->
                            "Sign-in was cancelled."
                        else -> msg
                    }
                }
            }
        }
    }
}
