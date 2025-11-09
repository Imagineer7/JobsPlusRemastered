## [View changes here](https://github.com/DAQEM/JobsPlusRemastered/releases)

[![BisectHosting code DAQEM for 25% off!](https://www.bisecthosting.com/partners/custom-banners/bb6b0cc7-75a1-4002-9257-561d8df48142.webp)](https://bisecthosting.com/DAQEM?r=JobsPlusRemastered+Changelog)

### [I trust BisectHosting with my servers, and you should too! Get 25% off your first month of a gaming server for new customers using DAQEM. They have outstanding support that's always there when you need it.](https://bisecthosting.com/DAQEM?r=JobsPlusRemastered+Changelog)

## [Unreleased]

### Fixed
- **Jobs GUI Tab Click-Through**: Fixed issue where the leave job button could be accidentally triggered when clicking in other tabs
  - The button is now properly disabled when viewing tabs other than the Info tab
  - Prevents accidental job leaving when interacting with Crafting, Power-ups, or Experience tabs
- **Job Slots Label Updates**: Fixed job player count label not updating immediately when players join or leave jobs
  - The label now updates in real-time as job statistics change
  - Improved color visibility on light backgrounds (changed from YELLOW/GREEN/RED to DARK_GRAY/DARK_GREEN/DARK_RED)
  - Player counts are now easier to read at a glance
- **Job Limitation Player Counting**: Fixed job slot counts only showing online players instead of all players with jobs
  - Implemented persistent storage system (`JobAssignmentData`) that tracks all job assignments across server restarts
  - Job slot counts now accurately reflect total players with each job, regardless of online status
  - Data is automatically synced when players log in and when they start/leave jobs
  - Stored in world save data at `world/data/jobsplus_job_assignments.dat`

### Added
- **Job Limitations System**: Server owners can now limit the number of players who can have specific jobs
  - Creates job scarcity to encourage economic interdependence between players
  - New config option: `enable_job_limitations` to toggle the feature
  - Three limitation modes:
    - `specific`: Set individual limits per job via `specific_job_limits`
    - `total_even`: Distribute `total_player_limit` evenly across all jobs (rounded up)
    - `total_ratio`: Distribute `total_player_limit` based on ratios defined in `job_ratios` (rounded up)
  - **Smart Rounding**: All player limits are automatically rounded UP to prevent fractional players
  - **Configuration Validation**: Automatic validation prevents negative limits and warns about unusual ratio totals
  - Players see current/max player counts in the job selection UI
  - Jobs show [FULL] status when at capacity
  - New command: `/job stats` to view current job statistics (admin only)
  - Full integration with existing job assignment and removal systems

- **Custom Command Rewards System**: Server owners can now execute custom commands when players level up jobs
  - New config option: `enable_command_rewards` to toggle the feature
  - New config option: `level_up_commands` to define commands to execute
  - Supports placeholders: `{player}`, `{job}`, `{level}`, `{coins}`
  - Commands execute as server console for maximum flexibility
  - Works alongside or instead of the existing coin reward system
  - See `COMMAND_REWARDS.md` for full documentation

- **Action-Based Coin Rewards**: Players can now earn coins for individual job actions, not just leveling up
  - **DISABLED BY DEFAULT** - Must be enabled by setting `action_coin_multiplier` > 0 or `use_action_payment_command: true`
  - New reward type: `jobsplus:job_action_coin` for use in ARC data files
  - New config option: `action_coin_multiplier` to globally scale action-based coin rewards (default: 0)
  - Supports random ranges (min/max) for coin amounts
  - Works alongside level-up coin rewards for hybrid reward systems
  - See `ACTION_BASED_REWARDS.md` for full documentation and examples

- **Custom Payment Commands for Actions**: Integrate with any economy mod for action-based payments
  - **DISABLED BY DEFAULT** - Must be enabled by setting `use_action_payment_command: true`
  - New config option: `use_action_payment_command` to enable custom payment commands (default: false)
  - New config option: `action_payment_command` to define payment command template
  - New config option: `action_payment_base_amount` for global payment scaling
  - Per-job payment multipliers: Configure different payment rates for each job
    - Example: Miners earn 1.2x, Farmers earn 0.8x
  - Supports placeholders: `{player}`, `{amount}`, `{job}`
  - Compatible with EconLib, Vault, PlayerPoints, and any economy mod
  - Can run alongside internal coin system or replace it
  - See `CUSTOM_PAYMENT_COMMANDS.md` for full documentation

- **Payment Feedback System**: Visual confirmation when players receive payment for job actions
  - New config option: `show_payment_in_action_bar` to display payment in action bar (default: true)
  - Shows "+{amount}$" above hotbar when players receive payment (similar to XP display)
  - Success messages appear in green, bold text in action bar
  - Error messages appear in chat if payment command fails
  - Detailed error information shown to help troubleshoot issues
  - Works with both internal coins and custom payment commands
  - See `PAYMENT_FEEDBACK.md` for full documentation
- Added `showPaymentInActionBar` config option for payment feedback
- Implemented `sendPaymentFeedback()` and `sendPaymentError()` methods in `JobActionCoinReward`
- Enhanced command execution to return success/failure status
- Added translation keys for payment feedback messages
### Technical
- Added `executeRewardCommands()` method in `JobEvents` class
- Extended `JobsPlusConfig` with command reward configuration options
- Created `JobActionCoinReward` reward type with serialization support
- Registered `JOB_ACTION_COIN` reward type in `JobsPlusRewardType`
- Added `actionCoinMultiplier` configuration entry
- Created `JobPaymentHelper` utility class for job multiplier management
- Extended `JobActionCoinReward` to support custom payment commands
- Added per-job multiplier config entries for all default jobs
