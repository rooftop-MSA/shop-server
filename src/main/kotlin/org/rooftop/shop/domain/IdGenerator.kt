package org.rooftop.shop.domain

fun interface IdGenerator {

    fun generate(): Long
}
