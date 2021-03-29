package tallestegg.bigbrain;

import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.item.ItemModelsProperties;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import tallestegg.bigbrain.capablities.ILoaf;
import tallestegg.bigbrain.capablities.Loaf;
import tallestegg.bigbrain.capablities.LoafStorage;
import tallestegg.bigbrain.entity.IBucklerUser;

@Mod(BigBrain.MODID)
public class BigBrain {
    public static final String MODID = "bigbrain";

    public BigBrain() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::enqueueIMC);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::processIMC);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, BigBrainConfig.COMMON_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, BigBrainConfig.CLIENT_SPEC);
        BigBrainItems.ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        BigBrainSounds.SOUNDS.register(FMLJavaModLoadingContext.get().getModEventBus());
        BigBrainEnchantments.ENCHANTMENTS.register(FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CapabilityManager.INSTANCE.register(ILoaf.class, new LoafStorage(), Loaf::new);
        });
    }

    private void doClientStuff(final FMLClientSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(new ItemModelHandler());
    }

    private void enqueueIMC(final InterModEnqueueEvent event) {
    }

    private void processIMC(final InterModProcessEvent event) {
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class TextureHandler {
        @SuppressWarnings("deprecation")
        @SubscribeEvent
        public static void onStitch(TextureStitchEvent.Pre event) {
            if (event.getMap().getTextureLocation().equals(AtlasTexture.LOCATION_BLOCKS_TEXTURE)) {
                event.addSprite(BucklerTexture.BUCKLER_TEXTURE.getTextureLocation());
            }
        }
    }

    public static class ItemModelHandler {
        public ItemModelHandler() {
            ItemModelsProperties.registerProperty(BigBrainItems.BUCKLER.get(), new ResourceLocation("blocking"), (stack, clientWorld, livingEntity) -> {
                boolean active = livingEntity != null && livingEntity.isHandActive() && livingEntity.getActiveItemStack() == stack || livingEntity != null && ((IBucklerUser) livingEntity).isBucklerDashing();
                return livingEntity != null && active ? 1.0F : 0.0F;
            });
        }
    }
}
