package uk.gov.justice.digital.hmpps.hmppsalertsapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.info.Info
import org.springframework.boot.actuate.info.InfoContributor
import org.springframework.stereotype.Component

@Component
class NavigationInfo(@Value("\${service.ui.url}") val url: String = "") : InfoContributor {
  override fun contribute(builder: Info.Builder?) {
    val navigation = Navigation(href = url)
    builder?.withDetail("navigation", navigation)
  }
}

data class Navigation(
  val description: String = "Create and manage alert types and codes. Add alerts in bulk for lists of prisoners.",
  val href: String,
  val navEnabled: Boolean = false,
)
