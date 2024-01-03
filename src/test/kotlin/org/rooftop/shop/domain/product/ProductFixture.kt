package org.rooftop.shop.domain.product

fun product(
    id: Long = 1L,
    sellerId: Long = 2L,
    title: String = "title",
    description: String = "description",
    price: Long = 100L,
    quantity: Long = 100L,
    isNew: Boolean = true,
): Product = Product(
    id = id,
    sellerId = sellerId,
    title = title,
    description = description,
    price = price,
    quantity = quantity,
    isNew = isNew
)
