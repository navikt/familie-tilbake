package no.nav.tilbakekreving

import no.nav.tilbakekreving.historikk.Historikk
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import java.util.UUID

class HistorikkStub<T : Historikk.HistorikkInnslag<UUID>>(val entry: T) : Historikk<UUID, T> {
    val id = UUID.randomUUID()

    companion object {
        fun <T : Historikk.HistorikkInnslag<UUID>> fakeReferanse(value: T): HistorikkReferanse<UUID, T> = HistorikkStub(value).lagre(value)
    }

    override fun lagre(innslag: T): HistorikkReferanse<UUID, T> {
        return HistorikkReferanse(this, id)
    }

    override fun finn(id: UUID): T {
        return entry
    }

    override fun nåværende(): HistorikkReferanse<UUID, T> {
        return HistorikkReferanse(this, id)
    }
}
