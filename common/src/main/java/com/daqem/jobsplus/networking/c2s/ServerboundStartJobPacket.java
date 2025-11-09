package com.daqem.jobsplus.networking.c2s;

import com.daqem.jobsplus.JobsPlus;
import com.daqem.jobsplus.config.JobsPlusConfig;
import com.daqem.jobsplus.integration.arc.holder.holders.job.JobInstance;
import com.daqem.jobsplus.networking.JobsPlusNetworking;
import com.daqem.jobsplus.networking.s2c.ClientboundJobStatsPacket;
import com.daqem.jobsplus.networking.s2c.ClientboundOpenJobsScreenPacket;
import com.daqem.jobsplus.player.JobsServerPlayer;
import com.daqem.jobsplus.player.job.JobLimitationManager;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

public class ServerboundStartJobPacket implements CustomPacketPayload {

    private final ResourceLocation jobLocation;

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundStartJobPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public @NotNull ServerboundStartJobPacket decode(RegistryFriendlyByteBuf buf) {
            return new ServerboundStartJobPacket(buf);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, ServerboundStartJobPacket packet) {
            buf.writeResourceLocation(packet.jobLocation);
        }
    };

    public ServerboundStartJobPacket(ResourceLocation jobLocation) {
        this.jobLocation = jobLocation;
    }

    public ServerboundStartJobPacket(RegistryFriendlyByteBuf friendlyByteBuf) {
        this.jobLocation = friendlyByteBuf.readResourceLocation();
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return JobsPlusNetworking.SERVERBOUND_START_JOB;
    }

    public static void handleServerSide(ServerboundStartJobPacket packet, NetworkManager.PacketContext context) {
        if (context.getPlayer() instanceof JobsServerPlayer serverPlayer) {
            JobInstance jobInstance = JobInstance.of(packet.jobLocation);

            if (jobInstance == null) {
                context.getPlayer().sendSystemMessage(JobsPlus.translatable("error.job_not_found", packet.jobLocation.toString()));
                return;
            }

            // Check job limitations
            if (!JobLimitationManager.getInstance().canPlayerJoinJob(jobInstance, context.getPlayer().getServer())) {
                int currentCount = JobLimitationManager.getInstance().getCurrentJobPlayerCount(jobInstance, context.getPlayer().getServer());
                int maxCount = JobLimitationManager.getInstance().getJobPlayerLimit(jobInstance);
                context.getPlayer().sendSystemMessage(JobsPlus.translatable("error.job_full",
                    jobInstance.getName().getString(), currentCount, maxCount));
                return;
            }

            if (serverPlayer.jobsplus$getJobs().size() >= JobsPlusConfig.maxJobs.get()) {
                context.getPlayer().sendSystemMessage(JobsPlus.translatable("error.max_jobs_reached"));
                return;
            }

            if (serverPlayer.jobsplus$getJobs().size() >= JobsPlusConfig.amountOfFreeJobs.get()) {
                if (serverPlayer.jobsplus$getCoins() < jobInstance.getPrice()) {
                    context.getPlayer().sendSystemMessage(JobsPlus.translatable("error.not_enough_coins"));
                    return;
                }
                serverPlayer.jobsplus$setCoins(serverPlayer.jobsplus$getCoins() - jobInstance.getPrice());
            }

            serverPlayer.jobsplus$addNewJob(jobInstance);

            // Record job assignment in persistent storage
            com.daqem.jobsplus.player.job.JobAssignmentData.get(context.getPlayer().getServer())
                .addPlayerToJob(serverPlayer.jobsplus$getServerPlayer().getUUID(), jobInstance.getLocation().toString());

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
