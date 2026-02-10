package no.nav.tilbakekreving.aktør

import no.nav.kontrakter.frontend.models.BrevmottakerDto
import no.nav.tilbakekreving.FrontendDto
import no.nav.tilbakekreving.behov.BehovObservatør
import no.nav.tilbakekreving.behov.BrukerinfoBehov
import no.nav.tilbakekreving.entities.BrukerEntity
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.hendelse.BrukerinfoHendelse
import no.nav.tilbakekreving.kontrakter.bruker.FrontendBrukerDto
import no.nav.tilbakekreving.kontrakter.bruker.Kjønn
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import java.time.LocalDate
import java.util.UUID

class Bruker(
    val aktør: Aktør,
    var språkkode: Språkkode? = null,
    private var navn: String? = null,
    private var fødselsdato: LocalDate? = null,
    private var kjønn: Kjønn? = null,
    private var dødsdato: LocalDate? = null,
) : FrontendDto<FrontendBrukerDto> {
    override fun tilFrontendDto(): FrontendBrukerDto {
        return FrontendBrukerDto(
            personIdent = aktør.ident,
            navn = navn ?: "Ukjent",
            fødselsdato = fødselsdato,
            kjønn = kjønn ?: Kjønn.UKJENT,
            dødsdato = dødsdato,
        )
    }

    fun trengerBrukerinfo(
        behovObservatør: BehovObservatør,
        ytelse: Ytelse,
    ) {
        behovObservatør.håndter(
            BrukerinfoBehov(
                ident = aktør.ident,
                ytelse = ytelse,
            ),
        )
    }

    fun tilEntity(tilbakekrevingId: String): BrukerEntity {
        return BrukerEntity(
            id = UUID.randomUUID(),
            tilbakekrevingRef = tilbakekrevingId,
            aktørEntity = aktør.tilEntity(),
            språkkode = språkkode,
            navn = navn,
            fødselsdato = fødselsdato,
            kjønn = kjønn,
            dødsdato = dødsdato,
        )
    }

    fun oppdater(hendelse: BrukerinfoHendelse) {
        navn = hendelse.navn
        fødselsdato = hendelse.fødselsdato
        kjønn = hendelse.kjønn
        dødsdato = hendelse.dødsdato ?: dødsdato
        språkkode = hendelse.språkkode ?: språkkode
    }

    fun hentBrukerinfo(): Brukerinfo = Brukerinfo(
        ident = aktør.ident,
        navn = navn!!,
        språkkode = språkkode ?: Språkkode.NB,
        dødsdato = dødsdato,
    )

    fun brevmeta(): BrevmottakerDto {
        return BrevmottakerDto(
            navn = navn!!,
            personIdent = aktør.ident,
        )
    }

    companion object {
        fun Bruker?.tilNullableFrontendDto(): FrontendBrukerDto {
            return this?.tilFrontendDto() ?: FrontendBrukerDto(
                personIdent = "Ukjent",
                navn = "Ukjent",
                fødselsdato = null,
                kjønn = Kjønn.UKJENT,
                dødsdato = null,
            )
        }
    }
}
