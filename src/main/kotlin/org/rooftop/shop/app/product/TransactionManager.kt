package org.rooftop.shop.app.product

import reactor.core.publisher.Mono

interface TransactionManager<T> {

    fun join(transactionId: String, state: T): Mono<String>

    fun exists(transactionId: String): Mono<String>

    fun commit(transactionId: String): Mono<Unit>

    fun rollback(transactionId: String): Mono<Unit>

}
