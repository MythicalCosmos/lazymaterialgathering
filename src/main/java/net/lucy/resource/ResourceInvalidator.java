package net.lucy.resource;

import net.lucy.resource.ResourceCache;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

public class ResourceInvalidator {
    public static void onBlockChanged(BlockPos pos, BlockState oldState, BlockState newState){
        if(oldState.isOf(Blocks.DIAMOND_ORE) && newState.isAir()){
            ResourceCache.get().remove("minecraft:diamond_ore", pos);
        }

        if(oldState.isOf(Blocks.IRON_ORE) && newState.isAir()){
            ResourceCache.get().remove("minecraft:iron_ore", pos);
        }

        if(oldState.isOf(Blocks.COAL_ORE) && newState.isAir()){
            ResourceCache.get().remove("minecraft:coal_ore", pos);
        }
    }
}