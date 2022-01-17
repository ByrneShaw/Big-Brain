package tallestegg.bigbrain.entity.ai.goals;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.item.SpyglassItem;

public class ZoomInAtRandomGoal extends Goal {
    private final Pillager pillager;
    private int zoomInTicks;

    public ZoomInAtRandomGoal(Pillager pillager) {
        this.pillager = pillager;
    }

    @Override
    public boolean canUse() {
        return pillager.getRandom().nextFloat() < 1.00F
                && pillager.getOffhandItem().getItem() instanceof SpyglassItem && pillager.getTarget() == null;
    }

    @Override
    public boolean canContinueToUse() {
        return zoomInTicks > 0 && pillager.getTarget() == null;
    }

    @Override
    public void start() {
        this.zoomInTicks = 100;
    }

    @Override
    public void tick() {
        this.zoomInTicks--;
        if (pillager.getOffhandItem().getItem() instanceof SpyglassItem)
            this.pillager.startUsingItem(InteractionHand.OFF_HAND);
        //System.out.println(this.zoomInTicks);
    }
    
    @Override
    public void stop() {
        this.pillager.stopUsingItem();
    }

}
