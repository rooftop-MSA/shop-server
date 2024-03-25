package org.rooftop.shop.app.product.event

data class PayCancelEvent(
    val payId: Long,
    val orderId: Long,
)
