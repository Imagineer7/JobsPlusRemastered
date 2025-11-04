package com.daqem.jobsplus.client;

import com.daqem.jobsplus.networking.s2c.ClientboundJobStatsPacket;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-side cache for job statistics received from the server
 */
public class JobStatsClientCache {

    private static Map<String, ClientboundJobStatsPacket.JobStatInfo> jobStats = new HashMap<>();

    public static void setJobStats(Map<String, ClientboundJobStatsPacket.JobStatInfo> stats) {
        jobStats = new HashMap<>(stats);
    }

    public static ClientboundJobStatsPacket.JobStatInfo getJobStats(String jobId) {
        return jobStats.get(jobId);
    }

    public static Map<String, ClientboundJobStatsPacket.JobStatInfo> getAllJobStats() {
        return new HashMap<>(jobStats);
    }

    public static boolean hasStats() {
        return !jobStats.isEmpty();
    }

    public static void clear() {
        jobStats.clear();
    }
}
