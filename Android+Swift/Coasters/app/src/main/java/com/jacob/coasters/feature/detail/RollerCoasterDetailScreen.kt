package com.jacob.coasters.feature.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RollerCoasterDetailScreen(
    state: RollerCoasterDetailState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val detail = state.detail
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detail?.coaster?.name.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        when {
            state.isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                }
            }

            state.error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp)
                ) {
                    Text(
                        text = state.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            detail != null -> {
                val uriHandler = LocalUriHandler.current
                val source = detail.coaster.sourceUrl
                val linkColor = MaterialTheme.colorScheme.primary
                val linkWeight = FontWeight.Medium
                val bodyStyle = MaterialTheme.typography.bodySmall
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp)
                ) {
                    AsyncImage(
                        model = detail.coaster.imageSource,
                        contentDescription = detail.coaster.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = detail.coaster.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = detail.coaster.construction,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (detail.coaster.prebuiltDesigns.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Prebuilt designs",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        detail.coaster.prebuiltDesigns.forEach { design ->
                            Text(
                                text = "• $design",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    val annotated = remember(source, linkColor, linkWeight) {
                        buildAnnotatedString {
                            append("Source: ")
                            pushStringAnnotation(tag = "URL", annotation = source)
                            withStyle(
                                SpanStyle(
                                    color = linkColor,
                                    fontWeight = linkWeight
                                )
                            ) {
                                append(source)
                            }
                            pop()
                        }
                    }
                    ClickableText(
                        text = annotated,
                        style = bodyStyle,
                        onClick = { offset ->
                            annotated.getStringAnnotations("URL", offset, offset)
                                .firstOrNull()?.let { uriHandler.openUri(it.item) }
                        }
                    )
                }
            }
        }
    }
}
