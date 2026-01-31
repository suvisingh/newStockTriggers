package com.example.stocktriggers

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Data class representing the state of a single stock tile in the favorites dashboard.
 */
data class FavoriteTileData(
    val symbol: String,
    val currentPrice: Double,
    val signal: Signal
)

/**
 * MainViewModel handles the business logic for the main stock screen and dashboard.
 *
 * Intent:
 * It acts as a bridge between the UI and the data layer (Repositories/Analyzers). It manages
 * the UI state, favorites dashboard data, and handles user interactions like updating symbols
 * or toggling favorites.
 *
 * Exposed APIs:
 * - [uiState]: A [StateFlow] of [StockUiState] for the main stock display.
 * - [favoritesDashboard]: A [StateFlow] of [FavoriteTileData] list for the dashboard.
 * - [isCurrentFavorite]: A [StateFlow] indicating if the currently searched symbol is a favorite.
 * - [messageEvent]: A [Flow] of transient messages (e.g., for Toasts).
 * - [updateSymbol]: Updates the currently tracked stock symbol and fetches new data.
 * - [fetchData]: Refreshes data for the current symbol.
 * - [toggleFavorite]: Toggles the favorite status of the current symbol.
 * - [refreshFavoritesDashboard]: Re-fetches data for all favorited symbols.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StockRepository()
    private val favoritesRepository = FavoritesRepository(application)
    private val analyzer = StockAnalyzer // Logic object
    private val TAG = "MainViewModel"

    private val _uiState = MutableStateFlow<StockUiState>(StockUiState.Loading)
    val uiState: StateFlow<StockUiState> = _uiState.asStateFlow()

    // Favorites Dashboard State
    private val _favoritesDashboard = MutableStateFlow<List<FavoriteTileData>>(emptyList())
    val favoritesDashboard: StateFlow<List<FavoriteTileData>> = _favoritesDashboard.asStateFlow()

    // Current Symbol Favorite Status
    private val _isCurrentFavorite = MutableStateFlow(false)
    val isCurrentFavorite: StateFlow<Boolean> = _isCurrentFavorite.asStateFlow()

    // Message Event for Toasts (using Channel)
    private val _messageEvent = Channel<String>()
    val messageEvent = _messageEvent.receiveAsFlow()

    // Keep track of current symbol
    private var currentSymbol = "^NSEI"

    init {
        fetchData()
        refreshFavoritesDashboard()
    }

    /**
     * Updates the current symbol and triggers a data fetch.
     *
     * @param symbol The new stock symbol to track (e.g., "AAPL").
     *
     * Example:
     * ```
     * viewModel.updateSymbol("MSFT")
     * ```
     */
    fun updateSymbol(symbol: String) {
        Log.d(TAG, "Updating symbol to: $symbol")
        currentSymbol = symbol.uppercase()
        fetchData()
    }

    /**
     * Fetches market data for the currently selected symbol and updates [uiState].
     *
     * Handles cases where data is insufficient or fetching fails by updating [uiState] to [StockUiState.Error].
     *
     * Example:
     * ```
     * viewModel.fetchData()
     * ```
     */
    fun fetchData() {
        Log.d(TAG, "Fetching data for symbol: $currentSymbol")
        viewModelScope.launch {
            _uiState.value = StockUiState.Loading
            checkCurrentFavoriteStatus()
            
            // Fetch main data
            val data = repository.getLastWorkingDaysData(currentSymbol, 6)
            if (data.size < 6) {
                // If fetching fails or insufficient data, we still show error state
                _uiState.value = StockUiState.Error("Insufficient data fetched for $currentSymbol")
            } else {
                 val analysis = StockAnalyzer.analyze(data)
                 if (analysis != null) {
                    _uiState.value = StockUiState.Success(
                        symbol = currentSymbol,
                        data = data,
                        currentPrice = data.last().close,
                        meanPrice = analysis.mean,
                        difference = analysis.difference,
                        percentageChange = analysis.percentageChange,
                        signal = analysis.signal
                    )
                 } else {
                     _uiState.value = StockUiState.Error("Analysis failed for $currentSymbol")
                 }
            }
        }
    }
    
    private fun checkCurrentFavoriteStatus() {
        _isCurrentFavorite.value = favoritesRepository.isFavorite(currentSymbol)
    }
    
    /**
     * Toggles the favorite status for the current symbol.
     * Enforces the maximum favorites limit and sends a message to [messageEvent].
     *
     * Example:
     * ```
     * viewModel.toggleFavorite()
     * ```
     */
    fun toggleFavorite() {
        if (favoritesRepository.isFavorite(currentSymbol)) {
            favoritesRepository.removeFavorite(currentSymbol)
            _isCurrentFavorite.value = false
            viewModelScope.launch { _messageEvent.send("Removed from favorites") }
        } else {
            val added = favoritesRepository.addFavorite(currentSymbol)
            if (added) {
                _isCurrentFavorite.value = true
                viewModelScope.launch { _messageEvent.send("Added to favorites") }
            } else {
                viewModelScope.launch { _messageEvent.send("Max 3 favorites allowed. Unstar one to add another.") }
            }
        }
        refreshFavoritesDashboard()
    }
    
    /**
     * Refreshes the data for all favorited symbols to update the dashboard.
     *
     * Example:
     * ```
     * viewModel.refreshFavoritesDashboard()
     * ```
     */
    fun refreshFavoritesDashboard() {
        viewModelScope.launch {
            val favorites = favoritesRepository.getFavorites()
            val tiles = mutableListOf<FavoriteTileData>()
            
            for (symbol in favorites) {
                 try {
                    val data = repository.getLastWorkingDaysData(symbol, 6)
                    val analysis = StockAnalyzer.analyze(data)
                    if (analysis != null) {
                        tiles.add(FavoriteTileData(symbol, analysis.currentPrice, analysis.signal))
                    }
                } catch (e: Exception) {
                    // Ignore failures for dashboard tiles to avoid crashing
                }
            }
            _favoritesDashboard.value = tiles
        }
    }

    /**
     * Trigger a test notification immediately.
     * Useful for verifying system permission and channel setup.
     */
    fun sendTestNotification() {
        // We use a OneTimeWorkRequest to trigger the worker immediately
        val workManager = androidx.work.WorkManager.getInstance(getApplication())
        val testRequest = androidx.work.OneTimeWorkRequestBuilder<StockSyncWorker>()
            .build()
        workManager.enqueue(testRequest)
        viewModelScope.launch { _messageEvent.send("Test Notification Triggered") }
    }
}
