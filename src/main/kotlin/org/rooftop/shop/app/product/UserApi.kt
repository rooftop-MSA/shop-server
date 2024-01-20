package org.rooftop.shop.app.product

import reactor.core.publisher.Mono

fun interface UserApi {

    fun findUserIdByToken(token: String): Mono<Long>
}
