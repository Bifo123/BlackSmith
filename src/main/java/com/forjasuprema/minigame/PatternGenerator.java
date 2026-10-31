package com.forjasuprema.minigame;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Gera o padrao de uma sessao: posicao-alvo e direcao de cada golpe. Deterministico para uma mesma
 * semente (o servidor sorteia a semente e envia o padrao pronto ao cliente). Classe pura.
 */
public final class PatternGenerator {
    public static final float MIN_TARGET = 0.15F;
    public static final float MAX_TARGET = 0.85F;
    /** Distancia minima entre os alvos de dois golpes seguidos. */
    public static final float MIN_SPACING = 0.20F;

    private PatternGenerator() {
    }

    public record Pattern(List<Float> targets, List<StrikeDirection> directions) {
        public int size() {
            return targets.size();
        }
    }

    public static Pattern generate(long seed, int strikes, boolean directional) {
        Random random = new Random(seed);
        List<Float> targets = new ArrayList<>(strikes);
        List<StrikeDirection> directions = new ArrayList<>(strikes);

        float previous = -1.0F;
        for (int i = 0; i < strikes; i++) {
            float target;
            if (directional) {
                target = MIN_TARGET + random.nextFloat() * (MAX_TARGET - MIN_TARGET);
                if (previous >= 0 && Math.abs(target - previous) < MIN_SPACING) {
                    // Garante o espacamento sem sortear de novo (resultado sempre determinístico e dentro da faixa).
                    target = previous < 0.5F ? previous + 0.25F : previous - 0.25F;
                }
                directions.add(StrikeDirection.values()[1 + random.nextInt(4)]);
            } else {
                target = 0.5F;
                directions.add(StrikeDirection.NONE);
            }
            targets.add(target);
            previous = target;
        }
        return new Pattern(List.copyOf(targets), List.copyOf(directions));
    }
}
