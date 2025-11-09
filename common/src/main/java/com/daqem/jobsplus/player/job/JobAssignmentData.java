package com.daqem.jobsplus.player.job;

import com.daqem.jobsplus.JobsPlus;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Persistent storage for tracking which players have which jobs across server restarts
 * This allows job limitation counts to be accurate even when players are offline
 */
public class JobAssignmentData extends SavedData {

    private static final String DATA_NAME = JobsPlus.MOD_ID + "_job_assignments";

    // Map of job ID -> Set of player UUIDs
    private final Map<String, Set<UUID>> jobAssignments = new HashMap<>();

    public JobAssignmentData() {
        super();
    }

    /**
     * Get or create the job assignment data for a server
     */
    public static JobAssignmentData get(MinecraftServer server) {
        DimensionDataStorage storage = server.overworld().getDataStorage();
        return storage.computeIfAbsent(
            new SavedData.Factory<>(
                JobAssignmentData::new,
                JobAssignmentData::load,
                null
            ),
            DATA_NAME
        );
    }

    /**
     * Load job assignment data from NBT
     */
    public static JobAssignmentData load(CompoundTag tag, HolderLookup.Provider provider) {
        JobAssignmentData data = new JobAssignmentData();

        CompoundTag assignmentsTag = tag.getCompound("job_assignments");
        for (String jobId : assignmentsTag.getAllKeys()) {
            ListTag playerList = assignmentsTag.getList(jobId, Tag.TAG_STRING);
            Set<UUID> players = new HashSet<>();

            for (int i = 0; i < playerList.size(); i++) {
                try {
                    UUID playerId = UUID.fromString(playerList.getString(i));
                    players.add(playerId);
                } catch (IllegalArgumentException e) {
                    JobsPlus.LOGGER.warn("Invalid UUID in job assignments: " + playerList.getString(i));
                }
            }

            data.jobAssignments.put(jobId, players);
        }

        return data;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        CompoundTag assignmentsTag = new CompoundTag();

        for (Map.Entry<String, Set<UUID>> entry : jobAssignments.entrySet()) {
            ListTag playerList = new ListTag();
            for (UUID playerId : entry.getValue()) {
                playerList.add(StringTag.valueOf(playerId.toString()));
            }
            assignmentsTag.put(entry.getKey(), playerList);
        }

        tag.put("job_assignments", assignmentsTag);
        return tag;
    }

    /**
     * Add a player to a job
     */
    public void addPlayerToJob(UUID playerId, String jobId) {
        jobAssignments.computeIfAbsent(jobId, k -> new HashSet<>()).add(playerId);
        setDirty();
    }

    /**
     * Remove a player from a job
     */
    public void removePlayerFromJob(UUID playerId, String jobId) {
        Set<UUID> players = jobAssignments.get(jobId);
        if (players != null) {
            players.remove(playerId);
            if (players.isEmpty()) {
                jobAssignments.remove(jobId);
            }
            setDirty();
        }
    }

    /**
     * Get the number of players assigned to a specific job
     */
    public int getPlayerCount(String jobId) {
        Set<UUID> players = jobAssignments.get(jobId);
        return players != null ? players.size() : 0;
    }

    /**
     * Check if a player has a specific job
     */
    public boolean hasJob(UUID playerId, String jobId) {
        Set<UUID> players = jobAssignments.get(jobId);
        return players != null && players.contains(playerId);
    }

    /**
     * Get all players assigned to a specific job
     */
    public Set<UUID> getPlayersWithJob(String jobId) {
        Set<UUID> players = jobAssignments.get(jobId);
        return players != null ? new HashSet<>(players) : new HashSet<>();
    }

    /**
     * Get all job assignments
     */
    public Map<String, Integer> getAllJobCounts() {
        Map<String, Integer> counts = new HashMap<>();
        for (Map.Entry<String, Set<UUID>> entry : jobAssignments.entrySet()) {
            counts.put(entry.getKey(), entry.getValue().size());
        }
        return counts;
    }

    /**
     * Remove a player from all jobs (e.g., when they're deleted from the server)
     */
    public void removePlayerFromAllJobs(UUID playerId) {
        boolean modified = false;
        Iterator<Map.Entry<String, Set<UUID>>> iterator = jobAssignments.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, Set<UUID>> entry = iterator.next();
            if (entry.getValue().remove(playerId)) {
                modified = true;
                if (entry.getValue().isEmpty()) {
                    iterator.remove();
                }
            }
        }

        if (modified) {
            setDirty();
        }
    }
}

