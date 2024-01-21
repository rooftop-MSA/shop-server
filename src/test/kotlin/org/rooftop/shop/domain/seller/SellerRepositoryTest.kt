package org.rooftop.shop.domain.seller

import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
import org.rooftop.shop.domain.R2dbcConfigurer
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing
import org.springframework.test.context.ContextConfiguration
import reactor.test.StepVerifier

@DataR2dbcTest
@DisplayName("SellerRepository 클래스의")
@ContextConfiguration(classes = [R2dbcConfigurer::class])
internal class SellerRepositoryTest(private val sellerRepository: SellerRepository) : DescribeSpec({

    extension(SpringExtension)

    afterEach {
        sellerRepository.deleteAll().block()
    }

    describe("findByUserId 메소드는") {
        context("userId에 해당하는 seller가 존재한다면,") {

            val expected = seller()
            sellerRepository.save(expected).block()

            it("seller를 반환한다.") {
                val result = sellerRepository.findByUserId(expected.userId)

                StepVerifier.create(result)
                    .assertNext { savedSeller ->
                        savedSeller.shouldBeEqualToIgnoringFields(
                            expected,
                            Seller::modifiedAt,
                            Seller::createdAt
                        )
                    }
                    .verifyComplete()
            }
        }

        context("userId에 해당하는 seller가 없다면,") {

            val notExistSeller = seller()

            it("empty를 반환한다.") {
                val result = sellerRepository.findByUserId(notExistSeller.userId)

                StepVerifier.create(result)
                    .verifyComplete()
            }
        }
    }
}) {

}
