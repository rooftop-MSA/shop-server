package org.rooftop.shop.domain.seller

import org.rooftop.api.shop.SellerRegisterRes
import org.rooftop.api.shop.sellerRegisterRes
import org.rooftop.shop.domain.IdGenerator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono

@Service
@Transactional(readOnly = true)
class SellerService(
    private val idGenerator: IdGenerator,
    private val sellerRepository: SellerRepository,
) {

    @Transactional
    fun registerSeller(userId: Long): Mono<SellerRegisterRes> {
        return sellerRepository.save(Seller(idGenerator.generate(), userId, true))
            .switchIfEmpty(
                Mono.error {
                    throw IllegalStateException("Fail to save new seller caused internal exception")
                }
            )
            .map {
                sellerRegisterRes {
                    this.id = it.id
                }
            }
    }

    fun findSellerByUserId(userId: Long): Mono<Seller> = sellerRepository.findByUserId(userId)
}
