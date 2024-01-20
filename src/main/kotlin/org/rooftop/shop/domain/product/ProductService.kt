package org.rooftop.shop.domain.product

import org.rooftop.api.shop.ProductConsumeReq
import org.rooftop.api.shop.ProductRegisterReq
import org.rooftop.shop.app.product.TransactionManager
import org.rooftop.shop.app.product.UndoProduct
import org.rooftop.shop.app.product.UserApi
import org.rooftop.shop.domain.IdGenerator
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
@Transactional(readOnly = true)
class ProductService(
    private val userApi: UserApi,
    private val idGenerator: IdGenerator,
    private val productRepository: ProductRepository,
    private val transactionManager: TransactionManager<UndoProduct>,
) {

    @Transactional
    fun registerProduct(
        token: String,
        sellerId: Long,
        productRegisterReq: ProductRegisterReq,
    ): Mono<Unit> {
        return Mono.fromCallable {
            Product(
                idGenerator.generate(),
                sellerId,
                productRegisterReq.title,
                productRegisterReq.description,
                productRegisterReq.price,
                productRegisterReq.quantity,
                isNew = true
            )
        }.flatMap { productRepository.save(it) }
            .map { }
    }

    fun findProducts(lastProductId: Long): Flux<Product> {
        return productRepository.findProducts(lastProductId)
    }

    fun getProductById(id: Long): Mono<Product> {
        return productRepository.findById(id)
            .switchIfEmpty(
                Mono.error { throw IllegalArgumentException("Cannot find product by productId \"$id\"") }
            )
    }

    @Transactional
    fun consumeProduct(productConsumeReq: ProductConsumeReq): Mono<Unit> {
        return transactionManager.join(
            productConsumeReq.transactionId.toString(),
            UndoProduct(productConsumeReq.productId, productConsumeReq.consumeQuantity)
        ).flatMap {
            productRepository.findById(productConsumeReq.productId)
                .switchIfEmpty(
                    Mono.error {
                        IllegalStateException("Cannot find product id \"${productConsumeReq.productId}\"")
                    }
                )
                .map {
                    it.consumeQuantity(productConsumeReq.consumeQuantity)
                    it
                }
                .flatMap {
                    productRepository.save(it)
                }
        }.flatMap {
            transactionManager.commit(productConsumeReq.transactionId.toString())
        }.onErrorResume {
            transactionManager.rollback(productConsumeReq.transactionId.toString())
            Mono.error(it)
        }
    }

    @EventListener(ProductRollbackEvent::class)
    fun rollbackProduct(productRollbackEvent: ProductRollbackEvent): Mono<Unit> {
        return productRepository.findById(productRollbackEvent.undoProduct.id)
            .map {
                it.increaseQuantity(productRollbackEvent.undoProduct.consumedQuantity)
                it
            }
            .flatMap { productRepository.save(it) }
            .map { }
            .retry()
    }
}
