package org.rooftop.shop.domain.product

import org.rooftop.api.shop.ProductConsumeReq
import org.rooftop.api.shop.ProductRegisterReq
import org.rooftop.api.shop.ProductRegisterRes
import org.rooftop.api.shop.productRegisterRes
import org.rooftop.shop.domain.IdGenerator
import org.springframework.context.event.EventListener
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.retry.RetrySpec
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

@Service
@Transactional(readOnly = true)
class ProductService(
    private val idGenerator: IdGenerator,
    private val productRepository: ProductRepository,
) {

    @Transactional
    fun registerProduct(
        sellerId: Long,
        productRegisterReq: ProductRegisterReq,
    ): Mono<ProductRegisterRes> {
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
        }.flatMap {
            productRepository.save(it)
        }.map {
            productRegisterRes {
                this.id = it.id
            }
        }
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
        return productRepository.findById(productConsumeReq.productId)
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
            .retryWhen(optimisticLockingFailureExceptionSpec)
            .map { }
    }

    @EventListener(ProductRollbackEvent::class)
    fun rollbackProduct(productRollbackEvent: ProductRollbackEvent): Mono<Unit> {
        return productRepository.findById(productRollbackEvent.undoProduct.id)
            .map {
                it.increaseQuantity(productRollbackEvent.undoProduct.consumedQuantity)
                it
            }
            .flatMap { productRepository.save(it) }
            .retryWhen(jitterSpec)
            .map { }
    }

    private companion object Retry {
        private const val RETRY_MOST_100_PERCENT = 1.0

        private val optimisticLockingFailureExceptionSpec =
            RetrySpec.fixedDelay(Long.MAX_VALUE, 50.milliseconds.toJavaDuration())
                .jitter(RETRY_MOST_100_PERCENT)
                .filter { it is OptimisticLockingFailureException }

        private val jitterSpec =
            RetrySpec.fixedDelay(Long.MAX_VALUE, 50.milliseconds.toJavaDuration())
                .jitter(RETRY_MOST_100_PERCENT)
    }
}
