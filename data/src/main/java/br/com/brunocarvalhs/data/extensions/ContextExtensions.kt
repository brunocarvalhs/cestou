package br.com.brunocarvalhs.data.extensions

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore by preferencesDataStore(name = "app_preferences")
