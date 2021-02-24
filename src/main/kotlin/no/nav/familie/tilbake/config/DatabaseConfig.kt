package no.nav.familie.tilbake.config

import no.nav.familie.tilbake.common.repository.Endret
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravstatuskode
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.PlatformTransactionManager
import java.util.Optional
import javax.sql.DataSource


@Configuration
@EnableJdbcAuditing
@EnableJdbcRepositories("no.nav.familie.tilbake", "no.nav.familie.prosessering")
class DatabaseConfig : AbstractJdbcConfiguration() {

    @Bean
    fun operations(dataSource: DataSource): NamedParameterJdbcOperations {
        return NamedParameterJdbcTemplate(dataSource)
    }

    @Bean
    fun transactionManager(dataSource: DataSource): PlatformTransactionManager {
        return DataSourceTransactionManager(dataSource)
    }

    @Bean
    fun auditSporbarEndret(): AuditorAware<Endret> {
        return AuditorAware {
            Optional.of(Endret())
        }
    }

    @Bean
    override fun jdbcCustomConversions(): JdbcCustomConversions {
        return JdbcCustomConversions(listOf(KravstatuskodeLesConverter(), KravstatuskodeSkrivConverter()))
    }

    @ReadingConverter
    class KravstatuskodeLesConverter : Converter<String, Kravstatuskode> {

        override fun convert(kode: String): Kravstatuskode {
            return Kravstatuskode.fraKode(kode)
        }

    }

    @WritingConverter
    class KravstatuskodeSkrivConverter : Converter<Kravstatuskode, String> {

        override fun convert(kravstatuskode: Kravstatuskode): String {
            return kravstatuskode.kode
        }

    }
}
