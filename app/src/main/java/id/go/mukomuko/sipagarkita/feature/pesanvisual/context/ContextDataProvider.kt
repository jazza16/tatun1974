package id.go.mukomuko.sipagarkita.feature.pesanvisual.context

import java.time.Instant

interface ContextDataProvider {
    suspend fun fetchContext(): SituationalContext
}

class PlaceholderContextDataProvider : ContextDataProvider {
    override suspend fun fetchContext(): SituationalContext =
        SituationalContext(fetchedAt = Instant.now())
}
