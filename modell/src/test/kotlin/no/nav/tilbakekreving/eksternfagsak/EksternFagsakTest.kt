package no.nav.tilbakekreving.eksternfagsak

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import no.nav.tilbakekreving.KlokkeStub
import no.nav.tilbakekreving.SystemKlokke
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.fagsysteminfoHendelse
import no.nav.tilbakekreving.hendelse.FagsysteminfoHendelse
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.test.februar
import no.nav.tilbakekreving.test.januar
import org.junit.jupiter.api.Named
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate
import java.util.UUID

class EksternFagsakTest {
    private fun eksternFagsak() = EksternFagsak(
        id = UUID.randomUUID(),
        eksternId = null,
        eksternIdFraOppdrag = "101010",
        ytelse = Ytelse.Barnetrygd,
        behandlinger = EksternFagsakBehandlingHistorikk(mutableListOf()),
    )

    @Test
    fun `samme fagsysteminfo mottatt to ganger`() {
        val eksternFagsak = eksternFagsak()
        val klokke = KlokkeStub(1.januar(2021))

        val første = eksternFagsak.lagre(fagsysteminfoHendelse(behandlingId = "1"), klokke)
        klokke.settTid(2.januar(2021))
        val andre = eksternFagsak.lagre(fagsysteminfoHendelse(behandlingId = "1"), klokke)

        andre shouldBe null
        eksternFagsak.behandlinger.nåværende().entry shouldBeSameInstanceAs første?.entry
    }

    @Test
    fun `lik fagsysteminfo med utvidede perioder i annen rekkefølge`() {
        val eksternFagsak = eksternFagsak()
        val utvidedePerioder = listOf(
            FagsysteminfoHendelse.UtvidetPeriode(
                kravgrunnlagPeriode = 1.januar(2021) til 15.januar(2021),
                vedtaksperiode = 1.januar(2021) til 31.januar(2021),
            ),
            FagsysteminfoHendelse.UtvidetPeriode(
                kravgrunnlagPeriode = 1.februar(2021) til 15.februar(2021),
                vedtaksperiode = 1.februar(2021) til 28.februar(2021),
            ),
        )
        val fagsysteminfo = fagsysteminfoHendelse(behandlingId = "1", utvidPerioder = utvidedePerioder)

        val første = eksternFagsak.lagre(fagsysteminfo, SystemKlokke)
        val andre = eksternFagsak.lagre(fagsysteminfoHendelse(behandlingId = "1", utvidPerioder = utvidedePerioder.reversed()), SystemKlokke)

        andre shouldBe null
        eksternFagsak.behandlinger.nåværende().entry shouldBeSameInstanceAs første?.entry
    }

    @ParameterizedTest
    @MethodSource("endringerIRevurdering")
    fun `fagsysteminfo med ett felt endret i revurderingen`(endretFagsysteminfo: FagsysteminfoHendelse) {
        val eksternFagsak = eksternFagsak()

        val første = eksternFagsak.lagre(fagsysteminfoHendelse(behandlingId = "1"), SystemKlokke).shouldNotBeNull()
        val andre = eksternFagsak.lagre(endretFagsysteminfo, SystemKlokke).shouldNotBeNull()

        andre.entry shouldNotBeSameInstanceAs første.entry
        eksternFagsak.behandlinger.nåværende().entry shouldBeSameInstanceAs andre.entry
    }

    @Test
    fun `fagsysteminfo med endret vedtaksperiode i en utvidet periode`() {
        val eksternFagsak = eksternFagsak()
        val fagsysteminfo = fagsysteminfoHendelse(
            behandlingId = "1",
            utvidPerioder = listOf(
                FagsysteminfoHendelse.UtvidetPeriode(
                    kravgrunnlagPeriode = 1.januar(2021) til 15.januar(2021),
                    vedtaksperiode = 1.januar(2021) til 31.januar(2021),
                ),
            ),
        )

        val første = eksternFagsak.lagre(fagsysteminfo, SystemKlokke).shouldNotBeNull()
        val andre = eksternFagsak.lagre(
            fagsysteminfoHendelse(
                behandlingId = "1",
                utvidPerioder = listOf(
                    FagsysteminfoHendelse.UtvidetPeriode(
                        kravgrunnlagPeriode = 1.januar(2021) til 15.januar(2021),
                        vedtaksperiode = 1.januar(2021) til 30.januar(2021),
                    ),
                ),
            ),
            SystemKlokke,
        ).shouldNotBeNull()

        andre.entry shouldNotBeSameInstanceAs første.entry
        eksternFagsak.behandlinger.nåværende().entry shouldBeSameInstanceAs andre.entry
    }

    @Test
    fun `fagsysteminfo med en ekstra utvidet periode`() {
        val eksternFagsak = eksternFagsak()
        val januar = FagsysteminfoHendelse.UtvidetPeriode(
            kravgrunnlagPeriode = 1.januar(2021) til 15.januar(2021),
            vedtaksperiode = 1.januar(2021) til 31.januar(2021),
        )
        val fagsysteminfo = fagsysteminfoHendelse(behandlingId = "1", utvidPerioder = listOf(januar))

        val første = eksternFagsak.lagre(fagsysteminfo, SystemKlokke).shouldNotBeNull()
        val andre = eksternFagsak.lagre(
            fagsysteminfoHendelse(
                behandlingId = "1",
                utvidPerioder = listOf(
                    januar,
                    FagsysteminfoHendelse.UtvidetPeriode(
                        kravgrunnlagPeriode = 1.februar(2021) til 15.februar(2021),
                        vedtaksperiode = 1.februar(2021) til 28.februar(2021),
                    ),
                ),
            ),
            SystemKlokke,
        ).shouldNotBeNull()

        andre.entry shouldNotBeSameInstanceAs første.entry
        eksternFagsak.behandlinger.nåværende().entry shouldBeSameInstanceAs andre.entry
    }

    @Test
    fun `lik fagsysteminfo med endret overstyrt ekstern fagsakId`() {
        val eksternFagsak = eksternFagsak()
        eksternFagsak.lagre(fagsysteminfoHendelse(behandlingId = "1", overstyrtEksternFagsakId = "første"), SystemKlokke)
        val andre = eksternFagsak.lagre(fagsysteminfoHendelse(behandlingId = "1", overstyrtEksternFagsakId = "andre"), SystemKlokke)

        andre shouldBe null
        eksternFagsak.eksternId() shouldBe "andre"
    }

    companion object {
        @JvmStatic
        fun endringerIRevurdering(): List<Arguments> = listOf(
            endring("behandlingId", fagsysteminfoHendelse(behandlingId = "2")),
            endring("revurderingsårsak", fagsysteminfoHendelse(behandlingId = "1", årsak = EksternFagsakRevurdering.Revurderingsårsak.KORRIGERING)),
            endring("årsakTilFeilutbetaling", fagsysteminfoHendelse(behandlingId = "1", årsakTilFeilutbetaling = "Annen årsak")),
            endring("vedtaksdato", fagsysteminfoHendelse(behandlingId = "1", vedtaksdato = LocalDate.now().plusDays(1))),
            endring("url", fagsysteminfoHendelse(behandlingId = "1", url = "http://localhost:8081")),
            endring("brukerInformertOmFeilutbetaling", fagsysteminfoHendelse(behandlingId = "1", brukerInformertOmFeilutbetaling = true)),
        )

        private fun endring(navn: String, fagsysteminfo: FagsysteminfoHendelse): Arguments = Arguments.of(Named.of(navn, fagsysteminfo))
    }
}
