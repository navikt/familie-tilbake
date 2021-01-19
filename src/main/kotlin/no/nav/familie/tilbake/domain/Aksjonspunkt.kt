package no.nav.familie.tilbake.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.time.LocalDateTime
import java.util.*

data class Aksjonspunkt(@Id
                        val id: UUID = UUID.randomUUID(),
                        val behandlingId: UUID,
                        val behandlingsstegstypeId: UUID,
                        val aksjonspunktsdefinisjonId: UUID,
                        val totrinnsbehandling: Boolean,
                        val status: String,
                        val tidsfrist: LocalDateTime?,
                        val ventearsak: String = "-",
                        val reaktiveringsstatus: String = "AKTIV",
                        val manueltOpprettet: Boolean = false,
                        val revurdering: Boolean = false,
                        val versjon: Int = 0,
                        @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                        val sporbar: Sporbar = Sporbar())