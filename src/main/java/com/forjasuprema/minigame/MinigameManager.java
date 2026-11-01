package com.forjasuprema.minigame;

import com.forjasuprema.finish.FinishRules;
import com.forjasuprema.finish.FinishState;
import com.forjasuprema.finish.FinishToolItem;
import com.forjasuprema.metal.Metal;
import com.forjasuprema.metal.MetalLookup;
import com.forjasuprema.network.CancelMinigamePayload;
import com.forjasuprema.network.EndMinigamePayload;
import com.forjasuprema.network.OpenMinigamePayload;
import com.forjasuprema.network.StrikePayload;
import com.forjasuprema.network.StrikeResultPayload;
import com.forjasuprema.quality.StageScores;
import com.forjasuprema.registry.ModDataComponents;
import com.forjasuprema.registry.ModRecipes;
import com.forjasuprema.thermal.Boostable;
import com.forjasuprema.thermal.HeatData;
import com.forjasuprema.thermal.ThermalMath;
import com.forjasuprema.treatment.TreatmentState;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Gerencia as sessoes de minigame no SERVIDOR. Uma sessao por jogador. Toda validacao e todo resultado
 * (nota, qualidade parcial, peca produzida) nascem aqui; o cliente so desenha e informa intencoes.
 *
 * <p>Nivel do jogador: a progressao entra na Fase 9; ate la o nivel usado e 0 (sem bonus de habilidade).
 */
public final class MinigameManager {
    private static final double MAX_DISTANCE_SQR = 6.0 * 6.0;
    private static final int BELLOWS_STRIKES = 6;
    private static final int PLAYER_LEVEL = 0;

    private static final Map<UUID, Active> SESSIONS = new HashMap<>();

    private MinigameManager() {
    }

    /** Contexto de uma sessao em andamento. */
    private static final class Active {
        final StrikeSession session;
        final ResourceKey<Level> dimension;
        final BlockPos pos;
        final InteractionHand hand;
        final Item tool;
        final ItemStack pieceRef;
        final ForgingRecipe recipe;
        final ResourceLocation metalId;
        final Metal metal;

        Active(StrikeSession session, ResourceKey<Level> dimension, BlockPos pos, InteractionHand hand, Item tool,
               ItemStack pieceRef, ForgingRecipe recipe, ResourceLocation metalId, Metal metal) {
            this.session = session;
            this.dimension = dimension;
            this.pos = pos;
            this.hand = hand;
            this.tool = tool;
            this.pieceRef = pieceRef;
            this.recipe = recipe;
            this.metalId = metalId;
            this.metal = metal;
        }
    }

    public static boolean hasSession(ServerPlayer player) {
        return SESSIONS.containsKey(player.getUUID());
    }

    // ------------------------------------------------------------------ inicio

