package xyz.bahamutmc.fishsystemwithpyrofishing;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.EquipmentSlot;

import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Random;

public class FishingGame {
    static boolean waitSwitch = true;
    //新的目標位置 用於動畫的變數
    private static int NewPosition = 10;

    //勝利條件
    private static final double WinProgress = 40;
    //失敗條件
    private static final double LoseProgress = -8;

    //目標寬度
    private static int targetWeight = 9;
    //新的目標寬度 用於動畫的變數
    private static int NewWeight = 9;
    //標題寬度
    private static final int titleWeight = 21;

    //    Logger log = getLogger();
    static Random random = new Random();

    Player player;
    FishSystemWithPyroFishing plugin;
    Entity CAUGHT_FISH;
    FishHook hookEntity;
    EquipmentSlot hand;
    HashMap<Player, GameRunnable> playerGame = new HashMap<>();
    HashMap<Player, BossBar> playerBossBar = new HashMap<>();


    public FishingGame(Player player, FishSystemWithPyroFishing plugin,Entity CAUGHT_FISH,FishHook hookEntity,EquipmentSlot hand) {
        this.player = player;
        this.plugin = plugin;
        this.CAUGHT_FISH=CAUGHT_FISH;
        this.hookEntity = hookEntity;
        this.hand = hand;
        startGame();
    }


    void startGame() {
        enterFishingState(player);
        //開始的大字報
        player.sendTitle("遊戲開始", null, 10, 70, 20);
        //目標位置
        int targetPosition = 10;
        plugin.fishingGameTargetPosition.put(player, targetPosition);
        plugin.fishingGamePower.put(player, targetPosition + ((targetWeight - 3) / 2));
        GameRunnable Game = new GameRunnable();
        Game.GameWinTask = createGameWinTask(player);
        Game.GamePlayTask = createGamePlayTask(player);
        Game.GamePowerControl = createGamePowerControl(player);
        Game.GameShowBossBar = createGameShowBossBar(player);
        Game.GameBossBarMovement = createGameBossBarMovement(player);
        Game.GameBossBarRandom = createGameBossBarRandom(player);
        playerGame.put(player,Game);
        // 開始遊戲，每隔0.1秒执行一次輸贏判斷
        Game.GameWinTask.runTaskTimer(plugin, 0L, 2L);
        // 開始遊戲，每隔0.2秒執行一次目標位置判斷
        Game.GamePlayTask.runTaskTimerAsynchronously(plugin, 0L, 4L);
        // 開始遊戲，每個0.2秒下降一點力度
        Game.GamePowerControl.runTaskTimerAsynchronously(plugin, 0L, 4L);
        // 顯示遊戲的進度條的GUI 0.05秒一次
        Game.GameShowBossBar.runTaskTimerAsynchronously(plugin, 0L, 1L);
        // 每0.2秒去移動一次BossBar
        Game.GameBossBarMovement.runTaskTimerAsynchronously(plugin, 0L, 4L);
        // 每0.3秒去偵測是否移動到定位 並隨機進行
        Game.GameBossBarRandom.runTaskTimerAsynchronously(plugin, 4L, 6L);

    }
    // 創建與該玩家相關的 GameWinTask
    private BukkitRunnable createGameWinTask(Player player) {
        return new BukkitRunnable() {
            @Override
            public void run() {
                // 遊戲勝利邏輯...
                //log.info("Progress : " + system.fishingGameProgress.get(player));
                //遊戲勝利
                if (plugin.fishingGameProgress.get(player) >= WinProgress) {
                    plugin.fishGameSwitch.put(player, false);
                    Bukkit.getServer().getPluginManager().callEvent(new PlayerFishEvent(player, CAUGHT_FISH, hookEntity, hand, PlayerFishEvent.State.CAUGHT_FISH));
                    stopGame(player);
                    plugin.gameOver(player);
                }
                //遊戲失敗
                else if (plugin.fishingGameProgress.get(player) <= LoseProgress) {
                    plugin.fishGameSwitch.put(player, false);
                    stopGame(player);
                    plugin.gameOver(player);
                }
            }
        };
    }

