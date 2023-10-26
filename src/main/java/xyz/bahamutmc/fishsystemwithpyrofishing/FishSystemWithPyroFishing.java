package xyz.bahamutmc.fishsystemwithpyrofishing;

import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;
//import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
//import org.bukkit.inventory.ItemStack;
//import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
//import java.util.function.Supplier;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class FishSystemWithPyroFishing extends JavaPlugin implements Listener {
    //儲存玩家釣起生物
    public Map<Player, Entity> fishingLoot = new HashMap<>();
    //儲存玩家 是否正在遊玩 如果HashMap裡有玩家 = 正在遊戲中。 如果HashMap裡的值為ture = 正在遊玩小遊戲中。 如果為false=遊戲結束 當結束判斷跑完會清除玩家。
    public Map<Player, Boolean> fishGameSwitch = new HashMap<>();
    //儲存玩家的力度
    public Map<Player, Integer> fishingGamePower = new HashMap<>();
    //儲存玩家的動作 如果玩家有動作就不會減少力度條
    public Map<Player, Boolean> fishingGamePowerSwitch = new HashMap<>();
    //儲存玩家的進度
    public Map<Player, Integer> fishingGameProgress = new HashMap<>();
    //儲存玩家的目標位置
    public Map<Player, Integer> fishingGameTargetPosition = new HashMap<>();


    private FishingGame Fishgame;


    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("插件已啟動");
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        removeAllFishingPlayer();
    }


    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFishing(PlayerFishEvent event) {
        Player player = event.getPlayer();
        synchronized (player) {
            //如果玩家是正在遊玩釣魚小遊戲
            if (fishGameSwitch.containsKey(player)) {
                if (fishGameSwitch.get(player)) {
//                log("玩家 "+player.getName()+" 正在遊玩釣魚小遊戲 /" + event.getState() + " / " + fishingLoot.get(player));
                    if (event.getState() == PlayerFishEvent.State.BITE || event.getState() == PlayerFishEvent.State.FAILED_ATTEMPT) {
                        fishingGamePowerSwitch.put(player, false);
                        event.setCancelled(true);
                        //BITE 釣魚事件 直接取消 不進行後面
//                    log("BITE FAILED_ATTEMPT 釣魚事件 直接取消 不進行後面");
                        return;
                    }
                    //對於是否下降 設一個開關 有點擊就不降
                    fishingGamePowerSwitch.put(player, true);
                    event.setCancelled(true);
                } else if (!fishGameSwitch.get(player)) {
                    log(" " + player.getName() + " 釣魚遊戲已結束");
                    gameOver(player);
                }
                //後面的無須執行
                return;
            }
            //如果玩家沒在玩釣魚小遊戲 檢查 釣上魚就玩釣魚小遊戲
            if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
                Entity CAUGHT_FISH = event.getCaught();
                FishHook fishHook = event.getHook();
                EquipmentSlot hand = event.getHand();
                //這段會檢查玩家有沒有在玩釣魚小遊戲 沒有就先註冊 註冊玩家開關 註冊玩家進度 註冊玩家力度 註冊玩家力度開關
                if (!fishGameSwitch.containsKey(player)) {
                    log("註冊玩家 " + player.getName() + " 開始釣魚小遊戲");
                    fishGameSwitch.put(player, true);
                    fishingGameProgress.put(player, 0);
                    fishingGamePower.put(player, 15);
                    fishingGamePowerSwitch.put(player, false);
                    fishingLoot.put(player,CAUGHT_FISH);
                    event.setCancelled(true);
                }
                //第一次進入遊戲
                if (fishGameSwitch.get(player)) {
//                log("玩家 "+player.getName()+" 正在遊玩釣魚小遊戲");
                    Fishgame = new FishingGame(player, this, CAUGHT_FISH, fishHook, hand);
                    event.setCancelled(true);
                }
            }
        }
    }

    public void removeAllFishingPlayer() {
        if (!fishingLoot.isEmpty()) {
            fishingLoot.clear();
        }
        if (!fishGameSwitch.isEmpty()) {
            fishGameSwitch.clear();
        }
        if (!fishingGamePower.isEmpty()) {
            fishingGamePower.clear();
        }
        if (!fishingGamePowerSwitch.isEmpty()) {
            fishingGamePowerSwitch.clear();
        }
        if (!fishingGameProgress.isEmpty()) {
            fishingGameProgress.clear();
        }
    }

    void gameOver(Player player) {
        fishGameSwitch.remove(player);
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (Fishgame != null && fishGameSwitch.containsKey(player)) {
            fishGameSwitch.put(player, false);
            Fishgame.stopGame(player);
        }
    }

    @EventHandler
    public void onPlayLeft(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (Fishgame != null) {
            Fishgame.quitServer(player);
        }
    }

    //----測試用----
//    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
//    public void onFishingHIGHEST(PlayerFishEvent event) {
//        if (event.getState() == PlayerFishEvent.State.CAUGHT_ENTITY) {
//            Item Itemtest = (Item) event.getCaught();
//            ItemStack ItemStacktest = Itemtest.getItemStack();
//            ItemMeta test = ItemStacktest.getItemMeta();
//            log("145 / event.getCaught() : " + test.getAsString());
//        }
//    }

    //----下面是工具----


    // 只是簡化log用法
    void log(String s) {
        Logger logger = getLogger();
        logger.info(s);
    }
    /*  void log(Supplier<String> msgSupplier) {
        Logger logger = getLogger();
        logger.info(msgSupplier);
    }*/
}