    /** Inicia o martelamento da peca apoiada na estacao. Retorna se a sessao foi aberta. */
    public static boolean startHammering(ServerPlayer player, ServerLevel level, BlockPos pos,
                                         PieceHolder station, ItemStack hammer, InteractionHand hand,
                                         ProcessingMode mode) {
        if (!mode.allowsMinigames() || SESSIONS.containsKey(player.getUUID())) {
            return false;
        }
        ItemStack piece = station.getPiece();
        Optional<Holder.Reference<Metal>> found = MetalLookup.find(level.registryAccess(), piece);
        if (found.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.forjasuprema.minigame.not_metal"), true);
            return false;
        }
        Metal metal = found.get().value();

        List<RecipeHolder<ForgingRecipe>> recipes = level.getRecipeManager().getAllRecipesFor(ModRecipes.FORGING.get());
        List<String> shapes = SmithHammerItem.shapesOf(recipes);
        String shape = SmithHammerItem.currentShape(hammer, shapes);
        SingleRecipeInput input = new SingleRecipeInput(piece);
        Optional<ForgingRecipe> recipe = recipes.stream().map(RecipeHolder::value)
                .filter(r -> r.shape().equals(shape) && r.matches(input, level)).findFirst();
        if (recipe.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.forjasuprema.minigame.no_recipe",
                    SmithHammerItem.shapeName(shape)), true);
            return false;
        }

        double temperature = pieceTemperature(piece, level.getGameTime());
        if (temperature < metal.workTempMin()) {
            player.displayClientMessage(Component.translatable("message.forjasuprema.minigame.too_cold",
                    Math.round(temperature), metal.workTempMin()), true);
            return false;
        }

        float toolQuality = hammer.getItem() instanceof SmithHammerItem smith ? smith.quality() : 0.0F;
        MinigameDifficulty.Params params = MinigameDifficulty.forHammering(metal.malleability(),
                recipe.get().complexity(), metal.tier(), toolQuality, PLAYER_LEVEL);
        long seed = level.getRandom().nextLong();
        StrikeSession session = new StrikeSession(MinigameKind.HAMMERING, seed, recipe.get().strikes(), params,
                level.getGameTime());
        ResourceLocation metalId = found.get().key().location();
        SESSIONS.put(player.getUUID(), new Active(session, level.dimension(), pos, hand, hammer.getItem(), piece,
                recipe.get(), metalId, metal));
        open(player, session, pos, shape);
        return true;
    }

    /** Inicia o minigame dos foles numa forja acesa. */
    public static boolean startBellows(ServerPlayer player, ServerLevel level, BlockPos pos, Boostable forge,
                                       ItemStack bellows, InteractionHand hand, ProcessingMode mode) {
        if (!mode.allowsMinigames() || SESSIONS.containsKey(player.getUUID())) {
            return false;
        }
        if (!forge.isLit()) {
            player.displayClientMessage(Component.translatable("message.forjasuprema.bellows.not_lit"), true);
            return false;
        }
        MinigameDifficulty.Params params = MinigameDifficulty.forBellows(0.0, PLAYER_LEVEL);
        StrikeSession session = new StrikeSession(MinigameKind.BELLOWS, level.getRandom().nextLong(),
                BELLOWS_STRIKES, params, level.getGameTime());
        SESSIONS.put(player.getUUID(), new Active(session, level.dimension(), pos, hand, bellows.getItem(),
                ItemStack.EMPTY, null, null, null));
        open(player, session, pos, "");
        return true;
    }

    /** Quantidade de golpes de cada minigame de acabamento. */
    private static int finishStrikes(MinigameKind kind) {
        return kind == MinigameKind.ENGRAVING ? 8 : 6;
    }

    private static boolean isFinishing(MinigameKind kind) {
        return kind == MinigameKind.POLISHING || kind == MinigameKind.SHARPENING || kind == MinigameKind.ENGRAVING;
    }

    /**
     * Inicia polimento, afiacao ou gravacao da peca apoiada na bancada. A peca precisa ser uma parte forjada,
     * estar fria e cumprir as exigencias de {@link FinishRules}; nada disso depende do desempenho no minigame.
     */
    public static boolean startFinishing(ServerPlayer player, ServerLevel level, BlockPos pos, PieceHolder station,
                                         ItemStack tool, InteractionHand hand, MinigameKind kind, ProcessingMode mode) {
        if (!mode.allowsMinigames() || !isFinishing(kind) || SESSIONS.containsKey(player.getUUID())) {
            return false;
        }
        ItemStack piece = station.getPiece();
        Optional<Holder.Reference<Metal>> found = MetalLookup.find(level.registryAccess(), piece);
        if (found.isEmpty() || piece.get(ModDataComponents.METAL_PART.get()) == null) {
            player.displayClientMessage(Component.translatable("message.forjasuprema.finish.needs_part"), true);
            return false;
        }
        Metal metal = found.get().value();
        FinishState state = piece.getOrDefault(ModDataComponents.FINISH.get(), FinishState.NONE);
        double temperature = pieceTemperature(piece, level.getGameTime());
        FinishRules.Check check = kind == MinigameKind.ENGRAVING
                ? FinishRules.checkEngraving(state, metal.tier(), temperature)
                : FinishRules.checkSurfaceWork(temperature);
        if (check != FinishRules.Check.OK) {
            player.displayClientMessage(Component.translatable(
                    "message.forjasuprema.finish." + check.name().toLowerCase(java.util.Locale.ROOT),
                    Math.round(FinishRules.MAX_WORK_TEMPERATURE), Math.round(temperature),
                    Math.round(FinishRules.MIN_POLISH_TO_ENGRAVE * 100), FinishRules.engravingSlots(metal.tier())), true);
            return false;
        }
        float toolQuality = tool.getItem() instanceof FinishToolItem finishTool ? finishTool.quality() : 0.0F;
        MinigameDifficulty.Params params = MinigameDifficulty.forFinishing(kind, metal.tier(), toolQuality, PLAYER_LEVEL);
        StrikeSession session = new StrikeSession(kind, level.getRandom().nextLong(), finishStrikes(kind), params,
                level.getGameTime());
        SESSIONS.put(player.getUUID(), new Active(session, level.dimension(), pos, hand, tool.getItem(), piece,
                null, found.get().key().location(), metal));
        open(player, session, pos, "");
        return true;
    }

    private static void open(ServerPlayer player, StrikeSession session, BlockPos pos, String shape) {
        MinigameDifficulty.Params p = session.params();
        PacketDistributor.sendToPlayer(player, new OpenMinigamePayload(session.kind().ordinal(), pos, session.seed(),
                session.strikes(), p.periodTicks(), (float) p.zoneHalfWidth(), shape));
    }

    // ------------------------------------------------------------------ pacotes do cliente

    public static void onStrike(StrikePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        Active active = SESSIONS.get(player.getUUID());
        if (active == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        MinigameEnd problem = validate(player, level, active);
        if (problem != null) {
            end(player, problem, 0.0F);
            return;
        }

        double factor = 1.0;
        if (active.session.kind() == MinigameKind.HAMMERING) {
            Metal metal = active.metal;
            double temp = pieceTemperature(active.pieceRef, level.getGameTime());
            factor = StrikeMath.temperatureFactor(temp, metal.workTempMin(), metal.workTempMax(), metal.meltTemperature());
        }
        Optional<StrikeSession.Result> result = active.session.submit(payload.index(), payload.elapsed(),
                StrikeDirection.fromOrdinal(payload.direction()), level.getGameTime(), factor);
        if (result.isEmpty()) {
            return;
        }
        StrikeSession.Result r = result.get();
        PacketDistributor.sendToPlayer(player, new StrikeResultPayload(r.index(), r.verdict().ordinal(), (float) r.accuracy()));
        strikeEffects(level, active, r.verdict());

        if (active.session.kind() != MinigameKind.BELLOWS) {
            ItemStack tool = player.getItemInHand(active.hand);
            tool.hurtAndBreak(1, player, LivingEntity.getSlotForHand(active.hand));
        }
        if (r.finished()) {
            complete(player, level, active);
        }
    }

    public static void onCancel(CancelMinigamePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && SESSIONS.containsKey(player.getUUID())) {
            end(player, MinigameEnd.CANCELLED, 0.0F);
        }
    }

    // ------------------------------------------------------------------ verificacao periodica

    /** Chamado a cada tick de jogador: encerra sessoes que deixaram de ser validas. */
    public static void tick(ServerPlayer player) {
        if (SESSIONS.isEmpty()) {
            return;
        }
        Active active = SESSIONS.get(player.getUUID());
        if (active == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        MinigameEnd problem = validate(player, level, active);
        if (problem != null) {
            end(player, problem, 0.0F);
        }
    }

    public static void onLogout(ServerPlayer player) {
        SESSIONS.remove(player.getUUID());
    }

    public static void clearAll() {
        SESSIONS.clear();
    }

    /** Motivo para encerrar a sessao agora, ou {@code null} se ela continua valida. */
    private static MinigameEnd validate(ServerPlayer player, ServerLevel level, Active active) {
        if (!player.isAlive() || player.isRemoved()) {
            return MinigameEnd.INTERRUPTED;
        }
        if (!level.dimension().equals(active.dimension)) {
            return MinigameEnd.TOO_FAR;
        }
        if (player.distanceToSqr(Vec3.atCenterOf(active.pos)) > MAX_DISTANCE_SQR) {
            return MinigameEnd.TOO_FAR;
        }
        if (player.getItemInHand(active.hand).getItem() != active.tool) {
            return MinigameEnd.TOOL_CHANGED;
        }
        if (active.session.isExpired(level.getGameTime())) {
            return MinigameEnd.TIMEOUT;
        }
        if (active.session.kind() == MinigameKind.BELLOWS) {
            if (!(level.getBlockEntity(active.pos) instanceof Boostable forge) || !forge.isLit()) {
                return MinigameEnd.INTERRUPTED;
            }
            return null;
        }
        if (!(level.getBlockEntity(active.pos) instanceof PieceHolder station) || station.getPiece() != active.pieceRef) {
            return MinigameEnd.PIECE_CHANGED;
        }
        if (active.session.kind() == MinigameKind.HAMMERING) {
            double temp = pieceTemperature(active.pieceRef, level.getGameTime());
            if (StrikeMath.shouldAbortCold(temp, active.metal.workTempMin())) {
                return MinigameEnd.COOLED;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ conclusao

    private static void complete(ServerPlayer player, ServerLevel level, Active active) {
        double score = active.session.finalScore();
        if (active.session.kind() == MinigameKind.BELLOWS) {
            if (level.getBlockEntity(active.pos) instanceof Boostable forge) {
                forge.boost(1.0 + 1.5 * score, 200 + (int) Math.round(400 * score));
            }
            ItemStack bellows = player.getItemInHand(active.hand);
            bellows.hurtAndBreak(1, player, LivingEntity.getSlotForHand(active.hand));
            level.playSound(null, active.pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.8F, 0.9F);
            end(player, MinigameEnd.COMPLETED, (float) score);
            return;
        }

        if (!(level.getBlockEntity(active.pos) instanceof PieceHolder station)) {
            end(player, MinigameEnd.PIECE_CHANGED, 0.0F);
            return;
        }
        if (isFinishing(active.session.kind())) {
            completeFinishing(player, level, active, station, score);
            return;
        }
        long now = level.getGameTime();
        HeatData oldHeat = active.pieceRef.get(ModDataComponents.HEAT.get());
        double burn = oldHeat == null ? 0.0 : oldHeat.burn();
        double temp = pieceTemperature(active.pieceRef, now);

        ItemStack result = active.recipe.assemble(new SingleRecipeInput(active.pieceRef), level.registryAccess());
        result.set(ModDataComponents.METAL_PART.get(), active.metalId);
        result.set(ModDataComponents.HEAT.get(), new HeatData(temp, now, burn));
        // Notas de etapas anteriores (soldagem, tratamentos) sao preservadas; trabalhar o metal a quente
        // apaga o endurecimento anterior (a peca volta ao estado recozido).
        StageScores previous = active.pieceRef.getOrDefault(ModDataComponents.STAGE_SCORES.get(), StageScores.empty());
        result.set(ModDataComponents.STAGE_SCORES.get(), previous
                .with(StageScores.MODELING, (float) score)
                .with(StageScores.THERMAL, (float) Math.max(0.0, 1.0 - burn)));
        result.set(ModDataComponents.FORGE_TIER.get(), active.pieceRef.getOrDefault(ModDataComponents.FORGE_TIER.get(), 0));
        result.remove(ModDataComponents.TREATMENT.get());
        result.remove(ModDataComponents.FINISH.get());

        station.setPiece(result);
        level.playSound(null, active.pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.7F, 1.1F);
        end(player, MinigameEnd.COMPLETED, (float) score);
    }

    private static void completeFinishing(ServerPlayer player, ServerLevel level, Active active, PieceHolder station,
                                          double score) {
        ItemStack piece = active.pieceRef;
        FinishState state = piece.getOrDefault(ModDataComponents.FINISH.get(), FinishState.NONE);
        TreatmentState treatment = piece.getOrDefault(ModDataComponents.TREATMENT.get(), TreatmentState.NONE);
        MinigameKind kind = active.session.kind();
        String stage;
        FinishState next;
        switch (kind) {
            case POLISHING -> {
                next = FinishRules.polish(state, score);
                stage = StageScores.POLISHING;
            }
            case SHARPENING -> {
                next = FinishRules.sharpen(state, score, active.metal.hardness() + treatment.hardness(),
                        treatment.brittleness());
                stage = StageScores.SHARPENING;
            }
            default -> {
                next = FinishRules.engrave(state, score);
                stage = StageScores.ENGRAVING;
            }
        }
        ItemStack result = piece.copy();
        result.set(ModDataComponents.FINISH.get(), next);
        StageScores previous = piece.getOrDefault(ModDataComponents.STAGE_SCORES.get(), StageScores.empty());
        result.set(ModDataComponents.STAGE_SCORES.get(), previous.with(stage, (float) score));
        station.setPiece(result);
        level.playSound(null, active.pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.8F, 1.3F);
        end(player, MinigameEnd.COMPLETED, (float) score);
    }

    private static void end(ServerPlayer player, MinigameEnd reason, float score) {
        SESSIONS.remove(player.getUUID());
        PacketDistributor.sendToPlayer(player, new EndMinigamePayload(reason.ordinal(), score));
        if (reason != MinigameEnd.COMPLETED && reason != MinigameEnd.CANCELLED) {
            player.displayClientMessage(Component.translatable(
                    "message.forjasuprema.minigame.end." + reason.name().toLowerCase(java.util.Locale.ROOT)), true);
        }
    }

    private static double pieceTemperature(ItemStack piece, long now) {
        HeatData heat = piece.get(ModDataComponents.HEAT.get());
        return heat == null ? ThermalMath.AMBIENT : heat.temperatureAt(now);
    }

    private static void strikeEffects(ServerLevel level, Active active, StrikeVerdict verdict) {
        if (isFinishing(active.session.kind())) {
            float pitch = verdict == StrikeVerdict.PERFECT ? 1.5F : verdict == StrikeVerdict.GOOD ? 1.2F : 0.8F;
            level.playSound(null, active.pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.4F, pitch);
            return;
        }
        if (active.session.kind() != MinigameKind.HAMMERING) {
            level.playSound(null, active.pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 1.6F);
            return;
        }
        float pitch = switch (verdict) {
            case PERFECT -> 1.4F;
            case GOOD -> 1.1F;
            case POOR -> 0.8F;
            case MISS, WRONG_DIRECTION -> 0.5F;
        };
        level.playSound(null, active.pos, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.6F, pitch);
        if (verdict == StrikeVerdict.PERFECT || verdict == StrikeVerdict.GOOD) {
            level.sendParticles(ParticleTypes.CRIT, active.pos.getX() + 0.5, active.pos.getY() + 1.0,
                    active.pos.getZ() + 0.5, verdict == StrikeVerdict.PERFECT ? 10 : 4, 0.2, 0.1, 0.2, 0.1);
        }
    }
}
