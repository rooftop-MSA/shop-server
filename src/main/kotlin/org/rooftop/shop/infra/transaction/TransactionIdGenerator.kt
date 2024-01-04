package org.rooftop.shop.infra.transaction

import com.github.f4b6a3.tsid.TsidFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.time.Instant

@Component
class TransactionIdGenerator {

    fun generate(): Mono<String> = Mono.fromCallable {
        Instant.now().toEpochMilli().toString() + "-" + tsidFactory.create().toLong().toString()
    }

    private companion object {
        private val tsidFactory = TsidFactory.newInstance256(100)
    }
}
