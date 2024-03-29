package org.rooftop.shop.integration

import org.rooftop.shop.domain.product.Product
import org.rooftop.shop.domain.seller.Seller
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate

private const val AUTHORIZED_TOKEN = "AUTHORIZED_TOKEN"
private const val UNAUTHORIZED_TOKEN = "UNAUTHORIZED_TOKEN"

internal fun R2dbcEntityTemplate.clearAll() {
    this.delete(Seller::class.java)
        .all()
        .block()

    this.delete(Product::class.java)
        .all()
        .block()
}
