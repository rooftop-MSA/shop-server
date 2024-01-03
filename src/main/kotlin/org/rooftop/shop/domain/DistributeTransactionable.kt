package org.rooftop.shop.domain

import reactor.core.publisher.Mono

interface DistributeTransactionable<T> {

    fun start(state: T): Mono<Long>

    fun join(transactionId: Long, state: T): Mono<Unit>

    fun commit(transactionId: Long): Mono<Unit>

    fun rollback(transactionId: Long): Mono<Unit>

}
