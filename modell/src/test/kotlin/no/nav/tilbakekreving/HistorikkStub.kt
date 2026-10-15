package no.nav.tilbakekreving

import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.historikk.Historikk
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import java.util.UUID

class HistorikkStub(val entry: Historikk.HistorikkInnslag<UUID>) : Historikk<UUID, Historikk.HistorikkInnslag<UUID>> {
    val id = UUID.randomUUID()

    companion object {
        fun <T : Historikk.HistorikkInnslag<UUID>> fakeReferanse(value: T): HistorikkReferanse<UUID, T> = HistorikkStub(value).lagre(value)
    }

    override fun <T : Historikk.HistorikkInnslag<UUID>> lagre(innslag: T): HistorikkReferanse<UUID, T> {
        return HistorikkReferanse(this, innslag.id) as HistorikkReferanse<UUID, T>
    }

    override fun finn(id: UUID, sporing: Sporing): HistorikkReferanse<UUID, Historikk.HistorikkInnslag<UUID>> {
        return HistorikkReferanse(this, id)
    }

    override fun entry(id: UUID): Historikk.HistorikkInnslag<UUID> {
        return entry
    }

    override fun nåværende(): HistorikkReferanse<UUID, Historikk.HistorikkInnslag<UUID>> {
        return HistorikkReferanse(this, id)
    }
}
