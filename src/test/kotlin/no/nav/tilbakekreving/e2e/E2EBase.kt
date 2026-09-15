package no.nav.tilbakekreving.e2e

import no.nav.familie.tilbake.LauncherLocal
import no.nav.familie.tilbake.database.DbContainerInitializer
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@SpringBootTest(classes = [LauncherLocal::class], webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("integrasjonstest", "mock-oauth", "mock-pdl", "mock-integrasjoner", "mock-oppgave", "mock-økonomi")
@EnableMockOAuth2Server
abstract class E2EBase
