package xyz.astolfo.astolfocommunity.modules

import net.dv8tion.jda.core.Permission
import xyz.astolfo.astolfocommunity.*

fun createAdminModule() = module("Admin") {
    command("settings") {
        inheritedAction {
            if (!event.member.hasPermission(Permission.ADMINISTRATOR)) {
                messageAction(embed("You must be a server admin in order to change settings!")).queue()
                return@inheritedAction false
            }
            true
        }
        action {
            messageAction(embed {
                title("Astolfo Guild Settings")
                description("**prefix**  -  prefix of the bot in the guild")
            }).queue()
        }
        command("prefix") {
            action {
                val data = application.astolfoRepositories.getEffectiveGuildSettings(event.guild.idLong)
                if (args.isBlank()) {
                    messageAction(embed {
                        title("Astolfo Guild Settings - Prefix")
                        description("Current Prefix: `${data.prefix.takeIf { it.isNotBlank() }
                                ?: application.properties.default_prefix}`")
                    }).queue()
                } else {
                    val oldPrefix = data.prefix
                    if (args.equals("reset", true)) data.prefix = ""
                    else data.prefix = args

                    application.astolfoRepositories.guildSettingsRepository.save(data)

                    messageAction(embed {
                        title("Astolfo Guild Settings - Prefix")
                        description("Old Prefix: `${oldPrefix.takeIf { it.isNotBlank() }
                                ?: application.properties.default_prefix}`" +
                                "\nNew Prefix: `${data.prefix.takeIf { it.isNotBlank() }
                                        ?: application.properties.default_prefix}`")
                    }).queue()
                }
            }
        }
    }
    command("prune", "purge", "delete") {
        action {
            if (!event.member.hasPermission(Permission.MESSAGE_MANAGE)) {
                messageAction(embed("You need the `Manage Messages` permission in order to delete messages!")).queue()
                return@action
            }
            val amountToDelete = args.takeIf { it.isNotBlank() }?.let {
                val amountNum = it.toIntOrNull()
                if (amountNum == null) {
                    messageAction("The amount to delete must be a whole number!").queue()
                    return@action
                }
                if (amountNum < 1) {
                    messageAction("The amount to delete must be at least 1!").queue()
                    return@action
                }
                if (amountNum > 100) {
                    messageAction("The amount to delete must be no more than 100!").queue()
                    return@action
                }
                amountNum
            } ?: 2
            val messages = event.textChannel.history.retrievePast(amountToDelete).complete()
            try {
                event.textChannel.deleteMessages(messages).queue()
            } catch (e: Exception) {
                messageAction("You cannot delete messages that are more than 2 weeks old!").queue()
                return@action
            }

            val authors = messages.map { it.author!! }.toSet()
            val nameLength = authors.map { it.name.length }.max()!!
            val messageCounts = authors.map { author -> author to messages.filter { it.author.idLong == author.idLong }.count() }.toMap()

            messageAction(embed {
                title("Astolfo Bot Prune")
                description("${event.message.author.asMention} has pruned the chat! Here are the results:")
                field("Total Messages Deleted:", "```$amountToDelete```", false)
                field("Messages Deleted:", "```Prolog" +
                        "\n${messageCounts.map { entry -> "${entry.key.name.padStart(nameLength)} : ${entry.value}" }.joinToString("\n")}" +
                        "\n```", false)
            }).queue()
        }
    }
    command("kick") {
        action {
            if (!event.guild.selfMember.hasPermission(Permission.KICK_MEMBERS)) {
                messageAction(embed("I need the `Kick Members` permission in order to kick people!")).queue()
                return@action
            }
            if (!event.member.hasPermission(Permission.KICK_MEMBERS)) {
                messageAction(embed("You need the `Kick Members` permission in order to kick people!")).queue()
                return@action
            }
            val query: String
            val reason: String
            if (args.contains(" ")) {
                query = args.substringBefore(" ").trim()
                reason = args.substringAfter(" ").trim()
            } else {
                query = args
                reason = ""
            }
            selectMember("Kick Selection", query) { selectedMember ->
                if (!event.guild.selfMember.canInteract(selectedMember)) {
                    messageAction("I cannot kick that member!").queue()
                    return@selectMember
                }
                val guildController = event.guild.controller
                val userString = "**${selectedMember.effectiveName}** (**${selectedMember.user.name}#${selectedMember.user.discriminator} ${selectedMember.user.id}**)"
                if (reason.isBlank()) {
                    guildController.kick(selectedMember).queue()
                    messageAction(embed("User $userString has been kicked!")).queue()
                } else {
                    guildController.kick(selectedMember, reason).queue()
                    messageAction(embed("User $userString has been kicked with reason **$reason**!")).queue()
                }
            }
        }
    }
    command("ban") {
        action {
            if (!event.guild.selfMember.hasPermission(Permission.BAN_MEMBERS)) {
                messageAction(embed("I need the `Ban Members` permission in order to ban people!")).queue()
                return@action
            }
            if (!event.member.hasPermission(Permission.BAN_MEMBERS)) {
                messageAction(embed("You need the `Ban Members` permission in order to ban people!")).queue()
                return@action
            }
            val query: String
            val reason: String
            if (args.contains(" ")) {
                query = args.substringBefore(" ").trim()
                reason = args.substringAfter(" ").trim()
            } else {
                query = args
                reason = ""
            }
            selectMember("Ban Selection", query) { selectedMember ->
                if (!event.guild.selfMember.canInteract(selectedMember)) {
                    messageAction("I cannot ban that member!").queue()
                    return@selectMember
                }
                val guildController = event.guild.controller
                val userString = "**${selectedMember.effectiveName}** (**${selectedMember.user.name}#${selectedMember.user.discriminator} ${selectedMember.user.id}**)"
                if (reason.isBlank()) {
                    guildController.ban(selectedMember, 0).queue()
                    messageAction(embed("User $userString has been banned!")).queue()
                } else {
                    guildController.ban(selectedMember, 0, reason).queue()
                    messageAction(embed("User $userString has been banned with reason **$reason**!")).queue()
                }
            }
        }
    }
}