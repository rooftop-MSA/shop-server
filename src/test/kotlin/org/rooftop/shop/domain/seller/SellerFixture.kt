package org.rooftop.shop.domain.seller

import org.rooftop.shop.domain.seller.Seller

fun seller(
    id: Long = 1L,
    userId: Long = 2L,
    isNew: Boolean = true
): Seller = Seller(
    id,
    userId,
    isNew
)
