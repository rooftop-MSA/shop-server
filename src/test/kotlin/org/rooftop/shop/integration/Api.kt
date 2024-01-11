package org.rooftop.shop.integration

import org.rooftop.api.shop.ProductConsumeReq
import org.rooftop.api.shop.ProductRegisterReq
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec

private const val VERSION = "/v1"

internal fun WebTestClient.registerSeller(token: String): ResponseSpec {
    return this.post()
        .uri("$VERSION/sellers")
        .header(HttpHeaders.AUTHORIZATION, token)
        .exchange()
}

internal fun WebTestClient.registerProduct(
    token: String,
    productRegisterReq: ProductRegisterReq,
): ResponseSpec {
    return this.post()
        .uri("$VERSION/products")
        .header(HttpHeaders.AUTHORIZATION, token)
        .bodyValue(productRegisterReq)
        .exchange()
}

internal fun WebTestClient.getProducts(): ResponseSpec {
    return this.get()
        .uri("$VERSION/products")
        .exchange()
}

internal fun WebTestClient.getProducts(lastProductId: Long): ResponseSpec {
    return this.get()
        .uri("$VERSION/products?last-product-id=$lastProductId")
        .exchange()
}

internal fun WebTestClient.getProduct(productId: Long): ResponseSpec {
    return this.get()
        .uri("$VERSION/products/$productId")
        .exchange()
}

internal fun WebTestClient.consumeProducts(productConsumeReq: ProductConsumeReq): ResponseSpec {
    return this.post()
        .uri("$VERSION/products/consumes")
        .bodyValue(productConsumeReq)
        .exchange()
}

