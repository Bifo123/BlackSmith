package com.forjasuprema.client;

import com.forjasuprema.minigame.MinigameKind;
import com.forjasuprema.minigame.PatternGenerator;
import com.forjasuprema.minigame.SmithHammerItem;
import com.forjasuprema.minigame.StrikeDirection;
import com.forjasuprema.minigame.StrikeMath;
import com.forjasuprema.minigame.StrikeSession;
import com.forjasuprema.minigame.StrikeVerdict;
import com.forjasuprema.network.CancelMinigamePayload;
import com.forjasuprema.network.EndMinigamePayload;
import com.forjasuprema.network.OpenMinigamePayload;
import com.forjasuprema.network.StrikePayload;
import com.forjasuprema.network.StrikeResultPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Tela do minigame (martelamento e foles). Apenas desenha o marcador e envia "bati" ao servidor; o
 * padrao vem da semente e os resultados vem do servidor. Nao pausa o jogo.
 */
public class MinigameScreen extends Screen {
    private static final int BAR_WIDTH = 220;
    private static final int BAR_HEIGHT = 16;
    private static final int WAIT_LIMIT_TICKS = 20;
    private static final int CLOSE_DELAY_TICKS = 30;

    private final MinigameKind kind;
    private final int strikes;
    private final int period;
    private final double halfWidth;
    private final PatternGenerator.Pattern pattern;
    private final String shape;
    private final List<StrikeVerdict> verdicts = new ArrayList<>();

    private int ticks;
    private float lastPartial;
    private int nextIndex;
    private boolean awaiting;
    private int sentTick;
    private boolean ended;
    private int endTick;
    private float score;
    private int endReason;

    public MinigameScreen(OpenMinigamePayload payload) {
        super(Component.translatable("screen.forjasuprema.minigame." + MinigameKind.fromOrdinal(payload.kind()).name().toLowerCase(java.util.Locale.ROOT)));
        this.kind = MinigameKind.fromOrdinal(payload.kind());
        this.strikes = payload.strikes();
        this.period = payload.periodTicks();
        this.halfWidth = payload.halfWidth();
        this.shape = payload.shape();
        this.pattern = PatternGenerator.generate(payload.seed(), strikes, kind.directional());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        ticks++;
        if (awaiting && ticks - sentTick > WAIT_LIMIT_TICKS) {
            awaiting = false;
        }
        if (ended && ticks - endTick > CLOSE_DELAY_TICKS) {
            onClose();
        }
    }

    public void onResult(StrikeResultPayload payload) {
        if (payload.index() == verdicts.size()) {
            verdicts.add(StrikeVerdict.fromOrdinal(payload.verdict()));
        }
        nextIndex = payload.index() + 1;
        awaiting = false;
    }

    public void onEnd(EndMinigamePayload payload) {
        ended = true;
        endTick = ticks;
        score = payload.score();
        endReason = payload.reason();
    }

    @Override
    public void onClose() {
        if (!ended) {
            PacketDistributor.sendToServer(new CancelMinigamePayload());
            ended = true;
        }
        super.onClose();
    }

    private double elapsed() {
        return ticks + lastPartial;
    }

