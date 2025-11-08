package com.daqem.jobsplus.client.components.jobs;

import com.daqem.jobsplus.JobsPlus;
import com.daqem.jobsplus.client.components.JobsButtonComponent;
import com.daqem.jobsplus.client.JobStatsClientCache;
import com.daqem.jobsplus.client.options.JobsScreenOptions;
import com.daqem.jobsplus.config.JobsPlusConfig;
import com.daqem.jobsplus.networking.c2s.ServerboundLeaveJobPacket;
import com.daqem.jobsplus.player.job.Job;
import com.daqem.jobsplus.player.job.JobLimitationManager;
import com.daqem.uilib.client.gui.component.AbstractComponent;
import com.daqem.uilib.client.gui.component.SolidColorComponent;
import com.daqem.uilib.client.gui.component.TextComponent;
import com.daqem.uilib.client.gui.text.Text;
import com.daqem.uilib.client.gui.text.multiline.MultiLineText;
import dev.architectury.networking.NetworkManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Objects;

public class JobInfoComponent extends AbstractComponent<JobInfoComponent> {

    private final JobsScreenOptions options;
    private final TextComponent title;
    private final TextComponent level;
    private final TextComponent experience;
    private final TextComponent description;
    private final TextComponent wantJob;
    private final TextComponent playerCount;
    private final JobsButtonComponent leaveJobButton;

    private Job cachedJob;
    private int cachedCurrentPlayers = -1;
    private int cachedMaxPlayers = -1;

    public JobInfoComponent(int x, int y, int width, int height, JobsScreenOptions options) {
        super(null, x, y, width, height);
        this.options = options;
        this.cachedJob = getJob();

        Font font = Minecraft.getInstance().font;
        Text titleText = new Text(font, getTitleText(), 4, 0);
        Text levelText = new Text(font, getLevelText(), 8, 18);
        Text experienceText = new Text(font, getExperienceText(), 70, 18);
        MultiLineText descriptionText = new MultiLineText(font, getDescriptionText(), 8, 32, width - 16);
        Text wantJobText = new Text(font, getWantJobText(), 8, 18);
        Text playerCountText = new Text(font, getPlayerCountText(), 8, height - 44);

        this.title = new TextComponent(titleText);
        this.level = new TextComponent(levelText);
        this.experience = new TextComponent(experienceText);
        this.description = new TextComponent(descriptionText);
        this.wantJob = new TextComponent(wantJobText);
        this.playerCount = new TextComponent(playerCountText);

        this.leaveJobButton = new JobsButtonComponent(
                8, height - 24,
                width - 16, 20,
                JobsPlus.translatable("gui.job.leave"),
                (clickedObject, screen, mouseX, mouseY, button) -> {
                    NetworkManager.sendToServer(new ServerboundLeaveJobPacket(getJob().getJobInstance().getLocation()));
                    return true;
                }
        );

        titleText.setBold(true);
        titleText.setTextColor(getJob().getJobInstance().getColorDecimal());
        levelText.setTextColor(ChatFormatting.DARK_GRAY);
        experienceText.setTextColor(ChatFormatting.DARK_GRAY);
        descriptionText.setTextColor(ChatFormatting.DARK_GRAY);
        wantJobText.setTextColor(ChatFormatting.DARK_GRAY);
        playerCountText.setTextColor(ChatFormatting.GOLD);
        this.title.setScale(2F);

        this.addChild(title);
        this.addChild(level);
        this.addChild(experience);
        this.addChild(new SolidColorComponent(7, 27, width - 14, 1, 0xFFFFFFFF));
        this.addChild(description);
        this.addChild(wantJob);
        this.addChild(playerCount);
        this.addChild(leaveJobButton);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (cachedJob != options.getSelectedJob()) {
            Objects.requireNonNull(title.getText()).setText(getTitleText());
            title.getText().setTextColor(getJob().getJobInstance().getColorDecimal());
            Objects.requireNonNull(level.getText()).setText(getLevelText());
            Objects.requireNonNull(experience.getText()).setText(getExperienceText());
            Objects.requireNonNull(description.getText()).setText(getDescriptionText());
            Objects.requireNonNull(wantJob.getText()).setText(getWantJobText());
            Objects.requireNonNull(playerCount.getText()).setText(getPlayerCountText());
            cachedJob = getJob();

            // Reset cached stats when job changes
            cachedCurrentPlayers = -1;
            cachedMaxPlayers = -1;
        }

        // Check if job stats have changed and update player count immediately
        if (JobsPlusConfig.enableJobLimitations.get()) {
            String jobId = getJob().getJobInstance().getLocation().toString();
            var jobStats = JobStatsClientCache.getJobStats(jobId);

            if (jobStats != null) {
                if (cachedCurrentPlayers != jobStats.currentPlayers || cachedMaxPlayers != jobStats.maxPlayers) {
                    Objects.requireNonNull(playerCount.getText()).setText(getPlayerCountText());
                    cachedCurrentPlayers = jobStats.currentPlayers;
                    cachedMaxPlayers = jobStats.maxPlayers;
                }
            }
        }

        // Show leave job button only if:
        // 1. Player has the job (level > 0)
        // 2. Leaving is enabled (maxLevelToLeaveJob > 0)
        // 3. Player's level is within the allowed range
        int maxLevelToLeave = JobsPlusConfig.maxLevelToLeaveJob.get();
        boolean canLeaveJob = getJob().getLevel() > 0 &&
                             maxLevelToLeave > 0 &&
                             getJob().getLevel() <= maxLevelToLeave;
        leaveJobButton.setVisible(canLeaveJob);
    }

