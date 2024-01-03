package org.rooftop.shop.domain.seller

fun seller(
    id: Long = 1L,
    userId: Long = 2L,
    isNew: Boolean = true,
): Seller = Seller(
    id,
    userId,
    isNew
)
