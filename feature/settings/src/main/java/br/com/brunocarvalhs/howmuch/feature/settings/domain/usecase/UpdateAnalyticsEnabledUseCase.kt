package br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.repository.SettingsRepository
import kotlinx.coroutines.CancellationException
import java.io.IOException
import javax.inject.Inject

internal class UpdateAnalyticsEnabledUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(enabled: Boolean): Result<Unit> = try {
        Result.success(repository.updateAnalyticsEnabled(enabled))
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        Result.failure(e)
    }
}