    private Component getTitleText() {
        return options.getSelectedJob().getJobInstance().getName();
    }

    private Component getLevelText() {
        if (getJob().getLevel() <= 0) return JobsPlus.literal("");
        return JobsPlus.translatable("gui.level", JobsPlus.literal(String.valueOf(getJob().getLevel())).withStyle(ChatFormatting.WHITE));
    }

    private Component getExperienceText() {
        if (getJob().getLevel() <= 0) return JobsPlus.literal("");
        return JobsPlus.translatable("gui.exp", JobsPlus.literal(JobsPlus.formatNumber(getJob().getExperience()) + "/" + Job.getExperienceToLevelUp(getJob().getLevel())).withStyle(ChatFormatting.WHITE));
    }

    private Component getDescriptionText() {
        return getJob().getJobInstance().getDescription();
    }

    private Component getWantJobText() {
        if (getJob().getLevel() > 0) return JobsPlus.literal("");
        return JobsPlus.translatable("gui.want_this_job.price", JobsPlus.literal(String.valueOf(getJob().getJobInstance().getPrice())).withStyle(ChatFormatting.WHITE), JobsPlus.translatable("gui.price.coins").withStyle(ChatFormatting.WHITE));
    }

    private Component getPlayerCountText() {
        if (!JobsPlusConfig.enableJobLimitations.get()) {
            return JobsPlus.literal("");
        }

        String jobId = getJob().getJobInstance().getLocation().toString();
        var jobStats = JobStatsClientCache.getJobStats(jobId);

        if (jobStats == null) {
            return JobsPlus.literal("");
        }

        if (jobStats.maxPlayers == Integer.MAX_VALUE) {
            return JobsPlus.literal("Players: " + jobStats.currentPlayers).withStyle(ChatFormatting.DARK_GRAY);
        } else {
            ChatFormatting color = jobStats.isFull() ? ChatFormatting.DARK_RED : ChatFormatting.DARK_GREEN;
            String status = jobStats.isFull() ? " [FULL]" : "";
            return JobsPlus.literal("Players: " + jobStats.currentPlayers + "/" + jobStats.maxPlayers + status)
                    .withStyle(color);
        }
    }

    private Job getJob() {
        return options.getSelectedJob();
    }

    @Override
    public boolean preformOnClickEvent(double mouseX, double mouseY, int button) {
        // Don't process clicks when this component is not visible (e.g., when on a different tab)
        if (!isVisible()) return false;
        return super.preformOnClickEvent(mouseX, mouseY, button);
    }
}
