package org.rooftop.shop.infra

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.web.reactive.function.client.WebClient

@Configuration
@Profile("prod")
class WebClientConfigurer(
    @Value("\${rooftop.server.identity:http://identity.rooftopmsa.org}") private val identityServerUri: String,
) {

    @Bean
    fun identityWebClient(): WebClient = WebClient.create(identityServerUri)
}
