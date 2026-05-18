package com.magazines

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val testJson = Json { ignoreUnknownKeys = true }

suspend inline fun <reified T> HttpClient.postJson(
    urlString: String,
    body: T,
    crossinline block: HttpRequestBuilder.() -> Unit = {},
): HttpResponse = post(urlString) {
    contentType(ContentType.Application.Json)
    setBody(testJson.encodeToString(body))
    block()
}

suspend inline fun <reified T> HttpResponse.jsonBody(): T =
    testJson.decodeFromString(bodyAsText())
