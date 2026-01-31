package com.example.stocktriggers

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * MainActivity is the entry point of the StockTriggers application.
 *
 * Intent:
 * It serves as the single Activity for the app, responsible for initializing the ViewModel,
 * setting up background synchronization workers, and hosting the main Compose UI.
 *
 * Exposed APIs:
 * - [onCreate]: Standard Activity lifecycle method to initialize the UI and background sync.
 */
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    /**
     * Called when the activity is starting.
     * Sets up background sync and initializes the Jetpack Compose UI.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in [onSaveInstanceState].
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setupHourlySync()

        setContent {
            Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = LocalContext.current
                    
                    // Listen for transient message events from ViewModel
                    LaunchedEffect(Unit) {
                        viewModel.messageEvent.collect { message ->
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    }

                    val uiState by viewModel.uiState.collectAsState()
                    val favoritesDashboard by viewModel.favoritesDashboard.collectAsState()
                    val isCurrentFavorite by viewModel.isCurrentFavorite.collectAsState()

                    StockScreen(
                        uiState = uiState,
                        favoritesDashboard = favoritesDashboard,
                        isCurrentFavorite = isCurrentFavorite,
                        onRefresh = { viewModel.fetchData() },
                        onUpdateSymbol = { viewModel.updateSymbol(it) },
                        onToggleFavorite = { viewModel.toggleFavorite() },
                        onTestNotification = { viewModel.sendTestNotification() }
                    )
                }
            }
        }
    }

    /**
     * Configures and schedules a periodic background task to sync stock data hourly.
     *
     * The task is scheduled to start at [AppConfig.SYNC_START_HOUR]:[AppConfig.SYNC_START_MINUTE] IST 
     * and repeat every [AppConfig.SYNC_INTERVAL_VALUE] [AppConfig.SYNC_INTERVAL_UNIT].
     * It uses [WorkManager] to ensure the task runs even if the app is closed.
     *
     * Example:
     * ```
     * setupHourlySync()
     * ```
     */
    private fun setupHourlySync() {
        val workManager = WorkManager.getInstance(this)
        
        // Calculate delay to next configured start time
        val calendar = Calendar.getInstance(java.util.TimeZone.getTimeZone(AppConfig.SYNC_TIMEZONE))
        val now = calendar.timeInMillis
        
        calendar.set(Calendar.HOUR_OF_DAY, AppConfig.SYNC_START_HOUR)
        calendar.set(Calendar.MINUTE, AppConfig.SYNC_START_MINUTE)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        if (calendar.timeInMillis <= now) {
            calendar.add(Calendar.DAY_OF_YEAR, 1) // Schedule for tomorrow if passed
        }
        
        val initialDelay = calendar.timeInMillis - now

        val syncRequest = PeriodicWorkRequestBuilder<StockSyncWorker>(
            AppConfig.SYNC_INTERVAL_VALUE, 
            AppConfig.SYNC_INTERVAL_UNIT
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
            
        workManager.enqueueUniquePeriodicWork(
            "StockSyncHourly",
            ExistingPeriodicWorkPolicy.UPDATE,
            syncRequest
        )
    }
}

/**
 * Main screen composable that displays the favorites dashboard, search bar, and stock details.
 *
 * @param uiState Current state of the stock data fetch.
 * @param favoritesDashboard List of favorited stocks to display in tiles.
 * @param isCurrentFavorite Whether the current stock is in favorites.
 * @param onRefresh Callback to trigger a data refresh.
 * @param onUpdateSymbol Callback to search for a new symbol.
 * @param onToggleFavorite Callback to add/remove the current symbol from favorites.
 * @param onTestNotification Callback to trigger a test notification.
 */
@Composable
fun StockScreen(
    uiState: StockUiState,
    favoritesDashboard: List<FavoriteTileData>,
    isCurrentFavorite: Boolean,
    onRefresh: () -> Unit,
    onUpdateSymbol: (String) -> Unit,
    onToggleFavorite: () -> Unit,
    onTestNotification: () -> Unit
) {
    var textState by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        
        // Favorites Dashboard
        if (favoritesDashboard.isNotEmpty()) {
            Text(
                text = "Favorites Dashboard",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                items(favoritesDashboard) { tile ->
                    DashboardTile(tile) {
                        onUpdateSymbol(tile.symbol)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Search Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = textState,
                onValueChange = { textState = it },
                label = { Text("Symbol (e.g. ^NSEI, AAPL)") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Button(onClick = { 
                if (textState.isNotBlank()) {
                    onUpdateSymbol(textState)
                } 
            }) {
                Text("GO")
            }
        }
        
        // Debug/Test Button
        /*
        Button(
            onClick = onTestNotification,
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            ),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text("Test Notification")
        }
        */
        
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            when (uiState) {
                is StockUiState.Loading -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Fetching Market Data...")
                    }
                }
                is StockUiState.Error -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Error: ${uiState.message}", color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onRefresh) {
                            Text("Retry")
                        }
                    }
                }
                is StockUiState.Success -> {
                    StockContent(
                        data = uiState,
                        isFavorite = isCurrentFavorite,
                        onToggleFavorite = onToggleFavorite
                    )
                }
            }
        }
    }
}

