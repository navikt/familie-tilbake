package no.nav.familie.tilbake.historikkinnslag

import no.nav.familie.kontrakter.felles.Applikasjon
import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.kontrakter.felles.historikkinnslag.OpprettHistorikkinnslagRequest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultatstype
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype.BEHANDLING_HENLAGT
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype.BEHANDLING_PÅ_VENT
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype.HENLEGGELSESBREV_SENDT
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype.HENLEGGELSESBREV_SENDT_TIL_VERGE
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype.INNHENT_DOKUMENTASJON_BREV_SENDT
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype.INNHENT_DOKUMENTASJON_BREV_SENDT_TIL_VERGE
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype.KORRIGERT_VARSELBREV_SENDT
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype.KORRIGERT_VARSELBREV_SENDT_TIL_VERGE
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype.VARSELBREV_SENDT
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype.VARSELBREV_SENDT_TIL_VERGE
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype.VEDTAKSBREV_SENDT
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype.VEDTAKSBREV_SENDT_TIL_VERGE
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype.VEDTAK_FATTET
import no.nav.familie.tilbake.integration.kafka.KafkaProducer
import no.nav.familie.tilbake.service.dokumentbestilling.felles.BrevsporingRepository
import no.nav.familie.tilbake.service.dokumentbestilling.felles.domain.Brevsporing
import no.nav.familie.tilbake.service.dokumentbestilling.felles.domain.Brevtype
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class HistorikkService(private val behandlingRepository: BehandlingRepository,
                       private val fagsakRepository: FagsakRepository,
                       private val brevsporingRepository: BrevsporingRepository,
                       private val behandlingskontrollService: BehandlingskontrollService,
                       private val kafkaProducer: KafkaProducer) {

    @Transactional
    fun lagHistorikkinnslag(behandlingId: UUID,
                            historikkinnslagstype: TilbakekrevingHistorikkinnslagstype,
                            aktør: Aktør,
                            begrunnelse: String? = null) {
        val request = lagHistorikkinnslagRequest(behandlingId = behandlingId,
                                                 aktør = aktør,
                                                 historikkinnslagstype = historikkinnslagstype,
                                                 begrunnelse = begrunnelse)
        kafkaProducer.sendHistorikkinnslag(behandlingId, request.behandlingId, request)
    }

    private fun lagHistorikkinnslagRequest(behandlingId: UUID,
                                           aktør: Aktør,
                                           historikkinnslagstype: TilbakekrevingHistorikkinnslagstype,
                                           begrunnelse: String?)
            : OpprettHistorikkinnslagRequest {

        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val brevdata = hentBrevdata(behandling, historikkinnslagstype)

        return OpprettHistorikkinnslagRequest(behandlingId = behandling.eksternBrukId.toString(),
                                              eksternFagsakId = fagsak.eksternFagsakId,
                                              fagsystem = fagsak.fagsystem,
                                              applikasjon = Applikasjon.FAMILIE_TILBAKE,
                                              type = historikkinnslagstype.type,
                                              aktør = aktør,
                                              aktørIdent = hentAktørIdent(behandling, aktør),
                                              steg = historikkinnslagstype.steg?.name,
                                              tittel = historikkinnslagstype.tittel,
                                              tekst = lagTekst(behandling, historikkinnslagstype, begrunnelse),
                                              journalpostId = brevdata?.journalpostId,
                                              dokumentId = brevdata?.dokumentId)
    }

    private fun lagTekst(behandling: Behandling,
                         historikkinnslagstype: TilbakekrevingHistorikkinnslagstype,
                         begrunnelse: String?): String? {
        return when (historikkinnslagstype) {
            BEHANDLING_PÅ_VENT -> {
                val stegstilstand = behandlingskontrollService.finnAktivStegstilstand(behandling.id)
                val beskrivelse: String? = stegstilstand?.venteårsak?.beskrivelse
                beskrivelse.let { historikkinnslagstype.tekst + beskrivelse }
            }
            VEDTAK_FATTET -> {
                val resultatstype: Behandlingsresultatstype? = behandling.sisteResultat?.type
                resultatstype?.let { historikkinnslagstype.tekst + it.navn }
            }
            BEHANDLING_HENLAGT -> {
                val resultatstype: Behandlingsresultatstype = requireNotNull(behandling.sisteResultat?.type)
                when (resultatstype) {
                    Behandlingsresultatstype.HENLAGT_KRAVGRUNNLAG_NULLSTILT,
                    Behandlingsresultatstype.HENLAGT_TEKNISK_VEDLIKEHOLD -> historikkinnslagstype.tekst + resultatstype.navn
                    else -> {
                        historikkinnslagstype.tekst + resultatstype.navn + ", " + "Begrunnelse: " + begrunnelse
                    }
                }
            }
            else -> historikkinnslagstype.tekst
        }
    }

    private fun hentAktørIdent(behandling: Behandling, aktør: Aktør): String {
        return when (aktør) {
            Aktør.VEDTAKSLØSNING -> "VL"
            Aktør.SAKSBEHANDLER -> behandling.ansvarligSaksbehandler
            Aktør.BESLUTTER -> behandling.ansvarligBeslutter!!
        }
    }

    private fun hentBrevdata(behandling: Behandling, historikkinnslagstype: TilbakekrevingHistorikkinnslagstype): Brevsporing? {
        val brevtype = when (historikkinnslagstype) {
            VARSELBREV_SENDT, VARSELBREV_SENDT_TIL_VERGE -> Brevtype.VARSEL
            KORRIGERT_VARSELBREV_SENDT, KORRIGERT_VARSELBREV_SENDT_TIL_VERGE -> Brevtype.KORRIGERT_VARSEL
            VEDTAKSBREV_SENDT, VEDTAKSBREV_SENDT_TIL_VERGE -> Brevtype.VEDTAK
            HENLEGGELSESBREV_SENDT, HENLEGGELSESBREV_SENDT_TIL_VERGE -> Brevtype.HENLEGGELSE
            INNHENT_DOKUMENTASJON_BREV_SENDT, INNHENT_DOKUMENTASJON_BREV_SENDT_TIL_VERGE -> Brevtype.INNHENT_DOKUMENTASJON
            else -> null
        }
        return brevtype?.let {
            brevsporingRepository.findFirstByBehandlingIdAndBrevtypeOrderBySporbarOpprettetTidDesc(behandling.id,
                                                                                                   brevtype)
        }
    }
}