    // 創建與該玩家相關的 GamePlayTask
    private BukkitRunnable createGamePlayTask(Player player) {
        BossBar bossBar =playerBossBar.get(player);
        return new BukkitRunnable() {
            @Override
            public void run() {
                int PlayerTargetPosition = plugin.fishingGameTargetPosition.get(player);
                // 遊戲主要程式邏輯...
                // 遊戲主要程式 判斷是否在目標寬度內
                int playerFishGamePower;
                if (plugin.fishingGamePower.containsKey(player)) {
                    playerFishGamePower = plugin.fishingGamePower.get(player);
                } else {
                    return;
                }
                int nowMaxTargetPos = PlayerTargetPosition + targetWeight;
                int nowMinTargetPos = PlayerTargetPosition;
                //如果力度在目標範圍內 進度上升
                if (nowMaxTargetPos > playerFishGamePower && playerFishGamePower > nowMinTargetPos) {
                    //如果在目標範圍內 bossBar 呈現綠色
                    bossBar.setColor(BarColor.GREEN);
                    int progress = plugin.fishingGameProgress.get(player);
                    progress++;
                    plugin.fishingGameProgress.put(player, progress);
                }
                //如果力度不在目標範圍內 進度下降
                else {
                    //如果在不目標範圍內 bossBar 呈現紅色
                    bossBar.setColor(BarColor.RED);
                    int progress = plugin.fishingGameProgress.get(player);
                    progress--;
                    plugin.fishingGameProgress.put(player, progress);
                }
            }
        };
    }

    // 創建與該玩家相關的 GamePowerControl
    private BukkitRunnable createGamePowerControl(Player player) {
        return new BukkitRunnable() {
            @Override
            public void run() {
                // 力度控制邏輯...
                // 玩家的力度條增減控制
                int playerFishGamePower;
                if (plugin.fishingGamePower.containsKey(player)) {
                    playerFishGamePower = plugin.fishingGamePower.get(player);
                } else {
                    return;
                }
                Boolean playerFishGamePowerSwitch = plugin.fishingGamePowerSwitch.get(player);
                if (playerFishGamePowerSwitch) {
                    playerFishGamePower++;
                    if (playerFishGamePower > titleWeight) {
                        playerFishGamePower = titleWeight;
                    }
                    plugin.fishingGamePower.put(player, playerFishGamePower);
                    plugin.fishingGamePowerSwitch.put(player, false);
                } else {
                    playerFishGamePower--;
                    if (playerFishGamePower < 0) {
                        playerFishGamePower = 0;
                    }
                    plugin.fishingGamePower.put(player, playerFishGamePower);

                }
            }
        };
    }

    // 創建與該玩家相關的 GameShowBossBar
    private BukkitRunnable createGameShowBossBar(Player player) {
        BossBar bossBar=playerBossBar.get(player);
        return new BukkitRunnable() {
            @Override
            public void run() {
                int PlayerTargetPosition = plugin.fishingGameTargetPosition.get(player);
                // BossBar 顯示邏輯...
                //力度條的顯示用GUI
                String space = "─";
                //最後輸出的BossBar數值
                String BossBarTitle;
                String BossBarTitleLeft;
                String BossBarTitleRight;

                //目標的數值 double型別
                if (!plugin.fishingGameProgress.containsKey(player)) {
                    cancel();
                    return;
                }
                double ProgressValueDouble = ProgressValueConvert(plugin.fishingGameProgress.get(player)) / WinProgress;
                double Double = ProgressValueDouble * 100;
                //轉換成 int無條件捨去小數
                int ProgressValueInt = (int) Double;

                //力度
                int playerFishGamePower = plugin.fishingGamePower.get(player);
                //進度
                String ProgressValue = ProgressValueInt + "%";

                int w = ((targetWeight - 3) / 2) - 1;
                String ProgressTargetRange = "┌" + "─".repeat(w) + ProgressValue + "─".repeat(w) + "┐";
                BossBarTitleLeft = space.repeat(PlayerTargetPosition);
                BossBarTitleRight = space.repeat(titleWeight - (PlayerTargetPosition - 1) - targetWeight);
                BossBarTitle = BossBarTitleLeft + ProgressTargetRange + BossBarTitleRight;
                bossBar.setTitle(BossBarTitle);
                bossBar.setProgress((double) playerFishGamePower / titleWeight);
                bossBar.getPlayers();
            }
        };
    }

