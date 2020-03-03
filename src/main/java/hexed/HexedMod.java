package hexed;

import java.util.*;

import arc.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import hexed.HexData.*;
import mindustry.content.*;
import mindustry.core.GameState.*;
import mindustry.core.NetServer.*;
import mindustry.entities.type.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.game.Schematic.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.net.Packets.*;
import mindustry.plugin.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.*;
import mindustry.core.*;

import static arc.util.Log.info;
import static mindustry.Vars.*;

public class HexedMod extends Plugin{
    //in seconds
    public static final float spawnDelay = 60 * 4;
    //health requirement needed to capture a hex; no longer used
    public static final float healthRequirement = 3500;
    //item requirement to captured a hex
    public static final int itemRequirement = 210;

    public static final int messageTime = 1;
    //in ticks: 60 minutes
    private final static int roundTime = 60 * 60 * 60;
    //in ticks: 3 minutes
    private final static int leaderboardTime = 60 * 60 * 2;

    private final static int updateTime = 60 * 2;

    private final static int winCondition = 10;

    private final static int timerBoard = 0, timerUpdate = 1, timerWinCheck = 2;

    private final Rules rules = new Rules();
    private Interval interval = new Interval(5);

    private HexData data;
    private boolean restarting = false, registered = false;

    private Schematic start;
    private double counter = 0f;
    private int lastMin;

