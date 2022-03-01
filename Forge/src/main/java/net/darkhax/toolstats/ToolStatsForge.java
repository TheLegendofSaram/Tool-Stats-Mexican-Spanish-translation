package net.darkhax.toolstats;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.TierSortingRegistry;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.forgespi.Environment;
import net.minecraftforge.network.NetworkConstants;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;

@Mod(Constants.MOD_ID)
public class ToolStatsForge {

    private static final Map<Tier, Integer> tierCache = new WeakHashMap<>();
    private static final Map<Integer, Tier> vanillaTierLevels = Map.of(0, Tiers.WOOD, 1, Tiers.STONE, 2, Tiers.IRON, 3, Tiers.DIAMOND, 4, Tiers.NETHERITE);

    public static int getTierLevel(Tier tier) {

        if (!tierCache.containsKey(tier)) {

            // If tier is sorted but has not been cached revalidate the cache.
            if (TierSortingRegistry.isTierSorted(tier)) {

                tierCache.clear();

                int tierLevel = 0;

                for (Tier currentTier : TierSortingRegistry.getSortedTiers()) {

                    final ResourceLocation id = TierSortingRegistry.getName(currentTier);
                    final boolean isVanilla = id != null && "minecraft".equals(id.getNamespace());

                    // Tier is not the same as the previous one.
                    if ((isVanilla || (currentTier.getTag() != null && !currentTier.getTag().getValues().isEmpty()))) {

                        tierLevel++;
                    }

                    tierCache.put(currentTier, tierLevel);
                }
            }

            // Unregistered tiers get matched with their vanilla counterparts.
            else if (vanillaTierLevels.containsKey(tier.getLevel())) {

                // Remap from vanilla tier levels to Forge's sorted tier levels.
                tierCache.put(tier, getTierLevel(vanillaTierLevels.get(tier.getLevel())));
            }
        }

        return tierCache.computeIfAbsent(tier, t -> -1);
    }

    public ToolStatsForge() {

        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));

        if (Environment.get().getDist() == Dist.CLIENT) {

            final ToolStatsCommon toolStats = new ToolStatsCommon(FMLPaths.CONFIGDIR.get(), ItemStack::getItemEnchantability, ToolStatsForge::getTierLevel);
            MinecraftForge.EVENT_BUS.addListener((Consumer<ItemTooltipEvent>) event -> toolStats.displayTooltipInfo(event.getItemStack(), event.getFlags(), event.getToolTip()));
        }
    }
}