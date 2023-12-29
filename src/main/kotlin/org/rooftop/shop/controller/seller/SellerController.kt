package org.rooftop.shop.controller.seller

import org.rooftop.api.identity.ErrorRes
import org.rooftop.api.identity.errorRes
import org.rooftop.shop.service.seller.SellerService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
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

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(illegalArgumentException: IllegalArgumentException): Mono<ErrorRes> {
        return Mono.just(errorRes {
            this.message = illegalArgumentException.message!!
        })
    }
}
