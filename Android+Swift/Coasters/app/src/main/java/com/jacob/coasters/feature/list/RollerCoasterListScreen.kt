package com.jacob.coasters.feature.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.jacob.coasters.model.RollerCoasterListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RollerCoasterListScreen(
    state: RollerCoasterListState,
    onQueryChange: (String) -> Unit,
    onItemSelected: (RollerCoasterListItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        val message = state.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Roller Coasters") }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search") },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (state.isLoading && state.items.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                }
            } else if (!state.isLoading && state.items.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No results",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.items) { item ->
                        RollerCoasterRow(item = item, onItemSelected = onItemSelected)
                    }
                }
            }
        }
    }
}

@Composable
private fun RollerCoasterRow(
    item: RollerCoasterListItem,
    onItemSelected: (RollerCoasterListItem) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemSelected(item) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val imageUrl = item.coaster.imageUrl
        val imageModifier = Modifier
            .padding(end = 12.dp)
            .size(72.dp)
            .clip(RoundedCornerShape(8.dp))
        if (imageUrl.isNotBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = item.coaster.name,
                modifier = imageModifier,
                contentScale = ContentScale.Crop
            )
        } else {
            Spacer(
                modifier = imageModifier
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
        Column {
            Text(
                text = item.coaster.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            val count = item.coaster.prebuiltDesigns.size
            val label = if (count == 1) "roller coaster" else "roller coasters"
            Text(
                text = "$count $label",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
            )
        }
    }
}
