package org.rooftop.shop.infra

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.rooftop.api.identity.UserGetByTokenRes
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.WebClient

@TestConfiguration
class MockIdentityServer {

    private val mockWebServer: MockWebServer = MockWebServer()

    init {
        mockWebServer.start()
    }

    fun enqueue(userGetByTokenRes: UserGetByTokenRes) {
        mockWebServer.enqueue(
            MockResponse().setBody(Buffer().write(userGetByTokenRes.toByteArray()))
                .setResponseCode(200)
                .addHeader(HttpHeaders.CONTENT_TYPE, "application/x-protobuf")
        )
    }

    fun enqueue400BadRequest() {
        mockWebServer.enqueue(
            MockResponse().setResponseCode(400)
        )
    }

    fun getWebClient(): WebClient = WebClient.create(mockWebServer.url("").toString())

    @Bean
    fun testIdentityWebClient(mockIdentityServer: MockIdentityServer): WebClient =
        mockIdentityServer.getWebClient()
}
