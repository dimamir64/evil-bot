package com.github.djaler.evilbot.service

import com.github.djaler.evilbot.config.BotProperties
import com.github.djaler.evilbot.entity.DicePollCaptchaRestriction
import com.github.djaler.evilbot.repository.DicePollCaptchaRestrictionRepository
import com.github.djaler.evilbot.utils.userId
import dev.inmo.tgbotapi.types.IdChatIdentifier
import dev.inmo.tgbotapi.types.PollIdentifier
import dev.inmo.tgbotapi.types.UserId
import dev.inmo.tgbotapi.types.chat.ChatPermissions
import dev.inmo.tgbotapi.types.chat.PublicChat
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.abstracts.Message
import dev.inmo.tgbotapi.types.message.content.DiceContent
import dev.inmo.tgbotapi.types.message.content.PollContent
import org.hibernate.exception.ConstraintViolationException
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class DicePollCaptchaService(
    private val chatService: ChatService,
    private val captchaRestrictionRepository: DicePollCaptchaRestrictionRepository,
    private val botProperties: BotProperties
) {
    fun fixRestriction(
        chat: PublicChat,
        member: User,
        joinMessage: Message,
        diceMessage: ContentMessage<DiceContent>,
        pollMessage: ContentMessage<PollContent>,
        correctAnswerIndex: Int,
        permissions: ChatPermissions
    ) {
        val (chatEntity, _) = chatService.getOrCreateChatFrom(chat)

        try {
            captchaRestrictionRepository.save(
                DicePollCaptchaRestriction(
                    chatEntity,
                    member.id.userId,
                    LocalDateTime.now(),
                    joinMessage.messageId,
                    diceMessage.messageId,
                    pollMessage.messageId,
                    null,
                    pollMessage.content.poll.id,
                    false,
                    correctAnswerIndex,
                    canSendMessages = permissions.canSendMessages,
                    canSendAudios = permissions.canSendAudios,
                    canSendDocuments = permissions.canSendDocuments,
                    canSendPhotos = permissions.canSendPhotos,
                    canSendVideos = permissions.canSendVideos,
                    canSendVideoNotes = permissions.canSendVideoNotes,
                    canSendVoiceNotes = permissions.canSendVoiceNotes,
                    canSendPolls = permissions.canSendPolls,
                    canSendOtherMessages = permissions.canSendOtherMessages,
                    canAddWebPagePreviews = permissions.canAddWebPagePreviews,
                    canChangeInfo = permissions.canChangeInfo,
                    canInviteUsers = permissions.canInviteUsers,
                    canPinMessages = permissions.canPinMessages
                )
            )
        } catch (e: ConstraintViolationException) {
            // ignore duplicate
        }
    }

    fun removeRestriction(restriction: DicePollCaptchaRestriction) {
        captchaRestrictionRepository.delete(restriction)
    }

    fun getOverdueRestrictions(): List<DicePollCaptchaRestriction> {
        val overdueDate = LocalDateTime.now().minus(botProperties.captchaKickTimeout)

        return captchaRestrictionRepository.findByDateTimeBefore(overdueDate)
    }

    fun getRestrictionsForKicked(): List<DicePollCaptchaRestriction> {
        return captchaRestrictionRepository.findKicked()
    }

    fun getRestriction(chatId: IdChatIdentifier, memberId: UserId): DicePollCaptchaRestriction? {
        return captchaRestrictionRepository.findByChatTelegramIdAndMemberTelegramId(chatId.chatId, memberId.userId)
    }

    fun getRestrictionForPollOrNull(pollId: PollIdentifier): DicePollCaptchaRestriction? {
        return captchaRestrictionRepository.findByPollId(pollId)
    }

    fun updateRestriction(restriction: DicePollCaptchaRestriction, diceMessage: Message, pollMessage: Message) {
        captchaRestrictionRepository.save(
            restriction.copy(
                diceMessageId = diceMessage.messageId,
                pollMessageId = pollMessage.messageId,
                dateTime = LocalDateTime.now()
            )
        )
    }

    fun markAsKicked(restriction: DicePollCaptchaRestriction, kickMessageId: Long?) {
        captchaRestrictionRepository.save(
            restriction.copy(
                kickMessageId = kickMessageId,
                kicked = true
            )
        )
    }
}
