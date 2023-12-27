package org.rooftop.shop.controller

import org.rooftop.shop.service.SellerService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class SellerController(
    private val sellerService: SellerService,
) {

    @PostMapping("/v1/sellers")
    @ResponseStatus(HttpStatus.OK)
    fun registerUserToSeller(
        @RequestHeader(HttpHeaders.AUTHORIZATION) token: String,
    ): Mono<Unit> = sellerService.registerSeller(token)
}
