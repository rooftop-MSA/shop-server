package org.rooftop.shop.service.seller

import org.rooftop.shop.domain.seller.Seller
import org.rooftop.shop.domain.seller.SellerConnector
import org.rooftop.shop.domain.seller.SellerRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono

@Service
@Transactional(readOnly = true)
class SellerConnectorImpl(
    private val sellerRepository: SellerRepository,
) : SellerConnector {

    override fun findSellerByUserId(userId: Long): Mono<Seller> =
        sellerRepository.findByUserId(userId)
}
