package no.nav.tilbakekreving.eksternfagsak

import no.nav.tilbakekreving.entities.EksternFagsakBehandlingEntity
import no.nav.tilbakekreving.entities.HistorikkEntity
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.historikk.Historikk
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import java.util.UUID

class EksternFagsakBehandlingHistorikk(
    private val historikk: MutableList<EksternFagsakRevurdering>,
) : Historikk<UUID, EksternFagsakRevurdering> {
    private val erstattet = mutableMapOf<UUID, UUID>()

    init {
        historikk.sortedByDescending { it.opprettet }
            .zipWithNext()
            .forEach { (forrige, neste) ->
                if (forrige is EksternFagsakRevurdering.Revurdering && (neste as? EksternFagsakRevurdering.Revurdering)?.erDuplikat(forrige) == true) {
                    erstattet[neste.id] = erstattet[forrige.id] ?: forrige.id
                    historikk.remove(neste)
                }
            }
    }

    override fun <T : EksternFagsakRevurdering> lagre(innslag: T): HistorikkReferanse<UUID, T> {
        historikk.add(innslag)
        return HistorikkReferanse(this, innslag.id) as HistorikkReferanse<UUID, T>
    }

    override fun finn(id: UUID, sporing: Sporing): HistorikkReferanse<UUID, EksternFagsakRevurdering> {
        val gjeldendeId = erstattet[id] ?: id
        if (historikk.none { it.id == gjeldendeId }) {
            throw ModellFeil.UgyldigOperasjonException(
                "Fant ikke ekstern fagsak behandling med historikk-id $id",
                sporing,
            )
        }
        return HistorikkReferanse(this, gjeldendeId)
    }

    override fun entry(id: UUID): EksternFagsakRevurdering {
        return historikk.first { it.id == id }
    }

    override fun nåværende(): HistorikkReferanse<UUID, EksternFagsakRevurdering> {
        return HistorikkReferanse(this, historikk.last().id)
    }

    fun harBehandling(): Boolean = historikk.isNotEmpty()

    fun tilEntity(eksternFagsakRef: UUID): HistorikkEntity<UUID, EksternFagsakBehandlingEntity, EksternFagsakRevurdering> {
        return HistorikkEntity(historikk.map { it.tilEntity(eksternFagsakRef) })
    }
}
