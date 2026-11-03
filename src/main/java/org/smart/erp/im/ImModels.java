package org.smart.erp.im;

import java.util.List;
import java.util.Map;

/** 对外 ID/序号使用字符串，大小和计数使用数字，避免全局 Long 序列化规则影响合同。 */
public final class ImModels {

    private ImModels() {
    }

    public record Config(int maxFileBytes, int maxGroupMembers, int textMaxChars, int textMaxUtf8Bytes,
            int maxAttachmentsPerMessage, List<String> allowedExtensions, int syncRetentionDays,
            int maxConnectionsPerUser) {
    }

    public record Contact(String userId, String displayName, String departmentName, String avatarUrl) {
    }

    public record ContactPage(List<Contact> records, String nextCursor, boolean hasMore) {
    }

    public record FileView(String id, String name, int size, String mime, String sha256) {
    }

    public record SystemPayload(String action, String actorId, List<String> targetUserIds, String description) {
    }

    public record Message(String id, String conversationId, String seq, String senderId, String clientMessageId,
            String type, String text, FileView file, SystemPayload system, String createdAt) {
    }

    public record Conversation(String id, String type, String name, String status, String ownerId, int version,
            int memberCount, String myRole, String visibleFromSeq, String lastSeq, String lastReadSeq, int unreadCount,
            boolean muted, Message lastMessage, String updatedAt) {
    }

    public record Snapshot(List<Conversation> records, String snapshotToken, String nextCursor, boolean hasMore,
            String syncEventSeq, int totalUnreadCount, String expiresAt) {
    }

    public record CreateDirect(String peerUserId) {
    }

    public record PreferenceInput(Boolean muted) {
    }

    public record Preference(String conversationId, boolean muted) {
    }

    public record SendMessage(String clientMessageId, String type, String text, String fileId) {
    }

    public record SendResult(Message message, boolean deduplicated, String senderEventSeq) {
    }

    public record MessagePage(List<Message> records, boolean hasMore, String nextSeq, String direction,
            String latestSeq) {
    }

    public record ReadInput(String throughSeq) {
    }

    public record ReadState(String conversationId, String lastReadSeq, int unreadCount) {
    }

    public record CreateGroup(String clientRequestId, String name, List<String> memberIds) {
    }

    public record UpdateGroup(String name, Integer expectedVersion) {
    }

    public record GroupMember(String userId, String displayName, String departmentName, String avatarUrl, String role,
            String joinedAt) {
    }

    public record MemberPage(List<GroupMember> records, String nextCursor, boolean hasMore, int groupVersion) {
    }

    public record AddMembers(List<String> memberIds, Integer expectedVersion) {
    }

    public record SetRole(String role, Integer expectedVersion) {
    }

    public record TransferOwner(String newOwnerId, Integer expectedVersion) {
    }

    public record VersionInput(Integer expectedVersion) {
    }

    public record LeftGroup(String conversationId, int version) {
    }

    public record ReserveUpload(String clientUploadId, String conversationId, String name, Integer size, String mime,
            String sha256) {
    }

    public record UploadReservation(String uploadId, String method, String uploadUrl,
            Map<String, String> requiredHeaders, String expiresAt, String reservationExpiresAt, int maxFileBytes) {
    }

    public record UploadStatus(String uploadId, String status, FileView file, String rejectionReason) {
    }

    public record TicketInput(String clientId) {
    }

    public record WsTicket(String ticket, String expiresAt, String websocketPath, int heartbeatSeconds,
            int idleTimeoutSeconds) {
    }

    public record Event(int protocolVersion, String eventId, String eventSeq, String type, String conversationId,
            String occurredAt, Object payload) {
    }

    public record EventPage(List<Event> records, String nextAfterEventSeq, boolean hasMore, String highWatermark) {
    }

}
