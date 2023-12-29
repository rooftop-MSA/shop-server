package org.rooftop.shop.domain.seller

import reactor.core.publisher.Mono

fun interface SellerConnector {

    fun existsSellerByUserId(userId: Long): Mono<Boolean>
}
