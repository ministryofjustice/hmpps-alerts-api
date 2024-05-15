package uk.gov.justice.digital.hmpps.hmppsalertsapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.info.Info
import org.springframework.boot.actuate.info.InfoContributor
import org.springframework.stereotype.Component

@Component
class NavigationInfo(@Value("\${environment}") val environment: String = "") : InfoContributor {
  override fun contribute(builder: Info.Builder?) {
    val url = if (this.environment.isNotEmpty()) {
      String.format(URL, "-$environment")
    } else {
      String.format(URL, "")
    }
    val navigation = Navigation(href = url)
    builder?.withDetail("navigation", navigation)
  }

  companion object {
    private const val URL = "https://alerts-ui%s.hmpps.service.justice.gov.uk"
  }
}

data class Navigation(
  val description: String = "Create and manage alert types and codes. Add alerts in bulk for lists of prisoners.",
  val href: String,
  val navEnabled: Boolean = false,
)