    @Override
    public void init(){
        rules.pvp = true;
        rules.tags.put("hexed", "true");
        rules.loadout = ItemStack.list(Items.copper, 1000, Items.lead, 1000, Items.graphite, 200, Items.metaglass, 200, Items.silicon, 200);
        rules.buildCostMultiplier = 1f;
        rules.buildSpeedMultiplier = 2;
        rules.blockHealthMultiplier = 1f;
        rules.unitBuildSpeedMultiplier = 1f;
        rules.playerDamageMultiplier = 0f;
        rules.enemyCoreBuildRadius = (Hex.diameter - 1) * tilesize / 2f;
        rules.unitDamageMultiplier = 1f;
        rules.playerHealthMultiplier = 0.5f;
        rules.canGameOver = false;
        rules.bannedBlocks.add(Blocks.hail);
        rules.bannedBlocks.add(Blocks.ripple);

        // Disable explosive stuff

        /*Array<Item> item_list = content.items();

        for(int i = 0; i < item_list.size; i++){
            Item temp = item_list.get(i);
            temp.explosiveness = 0;
            temp.flammability = 0;
        };*/
        
        // Log.info(Items.coal.explosiveness);

        Map<String, Integer> player_deaths = new HashMap<String, Integer>();
        int lives = 2;

        // start = Schematics.readBase64("bXNjaAB4nD2OSw7CMAxEx59ky4pjcKgKUokFiRTa85eYtDNe+PlZloyMLPC6fAr82XpR5G/rW+m4xfhY215fy/ZuFcAdkWPEAoSkJCM5KZ0k83Z0HTUjcztGPbdBlzM6o3M6p0t0aX4lQUoykpP+t/ID+vtccA==");
        // start = Schematics.readBase64("bXNjaAB4nE2SgY7CIAyGC2yDsXkXH2Tvcq+AkzMmc1tQz/j210JpXDL8hu3/lxYY4FtBs4ZbBLvG1ync4wGO87bvMU2vsCzTEtIlwvCxBW7e1r/43hKYkGY4nFN4XqbfMD+29IbhvmHOtIc1LjCmuIcrfm3X9QH2PofHIyYY5y3FaX3OS3ze4fiRwX7dLa5nDHTPddkCkT3l1DcA/OALihZNq4H6NHnV+HZCVshJXA9VYZC9kfVU+VQGKSsbjVT1lOgp1qO4rGIo9yvnquxH1ORIohap6HVIDbtpaNlDi4cWD80eFJdrNhbJc8W61Jzdqi/3wrRIRii7GYdelvWMZDQs1kNbqtYe9/KuGvDX5zD6d5SML66+5dwRqXgQee5GK3Edxw1ITfb3SJ71OomzUAdjuWsWqZyJavd8Issdb5BqVbaoGCVzJqrddaUGTWSFHPs67m6H5HlaTqbqpFc91Kfn+2eQSp9pr96/Xtx6cevZjeKKDuUOklvvXy9uPGdNZFjZi7IXZS/n8Hyf/wFbjj/q");
        start = Schematics.readBase64("bXNjaAB4nD2SUW7DIBBEd7ExBqcfOYg/epSeoEIOqiIREzl2qt6+rIGJJfNkdmd2R6GJPpj61T8C2ZiOWzr278+RpiU9n2Gbf32MdN3vu1/vx2Ne0voOf2mj6ytFv81Pv4Y4Z/oJdFnSFub1WGI4XtT5bSHzWvy+h43GY43J3zINj7DKSfRF7dfJi0EKVF5y01Or0aABZEAjRC1EHWgCXSpxrVbnI8D5W3NjuDHcGG5c3aTT1cG5KLctzjoFPQU9BT0FPYXpFVQ6nLp2CLfbHmdfNmGd6XTmIZNkwMpkOufrpK5l0ENFIzMNFV3yU/KtdWh0tEn6TKXDZGq+wzk1s9y23gG9BjuONXGhlp9BXctCUj73VEIaNIAMaKwqI1QsVCxULFQsVCxULFQsVBwScnVfIQUqf2DO1DwcPBw8HDwcPFyZsBNyoAlUJvgH0+orFw==");
        netServer.admins.addChatFilter((player, text) -> {
            for(String swear : CurseFilter.swears){
                text = text.replaceAll("(?i)" + swear, "****");
            }

            return text;
        });

        Events.on(Trigger.update, () -> {
            if(active()){
                data.updateStats();

                for(Player player : playerGroup.all()){
                    if(player.getTeam() != Team.derelict && player.getTeam().cores().isEmpty()){
                        player.kill();
                        killTiles(player.getTeam());
                        Integer curr_deaths = player_deaths.get(player.uuid);
                        int lives_left = lives-curr_deaths;
                        Call.sendMessage("[yellow](!)[] [accent]" + player.name + "[lightgray] has been eliminated![accent] " +  lives_left + "/" + lives + " lives left [yellow] (!)");
                        Call.onInfoMessage(player.con, "Your cores have been destroyed. You are defeated.");
                        player.setTeam(Team.derelict);
                        
                        
                        player_deaths.put(player.uuid, curr_deaths+1);
                    }

                    if(player.getTeam() == Team.derelict){
                        player.dead = true;
                    }else if(data.getControlled(player).size == data.hexes().size){
                        endGame();
                        break;
                    }
                }

                int minsToGo = (int)(roundTime - counter) / 60 / 60;
                if(minsToGo != lastMin){
                    lastMin = minsToGo;
                }

                if(interval.get(timerBoard, leaderboardTime)){
                    Call.onInfoToast(getLeaderboard(), 15f);
                }

                if(interval.get(timerUpdate, updateTime)){
                    data.updateControl();
                }

                if(interval.get(timerWinCheck, 60 * 2)){
                    Array<Player> players = data.getLeaderboard();
                    if(!players.isEmpty() && data.getControlled(players.first()).size >= winCondition && players.size > 1 && data.getControlled(players.get(1)).size <= 1){
                        endGame();
                    }
                }

                counter += Time.delta();

                //kick everyone and restart w/ the script
                if(counter > roundTime && !restarting){
                    endGame();
                }
            }else{
                counter = 0;
            }
        });

        Events.on(BlockDestroyEvent.class, event -> {
            //reset last spawn times so this hex becomes vacant for a while.
            if(event.tile.block() instanceof CoreBlock){
                //Log.info(event.tile.entity.sleeping);
                Hex hex = data.getHex(event.tile.pos());

                if(hex != null){
                    //update state
                    hex.spawnTime.reset();
                    hex.updateController();
                }
            }
        });

        Events.on(PlayerLeave.class, event -> {
            if(active() && event.player.getTeam() != Team.derelict){
                killTiles(event.player.getTeam());
            }
        });

        Events.on(PlayerConnect.class, event -> {
            for(String swear : CurseFilter.swears){
                if(event.player.name.toLowerCase().contains(swear)){
                    event.player.con.kick("That's not a very nice name.");
                    break;
                }
            }
        });

        Events.on(PlayerJoin.class, event -> {
            if(!active() || event.player.getTeam() == Team.derelict) return;

            Array<Hex> copy = data.hexes().copy();
            copy.shuffle();
            Hex hex = copy.find(h -> h.controller == null && h.spawnTime.get());

            if(!player_deaths.containsKey(event.player.uuid)){
                player_deaths.put(event.player.uuid, 0);
            }

            Integer curr_deaths = player_deaths.get(event.player.uuid);

            if(curr_deaths > lives){
                Call.onInfoMessage(event.player.con, "You have lost all your lives this round.\nAssigning into spectator mode.");
                event.player.kill();
                event.player.setTeam(Team.derelict);
            }
            else{
                if(hex != null){
                    loadout(event.player, hex.x, hex.y);
                    Core.app.post(() -> data.data(event.player).chosen = false);
                    hex.findController();
                }else{
                    Call.onInfoMessage(event.player.con, "There are currently no empty hex spaces available.\nAssigning into spectator mode.");
                    event.player.kill();
                    event.player.setTeam(Team.derelict);
                }
            }

            data.data(event.player).lastMessage.reset();
        });

        Events.on(ProgressIncreaseEvent.class, event -> updateText(event.player));

        Events.on(HexCaptureEvent.class, event -> updateText(event.player));

        Events.on(HexMoveEvent.class, event -> updateText(event.player));

        TeamAssigner prev = netServer.assigner;
        netServer.assigner = (player, players) -> {
            Array<Player> arr = Array.with(players);

            if(active()){
                //pick first inactive team
                for(Team team : Team.all()){
                    if(team.id > 5 && !team.active() && !arr.contains(p -> p.getTeam() == team) && !data.data(team).dying && !data.data(team).chosen){
                        data.data(team).chosen = true;
                        return team;
                    }
                }
                Call.onInfoMessage(player.con, "There are currently no empty hex spaces available.\nAssigning into spectator mode.");
                return Team.derelict;
            }else{
                return prev.assign(player, players);
            }
        };
    }

