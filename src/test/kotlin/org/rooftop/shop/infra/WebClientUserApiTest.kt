package org.rooftop.shop.infra

import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import org.rooftop.api.identity.userGetByTokenRes
import org.rooftop.shop.app.product.UserApi
import org.springframework.test.context.ContextConfiguration
import reactor.test.StepVerifier

@DisplayName("WebClientUserApi 클래스의")
@ContextConfiguration(classes = [WebClientUserApi::class, MockIdentityServer::class])
internal class WebClientUserApiTest(
    private val userApi: UserApi,
    private val mockIdentityServer: MockIdentityServer = MockIdentityServer(),
) : DescribeSpec({

    describe("findUserIdByToken 메소드는") {
        context("존재하는 유저의 token 을 입력받으면,") {
            val existUser = userGetByTokenRes {
                this.id = 1L
                this.name = "Jennifer"
            }

            mockIdentityServer.enqueue(existUser)

            it("해당 user의 id를 반환한다.") {
                val result = userApi.findUserIdByToken("EXIST_USER_TOKEN")

                StepVerifier.create(result)
                    .expectNext(1L)
                    .verifyComplete()
            }
        }

        context("존재하지 않는 유저의 token을 입력받으면,") {

            mockIdentityServer.enqueue400BadRequest()

            it("Mono.empty를 반환한다.") {
                val result = userApi.findUserIdByToken("NOT_EXIST_USER_TOKEN")

                StepVerifier.create(result)
                    .verifyComplete()
            }
        }
    }
})
