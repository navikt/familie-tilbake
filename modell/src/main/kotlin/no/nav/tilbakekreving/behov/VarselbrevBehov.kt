package no.nav.tilbakekreving.behov

import no.nav.tilbakekreving.aktør.Brukerinfo
import no.nav.tilbakekreving.behandling.Enhet
import no.nav.tilbakekreving.brev.Varselbrev
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.time.LocalDate
import java.util.UUID

data class VarselbrevBehov(
    val brevId: UUID,
    val brukerinfo: Brukerinfo,
    val behandlingId: UUID,
    val varselbrev: Varselbrev,
    val revurderingsvedtaksdato: LocalDate,
    val varseltekstFraSaksbehandler: String,
    val eksternFagsakId: String,
    val ytelse: Ytelse,
    val behandlendeEnhet: Enhet?,
    val feilutbetaltBeløp: Long,
    val feilutbetaltePerioder: List<Datoperiode>,
    val gjelderDødsfall: Boolean,
) : Behov
