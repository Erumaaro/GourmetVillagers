package net.Erumaaro.GourmetVillagers;

import net.minecraft.block.Block;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.ai.EntityAIMoveToBlock;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemSeedFood;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSeeds;
import net.minecraftforge.common.IPlantable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class GourmetEntityAIHarvestFarmland extends EntityAIMoveToBlock
{
    /** Villager that is harvesting */
    private final EntityVillager villager;
    private boolean hasFarmItem;
    private boolean wantsToReapStuff;
    /** 0 => harvest, 1 => replant, -1 => none */
    private int currentTask;

    public GourmetEntityAIHarvestFarmland(EntityVillager villagerIn, double speedIn)
    {
        super(villagerIn, speedIn, 16);
        this.villager = villagerIn;
    }
    public boolean isFarmItemInInventoryGV(EntityVillager villager)
    {
        for (int i = 0; i < villager.getVillagerInventory().getSizeInventory(); ++i)
        {
            ItemStack itemstack = villager.getVillagerInventory().getStackInSlot(i);

            if (!itemstack.isEmpty() && (itemstack.getItem() instanceof ItemSeedFood ||itemstack.getItem() instanceof ItemSeeds))
            {
                return true;
            }
        }

        return false;
    }
    public boolean canAbondonItemsGV()
    {
        return this.hasEnoughItemsGV(2);
    }

    public boolean wantsMoreFoodGV()
    {
        boolean flag = this.villager.getProfession() == 0;

        if (flag)
        {
            return !hasEnoughItemsGV(5);
        }
        else
        {
            return !hasEnoughItemsGV(1);
        }
    }

    /**
     * Returns true if villager has enough items in inventory
     */
    private boolean hasEnoughItemsGV(int multiplier)
    {
        boolean flag = villager.getProfession() == 0;

        for (int i = 0; i < villager.getVillagerInventory().getSizeInventory(); ++i)
        {
            ItemStack itemstack = villager.getVillagerInventory().getStackInSlot(i);

            if (!itemstack.isEmpty())
            {
                if (itemstack.getItem() instanceof ItemFood && itemstack.getCount() >= 3 * multiplier || itemstack.getItem() instanceof ItemSeedFood && itemstack.getCount() >= 12 * multiplier)
                {
                    return true;
                }

                if (flag && itemstack.getItem() == Items.WHEAT && itemstack.getCount() >= 9 * multiplier)
                {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Returns whether the EntityAIBase should begin execution.
     */
    public boolean shouldExecute()
    {
        if (this.runDelay <= 0)
        {
            if (!this.villager.world.getGameRules().getBoolean("mobGriefing"))
            {
                return false;
            }

            this.currentTask = -1;
            this.hasFarmItem = isFarmItemInInventoryGV(this.villager);
            this.wantsToReapStuff = wantsMoreFoodGV();
        }

        return super.shouldExecute();
    }

    /**
     * Returns whether an in-progress EntityAIBase should continue executing
     */
    public boolean shouldContinueExecuting()
    {
        return this.currentTask >= 0 && super.shouldContinueExecuting();
    }

    /**
     * Keep ticking a continuous task that has already been started
     */

    public void updateTask()
    {
        super.updateTask();
        this.villager.getLookHelper().setLookPosition((double)this.destinationBlock.getX() + 0.5D, (double)(this.destinationBlock.getY() + 1), (double)this.destinationBlock.getZ() + 0.5D, 10.0F, (float)this.villager.getVerticalFaceSpeed());

        if (this.getIsAboveDestination())
        {
            World world = this.villager.world;
            BlockPos blockpos = this.destinationBlock.up();
            IBlockState iblockstate = world.getBlockState(blockpos);
            Block block = iblockstate.getBlock();

            if (this.currentTask == 0 && block instanceof BlockCrops && ((BlockCrops)block).isMaxAge(iblockstate))
            {
                world.destroyBlock(blockpos, true);
            }
            else if (this.currentTask == 1 && iblockstate.getMaterial() == Material.AIR)
            {
                InventoryBasic inventorybasic = this.villager.getVillagerInventory();

                for (int i = 0; i < inventorybasic.getSizeInventory(); ++i)
                {
                    ItemStack itemstack = inventorybasic.getStackInSlot(i);
                    boolean flag = false;

                    if (!itemstack.isEmpty())
                    {
                        if (itemstack.getItem() instanceof ItemSeeds)
                        {
                            //world.setBlockState(blockpos, Blocks.WHEAT.getDefaultState(), 3);
                            world.setBlockState(blockpos, ((ItemSeeds)itemstack.getItem()).getPlant(world,blockpos), 3);
                            flag = true;
                        }
                        else if (itemstack.getItem() instanceof ItemSeedFood)
                        {
                            //world.setBlockState(blockpos, Blocks.POTATOES.getDefaultState(), 3);
                            world.setBlockState(blockpos, ((ItemSeedFood)itemstack.getItem()).getPlant(world,blockpos), 3);
                            flag = true;
                        }
                        //else if (itemstack.getItem() == Items.CARROT)
                        //{
                        //    world.setBlockState(blockpos, Blocks.CARROTS.getDefaultState(), 3);
                        //    flag = true;
                        //}
                        //else if (itemstack.getItem() == Items.BEETROOT_SEEDS)
                        //{
                        //    world.setBlockState(blockpos, Blocks.BEETROOTS.getDefaultState(), 3);
                        //   flag = true;
                        //}
                    }

                    if (flag)
                    {
                        itemstack.shrink(1);

                        if (itemstack.isEmpty())
                        {
                            inventorybasic.setInventorySlotContents(i, ItemStack.EMPTY);
                        }

                        break;
                    }
                }
            }

            this.currentTask = -1;
            this.runDelay = 10;
        }
    }

    /**
     * Return true to set given position as destination
     */
    protected boolean shouldMoveTo(World worldIn, BlockPos pos)
    {
        Block block = worldIn.getBlockState(pos).getBlock();

        if (block == Blocks.FARMLAND)
        {
            pos = pos.up();
            IBlockState iblockstate = worldIn.getBlockState(pos);
            block = iblockstate.getBlock();

            if (block instanceof BlockCrops && ((BlockCrops)block).isMaxAge(iblockstate) && this.wantsToReapStuff && (this.currentTask == 0 || this.currentTask < 0))
            {
                this.currentTask = 0;
                return true;
            }

            if (iblockstate.getMaterial() == Material.AIR && this.hasFarmItem && (this.currentTask == 1 || this.currentTask < 0))
            {
                this.currentTask = 1;
                return true;
            }
        }

        return false;
    }
}