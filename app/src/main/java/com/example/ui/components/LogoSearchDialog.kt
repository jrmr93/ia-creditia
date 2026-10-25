package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.ExpenseEntity
import com.example.ui.theme.*
import com.example.util.LogoCandidate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogoSearchDialog(
    expense: ExpenseEntity,
    searchQuery: String,
    isSearching: Boolean,
    candidates: List<LogoCandidate>,
    errorMessage: String?,
    isApplying: Boolean,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onSelectCandidate: (LogoCandidate) -> Unit,
    onApplyDirectUrlOrUri: (String) -> Unit,
    onRemoveLogo: (ExpenseEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var directUrlText by remember { mutableStateOf("") }

    // Launcher para elegir imagen de la galería
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onApplyDirectUrlOrUri(uri.toString())
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(CreditiaPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Buscar logo comercial",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = expense.establishment,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CreditiaPrimary
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Barra de Búsqueda
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Nombre o web del comercio...", fontSize = 13.sp) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = CreditiaPrimary)
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { onQueryChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Borrar", modifier = Modifier.size(18.dp))
                                }
                            }
                            IconButton(onClick = { onSearch(searchQuery) }) {
                                Icon(Icons.Default.Search, contentDescription = "Buscar", tint = CreditiaPrimary)
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch(searchQuery) }),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Botón para elegir imagen de la galería o archivo local
                Button(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CreditiaSurfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp), tint = CreditiaSecondary)
                        Text("Subir imagen desde galería o archivo", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Pegar enlace directo de imagen (URL)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CreditiaSurfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = directUrlText,
                            onValueChange = { directUrlText = it },
                            placeholder = { Text("Pega un enlace web de imagen (URL)...", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Button(
                            onClick = {
                                if (directUrlText.isNotBlank()) {
                                    onApplyDirectUrlOrUri(directUrlText)
                                }
                            },
                            enabled = directUrlText.isNotBlank() && !isApplying,
                            colors = ButtonDefaults.buttonColors(containerColor = CreditiaPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Usar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Encabezado de Resultados
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Imágenes y logos encontrados",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (candidates.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CreditiaPrimary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${candidates.size} opciones",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CreditiaPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isApplying) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CreditiaPrimary.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = CreditiaPrimary, strokeWidth = 2.dp)
                            Text("Gestionando y convirtiendo tamaño de imagen...", fontSize = 12.sp, color = CreditiaPrimary)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Grid de resultados
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when {
                        isSearching -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(color = CreditiaPrimary, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Buscando en múltiples fuentes (Wikipedia, Wikimedia, Web)...",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        errorMessage != null -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = CreditiaTertiary, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = errorMessage,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { onSearch(searchQuery) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CreditiaPrimary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Reintentar", fontSize = 12.sp)
                                }
                            }
                        }
                        candidates.isEmpty() -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "No se encontraron imágenes para este término.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Puedes ingresar otro nombre comercial, pegar un enlace web de imagen o seleccionar una foto desde tu galería.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        else -> {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(candidates) { candidate ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = CreditiaSurfaceContainerLow,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable(enabled = !isApplying) {
                                                onSelectCandidate(candidate)
                                            }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Surface(
                                                modifier = Modifier
                                                    .size(76.dp)
                                                    .clip(RoundedCornerShape(10.dp)),
                                                shape = RoundedCornerShape(10.dp),
                                                color = Color.White,
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                                shadowElevation = 2.dp
                                            ) {
                                                Box(
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    AsyncImage(
                                                        model = candidate.thumbnailUrl ?: candidate.url,
                                                        contentDescription = candidate.title,
                                                        contentScale = ContentScale.Fit,
                                                        modifier = Modifier
                                                            .size(62.dp)
                                                            .padding(4.dp)
                                                    )
                                                }
                                            }

                                            // Badge de fuente (Logo HD, Icono HD, Wikipedia, Wikimedia, Web)
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = when (candidate.source) {
                                                    "Logo HD" -> Color(0xFF6750A4).copy(alpha = 0.14f)
                                                    "Icono HD" -> Color(0xFF00639B).copy(alpha = 0.14f)
                                                    "Wikipedia" -> Color(0xFF5A5F62).copy(alpha = 0.14f)
                                                    "Wikimedia" -> Color(0xFF006A6A).copy(alpha = 0.14f)
                                                    else -> CreditiaPrimary.copy(alpha = 0.12f)
                                                }
                                            ) {
                                                Text(
                                                    text = candidate.source,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when (candidate.source) {
                                                        "Logo HD" -> Color(0xFF6750A4)
                                                        "Icono HD" -> Color(0xFF00639B)
                                                        "Wikipedia" -> Color(0xFF37474F)
                                                        "Wikimedia" -> Color(0xFF006A6A)
                                                        else -> CreditiaPrimary
                                                    },
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }

                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = candidate.title,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 1,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = candidate.domain,
                                                    fontSize = 10.sp,
                                                    maxLines = 1,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Button(
                                                onClick = { onSelectCandidate(candidate) },
                                                enabled = !isApplying,
                                                colors = ButtonDefaults.buttonColors(containerColor = CreditiaPrimary),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth().height(32.dp),
                                                contentPadding = PaddingValues(vertical = 0.dp)
                                            ) {
                                                Text("Elegir", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!expense.establishmentLogoUrl.isNullOrBlank()) {
                        OutlinedButton(
                            onClick = { onRemoveLogo(expense) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                        ) {
                            Text("Quitar logo", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = CreditiaSurfaceContainerHigh),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cerrar", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}
