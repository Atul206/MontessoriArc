package com.calmcoloring.app.content.remote

import io.ktor.client.HttpClient

/** Platform HTTP engine for [ContentApi] — OkHttp on Android, Darwin (NSURLSession) on iOS. */
expect fun createHttpClient(): HttpClient
