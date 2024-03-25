package org.rooftop.shop.app.product.event

import org.rooftop.shop.app.product.OrderConfirmEvent

fun orderConfirmEvent(
    payId: Long = 0L,
    orderId: Long = 0L,
    productId: Long = 0L,
    consumedQuantity: Long = 1L,
): OrderConfirmEvent = OrderConfirmEvent(
    payId,
    orderId,
    productId,
    consumedQuantity
)
