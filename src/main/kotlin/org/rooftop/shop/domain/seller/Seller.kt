package org.rooftop.shop.domain.seller

import org.rooftop.shop.domain.BaseEntity
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.PersistenceCreator
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table("seller")
class Seller(
    @Id
    @Column("id")
    val id: Long,

    @Column("user_id")
    val userId: Long,

    isNew: Boolean = false,
) : BaseEntity(isNew) {

    @PersistenceCreator
    constructor(
        id: Long,
        userId: Long,
    ) : this(id, userId, false)

    override fun getId(): Long = id
}
