package org.rooftop.shop.infra

import org.rooftop.shop.Application
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories

@Configuration
@EnableR2dbcRepositories(basePackageClasses = [Application::class])
internal abstract class R2dbcConfig : AbstractR2dbcConfiguration()
