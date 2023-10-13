package name.modid;

import net.fabricmc.api.ModInitializer;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.map.MapIcon;
import net.minecraft.item.map.MapState;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import carpet.script.CarpetContext;
import carpet.script.CarpetExpression;
import carpet.script.CarpetScriptServer;
import carpet.script.argument.FunctionArgument;
import carpet.script.value.BlockValue;
import carpet.script.value.FunctionValue;
import carpet.script.value.NumericValue;
import carpet.script.value.Value;
import carpet.script.value.ValueConversions;
import carpet.CarpetExtension;
import carpet.CarpetServer;

public class ExampleMod implements CarpetExtension, ModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("itertool");
    public static final ExampleMod ins = new ExampleMod();
    public static FunctionValue f;
    final static String funname = "villager_trade_ovr";
    public static String hostname;
    public static CarpetScriptServer scriptServer;

    @Override
    public void onInitialize() {
        CarpetServer.manageExtension(ins);
        f = null;
        LOGGER.info(funname + " loaded");
    }

    public void onServerLoaded(MinecraftServer server) {
        f = null;
        LOGGER.info(funname + " cleared\n\n\n\n");
    }

    public void scarpetApi(CarpetExpression ce) {

        var expression = ce.getExpr();
        expression.addFunctionWithDelegation(funname, -1, false, false, (c, t, expr, tok, lv) -> {
            if (lv.isEmpty()) {
                f = null;
                return Value.TRUE;
            }
            hostname = c.host.getName();
            scriptServer = (CarpetScriptServer) c.host.scriptServer();
            FunctionArgument functionArgument = FunctionArgument.findIn(c, expression.module, lv, 0, false, false);
            f = functionArgument.function;
            return Value.TRUE;
        });
        expression.addContextFunction("vil_loc", -1, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext) c;

            var l = cc.level().locateStructure(TagKey.of(RegistryKeys.STRUCTURE, new Identifier(lv.get(0).getString())),
                    ((BlockValue) lv.get(1)).getPos(), ((NumericValue) lv.get(2)).getInt(), lv.get(3).getBoolean());
            if (l == null) {
                return Value.NULL;
            }
            return new BlockValue(cc.level(), l);
        });
        expression.addContextFunction("vil_createmap", -1, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext) c;
            var i = FilledMapItem.createMap(cc.level(), ((BlockValue) lv.get(0)).getPos().getX(),
                    ((BlockValue) lv.get(0)).getPos().getZ(), (byte) ((NumericValue) lv.get(1)).getInt(),
                    lv.get(2).getBoolean(), lv.get(3).getBoolean());
            return ValueConversions.of(i, cc.level().getRegistryManager());
        });
        expression.addContextFunction("vil_drawmap", -1, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext) c;
            var i = ValueConversions
                    .getItemStackFromValue(lv.get(0), true, cc.level().getRegistryManager());
            FilledMapItem.fillExplorationMap(cc.level(), i);
            return ValueConversions.of(i, cc.level().getRegistryManager());
        });
        expression.addContextFunction("vil_markmap", -1, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext) c;

            var i = ValueConversions.getItemStackFromValue(lv.get(0),true, cc.level().getRegistryManager());
            MapState.addDecorationsNbt(i, ((BlockValue) lv.get(1)).getPos(), lv.get(2).getString(), MapIcon.Type.byId( (byte) ((NumericValue) lv.get(3)).getInt()));
            return ValueConversions.of(i, cc.level().getRegistryManager());
        });
    }
}