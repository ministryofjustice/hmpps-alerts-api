package uk.gov.justice.digital.hmpps.hmppsalertsapi.client

import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.io.IOException
import java.time.Duration

fun <T> Mono<T>.retryNetworkExceptions(): Mono<T> =
  retryWhen(
      Retry.backoff(3, Duration.ofSeconds(1))
          .filter {
              it is IOException || (it is WebClientResponseException && it.statusCode.is5xxServerError)
          }.onRetryExhaustedThrow { _, signal -> signal.failure()
        }
  )