package org.rooftop.shop.app.product

import org.rooftop.api.shop.ProductConsumeReq
import org.rooftop.shop.domain.product.ProductService
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class ProductConsumeFacade(
    private val productService: ProductService,
    private val transactionManager: TransactionManager<UndoProduct>,
) {

    fun consumeProduct(productConsumeReq: ProductConsumeReq): Mono<Unit> {
        return transactionManager.exists(productConsumeReq.transactionId)
            .joinTransaction(productConsumeReq)
            .consumeProduct(productConsumeReq)
            .doOnSuccess {
                transactionManager.commit(productConsumeReq.transactionId.toString())
            }.doOnError {
                transactionManager.rollback(productConsumeReq.transactionId.toString())
                throw it
            }
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
}
