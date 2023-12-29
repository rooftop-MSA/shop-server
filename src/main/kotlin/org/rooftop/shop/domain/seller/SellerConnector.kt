package org.rooftop.shop.domain.seller

import reactor.core.publisher.Mono

fun interface SellerConnector {

    fun findSellerByUserId(userId: Long): Mono<Seller>
}
