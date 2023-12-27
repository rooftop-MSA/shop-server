package org.rooftop.shop.domain

fun seller(
    id: Long = 0L,
    userId: Long = 0L,
    isNew: Boolean = true
): Seller = Seller(
    id,
    userId,
    isNew
)
