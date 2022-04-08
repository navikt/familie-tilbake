package no.nav.familie.tilbake.historikkinnslag

import no.nav.familie.kontrakter.felles.Applikasjon
import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.kontrakter.felles.historikkinnslag.OpprettHistorikkinnslagRequest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultatstype
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevsporingRepository
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevsporing
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype.BEHANDLING_HENLAGT
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype.BEHANDLING_PÅ_VENT
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype.BREV_IKKE_SENDT_UKJENT_ADRESSE
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype.ENDRET_ENHET
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype.VEDTAK_FATTET
import no.nav.familie.tilbake.integration.kafka.KafkaProducer
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class HistorikkService(private val behandlingRepository: BehandlingRepository,
                       private val fagsakRepository: FagsakRepository,
                       private val brevsporingRepository: BrevsporingRepository,
                       private val kafkaProducer: KafkaProducer) {

    @Transactional
    fun lagHistorikkinnslag(behandlingId: UUID,
                            historikkinnslagstype: TilbakekrevingHistorikkinnslagstype,
                            aktør: Aktør,
                            opprettetTidspunkt: LocalDateTime,
                            beskrivelse: String? = null,
                            brevtype: String? = null) {
        val request = lagHistorikkinnslagRequest(behandlingId = behandlingId,
                                                 aktør = aktør,
                                                 historikkinnslagstype = historikkinnslagstype,
                                                 opprettetTidspunkt = opprettetTidspunkt,
                                                 beskrivelse = beskrivelse,
                                                 brevtype = brevtype)
        kafkaProducer.sendHistorikkinnslag(behandlingId, request.behandlingId, request)
    }

    private fun lagHistorikkinnslagRequest(behandlingId: UUID,
                                           aktør: Aktør,
                                           historikkinnslagstype: TilbakekrevingHistorikkinnslagstype,
                                           opprettetTidspunkt: LocalDateTime,
                                           beskrivelse: String?,
                                           brevtype: String?): OpprettHistorikkinnslagRequest {

        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val brevdata = hentBrevdata(behandling, brevtype)

        return OpprettHistorikkinnslagRequest(behandlingId = behandling.eksternBrukId.toString(),
                                              eksternFagsakId = fagsak.eksternFagsakId,
                                              fagsystem = fagsak.fagsystem,
                                              applikasjon = Applikasjon.FAMILIE_TILBAKE,
                                              type = historikkinnslagstype.type,
                                              aktør = aktør,
                                              aktørIdent = hentAktørIdent(behandling, aktør),
                                              opprettetTidspunkt = opprettetTidspunkt,
                                              steg = historikkinnslagstype.steg?.name,
                                              tittel = historikkinnslagstype.tittel,
                                              tekst = lagTekst(behandling, historikkinnslagstype, beskrivelse),
                                              journalpostId = brevdata?.journalpostId,
                                              dokumentId = brevdata?.dokumentId)
    }

    private fun lagTekst(behandling: Behandling,
                         historikkinnslagstype: TilbakekrevingHistorikkinnslagstype,
                         beskrivelse: String?): String? {
        return when (historikkinnslagstype) {
            BEHANDLING_PÅ_VENT -> historikkinnslagstype.tekst + beskrivelse
            VEDTAK_FATTET -> behandling.sisteResultat?.type?.let { historikkinnslagstype.tekst + it.navn }
            BEHANDLING_HENLAGT -> {
                when (val resultatstype = requireNotNull(behandling.sisteResultat?.type)) {
                    Behandlingsresultatstype.HENLAGT_KRAVGRUNNLAG_NULLSTILT,
                    Behandlingsresultatstype.HENLAGT_TEKNISK_VEDLIKEHOLD -> historikkinnslagstype.tekst + resultatstype.navn
                    else -> historikkinnslagstype.tekst + resultatstype.navn + ", Begrunnelse: " + beskrivelse
                }
            }
            ENDRET_ENHET -> historikkinnslagstype.tekst + behandling.behandlendeEnhet + ", Begrunnelse: " + beskrivelse
            BREV_IKKE_SENDT_UKJENT_ADRESSE -> "$beskrivelse er ikke sendt"
            else -> historikkinnslagstype.tekst
        }
    }

    private fun hentAktørIdent(behandling: Behandling, aktør: Aktør): String {
        return when (aktør) {
            Aktør.VEDTAKSLØSNING -> Constants.BRUKER_ID_VEDTAKSLØSNINGEN
            Aktør.SAKSBEHANDLER -> behandling.ansvarligSaksbehandler
            Aktør.BESLUTTER -> behandling.ansvarligBeslutter!!
        }
    }

    private fun hentBrevdata(behandling: Behandling,
                             brevtypeIString: String?): Brevsporing? {
        val brevtype = brevtypeIString?.let { Brevtype.valueOf(it) }
        return brevtype?.let {
            brevsporingRepository.findFirstByBehandlingIdAndBrevtypeOrderBySporbarOpprettetTidDesc(behandling.id,
                                                                                                   brevtype)
        }
    }
}
