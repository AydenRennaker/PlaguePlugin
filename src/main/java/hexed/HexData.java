package hexed;

import arc.*;
import arc.struct.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import mindustry.entities.type.*;
import mindustry.game.*;
import mindustry.world.*;
import mindustry.content.*;

import static mindustry.Vars.playerGroup;

public class HexData{
    /** All hexes on the map. No order. */
    private Array<Hex> hexes = new Array<>();
    /** Maps world pos -> hex */
    private IntMap<Hex> hexPos = new IntMap<>();
    /** Maps team ID -> player */
    private IntMap<Player> teamMap = new IntMap<>();
    /** Maps team ID -> list of controlled hexes */
    private IntMap<Array<Hex>> control = new IntMap<>();
    /** Data of specific teams. */
    private HexTeam[] teamData = new HexTeam[256];

    public int terrain_type = (int)(Math.random() * 2);
    public int map_type = (int)(Math.random() * 100);

    Block[][] floors = get_floors();
    Block[][] blocks = get_blocks();

    public void updateStats(){
        teamMap.clear();
        for(Player player : playerGroup.all()){
            teamMap.put(player.getTeam().id, player);
        }
        for(Array<Hex> arr : control.values()){
            arr.clear();
        }

        for(Player player : playerGroup.all()){
            if(player.isDead()) continue;

            HexTeam team = data(player);
            Hex newHex = hexes.min(h -> player.dst2(h.wx, h.wy));
            if(team.location != newHex){
                team.location = newHex;
                team.progressPercent = newHex.getProgressPercent(player.getTeam());
                team.lastCaptured = newHex.controller == player.getTeam();
                Events.fire(new HexMoveEvent(player));
            }
            float currPercent = newHex.getProgressPercent(player.getTeam());
            int lp = (int)(team.progressPercent);
            int np = (int)(currPercent);
            team.progressPercent = currPercent;
            if(np != lp){
                Events.fire(new ProgressIncreaseEvent(player, currPercent));
            }

            boolean captured = newHex.controller == player.getTeam();
            if(team.lastCaptured != captured){
                team.lastCaptured = captured;
                if(captured && !newHex.hasCore()){
                    Events.fire(new HexCaptureEvent(player, newHex));
                }
            }
        }

        for(Hex hex : hexes){
            if(hex.controller != null){
                if(!control.containsKey(hex.controller.id)){
                    control.put(hex.controller.id, new Array<>());
                }
                control.get(hex.controller.id).add(hex);
            }
        }
    }

    public void updateControl(){
        hexes.each(Hex::updateController);
    }

    /** Allocates a new array of players sorted by score, descending. */
    public Array<Player> getLeaderboard(){
        Array<Player> players = playerGroup.all().copy();
        players.sort(p -> -getControlled(p).size);
        return players;
    }

    public @Nullable Player getPlayer(Team team){
        return teamMap.get(team.id);
    }

    public Array<Hex> getControlled(Player player){
        return getControlled(player.getTeam());
    }

    public Array<Hex> getControlled(Team team){
        if(!control.containsKey(team.id)){
            control.put(team.id, new Array<>());
        }
        return control.get(team.id);
    }

    public void initHexes(IntArray ints){
        for(int i = 0; i < ints.size; i++){
            int pos = ints.get(i);
            hexes.add(new Hex(i, Pos.x(pos), Pos.y(pos)));
            hexPos.put(pos, hexes.peek());
        }
    }

    public Array<Hex> hexes(){
        return hexes;
    }

    public @Nullable Hex getHex(int position){
        return hexPos.get(position);
    }

    public HexTeam data(Team team){
        if(teamData[team.id] == null) teamData[team.id] = new HexTeam();
        return teamData[team.id];
    }

    public HexTeam data(Player player){
        return data(player.getTeam());
    }

    public static class HexTeam{
        public boolean dying;
        public boolean chosen;
        public @Nullable Hex location;
        public float progressPercent;
        public boolean lastCaptured;
        public Timekeeper lastMessage = new Timekeeper(HexedMod.messageTime);
    }

    public static class HexCaptureEvent{
        public final Player player;
        public final Hex hex;

