package org.rooftop.shop.controller.product

import org.rooftop.api.identity.ErrorRes
import org.rooftop.api.identity.errorRes
import org.rooftop.api.shop.ProductRegisterReq
import org.rooftop.shop.service.product.ProductService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@RestController
class ProductController(
    private val productService: ProductService,
) {

    @PostMapping("/v1/products")
    @ResponseStatus(HttpStatus.OK)
    fun registerProduct(
        @RequestHeader(HttpHeaders.AUTHORIZATION) token: String,
        @RequestBody productRegisterReq: ProductRegisterReq,
    ): Mono<Unit> = productService.registerProduct(token, productRegisterReq)

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(illegalArgumentException: IllegalArgumentException): Mono<ErrorRes> {
        return Mono.just(errorRes {
            this.message = illegalArgumentException.message!!
        })
    }
}
