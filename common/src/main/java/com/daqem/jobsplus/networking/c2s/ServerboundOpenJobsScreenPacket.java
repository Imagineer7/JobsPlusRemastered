package com.daqem.jobsplus.networking.c2s;

import com.daqem.jobsplus.client.screen.job.JobsScreen;
import com.daqem.jobsplus.networking.JobsPlusNetworking;
import com.daqem.jobsplus.networking.s2c.ClientboundJobStatsPacket;
import com.daqem.jobsplus.networking.s2c.ClientboundOpenJobsScreenPacket;
import com.daqem.jobsplus.player.JobsServerPlayer;
import com.daqem.jobsplus.player.job.JobLimitationManager;
import dev.architectury.networking.NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.stream.Stream;

public class ServerboundOpenJobsScreenPacket implements CustomPacketPayload {

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundOpenJobsScreenPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public @NotNull ServerboundOpenJobsScreenPacket decode(RegistryFriendlyByteBuf buf) {
            return new ServerboundOpenJobsScreenPacket(buf);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, ServerboundOpenJobsScreenPacket packet) {
        }
    };

    public ServerboundOpenJobsScreenPacket() {
    }

    public ServerboundOpenJobsScreenPacket(RegistryFriendlyByteBuf friendlyByteBuf) {
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return JobsPlusNetworking.SERVERBOUND_OPEN_JOBS_SCREEN;
    }

    public static void handleServerSide(ServerboundOpenJobsScreenPacket packet, NetworkManager.PacketContext context) {
        if (context.getPlayer() instanceof JobsServerPlayer jobsServerPlayer) {
            // Send job data
            NetworkManager.sendToPlayer(jobsServerPlayer.jobsplus$getServerPlayer(), new ClientboundOpenJobsScreenPacket(
                    Stream.concat(jobsServerPlayer.jobsplus$getJobs().stream(), jobsServerPlayer.jobsplus$getInactiveJobs().stream()).toList(),
                    jobsServerPlayer.jobsplus$getCoins()
            ));

            // Send job stats for limitation display
            JobLimitationManager manager = JobLimitationManager.getInstance();
            var stats = manager.getAllJobStats(context.getPlayer().getServer());
            java.util.Map<String, ClientboundJobStatsPacket.JobStatInfo> clientStats = new java.util.HashMap<>();
            for (var entry : stats.entrySet()) {
                clientStats.put(entry.getKey(), new ClientboundJobStatsPacket.JobStatInfo(
                    entry.getValue().getCurrentPlayers(),
                    entry.getValue().getMaxPlayers()
                ));
            }
            NetworkManager.sendToPlayer(jobsServerPlayer.jobsplus$getServerPlayer(), new ClientboundJobStatsPacket(clientStats));
        }
    }
}
