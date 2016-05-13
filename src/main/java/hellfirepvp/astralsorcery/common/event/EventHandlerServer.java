package hellfirepvp.astralsorcery.common.event;

import hellfirepvp.astralsorcery.common.constellation.CelestialHandler;
import hellfirepvp.astralsorcery.common.entities.EntityItemHighlighted;
import hellfirepvp.astralsorcery.common.item.ItemConstellationPaper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.*;

/**
 * This class is part of the Astral Sorcery Mod
 * The complete source code for this mod can be found on github.
 * Class: EventHandlerServer
 * Created by HellFirePvP
 * Date: 07.05.2016 / 01:09
 */
public class EventHandlerServer {

    @SubscribeEvent
    public void onTick(TickEvent.WorldTickEvent event) {
        if(event.phase != TickEvent.Phase.END) return;
        if(event.world.provider.getDimension() == 0) {
            CelestialHandler.informTick(event.world);
        }
    }

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent event) {
        if(event.getWorld().isRemote) return;

        Entity joined = event.getEntity();
        if(joined instanceof EntityItem && !(joined instanceof EntityItemHighlighted)) {
            EntityItem ei = (EntityItem) joined;
            if(ei.getEntityItem() != null && (ei.getEntityItem().getItem() instanceof ItemConstellationPaper)) {
                ei.setDead();
                EntityItemHighlighted newItem = new EntityItemHighlighted(ei.worldObj, ei.posX, ei.posY, ei.posZ, ei.getEntityItem());
                int dmg = ei.getEntityItem().getItemDamage();
                if(dmg == 1) {
                    newItem.applyColor(Color.GRAY);
                } else {
                    newItem.applyColor(Color.BLUE);
                }
                newItem.motionX = ei.motionX;
                newItem.motionY = ei.motionY;
                newItem.motionZ = ei.motionZ;
                newItem.hoverStart = ei.hoverStart;
                newItem.setPickupDelay(40);

                event.getWorld().spawnEntityInWorld(newItem);
                event.setCanceled(true);
            }
        }
    }

}
