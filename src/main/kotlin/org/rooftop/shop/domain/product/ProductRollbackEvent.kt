package org.rooftop.shop.domain.product

data class ProductRollbackEvent(
    val undoProduct: UndoProduct,
)
