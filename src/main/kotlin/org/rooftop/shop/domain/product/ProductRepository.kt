package org.rooftop.shop.domain.product

import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.data.repository.query.Param
import reactor.core.publisher.Flux

interface ProductRepository : R2dbcRepository<Product, Long> {

    @Query("select * from product as p where p.id > :id and p.quantity > 0 order by p.id limit 10")
    fun findProducts(@Param("id") id: Long = 0L): Flux<Product>
}
