package uk.gov.justice.digital.hmpps.hmppsalertsapi.utils

object LanguageFormatUtils {
  fun formatDisplayName(userName: String): String = userName.replace("\\s+|_".toRegex(), " ").split("(?<=[-\\s])|(?=[-\\s])".toRegex())
    .joinToString("") { it.lowercase().replaceFirstChar { char -> char.uppercaseChar() } }
}
