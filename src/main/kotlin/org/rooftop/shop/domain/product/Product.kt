package org.rooftop.shop.domain.product

import org.rooftop.shop.domain.BaseEntity
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.PersistenceCreator
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table("product")
class Product(
    @Id
    @Column("id")
    val id: Long,

    @Column("seller_id")
    val sellerId: Long,

    @Column("title")
    val title: String,

    @Column("description")
    val description: String,

    @Column("price")
    val price: Long,

    @Column("quantity")
    val quantity: Long,

    @Version
    private var version: Int? = null,

    isNew: Boolean = false,
) : BaseEntity(isNew = isNew) {

    @PersistenceCreator
    constructor(
        id: Long,
        sellerId: Long,
        title: String,
        description: String,
        price: Long,
        quantity: Long,
    ) : this(id, sellerId, title, description, price, quantity, isNew = false)

    override fun getId(): Long = id

}
