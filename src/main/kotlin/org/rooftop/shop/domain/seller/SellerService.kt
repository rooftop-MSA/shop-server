package org.rooftop.shop.domain.seller

import org.rooftop.shop.app.product.UserApi
import org.rooftop.shop.domain.IdGenerator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono

@Service
@Transactional(readOnly = true)
class SellerService(
    private val userApi: UserApi,
    private val idGenerator: IdGenerator,
    private val sellerRepository: SellerRepository,
) {

    @Transactional
    fun registerSeller(token: String): Mono<Unit> {
        return userApi.findUserIdByToken(token)
            .switchIfEmpty(
                Mono.error {
                    throw IllegalArgumentException("Cannot find exists user by token \"$token\"")
                }
            )
            .flatMap {
                sellerRepository.save(Seller(idGenerator.generate(), it, true))
            }
            .switchIfEmpty(
                Mono.error {
                    throw IllegalStateException("Fail to save new seller caused internal exception")
                }
            )
            .map { }
    }

    fun findSellerByUserId(userId: Long): Mono<Seller> = sellerRepository.findByUserId(userId)
}
