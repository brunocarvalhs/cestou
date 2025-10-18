package br.com.brunocarvalhs.howmuch.app.modules.subscription

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import br.com.brunocarvalhs.domain.entities.PlanSubscription
import br.com.brunocarvalhs.howmuch.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionRoute(
    navController: NavController,
    viewModel: SubscriptionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.menu_subscription)) },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        SubscriptionScreen(
            modifier = Modifier.padding(paddingValues),
            state = state,
            onIntent = viewModel::onIntent,
        )
    }
}

@Composable
fun SubscriptionScreen(
    state: SubscriptionUIState,
    onIntent: (SubscriptionIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    when (state) {
        is SubscriptionUIState.Active -> ActiveSubscriptionScreen(
            modifier = modifier,
            state = state,
            onCancel = {
                onIntent.invoke(SubscriptionIntent.Cancel)
            }
        )

        is SubscriptionUIState.Inactive -> InactiveSubscriptionScreen(
            modifier = modifier,
            onSubscribe = {
                onIntent.invoke(SubscriptionIntent.Subscribe(context as Activity))
            }
        )

        is SubscriptionUIState.Loading -> LoadingScreen(modifier = modifier)
    }
}

@Composable
fun ActiveSubscriptionScreen(
    modifier: Modifier = Modifier,
    state: SubscriptionUIState.Active,
    onCancel: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "You are a Premium Subscriber",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Plano: ${state.plan.name}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Valor: ${state.plan.price}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Renova em: ${state.plan.renewsAt}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(onClick = onCancel) {
            Text(text = "Cancelar Assinatura")
        }
    }
}

@Composable
fun InactiveSubscriptionScreen(
    modifier: Modifier = Modifier,
    plans: List<PlanSubscription> = emptyList(),
    onSubscribe: () -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        item {
            PlanCard(
                isCurrentPlan = true,
                title = "Free",
                price = "$0/mês",
                features = listOf(
                    "Recursos básicos" to true,
                    "Anúncios" to false,
                    "Multiplos dispositivos" to false,
                    "Suporte prioritário" to false
                ),
                isRecommended = false
            )
        }

        items(plans) { plan ->
            PlanCard(
                title = plan.name,
                price = plan.price,
                features = plan.features.map { feature ->
                    feature to true
                },
                isRecommended = plan.isRecommended,
                onSubscribe = onSubscribe
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanCard(
    title: String,
    price: String,
    features: List<Pair<String, Boolean>>,
    isRecommended: Boolean,
    modifier: Modifier = Modifier,
    onSubscribe: (() -> Unit)? = null,
    isCurrentPlan: Boolean = false
) {
    val cardColors = if (isRecommended) CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer
    ) else CardDefaults.cardColors()

    BadgedBox(
        badge = {
            if (isCurrentPlan) {
                Badge {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(R.string.current_plan),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        },
        modifier = modifier.padding(8.dp)
    ) {
        Card(
            colors = cardColors,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = price, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(16.dp))
                features.forEach { (feature, isIncluded) ->
                    FeatureItem(feature, isIncluded)
                }
                onSubscribe?.let {
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = it) {
                        Text(text = stringResource(R.string.subscribe))
                    }
                }
            }
        }
    }
}

@Composable
fun FeatureItem(text: String, isIncluded: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        val icon: ImageVector = if (isIncluded) Icons.Default.Check else Icons.Default.Close
        val iconColor = if (isIncluded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}