package org.rooftop.shop.domain

import reactor.core.publisher.Mono

fun interface UserApi {

    fun findUserIdByToken(token: String): Mono<Long>
}
