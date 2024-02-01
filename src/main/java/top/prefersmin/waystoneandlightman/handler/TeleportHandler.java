package top.prefersmin.waystoneandlightman.handler;

import com.mojang.logging.LogUtils;
import io.github.lightman314.lightmanscurrency.api.money.MoneyAPI;
import io.github.lightman314.lightmanscurrency.api.money.value.MoneyValue;
import io.github.lightman314.lightmanscurrency.api.money.value.builtin.CoinValue;
import net.blay09.mods.waystones.api.WaystoneTeleportEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import top.prefersmin.waystoneandlightman.WayStoneAndLightMan;
import top.prefersmin.waystoneandlightman.config.ModConfig;

/**
 * 监听传送事件
 *
 * @author PrefersMin
 * @version 1.0
 */
@Mod.EventBusSubscriber(modid = WayStoneAndLightMan.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TeleportHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * 监听方法
     *
     * @param event 传送事件
     */
    @SubscribeEvent
    public void onWayStoneTeleport(WaystoneTeleportEvent.Pre event) {
        Entity teleportedEntity = event.getContext().getEntity();
        if (teleportedEntity instanceof Player player) {

            // 判断是否处于创造模式
            if (player.getAbilities().instabuild) {
                return;
            }

            // 计算距离
            BlockPos pos = event.getContext().getTargetWaystone().getPos();
            double dist = Math.sqrt(player.distanceToSqr(pos.getX(), player.getY(), pos.getZ()));

            // 计算花费
            int moneyValue = (ModConfig.roundUp ? (int) Math.ceil(dist / (double) 100) : (int) Math.floor(dist / (double) 100)) * ModConfig.moneyCostPerHundredMeter;
            if (moneyValue == 0) {
                return;
            }

            // 转换为货币
            MoneyValue moneyCost = CoinValue.fromNumber("main", Math.min(Math.max(moneyValue, ModConfig.minimumCost), ModConfig.maximumCost));
            // 判断余额
            boolean canPlayerAfford = MoneyAPI.canPlayerAfford(player, moneyCost);

            // 执行消费或取消传送
            if (canPlayerAfford) {
                MoneyAPI.takeMoneyFromPlayer(player, moneyCost);
            } else {
                event.setCanceled(true);
            }

            // 判断是否强制使用中文
            String moneyCostString = getMoneyCostString(moneyCost);

            // 控制台输出
            if (ModConfig.enableConsoleLog) {
                LOGGER.info("--------------------------------------------");
                LOGGER.info(Component.translatable("gui.teleportLog", player.getName().getString(), (int) dist).getString());
                if (canPlayerAfford) {
                    LOGGER.info(Component.translatable("gui.teleportCost", moneyCostString).getString());
                } else {
                    LOGGER.info(Component.translatable("gui.notSufficientFundsLog", moneyCostString).getString());
                }
                LOGGER.info("--------------------------------------------");
            }


            // 局内提示
            if (ModConfig.enableCostTip) {
                Component message;
                if (canPlayerAfford) {
                    message = Component.translatable("gui.alertMoneyCost", moneyCostString);
                } else {
                    message = Component.translatable("gui.notSufficientFunds");
                }
                player.displayClientMessage(message, true);
            }

        }
    }

    @NotNull
    private static String getMoneyCostString(MoneyValue moneyCost) {
        String moneyCostString = moneyCost.getString();
        if (ModConfig.forceEnableChineseLanguage) {
            moneyCostString = moneyCostString.replace("c", "铜币 ");
            moneyCostString = moneyCostString.replace("i", "铁币 ");
            moneyCostString = moneyCostString.replace("g", "金币 ");
            moneyCostString = moneyCostString.replace("e", "绿宝石币 ");
            moneyCostString = moneyCostString.replace("d", "钻石币 ");
            moneyCostString = moneyCostString.replace("n", "下界合金币 ");
        }
        return moneyCostString;
    }

}
