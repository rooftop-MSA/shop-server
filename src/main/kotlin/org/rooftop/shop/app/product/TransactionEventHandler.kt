package org.rooftop.shop.app.product

import org.rooftop.netx.api.TransactionRollbackEvent
import org.rooftop.netx.api.TransactionRollbackHandler
import org.rooftop.netx.meta.TransactionHandler
import org.rooftop.shop.domain.product.ProductService
import reactor.core.publisher.Mono
import reactor.util.retry.RetrySpec
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

@TransactionHandler
class TransactionEventHandler(
    private val productService: ProductService,
) {


    @TransactionRollbackHandler
    fun handleTransactionRollbackEvent(transactionRollbackEvent: TransactionRollbackEvent): Mono<Unit> {
        return Mono.just(transactionRollbackEvent)
            .map { parseReplayToMap(transactionRollbackEvent.undo) }
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
                "consumeProduct" -> productService.rollbackProduct(
                    it["productId"]?.toLong()
                        ?: throw IllegalStateException("Type \"consumeProduct\" must have \"productId\" field"),
                    it["consumeQuantity"]?.toLong()
                        ?: throw IllegalStateException("Type \"consumeProduct\" must have \"consumeQuantity\" field")
                ).retryWhen(jitterSpec)

                else -> error("Cannot find matched replay type \"${it["type"]}\"")
            }
        }.map { }
    }

    private companion object Retry {
        private const val RETRY_MOST_100_PERCENT = 1.0

        private val jitterSpec =
            RetrySpec.fixedDelay(Long.MAX_VALUE, 50.milliseconds.toJavaDuration())
                .jitter(RETRY_MOST_100_PERCENT)
    }
}
