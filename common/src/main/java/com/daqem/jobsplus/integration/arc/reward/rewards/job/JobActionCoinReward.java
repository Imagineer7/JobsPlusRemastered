package com.daqem.jobsplus.integration.arc.reward.rewards.job;

import com.daqem.arc.api.action.data.ActionData;
import com.daqem.arc.api.action.result.ActionResult;
import com.daqem.arc.api.player.ArcPlayer;
import com.daqem.arc.api.reward.AbstractReward;
import com.daqem.arc.api.reward.serializer.IRewardSerializer;
import com.daqem.arc.api.reward.type.IRewardType;
import com.daqem.jobsplus.JobsPlus;
import com.daqem.jobsplus.config.JobPaymentHelper;
import com.daqem.jobsplus.config.JobsPlusConfig;
import com.daqem.jobsplus.integration.arc.holder.holders.job.JobInstance;
import com.daqem.jobsplus.integration.arc.reward.type.JobsPlusRewardType;
import com.daqem.jobsplus.player.JobsServerPlayer;
import com.google.gson.JsonObject;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;

/**
 * Reward that gives coins to players when they perform job actions.
 * The amount can be scaled by a global multiplier in the config.
 * Can also execute custom payment commands (e.g., for economy mods).
 */
public class JobActionCoinReward extends AbstractReward {

    private final double min;
    private final double max;

    public JobActionCoinReward(double chance, int priority, double min, double max) {
        super(chance, priority);
        this.min = min;
        this.max = max;

        if (min > max) {
            throw new IllegalArgumentException("min cannot be greater than max for JobActionCoinReward.");
        }
    }

    @Override
    public IRewardType<?> getType() {
        return JobsPlusRewardType.JOB_ACTION_COIN;
    }

    @Override
    public ActionResult apply(ActionData actionData) {
        ArcPlayer player = actionData.getPlayer();

        JobsPlus.debug("JobActionCoinReward.apply() called - Player: {}", player);

        if (!(player instanceof JobsServerPlayer jobsServerPlayer)) {
            JobsPlus.debug("Player is not JobsServerPlayer, skipping reward");
            return new ActionResult();
        }

        // Get job information for multiplier
        ResourceLocation jobLocation = null;
        String jobName = "unknown";
        if (actionData.getSourceActionHolder() instanceof JobInstance jobInstance) {
            jobLocation = jobInstance.getLocation();
            jobName = jobInstance.getName().getString();
            JobsPlus.debug("Job info - Location: {}, Name: {}", jobLocation, jobName);
        } else {
            JobsPlus.debug("Source action holder is not a JobInstance: {}", actionData.getSourceActionHolder());
        }

        // Debug config values
        JobsPlus.debug("Config - useActionPaymentCommand: {}, actionCoinMultiplier: {}",
            JobsPlusConfig.useActionPaymentCommand.get(),
            JobsPlusConfig.actionCoinMultiplier.get());
        JobsPlus.debug("Reward range - min: {}, max: {}", min, max);

        // Only give rewards if enabled by user
        // Check if custom payment command is enabled
        if (JobsPlusConfig.useActionPaymentCommand.get()) {
            JobsPlus.debug("Using action payment command system");
            // Calculate random amount
            double randomAmount = min + player.arc$getPlayer().getRandom().nextDouble() * (max - min);
            JobsPlus.debug("Random amount calculated: {}", randomAmount);
            executePaymentCommand((ServerPlayer) jobsServerPlayer, randomAmount, jobLocation, jobName);
        }
        // Check if internal coin multiplier is enabled (> 0)
        else if (JobsPlusConfig.actionCoinMultiplier.get() > 0) {
            JobsPlus.debug("Using internal coin system with multiplier: {}", JobsPlusConfig.actionCoinMultiplier.get());
            // Calculate random amount
            double randomAmount = min + player.arc$getPlayer().getRandom().nextDouble() * (max - min);
            double finalAmount = JobPaymentHelper.calculatePaymentAmount(randomAmount, jobLocation);
            JobsPlus.debug("Internal coins - random: {}, final: {}", randomAmount, finalAmount);
            jobsServerPlayer.jobsplus$addCoins(finalAmount);

            // Show payment in action bar if enabled
            if (JobsPlusConfig.showPaymentInActionBar.get()) {
                sendPaymentFeedback((ServerPlayer) jobsServerPlayer, finalAmount);
            }
        } else {
            JobsPlus.debug("Neither payment system is enabled - useActionPaymentCommand: {}, actionCoinMultiplier: {}",
                JobsPlusConfig.useActionPaymentCommand.get(),
                JobsPlusConfig.actionCoinMultiplier.get());
        }

        return new ActionResult();
    }

