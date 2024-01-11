package org.rooftop.shop.integration

import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
import io.kotest.matchers.equality.shouldBeEqualUsingFields
import org.rooftop.api.identity.userGetByTokenRes
import org.rooftop.api.shop.*
import org.rooftop.shop.infra.MockIdentityServer
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.reactive.server.WebTestClient
import java.util.stream.IntStream

@AutoConfigureWebTestClient
@DisplayName("상점 도메인의")
@ContextConfiguration(classes = [MockIdentityServer::class])
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableAutoConfiguration(exclude = [RedisReactiveAutoConfiguration::class])
internal class IntegrationTest(
    private val webTestClient: WebTestClient,
    private val mockIdentityServer: MockIdentityServer,
    private val r2dbcEntityTemplate: R2dbcEntityTemplate,
) : DescribeSpec({

    afterEach {
        r2dbcEntityTemplate.clearAll()
    }

    describe("판매자 등록 API는") {
        context("가입된 유저가 판매자 등록을 요청 할 경우,") {

            mockIdentityServer.enqueue(userGetByTokenRes)

            it("판매자 등록을 성공하고 200OK를 반환한다.") {
                val result = webTestClient.registerSeller(AUTHORIZED_TOKEN)

                result.expectStatus().isOk
            }
        }

        context("가입되지 않은 유저가 판매자 등록을 요청 할 경우,") {

            mockIdentityServer.enqueue400BadRequest()

            it("판매자 등록을 실패하고, 400 Bad Request를 반환한다.") {
                val result = webTestClient.registerSeller(UNAUTHORIZED_TOKEN)

                result.expectStatus().isBadRequest
            }
        }
    }

    describe("상품 등록 API는") {
        context("seller가 상품 등록을 요청할 경우,") {

            registerSeller(mockIdentityServer, webTestClient)
            mockIdentityServer.enqueue(userGetByTokenRes)

            it("상품 등록을 성공하고 200OK를 반환한다.") {
                val result = webTestClient.registerProduct(AUTHORIZED_TOKEN, productRegisterReq)

                result.expectStatus().isOk
            }
        }

        context("등록되지 않은 seller가 상품 등록을 요청할 경우,") {

            mockIdentityServer.enqueue(userGetByTokenRes)

            it("400 Bad Request를 반환한다.") {
                val result = webTestClient.registerProduct(UNAUTHORIZED_TOKEN, productRegisterReq)

                result.expectStatus().isBadRequest
            }
        }
    }

    describe("상품 조회 API는") {
        context("존재하는 상품의 id를 입력받으면,") {
            registerSeller(mockIdentityServer, webTestClient)
            registerProducts(1, mockIdentityServer, webTestClient)

            val expectedProduct = toProductRes(webTestClient.getProducts())

            it("상품 정보를 반환한다.") {
                val result = webTestClient.getProduct(expectedProduct.id)

                result.expectStatus().isOk
                    .expectBody(ProductRes::class.java)
                    .returnResult().responseBody!! shouldBeEqualUsingFields expectedProduct
            }
        }
    }

    describe("모든 상품 조회 API는") {
        context("마지막으로 조회된 상품의 id없이 조회를 요청할 경우,") {

            registerSeller(mockIdentityServer, webTestClient)
            registerProducts(20, mockIdentityServer, webTestClient)
            val expectResult = productsRes {
                IntStream.range(0, 10).forEach {
                    this.products.add(product)
                }
            }

            it("재고가 남아있는 첫번째 상품부터 10개의 상품을 조회한다.") {
                val result = webTestClient.getProducts();

                result.expectStatus().isOk
                    .expectBody(ProductsRes::class.java)
                    .returnResult()
                    .responseBody.shouldBeEqualToIgnoringFields(
                        expectResult,
                        ProductsResKt.ProductKt.Dsl::id,
                        ProductsResKt.ProductKt.Dsl::sellerId
                    )
            }
        }

        context("마지막으로 조회된 상품의 id가 주어지면,") {

            registerSeller(mockIdentityServer, webTestClient)
            registerProducts(10, mockIdentityServer, webTestClient)

            val expectResult = productsRes {
                IntStream.range(0, 7).forEach {
                    this.products.add(product)
                }
            }

            val lastProductId = webTestClient.getProducts()
                .expectBody(ProductsRes::class.java)
                .returnResult()
                .responseBody!!.getProducts(2).id

            it("해당 상품 이후로 최대 10개의 상품을 조회한다.") {
                val result = webTestClient.getProducts(lastProductId)

                result.expectStatus().isOk
                    .expectBody(ProductsRes::class.java)
                    .returnResult()
                    .responseBody.shouldBeEqualToIgnoringFields(
                        expectResult,
                        ProductsResKt.ProductKt.Dsl::id,
                        ProductsResKt.ProductKt.Dsl::sellerId
                    )
            }
        }
    }

    describe("상품 재고 차감 API는") {
        context("productId에 해당하는 상품의 quantity가 차감할 quantity보다 크거나 같다면,") {

            registerSeller(mockIdentityServer, webTestClient)
            registerProducts(1, mockIdentityServer, webTestClient)

            val productId = webTestClient.getProducts()
                .expectBody(ProductsRes::class.java)
                .returnResult()
                .responseBody!!.getProducts(0).id

            val productConsumeReq = productConsumeReq {
                this.transactionId = 1L
                this.productId = productId
                this.consumeQuantity = 100
            }

            it("해당 상품 구매에 성공하고 200 OK 를 반환한다.") {
                val result = webTestClient.consumeProducts(productConsumeReq)

                result.expectStatus().isOk
            }
        }

        context("productId에 해당하는 상품의 quantity보다 많은 수를 차감하려 한다면,") {

            registerSeller(mockIdentityServer, webTestClient)
            registerProducts(1, mockIdentityServer, webTestClient)

            val productId = webTestClient.getProducts()
                .expectBody(ProductsRes::class.java)
                .returnResult()
                .responseBody!!.getProducts(0).id

            val productConsumeReq = productConsumeReq {
                this.transactionId = 1L
                this.productId = productId
                this.consumeQuantity = productRegisterReq.quantity + 1
            }

            it("해당 상품 구매를 실패하고, 400 Bad Request 를 반환한다.") {
                val result = webTestClient.consumeProducts(productConsumeReq)

                result.expectStatus().isBadRequest
            }
        }

    }
}) {

    private companion object {
        private const val AUTHORIZED_TOKEN = "AUTHORIZED_TOKEN"
        private const val UNAUTHORIZED_TOKEN = "UNAUTHORIZED_TOKEN"

        private val userGetByTokenRes = userGetByTokenRes {
            id = 1L
            name = "Jennifer"
        }

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

        private val product = ProductsResKt.product {
            this.id = 0L
            this.sellerId = 0L
            this.title = productRegisterReq.title
            this.description = productRegisterReq.description
            this.price = productRegisterReq.price
            this.quantity = productRegisterReq.quantity
        }

        private fun registerSeller(
            mockIdentityServer: MockIdentityServer,
            webTestClient: WebTestClient,
        ) {
            mockIdentityServer.enqueue(userGetByTokenRes)
            webTestClient.registerSeller(AUTHORIZED_TOKEN)
        }

        private fun registerProducts(
            count: Int,
            mockIdentityServer: MockIdentityServer,
            webTestClient: WebTestClient,
        ) {
            IntStream.range(0, count).forEach {
                mockIdentityServer.enqueue(userGetByTokenRes)
                webTestClient.registerProduct(AUTHORIZED_TOKEN, productRegisterReq)
            }
        }

        private fun toProductRes(responseSpec: WebTestClient.ResponseSpec): ProductRes {
            val productInProductsRes = responseSpec.expectBody(ProductsRes::class.java)
                .returnResult().responseBody!!.getProducts(0)

            return productRes {
                this.id = productInProductsRes.id
                this.sellerId = productInProductsRes.sellerId
                this.quantity = productInProductsRes.quantity
                this.price = productInProductsRes.price
                this.title = productInProductsRes.title
                this.description = productInProductsRes.description
            };
        }
    }
}
