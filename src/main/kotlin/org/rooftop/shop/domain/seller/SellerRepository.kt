package org.rooftop.shop.domain.seller

import org.springframework.data.r2dbc.repository.R2dbcRepository

interface SellerRepository: R2dbcRepository<Seller, Long> {
}