    // 創建與該玩家相關的 GameBossBarMovement
    private BukkitRunnable createGameBossBarMovement(Player player) {

        return new BukkitRunnable() {
            @Override
            public void run() {
                int PlayerTargetPosition = plugin.fishingGameTargetPosition.get(player);
                // BossBar 移動邏輯...
                // 控制BossBar移動的副程式 0.2秒動一次
                if (targetWeight > NewWeight) {
                    targetWeight -= 2;
                } else if (targetWeight < NewWeight) {
                    targetWeight += 2;
                } else {
                    if (PlayerTargetPosition < NewPosition) {
                        PlayerTargetPosition++;
                    } else if (PlayerTargetPosition > NewPosition) {
                        PlayerTargetPosition--;
                    }
                }
                plugin.fishingGameTargetPosition.put(player,PlayerTargetPosition);
            }
        };
    }

    // 創建與該玩家相關的 GameBossBarRandom
    private BukkitRunnable createGameBossBarRandom(Player player) {
        return new BukkitRunnable() {
            @Override
            public void run() {
                int PlayerTargetPosition = plugin.fishingGameTargetPosition.get(player);
                // 隨機邏輯...
                // 每0.3秒隨機一次是否要更動 目標位置 目標寬度 (移動速度?)
                if (NewPosition == PlayerTargetPosition && NewWeight == targetWeight) {
                    //隨機從 5 7 9 11 挑一個數字 成為新寬度
                    int[] WeightIndex = {7, 9, 11};
                    //
                    NewWeight = WeightIndex[random.nextInt(WeightIndex.length)];
                    if (NewWeight + PlayerTargetPosition > titleWeight) {
                        NewWeight = targetWeight;
                    }
                    //隨機一個新的目標位置 由最大Title寬度減去目標寬度得出
                    int NewMaxPos = titleWeight - NewWeight;
//                        log.info("NewMaxPos "+NewMaxPos);
                    NewPosition = random.nextInt(NewMaxPos);
//                        log.info("NewWeight "+NewWeight+"/NewPosition "+NewPosition);
                    waitSwitch = true;

                }
            }
        };
    }

    public void enterFishingState(Player player) {
        BossBar bossBar =Bukkit.createBossBar("──────┌──00%──┐──────", BarColor.GREEN, BarStyle.SOLID);
        bossBar.setProgress(0.5d);//§
        bossBar.addPlayer(player);
        playerBossBar.put(player,bossBar);
    }

    public void stopGame(Player player) {
        GameRunnable Game = playerGame.get(player);
        BossBar bossBar = playerBossBar.get(player);
        if (Game != null) {
            if (Game.GamePlayTask != null) {
                Game.GamePlayTask.cancel();
            }
            if (Game.GamePowerControl != null) {
                Game.GamePowerControl.cancel();
            }
            if (Game.GameShowBossBar != null) {
                Game.GameShowBossBar.cancel();
            }
            if (Game.GameBossBarMovement != null) {
                Game.GameBossBarMovement.cancel();
            }
            if (Game.GameBossBarRandom != null) {
                Game.GameBossBarRandom.cancel();
            }
            if (Game.GameWinTask != null) {
                Game.GameWinTask.cancel();
            }
        }
        if (bossBar != null) {
            bossBar.removePlayer(player);
        }
        plugin.fishingGamePower.remove(player);
        plugin.fishGameSwitch.remove(player);
        plugin.fishingGameProgress.remove(player);
        plugin.fishingGamePowerSwitch.remove(player);
        plugin.fishingGameTargetPosition.remove(player);
    }
    public void quitServer(Player player) {
        GameRunnable Game = playerGame.get(player);
        BossBar bossBar = playerBossBar.get(player);
        if (Game.GameWinTask != null) {
            Game.GameWinTask.cancel();
        }
        if (Game.GamePlayTask != null) {
            Game.GamePlayTask.cancel();
        }
        if (Game.GamePowerControl != null) {
            Game.GamePowerControl.cancel();
        }
        if (Game.GameShowBossBar != null) {
            Game.GameShowBossBar.cancel();
        }
        if (Game.GameBossBarMovement != null) {
            Game.GameBossBarMovement.cancel();
        }
        if (Game.GameBossBarRandom != null) {
            Game.GameBossBarRandom.cancel();
        }
        if (bossBar != null) {
            bossBar.removePlayer(player);
        }
        plugin.fishingGamePower.remove(player);
        plugin.fishGameSwitch.remove(player);
        plugin.fishingGameProgress.remove(player);
        plugin.fishingGamePowerSwitch.remove(player);
        plugin.fishingGameTargetPosition.remove(player);
    }

    private static int ProgressValueConvert(int number) {
        if (number > WinProgress) {
            return (int) WinProgress;
        }
        return Math.max(number, 0);
    }
}
