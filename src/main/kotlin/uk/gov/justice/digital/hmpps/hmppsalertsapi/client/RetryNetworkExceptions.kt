package uk.gov.justice.digital.hmpps.hmppsalertsapi.client

import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.DownstreamServiceException
import java.time.Duration

fun <T : Any> Mono<T>.retryNetworkExceptions(downstreamErrorMessage: String): Mono<T> = retryWhen(
  Retry.backoff(3, Duration.ofMillis(250))
    .filter {
      it is WebClientRequestException || (it is WebClientResponseException && it.statusCode.is5xxServerError)
    }.onRetryExhaustedThrow { _, signal ->
      if (signal.failure() is WebClientResponseException) {
        DownstreamServiceException(downstreamErrorMessage, signal.failure())
      } else {
        signal.failure()
      }
    },
)

fun <T : Any> Flux<T>.retryNetworkExceptions(downstreamErrorMessage: String): Flux<T> = retryWhen(
  Retry.backoff(3, Duration.ofMillis(250))
    .filter {
      it is WebClientRequestException || (it is WebClientResponseException && it.statusCode.is5xxServerError)
    }.onRetryExhaustedThrow { _, signal ->
      if (signal.failure() is WebClientResponseException) {
        DownstreamServiceException(downstreamErrorMessage, signal.failure())
      } else {
        signal.failure()
      }
    },
)
