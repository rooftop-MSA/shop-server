package org.rooftop.shop.domain.seller

import org.rooftop.shop.domain.seller.Seller

fun seller(
    id: Long = 0L,
    userId: Long = 0L,
    isNew: Boolean = true
): Seller = Seller(
    id,
    userId,
    isNew
)
