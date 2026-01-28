package com.example.stocktriggers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.util.Log

class MainViewModel : ViewModel() {

    private val repository = StockRepository()
    private val TAG = "MainViewModel"

    private val _uiState = MutableStateFlow<StockUiState>(StockUiState.Loading)
    val uiState: StateFlow<StockUiState> = _uiState

    // Keep track of current symbol
    private var currentSymbol = "^NSEI"

    init {
        fetchData()
    }

    fun updateSymbol(symbol: String) {
        Log.d(TAG, "Updating symbol to: $symbol")
        currentSymbol = symbol
        fetchData()
    }

    fun fetchData() {
        Log.d(TAG, "Fetching data for symbol: $currentSymbol")
        viewModelScope.launch {
            _uiState.value = StockUiState.Loading
            val data = repository.getLastWorkingDaysData(currentSymbol, 6)

            if (data.size < 6) {
                Log.e(TAG, "Insufficient data provided: ${data.size} items")
                _uiState.value = StockUiState.Error("Insufficient data fetched for $currentSymbol")
                return@launch
            }

            val analysis = StockAnalyzer.analyze(data)
            
            if (analysis == null) {
                 Log.e(TAG, "Analysis failed: Insufficient data or logic error")
                _uiState.value = StockUiState.Error("Analysis failed for $currentSymbol")
                return@launch
            }

            Log.d(TAG, "Analysis Complete. Mean: ${analysis.mean}, Current: ${data.last().close}, Diff: ${analysis.difference}, Signal: ${analysis.signal}")

            _uiState.value = StockUiState.Success(
                symbol = currentSymbol,
                data = data,
                currentPrice = data.last().close,
                meanPrice = analysis.mean,
                difference = analysis.difference,
                percentageChange = analysis.percentageChange,
                signal = analysis.signal
            )
        }
    }
}

sealed class StockUiState {
    object Loading : StockUiState()
    data class Error(val message: String) : StockUiState()
    data class Success(
        val symbol: String,
        val data: List<DailyClose>,
        val currentPrice: Double,
        val meanPrice: Double,
        val difference: Double,
        val percentageChange: Double,
        val signal: Signal
    ) : StockUiState()
}

enum class Signal {
    BUY, SELL, NEUTRAL
}
