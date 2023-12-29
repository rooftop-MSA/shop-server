package org.rooftop.shop.integration

import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import org.rooftop.api.identity.userGetByTokenRes
import org.rooftop.shop.infra.MockIdentityServer
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.reactive.server.WebTestClient

@AutoConfigureWebTestClient
@DisplayName("상점 도메인의")
@ContextConfiguration(classes = [MockIdentityServer::class])
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class IntegrationTest(
    private val webTestClient: WebTestClient,
    private val mockIdentityServer: MockIdentityServer,
) : DescribeSpec({

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
                val result = webTestClient.registerSeller(AUTHORIZED_TOKEN)

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
    }
}
