package org.rooftop.shop.domain.product

import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import org.rooftop.shop.infra.R2dbcConfig
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing
import org.springframework.test.context.ContextConfiguration
import reactor.test.StepVerifier
import java.util.stream.LongStream

@DataR2dbcTest
@EnableR2dbcAuditing
@DisplayName("ProductRepository 클래스의")
@ContextConfiguration(classes = [R2dbcConfig::class])
internal class ProductRepositoryTest(
    private val productRepository: ProductRepository,
) : DescribeSpec({

    extension(SpringExtension)

    afterEach {
        productRepository.deleteAll().block()
    }

    describe("findProducts 메소드는") {
        context("productId 가 주어지면,") {

            val products = saveProducts(51, productRepository)
            val lastProductId = products[33].id

            it("이후의 10개의 product들을 조회한다.") {
                val result = productRepository.findProducts(lastProductId)

                StepVerifier.create(result)
                    .expectNextCount(10)
                    .verifyComplete()
            }
        }

        context("productId가 주어지지 않으면,") {

            saveProducts(11, productRepository)

            it("처음부터 10개의 product들을 조회한다.") {
                val result = productRepository.findProducts()

                StepVerifier.create(result)
                    .expectNextCount(10)
                    .verifyComplete()
            }
        }
    }

}) {

    companion object {
        private fun saveProducts(count: Long, productRepository: ProductRepository): List<Product> =
            LongStream.range(1, count + 1)
                .mapToObj { productRepository.save(product(id = it)).block() }
                .toList()
    }
}
