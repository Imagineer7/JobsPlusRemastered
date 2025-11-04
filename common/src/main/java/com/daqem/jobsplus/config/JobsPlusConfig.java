package com.daqem.jobsplus.config;

import com.daqem.jobsplus.JobsPlus;
import com.daqem.yamlconfig.api.config.ConfigExtension;
import com.daqem.yamlconfig.api.config.ConfigType;
import com.daqem.yamlconfig.api.config.IConfigBuilder;
import com.daqem.yamlconfig.api.config.entry.IConfigEntry;
import com.daqem.yamlconfig.impl.config.ConfigBuilder;

public class JobsPlusConfig {

    public static final IConfigEntry<Boolean> enableDefaultJobs;
    public static final IConfigEntry<Integer> amountOfFreeJobs;
    public static final IConfigEntry<Integer> maxJobs;
    public static final IConfigEntry<Boolean> broadcastLevelUpMessages;
    public static final IConfigEntry<Integer> maxLevelToLeaveJob;
    public static final IConfigEntry<Boolean> resetStatsOnLeaveJob;

    // Job limitations system
    public static final IConfigEntry<Boolean> enableJobLimitations;
    public static final IConfigEntry<String> jobLimitationMode;
    public static final IConfigEntry<Integer> totalPlayerLimit;
    public static final IConfigEntry<java.util.List<String>> specificJobLimits;
    public static final IConfigEntry<java.util.List<String>> jobRatios;

    public static final IConfigEntry<Boolean> showXPInActionBar;
    public static final IConfigEntry<Boolean> showPaymentInActionBar;
    public static final IConfigEntry<Double> xpMultiplier;
    public static final IConfigEntry<Boolean> useDecimalValuesForXP;

    public static final IConfigEntry<Double> coinsPerLevelUp;
    public static final IConfigEntry<Double> actionCoinMultiplier;
    public static final IConfigEntry<Boolean> enableCommandRewards;
    public static final IConfigEntry<java.util.List<String>> levelUpCommands;

    // Action payment command system
    public static final IConfigEntry<Boolean> useActionPaymentCommand;
    public static final IConfigEntry<String> actionPaymentCommand;
    public static final IConfigEntry<Double> actionPaymentBaseAmount;
    public static final IConfigEntry<Double> jobMultiplierAlchemist;
    public static final IConfigEntry<Double> jobMultiplierBuilder;
    public static final IConfigEntry<Double> jobMultiplierDigger;
    public static final IConfigEntry<Double> jobMultiplierEnchanter;
    public static final IConfigEntry<Double> jobMultiplierFarmer;
    public static final IConfigEntry<Double> jobMultiplierFisherman;
    public static final IConfigEntry<Double> jobMultiplierHunter;
    public static final IConfigEntry<Double> jobMultiplierLumberjack;
    public static final IConfigEntry<Double> jobMultiplierMiner;
    public static final IConfigEntry<Double> jobMultiplierSmith;

    public static final IConfigEntry<Boolean> isDebug;

