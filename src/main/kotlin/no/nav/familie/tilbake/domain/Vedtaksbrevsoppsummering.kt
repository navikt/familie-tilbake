package no.nav.familie.tilbake.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.util.*

data class Vedtaksbrevsoppsummering(@Id
                                    val id: UUID = UUID.randomUUID(),
                                    val behandlingId: UUID,
                                    val oppsummeringFritekst: String?,
                                    val fritekst: String?,
                                    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                                    val sporbar: Sporbar = Sporbar())