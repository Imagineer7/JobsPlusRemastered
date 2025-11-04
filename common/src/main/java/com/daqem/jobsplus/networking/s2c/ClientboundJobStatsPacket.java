package com.daqem.jobsplus.networking.s2c;

import com.daqem.jobsplus.networking.JobsPlusNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class ClientboundJobStatsPacket implements CustomPacketPayload {

    private final Map<String, JobStatInfo> jobStats;

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundJobStatsPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public @NotNull ClientboundJobStatsPacket decode(RegistryFriendlyByteBuf buf) {
            return new ClientboundJobStatsPacket(buf);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, ClientboundJobStatsPacket packet) {
            buf.writeVarInt(packet.jobStats.size());
            for (Map.Entry<String, JobStatInfo> entry : packet.jobStats.entrySet()) {
                buf.writeUtf(entry.getKey());
                buf.writeVarInt(entry.getValue().currentPlayers);
                buf.writeVarInt(entry.getValue().maxPlayers);
            }
        }
    };

    public ClientboundJobStatsPacket(Map<String, JobStatInfo> jobStats) {
        this.jobStats = jobStats;
    }

    public ClientboundJobStatsPacket(RegistryFriendlyByteBuf friendlyByteBuf) {
        int size = friendlyByteBuf.readVarInt();
        this.jobStats = new HashMap<>();
        for (int i = 0; i < size; i++) {
            String jobId = friendlyByteBuf.readUtf();
            int current = friendlyByteBuf.readVarInt();
            int max = friendlyByteBuf.readVarInt();
            this.jobStats.put(jobId, new JobStatInfo(current, max));
        }
    }

    public Map<String, JobStatInfo> getJobStats() {
        return jobStats;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return JobsPlusNetworking.CLIENTBOUND_JOB_STATS;
    }

    public static class JobStatInfo {
        public final int currentPlayers;
        public final int maxPlayers;

        public JobStatInfo(int currentPlayers, int maxPlayers) {
            this.currentPlayers = currentPlayers;
            this.maxPlayers = maxPlayers;
        }

        public boolean isFull() {
            return currentPlayers >= maxPlayers;
        }
    }

    public static void handleClientSide(ClientboundJobStatsPacket packet, dev.architectury.networking.NetworkManager.PacketContext context) {
        // Store job stats on client side for UI display
        com.daqem.jobsplus.client.JobStatsClientCache.setJobStats(packet.getJobStats());
    }
}
