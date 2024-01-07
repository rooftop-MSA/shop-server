package org.rooftop.shop.service.product

import org.rooftop.shop.domain.product.ProductRepository
import org.rooftop.shop.domain.product.ProductRollbackEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import reactor.core.scheduler.Schedulers

@Service
class ProductEventService(
    private val productRepository: ProductRepository,
) {

    @EventListener(ProductRollbackEvent::class)
    fun rollbackProduct(productRollbackEvent: ProductRollbackEvent) {
        productRepository.findById(productRollbackEvent.undoProduct.id)
            .map {
                it.increaseQuantity(productRollbackEvent.undoProduct.consumedQuantity)
                it
            }
            .flatMap { productRepository.save(it) }
            .map { }
            .retry()
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe()
    }
}
