package org.rooftop.shop.app.product

import org.rooftop.netx.api.TransactionJoinEvent
import org.rooftop.netx.api.TransactionJoinListener
import org.rooftop.netx.api.TransactionManager
import org.rooftop.netx.meta.TransactionHandler
import org.rooftop.shop.app.product.event.PayCancelEvent
import org.rooftop.shop.domain.product.Product
import org.rooftop.shop.domain.product.ProductService
import org.springframework.dao.OptimisticLockingFailureException
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.util.retry.RetrySpec
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

@TransactionHandler
class ProductConsumeHandler(
    private val productService: ProductService,
    private val transactionManager: TransactionManager,
) {

    @TransactionJoinListener(event = OrderConfirmEvent::class)
    fun consumeProduct(event: TransactionJoinEvent): Mono<Product> {
        return Mono.just(event.decodeEvent(OrderConfirmEvent::class))
            .flatMap { orderConfirmEvent ->
                productService.consumeProduct(
                    orderConfirmEvent.productId,
                    orderConfirmEvent.consumeQuantity,
                ).retryWhen(optimisticLockingFailureExceptionSpec)
            }
            .commitOnSuccess(event)
            .rollbackOnError(event)
            .onErrorResume {
                if (it::class == IllegalArgumentException::class) {
                    return@onErrorResume Mono.empty()
                }
                throw it
            }
    }

    private fun <T> Mono<T>.commitOnSuccess(event: TransactionJoinEvent): Mono<T> {
        return this.doOnSuccess {
            transactionManager.commit(event.transactionId)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe()
        }
    }

    private fun <T> Mono<T>.rollbackOnError(event: TransactionJoinEvent): Mono<T> {
        return this.doOnError {
            val orderConfirmEvent = event.decodeEvent(OrderConfirmEvent::class)
            transactionManager.rollback(
                transactionId = event.transactionId,
                cause = it.message ?: it::class.simpleName!!,
                event = PayCancelEvent(orderConfirmEvent.payId, orderConfirmEvent.productId)
            ).subscribeOn(Schedulers.boundedElastic())
                .subscribe()
        }
    }

    private companion object Retry {
        private const val RETRY_MOST_100_PERCENT = 1.0

        private val optimisticLockingFailureExceptionSpec =
            RetrySpec.fixedDelay(Long.MAX_VALUE, 1000.milliseconds.toJavaDuration())
                .jitter(RETRY_MOST_100_PERCENT)
                .filter { it is OptimisticLockingFailureException }
    }
}
