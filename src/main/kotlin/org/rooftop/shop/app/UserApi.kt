package org.rooftop.shop.app

import reactor.core.publisher.Mono

fun interface UserApi {

    fun findUserIdByToken(token: String): Mono<Long>
}
