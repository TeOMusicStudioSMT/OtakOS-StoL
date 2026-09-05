package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import kotlin.math.cos
import kotlin.math.sin

/**
 * Tryb oświetlenia perłowego marmurowego stołu Katedry OtakOS.
 */
enum class TableLightingMode(val displayName: String) {
    PEARL_OPAL("Perłowy Opal"),
    OBSIDIAN_NIGHT("Nocny Neoryt"),
    GOLDEN_DAWN("Złoty Blask Katedry")
}

/**
 * Zaawansowane tło stołu z perłowego marmuru z proceduralnym cieniowaniem,
 * opalizującymi refleksami, fazowaną krawędzią (beveled edge) oraz
 * dynamicznym punktowym oświetleniem reagującym na dotyk.
 */
@Composable
fun MarbleBackground(
    modifier: Modifier = Modifier,
    lightingMode: TableLightingMode = TableLightingMode.PEARL_OPAL,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isEffectiveDark = lightingMode == TableLightingMode.OBSIDIAN_NIGHT || (lightingMode == TableLightingMode.PEARL_OPAL && isSystemDark)

    // Dynamic light sweep animation (przemieszczający się refleks świetlny po wypolerowanym kamieniu)
    val infiniteTransition = rememberInfiniteTransition(label = "marble_specular_transition")
    val sweepProgress by infiniteTransition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "specular_sweep"
    )

    // Pulsing subtle ambient breathing of the OtakOS inscribed node
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_pulse"
    )

    // Interactive tactile touch point light (światło wodzące za dotykiem użytkownika)
    var touchPoint by remember { mutableStateOf<Offset?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        touchPoint = offset
                        tryAwaitRelease()
                        touchPoint = null
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset -> touchPoint = offset },
                    onDrag = { change, _ ->
                        touchPoint = change.position
                        change.consume()
                    },
                    onDragEnd = { touchPoint = null },
                    onDragCancel = { touchPoint = null }
                )
            }
    ) {
        // 1. High-fidelity vertical marble image base
        Image(
            painter = painterResource(id = R.drawable.img_pearl_marble_vertical),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 2. Advanced Multi-layer Lighting & Shading Canvas overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()

                    val canvasWidth = size.width
                    val canvasHeight = size.height

                    // 2a. Global Ambient Shading & Vignette (Cieniowanie brzegowe i głębia stołu)
                    val vignetteBrush = when (lightingMode) {
                        TableLightingMode.PEARL_OPAL -> Brush.radialGradient(
                            colors = listOf(
                                Color(0x00FFFFFF),
                                Color(0x15F1F5F9),
                                Color(0x40E2E8F0),
                                Color(0x8094A3B8)
                            ),
                            center = Offset(canvasWidth * 0.5f, canvasHeight * 0.35f),
                            radius = canvasHeight * 0.85f
                        )
                        TableLightingMode.OBSIDIAN_NIGHT -> Brush.radialGradient(
                            colors = listOf(
                                Color(0x250284C7),
                                Color(0x600B0F17),
                                Color(0xC0070A0F),
                                Color(0xF2030508)
                            ),
                            center = Offset(canvasWidth * 0.5f, canvasHeight * 0.35f),
                            radius = canvasHeight * 0.9f
                        )
                        TableLightingMode.GOLDEN_DAWN -> Brush.radialGradient(
                            colors = listOf(
                                Color(0x35FEF3C7),
                                Color(0x20FDE68A),
                                Color(0x45D97706),
                                Color(0x8578350F)
                            ),
                            center = Offset(canvasWidth * 0.5f, canvasHeight * 0.35f),
                            radius = canvasHeight * 0.85f
                        )
                    }
                    drawRect(brush = vignetteBrush)

                    // 2b. Opalescent Iridescent Sheen (Opalizujący połysk perły: fiolet-róż-błękit-złoto)
                    val iridescentBrush = when (lightingMode) {
                        TableLightingMode.PEARL_OPAL -> Brush.linearGradient(
                            colors = listOf(
                                Color(0x12E0F2FE), // Ice cyan
                                Color(0x18FCE7F3), // Opal pink
                                Color(0x10FEF3C7), // Pearl gold
                                Color(0x14E0E7FF), // Lavender
                                Color(0x12E0F2FE)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(canvasWidth, canvasHeight)
                        )
                        TableLightingMode.OBSIDIAN_NIGHT -> Brush.linearGradient(
                            colors = listOf(
                                Color(0x1538BDF8),
                                Color(0x15C084FC),
                                Color(0x102DD4BF),
                                Color(0x180F172A)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(canvasWidth, canvasHeight)
                        )
                        TableLightingMode.GOLDEN_DAWN -> Brush.linearGradient(
                            colors = listOf(
                                Color(0x25FDE68A),
                                Color(0x18F43F5E),
                                Color(0x20FBBF24),
                                Color(0x15FDE68A)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(canvasWidth, canvasHeight)
                        )
                    }
                    drawRect(brush = iridescentBrush, blendMode = BlendMode.Screen)

                    // 2c. Dynamic Specular Light Band sweeping across the glossy marble
                    val sweepX = canvasWidth * sweepProgress
                    val specularBandBrush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x06FFFFFF),
                            if (isEffectiveDark) Color(0x28FFFFFF) else Color(0x38FFFFFF),
                            Color(0x06FFFFFF),
                            Color.Transparent
                        ),
                        start = Offset(sweepX - 250f, 0f),
                        end = Offset(sweepX + 250f, canvasHeight)
                    )
                    drawRect(brush = specularBandBrush, blendMode = BlendMode.Plus)

                    // 2d. Inscribed OtakOS Sacred Geometry on the marble table (geometryczne inskrypcje i grawerunki)
                    val tableCenter = Offset(canvasWidth / 2f, canvasHeight * 0.42f)
                    val baseRadius = canvasWidth * 0.38f * pulseScale

                    val inscriptionColor = when (lightingMode) {
                        TableLightingMode.PEARL_OPAL -> Color(0x280284C7)
                        TableLightingMode.OBSIDIAN_NIGHT -> Color(0x3538BDF8)
                        TableLightingMode.GOLDEN_DAWN -> Color(0x40D97706)
                    }

                    val goldAccentColor = when (lightingMode) {
                        TableLightingMode.OBSIDIAN_NIGHT -> Color(0x25FBBF24)
                        else -> Color(0x30B45309)
                    }

                    // Outer perimeter marble grove
                    drawCircle(
                        color = inscriptionColor,
                        radius = baseRadius * 1.35f,
                        center = tableCenter,
                        style = Stroke(width = 2.5f)
                    )

                    // Concentric fine ring
                    drawCircle(
                        color = goldAccentColor,
                        radius = baseRadius * 1.15f,
                        center = tableCenter,
                        style = Stroke(width = 1.2f)
                    )

                    // Inner energy disc
                    drawCircle(
                        color = inscriptionColor.copy(alpha = 0.08f),
                        radius = baseRadius * 0.9f,
                        center = tableCenter
                    )

                    // Radiating subtle tick marks around the table rim
                    val tickCount = 24
                    for (i in 0 until tickCount) {
                        val angle = (i * 360f / tickCount) * (Math.PI / 180f).toFloat()
                        val r1 = baseRadius * 1.30f
                        val r2 = baseRadius * 1.35f
                        val p1 = Offset(tableCenter.x + r1 * cos(angle), tableCenter.y + r1 * sin(angle))
                        val p2 = Offset(tableCenter.x + r2 * cos(angle), tableCenter.y + r2 * sin(angle))
                        drawLine(
                            color = inscriptionColor,
                            start = p1,
                            end = p2,
                            strokeWidth = if (i % 3 == 0) 2.2f else 1.0f
                        )
                    }

                    // 2e. 3D Architectural Bevel of the Marble Slab (Fazowana krawędź stołu)
                    val inset = 8f
                    val cornerRad = 32f

                    // Highlight rim (Górno-lewe światło odbite na krawędzi fazy)
                    val rimHighlightBrush = Brush.linearGradient(
                        colors = listOf(
                            if (isEffectiveDark) Color(0x70FFFFFF) else Color(0xB0FFFFFF),
                            Color(0x30FFFFFF),
                            Color(0x05FFFFFF),
                            if (isEffectiveDark) Color(0x20000000) else Color(0x30000000)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(canvasWidth, canvasHeight)
                    )
                    drawRoundRect(
                        brush = rimHighlightBrush,
                        topLeft = Offset(inset, inset),
                        size = Size(canvasWidth - inset * 2, canvasHeight - inset * 2),
                        cornerRadius = CornerRadius(cornerRad, cornerRad),
                        style = Stroke(width = 3.5f)
                    )

                    // Subtle inner shadow along bottom edge of slab (Cień wewnętrzny krawędzi)
                    val rimShadowBrush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            if (isEffectiveDark) Color(0x60000000) else Color(0x35000000)
                        ),
                        startY = canvasHeight - 120f,
                        endY = canvasHeight
                    )
                    drawRect(brush = rimShadowBrush)

                    // 2f. Interactive Touch Luminescence (Światło punktowe pod palcem użytkownika)
                    touchPoint?.let { pos ->
                        val touchLightBrush = Brush.radialGradient(
                            colors = listOf(
                                if (isEffectiveDark) Color(0x9038BDF8) else Color(0x70BAE6FD),
                                if (isEffectiveDark) Color(0x4038BDF8) else Color(0x3538BDF8),
                                Color.Transparent
                            ),
                            center = pos,
                            radius = 240f
                        )
                        drawCircle(
                            brush = touchLightBrush,
                            radius = 240f,
                            center = pos,
                            blendMode = BlendMode.Plus
                        )
                    }
                }
        )

        // 3. User interface components laid gracefully over the polished marble table
        content()
    }
}
