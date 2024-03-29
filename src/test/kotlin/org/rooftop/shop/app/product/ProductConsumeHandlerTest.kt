package org.rooftop.shop.app.product

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import org.rooftop.api.shop.productRegisterReq
import org.rooftop.netx.api.TransactionManager
import org.rooftop.order.app.TransactionEventCapture
import org.rooftop.shop.Application
import org.rooftop.shop.app.product.event.orderConfirmEvent
import org.rooftop.shop.domain.product.ProductService
import org.rooftop.shop.integration.MockIdentityServer
import org.rooftop.shop.integration.RedisContainer
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import kotlin.time.Duration.Companion.seconds

@SpringBootTest(classes = [Application::class])
@ContextConfiguration(
    classes = [
        MockIdentityServer::class,
        RedisContainer::class,
        TransactionEventCapture::class,
    ]
)
@DisplayName("ProductConsumeHandler 클래스의")
@TestPropertySource("classpath:application.properties")
internal class ProductConsumeHandlerTest(
    private val productService: ProductService,
    private val transactionManager: TransactionManager,
    private val transactionEventCapture: TransactionEventCapture,
) : DescribeSpec({

    beforeSpec {
        productId = productService.registerProduct(SELLER_ID, productRegisterReq).block()!!.id
    }

    beforeEach {
        transactionEventCapture.clear()
    }

    describe("consumeProduct 메소드는") {
        context("올바른 productConsumeReq 를 받으면,") {

            val transactionId = transactionManager.syncStart()

            it("상품의 재고를 차감한후, 트랜잭션을 커밋한다.") {
                transactionManager.syncCommit(
                    transactionId,
                    orderConfirmEvent(productId = productId, consumedQuantity = 100)
                )

                eventually(5.seconds) {
                    transactionEventCapture.commitShouldBeEqual(1)
                    transactionEventCapture.rollbackShouldBeEqual(0)
                }
            }
        }

        context("남은 수량보다 더 많은 수를 차감하려 한다면,") {
            val transactionId = transactionManager.syncStart()

            it("rollback을 호출한다.") {
                transactionManager.syncCommit(
                    transactionId,
                    orderConfirmEvent(productId = productId, consumedQuantity = 100_000)
                )

                eventually(100.seconds) {
                    transactionEventCapture.commitShouldBeEqual(1)
                    transactionEventCapture.rollbackShouldBeEqual(1)
                }

            }
        }
    }
}) {

    private companion object {
        private const val SELLER_ID = 2L

        private var productId: Long = 0

        private val productRegisterReq = productRegisterReq {
            this.title = "title"
            this.description = "description"
            this.price = 10_000
            this.quantity = 10_000
        }
    }
}
