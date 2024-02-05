package org.rooftop.shop.app.product

import org.rooftop.netx.api.TransactionRollbackEvent
import org.rooftop.shop.domain.product.ProductRollbackEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class TransactionEventHandler(
    private val applicationEventPublisher: ApplicationEventPublisher,
) {


    @EventListener(TransactionRollbackEvent::class)
    fun handleTransactionRollbackEvent(transactionRollbackEvent: TransactionRollbackEvent): Mono<Unit> {
        return Mono.just(transactionRollbackEvent)
            .map { parseReplayToMap(transactionRollbackEvent.replay) }
            .dispatch()
    }

    private fun parseReplayToMap(replay: String): Map<String, String> {
        val answer = mutableMapOf<String, String>()
        replay.split(":")
            .forEach {
                val line = it.split("=")
                answer[line[0]] = line[1]
            }
        return answer
    }

    private fun Mono<Map<String, String>>.dispatch(): Mono<Unit> {
        return this.doOnNext {
            when (it["type"]) {
                "consumeProduct" -> applicationEventPublisher.publishEvent(
                    ProductRollbackEvent(
                        it["productId"]?.toLong()
                            ?: throw IllegalStateException("Type \"consumeProduct\" must have \"productId\" field"),
                        it["consumeQuantity"]?.toLong()
                            ?: throw IllegalStateException("Type \"consumeProduct\" must have \"consumeQuantity\" field")
                    )
                )

                else -> error("Cannot find matched replay type \"${it["type"]}\"")
            }
        }.map { }
    }
}
