package no.nav.tilbakekreving.brev

import no.nav.tilbakekreving.hendelse.VarselbrevSendtHendelse
import no.nav.tilbakekreving.historikk.Historikk
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import java.util.UUID

class BrevHistorikk(
    private val historikk: MutableList<VarselbrevSendtHendelse>,
) : Historikk<UUID, VarselbrevSendtHendelse> {
    override fun finn(id: UUID): VarselbrevSendtHendelse {
        return historikk.single { it.internId == id }
    }

    override fun lagre(innslag: VarselbrevSendtHendelse): HistorikkReferanse<UUID, VarselbrevSendtHendelse> {
        historikk.add(innslag)
        return HistorikkReferanse(this, innslag.internId)
    }

    override fun nåværende(): HistorikkReferanse<UUID, VarselbrevSendtHendelse> {
        return HistorikkReferanse(this, historikk.last().internId)
    }
}
