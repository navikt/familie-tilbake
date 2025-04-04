package no.nav.familie.tilbake.historikkinnslag

import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevsporingRepository
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevsporing
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype.BEHANDLING_HENLAGT
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype.BEHANDLING_PÅ_VENT
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype.BREVMOTTAKER_ENDRET
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype.BREVMOTTAKER_FJERNET
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype.BREVMOTTAKER_LAGT_TIL
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype.BREV_IKKE_SENDT_DØDSBO_UKJENT_ADRESSE
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype.BREV_IKKE_SENDT_UKJENT_ADRESSE
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype.DISTRIBUSJON_BREV_DØDSBO_FEILET_6_MND
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype.DISTRIBUSJON_BREV_DØDSBO_SUKSESS
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype.ENDRET_ENHET
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype.VEDTAK_FATTET
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsresultatstype
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class HistorikkService(
    private val behandlingRepository: BehandlingRepository,
    private val brevsporingRepository: BrevsporingRepository,
    private val historikkinnslagRepository: HistorikkinnslagRepository,
) {
    @Transactional
    fun lagHistorikkinnslag(
        behandlingId: UUID,
        historikkinnslagstype: TilbakekrevingHistorikkinnslagstype,
        aktør: Aktør,
        opprettetTidspunkt: LocalDateTime,
        beskrivelse: String? = null,
        brevtype: Brevtype? = null,
        tittel: String? = null,
    ): Historikkinnslag {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val brevdata = hentBrevdata(behandling, brevtype?.name)

        val historikkinnslag =
            Historikkinnslag(
                behandlingId = behandlingId,
                aktør = aktør.type,
                type = historikkinnslagstype.type,
                tittel = tittel ?: historikkinnslagstype.tittel,
                tekst = lagTekst(behandling, historikkinnslagstype, beskrivelse?.replace("\r", "")?.replace("\n", " ")),
                steg = historikkinnslagstype.steg?.name,
                journalpostId = brevdata?.journalpostId,
                dokumentId = brevdata?.dokumentId,
                opprettetAv = aktør.ident,
                opprettetTid = opprettetTidspunkt,
            )
        return historikkinnslagRepository.insert(historikkinnslag)
    }

    @Transactional(readOnly = true)
    fun hentHistorikkinnslag(
        behandlingId: UUID,
    ): List<Historikkinnslag> = historikkinnslagRepository.findByBehandlingId(behandlingId)

    private fun lagTekst(
        behandling: Behandling,
        historikkinnslagstype: TilbakekrevingHistorikkinnslagstype,
        beskrivelse: String?,
    ): String? =
        when (historikkinnslagstype) {
            BEHANDLING_PÅ_VENT -> historikkinnslagstype.tekst + beskrivelse
            VEDTAK_FATTET -> behandling.sisteResultat?.type?.let { historikkinnslagstype.tekst + it.navn }
            BEHANDLING_HENLAGT -> {
                when (val resultatstype = requireNotNull(behandling.sisteResultat?.type)) {
                    Behandlingsresultatstype.HENLAGT_KRAVGRUNNLAG_NULLSTILT,
                    Behandlingsresultatstype.HENLAGT_TEKNISK_VEDLIKEHOLD,
                    -> historikkinnslagstype.tekst + resultatstype.navn
                    else -> historikkinnslagstype.tekst + resultatstype.navn + ", Begrunnelse: " + beskrivelse
                }
            }
            ENDRET_ENHET -> historikkinnslagstype.tekst + behandling.behandlendeEnhet + ", Begrunnelse: " + beskrivelse
            BREV_IKKE_SENDT_UKJENT_ADRESSE -> "$beskrivelse er ikke sendt"
            BREV_IKKE_SENDT_DØDSBO_UKJENT_ADRESSE -> "$beskrivelse er ikke sendt"
            DISTRIBUSJON_BREV_DØDSBO_SUKSESS -> "$beskrivelse er sendt"
            DISTRIBUSJON_BREV_DØDSBO_FEILET_6_MND -> "${historikkinnslagstype.tekst}. $beskrivelse er ikke sendt"
            BREVMOTTAKER_ENDRET, BREVMOTTAKER_LAGT_TIL, BREVMOTTAKER_FJERNET -> beskrivelse
            else -> historikkinnslagstype.tekst
        }

    private fun hentBrevdata(
        behandling: Behandling,
        brevtypeIString: String?,
    ): Brevsporing? {
        val brevtype = brevtypeIString?.let { Brevtype.valueOf(it) }
        return brevtype?.let {
            brevsporingRepository.findFirstByBehandlingIdAndBrevtypeOrderBySporbarOpprettetTidDesc(
                behandling.id,
                brevtype,
            )
        }
    }
}
