package org.rooftop.shop.controller.product

import org.rooftop.api.identity.ErrorRes
import org.rooftop.api.identity.errorRes
import org.rooftop.api.shop.*
import org.rooftop.shop.app.product.ProductRegisterFacade
import org.rooftop.shop.domain.product.ProductService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@RestController
class ProductController(
    private val productService: ProductService,
    private val productRegisterFacade: ProductRegisterFacade,
) {

    @PostMapping("/v1/products")
    @ResponseStatus(HttpStatus.OK)
    fun registerProduct(
        @RequestHeader(HttpHeaders.AUTHORIZATION) token: String,
        @RequestBody productRegisterReq: ProductRegisterReq,
    ): Mono<ProductRegisterRes> = productRegisterFacade.registerProduct(token, productRegisterReq)

    @GetMapping("/v1/products")
    @ResponseStatus(HttpStatus.OK)
    fun findProducts(
        @RequestParam(
            value = "last-product-id",
            defaultValue = "0",
            required = false
        ) lastProductId: Long,
    ): Mono<ProductsRes> {
        return productService.findProducts(lastProductId)
            .map {
                ProductsResKt.product {
                    this.id = it.id
                    this.sellerId = it.sellerId
                    this.title = it.title
                    this.description = it.description
                    this.price = it.price
                    this.quantity = it.getQuantity()
                }
            }
            .collectList()
            .map {
                productsRes {
                    this.products.addAll(it)
                }
            }
    }

    @GetMapping("/v1/products/{id}")
    fun getProductById(@PathVariable("id") id: Long): Mono<ProductRes> {
        return productService.getProductById(id)
            .map {
                productRes {
                    this.id = it.id
                    this.sellerId = it.sellerId
                    this.title = it.title
                    this.description = it.description
                    this.price = it.price
                    this.quantity = it.getQuantity()
                }
            }
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(illegalArgumentException: IllegalArgumentException): Mono<ErrorRes> {
        return Mono.just(errorRes {
            this.message = illegalArgumentException.message!!
        })
    }
}
