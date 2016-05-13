package hellfirepvp.astralsorcery.common.constellation;

import hellfirepvp.astralsorcery.common.data.DataActiveCelestials;
import hellfirepvp.astralsorcery.common.data.SyncDataHolder;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * This class is part of the Astral Sorcery Mod
 * The complete source code for this mod can be found on github.
 * Class: CelestialHandler
 * Created by HellFirePvP
 * Date: 06.02.2016 21:39
 */
public class CelestialHandler {

    public static final float MIN_DISTRIBUTION_RATE = 0.3F;
    private static final float DISTRIBUTION_MULTIPLIER = 1F - MIN_DISTRIBUTION_RATE;

    public static final int SOLAR_ECLIPSE_HALF_DUR = 2400;
    public static final int LUNAR_ECLIPSE_HALF_DUR = 2400;

    public static int lastTrackedDate = -1;
    private static Map<Tier, TierIteration> constellationIterations = new HashMap<Tier, TierIteration>();

    private static long savedSeed;
    private static Random rand = null;
    private static boolean seedInit = false; //Problem is, we can't write seed = -1 for default, since -1 could be a valid seed.

    private static StarlightDistribution starlightDistribution;

    public static boolean solarEclipse = false;
    public static boolean dayOfSolarEclipse = false;
    public static int prevSolarEclipseTick = 0;
    public static int solarEclipseTick = 0;

    public static boolean lunarEclipse = false;
    public static boolean dayOfLunarEclipse = false;
    public static int prevLunarEclipseTick = 0;
    public static int lunarEclipseTick = 0;

    public static void informTick(World world) {
        if(world.provider.getDimension() != 0) return;

        if(!seedInit) {
            savedSeed = world.getSeed();
            seedInit = true;
        }
        if(rand == null) {
            rand = new Random(savedSeed);
        }

        calcSolarLunarEclipseTimes(world);

        int days = (int) (world.getWorldTime() / 24000);

        int trackingDifference = days - lastTrackedDate;

        lastTrackedDate = days;
        if(trackingDifference > 0) {
            //Calculating until that day is reached.
            scheduleDayProgression(trackingDifference);
        } else if(trackingDifference < 0) {
            //Resetting and recalculating until specified day is reached!
            rand = new Random(savedSeed);
            constellationIterations.clear();
            scheduleDayProgression(days + 1);
        }
    }

    private static void calcSolarLunarEclipseTimes(World world) {
        int solarTime = (int) ((world.getWorldTime() % 888000) - 864000);
        dayOfSolarEclipse = solarTime > 0;
        if(solarTime > 3600 && solarTime < 8400) {
            solarEclipse = true;
            prevSolarEclipseTick = solarEclipseTick;
            solarEclipseTick = solarTime - 3600;
        } else {
            solarEclipse = false;
            solarEclipseTick = 0;
            prevSolarEclipseTick = 0;
        }

        int lunarTime = (int) ((world.getWorldTime() % 1656000) - 1632000);
        dayOfLunarEclipse = lunarTime > 0;
        if(lunarTime > 15600 && lunarTime < 20400) {
            lunarEclipse = true;
            prevLunarEclipseTick = lunarEclipseTick;
            lunarEclipseTick = lunarTime - 15600;
        } else {
            lunarEclipse = false;
            lunarEclipseTick = 0;
            prevLunarEclipseTick = 0;
        }
    }

    public static MoonPhase getCurrentMoonPhase() {
        return MoonPhase.values()[lastTrackedDate % 8];
    }

    private static void scheduleDayProgression(int loop) {
        for (int i = 0; i < loop; i++) {
            scheduleDayProgression(false);
        }

        ((DataActiveCelestials) SyncDataHolder.getDataServer(SyncDataHolder.DATA_CONSTELLATIONS)).updateIterations(constellationIterations.values());
    }

