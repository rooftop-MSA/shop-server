package org.rooftop.shop.infra.transaction

import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldBeEqual
import org.rooftop.shop.app.product.TransactionIdGenerator
import org.rooftop.shop.app.product.TransactionManager
import org.rooftop.shop.app.product.UndoProduct
import org.rooftop.shop.domain.app.undoProduct
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import reactor.test.StepVerifier

@DisplayName("ProductTransactionManager 클래스 의")
@ContextConfiguration(
    classes = [
        RedisContainerConfigurer::class,
        ByteArrayRedisSerializer::class,
        ReactiveRedisConfigurer::class,
        TransactionIdGeneratorImpl::class,
        ProductTransactionManager::class,
    ]
)
@TestPropertySource("classpath:application.properties")
internal class ProductTransactionManagerTest(
    private val transactionIdGenerator: TransactionIdGenerator,
    private val transactionManager: TransactionManager<UndoProduct>,
) : DescribeSpec({

    describe("join 메소드는") {
        context("undoServer 와 transactionServer 에 저장을 성공하면,") {

            val undoProduct = undoProduct()
            val transactionId = transactionIdGenerator.generate()

            it("새로운 트랜잭션을 만들고, 생성된 트랜잭션의 id를 리턴한다.") {
                val result = transactionManager.join(transactionId, undoProduct)
                    .log()

                StepVerifier.create(result)
                    .assertNext {
                        it::class shouldBeEqual String::class
                    }
                    .verifyComplete()
            }
        }
    }

    describe("commit 메소드는") {
        context("transactionId에 해당하는 transaction에 join한 적이 있으면,") {

            val transactionId = transactionIdGenerator.generate()
            transactionManager.join(transactionId, undoProduct()).block()

            it("transactionServer에 COMMIT 상태의 트랜잭션을 publish 한다.") {
                val result = transactionManager.commit(transactionId).log()

                StepVerifier.create(result)
                    .expectNext(Unit)
                    .verifyComplete()
            }
        }

        context("transactionId에 해당하는 transaction에 join한 적이 없으면,") {

            val transactionId = transactionIdGenerator.generate()

            it("IllegalStateException 을 던진다.") {
                val result = transactionManager.commit(transactionId).log()

                StepVerifier.create(result)
                    .verifyErrorMessage("Cannot find opened transaction id \"$transactionId\"")
            }

        }
    }

    describe("rollback 메소드는") {
        context("transactionId에 해당하는 transaction에 join한 적이 있으면,") {

            val transactionId = transactionIdGenerator.generate()
            transactionManager.join(transactionId, undoProduct()).block()

            it("transactionServer에 ROLLBACK 상태의 트랜잭션을 publish 한다.") {
                val result = transactionManager.rollback(transactionId).log()

                StepVerifier.create(result)
                    .expectNext(Unit)
                    .verifyComplete()
            }
        }

        context("transactionId에 해당하는 transaction에 join한 적이 없으면,") {

            val transactionId = transactionIdGenerator.generate()

            it("IllegalStateException을 던진다.") {
                val result = transactionManager.rollback(transactionId).log()

                StepVerifier.create(result)
                    .verifyErrorMessage("Cannot find opened transaction id \"$transactionId\"")
            }
        }
    }

    describe("exists 메소드는") {

        context("Id에 해당하는 열려있는 트랜잭션이 있으면,") {

            val transactionId = transactionIdGenerator.generate()
            transactionManager.join(transactionId, undoProduct()).block()
            
            it("transactionId를 반환한다.") {
                val result = transactionManager.exists(transactionId)

                StepVerifier.create(result)
                    .expectNext(transactionId)
                    .verifyComplete()
            }
        }

        context("Id에 해당하는 열려있는 트랜잭션이 없으면,") {

            val transactionId = transactionIdGenerator.generate()

            it("예외를 던진다.") {
                val result = transactionManager.exists(transactionId)

                StepVerifier.create(result)
                    .expectErrorMessage("Cannot find opened transaction id \"$transactionId\"")
            }
        }
    }
}) {
}