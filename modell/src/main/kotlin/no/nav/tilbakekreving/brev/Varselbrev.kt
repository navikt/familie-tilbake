package no.nav.tilbakekreving.brev

import no.nav.tilbakekreving.aktør.BrukerBrevmetadata
import no.nav.tilbakekreving.behandling.Behandlingsinformasjon
import no.nav.tilbakekreving.behandling.saksbehandling.RegistrertBrevmottaker
import no.nav.tilbakekreving.eksternfagsak.EksternFagsak
import no.nav.tilbakekreving.entities.BrevEntity
import no.nav.tilbakekreving.entities.Brevtype
import no.nav.tilbakekreving.entities.DatoperiodeEntity
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Period
import java.util.UUID

data class Varselbrev(
    override val id: UUID,
    override val opprettetDato: LocalDate,
    override val brevInformasjon: BrevInformasjon,
    override var journalpostId: String?,
    val varsletBeløp: Long,
    val revurderingsvedtaksdato: LocalDate,
    val fristdatoForTilbakemelding: LocalDate,
    val varseltekstFraSaksbehandler: String,
    val feilutbetaltePerioder: List<Datoperiode>,
) : Brev {
    companion object {
        private val brukersSvarfristOgPostgangstid: LocalDate = LocalDate.now().plus(Period.ofWeeks(3))

        fun opprett(
            bruker: BrukerBrevmetadata,
            behandling: Behandlingsinformasjon,
            mottaker: RegistrertBrevmottaker,
            fagsak: EksternFagsak,
            beløp: BigDecimal,
            feilutbetaltePerioder: List<Datoperiode>,
        ): Brev {
            val brevInformasjon = BrevInformasjon(
                brukerIdent = bruker.personIdent,
                brukerNavn = bruker.navn,
                mottaker = mottaker,
                behandlendeEnhet = behandling.enhet,
                ansvarligSaksbehandler = behandling.ansvarligSaksbehandler.ident,
                saksnummer = fagsak.eksternId,
                språkkode = bruker.språkkode,
                ytelse = fagsak.ytelse,
                gjelderDødsfall = bruker.dødsdato != null,
            )

            return Varselbrev(
                id = UUID.randomUUID(),
                opprettetDato = LocalDate.now(),
                brevInformasjon = brevInformasjon,
                varsletBeløp = beløp.toLong(),
                revurderingsvedtaksdato = fagsak.behandlinger.nåværende().entry.vedtaksdato,
                fristdatoForTilbakemelding = brukersSvarfristOgPostgangstid,
                varseltekstFraSaksbehandler = "Todo ", // todo Kanskje vi skal ha en varselTekst i behandling?
                feilutbetaltePerioder = feilutbetaltePerioder,
                journalpostId = null,
            )
        }
    }

    override fun tilEntity(): BrevEntity {
        return BrevEntity(
            brevType = Brevtype.VARSEL_BREV,
            id = id,
            opprettetDato = opprettetDato,
            brevInformasjonEntity = brevInformasjon.tilEntity(),
            varsletBeløp = varsletBeløp,
            revurderingsvedtaksdato = revurderingsvedtaksdato,
            fristdatoForTilbakemelding = fristdatoForTilbakemelding,
            varseltekstFraSaksbehandler = varseltekstFraSaksbehandler,
            feilutbetaltePerioder = feilutbetaltePerioder.map { DatoperiodeEntity(it.fom, it.tom) },
            journalpostId = journalpostId,
        )
    }
}
