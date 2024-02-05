package org.rooftop.shop.domain.product

data class ProductRollbackEvent(
    val productId: Long,
    val consumeQuantity: Long,
)
