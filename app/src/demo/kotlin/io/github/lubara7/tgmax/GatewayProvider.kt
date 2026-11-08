package io.github.lubara7.tgmax

import io.github.lubara7.tgmax.data.*

@Suppress("UNUSED_PARAMETER")
internal fun createMaxGateway(config: MaxClientConfig): MaxAuthGateway = DemoMaxAuthGateway()