    private static void scheduleDayProgression(boolean sendUpdates) {
        for(Tier tier : constellationIterations.keySet()) {
            constellationIterations.get(tier).nextDay();
        }

        for(Tier t : ConstellationRegistry.ascendingTiers()) {
            TierIteration ti;
            if(!constellationIterations.containsKey(t)) {
                ti = new TierIteration(t);
                constellationIterations.put(t, ti);
            } else {
                ti = constellationIterations.get(t);
            }

            if(ti.isShowing()) continue; //We don't need to change if the tier agrees that it doesn't need to vanish.
            if(ti.shouldShow() && rand.nextFloat() < t.getShowupChance()) {
                ti.setShowing();
            }
        }

        if(sendUpdates) {
            ((DataActiveCelestials) SyncDataHolder.getDataServer(SyncDataHolder.DATA_CONSTELLATIONS)).updateIterations(constellationIterations.values());
        }

        computeDistribution();
    }

    public static Float getCurrentDistribution(Constellation c) {
        if(starlightDistribution == null) return 0F;
        return starlightDistribution.getDistributionCharge(c);
    }

    private static void computeDistribution() {
        Map<Tier, StarlightDistribution.ConstellationDistribution> resultDistribution = new HashMap<Tier, StarlightDistribution.ConstellationDistribution>();
        for (Tier t : ConstellationRegistry.ascendingTiers()) {
            TierIteration ti = constellationIterations.get(t);
            Constellation active = null;
            boolean activeShowing = false;
            int cIndex = -1;
            if(ti != null) {
                if(ti.getCurrentConstellation() != null) {
                    active = ti.getCurrentConstellation();
                    activeShowing = ti.isShowing();
                }
                if(active != null) {
                    List<Constellation> constellations = t.getConstellations();
                    for (int i = 0; i < constellations.size(); i++) {
                        Constellation c = constellations.get(i);
                        if (c.equals(active)) {
                            cIndex = i;
                        }
                    }
                }
            }
            Map<Constellation, Float> distribution = new HashMap<Constellation, Float>();
            List<Constellation> constellations = t.getConstellations();
            float maxDst = constellations.size() / 2;
            for (int i = 0; i < constellations.size(); i++) {
                Constellation c = constellations.get(i);
                if(i != cIndex) {
                    float distance = Math.abs(cIndex - i);
                    float perc = 1F - (distance / maxDst);
                    distribution.put(c, MIN_DISTRIBUTION_RATE + (perc * DISTRIBUTION_MULTIPLIER));
                } else {
                    distribution.put(c, activeShowing ? 1F : MIN_DISTRIBUTION_RATE + (DISTRIBUTION_MULTIPLIER / 2F)); //Special case that we'd work around anyway, but just for consistency reasons..
                }
            }
            resultDistribution.put(t, new StarlightDistribution.ConstellationDistribution(distribution));
        }
        starlightDistribution = new StarlightDistribution(resultDistribution);
    }

    public static class TierIteration {

        private final Tier tier;

        private int counter = 0;
        private boolean showing = false;

        public TierIteration(Tier tier) {
            this.tier = tier;
        }

        //We only let it vanish, if the conditions are no longer met.
        public void nextDay() {
            if(!shouldShow())
                showing = false;
        }

        public void setShowing() {
            this.showing = true;
            this.counter++;
            this.counter %= tier.getConstellations().size();
        }

        public boolean shouldShow() {
            return tier.areAppearanceConditionsMet(getCurrentMoonPhase(), dayOfSolarEclipse, dayOfLunarEclipse);
        }

        public boolean isShowing() {
            return showing;
        }

        public Tier getTier() {
            return tier;
        }

        public Constellation getCurrentConstellation() {
            return tier.getConstellations().get(counter);
        }

    }

    public static enum MoonPhase {

        FULL, WANING3_4, WANING1_2, WANING1_4,
        NEW, WAXING1_4, WAXING1_2, WAXING3_4

    }

}
