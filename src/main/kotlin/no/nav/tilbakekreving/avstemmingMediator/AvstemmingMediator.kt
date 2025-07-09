package no.nav.tilbakekreving.avstemmingMediator

import no.nav.familie.tilbake.config.IntegrasjonerConfig
import no.nav.familie.tilbake.iverksettvedtak.IverksettelseService
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.tilbakekreving.entities.AktørType
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.vedtak.IverksattVedtak
import no.nav.tilbakekreving.vedtak.IverksettRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class AvstemmingMediator(
    private val iverksettelseService: IverksettelseService,
    private val iverksettRepository: IverksettRepository,
    private val integrasjonerConfig: IntegrasjonerConfig,
) {
    private val log = TracedLogger.getLogger<AvstemmingMediator>()
    private final val kvitteringerOkKoder = setOf("00", "04")

    fun avstem(dato: LocalDate): ByteArray? {
        val iverksattVedtakListe = iverksettRepository.hentIverksattVedtakMedOpprettetTid(dato) +
            iverksettelseService.hentGamleVedtak(dato)

        var antallFeilet = 0
        var antallFørstegangsvedtakUtenTilbakekreving = 0

        val avstemmingsrader =
            iverksattVedtakListe.mapNotNull { iverksattVedtak ->
                if (!erKvitteringOK(iverksattVedtak.kvittering)) {
                    antallFeilet++
                    return@mapNotNull null
                }

                val oppsummering = VedtakOppsummering.oppsummer(iverksattVedtak)
                if (erFørstegangsvedtakUtenTilbakekreving(oppsummering, iverksattVedtak)) {
                    antallFørstegangsvedtakUtenTilbakekreving++
                    return@mapNotNull null
                }

                lagAvstemmingsradForVedtaket(iverksattVedtak, oppsummering)
            }
        if (antallFeilet == 0) {
            log.medContext(SecureLog.Context.tom()) {
                info(
                    "Avstemmer {}. Sender {} vedtak til avstemming. Totalt ble {} vedtak sendt til OS dette døgnet. " +
                        "{} førstegangsvedtak uten tilbakekreving sendes ikke til avstemming",
                    dato,
                    avstemmingsrader.size,
                    iverksattVedtakListe.size,
                    antallFørstegangsvedtakUtenTilbakekreving,
                )
            }
        } else {
            log.medContext(SecureLog.Context.tom()) {
                warn(
                    "Avstemmer {}. Sender {} vedtak til avstemming. Totalt ble {} vedtak sendt til OS dette døgnet. " +
                        "{} førstegangsvedtak uten tilbakekreving sendes ikke til avstemming. " +
                        "{} vedtak fikk negativ kvittering fra OS og sendes ikke til avstemming",
                    dato,
                    avstemmingsrader.size,
                    iverksattVedtakListe.size,
                    antallFørstegangsvedtakUtenTilbakekreving,
                    antallFeilet,
                )
            }
        }

        return if (avstemmingsrader.isEmpty()) {
            null
        } else {
            AvstemmingFilMapper(avstemmingsrader).tilFlatfil()
        }
    }

    private fun lagAvstemmingsradForVedtaket(
        iverksattVedtak: IverksattVedtak,
        oppsummering: VedtakOppsummering,
    ): Avstemmingsrad {
        return Avstemmingsrad(
            avsender = integrasjonerConfig.applicationName,
            vedtakId = oppsummering.økonomivedtakId,
            fnr = when (iverksattVedtak.aktør.aktørType) {
                AktørType.Organisasjon -> padOrganisasjonsnummer(iverksattVedtak.aktør.ident)
                else -> iverksattVedtak.aktør.ident
            },
            vedtaksdato = iverksattVedtak.sporbar.opprettetTid.toLocalDate(),
            fagsakYtelseType = iverksattVedtak.ytelsestypeKode,
            tilbakekrevesBruttoUtenRenter = oppsummering.tilbakekrevesBruttoUtenRenter,
            tilbakekrevesNettoUtenRenter = oppsummering.tilbakekrevesNettoUtenRenter,
            skatt = oppsummering.skatt,
            renter = oppsummering.renter,
            erOmgjøringTilIngenTilbakekreving = erOmgjøringTilIngenTilbakekreving(oppsummering, iverksattVedtak),
        )
    }

    private fun padOrganisasjonsnummer(ident: String): String = "00" + ident

    private fun erFørstegangsvedtakUtenTilbakekreving(
        oppsummering: VedtakOppsummering,
        iverksattVedtak: IverksattVedtak,
    ): Boolean = iverksattVedtak.behandlingstype == Behandlingstype.TILBAKEKREVING && oppsummering.harIngenTilbakekreving()

    private fun erOmgjøringTilIngenTilbakekreving(
        oppsummering: VedtakOppsummering,
        iverksattVedtak: IverksattVedtak,
    ): Boolean = iverksattVedtak.behandlingstype == Behandlingstype.REVURDERING_TILBAKEKREVING && oppsummering.harIngenTilbakekreving()

    private fun erKvitteringOK(kvittering: String?): Boolean = kvitteringerOkKoder.contains(kvittering)
}
