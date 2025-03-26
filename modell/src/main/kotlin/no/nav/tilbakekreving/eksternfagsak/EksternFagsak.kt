package no.nav.tilbakekreving.eksternfagsak

import no.nav.tilbakekreving.FrontendDto
import no.nav.tilbakekreving.api.v2.EksternFagsakDto
import no.nav.tilbakekreving.behov.BehovObservatør
import no.nav.tilbakekreving.behov.FagsysteminfoBehov
import no.nav.tilbakekreving.hendelse.FagsysteminfoHendelse
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import no.nav.tilbakekreving.kontrakter.ytelse.Fagsystem
import no.nav.tilbakekreving.kontrakter.ytelse.Ytelsestype
import java.util.UUID

class EksternFagsak(
    val eksternId: String,
    private val ytelsestype: Ytelsestype,
    private val fagsystem: Fagsystem,
    private val behandlinger: EksternFagsakBehandlingHistorikk,
    private val behovObservatør: BehovObservatør,
) : FrontendDto<EksternFagsakDto> {
    override fun tilFrontendDto(): EksternFagsakDto {
        return EksternFagsakDto(
            eksternId = eksternId,
            ytelsestype = ytelsestype,
            fagsystem = fagsystem,
        )
    }

    fun lagre(fagsysteminfo: FagsysteminfoHendelse): HistorikkReferanse<UUID, EksternFagsakBehandling> {
        return behandlinger.lagre(
            EksternFagsakBehandling(
                internId = UUID.randomUUID(),
                eksternId = fagsysteminfo.eksternId,
            ),
        )
    }

    fun trengerFagsysteminfo() {
        behovObservatør.håndter(
            FagsysteminfoBehov(
                eksternFagsakId = eksternId,
                fagsystem = fagsystem,
                ytelsestype = ytelsestype,
            ),
        )
    }
}
