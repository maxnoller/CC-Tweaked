// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.integration.jei;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.integration.RecipeModHelpers;
import dan200.computercraft.shared.media.items.DiskItem;
import dan200.computercraft.shared.pocket.items.PocketComputerItem;
import dan200.computercraft.shared.turtle.items.TurtleItem;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.registration.IAdvancedRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

@JeiPlugin
public class JEIComputerCraft implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "jei");
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration subtypeRegistry) {
        subtypeRegistry.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModRegistry.Items.TURTLE_NORMAL.get(), turtleSubtype);
        subtypeRegistry.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModRegistry.Items.TURTLE_ADVANCED.get(), turtleSubtype);

        subtypeRegistry.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModRegistry.Items.POCKET_COMPUTER_NORMAL.get(), pocketSubtype);
        subtypeRegistry.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModRegistry.Items.POCKET_COMPUTER_ADVANCED.get(), pocketSubtype);

        subtypeRegistry.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModRegistry.Items.DISK.get(), diskSubtype);
    }

    @Override
    public void registerAdvanced(IAdvancedRegistration registry) {
        registry.addRecipeManagerPlugin(new RecipeResolver(getRegistryAccess()));
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        var registry = runtime.getRecipeManager();

        // Register all turtles/pocket computers (not just vanilla upgrades) as upgrades on JEI.
        var upgradeItems = RecipeModHelpers.getExtraStacks(getRegistryAccess());
        if (!upgradeItems.isEmpty()) {
            runtime.getIngredientManager().addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, upgradeItems);
        }

        // Hide all upgrade recipes
        var category = registry.createRecipeLookup(RecipeTypes.CRAFTING);
        category.get().forEach(wrapper -> {
            if (RecipeModHelpers.shouldRemoveRecipe(wrapper.id())) {
                registry.hideRecipes(RecipeTypes.CRAFTING, List.of(wrapper));
            }
        });
    }

    /**
     * Distinguishes turtles by upgrades and family.
     */
    private static final IIngredientSubtypeInterpreter<ItemStack> turtleSubtype = (stack, ctx) -> {
        var name = new StringBuilder("turtle:");

        // Add left and right upgrades to the identifier
        var left = TurtleItem.getUpgradeWithData(stack, TurtleSide.LEFT);
        var right = TurtleItem.getUpgradeWithData(stack, TurtleSide.RIGHT);
        if (left != null) name.append(left.holder().key().location());
        if (left != null && right != null) name.append('|');
        if (right != null) name.append(right.holder().key().location());

        return name.toString();
    };

    /**
     * Distinguishes pocket computers by upgrade and family.
     */
    private static final IIngredientSubtypeInterpreter<ItemStack> pocketSubtype = (stack, ctx) -> {
        var name = new StringBuilder("pocket:");

        // Add the upgrade to the identifier
        var upgrade = PocketComputerItem.getUpgradeWithData(stack);
        if (upgrade != null) name.append(upgrade.holder().key().location());

        return name.toString();
    };

    /**
     * Distinguishes disks by colour.
     */
    private static final IIngredientSubtypeInterpreter<ItemStack> diskSubtype = (stack, ctx) -> Integer.toString(DiskItem.getColour(stack));

    private static RegistryAccess getRegistryAccess() {
        return Minecraft.getInstance().level.registryAccess();
    }
}