    void updateText(Player player){
        HexTeam team = data.data(player);

        StringBuilder message = new StringBuilder("[white]Hex #" + team.location.id + "\n");

        if(!team.lastMessage.get()) return;

        if(team.location.controller == null){
            if(team.progressPercent > 0){
                message.append("[lightgray]Capture progress: [accent]").append((int)(team.progressPercent)).append("%");
            }else{
                message.append("[lightgray][[Empty]");
            }
        }else if(team.location.controller == player.getTeam()){
            message.append("[yellow][[Captured]");
        }else if(team.location != null && team.location.controller != null && data.getPlayer(team.location.controller) != null){
            message.append("[#").append(team.location.controller.color).append("]Captured by ").append(data.getPlayer(team.location.controller).name);
        }else{
            message.append("<Unknown>");
        }

        Call.setHudText(player.con, message.toString());
    }

    @Override
    public void registerServerCommands(CommandHandler handler){
        handler.register("hexed", "Begin hosting with the Hexed gamemode.", args -> {
            if(!state.is(State.menu)){
                Log.err("Stop the server first.");
                return;
            }

            data = new HexData();

            logic.reset();
            Log.info("Generating map...");
            HexedGenerator generator = new HexedGenerator();
            world.loadGenerator(generator);
            data.initHexes(generator.getHex());
            info("Map generated.");
            state.rules = rules.copy();
            logic.play();
            netServer.openServer();
        });

        handler.register("countdown", "Get the hexed restart countdown.", args -> {
            Log.info("Time until round ends: &lc{0} minutes", (int)(roundTime - counter) / 60 / 60);
        });

        handler.register("end", "End the game.", args -> endGame());

        handler.register("r", "Restart the server.", args -> System.exit(2));
    }

