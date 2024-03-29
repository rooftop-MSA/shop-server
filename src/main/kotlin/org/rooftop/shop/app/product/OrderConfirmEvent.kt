package org.rooftop.shop.app.product

data class OrderConfirmEvent(
    val payId: Long,
    val userId: Long,
    val orderId: Long,
    val productId: Long,
    val consumeQuantity: Long,
    val totalPrice: Long,
)
