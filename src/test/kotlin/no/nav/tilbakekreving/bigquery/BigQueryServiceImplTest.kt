package no.nav.tilbakekreving.bigquery

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.BigQueryError
import com.google.cloud.bigquery.BigQueryException
import com.google.cloud.bigquery.InsertAllRequest
import com.google.cloud.bigquery.InsertAllResponse
import com.google.cloud.bigquery.TableId
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.inspectors.forAll
import io.kotest.inspectors.forSingle
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.tilbake.data.Testdata
import no.nav.tilbakekreving.api.v1.dto.BigQueryVilkårsvurderingDataDto
import no.nav.tilbakekreving.api.v1.dto.BigQueryVilkårsvurderingsperiodeDto
import no.nav.tilbakekreving.api.v1.dto.RettsligGrunnlag
import no.nav.tilbakekreving.applicationProps
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.test.februar
import no.nav.tilbakekreving.test.januar
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.time.Instant

class BigQueryServiceImplTest {
    @Test
    fun `flere vurderte perioder i samme sending`() {
        val forespørsler = mutableListOf<InsertAllRequest>()
        val response = mockk<InsertAllResponse>()
        every { response.hasErrors() } returns false
        val klient = grensesnittStub<BigQuery> { metode, argumenter ->
            check(metode == "insertAll") { "Uventet BigQuery-kall: $metode" }
            forespørsler.add(argumenter.single() as InsertAllRequest)
            response
        }
        val service = BigQueryServiceImpl(applicationProps(), klient)
        val data = data()
        val før = Instant.now()
        service.lagreVilkårsvurdering(data)
        val etter = Instant.now()

        forespørsler.forSingle { request ->
            request.table shouldBe TableId.of("test-project", "test-dataset", "bq_vilkarsvurderingsperiode")
            request.rows.shouldHaveSize(data.perioder.size)
            val tidspunkt = request.rows.map { it.content.getValue("tid") }.toSet().single() as String
            val instant = Instant.parse(tidspunkt)
            instant.isBefore(før) shouldBe false
            instant.isAfter(etter) shouldBe false
            request.rows.forAll { rad ->
                data.perioder.filter { "${data.behandlingId}:${it.periodeId}" == rad.id }.forSingle { periode ->
                    rad.content shouldBe mapOf(
                        "tid" to tidspunkt,
                        "behandling_id" to data.behandlingId,
                        "periode_id" to periode.periodeId.toString(),
                        "periode_fom" to periode.periode.fom.toString(),
                        "periode_tom" to periode.periode.tom.toString(),
                        "ytelses_type" to data.ytelse,
                        "rettslig_grunnlag" to periode.rettsligGrunnlag.name,
                    )
                }
            }
        }

        service.lagreVilkårsvurdering(data)
        forespørsler.shouldHaveSize(2)
        forespørsler.map { it.rows.map { rad -> rad.id } }.distinct().shouldHaveSize(1)
    }

    @Test
    fun `sending uten vurderte perioder`() {
        val forespørsler = mutableListOf<InsertAllRequest>()
        val response = mockk<InsertAllResponse>()
        every { response.hasErrors() } returns false
        val klient = grensesnittStub<BigQuery> { metode, argumenter ->
            check(metode == "insertAll")
            forespørsler.add(argumenter.single() as InsertAllRequest)
            response
        }
        BigQueryServiceImpl(applicationProps(), klient).lagreVilkårsvurdering(data().copy(perioder = emptyList()))
        forespørsler.shouldBeEmpty()
    }

    @Test
    fun `feil på en rad i batch`() {
        val data = data()
        val response = mockk<InsertAllResponse>()
        every { response.hasErrors() } returns true
        every { response.insertErrors } returns mapOf(1L to listOf(BigQueryError("invalid", "rettslig_grunnlag", "Ugyldig verdi")))
        val klient = grensesnittStub<BigQuery> { metode, _ ->
            check(metode == "insertAll")
            response
        }
        medLoggfanging { logger ->
            shouldNotThrowAny {
                BigQueryServiceImpl(applicationProps(), klient).lagreVilkårsvurdering(data)
            }
            logger.forSingle {
                it.level shouldBe Level.ERROR
                it.formattedMessage shouldContain "Insert av vilkårsvurderingsperioder til BigQuery feilet for behandling ${data.behandlingId}"
                it.formattedMessage shouldContain "Ugyldig verdi"
            }
        }
    }

    @Test
    fun `klienten kaster feil under sending`() {
        val data = data()
        val forventet = BigQueryException(503, "BigQuery er utilgjengelig")
        val klient = grensesnittStub<BigQuery> { metode, _ ->
            check(metode == "insertAll")
            throw forventet
        }
        medLoggfanging { logger ->
            shouldNotThrowAny {
                BigQueryServiceImpl(applicationProps(), klient).lagreVilkårsvurdering(data)
            }
            logger.forSingle {
                it.level shouldBe Level.ERROR
                it.formattedMessage shouldBe "Kunne ikke sende vilkårsvurderingsperioder til BigQuery for behandling ${data.behandlingId}"
                it.throwableProxy.className shouldBe BigQueryException::class.java.name
                it.throwableProxy.message shouldBe forventet.message
            }
        }
    }

    @Test
    fun `uventet programmeringsfeil under sending`() {
        val forventet = IllegalStateException("Uventet programmeringsfeil")
        val klient = grensesnittStub<BigQuery> { metode, _ ->
            check(metode == "insertAll")
            throw forventet
        }
        shouldThrow<IllegalStateException> {
            BigQueryServiceImpl(applicationProps(), klient).lagreVilkårsvurdering(data())
        } shouldBe forventet
    }

    private fun medLoggfanging(test: (List<ILoggingEvent>) -> Unit) {
        val logger = LoggerFactory.getLogger(BigQueryServiceImpl::class.java) as Logger
        val appender = ListAppender<ILoggingEvent>()
        appender.start()
        logger.addAppender(appender)
        try {
            test(appender.list)
        } finally {
            logger.detachAppender(appender)
            appender.stop()
        }
    }

    private fun data(): BigQueryVilkårsvurderingDataDto {
        val fagsak = Testdata.fagsak()
        val behandling = Testdata.lagBehandling(fagsak.id)
        return BigQueryVilkårsvurderingDataDto(
            behandlingId = behandling.id.toString(),
            ytelse = fagsak.fagsystem.navn,
            perioder = listOf(
                BigQueryVilkårsvurderingsperiodeDto(Testdata.vilkårsperiode().id, 1.januar(2021) til 31.januar(2021), RettsligGrunnlag.GOD_TRO),
                BigQueryVilkårsvurderingsperiodeDto(Testdata.vilkårsperiode().id, 1.februar(2021) til 28.februar(2021), RettsligGrunnlag.GROVT_UAKTSOM),
            ),
        )
    }
}
