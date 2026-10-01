package net.lucy.resource;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

public class ResourceScanner {
    private final MinecraftClient client;
    public ResourceScanner(){
        client = MinecraftClient.getInstance();
    }

    public void scanChunk(WorldChunk chunk){
        int foundIron = 0;
        int foundCoal = 0;
        int foundDiamond = 0;
        for(BlockPos pos : chunk.getBlockEntityPositions()){
            Block block = chunk.getBlockState(pos).getBlock();
            if(block == Blocks.IRON_ORE){
                ResourceCache.get().add(new ResourceNode("minecraft:iron_ore", pos, 1, true));
                foundIron++;
            }

            if(block == Blocks.COAL_ORE){
                ResourceCache.get().add(new ResourceNode("minecraft:coal_ore", pos, 1, true));
                foundCoal++;
            }

            if(block == Blocks.DIAMOND_ORE){
                ResourceCache.get().add(new ResourceNode("minecraft:diamond_ore", pos, 1, true));
                foundDiamond++;
            }
        }
    }
}