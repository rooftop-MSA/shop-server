package org.rooftop.shop.domain

fun interface TransactionIdGenerator {
    fun generate(): String
}
