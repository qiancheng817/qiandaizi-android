package com.qiandaizi.app.core

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.qianDataStore by preferencesDataStore(name = "qian_session")

class SessionStore(context: Context) {

    private val ds = context.qianDataStore
    private val key = stringPreferencesKey("raw_session")

    val flow: Flow<RawSession> = ds.data.map { prefs ->
        prefs[key]?.let { raw ->
            runCatching {
                AppJson.decodeFromString(RawSession.serializer(), raw)
            }.getOrNull()
        } ?: RawSession()
    }

    suspend fun save(session: RawSession) {
        ds.edit { prefs ->
            prefs[key] = AppJson.encodeToString(RawSession.serializer(), session)
        }
    }
}