    /**
     * Executes the custom payment command configured in the config.
     */
    private void executePaymentCommand(ServerPlayer player, double baseAmount, ResourceLocation jobLocation, String jobName) {
        JobsPlus.debug("executePaymentCommand called - Player: {}, baseAmount: {}, jobLocation: {}, jobName: {}",
            player.getGameProfile().getName(), baseAmount, jobLocation, jobName);

        if (player.getServer() == null) {
            JobsPlus.debug("Player server is null, cannot execute command");
            return;
        }

        // Calculate final amount with job multiplier and base multiplier
        double finalAmount = JobPaymentHelper.calculatePaymentAmount(baseAmount, jobLocation);
        JobsPlus.debug("Payment calculation - baseAmount: {}, jobMultiplier: {}, baseMultiplier: {}, finalAmount: {}",
            baseAmount,
            JobPaymentHelper.getJobMultiplier(jobLocation),
            JobsPlusConfig.actionPaymentBaseAmount.get(),
            finalAmount);

        // Get command template from config
        String commandTemplate = JobsPlusConfig.actionPaymentCommand.get();
        JobsPlus.debug("Command template from config: '{}'", commandTemplate);

        if (commandTemplate == null || commandTemplate.isEmpty()) {
            JobsPlus.debug("Command template is null or empty, cannot execute");
            return;
        }

        // Replace placeholders
        String command = commandTemplate
                .replace("{player}", player.getGameProfile().getName())
                .replace("{amount}", String.format("%.2f", finalAmount))
                .replace("{job}", jobName);

        JobsPlus.debug("Final command after placeholder replacement: '{}'", command);

        try {
            JobsPlus.debug("Attempting to execute command via server command source...");
            // Execute command as server console
            player.getServer().getCommands().performPrefixedCommand(
                    player.getServer().createCommandSourceStack(),
                    command
            );

            JobsPlus.debug("Command executed successfully (no exception thrown)");

            // Command executed without exception - assume success
            // Show payment in action bar if enabled
            if (JobsPlusConfig.showPaymentInActionBar.get()) {
                sendPaymentFeedback(player, finalAmount);
            }

            JobsPlus.debug("Executed action payment command: {} (base: {}, multiplier: {}, final: {})",
                    command, baseAmount, JobPaymentHelper.getJobMultiplier(jobLocation), finalAmount);
        } catch (Exception e) {
            // Error executing command - send chat message to player
            JobsPlus.debug("Exception occurred while executing command: {}", e.getMessage());
            sendPaymentError(player, e.getMessage());
            JobsPlus.LOGGER.error("Failed to execute action payment command: {}", command, e);
        }
    }

    /**
     * Sends payment feedback to the player's action bar.
     */
    private void sendPaymentFeedback(ServerPlayer player, double amount) {
        Component component = JobsPlus.translatable("job.payment.gain",
                JobsPlus.formatNumber(amount))
                .withStyle(net.minecraft.ChatFormatting.GREEN)
                .withStyle(net.minecraft.ChatFormatting.BOLD);
        player.sendSystemMessage(component, true);
    }

    /**
     * Sends payment error message to the player's chat.
     */
    private void sendPaymentError(ServerPlayer player, String error) {
        Component component = JobsPlus.translatable("job.payment.command_error");
        player.sendSystemMessage(component, false);

        if (error != null && !error.isEmpty()) {
            Component detailComponent = Component.literal("  " + error)
                    .withStyle(net.minecraft.ChatFormatting.GRAY);
            player.sendSystemMessage(detailComponent, false);
        }
    }

    @Override
    public Component getDescription() {
        if (min == max) {
            return Component.literal("Coins: " + min);
        }
        return Component.literal("Coins: " + min + "-" + max);
    }

    public static class Serializer implements IRewardSerializer<JobActionCoinReward> {

        @Override
        public JobActionCoinReward fromJson(JsonObject jsonObject, double chance, int priority) {
            double min = GsonHelper.getAsDouble(jsonObject, "min", 0);
            double max = GsonHelper.getAsDouble(jsonObject, "max", min);

            return new JobActionCoinReward(chance, priority, min, max);
        }

        @Override
        public JobActionCoinReward fromNetwork(RegistryFriendlyByteBuf friendlyByteBuf, double chance, int priority) {
            return new JobActionCoinReward(
                    chance,
                    priority,
                    friendlyByteBuf.readDouble(),
                    friendlyByteBuf.readDouble()
            );
        }

        @Override
        public void toNetwork(RegistryFriendlyByteBuf friendlyByteBuf, JobActionCoinReward reward) {
            friendlyByteBuf.writeDouble(reward.min);
            friendlyByteBuf.writeDouble(reward.max);
        }
    }
}

