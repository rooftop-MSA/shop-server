package org.rooftop.shop.app.product

data class OrderConfirmEvent(
    val productId: Long,
    val consumeQuantity: Long,
)
