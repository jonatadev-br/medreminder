package com.jonataprezende.medreminder.ui.components

import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Medical as IconsMedical
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Chip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jonataprezende.medreminder.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedReminderCard(
    modifier: Modifier = Modifier,
    elevation: androidx.compose.material3.CardElevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    colors: androidx.compose.material3.CardColors = CardDefaults.cardColors(),
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        elevation = elevation,
        colors = colors,
        shape = MaterialTheme.shapes.medium
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CTAButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                Icon(it, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                fontWeight = FontWeight.W600,
                fontSize = 15.sp,
                letterSpacing = 0.008.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.W600,
            fontSize = 15.sp,
            letterSpacing = 0.008.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusChip(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Chip(
        onClick = {},
        modifier = modifier,
        colors = androidx.compose.material3.ChipDefaults.chipColors(
            containerColor = color.copy(alpha = 0.12f),
            contentColor = color
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.W700,
            letterSpacing = 0.05.sp
        )
    }
}

@Composable
fun MedicationColorIcon(
    colorHex: String,
    iconName: String,
    modifier: Modifier = Modifier.size(48.dp)
) {
    val color = Color.Unspecified.copy().let {
        try {
            android.graphics.Color.parseColor(colorHex)
        } catch (e: Exception) {
            MaterialTheme.colorScheme.primary.toArgb()
        }
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(color)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = getMedicationIcon(iconName),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun MedicationColorCircle(
    colorHex: String,
    modifier: Modifier = Modifier.size(40.dp)
) {
    val color = try {
        android.graphics.Color.parseColor(colorHex)
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary.toArgb()
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color(color))
    )
}

private fun getMedicationIcon(iconName: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (iconName) {
        "capsule" -> IconsMedical.Capsule
        "drops" -> IconsMedical.Drops
        "syringe" -> IconsMedical.Syringe
        "inhaler" -> IconsMedical.Inhaler
        "patch" -> IconsMedical.Patch
        else -> IconsMedical.Pill
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.W700,
        letterSpacing = 0.08.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(72.dp))
        androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.W600,
            color = MaterialTheme.colorScheme.onSurface
        )
        androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
        Text(
            text = subtitle,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        actionText?.let { text ->
            onAction?.let { action ->
                androidx.compose.foundation.layout.Spacer(Modifier.height(24.dp))
                CTAButton(text = text, onClick = action, modifier = Modifier.width(280.dp))
            }
        }
    }
}

@Composable
fun SkeletonCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        androidx.compose.animation.core.InfiniteAnimation {
            // Skeleton loading animation handled by background color pulse
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
    }
}