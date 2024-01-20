package org.rooftop.shop.domain.product

import org.rooftop.shop.app.product.UndoProduct

data class ProductRollbackEvent(
    val undoProduct: UndoProduct,
)
