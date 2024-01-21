package org.rooftop.shop.domain.app.product

import org.rooftop.shop.app.product.UndoProduct

fun undoProduct(
    id: Long = 1L,
    consumedQuantity: Long = 2L,
): UndoProduct = UndoProduct(
    id,
    consumedQuantity
)
