package no.nav.familie.tilbake.service.dokumentbestilling.handlebars

import com.fasterxml.jackson.databind.JsonNode
import com.github.jknack.handlebars.Context
import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.JsonNodeValueResolver
import com.github.jknack.handlebars.Template
import com.github.jknack.handlebars.context.JavaBeanValueResolver
import com.github.jknack.handlebars.context.MapValueResolver
import com.github.jknack.handlebars.helper.ConditionalHelpers
import com.github.jknack.handlebars.io.ClassPathTemplateLoader
import no.nav.familie.kontrakter.felles.tilbakekreving.Språkkode
import java.io.IOException
import java.nio.charset.StandardCharsets

object FellesTekstformaterer {

    private val OM = ObjectMapperForUtvekslingAvDataMedHandlebars.INSTANCE
    private fun opprettHandlebarsKonfigurasjon(): Handlebars {
        val loader = ClassPathTemplateLoader().apply {
            charset = StandardCharsets.UTF_8
            prefix = "/templates/"
            suffix = ".hbs"
        }
        return Handlebars(loader).apply {
            charset = StandardCharsets.UTF_8
            setInfiniteLoops(false)
            setPrettyPrint(true)
            registerHelpers(ConditionalHelpers::class.java)
        }
    }

    fun applyTemplate(template: Template, data: Any): String {
        return try {
            //Går via JSON for å
            //1. tilrettelegger for å flytte generering til PDF etc til ekstern applikasjon
            //2. ha egen navngiving på variablene i template for enklere å lese template
            //3. unngår at template feiler når variable endrer navn
            val jsonNode: JsonNode = OM.valueToTree(data)
            val context: Context = Context.newBuilder(jsonNode)
                    .resolver(JsonNodeValueResolver.INSTANCE, JavaBeanValueResolver.INSTANCE, MapValueResolver.INSTANCE)
                    .build()
            template.apply(context).trim()
        } catch (e: IOException) {
            throw IllegalStateException("Feil ved tekstgenerering.")
        }
    }

    fun opprettHandlebarsTemplate(filsti: String, språkkode: Språkkode): Template {
        val handlebars: Handlebars = opprettHandlebarsKonfigurasjon()
        handlebars.registerHelper("lookup-map", MapLookupHelper())
        handlebars.registerHelper("kroner", KroneFormattererMedTusenskille())
        handlebars.registerHelper("dato", DatoHelper())
        handlebars.registerHelper("kortdato", KortdatoHelper())
        return handlebars.compile(lagSpråkstøttetFilsti(filsti, språkkode))
    }

    private fun lagSpråkstøttetFilsti(filsti: String, språkkode: Språkkode): String {
        return String.format("%s/%s", språkkode.name.toLowerCase(), filsti)
    }
}
