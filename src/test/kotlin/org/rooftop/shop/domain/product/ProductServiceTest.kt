package org.rooftop.shop.domain.product

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearAllMocks
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.verify
import org.rooftop.api.shop.productConsumeReq
import org.rooftop.api.shop.productRegisterReq
import org.rooftop.shop.domain.IdGenerator
import org.rooftop.shop.domain.TransactionPublisher
import org.rooftop.shop.domain.UserApi
import org.rooftop.shop.domain.seller.SellerConnector
import org.rooftop.shop.domain.seller.seller
import org.rooftop.shop.service.product.ProductService
import org.springframework.test.context.ContextConfiguration
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

@DisplayName("ProductService 클래스의")
@ContextConfiguration(classes = [ProductService::class])
internal class ProductServiceTest(
    private val productService: ProductService,
    @MockkBean private val userApi: UserApi,
    @MockkBean private val idGenerator: IdGenerator,
    @MockkBean private val sellerConnector: SellerConnector,
    @MockkBean private val productRepository: ProductRepository,
    @MockkBean private val transactionPublisher: TransactionPublisher<UndoProduct>,
) : DescribeSpec({

    every { idGenerator.generate() } returns 1L
    every { productRepository.save(any()) } returns Mono.empty()

    afterEach {
        clearAllMocks()
    }

    describe("registerProduct 메소드는") {
        context("seller로 등록된 user의 token과 productRegisterReq가 들어오면,") {

            every { userApi.findUserIdByToken(any()) } returns Mono.just(USER_ID)
            every { sellerConnector.findSellerByUserId(USER_ID) } returns Mono.just(seller(id = SELLER_ID))

            it("새로운 상품 등록에 성공한다.") {
                val result = productService.registerProduct("VALID_TOKEN", productRegisterReq)

                StepVerifier.create(result)
                    .expectNext()
                    .verifyComplete()
            }
        }

        context("존재하지 않는 user의 token이 들어오면,") {

            every { userApi.findUserIdByToken(any()) } returns Mono.empty()

            it("IllegalArgumentException을 던진다.") {
                val result = productService.registerProduct("INVALID_TOKEN", productRegisterReq)

                StepVerifier.create(result)
                    .expectErrorMessage("Cannot find exists user by token \"INVALID_TOKEN\"")
                    .verify()
            }
        }

        context("seller로 등록되지 않은 user의 token이 들어오면,") {

            every { userApi.findUserIdByToken(any()) } returns Mono.just(USER_ID)
            every { sellerConnector.findSellerByUserId(USER_ID) } returns Mono.empty()

            it("IllegalArgumentException을 던진다.") {
                val result = productService.registerProduct("VALID_TOKEN", productRegisterReq)

                StepVerifier.create(result)
                    .expectErrorMessage("User not registered seller")
                    .verify()
            }
        }
    }

    describe("consumeProduct 메소드는") {
        context("product 재고 차감을 성공하면,") {
            every { productRepository.findById(productConsumeReq.productId) } returns
                    Mono.just(product)
            every { productRepository.save(any()) } returns
                    Mono.just(product)

            every { transactionPublisher.join(any(), any()) } returns
                    Mono.just(TRANSACTION_ID)
            every { transactionPublisher.commit(any()) } returns Mono.just(Unit)
            every { transactionPublisher.rollback(any()) } returns Mono.just(Unit)

            it("분산 트랜잭션 서버에 commit을 호출한다.") {
                val result = productService.consumeProduct(productConsumeReq).log()

                StepVerifier.create(result)
                    .assertNext {
                        verify(exactly = 1) {
                            transactionPublisher.join(any(), any())
                        }
                        verify(exactly = 1) { transactionPublisher.commit(any()) }
                        verify(exactly = 0) { transactionPublisher.rollback(any()) }
                        verify(exactly = 1) { productRepository.save(any()) }
                    }
                    .verifyComplete()
            }
        }

        context("product 재고 차감을 실패하면,") {

            every { productRepository.findById(productConsumeReq.productId) } returns
                    Mono.just(product)
            every { productRepository.save(any()) } returns
                    Mono.error(IllegalStateException("Cannot consume quantity"))

            every { transactionPublisher.join(any(), any()) } returns
                    Mono.just(TRANSACTION_ID)
            every { transactionPublisher.commit(any()) } returns Mono.just(Unit)
            every { transactionPublisher.rollback(any()) } returns Mono.just(Unit)

            it("분산 트랜잭션 서버에 rollback 을 호출한다.") {
                val result = productService.consumeProduct(productConsumeReq).log()

                StepVerifier.create(result)
                    .then {
                        verify(exactly = 1) {
                            transactionPublisher.join(any(), any())
                        }
                        verify(exactly = 0) { transactionPublisher.commit(any()) }
                        verify(exactly = 1) { transactionPublisher.rollback(any()) }
                        verify(exactly = 1) { productRepository.save(any()) }
                    }
                    .verifyError()
            }
        }
    }
}) {

    private companion object {
        private const val SELLER_ID = 1L
        private const val USER_ID = 1L
        private const val TRANSACTION_ID = "123"

        private val product = product(quantity = 100)

        private val productRegisterReq = productRegisterReq {
            this.title = "Buy iPhone 15 Pro"
            this.description = """
                Our environmental goals.
                As part of our efforts to reach carbon neutrality by 2030, iPhone 15 Pro and iPhone 15 Pro Max do not include a power adapter or EarPods. Included in the box is a USB‑C Charge Cable that supports fast charging and is compatible with USB‑C power adapters and computer ports.
                We encourage you to use any compatible USB‑C power adapter. If you need a new Apple power adapter or headphones, they are available for purchase.
            """.trimIndent()
            this.price = 1291152L
            this.quantity = 9999999999L
        }

        private val productConsumeReq = productConsumeReq {
            this.productId = 1L
            this.consumeQuantity = 1L
            this.transactionId = TRANSACTION_ID.toLong()
        }
    }
}
