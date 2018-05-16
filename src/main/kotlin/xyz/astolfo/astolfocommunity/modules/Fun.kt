package xyz.astolfo.astolfocommunity.modules

import com.github.natanbc.weeb4j.image.HiddenMode
import com.github.natanbc.weeb4j.image.NsfwFilter
import com.github.salomonbrys.kotson.array
import com.github.salomonbrys.kotson.get
import com.google.gson.JsonObject
import com.oopsjpeg.osu4j.OsuUser
import com.oopsjpeg.osu4j.backend.EndpointUsers
import com.oopsjpeg.osu4j.backend.Osu
import net.dv8tion.jda.core.MessageBuilder
import org.jsoup.Jsoup
import xyz.astolfo.astolfocommunity.*
import java.util.*
import java.util.concurrent.TimeUnit

fun createFunModule() = module("Fun") {
    command("isplaying") {
        action {
            val user = event.message.mentionedMembers.getOrNull(0)
            if (user == null) {
                messageAction("Mention a user to see what they're playing!").queue()
                return@action
            }
            val game = user.game
            if (game == null) {
                messageAction(embed("${user.asMention} is not playing anything right now!")).queue()
                return@action
            }
            messageAction(embed("${user.asMention} is playing `${game.name}`!")).queue()
        }
    }
    command("steam") {
        val steamPicture = "https://seeklogo.com/images/S/steam-logo-73274B19E3-seeklogo.com.png"
        action {
            messageAction(embed {
                title("Astolfo Steam Integration")
                description("**profile**  -  gets user data from the steam api")
                thumbnail(steamPicture)
            }).queue()
        }
        command("profile", "p", "user", "stats", "summary") {
            action {
                val url = "http://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/?key=${application.properties.steam_api_key}&steamids="
                val uid = args.takeIf { it.isNotBlank() }?.let {
                    val num = it.matches("-?\\d+(\\.\\d+)?".toRegex())
                    if (!num) {
                        messageAction("User ID's must be numerical!").queue()
                        return@action
                    }
                    if (webJson<SteamUser>("$url$it")!!.response!!.get("players").array.size() == 0) {
                        messageAction("Enter a valid User ID!").queue()
                        return@action
                    }
                    it
                }
                if (args.isBlank()) {
                    messageAction("Provide the 64bit id of the user you wish to search for!").queue()
                    return@action
                }
                val user = SteamUser(webJson<SteamUser>("$url$uid")!!.response!!)
                val status = when (user.state) {
                    "0" -> "Offline"
                    "1" -> "Online"
                    "2" -> "Busy"
                    "3" -> "Away"
                    "4" -> "Snooze"
                    "5" -> "Looking to Trade"
                    "6" -> "Looking to Play"
                    else -> "Unknown"
                }
                messageAction(embed {
                    title("Astolfo Steam Profiles", user.profileUrl)
                    description("\nProfile url: ${user.profileUrl}" +
                            "\nDisplay Name: **${user.displayName}**" +
                            "\nStatus: **$status**")
                    thumbnail(user.avatar!!)
                }).queue()
            }
        }
    }
    command("osu") {
        action {
            messageAction(embed {
                val osuPicture = "https://upload.wikimedia.org/wikipedia/commons/d/d3/Osu%21Logo_%282015%29.png"
                title("Astolfo Osu Integration")
                description("**signature**  -  generates an osu signature of the user" +
                        "\n**profile**  -  gets user data from the osu api")
                thumbnail(osuPicture)
            }).queue()
        }
        command("signature", "sig", "s") {
            action {
                if (args.isBlank()) {
                    messageAction("Provide the name of the user you wish to search for!").queue()
                    return@action
                }
                messageAction(embed {
                    val url = "http://lemmmy.pw/osusig/sig.php?colour=purple&uname=$args&pp=1"
                    title("Astolfo Osu Signature", url)
                    description("$args\'s Osu Signature!")
                    image(url)
                }).queue()
            }
        }
        command("profile", "p", "user", "stats") {
            action {
                if (args.isBlank()) {
                    messageAction("Provide the name of the user you wish the search for!").queue()
                    return@action
                }
                val osu = Osu.getAPI(application.properties.osu_api_token)
                val user: OsuUser
                try {
                    user = osu.users.query(EndpointUsers.ArgumentsBuilder(args).build())
                } catch (e: Exception) {
                    messageAction(":mag: I looked far and wide, but couldn't find user `$args`!" +
                            "\n If they're a real person, Try doing `?osu sig $args` instead.").queue()
                    return@action
                }

                val topPlayBeatmap = user.getTopScores(1).get()[0].beatmap.get()
                messageAction(embed {
                    title("Osu stats for ${user.username}", user.url.toString())
                    description("\nProfile url: ${user.url}" +
                            "\nCountry: **${user.country}**" +
                            "\nGlobal Rank: **#${user.rank} (${user.pp}pp)**" +
                            "\nAccuracy: **${user.accuracy}%**" +
                            "\nPlay Count: **${user.playCount} (Lv${user.level})**" +
                            "\nTop play: **$topPlayBeatmap** ${topPlayBeatmap.url}")
                }).queue()
            }
        }
    }
    command("advice") {
        action {
            messageAction(embed("\uD83D\uDCD6 ${webJson<Advice>("http://api.adviceslip.com/advice")!!.slip!!.advice}")).queue()
        }
    }
    command("cat", "cats") {
        action {
            messageAction(webJson<Cat>("http://aws.random.cat/meow", null)!!.file!!).queue()
        }
    }
    command("catgirl", "neko", "catgirls") {
        action {
            messageAction(webJson<Neko>("https://nekos.life/api/neko")!!.neko!!).queue()
        }
    }
    command("coinflip", "flip", "coin") {
        val random = Random()
        action {
            messageAction("Flipping a coin for you...").queue {
                it.editMessage(MessageBuilder().append("Coin landed on **${if (random.nextBoolean()) "Heads" else "Tails"}**").build()).queueAfter(1, TimeUnit.SECONDS)
            }
        }
    }
    command("roll", "die", "dice") {
        val random = Random()
        action {
            val max = args.takeIf { it.isNotBlank() }?.let {
                val int = it.toIntOrNull()
                if (int == null) {
                    messageAction("The die max value must be a whole number!").queue()
                    return@action
                } else if (int <= 1) {
                    messageAction("The die max value must be a positive number greater than 1!").queue()
                    return@action
                }
                int
            } ?: 6

            messageAction(":game_die: Rolling a die for you...").queue {
                it.editMessage(MessageBuilder().append("Die landed on **${random.nextInt(max - 1) + 1}**").build()).queueAfter(1, TimeUnit.SECONDS)
            }
        }
    }
    command("8ball") {
        val random = Random()
        val responses = arrayOf("It is certain", "You may rely on it", "Cannot predict now", "Yes", "Reply hazy try again", "Yes definitely", "My reply is no", "Better not tell yo now", "Don't count on it", "Most likely", "Without a doubt", "As I see it, yes", "Outlook not so good", "Outlook good", "My sources say no", "Signs point to yes", "Very doubtful", "It is decidedly so", "Concentrate and ask again")
        action {
            if (args.isEmpty()) {
                messageAction(embed(":exclamation: Make sure to ask a question next time. :)")).queue()
            } else {
                val question = args
                messageAction(embed {
                    title(":8ball: 8 Ball")
                    field("Question", question, false)
                    field("Answer", responses[random.nextInt(responses.size)], false)
                }).queue()
            }
        }
    }
    command("csshumor", "cssjoke", "cssh") {
        action {
            messageAction(embed("```css" +
                    "\n${Jsoup.parse(web("https://csshumor.com/")).select(".crayon-code").text()}" +
                    "\n```")).queue()
        }
    }
    command("cyanideandhappiness", "cnh") {
        val random = Random()
        action {
            val r = random.nextInt(4665) + 1
            messageAction(embed {
                title("Cyanide and Happiness")
                image(Jsoup.parse(web("http://explosm.net/comics/$r/"))
                        .select("#main-comic").first()
                        .attr("src")
                        .let { if (it.startsWith("//")) "https:$it" else it })
            }).queue()
        }
    }
    command("dadjoke", "djoke", "dadjokes", "djokes") {
        action {
            messageAction(embed("\uD83D\uDCD6 **Dadjoke:** ${webJson<DadJoke>("https://icanhazdadjoke.com/")!!.joke!!}")).queue()
        }
    }
    command("hug") {
        action {
            selectMember("Hug Selection", args) { selectedMember ->
                val image = application.weeb4J.imageProvider.getRandomImage("hug", HiddenMode.DEFAULT, NsfwFilter.NO_NSFW).execute()
                messageAction(embed {
                    description("${event.author.asMention} has hugged ${selectedMember.asMention}")
                    image(image.url)
                    footer("Powered by weeb.sh")
                }).queue()
            }
        }
    }
    command("kiss") {
        action {
            selectMember("Kiss Selection", args) { selectedMember ->
                val image = application.weeb4J.imageProvider.getRandomImage("kiss", HiddenMode.DEFAULT, NsfwFilter.NO_NSFW).execute()
                messageAction(embed {
                    description("${event.author.asMention} has kissed ${selectedMember.asMention}")
                    image(image.url)
                    footer("Powered by weeb.sh")
                }).queue()
            }
        }
    }
}

class Advice(val slip: AdviceSlip?) {
    inner class AdviceSlip(val advice: String?, val slip_id: String?)
}

class SteamUser(val response: JsonObject?) {
    private fun getStuff(prop: String): String? = response!!.get("players")[0][prop].toString().trim('"')

    // public values
    val profileUrl = getStuff("profileurl")
    val state = getStuff("personastate")
    val avatar = getStuff("avatarfull")
    val displayName = getStuff("personaname")

    // private values
    //val country = getStuff("loccountrycode") ?: "not set"
    //val game = getStuff("gameextrainfo") ?: "none"
}
class Cat(val file: String?)
class Neko(val neko: String?)
class DadJoke(val id: String?, val status: Int?, var joke: String?)