    private void strike(StrikeDirection direction) {
        if (ended || awaiting || nextIndex >= strikes) {
            return;
        }
        awaiting = true;
        sentTick = ticks;
        long scaled = Math.round(elapsed() * StrikeSession.TIME_SCALE);
        PacketDistributor.sendToServer(new StrikePayload(nextIndex, scaled, direction.ordinal()));
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (kind.directional()) {
            StrikeDirection direction = switch (key) {
                case GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_W -> StrikeDirection.UP;
                case GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_S -> StrikeDirection.DOWN;
                case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_A -> StrikeDirection.LEFT;
                case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_D -> StrikeDirection.RIGHT;
                default -> StrikeDirection.NONE;
            };
            if (direction != StrikeDirection.NONE) {
                strike(direction);
                return true;
            }
        } else if (key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_ENTER) {
            strike(StrikeDirection.NONE);
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!kind.directional() && button == 0) {
            strike(StrikeDirection.NONE);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static int verdictColor(StrikeVerdict verdict) {
        return switch (verdict) {
            case PERFECT -> 0xFF55FF55;
            case GOOD -> 0xFFFFFF55;
            case POOR -> 0xFFFFAA00;
            case MISS, WRONG_DIRECTION -> 0xFFFF5555;
        };
    }

    private static String arrow(StrikeDirection direction) {
        return switch (direction) {
            case UP -> "↑";
            case DOWN -> "↓";
            case LEFT -> "←";
            case RIGHT -> "→";
            case NONE -> "";
        };
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        lastPartial = partialTick;
        int cx = width / 2;
        int barX = cx - BAR_WIDTH / 2;
        int barY = height / 2 + 20;

        graphics.drawCenteredString(font, title, cx, barY - 80, 0xFFFFFFFF);
        if (!shape.isEmpty()) {
            graphics.drawCenteredString(font, SmithHammerItem.shapeName(shape), cx, barY - 68, 0xFFAAAAAA);
        }
        graphics.drawCenteredString(font, Component.translatable("minigame.forjasuprema.progress",
                Math.min(nextIndex + 1, strikes), strikes), cx, barY - 54, 0xFFDDDDDD);

        if (!ended && nextIndex < strikes && kind.directional()) {
            graphics.pose().pushPose();
            graphics.pose().scale(3.0F, 3.0F, 1.0F);
            graphics.drawCenteredString(font, arrow(pattern.directions().get(nextIndex)), cx / 3, (barY - 40) / 3, 0xFFFFD060);
            graphics.pose().popPose();
        }

        // Barra e zonas de acerto.
        graphics.fill(barX - 2, barY - 2, barX + BAR_WIDTH + 2, barY + BAR_HEIGHT + 2, 0xFF000000);
        graphics.fill(barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, 0xFF303030);
        for (int i = 0; i < strikes; i++) {
            boolean done = i < verdicts.size();
            boolean current = i == nextIndex && !ended;
            if (!done && !current) {
                continue;
            }
            int zoneColor = done ? verdictColor(verdicts.get(i)) & 0x80FFFFFF | 0x80000000 : 0xFF8A6A2A;
            double target = pattern.targets().get(i);
            int left = barX + (int) Math.round((target - halfWidth) * BAR_WIDTH);
            int right = barX + (int) Math.round((target + halfWidth) * BAR_WIDTH);
            graphics.fill(Math.max(barX, left), barY, Math.min(barX + BAR_WIDTH, right), barY + BAR_HEIGHT, zoneColor);
            if (current) {
                int coreHalf = (int) Math.round(halfWidth * StrikeMath.PERFECT_FRACTION * BAR_WIDTH);
                int mid = barX + (int) Math.round(target * BAR_WIDTH);
                graphics.fill(mid - coreHalf, barY, mid + coreHalf, barY + BAR_HEIGHT, 0xFFE0B040);
            }
        }
        if (!ended) {
            int cursorX = barX + (int) Math.round(StrikeMath.cursor(elapsed(), period) * BAR_WIDTH);
            graphics.fill(cursorX - 1, barY - 4, cursorX + 1, barY + BAR_HEIGHT + 4, 0xFFFFFFFF);
        }

        if (!verdicts.isEmpty()) {
            StrikeVerdict last = verdicts.get(verdicts.size() - 1);
            graphics.drawCenteredString(font, Component.translatable("minigame.forjasuprema.verdict."
                            + last.name().toLowerCase(java.util.Locale.ROOT)),
                    cx, barY + BAR_HEIGHT + 10, verdictColor(last));
        }
        Component hint = ended
                ? Component.translatable("minigame.forjasuprema.score", Math.round(score * 100))
                : Component.translatable(kind.directional() ? "minigame.forjasuprema.hint.directional"
                        : "minigame.forjasuprema.hint.rhythm");
        graphics.drawCenteredString(font, hint, cx, barY + BAR_HEIGHT + 26, 0xFFAAAAAA);
    }
}
