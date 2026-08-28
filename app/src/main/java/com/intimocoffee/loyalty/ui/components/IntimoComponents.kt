package com.intimocoffee.loyalty.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intimocoffee.loyalty.ui.theme.IntimoColors

@Composable
fun IntimoWarmBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(IntimoColors.Background)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        IntimoColors.Caramel.copy(alpha = 0.12f),
                        Color.Transparent,
                    ),
                    startY = 0f,
                    endY = 480f,
                )
            ),
        content = content,
    )
}

@Composable
fun IntimoScreenHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(bottom = 4.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = IntimoColors.Cream,
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = IntimoColors.SubtleText,
            )
        }
    }
}

@Composable
fun IntimoPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        enabled = enabled && !loading,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = IntimoColors.Caramel,
            contentColor = IntimoColors.Espresso,
            disabledContainerColor = IntimoColors.Caramel.copy(alpha = 0.35f),
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = IntimoColors.Espresso,
                strokeWidth = 2.dp,
            )
        } else {
            Text(text, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
        }
    }
}

@Composable
fun IntimoAvatar(initials: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(IntimoColors.Caramel, IntimoColors.CaramelDark)
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initials.take(2).uppercase(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = IntimoColors.Espresso,
        )
    }
}

@Composable
fun IntimoEmptyState(
    emoji: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, fontSize = 40.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = IntimoColors.Cream,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = IntimoColors.SubtleText,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun IntimoNavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (selected) IntimoColors.Caramel else IntimoColors.SubtleText,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) IntimoColors.Cream else IntimoColors.SubtleText,
        )
    }
}
