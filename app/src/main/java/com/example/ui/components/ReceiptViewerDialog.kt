package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import java.io.File

import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.runtime.mutableIntStateOf

@Composable
fun ReceiptViewerDialog(
    imageUri: String,
    title: String = "Comprobante de compra",
    subtitle: String? = null,
    onDeleteReceipt: ((deletedUri: String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val uriList = remember(imageUri) {
        imageUri.split("|").map { it.trim() }.filter { it.isNotBlank() }
    }
    var currentPhotoIndex by remember { mutableIntStateOf(0) }
    val safeIndex = currentPhotoIndex.coerceIn(0, (uriList.size - 1).coerceAtLeast(0))
    val activePhotoUri = uriList.getOrNull(safeIndex) ?: imageUri

    var scale by remember(activePhotoUri) { mutableFloatStateOf(1f) }
    var offset by remember(activePhotoUri) { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize(1080, 1920)) }

    fun clampOffset(proposedOffset: Offset, currentScale: Float): Offset {
        if (currentScale <= 1f) return Offset.Zero
        val maxBoundX = ((currentScale - 1f) * containerSize.width / 2f + 120f).coerceAtLeast(0f)
        val maxBoundY = ((currentScale - 1f) * containerSize.height / 2f + 180f).coerceAtLeast(0f)
        return Offset(
            x = proposedOffset.x.coerceIn(-maxBoundX, maxBoundX),
            y = proposedOffset.y.coerceIn(-maxBoundY, maxBoundY)
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.96f))
        ) {
            // Imagen central interactiva con zoom enfocado en el punto del gesto y paneo rápido
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { containerSize = it }
                    .pointerInput(activePhotoUri) {
                        detectTapGestures(
                            onDoubleTap = { tapOffset ->
                                if (scale > 1.2f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    val targetScale = 2.8f
                                    val center = Offset(containerSize.width / 2f, containerSize.height / 2f)
                                    val focalFromCenter = tapOffset - center
                                    val targetOffset = -focalFromCenter * (targetScale - 1f)
                                    scale = targetScale
                                    offset = clampOffset(targetOffset, targetScale)
                                }
                            }
                        )
                    }
                    .pointerInput(activePhotoUri) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            val panMultiplier = 1.9f // Multiplicador para mover la imagen mucho más rápido
                            val newScale = (scale * zoom).coerceIn(1f, 7f)
                            if (newScale > 1f) {
                                val scaleFactor = newScale / scale
                                val center = Offset(containerSize.width / 2f, containerSize.height / 2f)
                                val focalFromCenter = centroid - center

                                // Focal zoom: se amplía exactamente la parte donde se realiza el gesto de zoom
                                val zoomedOffset = (offset * scaleFactor) +
                                        (focalFromCenter * (1f - scaleFactor)) +
                                        (pan * panMultiplier)

                                offset = clampOffset(zoomedOffset, newScale)
                            } else {
                                offset = Offset.Zero
                            }
                            scale = newScale
                        }
                    }
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    ),
                contentAlignment = Alignment.Center
            ) {
                val imageModel: Any = if (activePhotoUri.startsWith("/") || activePhotoUri.startsWith("file://")) {
                    File(activePhotoUri.removePrefix("file://"))
                } else {
                    activePhotoUri
                }

                SubcomposeAsyncImage(
                    model = imageModel,
                    contentDescription = "Foto completa del comprobante",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp)
                        }
                    },
                    error = {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No se pudo cargar la imagen del comprobante",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                )
            }

            // Botones flotantes de navegación entre fotos si hay más de 1 comprobante
            if (uriList.size > 1) {
                // Flecha Anterior
                if (safeIndex > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 12.dp)
                    ) {
                        IconButton(
                            onClick = {
                                currentPhotoIndex = (safeIndex - 1).coerceAtLeast(0)
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.65f))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Foto anterior",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Flecha Siguiente
                if (safeIndex < uriList.size - 1) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 12.dp)
                    ) {
                        IconButton(
                            onClick = {
                                currentPhotoIndex = (safeIndex + 1).coerceAtMost(uriList.size - 1)
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.65f))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Siguiente foto",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // Barra superior con título, indicador de fotos y botón de cierre
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (uriList.size > 1) {
                                Surface(
                                    shape = RoundedCornerShape(100.dp),
                                    color = Color(0xFF0284C7)
                                ) {
                                    Text(
                                        text = "${safeIndex + 1}/${uriList.size}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        val subtitleText = if (uriList.size > 1) {
                            "Comprobante ${safeIndex + 1} de ${uriList.size}${if (!subtitle.isNullOrBlank()) " • $subtitle" else ""}"
                        } else {
                            subtitle
                        }
                        if (!subtitleText.isNullOrBlank()) {
                            Text(
                                text = subtitleText,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (scale > 1.05f) {
                            IconButton(
                                onClick = {
                                    scale = 1f
                                    offset = Offset.Zero
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Restablecer zoom",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        if (onDeleteReceipt != null) {
                            IconButton(
                                onClick = { showDeleteConfirmation = true },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444).copy(alpha = 0.35f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Eliminar comprobante",
                                    tint = Color(0xFFFCA5A5),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // Diálogo de confirmación para eliminar foto del comprobante
            if (showDeleteConfirmation) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmation = false },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(32.dp)
                        )
                    },
                    title = {
                        Text(
                            text = "¿Eliminar comprobante?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    },
                    text = {
                        Text(
                            text = "La imagen de este comprobante se borrará permanentemente del almacenamiento interno de la app.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showDeleteConfirmation = false
                                onDeleteReceipt?.invoke(activePhotoUri)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Eliminar", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirmation = false }) {
                            Text("Cancelar")
                        }
                    }
                )
            }

            // Barra inferior con controles y ayuda de gestos
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pellizca hacia cualquier parte • Desliza rápido para mover • Doble toque",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = {
                                val newScale = (scale - 0.5f).coerceAtLeast(1f)
                                scale = newScale
                                offset = clampOffset(offset, newScale)
                            },
                            enabled = scale > 1f,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomOut,
                                contentDescription = "Alejar",
                                tint = if (scale > 1f) Color.White else Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                val newScale = (scale + 0.5f).coerceAtMost(7f)
                                scale = newScale
                                offset = clampOffset(offset, newScale)
                            },
                            enabled = scale < 7f,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomIn,
                                contentDescription = "Acercar",
                                tint = if (scale < 7f) Color.White else Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
