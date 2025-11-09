package com.daqem.jobsplus.networking.c2s;

import com.daqem.jobsplus.JobsPlus;
import com.daqem.jobsplus.config.JobsPlusConfig;
import com.daqem.jobsplus.integration.arc.holder.holders.job.JobInstance;
import com.daqem.jobsplus.networking.JobsPlusNetworking;
import com.daqem.jobsplus.networking.s2c.ClientboundJobStatsPacket;
import com.daqem.jobsplus.networking.s2c.ClientboundOpenJobsScreenPacket;
import com.daqem.jobsplus.player.JobsServerPlayer;
import com.daqem.jobsplus.player.job.Job;
import com.daqem.jobsplus.player.job.JobLimitationManager;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

public class ServerboundLeaveJobPacket implements CustomPacketPayload {

    private final ResourceLocation jobLocation;

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundLeaveJobPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public @NotNull ServerboundLeaveJobPacket decode(RegistryFriendlyByteBuf buf) {
            return new ServerboundLeaveJobPacket(buf);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, ServerboundLeaveJobPacket packet) {
            buf.writeResourceLocation(packet.jobLocation);
        }
    };

    public ServerboundLeaveJobPacket(ResourceLocation jobLocation) {
        this.jobLocation = jobLocation;
    }

    public ServerboundLeaveJobPacket(RegistryFriendlyByteBuf friendlyByteBuf) {
        this.jobLocation = friendlyByteBuf.readResourceLocation();
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return JobsPlusNetworking.SERVERBOUND_LEAVE_JOB;
    }

    public static void handleServerSide(ServerboundLeaveJobPacket packet, NetworkManager.PacketContext context) {
        if (context.getPlayer() instanceof JobsServerPlayer serverPlayer) {
            JobInstance jobInstance = JobInstance.of(packet.jobLocation);

            if (jobInstance == null) {
                context.getPlayer().sendSystemMessage(JobsPlus.translatable("error.job_not_found", packet.jobLocation.toString()));
                return;
            }

            Job job = serverPlayer.jobsplus$getJob(jobInstance);
            if (job == null) {
                context.getPlayer().sendSystemMessage(JobsPlus.translatable("error.job.not_performing"));
                return;
            }

            int maxLevelToLeave = JobsPlusConfig.maxLevelToLeaveJob.get();
            if (maxLevelToLeave == 0) {
                context.getPlayer().sendSystemMessage(JobsPlus.translatable("error.job.leaving_disabled"));
                return;
            }

            if (job.getLevel() > maxLevelToLeave) {
                context.getPlayer().sendSystemMessage(JobsPlus.translatable("error.job.level_too_high", maxLevelToLeave));
                return;
            }

            if (JobsPlusConfig.resetStatsOnLeaveJob.get()) {
                // Reset stats by removing the job completely
                serverPlayer.jobsplus$removeJob(jobInstance);
                context.getPlayer().sendSystemMessage(JobsPlus.translatable("message.job.left_with_reset", jobInstance.getName()));
            } else {
                // Keep stats but mark as inactive by setting level to 0
                job.setLevel(0);
                job.setExperience(0, false);
                serverPlayer.jobsplus$removeActionHolders(job);
                serverPlayer.jobsplus$removeJobOnClient(job);
                context.getPlayer().sendSystemMessage(JobsPlus.translatable("message.job.left_without_reset", jobInstance.getName()));
            }

            // Remove job assignment from persistent storage
            com.daqem.jobsplus.player.job.JobAssignmentData.get(context.getPlayer().getServer())
                .removePlayerFromJob(serverPlayer.jobsplus$getServerPlayer().getUUID(), jobInstance.getLocation().toString());

            NetworkManager.sendToPlayer((ServerPlayer) serverPlayer, new ClientboundOpenJobsScreenPacket(
                    Stream.concat(serverPlayer.jobsplus$getJobs().stream(), serverPlayer.jobsplus$getInactiveJobs().stream()).toList(),
                    serverPlayer.jobsplus$getCoins()
            ));

            // Send updated job stats to client
            JobLimitationManager manager = JobLimitationManager.getInstance();
            var stats = manager.getAllJobStats(context.getPlayer().getServer());
            java.util.Map<String, ClientboundJobStatsPacket.JobStatInfo> clientStats = new java.util.HashMap<>();
            for (var entry : stats.entrySet()) {
                clientStats.put(entry.getKey(), new ClientboundJobStatsPacket.JobStatInfo(
                    entry.getValue().getCurrentPlayers(),
                    entry.getValue().getMaxPlayers()
                ));
            }
            NetworkManager.sendToPlayer((ServerPlayer) serverPlayer, new ClientboundJobStatsPacket(clientStats));
        }
    }
}

