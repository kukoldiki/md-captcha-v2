package example

import arc.Events
import arc.util.CommandHandler
import arc.util.Log
import mindustry.Vars
import mindustry.content.Blocks
import mindustry.content.Items
import mindustry.game.EventType.BuildSelectEvent
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.gen.Player
import mindustry.mod.Plugin
import mindustry.net.Administration.ActionType
import mindustry.net.Administration.PlayerAction
import mindustry.world.blocks.storage.CoreBlock

class ExamplePlugin : Plugin() {
    //called when game initializes
    override fun init() {
        //listen for a block selection event
        Events.on(BuildSelectEvent::class.java) { event: BuildSelectEvent ->
            if (!event.breaking && event.builder != null && event.builder.buildPlan() != null && event.builder.buildPlan().block === Blocks.thoriumReactor && event.builder.isPlayer) {
                //player is the unit controller
                val player = event.builder.player

                //send a message to everyone saying that this player has begun building a reactor
                Call.sendMessage("[scarlet]ALERT![] " + player.name + " has begun building a reactor at " + event.tile.x + ", " + event.tile.y)
            }
        }

        //add a chat filter that changes the contents of all messages
        //in this case, all instances of "heck" are censored
        Vars.netServer.admins.addChatFilter { _: Player, text: String ->
            text.replace(
                "heck",
                "h*ck"
            )
        }

        //add an action filter for preventing players from doing certain things
        Vars.netServer.admins.addActionFilter { action: PlayerAction ->
            //random example: prevent blast compound depositing
            if (action.type == ActionType.depositItem && action.item === Items.blastCompound && action.tile.block() is CoreBlock) {
                action.player.sendMessage("Example action filter: Prevents players from depositing blast compound into the core.")
                return@addActionFilter false
            }
            true
        }
    }

    //register commands that run on the server
    override fun registerServerCommands(handler: CommandHandler) {
        handler.register("reactors", "List all thorium reactors in the map.") {
            for (x in 0..<Vars.world.width()) {
                for (y in 0..<Vars.world.height()) {
                    //loop through and log all found reactors
                    //make sure to only log reactor centers
                    if (Vars.world.tile(x, y).block() === Blocks.thoriumReactor && Vars.world.tile(x, y).isCenter) {
                        Log.info("Reactor at @, @", x, y)
                    }
                }
            }
        }
    }

    //register commands that player can invoke in-game
    override fun registerClientCommands(handler: CommandHandler) {
        //register a simple reply command

        handler.register(
            "reply",
            "<text...>",
            "A simple ping command that echoes a player's text."
        ) { args: Array<String>, player: Player ->
            player.sendMessage("You said: [accent] " + args[0])
        }

        //register a whisper command which can be used to send other players messages
        handler.register(
            "whisper",
            "<player> <text...>",
            "Whisper text to another player."
        ) { args: Array<String>, player: Player ->
            //find player by name
            val other = Groups.player.find { p: Player -> p.name.equals(args[0], ignoreCase = true) }

            //give error message with scarlet-colored text if player isn't found
            if (other == null) {
                player.sendMessage("[scarlet]No player by that name found!")
                return@register
            }

            //send the other player a message, using [lightgray] for gray text color and [] to reset color
            other.sendMessage("[lightgray](whisper) " + player.name + ":[] " + args[1])
        }
    }
}