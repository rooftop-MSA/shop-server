package org.rooftop.shop.service.product

import org.rooftop.api.shop.ProductRegisterReq
import org.rooftop.shop.domain.DistributeTransactionable
import org.rooftop.shop.domain.IdGenerator
import org.rooftop.shop.domain.UserApi
import org.rooftop.shop.domain.product.Product
import org.rooftop.shop.domain.product.ProductRepository
import org.rooftop.shop.domain.seller.SellerConnector
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
@Transactional(readOnly = true)
class ProductService(
    private val userApi: UserApi,
    private val idGenerator: IdGenerator,
    private val sellerConnector: SellerConnector,
    private val productRepository: ProductRepository,
    private val distributeTransaction: DistributeTransactionable<Product>,
) {

    @Transactional
    fun registerProduct(token: String, productRegisterReq: ProductRegisterReq): Mono<Unit> {
        return userApi.findUserIdByToken(token)
            .switchIfEmpty(
                Mono.error {
                    throw IllegalArgumentException("Cannot find exists user by token \"$token\"")
                }
            )
            .flatMap { userId ->
                sellerConnector.findSellerByUserId(userId)
            }
            .switchIfEmpty(
                Mono.error { throw IllegalArgumentException("User not registered seller") }
            )
            .map { seller ->
                Product(
                    idGenerator.generate(),
                    seller.id,
                    productRegisterReq.title,
                    productRegisterReq.description,
                    productRegisterReq.price,
                    productRegisterReq.quantity,
                    isNew = true
                )
            }
            .flatMap { productRepository.save(it) }
            .map { }
    }

    fun findProducts(lastProductId: Long): Flux<Product> {
        return productRepository.findProducts(lastProductId)
    }

    @Transactional
    fun consumeProduct(transactionId: Long, productId: Long, quantity: Long): Mono<Unit> {
        return productRepository.findById(productId)
            .switchIfEmpty(
                Mono.error { throw IllegalArgumentException("Cannot find product \"$productId\"") }
            )
            .doOnNext { distributeTransaction.join(transactionId, it) }
            .doOnNext { it.consumeQuantity(quantity) }
            .flatMap { productRepository.save(it) }
            .doOnSuccess { distributeTransaction.commit(transactionId) }
            .doOnError { distributeTransaction.rollback(transactionId) }
            .map { }
    }
}
