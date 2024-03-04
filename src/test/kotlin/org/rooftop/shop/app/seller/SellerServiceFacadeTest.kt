package org.rooftop.shop.app.seller

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equality.shouldBeEqualUsingFields
import io.mockk.every
import org.rooftop.api.shop.sellerRegisterRes
import org.rooftop.shop.app.UserApi
import org.rooftop.shop.app.seller.SellerRegisterFacade
import org.rooftop.shop.domain.seller.SellerService
import org.springframework.test.context.ContextConfiguration
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

@DisplayName("SellerServiceFacde 클래스의")
@ContextConfiguration(classes = [SellerRegisterFacade::class])
internal class SellerServiceFacadeTest(
    private val sellerRegisterFacade: SellerRegisterFacade,
    @MockkBean private val userApi: UserApi,
    @MockkBean private val sellerService: SellerService,
) : DescribeSpec({

    every { userApi.findUserIdByToken(any()) } returns Mono.empty()
    every { userApi.findUserIdByToken(EXIST_USER_TOKEN) } returns Mono.just(EXIST_USER_ID)

    every { sellerService.registerSeller(EXIST_USER_ID) } returns Mono.just(sellerRegisterRes)

    describe("registerSeller 메소드는") {
        context("존재하는 유저의 토근이 주어지면,") {
            it("Seller 저장에 성공한다") {
                val result = sellerRegisterFacade.registerSeller(EXIST_USER_TOKEN)

                StepVerifier.create(result)
                    .assertNext {
                        it shouldBeEqualUsingFields sellerRegisterRes
                    }
                    .verifyComplete()
            }
        }

        context("존재하지 않는 유저의 토큰이 주어지면,") {

            it("IllegalArgumentException 을 던진다.") {
                val result = sellerRegisterFacade.registerSeller("NOT_EXIST_USER_TOKEN");

                StepVerifier.create(result)
                    .expectErrorMessage("Cannot find exists user by token \"NOT_EXIST_USER_TOKEN\"")
                    .verify()
            }
        }
    }

}) {
    companion object {
        private const val EXIST_USER_ID = 1L;
        private const val EXIST_USER_TOKEN = "EXIST_USER_TOKEN"

        private val sellerRegisterRes = sellerRegisterRes {
            this.id = EXIST_USER_ID
        }
    }
}
