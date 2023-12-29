package org.rooftop.shop.service

import org.rooftop.shop.domain.IdGenerator
import org.rooftop.shop.domain.seller.Seller
import org.rooftop.shop.domain.seller.SellerRepository
import org.rooftop.shop.domain.UserApi
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
}
