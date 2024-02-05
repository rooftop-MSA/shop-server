package org.rooftop.shop.domain.app.product

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equality.shouldBeEqualUsingFields
import io.mockk.every
import io.mockk.verify
import org.rooftop.api.shop.productConsumeReq
import org.rooftop.api.shop.productRegisterReq
import org.rooftop.api.shop.productRegisterRes
import org.rooftop.netx.api.TransactionManager
import org.rooftop.shop.app.product.ProductConsumeFacade
import org.rooftop.shop.domain.product.ProductService
import org.rooftop.shop.integration.MockIdentityServer
import org.rooftop.shop.integration.RedisContainer
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

@SpringBootTest
@DisplayName("ProductConsumeFacade 클래스의")
@TestPropertySource("classpath:application.properties")
@ContextConfiguration(
    classes = [
        MockIdentityServer::class,
        RedisContainer::class
    ]
)
internal class ProductConsumeFacadeTest(
    private val productService: ProductService,
    private val productConsumeFacade: ProductConsumeFacade,
    @MockkBean private val transactionManager: TransactionManager,
) : DescribeSpec({

    beforeSpec {
        PRODUCT_ID = productService.registerProduct(SELLER_ID, productRegisterReq).block()!!.id
    }

    every { transactionManager.exists(TRANSACTION_ID) } returns Mono.just(TRANSACTION_ID)
    every { transactionManager.join(TRANSACTION_ID, any()) } returns Mono.just(TRANSACTION_ID)
    every { transactionManager.commit(TRANSACTION_ID) } returns Mono.just(TRANSACTION_ID)
    every { transactionManager.rollback(TRANSACTION_ID, any()) } returns Mono.just(TRANSACTION_ID)

    describe("consumeProduct 메소드는") {
        context("올바른 productConsumeReq 를 받으면,") {

            val productConsumeReq = productConsumeReq {
                this.productId = PRODUCT_ID
                this.consumeQuantity = 100
                this.transactionId = TRANSACTION_ID
            }

            val expected = productRegisterRes {
                this.id = PRODUCT_ID
            }

            it("분산 트랜잭션에 참여하고, 상품의 재고를 차감한후, 트랜잭션을 커밋한다.") {
                val result = productConsumeFacade.consumeProduct(productConsumeReq)

                StepVerifier.create(result)
                    .assertNext {
                        verify(exactly = 1) { transactionManager.commit(TRANSACTION_ID) }
                        verify(exactly = 0) { transactionManager.rollback(TRANSACTION_ID, any()) }
                        it shouldBeEqualUsingFields expected
                    }
            }
        }

        context("남은 수량보다 더 많은 수를 차감하려 한다면,") {
            val productConsumeReq = productConsumeReq {
                this.productId = PRODUCT_ID
                this.consumeQuantity = Long.MAX_VALUE
                this.transactionId = TRANSACTION_ID
            }

            it("rollback을 호출한다.") {
                val result = productConsumeFacade.consumeProduct(productConsumeReq)

                StepVerifier.create(result)
                    .then {
                        verify(exactly = 0) { transactionManager.commit(TRANSACTION_ID) }
                        verify(exactly = 1) { transactionManager.rollback(TRANSACTION_ID, any()) }
                    }
                    .expectError()
            }
        }
    }
}) {

    private companion object {

        private const val TRANSACTION_ID = "123"
        private const val SELLER_ID = 2L

        private var PRODUCT_ID: Long = 0

        private val productRegisterReq = productRegisterReq {
            this.title = "title"
            this.description = "description"
            this.price = 10_000
            this.quantity = 10_000
        }
    }
}
