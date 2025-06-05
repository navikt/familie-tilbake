package no.nav.familie.tilbake.common.repository

import org.springframework.data.repository.CrudRepository

inline fun <reified T, ID : Any> CrudRepository<T, ID>.findByIdOrThrow(id: ID): T = findById(id).orElseThrow { IllegalStateException("Finner ikke ${T::class.simpleName} med id=$id") }
