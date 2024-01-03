package org.rooftop.shop.infra

import org.rooftop.shop.domain.DistributeTransactionable
import org.rooftop.shop.domain.IdGenerator
import org.rooftop.shop.domain.product.Product
import org.rooftop.shop.domain.product.ProductRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono

/*
    분산트랜잭션 서버 만들고 구현 하기
    지금은 inmemory로 구현
 */
@Service
class ProductDistributeTransaction(
    private val idGenerator: IdGenerator,
    private val undoLog: MutableMap<Long, Product>,
    private val productRepository: ProductRepository,
) : DistributeTransactionable<Product> {

    override fun start(state: Product): Mono<Long> {
        return Mono.fromCallable { idGenerator.generate() }
            .doOnNext { transactionId ->
                undoLog[transactionId] = state
            }
    }

    override fun join(transactionId: Long, state: Product): Mono<Unit> {
        return Mono.fromCallable { undoLog[transactionId] = state }
    }

    override fun commit(transactionId: Long): Mono<Unit> {
        return Mono.fromCallable { undoLog.remove(transactionId) }
    }

    @Transactional
    override fun rollback(transactionId: Long): Mono<Unit> {
        return Mono.fromCallable {
            undoLog.getOrElse(transactionId) {
                throw IllegalStateException(
                    "Cannot find distributed transaction id \"$transactionId\""
                )
            }
        }.flatMap {
            productRepository.save(it)
        }.map { }
    }
}
