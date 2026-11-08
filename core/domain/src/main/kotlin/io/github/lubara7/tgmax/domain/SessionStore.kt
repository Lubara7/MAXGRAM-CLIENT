package io.github.lubara7.tgmax.domain

interface SessionStore {
    suspend fun read(): Session?
    suspend fun write(session: Session)
    suspend fun clear()
}
