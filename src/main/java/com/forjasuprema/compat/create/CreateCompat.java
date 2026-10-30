package com.forjasuprema.compat.create;

import com.forjasuprema.ForjaSuprema;

/**
 * Integracao com Create (Fase 10). Este e o unico pacote autorizado a importar classes do Create.
 *
 * <p>Na Fase 1 ela apenas confirma que o ponto de entrada isolado funciona; as maquinas e o
 * avaliador automatizado serao implementados na Fase 10.
 */
public final class CreateCompat {
    private CreateCompat() {
    }

    public static void init() {
        ForjaSuprema.LOGGER.info("Create detectado: integracao pendente (Fase 10).");
    }
}
