package org.rooftop.shop.domain.product

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.every
import io.mockk.verify
import org.rooftop.api.shop.productConsumeReq
import org.rooftop.api.shop.productRegisterReq
import org.rooftop.shop.domain.DistributeTransactionable
import org.rooftop.shop.domain.IdGenerator
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
    @MockkBean private val distributeTransaction: DistributeTransactionable<Product>,
) : DescribeSpec({

    every { idGenerator.generate() } returns 1L
    every { productRepository.save(any()) } returns Mono.empty()

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
        context("구매에 성공하면,") {

            val product = product()
            val transactionId = 1L
            val quantity = product.getQuantity()
            val productConsumeReq = productConsumeReq {
                this.transactionId = transactionId
                this.productId = product.id
                this.consumeQuantity = quantity
            }

            every { productRepository.findById(product.id) } returns Mono.just(product)
            every { productRepository.save(product) } returns Mono.just(product)
            every { distributeTransaction.join(transactionId, product) } returns Mono.empty()
            every { distributeTransaction.commit(transactionId) } returns Mono.empty()

            it("분산 트랜잭션을 commit 한다.") {
                val result = productService.consumeProduct(productConsumeReq)

                StepVerifier.create(result)
                    .assertNext {
                        verify(exactly = 1) { distributeTransaction.commit(transactionId) }
                        verify(exactly = 0) { distributeTransaction.rollback(transactionId) }
                    }
                    .verifyComplete()
            }
        }

        context("구매에 실패하면,") {

            val product = product()
            val transactionId = 2L
            val exceedQuantity = product.getQuantity() + 1
            val productConsumeReq = productConsumeReq {
                this.transactionId = transactionId
                this.productId = product.id
                this.consumeQuantity = exceedQuantity
            }

            every { productRepository.findById(product.id) } returns Mono.just(product)
            every { productRepository.save(product) } returns Mono.just(product)
            every { distributeTransaction.join(transactionId, product) } returns Mono.empty()
            every { distributeTransaction.rollback(transactionId) } returns Mono.empty()

            it("분산 트랜잭션을 rollback 한다.") {
                val result = productService.consumeProduct(productConsumeReq)

                StepVerifier.create(result)
                    .then {
                        verify(exactly = 0) { distributeTransaction.commit(transactionId) }
                        verify(exactly = 1) { distributeTransaction.rollback(transactionId) }
                    }
                    .verifyError()
            }
        }
    }
}) {

    private companion object {
        private const val SELLER_ID = 1L
        private const val USER_ID = 1L

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
    }
}
