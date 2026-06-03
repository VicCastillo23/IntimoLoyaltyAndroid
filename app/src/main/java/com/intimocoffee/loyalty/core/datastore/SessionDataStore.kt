package com.intimocoffee.loyalty.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "loyalty_session")

@Singleton
class SessionDataStore @Inject constructor(
    private val context: Context
) {
    companion object {
        private val KEY_CUSTOMER_ID = longPreferencesKey("customer_id")
        private val KEY_CUSTOMER_NAME = stringPreferencesKey("customer_name")
        private val KEY_CUSTOMER_PHONE = stringPreferencesKey("customer_phone")
        private val KEY_SERVER_IP = stringPreferencesKey("server_ip")
        private val KEY_SERVER_PORT = intPreferencesKey("server_port")
        private val KEY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
    }
    
    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { it[KEY_IS_LOGGED_IN] ?: false }
    val customerId: Flow<Long?> = context.dataStore.data.map { it[KEY_CUSTOMER_ID] }
    val customerName: Flow<String?> = context.dataStore.data.map { it[KEY_CUSTOMER_NAME] }
    val customerPhone: Flow<String?> = context.dataStore.data.map { it[KEY_CUSTOMER_PHONE] }
    val serverIp: Flow<String> = context.dataStore.data.map { it[KEY_SERVER_IP] ?: "api.cafeintimo.mx" }
    val serverPort: Flow<Int> = context.dataStore.data.map { it[KEY_SERVER_PORT] ?: 443 }
    
    suspend fun saveSession(customerId: Long, customerName: String, phone: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CUSTOMER_ID] = customerId
            prefs[KEY_CUSTOMER_NAME] = customerName
            prefs[KEY_CUSTOMER_PHONE] = phone
            prefs[KEY_IS_LOGGED_IN] = true
        }
    }
    
    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_CUSTOMER_ID)
            prefs.remove(KEY_CUSTOMER_NAME)
            prefs.remove(KEY_CUSTOMER_PHONE)
            prefs[KEY_IS_LOGGED_IN] = false
        }
    }
    
    suspend fun saveServerConfig(ip: String, port: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SERVER_IP] = ip
            prefs[KEY_SERVER_PORT] = port
        }
    }
}
