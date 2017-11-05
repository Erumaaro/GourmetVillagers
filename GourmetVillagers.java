package net.Erumaaro.GourmetVillagers;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.*;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.item.*;
import net.minecraft.init.Items;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.VillagerRegistry;

@Mod(modid = GourmetVillagers.modId, name = GourmetVillagers.name, version = GourmetVillagers.version, acceptedMinecraftVersions = "[1.12.2]")
public class GourmetVillagers {

    public static final String modId = "gourmetvillagers";
    public static final String name = "GourmetVillagers";
    public static final String version = "1.0.0";

    @Mod.Instance(modId)
    public static GourmetVillagers instance;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        System.out.println(name + " is loading...stuff...");
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {

    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {

    }

    @Mod.EventBusSubscriber
    public static class MyForgeEventHandler {
        @SubscribeEvent
        public static void EntityJoinWorld(EntityJoinWorldEvent event) {
            if(event.getEntity() instanceof EntityVillager) {
                EntityVillager thisVillager =((EntityVillager)event.getEntity());

                for (Object entry : thisVillager.tasks.taskEntries.toArray()) {
                    EntityAIBase ai = ((EntityAITasks.EntityAITaskEntry) entry).action;
                    if (ai instanceof EntityAIVillagerMate) {
                        thisVillager.tasks.removeTask(ai);
                    }
                    if (ai instanceof EntityAIVillagerInteract) {
                        thisVillager.tasks.removeTask(ai);
                    }
                    if (ai instanceof EntityAIHarvestFarmland) {
                        thisVillager.tasks.removeTask(ai);
                    }
                }

                thisVillager.tasks.addTask(9, new GourmetEntityAIVillagerInteract(thisVillager));
                if(!thisVillager.isChild()) {
                    thisVillager.tasks.addTask(6, new GourmetEntityAIVillagerMate(thisVillager));
                }
                if(thisVillager.getProfessionForge() == VillagerRegistry.getById(0)) {
                    thisVillager.tasks.addTask(6, new GourmetEntityAIHarvestFarmland(thisVillager, 0.6D));
                }
            }
        }
        @SubscribeEvent
        public static void UpdateLiving(LivingEvent.LivingUpdateEvent event){

            if(event.getEntity() instanceof EntityVillager){
                GourmetVillagerLivingUpdate((EntityLivingBase) event.getEntity());

            }

        }
    }




    public static void GourmetVillagerLivingUpdate(EntityLivingBase LivingEntity)
    {
        EntityVillager villager = (EntityVillager)LivingEntity;
        //super.GourmetVillagerLivingUpdate();
        villager.world.profiler.startSection("looting");

        if (!villager.world.isRemote && villager.canPickUpLoot() && villager.isEntityAlive() && villager.world.getGameRules().getBoolean("mobGriefing"))
        {
            for (EntityItem entityitem : villager.world.getEntitiesWithinAABB(EntityItem.class, villager.getEntityBoundingBox().grow(1.0D, 0.0D, 1.0D)))
            {
                if (!entityitem.isDead && !entityitem.getItem().isEmpty() && !entityitem.cannotPickup())
                {
                    updateEquipmentIfNeededGV(villager,entityitem);
                }
            }
        }

        villager.world.profiler.endSection();
    }
    public static void updateEquipmentIfNeededGV(EntityVillager villager, EntityItem itemEntity)
    {
        ItemStack itemstack = itemEntity.getItem();
        Item item = itemstack.getItem();

        if (item instanceof ItemSeedFood ||item instanceof ItemSeeds)
        {
            ItemStack itemstack1 = villager.getVillagerInventory().addItem(itemstack);

            if (itemstack1.isEmpty())
            {
                itemEntity.setDead();
            }
            else
            {
                itemstack.setCount(itemstack1.getCount());
            }
        }
    }
    public static boolean isFarmItemInInventoryGV(EntityVillager villager)
    {
        for (int i = 0; i < villager.getVillagerInventory().getSizeInventory(); ++i)
        {
            ItemStack itemstack = villager.getVillagerInventory().getStackInSlot(i);

            if (!itemstack.isEmpty() && (itemstack.getItem() instanceof ItemSeedFood ||itemstack.getItem() instanceof ItemSeeds)&& itemstack.getItem() != Items.WHEAT)
            {
                return true;
            }
        }

        return false;
    }
    public static boolean canAbandonItemsGV(EntityVillager villagerIn)
    {

        return hasEnoughItemsGV(villagerIn,2);
    }

    public static boolean wantsMoreFoodGV(EntityVillager villagerIn)
    {
        boolean flag = villagerIn.getProfession() == 0;

        if (flag)
        {
            return !hasEnoughItemsGV(villagerIn,5);
        }
        else
        {
            return !hasEnoughItemsGV(villagerIn,1);
        }
    }

    /**
     * Returns true if villager has enough items in inventory
     */
    static boolean hasEnoughItemsGV(EntityVillager villagerIn, int multiplier)
    {
        boolean flag = villagerIn.getProfession() == 0;

        for (int i = 0; i < villagerIn.getVillagerInventory().getSizeInventory(); ++i)
        {
            ItemStack itemstack = villagerIn.getVillagerInventory().getStackInSlot(i);

            if (!itemstack.isEmpty())
            {
                if ((itemstack.getItem() instanceof ItemFood) && !(itemstack.getItem() instanceof ItemSeedFood) && itemstack.getCount() >= 3 * multiplier || itemstack.getItem() instanceof ItemSeedFood && itemstack.getCount() >= 12 * multiplier  && itemstack.getItem() != Items.WHEAT)
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

}

