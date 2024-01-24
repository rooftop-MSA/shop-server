package org.rooftop.shop.app.product

import org.rooftop.api.shop.ProductConsumeReq
import org.rooftop.shop.domain.product.ProductService
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@Service
class ProductConsumeFacade(
    private val productService: ProductService,
    private val transactionManager: TransactionManager<UndoProduct>,
) {

    fun consumeProduct(productConsumeReq: ProductConsumeReq): Mono<Unit> {
        return transactionManager.exists(productConsumeReq.transactionId)
            .joinTransaction(productConsumeReq)
            .consumeProduct(productConsumeReq)
            .commitOnSuccess(productConsumeReq)
            .rollbackOnError(productConsumeReq)
    }

    private fun Mono<String>.joinTransaction(productConsumeReq: ProductConsumeReq): Mono<String> {
        return this.flatMap {
            transactionManager.join(
                productConsumeReq.transactionId,
                UndoProduct(productConsumeReq.productId, productConsumeReq.consumeQuantity)
            )
        }
    }

    private fun Mono<String>.consumeProduct(productConsumeReq: ProductConsumeReq): Mono<Unit> {
        return this.flatMap {
            productService.consumeProduct(productConsumeReq)
        }
    }

    private fun <T> Mono<T>.commitOnSuccess(productConsumeReq: ProductConsumeReq): Mono<T> {
        return this.doOnSuccess {
            transactionManager.commit(productConsumeReq.transactionId.toString())
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe()
        }
    }

    private fun <T> Mono<T>.rollbackOnError(productConsumeReq: ProductConsumeReq): Mono<T> {
        return this.doOnError {
            transactionManager.rollback(productConsumeReq.transactionId)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe()
            throw it
        }
    }
}
