package io.github.lubara7.tgmax

import io.github.lubara7.tgmax.data.*

internal fun createMaxGateway(config: MaxClientConfig): MaxAuthGateway = OpenClientAuthGateway(config)
