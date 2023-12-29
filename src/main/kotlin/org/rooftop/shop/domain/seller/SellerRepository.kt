package org.rooftop.shop.domain.seller

import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Mono

interface SellerRepository : R2dbcRepository<Seller, Long> {

    fun findByUserId(userId: Long): Mono<Seller>
}
