package no.nav.familie.tilbake.brev.dokumentbestilling.felles.pdf

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

internal class DokprodTilHtmlTest {

    @Test
    fun `dokprodInnholdTilHtml skal Konvertere Overskrift Og Avsnitt`() {
        val resultat =
                DokprodTilHtml.dokprodInnholdTilHtml("_Overskrift\nFørste avsnitt\n\nAndre avsnitt\n\nTredje avsnitt")
        Assertions.assertThat(resultat).isEqualTo("<div class=\"samepage\"><h2>Overskrift</h2><p>Første avsnitt</p>" +
                                                  "</div><p>Andre avsnitt</p><p>Tredje avsnitt</p>")
    }

    @Test
    fun `dokprodInnholdTilHtml skal Konvertere Non Break Space`() {
        // utf8nonBreakingSpace = "\u00A0";
        val resultat = DokprodTilHtml.dokprodInnholdTilHtml("10\u00A0000\u00A0kroner")

        Assertions.assertThat(resultat).isEqualTo("<p>10&nbsp;000&nbsp;kroner</p>")
    }

    @Test
    fun `dokprodInnholdTilHtml skal Konvertere Bullet Points`() {
        val resultat = DokprodTilHtml.dokprodInnholdTilHtml("*-bulletpoint 1\nbulletpoint 2\nsiste bulletpoint-*")

        Assertions.assertThat(resultat)
                .isEqualTo("<ul><li>bulletpoint 1</li><li>bulletpoint 2</li><li>siste bulletpoint</li></ul>")
    }

    @Test
    fun `dokprodInnholdTilHtml skal Konvertere Bullet Points Når Første Linje Er Tom`() {
        val resultat = DokprodTilHtml.dokprodInnholdTilHtml("*-\nbulletpoint 1\nbulletpoint 2\nsiste bulletpoint-*")

        Assertions.assertThat(resultat)
                .isEqualTo("<ul><li>bulletpoint 1</li><li>bulletpoint 2</li><li>siste bulletpoint</li></ul>")
    }

    @Test
    fun `dokprodInnholdTilHtml skal Konvertere Halvhjertede Avsnitt`() {
        //halvhjertet avsnitt er hvor det er tatt kun ett linjeskift.
        val resultat = DokprodTilHtml.dokprodInnholdTilHtml("Foo\nBar")

        Assertions.assertThat(resultat).isEqualTo("<p>Foo<br/>Bar</p>")
    }

    @Test
    fun `dokprodInnholdTilHtml skal Spesialbehandle Hilsen`() {
        //halvhjertet avsnitt er hvor det er tatt kun ett linjeskift.
        val resultat = DokprodTilHtml.dokprodInnholdTilHtml("Med vennlig hilsen\nNAV Familie- og pensjonsytelser")

        Assertions.assertThat(resultat).isEqualTo(
                "<p class=\"hilsen\">Med vennlig hilsen<br/>NAV Familie- og pensjonsytelser</p>"
        )
    }
}
