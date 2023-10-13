package name.modid.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import carpet.script.CarpetScriptServer;
import carpet.script.exception.IntegrityException;
import carpet.script.exception.InvalidCallbackException;
import carpet.script.external.Vanilla;
import carpet.script.value.AbstractListValue;
import carpet.script.value.EntityValue;
import carpet.script.value.NBTSerializableValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;

@Mixin({WanderingTraderEntity.class,VillagerEntity.class})
public class TradeingMixin {
    @Inject(at = @At("HEAD"), method = "fillRecipes", cancellable = true)
    protected void fillRecipes(CallbackInfo ci) {
        if (name.modid.ExampleMod.f == null) {
            return;
        }
        if (((MerchantEntity) (Object) this).getWorld().isClient()) {
            return;
        }
        ServerCommandSource source = ((MerchantEntity) (Object) this).getCommandSource()
                .withLevel(Vanilla.MinecraftServer_getRunPermissionLevel(((MerchantEntity) (Object) this).getServer()));
        var h = name.modid.ExampleMod.scriptServer.getAppHostByName(name.modid.ExampleMod.hostname);
        if(h==null){
            name.modid.ExampleMod.f = null;
            return;
        }
        Value returnValue = null;
        try {
            var x = new java.util.ArrayList<Value>(4);

            x.add(new EntityValue((MerchantEntity) (Object) this));
            if ((Object) this instanceof VillagerEntity villager) {
                x.add(new StringValue(villager.getVillagerData().getProfession().id()));
                x.add(new NumericValue(villager.getVillagerData().getLevel()));
                x.add(new StringValue(villager.getVillagerData().getType().toString()));
            }else{
                x.add(Value.NULL);x.add(Value.NULL);x.add(Value.NULL);
            }
            returnValue = h.callUDF(source, name.modid.ExampleMod.f, x);

        } catch (NullPointerException | InvalidCallbackException | IntegrityException error) {
            CarpetScriptServer.LOG.error("Got exception when running event call ", error);
        }
        if (!(returnValue instanceof AbstractListValue abl)) {
            return;
        }
        TradeOfferList tradeOfferList = ((MerchantEntity) (Object) this).getOffers();
        for (Value ofValue : abl) {
            if (ofValue instanceof NBTSerializableValue) 
                tradeOfferList.add(new TradeOffer((NbtCompound) ((NBTSerializableValue) ofValue).getTag()));
        }
        ci.cancel();
    }
}