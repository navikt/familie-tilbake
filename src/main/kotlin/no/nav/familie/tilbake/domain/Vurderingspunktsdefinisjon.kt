package no.nav.familie.tilbake.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.util.*

data class Vurderingspunktsdefinisjon(@Id
                                      val id: UUID = UUID.randomUUID(),
                                      val behandlingsstegstypeId: UUID,
                                      val kode: String,
                                      val navn: String,
                                      val beskrivelse: String?,
                                      val vurderingspunktstype: String = "UT",
                                      @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                                      val sporbar: Sporbar = Sporbar())