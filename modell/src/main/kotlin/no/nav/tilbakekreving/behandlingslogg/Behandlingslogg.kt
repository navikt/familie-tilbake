package no.nav.tilbakekreving.behandlingslogg

import no.nav.tilbakekreving.entities.LoggInnlagEntity
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.historikk.Historikk
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import no.nav.tilbakekreving.kontrakter.frontend.models.LogginnslagDto
import java.time.ZoneOffset
import java.util.UUID

data class Behandlingslogg(
    private val historikk: MutableList<LoggInnslag>,
) : Historikk<UUID, LoggInnslag> {
    override fun lagre(innslag: LoggInnslag): HistorikkReferanse<UUID, LoggInnslag> {
        historikk.add(innslag)
        return HistorikkReferanse(this, innslag.id)
    }

    override fun finn(id: UUID, sporing: Sporing): HistorikkReferanse<UUID, LoggInnslag> {
        if (historikk.none { it.id == id }) {
            throw ModellFeil.UgyldigOperasjonException(
                "Fant ikke behandling med historikk-id $id",
                sporing,
            )
        }
        return HistorikkReferanse(this, id)
    }

    override fun entry(id: UUID): LoggInnslag {
        return historikk.single { it.id == id }
    }

    override fun nåværende(): HistorikkReferanse<UUID, LoggInnslag> {
        return HistorikkReferanse(this, historikk.last().id)
    }

    fun tilFrontend(): List<LogginnslagDto> {
        return historikk.map {
            LogginnslagDto(
                behandlingId = it.behandlingId.toString(),
                type = it.type.toString(),
                aktør = it.utfører.toString(),
                aktørIdent = it.utførerIdent,
                tittel = it.tittel,
                tekst = it.tekst,
                steg = it.steg,
                opprettetTid = it.opprettetTid.atOffset(ZoneOffset.UTC),
            )
        }
    }

    fun tilEntity(tilbakekrevingId: String): List<LoggInnlagEntity> {
        return historikk.map { it.tilEntity(tilbakekrevingId) }
    }
}
