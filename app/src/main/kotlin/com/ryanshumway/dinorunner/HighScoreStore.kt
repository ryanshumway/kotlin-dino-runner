package com.ryanshumway.dinorunner

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Kotlin note: `by preferencesDataStore(...)` is another property delegate.
 * It creates the DataStore the first time it's read and reuses it afterward,
 * so the app only ever opens one store for this file, which DataStore
 * requires.
 */
private val Context.scores: DataStore<Preferences> by preferencesDataStore(name = "scores")

/**
 * Saves the best score on the device. DataStore is Android's async key-value
 * store, the counterpart to `UserDefaults` / `@AppStorage` or `localStorage`,
 * except reads and writes never block the UI thread.
 */
class HighScoreStore(context: Context) {
    private val store = context.applicationContext.scores
    private val key = intPreferencesKey("high_score")

    /**
     * The saved best, as a stream that emits again whenever it changes. A
     * `Flow` is Kotlin's async sequence, like an AsyncSequence in Swift or an
     * RxJS observable.
     */
    val highScore: Flow<Int> = store.data.map { it[key] ?: 0 }

    /**
     * Keeps [score] if it beats the saved best. The compare happens inside
     * `edit`, which DataStore runs as a single read-modify-write transaction,
     * so two quick saves can never overwrite a higher score with a lower one.
     *
     * Kotlin note: `suspend` marks a function that can pause without blocking
     * a thread, like `async` in Swift or JavaScript.
     */
    suspend fun saveIfBest(score: Int) {
        store.edit { prefs ->
            if (score > (prefs[key] ?: 0)) prefs[key] = score
        }
    }
}
