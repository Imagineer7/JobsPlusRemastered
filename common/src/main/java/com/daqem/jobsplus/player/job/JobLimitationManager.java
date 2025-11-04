package com.daqem.jobsplus.player.job;

import com.daqem.jobsplus.config.JobsPlusConfig;
import com.daqem.jobsplus.integration.arc.holder.holders.job.JobInstance;
import com.daqem.jobsplus.integration.arc.holder.holders.job.JobManager;
import com.daqem.jobsplus.player.JobsPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages job limitations and player counts for jobs
 */
public class JobLimitationManager {

    private static JobLimitationManager instance;

    public static JobLimitationManager getInstance() {
        if (instance == null) {
            instance = new JobLimitationManager();
        }
        return instance;
    }

    private JobLimitationManager() {}

    private boolean hasValidated = false;

    /**
     * Check if a player can join a specific job
     * @param jobInstance The job instance to check
     * @param server The minecraft server instance
     * @return true if the player can join, false if the job is full
     */
    public boolean canPlayerJoinJob(JobInstance jobInstance, MinecraftServer server) {
        if (!JobsPlusConfig.enableJobLimitations.get()) {
            return true;
        }

        // Validate configuration on first use
        if (!hasValidated) {
            validateConfiguration();
            hasValidated = true;
        }

        int currentCount = getCurrentJobPlayerCount(jobInstance, server);
        int limit = getJobPlayerLimit(jobInstance);

        return currentCount < limit;
    }

    /**
     * Get the current number of players with a specific job
     * @param jobInstance The job instance to count
     * @param server The minecraft server instance
     * @return Current number of players with this job
     */
    public int getCurrentJobPlayerCount(JobInstance jobInstance, MinecraftServer server) {
        int count = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player instanceof JobsPlayer jobsPlayer) {
                Job job = jobsPlayer.jobsplus$getJob(jobInstance);
                if (job != null && job.getLevel() > 0) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Get the player limit for a specific job
     * @param jobInstance The job instance
     * @return The maximum number of players that can have this job
     *
     * Note: All calculations use Math.ceil() to round UP to avoid fractional players.
     * This ensures each job gets at least 1 player slot when the total limit allows it.
     */
    public int getJobPlayerLimit(JobInstance jobInstance) {
        String mode = JobsPlusConfig.jobLimitationMode.get();
        String jobId = jobInstance.getLocation().toString();

        switch (mode.toLowerCase()) {
            case "specific":
                return getSpecificJobLimits().getOrDefault(jobId, Integer.MAX_VALUE);

            case "total_even":
                int totalJobs = JobManager.getInstance().getJobs().size();
                if (totalJobs == 0) return Integer.MAX_VALUE;
                // Use Math.ceil to round up so each job gets at least 1 player if total limit allows
                return (int) Math.ceil((double) JobsPlusConfig.totalPlayerLimit.get() / totalJobs);

            case "total_ratio":
                double ratio = getJobRatios().getOrDefault(jobId, 0.0);
                // Ensure we round up to avoid fractional players (already using Math.ceil)
                return (int) Math.ceil(JobsPlusConfig.totalPlayerLimit.get() * ratio);

            default:
                return Integer.MAX_VALUE;
        }
    }

    /**
     * Parse specific job limits from config string list
     * @return Map of job ID to player limit
     */
    private Map<String, Integer> getSpecificJobLimits() {
        Map<String, Integer> limits = new HashMap<>();
        for (String entry : JobsPlusConfig.specificJobLimits.get()) {
            String[] parts = entry.split(":");
            if (parts.length == 3) { // jobsplus:job:limit
                String jobId = parts[0] + ":" + parts[1];
                try {
                    int limit = Integer.parseInt(parts[2]);
                    limits.put(jobId, limit);
                } catch (NumberFormatException e) {
                    // Skip invalid entries
                }
            }
        }
        return limits;
    }

    /**
     * Parse job ratios from config string list
     * @return Map of job ID to ratio
     */
    private Map<String, Double> getJobRatios() {
        Map<String, Double> ratios = new HashMap<>();
        for (String entry : JobsPlusConfig.jobRatios.get()) {
            String[] parts = entry.split(":");
            if (parts.length == 3) { // jobsplus:job:ratio
                String jobId = parts[0] + ":" + parts[1];
                try {
                    double ratio = Double.parseDouble(parts[2]);
                    ratios.put(jobId, ratio);
                } catch (NumberFormatException e) {
                    // Skip invalid entries
                }
            }
        }
        return ratios;
    }

    /**
     * Get job statistics for all jobs
     * @param server The minecraft server instance
     * @return Map of job ID to current/max player counts
     */
    public Map<String, JobStats> getAllJobStats(MinecraftServer server) {
        Map<String, JobStats> stats = new HashMap<>();

        for (JobInstance jobInstance : JobManager.getInstance().getJobs().values()) {
            int current = getCurrentJobPlayerCount(jobInstance, server);
            int limit = getJobPlayerLimit(jobInstance);
            stats.put(jobInstance.getLocation().toString(), new JobStats(current, limit));
        }

        return stats;
    }

    /**
     * Validate job limitation configuration
     * @return true if configuration is valid, false otherwise
     */
    public boolean validateConfiguration() {
        if (!JobsPlusConfig.enableJobLimitations.get()) {
            return true; // No validation needed if disabled
        }

        String mode = JobsPlusConfig.jobLimitationMode.get();

        switch (mode.toLowerCase()) {
            case "total_ratio":
                // Check if ratios add up to reasonable amount and no negative values
                Map<String, Double> ratios = getJobRatios();
                double totalRatio = ratios.values().stream().mapToDouble(Double::doubleValue).sum();

                if (totalRatio > 1.5) {
                    // Warn if ratios exceed 150% - might be unintentional
                    System.out.println("WARNING: Job ratios sum to " + String.format("%.2f", totalRatio) +
                                     " (>150%). This may result in more job slots than intended.");
                }

                // Check for negative ratios
                for (Map.Entry<String, Double> entry : ratios.entrySet()) {
                    if (entry.getValue() < 0) {
                        System.err.println("ERROR: Job " + entry.getKey() + " has negative ratio: " + entry.getValue());
                        return false;
                    }
                }
                break;

            case "specific":
                // Check for negative limits
                Map<String, Integer> limits = getSpecificJobLimits();
                for (Map.Entry<String, Integer> entry : limits.entrySet()) {
                    if (entry.getValue() < 0) {
                        System.err.println("ERROR: Job " + entry.getKey() + " has negative limit: " + entry.getValue());
                        return false;
                    }
                }
                break;

            case "total_even":
                // Check if total limit is reasonable
                if (JobsPlusConfig.totalPlayerLimit.get() <= 0) {
                    System.err.println("ERROR: total_player_limit must be positive, got: " +
                                     JobsPlusConfig.totalPlayerLimit.get());
                    return false;
                }
                break;
        }

        return true;
    }

    /**
     * Data class to hold job statistics
     */
    public static class JobStats {
        private final int currentPlayers;
        private final int maxPlayers;

        public JobStats(int currentPlayers, int maxPlayers) {
            this.currentPlayers = currentPlayers;
            this.maxPlayers = maxPlayers;
        }

        public int getCurrentPlayers() {
            return currentPlayers;
        }

        public int getMaxPlayers() {
            return maxPlayers;
        }

        public boolean isFull() {
            return currentPlayers >= maxPlayers;
        }

        public double getFillPercentage() {
            if (maxPlayers == Integer.MAX_VALUE) return 0.0;
            return (double) currentPlayers / maxPlayers * 100.0;
        }
    }
}
