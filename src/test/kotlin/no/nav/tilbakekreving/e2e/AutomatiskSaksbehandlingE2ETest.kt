package no.nav.tilbakekreving.e2e

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import no.nav.tilbakekreving.Testdata
import no.nav.tilbakekreving.e2e.ytelser.TilleggsstønaderE2ETest.Companion.TILLEGGSSTØNADER_KØ_NAVN
import no.nav.tilbakekreving.fagsystem.FagsystemIntegrasjonService
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.kontrakter.foreldelse.Foreldelsesvurderingstype
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class AutomatiskSaksbehandlingE2ETest : TilbakekrevingE2EBase() {
    @Autowired
    private lateinit var fagsystemIntegrasjonService: FagsystemIntegrasjonService

    @Test
    fun `automatisk vurdering av foreldelse blir lagret med begrunnelse`() {
        val fom = LocalDate.now().minusMonths(10).withDayOfMonth(1)
        val tom = fom.withDayOfMonth(fom.lengthOfMonth())
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)

        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
                perioder = listOf(KravgrunnlagGenerator.standardPeriode(fom til tom)),
            ),
        )
        fagsystemIntegrasjonService.håndter(
            Ytelse.Tilleggsstønad,
            Testdata.fagsysteminfoSvar(fagsystemId = fagsystemId, utvidPerioder = emptyList()),
        )

        val behandlingId = behandlingIdFor(fagsystemId, FagsystemDTO.TS).shouldNotBeNull()
        val periode = tilbakekreving(behandlingId)
            .behandlingHistorikk.nåværende().entry
            .foreldelsestegDto.tilFrontendDto()
            .foreldetPerioder.single()

        periode.foreldelsesvurderingstype shouldBe Foreldelsesvurderingstype.IKKE_FORELDET

        periode.begrunnelse shouldStartWith "Ingen perioder er foreldet fordi det er mindre enn tre år siden første feilutbetaling fant sted. Dette følger av foreldelsesloven §§ 2 og 3."
    }
}
