package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.ui.components.AddCardDialog
import com.example.ui.components.CardFormDialog
import com.example.ui.components.CreditiaBottomNav
import com.example.ui.components.CreditiaFab
import com.example.ui.components.CreditiaHeader
import com.example.ui.components.ToastNotification
import com.example.ui.screens.AjustesScreen
import com.example.ui.screens.ConsumosScreen
import com.example.ui.screens.NuevoConsumoScreen
import com.example.ui.screens.SeguridadScreen
import com.example.ui.screens.TarjetasScreen
import com.example.ui.theme.CreditiaTheme
import com.example.ui.viewmodel.CreditiaViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                scrim = android.graphics.Color.WHITE,
                darkScrim = android.graphics.Color.WHITE
            )
        )
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        windowInsetsController.show(WindowInsetsCompat.Type.statusBars())
        windowInsetsController.isAppearanceLightStatusBars = true
        setContent {
            CreditiaTheme {
                CreditiaApp()
            }
        }
    }
}

@Composable
fun CreditiaApp(viewModel: CreditiaViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allExpenses by viewModel.allExpenses.collectAsStateWithLifecycle()

    var isAddingNewExpense by remember { mutableStateOf(false) }

    // Handle back button on subscreen
    BackHandler(enabled = isAddingNewExpense) {
        viewModel.clearEditingExpense()
        isAddingNewExpense = false
    }

    val currentTitle = when {
        isAddingNewExpense -> if (uiState.editingExpense != null) "Editar Consumo" else "Nuevo Consumo"
        uiState.currentTab == "consumos" -> "Consumos"
        uiState.currentTab == "tarjetas" -> "Tarjetas"
        uiState.currentTab == "ajustes" -> "Ajustes"
        else -> "Consumos"
    }

    val isLocked = uiState.isFirstLaunch || !uiState.isVaultUnlocked

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLocked) {
            SeguridadScreen(
                viewModel = viewModel,
                uiState = uiState,
                isLockScreen = true
            )
        } else {
            Scaffold(
                topBar = {
                    CreditiaHeader(
                        title = currentTitle,
                        subtitle = if (uiState.currentTab == "consumos" && !isAddingNewExpense) {
                            if (!uiState.selectedExactDate.isNullOrBlank()) {
                                "Fecha: ${uiState.selectedExactDate}"
                            } else {
                                val currentActive = viewModel.getCurrentActivePeriod(uiState.selectedCard)
                                val periodToShow = if (uiState.selectedPeriod.isNotBlank()) uiState.selectedPeriod else currentActive
                                if (periodToShow == currentActive) "Periodo: $periodToShow (Actual)" else "Periodo: $periodToShow"
                            }
                        } else null,
                        showBackButton = isAddingNewExpense,
                        onBackClick = {
                            viewModel.clearEditingExpense()
                            isAddingNewExpense = false
                        },
                        onSearchClick = {
                            if (!isAddingNewExpense) {
                                if (uiState.currentTab != "consumos") {
                                    viewModel.selectTab("consumos")
                                }
                                viewModel.toggleSearch(!uiState.isSearchActive)
                            }
                        }
                    )
                },
                bottomBar = {
                    if (!isAddingNewExpense) {
                        CreditiaBottomNav(
                            currentTab = uiState.currentTab,
                            onTabSelected = { tab ->
                                viewModel.selectTab(tab)
                            }
                        )
                    }
                },
                floatingActionButton = {
                    if (!isAddingNewExpense && uiState.currentTab in listOf("consumos", "tarjetas", "ajustes")) {
                        CreditiaFab(
                            onClick = {
                                if (uiState.cards.isEmpty()) {
                                    viewModel.openAddCard()
                                    viewModel.showToast("Primero debes registrar una tarjeta de crédito")
                                } else {
                                    viewModel.clearEditingExpense()
                                    isAddingNewExpense = true
                                }
                            },
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isAddingNewExpense) {
                        NuevoConsumoScreen(
                            viewModel = viewModel,
                            uiState = uiState,
                            onBack = {
                                isAddingNewExpense = false
                            }
                        )
                    } else {
                        AnimatedContent(
                            targetState = uiState.currentTab,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "tab_transition"
                        ) { targetTab ->
                            when (targetTab) {
                                "consumos" -> ConsumosScreen(
                                    viewModel = viewModel,
                                    uiState = uiState,
                                    allExpenses = allExpenses,
                                    onNavigateToNewExpense = {
                                        isAddingNewExpense = true
                                    }
                                )

                                "tarjetas" -> TarjetasScreen(
                                    viewModel = viewModel,
                                    uiState = uiState,
                                    allExpenses = allExpenses,
                                    onNavigateToConsumosWithCard = { card ->
                                        viewModel.selectCard(card)
                                        viewModel.selectTab("consumos")
                                    }
                                )

                                "ajustes" -> AjustesScreen(
                                    viewModel = viewModel,
                                    uiState = uiState
                                )

                                else -> ConsumosScreen(
                                    viewModel = viewModel,
                                    uiState = uiState,
                                    allExpenses = allExpenses,
                                    onNavigateToNewExpense = {
                                        isAddingNewExpense = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Global Add Card Dialog
        if (uiState.isAddCardOpen) {
            CardFormDialog(
                cardToEdit = null,
                onDismiss = { viewModel.closeAddCard() },
                onConfirm = { bank, name, network, tier, last4, cut, businessDays, payDay, limit, style ->
                    viewModel.addNewCard(
                        bankName = bank,
                        cardName = name,
                        network = network,
                        tier = tier,
                        last4 = last4,
                        cutDay = cut,
                        payBusinessDays = businessDays,
                        payDay = payDay,
                        creditLimit = limit,
                        style = style
                    )
                }
            )
        }

        // Global Edit Card Dialog
        uiState.editingCard?.let { targetCard ->
            CardFormDialog(
                cardToEdit = targetCard,
                onDismiss = { viewModel.closeEditCard() },
                onConfirm = { bank, name, network, tier, last4, cut, businessDays, payDay, limit, style ->
                    viewModel.updateCardDetails(
                        cardId = targetCard.id,
                        bankName = bank,
                        cardName = name,
                        network = network,
                        tier = tier,
                        last4 = last4,
                        cutDay = cut,
                        payBusinessDays = businessDays,
                        payDay = payDay,
                        creditLimit = limit,
                        style = style
                    )
                }
            )
        }

        // Global Toast Notification
        ToastNotification(
            message = uiState.toastMessage,
            onDismiss = { viewModel.clearToast() }
        )
    }
}
