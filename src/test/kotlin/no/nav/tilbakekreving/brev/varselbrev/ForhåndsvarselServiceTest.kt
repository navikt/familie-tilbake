package no.nav.tilbakekreving.brev.varselbrev

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.inspectors.forOne
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.familie.tilbake.api.DokumentController
import no.nav.tilbakekreving.Testdata
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.api.v1.dto.BestillBrevDto
import no.nav.tilbakekreving.api.v1.dto.BrukeruttalelseDto
import no.nav.tilbakekreving.api.v1.dto.ForhåndsvarselUnntakDto
import no.nav.tilbakekreving.api.v1.dto.FristUtsettelseDto
import no.nav.tilbakekreving.api.v1.dto.HarBrukerUttaltSeg
import no.nav.tilbakekreving.api.v1.dto.Uttalelsesdetaljer
import no.nav.tilbakekreving.api.v1.dto.VarslingsUnntak
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator.Tilbakekrevingsbeløp.Companion.medFeilutbetaling
import no.nav.tilbakekreving.e2e.TilbakekrevingE2EBase
import no.nav.tilbakekreving.e2e.ytelser.TilleggsstønaderE2ETest.Companion.TILLEGGSSTØNADER_KØ_NAVN
import no.nav.tilbakekreving.fagsystem.FagsystemIntegrasjonService
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.januar
import no.nav.tilbakekreving.kontrakter.brev.Dokumentmalstype
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.util.kroner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class ForhåndsvarselServiceTest : TilbakekrevingE2EBase() {
    @Autowired
    private lateinit var fagsystemIntegrasjonService: FagsystemIntegrasjonService

    @Autowired
    protected lateinit var dokumentController: DokumentController

    @BeforeEach
    fun cleanup() {
        jdbcTemplate.update("DELETE FROM tilbakekreving_uttalelse_informasjon")
        jdbcTemplate.update("DELETE FROM tilbakekreving_forhåndsvarsel_unntak")
        jdbcTemplate.update("DELETE FROM tilbakekreving_utsett_uttalelse")
        jdbcTemplate.update("DELETE FROM tilbakekreving_brukeruttalelse")
    }

    @Test
    fun `henter tekster til varselbrev når det skal sendes forhåndsvarsel`() {
        val tilbakekreving = opprettTilbakekrevingOgHentFagsystemId()
        val tekster = dokumentController.hentForhåndsvarselTekst(tilbakekreving.behandlingHistorikk.nåværende().entry.id)

        tekster.data.shouldNotBeNull()
        tekster.data.avsnitter.shouldNotBeEmpty()
        tekster.data.overskrift shouldContain "Nav vurderer om du må betale tilbake"
        tekster.data.avsnitter.forOne {
            it.title shouldBe ""
            it.body shouldContain "Før vi avgjør om du skal betale tilbake,"
        }
        tekster.data.avsnitter.forOne {
            it.title shouldBe "Dette har skjedd"
            it.body shouldContain "og endringen har ført til at du har fått utbetalt for mye."
        }
        tekster.data.avsnitter.forOne {
            it.title shouldBe "Dette legger vi vekt på i vurderingen vår"
            it.body shouldContain "For å avgjøre om vi kan kreve tilbake,"
        }
        tekster.data.avsnitter.forOne {
            it.title shouldBe "Slik uttaler du deg"
            it.body shouldContain "Du kan sende uttalelsen din ved å logge deg inn på"
        }
        tekster.data.avsnitter.forOne {
            it.title shouldBe "Har du spørsmål?"
            it.body shouldContain "Du finner mer informasjon på nav.no/tilleggsstonad."
        }
        tekster.data.avsnitter.forOne {
            it.title shouldBe "Du har rett til innsyn i saken din"
            it.body shouldContain "Du har rett til å se dokumentene i saken din. Dette følger av forvaltningsloven § 18"
        }
        tekster.data.avsnitter.forOne {
            it.title shouldBe "Du har rettigheter knyttet til personopplysningene dine"
            it.body shouldContain "Du finner informasjon om hvordan Nav behandler personopplysningene dine,"
        }
    }

    @Test
    fun `forhåndsvarsel detaljene er null når varselbrev ikke er sendt`() {
        val tilbakekreving = opprettTilbakekrevingOgHentFagsystemId()
        val behandling = tilbakekreving.behandlingHistorikk.nåværende().entry

        val antall = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM tilbakekreving_brukeruttalelse WHERE behandling_ref = ?",
            Int::class.java,
            behandling.id,
        )
        antall shouldBe 0

        val antallUttalelseinfo = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM tilbakekreving_uttalelse_informasjon",
            Int::class.java,
        )
        antallUttalelseinfo shouldBe 0

        val forhåndsvarselDto = tilbakekreving.hentForhåndsvarselFrontendDto()

        forhåndsvarselDto.varselbrevDto shouldBe null
        forhåndsvarselDto.brukeruttalelse shouldBe null
        forhåndsvarselDto.forhåndsvarselUnntak shouldBe null
    }

    @Test
    fun `sende forhåndsvarsel skal oppdatere varselbrevet i brevhistorikk med tid og journlaførtId`() {
        val tilbakekreving = opprettTilbakekrevingOgHentFagsystemId()
        val behandling = tilbakekreving.behandlingHistorikk.nåværende().entry
        val bestillBrevDto = BestillBrevDto(
            behandlingId = behandling.id,
            brevmalkode = Dokumentmalstype.VARSEL,
            fritekst = "Tekst fra saksbehandler",
        )

        tilbakekreving.brevHistorikk.sisteVarselbrev() shouldBe null
        dokumentController.bestillBrev(bestillBrevDto)

        val tilbakekrevingEtterVarselbrev = tilbakekrevingService.hentTilbakekreving(FagsystemDTO.TS, tilbakekreving.eksternFagsak.eksternId)
        tilbakekrevingEtterVarselbrev!!.brevHistorikk.sisteVarselbrev() shouldNotBeNull {
            journalpostId shouldBe "-1"
        }
    }

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("uttalelseGyldigeCases")
    fun `gyldige brukeruttalelser lagres riktig`(case: GyldigBrukeruttalelseCase) {
        val tilbakekreving = opprettTilbakekrevingOgHentFagsystemId()
        val behandling = tilbakekreving.behandlingHistorikk.nåværende().entry

        val bestillBrevDto = BestillBrevDto(
            behandlingId = behandling.id,
            brevmalkode = Dokumentmalstype.VARSEL,
            fritekst = "Tekst fra saksbehandler",
        )
        dokumentController.bestillBrev(bestillBrevDto)

        dokumentController.lagreBrukeruttalelse(behandling.id, case.input)

        val forhåndsvarsel = tilbakekrevingService
            .hentTilbakekreving(FagsystemDTO.TS, tilbakekreving.eksternFagsak.eksternId)
            .shouldNotBeNull().hentForhåndsvarselFrontendDto()
        forhåndsvarsel.forhåndsvarselUnntak shouldBe null
        forhåndsvarsel.utsettUttalelseFrist.shouldBeEmpty()

        val brukeruttalelse = forhåndsvarsel.brukeruttalelse.shouldNotBeNull()

        brukeruttalelse.harBrukerUttaltSeg shouldBe case.forventetHarBrukerUttaltSeg
        brukeruttalelse.uttalelsesdetaljer.orEmpty().size shouldBe case.forventetAntallDetaljer
        brukeruttalelse.kommentar shouldBe case.forventetKommentar
    }

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("uttalelseUgyldigeCases")
    fun `ugyldige brukeruttalelser gir valideringsfeil`(case: UgyldigBrukeruttalelseCase) {
        val tilbakekreving = opprettTilbakekrevingOgHentFagsystemId()
        val behandling = tilbakekreving.behandlingHistorikk.nåværende().entry
        val bestillBrevDto = BestillBrevDto(
            behandlingId = behandling.id,
            brevmalkode = Dokumentmalstype.VARSEL,
            fritekst = "Tekst fra saksbehandler",
        )
        dokumentController.bestillBrev(bestillBrevDto)

        shouldThrow<Exception> {
            dokumentController.lagreBrukeruttalelse(behandling.id, case.input)
        }.message shouldBe case.forventetFeilmelding
    }

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("forhåndsvarseUnntakCases")
    fun `forhåndsvarsel unntak lagres og hentes riktig`(case: ForhåndsvarselUnntakCase) {
        val tilbakekreving = opprettTilbakekrevingOgHentFagsystemId()
        val behandling = tilbakekreving.behandlingHistorikk.nåværende().entry

        dokumentController.forhåndsvarselUnntak(behandling.id, case.input)
        val forhåndsvarsel = tilbakekrevingService
            .hentTilbakekreving(FagsystemDTO.TS, tilbakekreving.eksternFagsak.eksternId)
            .shouldNotBeNull().hentForhåndsvarselFrontendDto()
        forhåndsvarsel.brukeruttalelse shouldBe null
        forhåndsvarsel.utsettUttalelseFrist.shouldBeEmpty()

        forhåndsvarsel.forhåndsvarselUnntak.shouldNotBeNull {
            begrunnelseForUnntak shouldBe case.forventetBegrunnelseForUnntak
            beskrivelse shouldBe case.forventetBeskrivelse
        }
    }

    @Test
    fun `utsettelse av frist lagres og hentes riktig`() {
        val tilbakekreving = opprettTilbakekrevingOgHentFagsystemId()
        val behandling = tilbakekreving.behandlingHistorikk.nåværende().entry
        val bestillBrevDto = BestillBrevDto(
            behandlingId = behandling.id,
            brevmalkode = Dokumentmalstype.VARSEL,
            fritekst = "Tekst fra saksbehandler",
        )
        dokumentController.bestillBrev(bestillBrevDto)

        val førsteFrist = FristUtsettelseDto(
            LocalDate.of(2025, 11, 15),
            "Advokat vil ha mer tid",
        )
        dokumentController.utsettUttalelseFrist(behandling.id, førsteFrist)

        val etterFørsteUtsettelse = tilbakekrevingService
            .hentTilbakekreving(FagsystemDTO.TS, tilbakekreving.eksternFagsak.eksternId)
            .shouldNotBeNull().hentForhåndsvarselFrontendDto()

        etterFørsteUtsettelse.utsettUttalelseFrist.shouldNotBeNull {
            size shouldBe 1
            etterFørsteUtsettelse.utsettUttalelseFrist?.get(0)?.nyFrist shouldBe LocalDate.of(2025, 11, 15)
        }

        val andreFrist = FristUtsettelseDto(
            LocalDate.of(2025, 11, 25),
            "Advokat vil ha enda mer tid",
        )
        dokumentController.utsettUttalelseFrist(behandling.id, andreFrist)

        val etterAndreUtsettelse = tilbakekrevingService
            .hentTilbakekreving(FagsystemDTO.TS, tilbakekreving.eksternFagsak.eksternId)
            .shouldNotBeNull().hentForhåndsvarselFrontendDto()
        etterAndreUtsettelse.utsettUttalelseFrist.shouldNotBeNull {
            size shouldBe 2
            etterAndreUtsettelse.utsettUttalelseFrist.get(0).nyFrist shouldBe LocalDate.of(2025, 11, 15)
            etterAndreUtsettelse.utsettUttalelseFrist.get(1).nyFrist shouldBe LocalDate.of(2025, 11, 25)
        }
        etterAndreUtsettelse.forhåndsvarselUnntak shouldBe null
        etterAndreUtsettelse.brukeruttalelse shouldBe null
    }

    private fun opprettTilbakekrevingOgHentFagsystemId(): Tilbakekreving {
        val fnr = "12312312311"
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        val vedtakId = KravgrunnlagGenerator.nextPaddedId(6)
        val ansvarligEnhet = KravgrunnlagGenerator.nextPaddedId(4)
        sendKravgrunnlagOgAvventLesing(
            TILLEGGSSTØNADER_KØ_NAVN,
            KravgrunnlagGenerator.forTilleggsstønader(
                fødselsnummer = fnr,
                fagsystemId = fagsystemId,
                vedtakId = vedtakId,
                ansvarligEnhet = ansvarligEnhet,
                perioder = listOf(
                    KravgrunnlagGenerator.Tilbakekrevingsperiode(
                        1.januar(2021) til 1.januar(2021),
                        tilbakekrevingsbeløp = listOf(
                            KravgrunnlagGenerator.Tilbakekrevingsbeløp.forKlassekode(
                                klassekode = KravgrunnlagGenerator.NyKlassekode.TSTBASISP4_OP,
                                beløpTilbakekreves = 2000.kroner,
                                beløpOpprinneligUtbetalt = 20000.kroner,
                            ),
                        ).medFeilutbetaling(KravgrunnlagGenerator.NyKlassekode.KL_KODE_FEIL_ARBYT),
                    ),
                ),
            ),
        )
        fagsystemIntegrasjonService.håndter(Ytelse.Tilleggsstønad, Testdata.fagsysteminfoSvar(fagsystemId))
        return tilbakekrevingService.hentTilbakekreving(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
    }

    companion object {
        @JvmStatic
        fun forhåndsvarseUnntakCases() = listOf(
            ForhåndsvarselUnntakCase(
                navn = "Unntak IKKE_PRAKTISK_MULIG",
                input = ForhåndsvarselUnntakDto(
                    begrunnelseForUnntak = VarslingsUnntak.IKKE_PRAKTISK_MULIG,
                    beskrivelse = "Ikke mulig",
                ),
                forventetBegrunnelseForUnntak = VarslingsUnntak.IKKE_PRAKTISK_MULIG,
                forventetBeskrivelse = "Ikke mulig",
            ),
            ForhåndsvarselUnntakCase(
                navn = "Unntak UKJENT_ADRESSE",
                input = ForhåndsvarselUnntakDto(
                    begrunnelseForUnntak = VarslingsUnntak.UKJENT_ADRESSE_ELLER_URIMELIG_ETTERSPORING,
                    beskrivelse = "Ukjent adresse",
                ),
                forventetBegrunnelseForUnntak = VarslingsUnntak.UKJENT_ADRESSE_ELLER_URIMELIG_ETTERSPORING,
                forventetBeskrivelse = "Ukjent adresse",
            ),
            ForhåndsvarselUnntakCase(
                navn = "Unntak ÅPENBART_UNØDVENDIG",
                input = ForhåndsvarselUnntakDto(
                    begrunnelseForUnntak = VarslingsUnntak.ÅPENBART_UNØDVENDIG,
                    beskrivelse = "Åpenbart unødvendig",
                ),
                forventetBegrunnelseForUnntak = VarslingsUnntak.ÅPENBART_UNØDVENDIG,
                forventetBeskrivelse = "Åpenbart unødvendig",
            ),
        )

        @JvmStatic
        fun uttalelseGyldigeCases() = listOf(
            // 1) Har bruker uttalet seg: JA, én uttalelse
            GyldigBrukeruttalelseCase(
                navn = "JA – én uttalelse (Modia)",
                input = BrukeruttalelseDto(
                    harBrukerUttaltSeg = HarBrukerUttaltSeg.JA_ETTER_FORHÅNDSVARSEL,
                    uttalelsesdetaljer = listOf(
                        Uttalelsesdetaljer(
                            hvorBrukerenUttalteSeg = "Modia",
                            uttalelsesdato = LocalDate.of(2025, 10, 15),
                            uttalelseBeskrivelse = "Bruker har uttalet seg",
                        ),
                    ),
                    kommentar = null,
                ),
                forventetHarBrukerUttaltSeg = HarBrukerUttaltSeg.JA_ETTER_FORHÅNDSVARSEL,
                forventetAntallDetaljer = 1,
                forventetKommentar = null,
            ),
            // 2)Har bruker uttalet seg:  JA, to uttalelser
            GyldigBrukeruttalelseCase(
                navn = "JA – to uttalelser (Tlf + Modia)",
                input = BrukeruttalelseDto(
                    harBrukerUttaltSeg = HarBrukerUttaltSeg.JA_ETTER_FORHÅNDSVARSEL,
                    uttalelsesdetaljer = listOf(
                        Uttalelsesdetaljer(
                            hvorBrukerenUttalteSeg = "Tlf",
                            uttalelsesdato = LocalDate.of(2025, 9, 15),
                            uttalelseBeskrivelse = "Bruker har sagt ...",
                        ),
                        Uttalelsesdetaljer(
                            hvorBrukerenUttalteSeg = "Modia",
                            uttalelsesdato = LocalDate.of(2025, 9, 17),
                            uttalelseBeskrivelse = "Bruker har skrevet ...",
                        ),
                    ),
                    kommentar = null,
                ),
                forventetHarBrukerUttaltSeg = HarBrukerUttaltSeg.JA_ETTER_FORHÅNDSVARSEL,
                forventetAntallDetaljer = 2,
                forventetKommentar = null,
            ),
            // 3) Har bruker uttalet seg:NEI
            GyldigBrukeruttalelseCase(
                navn = "NEI – med kommentar",
                input = BrukeruttalelseDto(
                    harBrukerUttaltSeg = HarBrukerUttaltSeg.NEI_ETTER_FORHÅNDSVARSEL,
                    uttalelsesdetaljer = null,
                    kommentar = "Ville ikke",
                ),
                forventetHarBrukerUttaltSeg = HarBrukerUttaltSeg.NEI_ETTER_FORHÅNDSVARSEL,
                forventetAntallDetaljer = 0,
                forventetKommentar = "Ville ikke",
            ),
            GyldigBrukeruttalelseCase(
                navn = "ALLEREDE_UTTALET_SEG",
                input = BrukeruttalelseDto(
                    harBrukerUttaltSeg = HarBrukerUttaltSeg.UNNTAK_ALLEREDE_UTTALET_SEG,
                    uttalelsesdetaljer = listOf(
                        Uttalelsesdetaljer(
                            hvorBrukerenUttalteSeg = "Tlf",
                            uttalelsesdato = LocalDate.of(2025, 9, 15),
                            uttalelseBeskrivelse = "Bruker har sagt ...",
                        ),
                    ),
                    kommentar = null,
                ),
                forventetHarBrukerUttaltSeg = HarBrukerUttaltSeg.UNNTAK_ALLEREDE_UTTALET_SEG,
                forventetAntallDetaljer = 1,
                forventetKommentar = null,
            ),
        )

        @JvmStatic
        fun uttalelseUgyldigeCases() = listOf(
            UgyldigBrukeruttalelseCase(
                navn = "JA – uten detaljer (null)",
                input = BrukeruttalelseDto(
                    harBrukerUttaltSeg = HarBrukerUttaltSeg.JA_ETTER_FORHÅNDSVARSEL,
                    uttalelsesdetaljer = null,
                    kommentar = null,
                ),
                forventetFeilmelding = "Det kreves uttalelsedetaljer når brukeren har uttalet seg. uttalelsedetaljer var null",
            ),
            UgyldigBrukeruttalelseCase(
                navn = "JA – tom liste med detaljer",
                input = BrukeruttalelseDto(
                    harBrukerUttaltSeg = HarBrukerUttaltSeg.JA_ETTER_FORHÅNDSVARSEL,
                    uttalelsesdetaljer = listOf(),
                    kommentar = null,
                ),
                forventetFeilmelding = "Det kreves uttalelsedetaljer når brukeren har uttalet seg. uttalelsedetaljer var tom",
            ),
            UgyldigBrukeruttalelseCase(
                navn = "NEI – uten kommentar (null)",
                input = BrukeruttalelseDto(
                    harBrukerUttaltSeg = HarBrukerUttaltSeg.NEI_ETTER_FORHÅNDSVARSEL,
                    uttalelsesdetaljer = null,
                    kommentar = null,
                ),
                forventetFeilmelding = "Det kreves en kommentar når brukeren ikke uttaler seg. Kommentar var null",
            ),
            UgyldigBrukeruttalelseCase(
                navn = "NEI – tom kommentar ('')",
                input = BrukeruttalelseDto(
                    harBrukerUttaltSeg = HarBrukerUttaltSeg.NEI_ETTER_FORHÅNDSVARSEL,
                    uttalelsesdetaljer = null,
                    kommentar = "",
                ),
                forventetFeilmelding = "Det kreves en kommentar når brukeren ikke uttaler seg. Kommentar var tom",
            ),
        )
    }
}

data class GyldigBrukeruttalelseCase(
    val navn: String,
    val input: BrukeruttalelseDto,
    val forventetHarBrukerUttaltSeg: HarBrukerUttaltSeg?,
    val forventetAntallDetaljer: Int,
    val forventetKommentar: String?,
)

data class UgyldigBrukeruttalelseCase(
    val navn: String,
    val input: BrukeruttalelseDto,
    val forventetFeilmelding: String,
)

data class ForhåndsvarselUnntakCase(
    val navn: String,
    val input: ForhåndsvarselUnntakDto,
    val forventetBegrunnelseForUnntak: VarslingsUnntak,
    val forventetBeskrivelse: String,
)
