package uk.gov.justice.digital.hmpps.hmppsalertsapi.config

import io.sentry.SentryOptions
import jakarta.servlet.http.HttpServletRequest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.digital.hmpps.hmppsalertsapi.exceptions.NotFoundException
import java.util.regex.Pattern.matches

@Configuration
class SentryConfig {
  @Bean
  fun eventFilter() = SentryOptions.BeforeSendCallback { event, _ ->
    event.takeIf {
      it.throwable !is NotFoundException
    }
  }

  @Bean
  fun ignoreHealthRequests() = SentryOptions.BeforeSendTransactionCallback { transaction, _ ->
    transaction.transaction?.let { if (it.startsWith("GET /health") or it.startsWith("GET /info")) null else transaction }
  }

  @Bean
  fun transactionSampling() = SentryOptions.TracesSamplerCallback { context ->
    context.customSamplingContext?.let {
      val request = it["request"] as HttpServletRequest
      when (request.method) {
        "GET" if (matches("/prisoners/[A-Z][0-9]{4}[A-Z]{2}/alerts", request.requestURI)) -> {
          0.0025
        }

        else -> {
          0.05
        }
      }
    }
  }
}
