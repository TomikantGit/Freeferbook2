package com.livrohub.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.livrohub.ui.theme.LivroHubTheme
import kotlin.math.max

data class TutorialStep(
    val targetRect: Rect,
    val title: String,
    val description: String
)

@Composable
fun TutorialOverlay(
    steps: List<TutorialStep>,
    isVisible: Boolean,
    onFinish: (dontShowAgain: Boolean) -> Unit
) {
    if (!isVisible || steps.isEmpty()) return

    var currentStepIndex by remember { mutableIntStateOf(0) }
    var dontShowAgain by remember { mutableStateOf(false) }

    val currentStep = steps.getOrNull(currentStepIndex) ?: return

    // Smooth transition for cutout
    val animatedLeft by animateFloatAsState(targetValue = currentStep.targetRect.left, animationSpec = tween(500), label = "left")
    val animatedTop by animateFloatAsState(targetValue = currentStep.targetRect.top, animationSpec = tween(500), label = "top")
    val animatedRight by animateFloatAsState(targetValue = currentStep.targetRect.right, animationSpec = tween(500), label = "right")
    val animatedBottom by animateFloatAsState(targetValue = currentStep.targetRect.bottom, animationSpec = tween(500), label = "bottom")

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    // Prevent clicks from passing through
                }
        ) {
            // Cutout background
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.99f } // Required for BlendMode.Clear to work in Canvas
            ) {
                // Draw dark overlay
                drawRect(
                    color = Color.Black.copy(alpha = 0.7f),
                    size = size
                )

                // Cut out the target area
                val padding = 16.dp.toPx()
                val targetRectWithPadding = Rect(
                    left = max(0f, animatedLeft - padding),
                    top = max(0f, animatedTop - padding),
                    right = animatedRight + padding,
                    bottom = animatedBottom + padding
                )

                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = Offset(targetRectWithPadding.left, targetRectWithPadding.top),
                    size = Size(targetRectWithPadding.width, targetRectWithPadding.height),
                    cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                    blendMode = BlendMode.Clear
                )
            }

            // Tooltip Card placement
            var cardSize by remember { mutableStateOf(IntSize.Zero) }
            val density = LocalDensity.current
            val screenHeight = LocalConfiguration.current.screenHeightDp.dp
            val screenWidth = LocalConfiguration.current.screenWidthDp.dp

            val isTargetAtBottom = currentStep.targetRect.center.y > with(density) { screenHeight.toPx() / 2 }
            
            // Calculate tooltip position
            val tooltipOffset = remember(animatedLeft, animatedTop, animatedRight, animatedBottom, cardSize) {
                val paddingPx = with(density) { 32.dp.toPx() }
                var y = if (isTargetAtBottom) {
                    // Place above
                    animatedTop - paddingPx - cardSize.height
                } else {
                    // Place below
                    animatedBottom + paddingPx
                }

                // Center horizontally relative to target, clamp to screen
                var x = animatedLeft + (animatedRight - animatedLeft) / 2 - cardSize.width / 2f
                val marginPx = with(density) { 16.dp.toPx() }
                
                if (x < marginPx) x = marginPx
                if (x + cardSize.width > with(density) { screenWidth.toPx() } - marginPx) {
                    x = with(density) { screenWidth.toPx() } - cardSize.width - marginPx
                }

                if(y < marginPx) y = marginPx

                IntOffset(x.toInt(), y.toInt())
            }

            // Tooltip Card
            Card(
                modifier = Modifier
                    .onGloballyPositioned { cardSize = it.size }
                    .offset { tooltipOffset }
                    .widthIn(max = 300.dp)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = LivroHubTheme.colors.surface,
                    contentColor = LivroHubTheme.colors.onSurface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = currentStep.title,
                        style = LivroHubTheme.typography.titleMedium,
                        color = LivroHubTheme.colors.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = currentStep.description,
                        style = LivroHubTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { dontShowAgain = !dontShowAgain }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = dontShowAgain,
                            onCheckedChange = { dontShowAgain = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = LivroHubTheme.colors.primary,
                                checkmarkColor = LivroHubTheme.colors.onPrimary
                            )
                        )
                        Text(
                            text = "Não me mostre isto novamente",
                            style = LivroHubTheme.typography.labelSmall,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { onFinish(dontShowAgain) }) {
                            Text("Pular", color = LivroHubTheme.colors.onSurfaceVariant)
                        }
                        
                        Button(
                            onClick = {
                                if (currentStepIndex < steps.size - 1) {
                                    currentStepIndex++
                                } else {
                                    onFinish(dontShowAgain)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LivroHubTheme.colors.primary,
                                contentColor = LivroHubTheme.colors.onPrimary
                            )
                        ) {
                            Text(if (currentStepIndex < steps.size - 1) "Próximo" else "Entendi")
                        }
                    }
                }
            }
        }
    }
}
