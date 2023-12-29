package org.rooftop.shop.service.product

import org.rooftop.api.shop.ProductRegisterReq
import org.rooftop.shop.domain.IdGenerator
import org.rooftop.shop.domain.UserApi
import org.rooftop.shop.domain.product.ProductRepository
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono

@Transactional(readOnly = true)
class ProductService(
    userApi: UserApi,
    idGenerator: IdGenerator,
    productRepository: ProductRepository,
) {

    fun registerProduct(token: String, productRegisterReq: ProductRegisterReq): Mono<Unit> {
        TODO()
    }
}
