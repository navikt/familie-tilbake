package no.nav.tilbakekreving.pdf.handlebars

import com.fasterxml.jackson.databind.JsonNode
import com.github.jknack.handlebars.Context
import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.JsonNodeValueResolver
import com.github.jknack.handlebars.Template
import com.github.jknack.handlebars.context.JavaBeanValueResolver
import com.github.jknack.handlebars.context.MapValueResolver
import com.github.jknack.handlebars.helper.ConditionalHelpers
import com.github.jknack.handlebars.io.ClassPathTemplateLoader
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import no.nav.tilbakekreving.pdf.handlebars.dto.Språkstøtte
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object FellesTekstformaterer {
    private val TEMPLATE_CACHE = ConcurrentHashMap<String, Template>()

    private val OM = ObjectMapperForUtvekslingAvDataMedHandlebars.INSTANCE

    fun lagBrevtekst(
        data: Språkstøtte,
        filsti: String,
    ): String {
        val template = getTemplate(data.språkkode, filsti)
        return applyTemplate(data, template)
    }

    fun lagDeltekst(
        data: Språkstøtte,
        filsti: String,
    ): String {
        val template = getTemplateFraPartial(data.språkkode, filsti)
        return applyTemplate(data, template)
    }

    private fun getTemplate(
        språkkode: Språkkode,
        filsti: String,
    ): Template {
        return TEMPLATE_CACHE.computeIfAbsent(lagSpråkstøttetFilsti(filsti, språkkode), ::opprettTemplate)
    }

    private fun getTemplateFraPartial(
        språkkode: Språkkode,
        partial: String,
    ): Template {
        return TEMPLATE_CACHE.computeIfAbsent(lagSpråkstøttetFilsti(partial, språkkode)) { filsti ->
            opprettTemplateFraPartials(lagSpråkstøttetFilsti("vedtak/vedtak_felles", språkkode), filsti)
        }
    }

    private fun opprettTemplate(språkstøttetFilsti: String): Template = opprettHandlebarsKonfigurasjon().compile(språkstøttetFilsti)

    private fun opprettTemplateFraPartials(vararg partials: String): Template {
        val partialString = partials.joinToString("") { "{{> $it}}\n" }
        return try {
            opprettHandlebarsKonfigurasjon().compileInline(partialString)
        } catch (e: IOException) {
            throw Exception("Klarte ikke å kompilere partial template $partials", e)
        }
    }

    private fun applyTemplate(
        data: Any,
        template: Template,
    ): String =
        try {
            // Går via JSON for å
            // 1. tilrettelegger for å flytte generering til PDF etc til ekstern applikasjon
            // 2. ha egen navngiving på variablene i template for enklere å lese template
            // 3. unngår at template feiler når variable endrer navn
            val jsonNode: JsonNode = OM.valueToTree(data)
            val context =
                Context
                    .newBuilder(jsonNode)
                    .resolver(JsonNodeValueResolver.INSTANCE, JavaBeanValueResolver.INSTANCE, MapValueResolver.INSTANCE)
                    .build()
            template.apply(context).trim()
        } catch (e: IOException) {
            throw IllegalStateException("Feil ved tekstgenerering.", e)
        }

    private fun opprettHandlebarsKonfigurasjon(): Handlebars {
        val loader =
            ClassPathTemplateLoader().apply {
                charset = StandardCharsets.UTF_8
                prefix = "/templates/"
                suffix = ".hbs"
            }

        return Handlebars(loader).apply {
            charset = StandardCharsets.UTF_8
            setInfiniteLoops(false)
            setPrettyPrint(true)
            registerHelpers(ConditionalHelpers::class.java)
            registerHelper("kroner", KroneFormattererMedTusenskille())
            registerHelper("dato", DatoHelper())
            registerHelper("kortdato", KortdatoHelper())
            registerHelper("måned", MånedHelper())
            registerHelper("storForbokstav", StorBokstavHelper())
            registerHelper("switch", SwitchHelper())
            registerHelper("case", CaseHelper())
            registerHelper("var", VariableHelper())
            registerHelper("lookup-map", MapLookupHelper())
        }
    }

    private fun lagSpråkstøttetFilsti(
        filsti: String,
        språkkode: Språkkode,
    ): String = String.format("%s/%s", språkkode.name.lowercase(Locale.getDefault()), filsti)
}
