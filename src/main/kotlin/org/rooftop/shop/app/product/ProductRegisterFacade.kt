package org.rooftop.shop.app.product

import org.rooftop.api.shop.ProductRegisterReq
import org.rooftop.api.shop.ProductRegisterRes
import org.rooftop.shop.app.UserApi
import org.rooftop.shop.domain.product.ProductService
import org.rooftop.shop.domain.seller.SellerService
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class ProductRegisterFacade(
    private val userApi: UserApi,
    private val sellerService: SellerService,
    private val productService: ProductService,
) {

    fun registerProduct(token: String, productRegisterReq: ProductRegisterReq): Mono<ProductRegisterRes> {
        return userApi.findUserIdByToken(token)
            .switchIfEmpty(
                Mono.error {
                    throw IllegalArgumentException("Cannot find exists user by token \"$token\"")
                }
            )
            .flatMap { sellerService.findSellerByUserId(it) }
            .switchIfEmpty(
                Mono.error { throw IllegalArgumentException("User not registered seller") }
            )
            .flatMap { productService.registerProduct(it.id, productRegisterReq) }
    }
}
