package com.example.data.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "marudhara_user_session")

class SessionManager(private val context: Context) {
    companion object {
        private val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val MOBILE_NUMBER = stringPreferencesKey("mobile_number")
        private val STUDENT_NAME = stringPreferencesKey("student_name")
        private val REMEMBER_ME = booleanPreferencesKey("remember_me")
        private val SAVED_PASSWORD = stringPreferencesKey("saved_password")
        private val PROFILE_PHOTO_URL = stringPreferencesKey("profile_photo_url")
        private val APP_LANGUAGE = stringPreferencesKey("app_language")
    }

    val isLoggedInFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_LOGGED_IN] ?: false
    }

    val appLanguageFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[APP_LANGUAGE] ?: "en"
    }

    suspend fun saveAppLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[APP_LANGUAGE] = language
        }
    }

    val mobileNumberFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[MOBILE_NUMBER]
    }

    val studentNameFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[STUDENT_NAME] ?: "प्रिय विद्यार्थी"
    }

    val rememberMeFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[REMEMBER_ME] ?: false
    }

    val savedPasswordFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[SAVED_PASSWORD]
    }

    val profilePhotoFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PROFILE_PHOTO_URL]
    }

    suspend fun saveSession(
        mobile: String,
        name: String,
        rememberMe: Boolean,
        password: String,
        profilePhoto: String? = null
    ) {
        context.dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = true
            preferences[MOBILE_NUMBER] = mobile
            preferences[STUDENT_NAME] = name
            preferences[REMEMBER_ME] = rememberMe
            preferences[SAVED_PASSWORD] = password
            if (profilePhoto != null) {
                preferences[PROFILE_PHOTO_URL] = profilePhoto
            } else {
                preferences.remove(PROFILE_PHOTO_URL)
            }
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = false
            val rememberMe = preferences[REMEMBER_ME] ?: false
            preferences.remove(SAVED_PASSWORD) // Cleanly wipe active password
            if (!rememberMe) {
                preferences.remove(MOBILE_NUMBER)
            }
            preferences.remove(STUDENT_NAME)
            preferences.remove(PROFILE_PHOTO_URL)
        }
    }
}
