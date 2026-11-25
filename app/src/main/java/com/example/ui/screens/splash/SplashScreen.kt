package com.example.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R as LucideR
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkTextMuted
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.LightBackground
import com.example.ui.theme.LightSurface
import com.example.ui.theme.LightTextMuted
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.ui.theme.LocalDarkTheme
import com.example.ui.theme.MarketZedOrange
import com.example.ui.theme.MarketZedOrangeDark
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(0.75f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch {
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 380)
                )
            }
            launch {
                alpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 320)
                )
            }
        }
        delay(260)
        onSplashFinished()
    }

    val isDark = LocalDarkTheme.current
    val bgColors = if (isDark) {
        listOf(
            DarkBackground,
            DarkSurface,
            Color(0xFF1E120B)
        )
    } else {
        listOf(
            LightSurface,
            LightBackground,
            Color(0xFFFFF3E8)
        )
    }
    val titleColor = if (isDark) DarkTextPrimary else LightTextPrimary
    val subtitleColor = if (isDark) DarkTextSecondary else LightTextSecondary
    val footerColor = if (isDark) DarkTextMuted else LightTextMuted

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = bgColors)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .scale(scale.value)
                .alpha(alpha.value)
                .padding(24.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MarketZedOrange,
                shadowElevation = 10.dp,
                border = BorderStroke(1.dp, MarketZedOrangeDark.copy(alpha = 0.15f)),
                modifier = Modifier.size(92.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(LucideR.drawable.lucide_ic_shopping_bag),
                        contentDescription = "MarketZed Logo",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "MarketZed",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = titleColor
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Zambia's Trusted Online Marketplace",
                style = MaterialTheme.typography.bodyMedium,
                color = subtitleColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "App by Tech Generative Universe (TechGU)",
                style = MaterialTheme.typography.labelSmall,
                color = footerColor
            )
        }
    }
}
