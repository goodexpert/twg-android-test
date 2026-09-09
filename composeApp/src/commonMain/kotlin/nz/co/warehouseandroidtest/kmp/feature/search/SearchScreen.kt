package nz.co.warehouseandroidtest.kmp.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import nz.co.warehouseandroidtest.kmp.AppContainer

/**
 * Search input and history. Submitting — by enter or by tapping history — opens the product
 * list with the term.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    container: AppContainer,
    onOpenProductList: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: SearchViewModel = viewModel { SearchViewModel(container.recentSearchStore) }
    val state by viewModel.state.collectAsState()

    // Effects rather than state: navigating on a state field would fire again every time the
    // user came back to this screen, because the field would still be set.
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SearchEffect.OpenProductList -> onOpenProductList(effect.query)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = state.query,
                        onValueChange = { viewModel.onIntent(SearchIntent.QueryChanged(it)) },
                        placeholder = { Text("Search products") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = { viewModel.onIntent(SearchIntent.Submit) },
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            RecentSearches(
                terms = state.recentSearches,
                onSelect = { viewModel.onIntent(SearchIntent.RecentSearchSelected(it)) },
                onClearAll = { viewModel.onIntent(SearchIntent.RecentSearchesCleared) },
            )
        }
    }
}

@Composable
private fun RecentSearches(
    terms: List<String>,
    onSelect: (String) -> Unit,
    onClearAll: () -> Unit,
) {
    if (terms.isEmpty()) {
        Text(
            text = "No recent searches yet.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
        )
        return
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Recent", style = MaterialTheme.typography.titleSmall)
            TextButton(onClick = onClearAll) { Text("Clear") }
        }

        LazyColumn {
            items(items = terms, key = { it }) { term ->
                ListItem(
                    headlineContent = { Text(term) },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.clickable { onSelect(term) },
                )
            }
        }
    }
}
