package org.rooftop.shop.domain.product

import org.rooftop.api.shop.ProductRegisterReq
import org.rooftop.api.shop.ProductRegisterRes
import org.rooftop.api.shop.productRegisterRes
import org.rooftop.shop.domain.IdGenerator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
@Transactional(readOnly = true)
class ProductService(
    private val idGenerator: IdGenerator,
    private val productRepository: ProductRepository,
) {

    @Transactional
    fun registerProduct(
        sellerId: Long,
        productRegisterReq: ProductRegisterReq,
    ): Mono<ProductRegisterRes> {
        return Mono.fromCallable {
            Product(
                idGenerator.generate(),
                sellerId,
                productRegisterReq.title,
                productRegisterReq.description,
                productRegisterReq.price,
                productRegisterReq.quantity,
                isNew = true
            )
        }.flatMap {
            productRepository.save(it)
        }.map {
            productRegisterRes {
                this.id = it.id
            }
        }
    }

    fun findProducts(lastProductId: Long): Flux<Product> {
        return productRepository.findProducts(lastProductId)
    }

    fun getProductById(id: Long): Mono<Product> {
        return productRepository.findById(id)
            .switchIfEmpty(
                Mono.error { throw IllegalArgumentException("Cannot find product by productId \"$id\"") }
            )
    }

    @Transactional
    fun consumeProduct(productId: Long, consumeQuantity: Long): Mono<Product> {
        return productRepository.findById(productId)
            .switchIfEmpty(
                Mono.error {
                    IllegalStateException("Cannot find product id \"$productId\"")
                }
            )
            .map {
                it.consumeQuantity(consumeQuantity)
                it
            }
            .flatMap {
                productRepository.save(it)
            }
    }

    @Transactional
    fun rollbackProduct(productId: Long, consumeQuantity: Long): Mono<Product> {
        return productRepository.findById(productId)
            .map {
                it.increaseQuantity(consumeQuantity)
                it
            }
            .flatMap { productRepository.save(it) }
    }
}
