package org.rooftop.shop.infra.transaction

import com.github.f4b6a3.tsid.TsidFactory
import org.rooftop.shop.app.product.TransactionIdGenerator
import org.springframework.stereotype.Component

@Component
class TransactionIdGeneratorImpl : TransactionIdGenerator {

    override fun generate(): String = tsidFactory.create().toLong().toString()

    private companion object {
        private val tsidFactory = TsidFactory.newInstance256(100)
    }
}
