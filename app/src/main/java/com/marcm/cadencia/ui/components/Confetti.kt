package com.marcm.cadencia.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.sin
import kotlin.random.Random

/** Aceleración de caída en px/s². */
private const val GRAVITY = 650f

/** Duración total de la celebración (segundos). */
private const val DURATION = 4.5f

/** Paleta festiva: rojo del acento + colores vivos para que destaque. */
private val ConfettiColors = listOf(
    Color(0xFFD64C4C), // acento
    Color(0xFFFFC542), // dorado
    Color(0xFF4CAF50), // verde
    Color(0xFF42A5F5), // azul
    Color(0xFFFF7D76), // coral
    Color(0xFFAB47BC), // morado
    Color(0xFFFFFFFF)  // blanco
)

private class Confetto(
    var x: Float,
    var y: Float,
    val vx: Float,
    var vy: Float,
    val color: Color,
    val w: Float,
    val h: Float,
    var rotation: Float,
    val rotSpeed: Float,
    val swayPhase: Float,
    val swaySpeed: Float,
    val swayAmp: Float
)

private fun generateConfetti(width: Float, height: Float): List<Confetto> =
    List(120) {
        val size = Random.nextFloat() * 14f + 10f // 10..24 px
        Confetto(
            x = Random.nextFloat() * width,
            y = -Random.nextFloat() * height * 0.6f, // arrancan por encima del borde
            vx = (Random.nextFloat() - 0.5f) * 140f,
            vy = Random.nextFloat() * 260f + 140f,
            color = ConfettiColors[Random.nextInt(ConfettiColors.size)],
            w = size,
            h = size * (Random.nextFloat() * 0.4f + 0.5f), // rectangulitos
            rotation = Random.nextFloat() * 360f,
            rotSpeed = (Random.nextFloat() - 0.5f) * 600f,
            swayPhase = Random.nextFloat() * 6.28f,
            swaySpeed = Random.nextFloat() * 4f + 2f,
            swayAmp = Random.nextFloat() * 130f + 40f
        )
    }

/**
 * Lluvia de confeti animada que ocupa todo el espacio disponible. Se reproduce una
 * vez (≈[DURATION] s) y se desvanece al final. No intercepta toques.
 */
@Composable
fun ConfettiOverlay(modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    BoxWithConstraints(modifier.fillMaxSize()) {
        val w = with(density) { maxWidth.toPx() }
        val h = with(density) { maxHeight.toPx() }
        val particles = remember(w, h) {
            if (w > 0f && h > 0f) generateConfetti(w, h) else emptyList()
        }
        var frame by remember { mutableStateOf(0L) }
        var alpha by remember { mutableStateOf(1f) }

        LaunchedEffect(particles) {
            if (particles.isEmpty()) return@LaunchedEffect
            val start = withFrameNanos { it }
            var last = start
            while (true) {
                val now = withFrameNanos { it }
                val dt = ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
                last = now
                val elapsed = (now - start) / 1_000_000_000f
                particles.forEach { p ->
                    p.vy += GRAVITY * dt
                    p.x += p.vx * dt + sin((elapsed + p.swayPhase) * p.swaySpeed) * p.swayAmp * dt
                    p.y += p.vy * dt
                    p.rotation += p.rotSpeed * dt
                }
                alpha = when {
                    elapsed < 3f -> 1f
                    elapsed > DURATION -> 0f
                    else -> 1f - (elapsed - 3f) / (DURATION - 3f)
                }
                frame = now
                if (elapsed > DURATION) break
            }
        }

        Canvas(Modifier.fillMaxSize()) {
            if (frame >= 0L) {
                particles.forEach { p ->
                    rotate(degrees = p.rotation, pivot = Offset(p.x, p.y)) {
                        drawRect(
                            color = p.color.copy(alpha = p.color.alpha * alpha),
                            topLeft = Offset(p.x - p.w / 2f, p.y - p.h / 2f),
                            size = Size(p.w, p.h)
                        )
                    }
                }
            }
        }
    }
}