    @Override
    public void registerClientCommands(CommandHandler handler){
        if(registered) return;
        registered = true;

        handler.<Player>register("spectate", "Enter spectator mode. This destroys your base.", (args, player) -> {
             if(player.getTeam() == Team.derelict){
                 player.sendMessage("[scarlet]You're already spectating.");
             }else{
                 killTiles(player.getTeam());
                 player.kill();
                 player.setTeam(Team.derelict);
             }
        });

        handler.<Player>register("captured", "Dispay the number of hexes you have captured.", (args, player) -> {
            if(player.getTeam() == Team.derelict){
                player.sendMessage("[scarlet]You're spectating.");
            }else{
                player.sendMessage("[lightgray]You've captured[accent] " + data.getControlled(player).size + "[] hexes.");
            }
        });

        handler.<Player>register("leaderboard", "Display the leaderboard", (args, player) -> {
            player.sendMessage(getLeaderboard());
        });

        handler.<Player>register("hexstatus", "Get hex status at your position.", (args, player) -> {
            Hex hex = data.data(player).location;
            if(hex != null){
                hex.updateController();
                StringBuilder builder = new StringBuilder();
                builder.append("| [lightgray]Hex #").append(hex.id).append("[]\n");
                builder.append("| [lightgray]Owner:[] ").append(hex.controller != null && data.getPlayer(hex.controller) != null ? data.getPlayer(hex.controller).name : "<none>").append("\n");
                for(TeamData data : state.teams.getActive()){
                    if(hex.getProgressPercent(data.team) > 0){
                        builder.append("|> [accent]").append(this.data.getPlayer(data.team).name).append("[lightgray]: ").append((int)hex.getProgressPercent(data.team)).append("% captured\n");
                    }
                }
                player.sendMessage(builder.toString());
            }else{
                player.sendMessage("[scarlet]No hex found.");
            }
        });
    }

    void endGame(){
        if(restarting) return;

        restarting = true;
        Array<Player> players = data.getLeaderboard();
        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < players.size && i < 3; i++){
            if(data.getControlled(players.get(i)).size > 1){
                builder.append("[yellow]").append(i + 1).append(".[accent] ").append(players.get(i).name)
                .append("[lightgray] (x").append(data.getControlled(players.get(i)).size).append(")[]\n");
            }
        }

        if(!players.isEmpty()){
            boolean dominated = data.getControlled(players.first()).size == data.hexes().size;

            for(Player player : playerGroup.all()){
                Call.onInfoMessage(player.con, "[accent]--ROUND OVER--\n\n[lightgray]"
                + (player == players.first() ? "[accent]You[] were" : "[yellow]" + players.first().name + "[lightgray] was") +
                " victorious, with [accent]" + data.getControlled(players.first()).size + "[lightgray] hexes conquered." + (dominated ? "" : "\n\nFinal scores:\n" + builder));
            }
        }

        Log.info("&ly--SERVER RESTARTING--");
        Time.runTask(60f * 10f, () -> {
            netServer.kickAll(KickReason.serverRestarting);
            Time.runTask(5f, () -> System.exit(2));
        });
    }

    String getLeaderboard(){
        StringBuilder builder = new StringBuilder();
        builder.append("[accent]Leaderboard\n[scarlet]").append(lastMin).append("[lightgray] mins. remaining\n\n");
        int count = 0;
        for(Player player : data.getLeaderboard()){
            builder.append("[yellow]").append(++count).append(".[white] ")
            .append(player.name).append("[orange] (").append(data.getControlled(player).size).append(" hexes)\n[white]");

            if(count > 4) break;
        }
        return builder.toString();
    }

    void killTiles(Team team){
        data.data(team).dying = true;
        Time.runTask(8f, () -> data.data(team).dying = false);
        for(int x = 0; x < world.width(); x++){
            for(int y = 0; y < world.height(); y++){
                Tile tile = world.tile(x, y);
                if(tile.entity != null && tile.getTeam() == team){
                    Time.run(Mathf.random(60f * 6), tile.entity::kill);
                }
            }
        }
    }

    void loadout(Player player, int x, int y){
        // Set coretile to be an instance of HexTile
        Stile coreTile = start.tiles.find(s -> s.block instanceof CoreBlock);
        if(coreTile == null) throw new IllegalArgumentException("Schematic has no core tile. Exiting.");
        int ox = x - coreTile.x, oy = y - coreTile.y;
        start.tiles.each(st -> {
            Tile tile = world.tile(st.x + ox,st.y + oy);
            if(tile == null) return;

            if(tile.link().block() != Blocks.air){
                tile.link().removeNet();
            }

            tile.setNet(st.block, player.getTeam(), st.rotation);

            if(st.block.posConfig){
                tile.configureAny(Pos.get(tile.x - st.x + Pos.x(st.config), tile.y - st.y + Pos.y(st.config)));
            }else{
                tile.configureAny(st.config);
            }
            if(tile.block() instanceof CoreBlock){
                for(ItemStack stack : state.rules.loadout){
                    Call.transferItemTo(stack.item, stack.amount, tile.drawx(), tile.drawy(), tile);
                }
            }
        });
    }

    public boolean active(){
        return state.rules.tags.getBool("hexed") && !state.is(State.menu);
    }


}
