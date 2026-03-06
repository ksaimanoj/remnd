package com.remnd.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.remnd.ui.theme.hslToColor
import com.remnd.viewmodel.ThemeViewModel

private val presetHues = listOf(
    0f   to "Red",
    30f  to "Orange",
    60f  to "Yellow",
    120f to "Green",
    180f to "Teal",
    210f to "Sky",
    240f to "Indigo",
    264f to "Purple",
    300f to "Magenta",
    330f to "Pink",
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ThemeViewModel = hiltViewModel()
) {
    val hue by viewModel.themeHue.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("App Theme Colour", style = MaterialTheme.typography.titleMedium)

            // Colour preview bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                hslToColor(hue, 0.40f, 0.40f),
                                hslToColor(hue, 0.55f, 0.55f),
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Preview",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Hue slider
            Text(
                "Drag the slider to pick a hue",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HueSlider(
                hue = hue,
                onHueChange = { viewModel.setThemeHue(it) },
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()

            // Preset swatches
            Text("Presets", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                presetHues.forEach { (presetHue, name) ->
                    val isSelected = kotlin.math.abs(hue - presetHue) < 5f
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(hslToColor(presetHue, 0.65f, 0.50f))
                                .then(
                                    if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                )
                                .clickable { viewModel.setThemeHue(presetHue) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HueSlider(
    hue: Float,
    onHueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var sliderWidth by remember { mutableStateOf(1f) }
    val rainbowColors = remember {
        listOf(
            hslToColor(0f, 1f, 0.5f),
            hslToColor(30f, 1f, 0.5f),
            hslToColor(60f, 1f, 0.5f),
            hslToColor(90f, 1f, 0.5f),
            hslToColor(120f, 1f, 0.5f),
            hslToColor(150f, 1f, 0.5f),
            hslToColor(180f, 1f, 0.5f),
            hslToColor(210f, 1f, 0.5f),
            hslToColor(240f, 1f, 0.5f),
            hslToColor(270f, 1f, 0.5f),
            hslToColor(300f, 1f, 0.5f),
            hslToColor(330f, 1f, 0.5f),
            hslToColor(360f, 1f, 0.5f),
        )
    }

    Canvas(
        modifier = modifier
            .height(56.dp)
            .onSizeChanged { sliderWidth = it.width.toFloat() }
            .pointerInput(sliderWidth) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    onHueChange((down.position.x / sliderWidth * 360f).coerceIn(0f, 360f))
                    drag(down.id) { change ->
                        onHueChange((change.position.x / sliderWidth * 360f).coerceIn(0f, 360f))
                    }
                }
            }
    ) {
        val trackHeight = 28.dp.toPx()
        val trackTop = (size.height - trackHeight) / 2f

        // Rainbow gradient track
        drawRoundRect(
            brush = Brush.horizontalGradient(rainbowColors),
            topLeft = Offset(0f, trackTop),
            size = Size(size.width, trackHeight),
            cornerRadius = CornerRadius(trackHeight / 2, trackHeight / 2)
        )

        // Thumb position
        val thumbX = (hue / 360f * size.width).coerceIn(14.dp.toPx(), size.width - 14.dp.toPx())
        val thumbY = size.height / 2f

        // Thumb: white outer ring
        drawCircle(color = Color.White, radius = 14.dp.toPx(), center = Offset(thumbX, thumbY))
        // Thumb: colour fill
        drawCircle(
            color = hslToColor(hue, 1f, 0.5f),
            radius = 11.dp.toPx(),
            center = Offset(thumbX, thumbY)
        )
        // Thumb: subtle inner stroke for definition
        drawCircle(
            color = Color.White.copy(alpha = 0.6f),
            radius = 11.dp.toPx(),
            center = Offset(thumbX, thumbY),
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}
