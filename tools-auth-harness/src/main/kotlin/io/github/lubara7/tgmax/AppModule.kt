package io.github.lubara7.tgmax

import android.content.Context
import io.github.lubara7.tgmax.data.KeystoreSessionStore
import io.github.lubara7.tgmax.data.MaxAuthRepository
import io.github.lubara7.tgmax.data.MaxClientConfig
import io.github.lubara7.tgmax.domain.AuthRepository

/** Small explicit composition root; features only depend on domain contracts. */
class AppModule(context: Context) {
    val authRepository: AuthRepository = MaxAuthRepository(
        gateway = createMaxGateway(MaxClientConfig(BuildConfig.MAX_APP_ID)),
        store = KeystoreSessionStore(context.applicationContext),
    )
}
