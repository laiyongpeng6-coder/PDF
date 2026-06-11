package com.scantidy.scan.core.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

/** 全局唯一的 DataStore 实例 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

object AppPreferences {

    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    /** 是否已完成新手引导 */
    suspend fun isOnboardingCompleted(context: Context): Boolean {
        return try {
            context.dataStore.data.first()[Keys.ONBOARDING_COMPLETED] ?: false
        } catch (_: Exception) {
            false
        }
    }

    /** 标记新手引导已完成 */
    suspend fun markOnboardingCompleted(context: Context) {
        try {
            context.dataStore.edit { prefs ->
                prefs[Keys.ONBOARDING_COMPLETED] = true
            }
        } catch (_: Exception) {
            // 静默失败，下次启动重新显示引导
        }
    }
}
