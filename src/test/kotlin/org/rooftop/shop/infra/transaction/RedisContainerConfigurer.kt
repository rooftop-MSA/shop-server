package org.rooftop.shop.infra.transaction

import org.springframework.boot.test.context.TestConfiguration
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

@TestConfiguration
class RedisContainerConfigurer {

    init {
        val redis: GenericContainer<*> = GenericContainer(DockerImageName.parse("redis:7.2.3"))
            .withExposedPorts(6379)
        redis.start()
    }
}
