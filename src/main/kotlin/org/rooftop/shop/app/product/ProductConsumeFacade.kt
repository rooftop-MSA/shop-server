package org.rooftop.shop.app.product

import org.rooftop.api.shop.ProductConsumeReq
import org.rooftop.netx.api.TransactionManager
import org.rooftop.shop.domain.product.ProductService
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.util.retry.RetrySpec
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

@Service
class ProductConsumeFacade(
    private val productService: ProductService,
    private val transactionManager: TransactionManager,
) {

    fun consumeProduct(productConsumeReq: ProductConsumeReq): Mono<Unit> {
        return joinTransaction(productConsumeReq)
            .consumeProduct(productConsumeReq)
            .commitOnSuccess(productConsumeReq)
            .rollbackOnError(productConsumeReq)
    }

    private fun joinTransaction(productConsumeReq: ProductConsumeReq): Mono<String> {
        return transactionManager.join(
            productConsumeReq.transactionId,
            "type=consumeProduct:productId=${productConsumeReq.productId}:consumeQuantity=${productConsumeReq.consumeQuantity}"
        )
    }

    private fun Mono<String>.consumeProduct(productConsumeReq: ProductConsumeReq): Mono<Unit> {
        return this.flatMap {
            productService.consumeProduct(productConsumeReq)
                .retryWhen(optimisticLockingFailureExceptionSpec)
        }
    }

    private fun <T> Mono<T>.commitOnSuccess(productConsumeReq: ProductConsumeReq): Mono<T> {
        return this.doOnSuccess {
            transactionManager.commit(productConsumeReq.transactionId.toString())
                .subscribeOn(Schedulers.parallel())
                .subscribe()
        }
    }

    private fun <T> Mono<T>.rollbackOnError(productConsumeReq: ProductConsumeReq): Mono<T> {
        return this.doOnError {
            transactionManager.rollback(
                productConsumeReq.transactionId,
                it.message ?: it::class.simpleName!!
            ).subscribeOn(Schedulers.parallel())
                .subscribe()
            throw it
        }
    }

    private companion object Retry {
        private const val RETRY_MOST_100_PERCENT = 1.0

        private val optimisticLockingFailureExceptionSpec =
            RetrySpec.fixedDelay(Long.MAX_VALUE, 50.milliseconds.toJavaDuration())
                .jitter(RETRY_MOST_100_PERCENT)
                .filter { it is OptimisticLockingFailureException }
    }
}
