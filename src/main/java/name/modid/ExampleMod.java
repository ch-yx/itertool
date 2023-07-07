package name.modid;

import net.fabricmc.api.ModInitializer;
import net.minecraft.network.packet.s2c.play.SetCameraEntityS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import carpet.script.CarpetContext;
import carpet.script.CarpetExpression;
import carpet.script.value.Value;
import carpet.script.value.BooleanValue;
import carpet.script.LazyValue;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.AbstractListValue;
import carpet.script.value.EntityValue;
import carpet.script.value.LazyListValue;
import carpet.script.value.ListValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import carpet.CarpetExtension;
import carpet.CarpetServer;

public class ExampleMod implements CarpetExtension, ModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("itertool");
    public static final ExampleMod ins = new ExampleMod();

    @Override
    public void onInitialize() {
        CarpetServer.manageExtension(ins);
        LOGGER.info("itertools loaded");
    }

    public Value chain_fun(List<Value> lv) {

        if (lv.isEmpty()) {
            throw new InternalExpressionException("'iter_chain' function should have at least 1 argument");
        }
        Iterator<Value> it;
        ArrayList<AbstractListValue> need_reset = new ArrayList<>();
        if (lv.size() <= 1) {
            if (lv.get(0) instanceof AbstractListValue alv) {
                it = alv.iterator();
                need_reset.add(alv);
            } else
                throw new InternalExpressionException(
                        "The argument of 'iter_chain' function should be a list or iterator");
        } else {
            it = lv.iterator();
        }

        Iterable<Iterable<Value>> warper = ()-> new Iterator<Iterable<Value>>() {
            public boolean hasNext() {
                return it.hasNext();
            }

            public Iterable<Value> next() {
                var out = it.next();
                if (!(out instanceof AbstractListValue ablvo)) {
                    throw new InternalExpressionException(
                            "The argument of 'iter_chain' function should be a list or iterator");
                }
                need_reset.add(ablvo);
                return ablvo;
            }
        };
        var out = com.google.common.collect.Iterables.concat(warper)
                .iterator();

        final class __chain_iter extends LazyListValue {
            __chain_iter(Iterator<Value> out, ArrayList<AbstractListValue> need_reset) {
                _out = out;
                _need_reset = need_reset;
            }

            public Iterator<Value> _out;
            public ArrayList<AbstractListValue> _need_reset;

            public boolean hasNext() {
                return _out.hasNext();
            }

            public Value next() {
                return _out.next();
            }

            public void reset() {
                _need_reset.forEach(AbstractListValue::fatality);
                _need_reset.clear();
                __chain_iter new_value = (__chain_iter) chain_fun(lv);
                this._out = new_value._out;
                this._need_reset = new_value._need_reset;
            }
        }
        return new __chain_iter(out, need_reset);
    }

    public void scarpetApi(CarpetExpression ce) {
        var expression = ce.getExpr();
        // expression.addFunction("test",(lv)->StringValue.of(
        // lv.get(0).getVariable()+"|||"+lv.get(0).hashCode()+"\n"+
        // lv.get(1).getVariable()+"|||"+lv.get(1).hashCode()+"\n"+
        // (lv.get(0)==lv.get(1))

        // ));

        expression.addImpureFunction("iter_chain", this::chain_fun);
        expression.addImpureUnaryFunction("iter_has_next", v -> {
            if (!(v instanceof final AbstractListValue alv)) {
                throw new InternalExpressionException(
                        "The argument of 'iter_has_next' function should be a list or iterator");
            }
            return BooleanValue.of(alv.iterator().hasNext());
        });
        expression.addImpureUnaryFunction("iter_next", v -> {
            if (!(v instanceof final AbstractListValue alv)) {
                throw new InternalExpressionException(
                        "The argument of 'iter_next' function should be a list or iterator");
            }
            return alv.iterator().next();
        });
        expression.addContextFunction("spec", 2, (c, t, lv) -> {
            var player = lv.get(0);
            var id = lv.get(1);
            if (id instanceof StringValue sid) {
                var e = EntityValue.getEntitiesFromSelector(((CarpetContext) c).source(), sid.getString()).stream()
                        .findFirst();
                id = EntityValue.of(e.orElse(null));
            }
            if (player instanceof EntityValue pv && pv.getEntity() instanceof ServerPlayerEntity pe
                    && id instanceof EntityValue target) {
                pe.networkHandler.sendPacket(new SetCameraEntityS2CPacket(target.getEntity()));
                return Value.TRUE;
            }
            return Value.NULL;
        });
        expression.addLazyFunction("generator", -1, (c, t, lv) -> {

            return (cc, tt) -> new LazyListValue() {
                LazyValue State = (c, t) -> Value.NULL.reboundedTo("_");
                LazyValue nextexpr = lv.get(0);
                LazyValue hasNextexpr = lv.get(1);

                @Override
                public boolean hasNext() {
                    LazyValue _val = c.getVariable("_");
                    c.setVariable("_", State);
                    try {
                        Value result = hasNextexpr.evalValue(c, carpet.script.Context.Type.BOOLEAN);
                        return result.getBoolean();
                    } finally {
                        c.setVariable("_", _val);
                    }
                }

                @Override
                public Value next() {
                    LazyValue _val = c.getVariable("_");
                    c.setVariable("_", State);
                    try {
                        Value result = nextexpr.evalValue(c, t);
                        return result;
                    } finally {
                        State = c.getVariable("_");
                        c.setVariable("_", _val);
                    }
                }

                @Override
                public void reset() {
                    if (lv.size() > 2) {
                        LazyValue _val = c.getVariable("_");
                        c.setVariable("_", State);
                        try {
                            lv.get(2).evalValue(c, t);
                        } finally {
                            c.setVariable("_", _val);
                        }

                    }
                    State = (c, t) -> Value.NULL.reboundedTo("_");
                }

            };
        });

        // script run
        // cart_product(...iters)->(res=[[]];for(iters,iter=_;newres=[];for(res,ent=_;for(iter,put(newres,null,[...ent,_]);));res=newres;);return
        // (res););
        // cart_product([1,2,3,4],[5,6,7],[8])
        expression.addFunction("cartesian_product", list_of_iterable_values -> {

            ArrayList<ArrayList<Value>> res = new ArrayList<ArrayList<Value>>();
            res.add(new ArrayList<Value>());

            for (Value a_iterable : list_of_iterable_values) {
                ArrayList<ArrayList<Value>> nres = new ArrayList<ArrayList<Value>>();
                for (ArrayList<Value> eres : res) {
                    ArrayList<Value> _eres = null;
                    for (Value a : ((AbstractListValue) a_iterable).unpack()) {
                        _eres = new ArrayList<>();
                        _eres.addAll(eres);
                        _eres.add(a);
                        nres.add(_eres);
                    }
                }
                res = nres;
            }
            return ListValue.wrap(res.stream().map(ListValue::wrap));
        });
        expression.addFunction("zip", (list_of_iterable_values) -> {
            /*
             * if(list_of_iterable_values.size()<1){
             * throw new
             * InternalExpressionException("There is no arguments of 'zip' function");
             * }
             */
            if (list_of_iterable_values.stream().allMatch(
                    v -> (v instanceof AbstractListValue || v instanceof NumericValue || v instanceof StringValue))) {
                return new LazyListValue() {
                    Iterator<? extends Value> fromValuetoIterator(Value v) {
                        if (v instanceof AbstractListValue) {
                            return ((AbstractListValue) v).iterator();
                        } else if (v instanceof NumericValue) {
                            return LongStream.range(0, NumericValue.asNumber(v).getLong()).mapToObj(NumericValue::new)
                                    .iterator();
                        } else if (v instanceof StringValue sv) {
                            return sv.getString().codePoints().mapToObj(Character::toChars).map(String::new)
                                    .map(StringValue::new).iterator();
                        } else
                            return null;
                    }

                    // List<AbstractListValue>
                    // iterator_value_list=list_of_iterable_values.stream().map(v->(AbstractListValue)v).collect(Collectors.toList());
                    List<Iterator<? extends Value>> iterator_list = list_of_iterable_values.stream()
                            .map(v -> fromValuetoIterator(v)).collect(Collectors.toList());

                    @Override
                    public boolean hasNext() {
                        return iterator_list.stream().anyMatch(Iterator::hasNext);
                    }

                    @Override
                    public Value next() {
                        return ListValue.wrap(iterator_list.stream().map((v) -> v.hasNext() ? v.next() : Value.NULL));
                    }

                    @Override
                    public void reset() {
                        list_of_iterable_values.stream().filter(v -> v instanceof LazyListValue)
                                .forEach(v -> ((LazyListValue) v).reset());
                        // this.iterator_value_list=list_of_iterable_values.stream().map(v->(AbstractListValue)v).collect(Collectors.toList());
                        this.iterator_list = list_of_iterable_values.stream().map(v -> fromValuetoIterator(v))
                                .collect(Collectors.toList());
                    }

                    @Override
                    public String getString() {
                        return "[zip_of_" + list_of_iterable_values.size() + "...]";
                    }
                };
            } else {
                throw new InternalExpressionException("All argument of 'zip' function should be a list or iterator");
            }

        });

    }
}