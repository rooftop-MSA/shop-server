package org.rooftop.shop.app.product.event

data class PayCancelEvent(
    val payId: Long,
    val userId: Long,
    val orderId: Long,
    val paidPoint: Long,
)
