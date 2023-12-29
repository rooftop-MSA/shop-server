package org.rooftop.shop.domain.seller

import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import org.rooftop.shop.infra.R2dbcConfig
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing
import org.springframework.test.context.ContextConfiguration
import reactor.test.StepVerifier

@DataR2dbcTest
@EnableR2dbcAuditing
@DisplayName("SellerRepository 클래스의")
@ContextConfiguration(classes = [R2dbcConfig::class])
internal class SellerRepositoryTest(private val sellerRepository: SellerRepository) : DescribeSpec({

    extension(SpringExtension)

    afterEach {
        sellerRepository.deleteAll().block()
    }

    describe("existsByUserId 메소드는") {
        context("userId에 해당하는 user가 존재한다면,") {

            val existsSeller = seller()
            sellerRepository.save(existsSeller).block()

            it("true를 반환한다.") {
                val result = sellerRepository.existsByUserId(existsSeller.userId)

                StepVerifier.create(result)
                    .expectNext(true)
                    .verifyComplete()
            }
        }

        context("userId에 해당하는 user가 없다면,") {

            val notExistSeller = seller()

            it("false를 반환한다.") {
                val result = sellerRepository.existsByUserId(notExistSeller.userId)

                StepVerifier.create(result)
                    .expectNext(false)
                    .verifyComplete()
            }
        }
    }
}) {

}
