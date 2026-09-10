package com.aktarjabed.inbusiness.domain.context

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "business_prefs")

@Singleton
class BusinessContext @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val USER_ID_KEY = stringPreferencesKey("current_user_id")
    private val BUSINESS_ID_KEY = stringPreferencesKey("active_business_id")

    val currentUserId: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_ID_KEY] ?: throw IllegalStateException("No active user ID found in context")
    }

    val activeBusinessId: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[BUSINESS_ID_KEY] ?: throw IllegalStateException("No active business ID found in context")
    }

    suspend fun setUserId(userId: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = userId
        }
    }

    suspend fun setActiveBusinessId(businessId: String) {
        context.dataStore.edit { preferences ->
            preferences[BUSINESS_ID_KEY] = businessId
        }
    }
}
