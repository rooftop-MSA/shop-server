package org.rooftop.shop.app.product

fun interface TransactionIdGenerator {
    fun generate(): String
}
