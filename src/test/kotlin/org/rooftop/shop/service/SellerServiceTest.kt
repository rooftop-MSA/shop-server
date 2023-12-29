package org.rooftop.shop.service

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.every
import org.rooftop.shop.domain.IdGenerator
import org.rooftop.shop.domain.seller.SellerRepository
import org.rooftop.shop.domain.UserApi
import org.rooftop.shop.domain.seller.seller
import org.rooftop.shop.service.seller.SellerService
import org.springframework.test.context.ContextConfiguration
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

@DisplayName("SellerService 의")
@ContextConfiguration(classes = [SellerService::class])
internal class SellerServiceTest(
    private val sellerService: SellerService,
    @MockkBean private val userApi: UserApi,
    @MockkBean private val idGenerator: IdGenerator,
    @MockkBean private val sellerRepository: SellerRepository,
) : DescribeSpec({

    beforeEach {
        every { idGenerator.generate() } returns 1L

        every { userApi.findUserIdByToken(any()) } returns Mono.empty()
        every { userApi.findUserIdByToken(EXIST_USER_TOKEN) } returns Mono.just(EXIST_USER_ID)

        every { sellerRepository.save(any()) } returns Mono.just(existUserIdSeller)
    }

    describe("registerSeller 메소드는") {
        context("존재하는 유저의 토근이 주어지면,") {
            it("Seller 저장에 성공한다") {
                val result = sellerService.registerSeller(EXIST_USER_TOKEN)

                StepVerifier.create(result)
                    .expectNext(Unit)
                    .verifyComplete()
            }
        }

        context("존재하지 않는 유저의 토큰이 주어지면,") {

            it("IllegalArgumentException 을 던진다.") {
                val result = sellerService.registerSeller("NOT_EXIST_USER_TOKEN");

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

        private val existUserIdSeller = seller(userId = EXIST_USER_ID)
    }
}
