package no.nav.familie.tilbake

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.github.tomakehurst.wiremock.WireMockServer
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingsårsak
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandling.domain.Fagsystemsbehandling
import no.nav.familie.tilbake.behandling.domain.Fagsystemskonsekvens
import no.nav.familie.tilbake.behandling.domain.Varsel
import no.nav.familie.tilbake.behandling.domain.Verge
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.database.DbContainerInitializer
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevsporing
import no.nav.familie.tilbake.dokumentbestilling.vedtak.domain.Vedtaksbrevsoppsummering
import no.nav.familie.tilbake.dokumentbestilling.vedtak.domain.Vedtaksbrevsperiode
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.FaktaFeilutbetaling
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.FaktaFeilutbetalingsperiode
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesperiode
import no.nav.familie.tilbake.foreldelse.domain.VurdertForeldelse
import no.nav.familie.tilbake.iverksettvedtak.domain.ØkonomiXmlSendt
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsbeløp433
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsperiode432
import no.nav.familie.tilbake.kravgrunnlag.domain.ØkonomiXmlMottatt
import no.nav.familie.tilbake.kravgrunnlag.domain.ØkonomiXmlMottattArkiv
import no.nav.familie.tilbake.totrinn.domain.Totrinnsvurdering
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurdering
import no.nav.familie.tilbake.vilkårsvurdering.domain.VilkårsvurderingAktsomhet
import no.nav.familie.tilbake.vilkårsvurdering.domain.VilkårsvurderingGodTro
import no.nav.familie.tilbake.vilkårsvurdering.domain.VilkårsvurderingSærligGrunn
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsperiode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.cache.CacheManager
import org.springframework.context.ApplicationContext
import org.springframework.data.jdbc.core.JdbcAggregateOperations
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension


@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@SpringBootTest(classes = [LauncherLocal::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integrasjonstest", "mock-oauth", "mock-pdl", "mock-integrasjoner", "mock-oppgave", "mock-økonomi")
abstract class OppslagSpringRunnerTest {

    private val listAppender = initLoggingEventListAppender()
    protected var loggingEvents: MutableList<ILoggingEvent> = listAppender.list
    protected val restTemplate = TestRestTemplate()

    @Autowired private lateinit var jdbcAggregateOperations: JdbcAggregateOperations
    @Autowired private lateinit var applicationContext: ApplicationContext
    @Autowired private lateinit var cacheManager: CacheManager

    @LocalServerPort
    private var port: Int? = 0

    @AfterEach
    fun reset() {
        loggingEvents.clear()
        resetDatabase()
        clearCaches()
        resetWiremockServers()
    }

    private fun resetWiremockServers() {
        applicationContext.getBeansOfType(WireMockServer::class.java).values.forEach(WireMockServer::resetRequests)
    }

    private fun clearCaches() {
        cacheManager.cacheNames.mapNotNull { cacheManager.getCache(it) }
                .forEach { it.clear() }
    }

    private fun resetDatabase() {
        listOf(Fagsak::class,
               Behandling::class,
               Behandlingsårsak::class,
               Fagsystemsbehandling::class,
               Fagsystemskonsekvens::class,
               Behandlingsstegstilstand::class,
               Totrinnsvurdering::class,
               VurdertForeldelse::class,
               Foreldelsesperiode::class,
               Kravgrunnlag431::class,
               Kravgrunnlagsperiode432::class,
               Kravgrunnlagsbeløp433::class,
               Vilkårsvurdering::class,
               Vilkårsvurderingsperiode::class,
               VilkårsvurderingAktsomhet::class,
               VilkårsvurderingSærligGrunn::class,
               VilkårsvurderingGodTro::class,
               FaktaFeilutbetaling::class,
               FaktaFeilutbetalingsperiode::class,
               ØkonomiXmlMottatt::class,
               Vedtaksbrevsoppsummering::class,
               Vedtaksbrevsperiode::class,
               ØkonomiXmlSendt::class,
               Varsel::class,
               Brevsporing::class,
               ØkonomiXmlMottattArkiv::class,
               Verge::class,
               Task::class)
                .reversed()
                .forEach { jdbcAggregateOperations.deleteAll(it.java) }
    }

    protected fun getPort(): String {
        return port.toString()
    }

    companion object {

        private const val LOCALHOST = "http://localhost:"
        protected fun initLoggingEventListAppender(): ListAppender<ILoggingEvent> {
            val listAppender = ListAppender<ILoggingEvent>()
            listAppender.start()
            return listAppender
        }
    }
}
