package com.daqem.jobsplus.integration.arc.reward.rewards.job;

import com.daqem.arc.api.action.data.ActionData;
import com.daqem.arc.api.action.result.ActionResult;
import com.daqem.arc.api.reward.AbstractReward;
import com.daqem.arc.api.reward.serializer.IRewardSerializer;
import com.daqem.arc.api.reward.type.IRewardType;
import com.daqem.jobsplus.JobsPlus;
import com.daqem.jobsplus.config.JobPaymentHelper;
import com.daqem.jobsplus.config.JobsPlusConfig;
import com.daqem.jobsplus.integration.arc.holder.holders.job.JobInstance;
import com.daqem.jobsplus.integration.arc.reward.type.JobsPlusRewardType;
import com.daqem.jobsplus.player.JobsServerPlayer;
import com.daqem.jobsplus.player.job.Job;
import com.google.gson.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;

public class JobExpReward extends AbstractReward {

    private final int min;
    private final int max;

    // Optional payment override - if set, use these values instead of XP for payment
    private final Double paymentMin;
    private final Double paymentMax;

    public JobExpReward(double chance, int priority, int min, int max, Double paymentMin, Double paymentMax) {
        super(chance, priority);
        this.min = min;
        this.max = max;
        this.paymentMin = paymentMin;
        this.paymentMax = paymentMax;

        if (min > max) {
            throw new IllegalArgumentException("min cannot be greater than max for JobExpActionReward.");
        }

        if (paymentMin != null && paymentMax != null && paymentMin > paymentMax) {
            throw new IllegalArgumentException("payment_min cannot be greater than payment_max for JobExpReward.");
        }
    }

    @Override
    public IRewardType<?> getType() {
        return JobsPlusRewardType.JOB_EXP;
    }

    @Override
    public ActionResult apply(ActionData actionData) {
        if (actionData.getSourceActionHolder() instanceof JobInstance jobInstance) {
            if (actionData.getPlayer() instanceof JobsServerPlayer jobsServerPlayer) {
                Job job = jobsServerPlayer.jobsplus$getJob(jobInstance);
                if (job != null) {
                    double exp = min + actionData.getPlayer().arc$getPlayer().getRandom().nextDouble() * (max - min);
                    job.addExperience(exp);

                    // Check if payment command should be executed
                    if (JobsPlusConfig.useActionPaymentCommand.get()) {
                        // Determine payment amount - use override if specified, otherwise use XP amount
                        double paymentAmount;
                        if (paymentMin != null && paymentMax != null) {
                            // Override specified - use payment_min and payment_max
                            paymentAmount = paymentMin + actionData.getPlayer().arc$getPlayer().getRandom().nextDouble() * (paymentMax - paymentMin);
                            JobsPlus.debug("JobExpReward: Using payment override - paymentMin: {}, paymentMax: {}, rolled: {}",
                                paymentMin, paymentMax, paymentAmount);
                        } else {
                            // No override - use XP amount
                            paymentAmount = exp;
                            JobsPlus.debug("JobExpReward: No payment override, using XP amount: {}", paymentAmount);
                        }

                        JobsPlus.debug("JobExpReward: Payment command is enabled, executing payment for job action");
                        executePaymentCommand((ServerPlayer) jobsServerPlayer, paymentAmount, jobInstance.getLocation(), jobInstance.getName().getString());
                    } else if (JobsPlusConfig.actionCoinMultiplier.get() > 0) {
                        // Internal coin system - also respects payment override
                        double coinBaseAmount = (paymentMin != null && paymentMax != null)
                            ? paymentMin + actionData.getPlayer().arc$getPlayer().getRandom().nextDouble() * (paymentMax - paymentMin)
                            : exp;

                        JobsPlus.debug("JobExpReward: Internal coin system is enabled (multiplier: {}), base amount: {}",
                            JobsPlusConfig.actionCoinMultiplier.get(), coinBaseAmount);
                        double coinAmount = JobPaymentHelper.calculatePaymentAmount(coinBaseAmount, jobInstance.getLocation());
                        jobsServerPlayer.jobsplus$addCoins(coinAmount);

                        if (JobsPlusConfig.showPaymentInActionBar.get()) {
                            sendPaymentFeedback((ServerPlayer) jobsServerPlayer, coinAmount);
                        }
                    }
                }
            }
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

            JobsPlus.debug("Command executed successfully");

            // Show payment in action bar if enabled
            if (JobsPlusConfig.showPaymentInActionBar.get()) {
                sendPaymentFeedback(player, finalAmount);
            }

            JobsPlus.debug("Executed action payment command: {} (base: {}, multiplier: {}, final: {})",
                    command, baseAmount, JobPaymentHelper.getJobMultiplier(jobLocation), finalAmount);
        } catch (Exception e) {
            JobsPlus.debug("Exception occurred while executing command: {}", e.getMessage());
            JobsPlus.LOGGER.error("Failed to execute action payment command: {}", command, e);
        }
    }

    /**
     * Sends payment feedback to the player's action bar.
     */
    private void sendPaymentFeedback(ServerPlayer player, double amount) {
        player.displayClientMessage(
            Component.literal(String.format("§a+%.2f coins", amount)),
            true // Action bar
        );
    }

    @Override
    public Component getDescription() {
        return this.getDescription(this.min, this.max);
    }

    public static class Serializer implements IRewardSerializer<JobExpReward> {
        @Override
        public JobExpReward fromJson(JsonObject jsonObject, double chance, int priority) {
            // Required fields
            int min = GsonHelper.getAsInt(jsonObject, "min");
            int max = GsonHelper.getAsInt(jsonObject, "max");

            // Optional payment override fields
            Double paymentMin = jsonObject.has("payment_min") ? GsonHelper.getAsDouble(jsonObject, "payment_min") : null;
            Double paymentMax = jsonObject.has("payment_max") ? GsonHelper.getAsDouble(jsonObject, "payment_max") : null;

            return new JobExpReward(chance, priority, min, max, paymentMin, paymentMax);
        }

        @Override
        public JobExpReward fromNetwork(RegistryFriendlyByteBuf friendlyByteBuf, double chance, int priority) {
            int min = friendlyByteBuf.readInt();
            int max = friendlyByteBuf.readInt();

            // Read optional payment overrides
            boolean hasPaymentOverride = friendlyByteBuf.readBoolean();
            Double paymentMin = hasPaymentOverride ? friendlyByteBuf.readDouble() : null;
            Double paymentMax = hasPaymentOverride ? friendlyByteBuf.readDouble() : null;

            return new JobExpReward(chance, priority, min, max, paymentMin, paymentMax);
        }

        @Override
        public void toNetwork(RegistryFriendlyByteBuf friendlyByteBuf, JobExpReward type) {
            IRewardSerializer.super.toNetwork(friendlyByteBuf, type);
            friendlyByteBuf.writeInt(type.min);
            friendlyByteBuf.writeInt(type.max);

            // Write optional payment overrides
            boolean hasPaymentOverride = type.paymentMin != null && type.paymentMax != null;
            friendlyByteBuf.writeBoolean(hasPaymentOverride);
            if (hasPaymentOverride) {
                friendlyByteBuf.writeDouble(type.paymentMin);
                friendlyByteBuf.writeDouble(type.paymentMax);
            }
        }
    }
}
