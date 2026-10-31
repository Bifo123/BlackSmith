package com.forjasuprema.minigame;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Estado e regras de uma sessao de minigame, do ponto de vista do SERVIDOR. Classe pura (sem Minecraft),
 * por isso testavel. O cliente apenas desenha o marcador e informa "bati no instante X"; quem decide
 * o resultado e esta classe.
 *
 * <p>Defesas contra cliente adulterado:
 * <ul>
 *   <li>o indice do golpe precisa ser exatamente o proximo esperado;</li>
 *   <li>intervalo minimo entre golpes, medido no relogio do servidor;</li>
 *   <li>o instante informado so e aceito se estiver a no maximo {@link #MAX_SKEW} do relogio do
 *       servidor (senao vale o do servidor) e precisa avancar entre golpes;</li>
 *   <li>a sessao expira sozinha.</li>
 * </ul>
 * Limite conhecido: um bot que le o padrao e calcula o instante certo nao pode ser distinguido de um
 * jogador habil por regras de servidor; isso e documentado, nao escondido.
 */
public final class StrikeSession {
    /** Instantes sao trocados em centesimos de tick, para nao perder precisao com zonas estreitas. */
    public static final int TIME_SCALE = 100;
    /** Diferenca maxima aceita entre o instante informado e o do servidor (8 ticks, cobre ~400 ms de ida e volta). */
    public static final long MAX_SKEW = 8L * TIME_SCALE;
    /** Intervalo minimo entre dois golpes, no tempo informado (3 ticks). */
    public static final long MIN_GAP = 3L * TIME_SCALE;
    /** Intervalo minimo entre dois golpes no relogio do servidor. */
    public static final long MIN_SERVER_GAP_TICKS = 2L;

    /** Resultado de um golpe aceito. */
    public record Result(int index, StrikeVerdict verdict, double accuracy, boolean finished) {
    }

    private final MinigameKind kind;
    private final long seed;
    private final int strikes;
    private final MinigameDifficulty.Params params;
    private final PatternGenerator.Pattern pattern;
    private final long startTick;
    private final List<Double> scores = new ArrayList<>();

    private int next;
    private long lastStrikeTick = Long.MIN_VALUE / 2;
    private long lastTrusted = Long.MIN_VALUE / 2;

    public StrikeSession(MinigameKind kind, long seed, int strikes, MinigameDifficulty.Params params, long startTick) {
        this.kind = kind;
        this.seed = seed;
        this.strikes = strikes;
        this.params = params;
        this.startTick = startTick;
        this.pattern = PatternGenerator.generate(seed, strikes, kind.directional());
    }

    public MinigameKind kind() {
        return kind;
    }

    public long seed() {
        return seed;
    }

    public int strikes() {
        return strikes;
    }

    public MinigameDifficulty.Params params() {
        return params;
    }

    public PatternGenerator.Pattern pattern() {
        return pattern;
    }

    public long startTick() {
        return startTick;
    }

    public int nextIndex() {
        return next;
    }

    public boolean isFinished() {
        return next >= strikes;
    }

    /** Notas por golpe ja registradas (0 a 1). */
    public List<Double> scores() {
        return List.copyOf(scores);
    }

    /** Nota final da sessao, de 0 a 1. */
    public double finalScore() {
        return StrikeMath.sessionScore(scores);
    }

    /** Tempo maximo de uma sessao: ate 4 voltas do marcador por golpe, mais folga. */
    public boolean isExpired(long nowTick) {
        return nowTick - startTick > (long) strikes * params.periodTicks() * 4L + 200L;
    }

    /**
     * Registra um golpe.
     *
     * @param index             indice do golpe informado pelo cliente
     * @param claimedElapsed    instante do golpe segundo o cliente, em centesimos de tick desde o inicio
     * @param direction         direcao informada (ignorada se o minigame nao for direcional)
     * @param nowTick           tempo de jogo atual do servidor
     * @param temperatureFactor eficiencia pela temperatura da peca (1.0 para foles)
     * @return o resultado, ou vazio se o golpe foi recusado (fora de ordem, rapido demais, sessao encerrada)
     */
    public Optional<Result> submit(int index, long claimedElapsed, StrikeDirection direction,
                                   long nowTick, double temperatureFactor) {
        if (isFinished() || index != next) {
            return Optional.empty();
        }
        if (nowTick - lastStrikeTick < MIN_SERVER_GAP_TICKS) {
            return Optional.empty();
        }
        long serverElapsed = (nowTick - startTick) * TIME_SCALE;
        long trusted = StrikeMath.trustedElapsed(claimedElapsed, serverElapsed, MAX_SKEW);
        if (next > 0 && trusted - lastTrusted < MIN_GAP) {
            return Optional.empty();
        }

        double cursor = StrikeMath.cursor(trusted / (double) TIME_SCALE, params.periodTicks());
        double error = cursor - pattern.targets().get(index);
        boolean directionOk = !kind.directional() || direction == pattern.directions().get(index);
        double accuracy = StrikeMath.accuracy(error, params.zoneHalfWidth())
                * Math.max(0.0, Math.min(1.0, temperatureFactor));
        StrikeVerdict verdict = StrikeVerdict.of(accuracy, directionOk);

        scores.add(directionOk ? accuracy : 0.0);
        lastStrikeTick = nowTick;
        lastTrusted = trusted;
        next++;
        return Optional.of(new Result(index, verdict, directionOk ? accuracy : 0.0, isFinished()));
    }
}