/**
 * Composable for a single dashboard tile representing a favorite stock.
 */
@Composable
fun DashboardTile(data: FavoriteTileData, onClick: () -> Unit) {
    val (color, _) = getSignalColors(data.signal)
    
    Card(
        modifier = Modifier
            .size(width = 100.dp, height = 80.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = data.symbol,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = String.format("%.2f", data.currentPrice),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
            Text(
                text = data.signal.name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }
    }
}

/**
 * Composable that displays the detailed stock information including price and chart.
 */
@Composable
fun StockContent(
    data: StockUiState.Success,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = data.symbol,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurface 
                )
            }
        }
        
        Text(
            text = "Current: ${String.format("%.2f", data.currentPrice)}",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "5-Day Mean: ${String.format("%.2f", data.meanPrice)}",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        // Signal Display
        SignalCard(signal = data.signal, difference = data.difference, percent = data.percentageChange)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Graph
        StockGraph(data.data, data.meanPrice)
    }
}

/**
 * Returns a pair of background and text colors based on the trading signal.
 *
 * @param signal The [Signal] (BUY, SELL, NEUTRAL).
 * @return Pair of [Color] for background and text.
 */
fun getSignalColors(signal: Signal): Pair<Color, Color> {
    return when (signal) {
        Signal.BUY -> Pair(Color(0xFFF44336), Color.White) // Red for Buy
        Signal.SELL -> Pair(Color(0xFF4CAF50), Color.White) // Green for Sell
        Signal.NEUTRAL -> Pair(Color(0xFF2196F3), Color.White) // Blue for Neutral
    }
}

/**
 * Composable displaying a large card with the trading signal and price difference.
 */
@Composable
fun SignalCard(signal: Signal, difference: Double, percent: Double) {
    val infiniteTransition = rememberInfiniteTransition(label = "flash")
    // Only flash if signal is not NEUTRAL
    val alpha = if (signal != Signal.NEUTRAL) {
        infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "flashAlpha"
        ).value
    } else 1.0f

    val (backgroundColor, textColor) = getSignalColors(signal)

    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor.copy(alpha = alpha)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = signal.name,
                style = MaterialTheme.typography.displayMedium,
                color = textColor,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Diff: ${if(difference > 0) "+" else ""}${String.format("%.2f", difference)}",
                style = MaterialTheme.typography.titleMedium,
                color = textColor
            )
            Text(
                text = "${if(percent > 0) "+" else ""}${String.format("%.2f", percent)}%",
                style = MaterialTheme.typography.titleMedium,
                color = textColor
            )
        }
    }
}

/**
 * Composable that renders a line chart of the stock's trend over the last 6 days.
 *
 * @param data List of [DailyClose] data points.
 * @param meanPrice The 5-day mean price to draw as a reference line.
 */
@Composable
fun StockGraph(data: List<DailyClose>, meanPrice: Double? = null) {
    if (data.isEmpty()) return

    val points = data.map { it.close }
    val min = points.minOrNull() ?: 0.0
    val max = points.maxOrNull() ?: 1.0
    
    val actualRange = (max - min)
    val range = if (actualRange == 0.0) 1.0 else actualRange * 1.1
    val effectiveMin = if (actualRange == 0.0) min - 0.5 else min - (range * 0.05)

    Text("Last 6 Days Trend (Gray Dashed = 5-Day Mean)", style = MaterialTheme.typography.labelLarge)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                
                if (points.size > 1) {
                    val xStep = width / (points.size - 1)
                    val path = Path()
                    points.forEachIndexed { index, value ->
                        val x = index * xStep
                        val normalizedValue = (value - effectiveMin) / range
                        val y = height - (normalizedValue * height).toFloat()

                        if (index == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }

                    // Draw Mean reference Line
                    meanPrice?.let { mean ->
                        val normalizedMean = (mean - effectiveMin) / range
                        val yMean = height - (normalizedMean * height).toFloat()
                        
                        drawLine(
                            color = Color.Gray,
                            start = Offset(0f, yMean),
                            end = Offset(width, yMean),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    }

                    // Draw Stock trend Path
                    drawPath(
                        path = path,
                        color = Color.Blue,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw data points
                    points.forEachIndexed { index, value ->
                        val x = index * xStep
                        val normalizedValue = (value - effectiveMin) / range
                        val y = height - (normalizedValue * height).toFloat()
                        
                        drawCircle(
                            color = Color.Blue,
                            center = Offset(x, y),
                            radius = 6.dp.toPx()
                        )
                    }
                }
            }
            
             Row(
                modifier = Modifier.fillMaxSize().align(Alignment.BottomStart),
                 horizontalArrangement = Arrangement.SpaceBetween
            ) {
                 val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
                 if (data.isNotEmpty()) {
                    Text(text = dateFormat.format(Date(data.first().timestamp * 1000)), style = MaterialTheme.typography.labelSmall)
                    Text(text = dateFormat.format(Date(data.last().timestamp * 1000)), style = MaterialTheme.typography.labelSmall)
                 }
             }
        }
    }
}

/**
 * Basic application theme wrapper.
 */
@Composable
fun Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(),
        content = content
    )
}
