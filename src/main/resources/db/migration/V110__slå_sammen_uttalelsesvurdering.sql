UPDATE tilbakekreving_brukeruttalelse SET uttalelse_vurdering='JA' WHERE uttalelse_vurdering IN ('JA_ETTER_FORHÅNDSVARSEL', 'UNNTAK_ALLEREDE_UTTALT_SEG');
UPDATE tilbakekreving_brukeruttalelse SET uttalelse_vurdering='NEI' WHERE uttalelse_vurdering IN ('NEI_ETTER_FORHÅNDSVARSEL', 'UNNTAK_INGEN_UTTALELSE');
