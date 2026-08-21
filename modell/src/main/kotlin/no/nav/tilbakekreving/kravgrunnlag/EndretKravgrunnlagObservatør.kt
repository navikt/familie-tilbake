package no.nav.tilbakekreving.kravgrunnlag

interface EndretKravgrunnlagObservatør {
    fun perideEndretBeløp(forskjell: KravgrunnlagSammenligning.Forskjell.JustertBeløp)

    fun nyPeriode(periode: KravgrunnlagSammenligning.Forskjell.NyPeriode) {}
}
