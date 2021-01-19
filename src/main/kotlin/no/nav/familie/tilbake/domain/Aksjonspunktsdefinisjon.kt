package no.nav.familie.tilbake.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import java.util.*

data class Aksjonspunktsdefinisjon(@Id
                                   val id: UUID = UUID.randomUUID(),
                                   val vurderingspunktsdefinisjonId: UUID,
                                   val kode: String,
                                   val navn: String,
                                   val beskrivelse: String?,
                                   @Column("vilkarstype")
                                   val vilkårstype: String?,
                                   val totrinnsbehandlingDefault: Boolean,
                                   val fristperiode: String?,
                                   val skjermlenketype: String,
                                   val aksjonspunktstype: String = "MANU",
                                   val tilbakehoppVedGjenopptakelse: Boolean = false,
                                   val lagUtenHistorikk: Boolean = false,
                                   @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                                   val sporbar: Sporbar = Sporbar())