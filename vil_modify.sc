//如果仍然有BUG或没明白的地方，请私信我的b站 https://space.bilibili.com/67131398
mp(villager,profession,level,biome)->(
    
print(player(),[villager,profession,level,biome]);// if he/she is not a villager, all arguments but the first one will be null

if(level==1,'vanilla';return());//use vanilla

[
    nbt('{maxUses:9999999,buy:{id:cobblestone,Count:64},sell:{id:coal,Count:10}}'),
    (temp=nbt('{maxUses:9999999,buy:{id:cobblestone,Count:64}}');map=getmap(villager);if(map==null,null,temp:'sell'=map;temp)),
    nbt('{maxUses:9999999,buyB:{id:cobblestone,Count:64},sell:{id:coal,Count:10}}'),
]

);


villager_trade_ovr('mp');//use villager_trade_ovr() to unregister


getmap(villager)->(
    location=vil_loc('eye_of_ender_located',block(pos(villager)),100,true);
    if(location==null,return());
    mapitem=vil_createmap(location,2,true,true);
    mapitem=vil_drawmap(mapitem);
    mapitem=vil_markmap(mapitem,location,'+','village_snowy');
    mapitem:2:'display.Name'=  '"{\\"translate\\":\\"filled_map.monument\\"}"';
    print(player(),mapitem);
    //modify(spawn('item',player()~'pos',nbt('{Item:{id:stone,Count:1b}}')),'item',mapitem)
    encode_nbt({'id'->mapitem:0,'Count'->mapitem:1,'tag'->mapitem:2},true)
)