        public HexCaptureEvent(Player player, Hex hex){
            this.player = player;
            this.hex = hex;
        }
    }

    public static class HexMoveEvent{
        public final Player player;

        public HexMoveEvent(Player player){
            this.player = player;
        }
    }

    public static class ProgressIncreaseEvent{
        public final Player player;
        public final float percent;

        public ProgressIncreaseEvent(Player player, float percent){
            this.player = player;
            this.percent = percent;
        }
    }

    private Block[][] get_floors(){
        // I don't know java, I'm just winging it. Leave me alone ok, I don't know how to do this without using t1 as a decoy lol
        Block[][] t1 = {};

        if(terrain_type == 0){
            Block[][] t = {
                {Blocks.sand, Blocks.sand, Blocks.sand, Blocks.sand, Blocks.sand, Blocks.sand},
                {Blocks.darksand, Blocks.darksand, Blocks.darksand, Blocks.darksand, Blocks.sand, Blocks.sand},
                {Blocks.darksand, Blocks.darksand, Blocks.darksand, Blocks.darksand, Blocks.sand, Blocks.sand},
                {Blocks.darksand, Blocks.darksand, Blocks.darksand, Blocks.darksand, Blocks.sand, Blocks.sand},
                {Blocks.darksand, Blocks.darksand, Blocks.darksand, Blocks.darksand, Blocks.sand, Blocks.sand}
            };
            return t;
        }

        if(terrain_type == 1){
            Block[][] t = {
                {Blocks.sand, Blocks.sand, Blocks.sand, Blocks.sand, Blocks.sand, Blocks.grass},
                {Blocks.darksand, Blocks.darksand, Blocks.darksand, Blocks.darksand, Blocks.grass, Blocks.grass},
                {Blocks.darksand, Blocks.darksand, Blocks.darksand, Blocks.darksand, Blocks.grass, Blocks.shale},
                {Blocks.darksand, Blocks.darksand, Blocks.moss, Blocks.moss, Blocks.sporeMoss, Blocks.stone},
                {Blocks.ice, Blocks.iceSnow, Blocks.snow, Blocks.holostone, Blocks.hotrock, Blocks.salt}
            };
            return t;
        }
        return t1;
    }

    private Block[][] get_blocks(){
        Block[][] t1 = {};

        if(terrain_type == 0){
            Block[][] t = {
                {Blocks.rocks, Blocks.rocks, Blocks.sandRocks, Blocks.sandRocks, Blocks.sandRocks, Blocks.sandRocks},
                {Blocks.rocks, Blocks.rocks, Blocks.sandRocks, Blocks.sandRocks, Blocks.sandRocks, Blocks.sandRocks},
                {Blocks.rocks, Blocks.rocks, Blocks.sandRocks, Blocks.sandRocks, Blocks.sandRocks, Blocks.sandRocks},
                {Blocks.sandRocks, Blocks.sandRocks, Blocks.sandRocks, Blocks.sandRocks, Blocks.sandRocks, Blocks.rocks},
                {Blocks.sandRocks, Blocks.sandRocks, Blocks.sandRocks, Blocks.sandRocks, Blocks.rocks, Blocks.sandRocks}
            };
            return t;
        }

        if(terrain_type == 1){
            Block[][] t = {
                {Blocks.rocks, Blocks.rocks, Blocks.sandRocks, Blocks.sandRocks, Blocks.pine, Blocks.pine},
                {Blocks.rocks, Blocks.rocks, Blocks.duneRocks, Blocks.duneRocks, Blocks.pine, Blocks.pine},
                {Blocks.rocks, Blocks.rocks, Blocks.duneRocks, Blocks.duneRocks, Blocks.pine, Blocks.pine},
                {Blocks.sporerocks, Blocks.duneRocks, Blocks.sporerocks, Blocks.sporerocks, Blocks.sporerocks, Blocks.rocks},
                {Blocks.icerocks, Blocks.snowrocks, Blocks.snowrocks, Blocks.snowrocks, Blocks.rocks, Blocks.saltRocks}
            };
            return t;
        }
        return t1;
    }
}
