package uk.gov.justice.digital.hmpps.hmppsalertsapi.utils

import kotlin.reflect.KProperty0

fun <T : Any, V : Any?> T.set(field: KProperty0<V>, value: V): T {
  return setByName(field.name, value)
}

fun <T : Any, V : Any?> T.setByName(field: String, value: V): T {
  val f = this::class.java.getDeclaredField(field)
  f.isAccessible = true
  f[this] = value
  f.isAccessible = false
  return this
}
