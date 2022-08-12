package dev.bitflow.kfox.data

import dev.bitflow.kfox.localization.TranslationProvider
import dev.kord.common.Locale
import dev.kord.common.entity.Permission

fun TranslationProvider.getPermissionName(permission: Permission, locale: Locale = defaultLocale): String =
    getString(permission.toTranslationKey(), locale = locale, module = "permission")

fun Permission.toTranslationKey(): String = when (this) {
    Permission.AddReactions -> "addReactions"
    Permission.Administrator -> "administrator"
    Permission.All -> "all"
    Permission.AttachFiles -> "attachFiles"
    Permission.BanMembers -> "banMembers"
    Permission.ChangeNickname -> "changeNickname"
    Permission.Connect -> "connect"
    Permission.CreateInstantInvite -> "createInstantInvite"
    Permission.DeafenMembers -> "deafenMembers"
    Permission.EmbedLinks -> "embedLinks"
    Permission.KickMembers -> "kickMembers"
    Permission.ManageChannels -> "manageChannels"
    Permission.ManageEmojisAndStickers -> "manageEmojisAndStickers"
    Permission.ManageEvents -> "manageEvents"
    Permission.ManageGuild -> "manageGuild"
    Permission.ManageMessages -> "manageMessages"
    Permission.ManageNicknames -> "manageNicknames"
    Permission.ManageRoles -> "manageRoles"
    Permission.ManageThreads -> "manageThreads"
    Permission.ManageWebhooks -> "manageWebhooks"
    Permission.MentionEveryone -> "mentionEveryone"
    Permission.MoveMembers -> "moveMembers"
    Permission.MuteMembers -> "muteMembers"
    Permission.PrioritySpeaker -> "prioritySpeaker"
    Permission.ReadMessageHistory -> "readMessageHistory"
    Permission.RequestToSpeak -> "requestToSpeak"
    Permission.SendMessages -> "sendMessages"
    Permission.SendTTSMessages -> "sendTTSMessages"
    Permission.Speak -> "speak"
    Permission.Stream -> "stream"
    Permission.ModerateMembers -> "timeoutMembers"
    Permission.UseExternalEmojis -> "useExternalEmojis"
    Permission.UseApplicationCommands -> "useApplicationCommands"
    Permission.UseVAD -> "useVAD"
    Permission.ViewAuditLog -> "viewAuditLog"
    Permission.ViewChannel -> "viewChannel"
    Permission.ViewGuildInsights -> "viewGuildInsights"
    Permission.CreatePublicThreads -> "createPublicThreads"
    Permission.CreatePrivateThreads -> "createPrivateThreads"
    Permission.SendMessagesInThreads -> "sendMessagesInThreads"
    Permission.UseExternalStickers -> "useExternalStickers"
    Permission.UseEmbeddedActivities -> "useEmbeddedActivities"
    is Permission.Unknown -> "unknown"
}
