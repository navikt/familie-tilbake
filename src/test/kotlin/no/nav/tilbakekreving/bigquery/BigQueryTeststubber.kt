package no.nav.tilbakekreving.bigquery

import no.nav.tilbakekreving.api.v1.dto.BigQueryBehandlingDataDto
import no.nav.tilbakekreving.api.v1.dto.BigQueryVilkårsvurderingDataDto
import java.lang.reflect.Proxy

/**
 * Smale stubber for store tredjeparts- og Spring Data-grensesnitt.
 * Alle andre kall enn de testen eksplisitt håndterer, skal feile.
 */
internal inline fun <reified T> grensesnittStub(crossinline svar: (String, List<Any?>) -> Any?): T =
    Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, metode, argumenter ->
        svar(metode.name, argumenter?.toList().orEmpty())
    } as T

internal class BigQueryOppsamler : BigQueryService {
    val vurderinger = mutableListOf<BigQueryVilkårsvurderingDataDto>()
    val behandlinger = mutableListOf<BigQueryBehandlingDataDto>()

    override fun lagreVilkårsvurdering(bigqueryData: BigQueryVilkårsvurderingDataDto) {
        vurderinger.add(bigqueryData)
    }

    override fun oppdaterBehandling(bigqueryData: BigQueryBehandlingDataDto) {
        behandlinger.add(bigqueryData)
    }
}
