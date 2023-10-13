package name.modid.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.google.common.collect.Lists;

import carpet.script.CarpetEventServer.CallbackResult;
import carpet.script.CarpetScriptServer;
import carpet.script.exception.IntegrityException;
import carpet.script.exception.InvalidCallbackException;
import carpet.script.external.Vanilla;
import carpet.script.value.EntityValue;
import carpet.script.value.NBTSerializableValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import it.unimi.dsi.fastutil.Arrays;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;


@Mixin(VillagerEntity.class)
public class TradeingMixin {
    @Inject(at = @At("HEAD"), method = "fillRecipes", cancellable = true)
    protected void fillRecipes(CallbackInfo ci) {
        if (name.modid.ExampleMod.f == null) {
            return;
        }
        ServerCommandSource source = ((VillagerEntity)(Object)this).getCommandSource().withLevel(Vanilla.MinecraftServer_getRunPermissionLevel(((VillagerEntity)(Object)this).getServer()));
        var h =  name.modid.ExampleMod.scriptServer.getAppHostByName(name.modid.ExampleMod.hostname);
        Value returnValue=null;
        try
        {
            var x = new java.util.ArrayList<Value>(3);
            
            x.add(new EntityValue((VillagerEntity)(Object)this));
            x.add(new StringValue(((VillagerEntity)(Object)this).getVillagerData().getProfession().id()));
            x.add(new NumericValue(((VillagerEntity)(Object)this).getVillagerData().getLevel()));
            returnValue = h.callUDF(source, name.modid.ExampleMod.f,x );
            
        }
        catch (NullPointerException | InvalidCallbackException | IntegrityException error)
        {
            CarpetScriptServer.LOG.error("Got exception when running event call ", error);
        }
        TradeOfferList tradeOfferList = ((VillagerEntity)(Object)this).getOffers();
        tradeOfferList.add(new TradeOffer((NbtCompound) ((NBTSerializableValue)returnValue).getTag()));
        ci.cancel();
    }
}