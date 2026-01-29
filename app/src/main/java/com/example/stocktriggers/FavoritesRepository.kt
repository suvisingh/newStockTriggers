package com.example.stocktriggers

import android.content.Context
import android.content.SharedPreferences

class FavoritesRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("stock_favorites", Context.MODE_PRIVATE)
    private val KEY_FAVORITES = "favorite_symbols"
    private val MAX_FAVORITES = 3

    fun getFavorites(): Set<String> {
        return prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
    }

    /**
     * Adds a symbol to favorites.
     * @return true if added, false if limit reached.
     */
    fun addFavorite(symbol: String): Boolean {
        val current = getFavorites().toMutableSet()
        if (current.size >= MAX_FAVORITES) {
            return false
        }
        current.add(symbol.uppercase())
        prefs.edit().putStringSet(KEY_FAVORITES, current).apply()
        return true
    }

    fun removeFavorite(symbol: String) {
        val current = getFavorites().toMutableSet()
        current.remove(symbol.uppercase())
        prefs.edit().putStringSet(KEY_FAVORITES, current).apply()
    }

    fun isFavorite(symbol: String): Boolean {
        return getFavorites().contains(symbol.uppercase())
    }
}
