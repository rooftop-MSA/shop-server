package org.rooftop.shop.controller.seller

import org.rooftop.api.identity.ErrorRes
import org.rooftop.api.identity.errorRes
import org.rooftop.api.shop.SellerRegisterRes
import org.rooftop.shop.app.seller.SellerRegisterFacade
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@RestController
class SellerController(
    private val sellerRegisterFacade: SellerRegisterFacade,
) {

    @PostMapping("/v1/sellers")
    @ResponseStatus(HttpStatus.OK)
    fun registerUserToSeller(
        @RequestHeader(HttpHeaders.AUTHORIZATION) token: String,
    ): Mono<SellerRegisterRes> = sellerRegisterFacade.registerSeller(token)

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(illegalArgumentException: IllegalArgumentException): Mono<ErrorRes> {
        return Mono.just(errorRes {
            this.message = illegalArgumentException.message!!
        })
    }
}
