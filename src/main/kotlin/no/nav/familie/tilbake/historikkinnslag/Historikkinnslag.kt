package no.nav.familie.tilbake.historikkinnslag

import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.Constants
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

data class Historikkinnslag(
    @Id val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    val type: Historikkinnslagstype,
    @Column("aktor")
    val aktør: Aktør,
    val tittel: String,
    val tekst: String? = null,
    val steg: String? = null,
    val journalpostId: String? = null,
    val dokumentId: String? = null,
    val opprettetAv: String,
    val opprettetTid: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS),
) {
    enum class Aktør {
        SAKSBEHANDLER,
        BESLUTTER,
        VEDTAKSLØSNING,
    }
}

sealed interface Aktør {
    val ident: String
    val type: Historikkinnslag.Aktør

    data class Saksbehandler(
        override val ident: String,
    ) : Aktør {
        override val type = Historikkinnslag.Aktør.SAKSBEHANDLER

        companion object {
            fun fraBehandling(
                behandlingId: UUID,
                behandlingRepository: BehandlingRepository,
            ): Saksbehandler = Saksbehandler(behandlingRepository.findByIdOrThrow(behandlingId).ansvarligSaksbehandler)
        }
    }

    data class Beslutter(
        override val ident: String,
    ) : Aktør {
        override val type = Historikkinnslag.Aktør.BESLUTTER
    }

    object Vedtaksløsning : Aktør {
        override val ident: String = Constants.BRUKER_ID_VEDTAKSLØSNINGEN
        override val type = Historikkinnslag.Aktør.VEDTAKSLØSNING
    }
}

enum class Historikkinnslagstype {
    HENDELSE,

    SKJERMLENKE,

    BREV,
}
