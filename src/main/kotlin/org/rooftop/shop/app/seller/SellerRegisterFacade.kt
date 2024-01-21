package org.rooftop.shop.app.seller

import org.rooftop.api.shop.SellerRegisterRes
import org.rooftop.shop.app.UserApi
import org.rooftop.shop.domain.seller.SellerService
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class SellerRegisterFacade(
    private val userApi: UserApi,
    private val sellerService: SellerService,
) {

    fun registerSeller(token: String): Mono<SellerRegisterRes> {
        return userApi.findUserIdByToken(token)
            .switchIfEmpty(
                Mono.error {
                    throw IllegalArgumentException("Cannot find exists user by token \"$token\"")
                }
            )
            .flatMap { sellerService.registerSeller(it) }
    }
}