    static {
        IConfigBuilder config = new ConfigBuilder(JobsPlus.MOD_ID, "jobsplus-common", ConfigExtension.YAML, ConfigType.COMMON);

        config.push("jobs");
        enableDefaultJobs = config.defineBoolean("enable_default_jobs", true).withComments("if true, the default jobs are enabled. WARNING: setting this to false will erase all the stats for these jobs");
        amountOfFreeJobs = config.defineInteger("amount_of_free_jobs", 2, 0, Integer.MAX_VALUE).withComments("the amount of free jobs a player can have");
        maxJobs = config.defineInteger("max_jobs", Integer.MAX_VALUE, 0, Integer.MAX_VALUE).withComments("the maximum amount of jobs a player can have");
        broadcastLevelUpMessages = config.defineBoolean("broadcast_level_up_messages", true).withComments("if true, a message will be shown to all players when they level up a job");
        maxLevelToLeaveJob = config.defineInteger("max_level_to_leave_job", 5, 0, Integer.MAX_VALUE).withComments("the maximum level a player can be to leave a job (default: 5, set to 0 to disable leaving jobs, set to Integer.MAX_VALUE to allow leaving at any level)");
        resetStatsOnLeaveJob = config.defineBoolean("reset_stats_on_leave_job", true).withComments("if true, all stats and powerups will be reset when a player leaves a job");

        config.push("limitations");
        enableJobLimitations = config.defineBoolean("enable_job_limitations", false).withComments(
                "Enable job limitations to control how many players can have specific jobs",
                "This creates job scarcity and encourages economic interdependence"
        );
        jobLimitationMode = config.defineString("job_limitation_mode", "specific").withComments(
                "How to handle job limitations:",
                "- 'specific': Use specific limits per job defined in specific_job_limits",
                "- 'total_even': Divide total_player_limit evenly among all jobs",
                "- 'total_ratio': Distribute total_player_limit based on ratios defined in job_ratios"
        );
        totalPlayerLimit = config.defineInteger("total_player_limit", 100, 1, Integer.MAX_VALUE).withComments(
                "Total number of players that can have jobs when using 'total_even' or 'total_ratio' modes"
        );
        specificJobLimits = config.defineStringList("specific_job_limits", java.util.Arrays.asList(
                "jobsplus:miner:10",
                "jobsplus:farmer:8",
                "jobsplus:builder:5",
                "jobsplus:alchemist:3"
        )).withComments(
                "Specific player limits for each job when using 'specific' mode",
                "Format: 'job_id:limit'",
                "Example: 'jobsplus:miner:10' means only 10 players can be miners"
        );
        jobRatios = config.defineStringList("job_ratios", java.util.Arrays.asList(
                "jobsplus:miner:0.3",
                "jobsplus:farmer:0.25",
                "jobsplus:builder:0.2",
                "jobsplus:lumberjack:0.15",
                "jobsplus:alchemist:0.1"
        )).withComments(
                "Job ratios when using 'total_ratio' mode",
                "Format: 'job_id:ratio'",
                "Ratios should add up to 1.0 for optimal distribution",
                "Example: 'jobsplus:miner:0.3' means 30% of total_player_limit can be miners"
        );
        config.pop();

        config.push("experience");
        showXPInActionBar = config.defineBoolean("show_xp_in_action_bar", true).withComments("if true, the player's job XP gain will be shown in the action bar, when they gain XP");
        showPaymentInActionBar = config.defineBoolean("show_payment_in_action_bar", true).withComments("if true, the player's payment will be shown in the action bar when they receive payment for job actions");
        xpMultiplier = config.defineDouble("xp_multiplier", 1, 0, Double.MAX_VALUE).withComments("multiplier for the amount of job XP a player gains");
        useDecimalValuesForXP = config.defineBoolean("use_decimal_values_for_xp", false).withComments("if true, decimal values will be used for job XP.");
        config.pop();
        config.push("coins");
        coinsPerLevelUp = config.defineDouble("coins_per_level_up", 1, 0, Double.MAX_VALUE).withComments("the amount of coins a player gets when they level up a job");
        actionCoinMultiplier = config.defineDouble("action_coin_multiplier", 0, 0, Double.MAX_VALUE).withComments(
                "multiplier for coins earned from performing job actions",
                "this affects the 'job_action_coin' reward type in data files",
                "set to 0 to disable action-based coin rewards (default)",
                "set to 1.0 or higher to enable and scale action-based coin rewards"
        );
        config.pop();
        config.push("commands");
        enableCommandRewards = config.defineBoolean("enable_command_rewards", false).withComments("if true, custom commands will be executed when a player levels up a job");
        levelUpCommands = config.defineStringList("level_up_commands", java.util.Arrays.asList(
                "give {player} minecraft:diamond 1",
                "tellraw {player} {\"text\":\"Congratulations on reaching level {level} in {job}!\",\"color\":\"gold\"}"
        )).withComments(
                "Commands to execute when a player levels up a job.",
                "Available placeholders: {player} (player name), {job} (job name), {level} (new level), {coins} (coins awarded)",
                "Commands are executed by the server console, so no / prefix is needed"
        );
        config.push("action_payments");
        useActionPaymentCommand = config.defineBoolean("use_action_payment_command", false).withComments(
                "if true, use a custom command to pay players for job actions instead of the internal coin system",
                "this allows integration with economy mods like EconLib, PlayerPoints, etc."
        );
        actionPaymentCommand = config.defineString("action_payment_command", "econlib pay {player} {amount} money").withComments(
                "Command to execute when paying a player for a job action",
                "Available placeholders: {player} (player name), {amount} (payment amount with multipliers applied), {job} (job name)",
                "Example for EconLib: 'econlib pay {player} {amount} money'",
                "Example for PlayerPoints: 'points give {player} {amount}'",
                "Example for built-in coins (redundant): 'job addcoins {player} {amount}'"
        );
        actionPaymentBaseAmount = config.defineDouble("action_payment_base_amount", 1.0, 0, Double.MAX_VALUE).withComments(
                "Base payment amount for job actions when using custom payment commands",
                "This is multiplied by the action's configured amount and the job-specific multiplier",
                "Final amount = (action amount from data file) * (job multiplier) * (this base amount)"
        );
        config.push("job_multipliers");
        jobMultiplierAlchemist = config.defineDouble("alchemist", 1.0, 0, Double.MAX_VALUE).withComments("Payment multiplier for Alchemist job actions");
        jobMultiplierBuilder = config.defineDouble("builder", 1.0, 0, Double.MAX_VALUE).withComments("Payment multiplier for Builder job actions");
        jobMultiplierDigger = config.defineDouble("digger", 1.0, 0, Double.MAX_VALUE).withComments("Payment multiplier for Digger job actions");
        jobMultiplierEnchanter = config.defineDouble("enchanter", 1.0, 0, Double.MAX_VALUE).withComments("Payment multiplier for Enchanter job actions");
        jobMultiplierFarmer = config.defineDouble("farmer", 1.0, 0, Double.MAX_VALUE).withComments("Payment multiplier for Farmer job actions");
        jobMultiplierFisherman = config.defineDouble("fisherman", 1.0, 0, Double.MAX_VALUE).withComments("Payment multiplier for Fisherman job actions");
        jobMultiplierHunter = config.defineDouble("hunter", 1.0, 0, Double.MAX_VALUE).withComments("Payment multiplier for Hunter job actions");
        jobMultiplierLumberjack = config.defineDouble("lumberjack", 1.0, 0, Double.MAX_VALUE).withComments("Payment multiplier for Lumberjack job actions");
        jobMultiplierMiner = config.defineDouble("miner", 1.2, 0, Double.MAX_VALUE).withComments("Payment multiplier for Miner job actions");
        jobMultiplierSmith = config.defineDouble("smith", 1.0, 0, Double.MAX_VALUE).withComments("Payment multiplier for Smith job actions");
        config.pop();
        config.pop();
        config.pop();
        config.pop();

        config.push("debug");
        isDebug = config.defineBoolean("is_debug", false).withComments("if true, debug mode is enabled");
        config.pop();

        config.build();
    }

    public static void init() {
    }
}
