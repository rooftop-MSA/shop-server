package org.rooftop.shop.domain.product

import org.springframework.data.r2dbc.repository.R2dbcRepository

interface ProductRepository : R2dbcRepository<Product, Long>
