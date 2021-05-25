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
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype.BEHANDLING_PÅ_VENT
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype.HENLEGGELSESBREV_SENDT
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype.INNHENT_DOKUMENTASJON_BREV_SENDT
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype.KORRIGERT_VARSELBREV_SENDT
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype.VARSELBREV_SENDT
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype.VEDTAKSBREV_SENDT
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
                            aktør: Aktør) {
        val request = lagHistorikkinnslagRequest(behandlingId = behandlingId,
                                                 aktør = aktør,
                                                 historikkinnslagstype = historikkinnslagstype)
        kafkaProducer.sendHistorikkinnslag(behandlingId, request.behandlingId, request)
    }

    private fun lagHistorikkinnslagRequest(behandlingId: UUID,
                                           aktør: Aktør,
                                           historikkinnslagstype: TilbakekrevingHistorikkinnslagstype)
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
                                              tekst = lagTekst(behandling, historikkinnslagstype),
                                              journalpostId = brevdata?.journalpostId,
                                              dokumentId = brevdata?.dokumentId)
    }

    private fun lagTekst(behandling: Behandling, historikkinnslagstype: TilbakekrevingHistorikkinnslagstype): String? {
        return when (historikkinnslagstype) {
            BEHANDLING_PÅ_VENT -> {
                val stegstilstand = behandlingskontrollService.finnAktivStegstilstand(behandling.id)
                val beskrivelse: String? = stegstilstand?.venteårsak?.beskrivelse
                beskrivelse.let { historikkinnslagstype.tekst + beskrivelse }
            }
            VARSELBREV_SENDT, KORRIGERT_VARSELBREV_SENDT,
            HENLEGGELSESBREV_SENDT, INNHENT_DOKUMENTASJON_BREV_SENDT,
            VEDTAKSBREV_SENDT -> historikkinnslagstype.tekst
            VEDTAK_FATTET -> {
                val resultatstype: Behandlingsresultatstype? = behandling.sisteResultat?.type
                resultatstype?.let { historikkinnslagstype.tekst + it.navn }
            }
            else -> null
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
            VARSELBREV_SENDT -> Brevtype.VARSEL
            KORRIGERT_VARSELBREV_SENDT -> Brevtype.KORRIGERT_VARSEL
            VEDTAKSBREV_SENDT -> Brevtype.VEDTAK
            HENLEGGELSESBREV_SENDT -> Brevtype.HENLEGGELSE
            INNHENT_DOKUMENTASJON_BREV_SENDT -> Brevtype.INNHENT_DOKUMENTASJON
            else -> null
        }
        return brevtype?.let {
            brevsporingRepository.findFirstByBehandlingIdAndBrevtypeOrderBySporbarOpprettetTidDesc(behandling.id,
                                                                                                   brevtype)
        }
    }
}
