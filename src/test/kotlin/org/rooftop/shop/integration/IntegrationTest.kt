package org.rooftop.shop.integration

import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import org.rooftop.api.identity.userGetByTokenRes
import org.rooftop.api.shop.productRegisterReq
import org.rooftop.shop.infra.MockIdentityServer
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.reactive.server.WebTestClient

@AutoConfigureWebTestClient
@DisplayName("상점 도메인의")
@ContextConfiguration(classes = [MockIdentityServer::class])
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class IntegrationTest(
    private val webTestClient: WebTestClient,
    private val mockIdentityServer: MockIdentityServer,
    private val r2dbcEntityTemplate: R2dbcEntityTemplate,
) : DescribeSpec({

    afterEach {
        r2dbcEntityTemplate.clearAll()
    }

    describe("판매자 등록 api는") {
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

            mockIdentityServer.enqueue(userGetByTokenRes)
            webTestClient.registerSeller(AUTHORIZED_TOKEN)
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
    }
}
