package uk.gov.justice.digital.hmpps.hmppsalertsapi.utils

object LanguageFormatUtils {
  fun formatDisplayName(userName: String): String {
    return userName.replace("_".toRegex(), " ").split(" ")
      .joinToString(" ") { it.lowercase().replaceFirstChar { char -> char.uppercaseChar() } }
  }
}
