package org.rooftop.shop.domain.product

class UndoProduct(
    val id: Long,
    val consumedQuantity: Long,
) {
    constructor(product: Product) : this(product.id, product.getQuantity())
}
