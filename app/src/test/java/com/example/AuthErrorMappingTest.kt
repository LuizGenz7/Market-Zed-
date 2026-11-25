package com.example

import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialCustomException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.example.data.auth.AuthRepository
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthErrorMappingTest {

    @Test
    fun noCredentialExceptionMapsToActionableMessage() {
        val msg = AuthRepository.friendlyAuthError(NoCredentialException("No credentials available"))
        assertTrue(msg.contains("Add a Google account", ignoreCase = true))
        assertTrue(msg.contains("email and password", ignoreCase = true))
    }

    @Test
    fun cancellationExceptionMapsToFriendlyCancelMessage() {
        val msg = AuthRepository.friendlyAuthError(GetCredentialCancellationException("Activity is cancelled by the user."))
        assertEquals("Google Sign-In was cancelled.", msg)
    }

    @Test
    fun providerConfigurationAndUnsupportedMapClearly() {
        val providerMsg = AuthRepository.friendlyAuthError(GetCredentialProviderConfigurationException("no provider"))
        assertTrue(providerMsg.contains("Play Services", ignoreCase = true))

        val unsupportedMsg = AuthRepository.friendlyAuthError(GetCredentialUnsupportedException("unsupported"))
        assertTrue(unsupportedMsg.contains("not supported", ignoreCase = true))
    }

    @Test
    fun interruptedAndTokenParsingMapClearly() {
        val interruptedMsg = AuthRepository.friendlyAuthError(GetCredentialInterruptedException("interrupted"))
        assertTrue(interruptedMsg.contains("interrupted", ignoreCase = true))

        val parsingMsg = AuthRepository.friendlyAuthError(GoogleIdTokenParsingException(IllegalStateException("invalid token")))
        assertTrue(parsingMsg.contains("invalid Google Sign-In token", ignoreCase = true))
    }

    @Test
    fun developerErrorHintMapsToSha1Guidance() {
        val msg = AuthRepository.friendlyAuthError(
            GetCredentialCustomException("androidx.credentials.TYPE_CUSTOM", "28444: Developer console is not set up correctly")
        )
        assertTrue(msg.contains("SHA-1", ignoreCase = true))
    }

    @Test
    fun firebaseEmailErrorsMapCleanly() {
        val invalidCred = AuthRepository.friendlyAuthError(IllegalStateException("INVALID_LOGIN_CREDENTIALS"))
        assertTrue(invalidCred.contains("Invalid email or password"))
    }

    @Test
    fun googleOAuthFallbackTriggersOnInstantCancelAndConfigErrorsOnly() {
        assertTrue(
            AuthRepository.shouldFallbackToFirebaseGoogleOAuth(
                GetCredentialCancellationException("Activity is cancelled by the user."),
                elapsedMs = 350L
            )
        )
        assertTrue(
            !AuthRepository.shouldFallbackToFirebaseGoogleOAuth(
                GetCredentialCancellationException("Activity is cancelled by the user."),
                elapsedMs = 4_000L
            )
        )
        assertTrue(
            AuthRepository.shouldFallbackToFirebaseGoogleOAuth(
                NoCredentialException("No matching credentials"),
                elapsedMs = 5_000L
            )
        )
        assertTrue(
            AuthRepository.shouldFallbackToFirebaseGoogleOAuth(
                GetCredentialCustomException("TYPE_CUSTOM", "28444: DEVELOPER_ERROR"),
                elapsedMs = 5_000L
            )
        )
    }
}
