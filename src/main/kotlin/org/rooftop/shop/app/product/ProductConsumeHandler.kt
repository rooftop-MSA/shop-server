package org.rooftop.shop.app.product

import org.rooftop.netx.api.SagaCommitEvent
import org.rooftop.netx.api.SagaCommitListener
import org.rooftop.netx.meta.SagaHandler
import org.rooftop.shop.app.product.event.PayCancelEvent
import org.rooftop.shop.domain.product.Product
import org.rooftop.shop.domain.product.ProductService
import org.springframework.dao.OptimisticLockingFailureException
import reactor.core.publisher.Mono
import reactor.util.retry.RetrySpec
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

@SagaHandler
class ProductConsumeHandler(
    private val productService: ProductService,
) {

    @SagaCommitListener(event = OrderConfirmEvent::class)
    fun consumeProduct(event: SagaCommitEvent): Mono<Product> {
        return Mono.just(event.decodeEvent(OrderConfirmEvent::class))
            .flatMap { orderConfirmEvent ->
                productService.consumeProduct(
                    orderConfirmEvent.productId,
                    orderConfirmEvent.consumeQuantity,
                ).retryWhen(optimisticLockingFailureExceptionSpec)
            }
            .rollbackOnError(event)
    }

    private fun <T> Mono<T>.rollbackOnError(event: SagaCommitEvent): Mono<T> {
        return this.doOnError {
            val orderConfirmEvent = event.decodeEvent(OrderConfirmEvent::class)
            val payCancelEvent = PayCancelEvent(
                orderConfirmEvent.payId,
                orderConfirmEvent.userId,
                orderConfirmEvent.orderId,
                orderConfirmEvent.totalPrice,
            )
            event.setNextEvent(payCancelEvent)
            throw it
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