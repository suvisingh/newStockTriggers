package com.example.stocktriggers

import android.content.Context
import android.content.SharedPreferences

/**
 * FavoritesRepository manages the persistence of favorite stock symbols using SharedPreferences.
 *
 * Intent:
 * This class provides a centralized way to store, retrieve, and manage a limited list of favorite stock symbols.
 *
 * Exposed APIs:
 * - [getFavorites]: Retrieves the current set of favorite symbols.
 * - [addFavorite]: Adds a new symbol to favorites if the limit hasn't been reached.
 * - [removeFavorite]: Removes a symbol from favorites.
 * - [isFavorite]: Checks if a specific symbol is in the favorites list.
 */
class FavoritesRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("stock_favorites", Context.MODE_PRIVATE)
    private val KEY_FAVORITES = "favorite_symbols"
    private val MAX_FAVORITES = 3

    /**
     * Retrieves the set of favorite stock symbols from persistent storage.
     *
     * @return A Set of Strings containing the uppercase stock symbols currently favorited.
     *
     * Example:
     * ```
     * val favorites = repository.getFavorites()
     * // returns SetOf("AAPL", "^NSEI")
     * ```
     */
    fun getFavorites(): Set<String> {
        return prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
    }

    /**
     * Adds a stock symbol to the favorites list, enforcing a maximum limit.
     *
     * @param symbol The stock symbol to add (e.g., "AAPL"). It will be converted to uppercase.
     * @return Boolean: true if the symbol was successfully added or already exists;
     *                  false if the symbol couldn't be added because the maximum limit (3) has been reached.
     *
     * Example:
     * ```
     * val success = repository.addFavorite("TSLA")
     * if (success) {
     *     // Symbol added
     * } else {
     *     // Limit reached
     * }
     * ```
     */
    fun addFavorite(symbol: String): Boolean {
        val current = getFavorites().toMutableSet()
        if (current.contains(symbol.uppercase())) return true
        if (current.size >= MAX_FAVORITES) {
            return false
        }
        current.add(symbol.uppercase())
        prefs.edit().putStringSet(KEY_FAVORITES, current).apply()
        return true
    }

    /**
     * Removes a stock symbol from the favorites list.
     *
     * @param symbol The stock symbol to remove.
     *
     * Example:
     * ```
     * repository.removeFavorite("AAPL")
     * ```
     */
    fun removeFavorite(symbol: String) {
        val current = getFavorites().toMutableSet()
        current.remove(symbol.uppercase())
        prefs.edit().putStringSet(KEY_FAVORITES, current).apply()
    }

    /**
     * Checks if a specific stock symbol is currently in the favorites list.
     *
     * @param symbol The stock symbol to check.
     * @return Boolean: true if the symbol is in favorites, false otherwise.
     *
     * Example:
     * ```
     * if (repository.isFavorite("^NSEI")) {
     *     // Is favorite
     * }
     * ```
     */
    fun isFavorite(symbol: String): Boolean {
        return getFavorites().contains(symbol.uppercase())
    }
}
