package org.rooftop.shop.infra

import org.rooftop.api.identity.UserGetByTokenRes
import org.rooftop.shop.domain.UserApi
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

@Service
class WebClientUserApi(
    private val webClient: WebClient,
) : UserApi {

    override fun findUserIdByToken(token: String): Mono<Long> {
        return webClient.get()
            .uri(URI)
            .header(HttpHeaders.AUTHORIZATION, token)
            .exchangeToMono { response ->
                when (response.statusCode()) {
                    HttpStatusCode.valueOf(200) -> response.bodyToMono<UserGetByTokenRes>()
                    else -> Mono.empty()
                }
            }
            .map { it.id }
    }

    private companion object {
        private const val URI = "/v1/users/tokens"
    }
}
