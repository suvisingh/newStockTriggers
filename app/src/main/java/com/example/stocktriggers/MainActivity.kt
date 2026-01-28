package com.example.stocktriggers

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel by lazy { MainViewModel() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val uiState by viewModel.uiState.collectAsState()
                    StockScreen(
                        uiState = uiState,
                        onRefresh = { viewModel.fetchData() },
                        onUpdateSymbol = { viewModel.updateSymbol(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun StockScreen(
    uiState: StockUiState,
    onRefresh: () -> Unit,
    onUpdateSymbol: (String) -> Unit
) {
    var textState by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
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
                    StockContent(data = uiState)
                }
            }
        }
    }
}

@Composable
fun StockContent(data: StockUiState.Success) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = data.symbol,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
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
        if (data.signal != Signal.NEUTRAL) {
            SignalCard(signal = data.signal, difference = data.difference, percent = data.percentageChange)
        } else {
            Text(
                text = "Hold / Neutral",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.Gray
            )
            Text(
                 text = "Change: ${String.format("%.2f", data.percentageChange)}% (Diff: ${String.format("%.2f", data.difference)})"
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(onClick = { /* No-op here, refresh logic moved up */ }) {
             Text("Data Loaded")
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        // Graph
        StockGraph(data.data)
    }
}

@Composable
fun SignalCard(signal: Signal, difference: Double, percent: Double) {
    val infiniteTransition = rememberInfiniteTransition(label = "flash")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flashAlpha"
    )

    val (backgroundColor, textColor, label) = when (signal) {
        Signal.BUY -> Triple(Color(0xFF4CAF50), Color.White, "BUY")
        Signal.SELL -> Triple(Color(0xFFF44336), Color.White, "SELL")
        else -> Triple(Color.Gray, Color.Black, "NEUTRAL") // Should not happen here
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor.copy(alpha = alpha)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
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

@Composable
fun StockGraph(data: List<DailyClose>) {
    if (data.isEmpty()) return

    val points = data.map { it.close }
    val min = points.minOrNull() ?: 0.0
    val max = points.maxOrNull() ?: 1.0
    
    // Ensure range is at least 1.0 to avoid division by zero
    val actualRange = (max - min)
    val range = if (actualRange == 0.0) 1.0 else actualRange * 1.1
    val effectiveMin = if (actualRange == 0.0) min - 0.5 else min - (range * 0.05)

    Text("Last 6 Days Trend", style = MaterialTheme.typography.labelLarge)
    
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
                
                // If only one point, we can't draw a line
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

                    drawPath(
                        path = path,
                        color = Color.Blue,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )

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
                 Text(text = dateFormat.format(Date(data.first().timestamp * 1000)), style = MaterialTheme.typography.labelSmall)
                 Text(text = dateFormat.format(Date(data.last().timestamp * 1000)), style = MaterialTheme.typography.labelSmall)
             }
        }
    }
}

@Composable
fun Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(),
        content = content
    )
}
