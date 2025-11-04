package com.daqem.jobsplus.config;

import net.minecraft.resources.ResourceLocation;

/**
 * Helper class for retrieving job-specific payment multipliers from config.
 */
public class JobPaymentHelper {

    /**
     * Get the payment multiplier for a specific job.
     *
     * @param jobLocation The ResourceLocation of the job
     * @return The multiplier from config, or 1.0 if job not recognized
     */
    public static double getJobMultiplier(ResourceLocation jobLocation) {
        if (jobLocation == null) return 1.0;

        String jobPath = jobLocation.getPath();

        return switch (jobPath) {
            case "alchemist" -> JobsPlusConfig.jobMultiplierAlchemist.get();
            case "builder" -> JobsPlusConfig.jobMultiplierBuilder.get();
            case "digger" -> JobsPlusConfig.jobMultiplierDigger.get();
            case "enchanter" -> JobsPlusConfig.jobMultiplierEnchanter.get();
            case "farmer" -> JobsPlusConfig.jobMultiplierFarmer.get();
            case "fisherman" -> JobsPlusConfig.jobMultiplierFisherman.get();
            case "hunter" -> JobsPlusConfig.jobMultiplierHunter.get();
            case "lumberjack" -> JobsPlusConfig.jobMultiplierLumberjack.get();
            case "miner" -> JobsPlusConfig.jobMultiplierMiner.get();
            case "smith" -> JobsPlusConfig.jobMultiplierSmith.get();
            default -> 1.0; // Default multiplier for custom jobs
        };
    }

    /**
     * Get the formatted job name from ResourceLocation.
     *
     * @param jobLocation The ResourceLocation of the job
     * @return The job name (path part)
     */
    public static String getJobName(ResourceLocation jobLocation) {
        if (jobLocation == null) return "unknown";
        return jobLocation.getPath();
    }

    /**
     * Calculate the final payment amount with all multipliers applied.
     *
     * @param baseAmount The base amount from the reward data
     * @param jobLocation The job's ResourceLocation
     * @return The final payment amount
     */
    public static double calculatePaymentAmount(double baseAmount, ResourceLocation jobLocation) {
        double jobMultiplier = getJobMultiplier(jobLocation);
        double baseMultiplier = JobsPlusConfig.actionPaymentBaseAmount.get();
        return baseAmount * jobMultiplier * baseMultiplier;
    }
}

