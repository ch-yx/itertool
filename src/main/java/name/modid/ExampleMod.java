package name.modid;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import carpet.script.CarpetEventServer;
import carpet.script.CarpetExpression;
import carpet.script.CarpetScriptServer;
import carpet.script.ScriptHost;
import carpet.script.argument.FunctionArgument;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.FunctionValue;
import carpet.script.value.Value;
import carpet.CarpetExtension;
import carpet.CarpetServer;

public class ExampleMod implements CarpetExtension, ModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("itertool");
    public static final ExampleMod ins = new ExampleMod();
    public static FunctionValue f;

    @Override
    public void onInitialize() {
        CarpetServer.manageExtension(ins);
        f=null;
        LOGGER.info(funname+" loaded");
    }


    String funname="villager_trade_ovr";
    public static String hostname;
    public static CarpetScriptServer scriptServer;
    

    public void scarpetApi(CarpetExpression ce) {
        
        f=null;
        LOGGER.info(funname+" cleared\n\n\n\n");
        var expression=ce.getExpr();
        expression.addFunctionWithDelegation(funname, 1, false, false, (c, t, expr, tok, lv) ->
        {
            if (lv.isEmpty())
            {
                f=null;
            }
            
                
            this.hostname = c.host.getName();
            this.scriptServer = (CarpetScriptServer) c.host.scriptServer();
            FunctionArgument functionArgument = FunctionArgument.findIn(c, expression.module, lv, 0, false, true);
            f = functionArgument.function;
            return Value.TRUE;
        });
    }
}