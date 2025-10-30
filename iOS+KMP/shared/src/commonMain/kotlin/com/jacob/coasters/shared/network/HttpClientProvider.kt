package com.jacob.coasters.shared.network

import io.ktor.client.HttpClient

internal expect fun createHttpClient(): HttpClient
