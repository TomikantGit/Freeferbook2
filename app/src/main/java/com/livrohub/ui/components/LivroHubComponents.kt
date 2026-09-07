package com.livrohub.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.livrohub.ui.theme.LivroHubTheme

@Composable
fun LivroHubButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPrimary: Boolean = true,
    icon: @Composable (() -> Unit)? = null
) {
    val backgroundColor = if (isPrimary) LivroHubTheme.colors.primary else LivroHubTheme.colors.secondary
    val contentColor = if (isPrimary) LivroHubTheme.colors.onPrimary else LivroHubTheme.colors.onSecondary
    val shape = RoundedCornerShape(LivroHubTheme.radius.medium)

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(56.dp) // Generous touch target
            .shadow(LivroHubTheme.shadows.elevationLow, shape),
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor,
            disabledContainerColor = backgroundColor.copy(alpha = 0.5f),
            disabledContentColor = contentColor.copy(alpha = 0.5f)
        ),
        contentPadding = PaddingValues(horizontal = LivroHubTheme.spacing.l)
    ) {
        if (icon != null) {
            icon()
            Spacer(modifier = Modifier.width(LivroHubTheme.spacing.s))
        }
        Text(
            text = text,
            style = LivroHubTheme.typography.labelLarge,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
    }
}

@Composable
fun LivroHubCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(LivroHubTheme.radius.large)
    
    // Antigravity / Glassmorphism support: 
    // If the theme is set up with transparency, we apply it here.
    val isGlass = LivroHubTheme.colors.surface.alpha < 1f
    
    val baseModifier = modifier
        .shadow(LivroHubTheme.shadows.elevationMedium, shape)
        .clip(shape)
        .background(LivroHubTheme.colors.surface)
        .let {
            if (isGlass) {
                it.border(1.dp, LivroHubTheme.colors.border.copy(alpha = 0.5f), shape)
            } else {
                it.border(1.dp, LivroHubTheme.colors.border, shape)
            }
        }
        .let { 
            if (onClick != null) it.clickable { onClick() } else it
        }
        
    Column(
        modifier = baseModifier.padding(LivroHubTheme.spacing.m),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LivroHubTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(LivroHubTheme.radius.medium)
    
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = LivroHubTheme.typography.bodyMedium) },
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        shape = shape,
        trailingIcon = trailingIcon,
        textStyle = LivroHubTheme.typography.bodyLarge,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LivroHubTheme.colors.primary,
            unfocusedBorderColor = LivroHubTheme.colors.border,
            focusedLabelColor = LivroHubTheme.colors.primary,
            unfocusedLabelColor = LivroHubTheme.colors.onSurfaceVariant,
            focusedContainerColor = LivroHubTheme.colors.background,
            unfocusedContainerColor = LivroHubTheme.colors.background
        )
    )
}

@Composable
fun LivroHubHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Column(modifier = modifier.padding(vertical = LivroHubTheme.spacing.m)) {
        Text(
            text = title,
            style = LivroHubTheme.typography.headlineMedium,
            color = LivroHubTheme.colors.onBackground,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(LivroHubTheme.spacing.xs))
            Text(
                text = subtitle,
                style = LivroHubTheme.typography.bodyMedium,
                color = LivroHubTheme.colors.onSurfaceVariant
            )
        }
    }
}
