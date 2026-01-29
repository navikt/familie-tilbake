package no.nav.tilbakekreving.e2e.brev

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.api.DokumentController
import no.nav.tilbakekreving.Testdata
import no.nav.tilbakekreving.api.v1.dto.BestillBrevDto
import no.nav.tilbakekreving.api.v1.dto.BrukeruttalelseDto
import no.nav.tilbakekreving.api.v1.dto.ForhåndsvarselUnntakDto
import no.nav.tilbakekreving.api.v1.dto.HarBrukerUttaltSeg
import no.nav.tilbakekreving.api.v1.dto.Uttalelsesdetaljer
import no.nav.tilbakekreving.api.v1.dto.VarslingsUnntak
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator
import no.nav.tilbakekreving.e2e.TilbakekrevingE2EBase
import no.nav.tilbakekreving.e2e.ytelser.TilleggsstønaderE2ETest.Companion.TILLEGGSSTØNADER_KØ_NAVN
import no.nav.tilbakekreving.fagsystem.FagsystemIntegrasjonService
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.kontrakter.brev.Dokumentmalstype
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID

class ForhåndsvarselE2ETest : TilbakekrevingE2EBase() {
    @Autowired
    private lateinit var fagsystemIntegrasjonService: FagsystemIntegrasjonService

    @Autowired
    private lateinit var documentController: DokumentController

    @Test
    fun `forhåndsvarsel sendes og lagres riktig i DB`() {
        val behandlingId = hentBehandlingId()
        val tilbakekrevingFørForhåndsvarsel = tilbakekreving(behandlingId)
        documentController.bestillBrev(
            BestillBrevDto(
                behandlingId = behandlingId,
                brevmalkode = Dokumentmalstype.VARSEL,
                fritekst = "Tekst fra saksbehandler",
            ),
        )
        val tilbakekrevingEtterForhåndsvarsel = tilbakekreving(behandlingId)

        tilbakekrevingFørForhåndsvarsel.brevHistorikk.sisteVarselbrev().shouldBeNull()
        tilbakekrevingEtterForhåndsvarsel.brevHistorikk.sisteVarselbrev().shouldNotBeNull {
            tekstFraSaksbehandler shouldBe "Tekst fra saksbehandler"
        }
    }

    @Test
    fun `uttalelse kan redigeres`() {
        val behandlingId = hentBehandlingId()

        documentController.lagreBrukeruttalelse(behandlingId, hentBrukerUttalelseDto(LocalDate.of(2026, 1, 2)))

        documentController.hentForhåndsvarselinfo(behandlingId).data.shouldNotBeNull {
            brukeruttalelse.shouldNotBeNull {
                harBrukerUttaltSeg shouldBe HarBrukerUttaltSeg.JA
                uttalelsesdetaljer.shouldNotBeNull {
                    size shouldBe 1
                    get(0).uttalelsesdato shouldBe LocalDate.of(2026, 1, 2)
                }
            }
        }

        documentController.lagreBrukeruttalelse(behandlingId, hentBrukerUttalelseDto(LocalDate.of(2025, 12, 2)))

        documentController.hentForhåndsvarselinfo(behandlingId).data.shouldNotBeNull {
            brukeruttalelse.shouldNotBeNull {
                harBrukerUttaltSeg shouldBe HarBrukerUttaltSeg.JA
                uttalelsesdetaljer.shouldNotBeNull {
                    size shouldBe 1
                    get(0).uttalelsesdato shouldBe LocalDate.of(2025, 12, 2)
                }
            }
        }
    }

    @Test
    fun `unntak kan redigeres`() {
        val behandlingId = hentBehandlingId()

        documentController.forhåndsvarselUnntak(
            behandlingId,
            ForhåndsvarselUnntakDto(
                begrunnelseForUnntak = VarslingsUnntak.IKKE_PRAKTISK_MULIG,
                beskrivelse = "Ikke mulig",
            ),
        )

        documentController.hentForhåndsvarselinfo(behandlingId).data.shouldNotBeNull {
            forhåndsvarselUnntak.shouldNotBeNull {
                begrunnelseForUnntak shouldBe VarslingsUnntak.IKKE_PRAKTISK_MULIG
                beskrivelse shouldBe "Ikke mulig"
            }
        }

        documentController.forhåndsvarselUnntak(
            behandlingId,
            ForhåndsvarselUnntakDto(
                begrunnelseForUnntak = VarslingsUnntak.UKJENT_ADRESSE_ELLER_URIMELIG_ETTERSPORING,
                beskrivelse = "Ukjent adresse",
            ),
        )

        documentController.hentForhåndsvarselinfo(behandlingId).data.shouldNotBeNull {
            forhåndsvarselUnntak.shouldNotBeNull {
                begrunnelseForUnntak shouldBe VarslingsUnntak.UKJENT_ADRESSE_ELLER_URIMELIG_ETTERSPORING
                beskrivelse shouldBe "Ukjent adresse"
            }
        }
    }

