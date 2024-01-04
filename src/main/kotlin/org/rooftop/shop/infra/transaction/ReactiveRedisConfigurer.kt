package org.rooftop.shop.infra.transaction

import org.rooftop.shop.domain.product.UndoProduct
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class ReactiveRedisConfigurer(
    private val byteArrayRedisSerializer: ByteArrayRedisSerializer,
    @Value("\${distributed.transaction.host.undo-server}") private val undoServerHost: String,
    @Value("\${distributed.transaction.port.undo-server}") private val undoServerPort: String,
    @Value("\${distributed.transaction.host.transaction-server}") private val transactionServerHost: String,
    @Value("\${distributed.transaction.port.transaction-server}") private val transactionServerPort: String,
) {

    @Bean("transactionServer")
    fun transactionServer(): ReactiveRedisTemplate<String, ByteArray> {
        val builder = RedisSerializationContext.newSerializationContext<String, ByteArray>(
            StringRedisSerializer()
        )

        val context = builder.value(byteArrayRedisSerializer).build()

        return ReactiveRedisTemplate(transactionServerConnectionFactory(), context)
    }

    @Bean("transactionServerConnectionFactory")
    fun transactionServerConnectionFactory(): ReactiveRedisConnectionFactory =
        LettuceConnectionFactory(transactionServerHost, transactionServerPort.toInt())

    @Bean("undoServer")
    fun undoServer(): ReactiveRedisTemplate<String, UndoProduct> {
        val builder = RedisSerializationContext.newSerializationContext<String, UndoProduct>(
            StringRedisSerializer()
        )

        val undoProductJacksonSerializer = Jackson2JsonRedisSerializer(UndoProduct::class.java)

        val context = builder.value(undoProductJacksonSerializer).build()

        return ReactiveRedisTemplate(undoServerConnectionFactory(), context)
    }

    @Bean("undoServerConnectionFactory")
    fun undoServerConnectionFactory(): ReactiveRedisConnectionFactory =
        LettuceConnectionFactory(undoServerHost, undoServerPort.toInt())

}
