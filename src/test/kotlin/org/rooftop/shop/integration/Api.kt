package org.rooftop.shop.integration

import org.rooftop.api.shop.ProductRegisterReq
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient

private const val VERSION = "/v1"

internal fun WebTestClient.registerSeller(token: String): WebTestClient.ResponseSpec {
    return this.post()
        .uri("$VERSION/sellers")
        .header(HttpHeaders.AUTHORIZATION, token)
        .exchange()
}

internal fun WebTestClient.registerProduct(
    token: String,
    productRegisterReq: ProductRegisterReq,
): WebTestClient.ResponseSpec {
    return this.post()
        .uri("$VERSION/products")
        .header(HttpHeaders.AUTHORIZATION, token)
        .bodyValue(productRegisterReq)
        .exchange()
}

internal fun WebTestClient.getProducts(): WebTestClient.ResponseSpec {
    return this.get()
        .uri("$VERSION/products")
        .exchange()
}

internal fun WebTestClient.getProducts(lastProductId: Long): WebTestClient.ResponseSpec {
    return this.get()
        .uri("$VERSION/products?last-product-id=$lastProductId")
        .exchange()
}