    @Test
    fun `unntak med alternativ ÅPENBART_UNØDVENDIG og registrert uttalelse kan redigeres`() {
        val behandlingId = hentBehandlingId()

        documentController.forhåndsvarselUnntak(
            behandlingId,
            ForhåndsvarselUnntakDto(
                begrunnelseForUnntak = VarslingsUnntak.ÅPENBART_UNØDVENDIG,
                beskrivelse = "allerede uttalet seg",
            ),
        )

        documentController.lagreBrukeruttalelse(behandlingId, hentBrukerUttalelseDto(LocalDate.of(2026, 1, 2)))

        documentController.hentForhåndsvarselinfo(behandlingId).data.shouldNotBeNull {
            forhåndsvarselUnntak.shouldNotBeNull {
                begrunnelseForUnntak shouldBe VarslingsUnntak.ÅPENBART_UNØDVENDIG
                beskrivelse shouldBe "allerede uttalet seg"
            }
            brukeruttalelse.shouldNotBeNull {
                harBrukerUttaltSeg shouldBe HarBrukerUttaltSeg.JA
                uttalelsesdetaljer.shouldNotBeNull {
                    get(0).uttalelsesdato shouldBe LocalDate.of(2026, 1, 2)
                }
            }
        }

        documentController.lagreBrukeruttalelse(behandlingId, hentBrukerUttalelseDto(LocalDate.of(2025, 12, 2)))

        documentController.hentForhåndsvarselinfo(behandlingId).data.shouldNotBeNull {
            forhåndsvarselUnntak.shouldNotBeNull()
            brukeruttalelse.shouldNotBeNull {
                harBrukerUttaltSeg shouldBe HarBrukerUttaltSeg.JA
                uttalelsesdetaljer.shouldNotBeNull {
                    get(0).uttalelsesdato shouldBe LocalDate.of(2025, 12, 2)
                }
            }
        }

        documentController.lagreBrukeruttalelse(
            behandlingId,
            BrukeruttalelseDto(
                harBrukerUttaltSeg = HarBrukerUttaltSeg.NEI,
                uttalelsesdetaljer = null,
                kommentar = "har ikke uttalet seg",
            ),
        )

        documentController.hentForhåndsvarselinfo(behandlingId).data.shouldNotBeNull {
            forhåndsvarselUnntak.shouldNotBeNull()
            brukeruttalelse.shouldNotBeNull {
                harBrukerUttaltSeg shouldBe HarBrukerUttaltSeg.NEI
                uttalelsesdetaljer.shouldBeEmpty()
                kommentar shouldBe "har ikke uttalet seg"
            }
        }

        documentController.forhåndsvarselUnntak(
            behandlingId,
            ForhåndsvarselUnntakDto(
                begrunnelseForUnntak = VarslingsUnntak.IKKE_PRAKTISK_MULIG,
                beskrivelse = "Unntak",
            ),
        )

        documentController.hentForhåndsvarselinfo(behandlingId).data.shouldNotBeNull {
            forhåndsvarselUnntak.shouldNotBeNull {
                begrunnelseForUnntak shouldBe VarslingsUnntak.IKKE_PRAKTISK_MULIG
                beskrivelse shouldBe "Unntak"
            }
            brukeruttalelse.shouldBeNull()
        }
    }

    private fun hentBehandlingId(): UUID {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
            ),
        )
        fagsystemIntegrasjonService.håndter(Ytelse.Tilleggsstønad, Testdata.fagsysteminfoSvar(fagsystemId))
        return behandlingIdFor(fagsystemId, FagsystemDTO.TS).shouldNotBeNull()
    }

    private fun hentBrukerUttalelseDto(uttalelsesdato: LocalDate) = BrukeruttalelseDto(
        harBrukerUttaltSeg = HarBrukerUttaltSeg.JA,
        uttalelsesdetaljer = listOf(
            Uttalelsesdetaljer(
                uttalelsesdato = uttalelsesdato,
                hvorBrukerenUttalteSeg = "Godsys",
                uttalelseBeskrivelse = "uttalelse",
            ),
        ),
        kommentar = null,
    )
}
