package org.smart.erp.im;

import jakarta.servlet.http.HttpServletResponse;
import org.smart.erp.common.result.Result;
import org.smart.erp.im.attachment.ImAttachments;
import org.springframework.http.ContentDisposition;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import static org.smart.erp.im.ImModels.*;

@RestController
@RequestMapping("/im")
public class ImController {

    private final ImService chat;

    private final ImSnapshots snapshots;

    private final ImEvents events;

    private final ImTickets tickets;

    private final ImAttachments files;

    private final ImJson json;

    public ImController(ImService chat, ImSnapshots snapshots, ImEvents events, ImTickets tickets, ImAttachments files,
            ImJson json) {
        this.chat = chat;
        this.snapshots = snapshots;
        this.events = events;
        this.tickets = tickets;
        this.files = files;
        this.json = json;
    }

    private <T> T body(String body, Class<T> type) {
        if (body == null || body.length() > 65536)
            throw ImException.invalid("请求体为空或过长");
        var result = json.read(body, type);
        if (result == null)
            throw ImException.invalid("请求体不能为空");
        return result;
    }

    private long id(String id) {
        return ImValues.id(id);
    }

    private int limit(Integer value) {
        return ImValues.limit(value);
    }

    @GetMapping("/config")
    public Result<Config> config() {
        return Result.success(chat.config());
    }

    @GetMapping("/contacts")
    public Result<ContactPage> contacts(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) String cursor, @RequestParam(required = false) Integer limit) {
        return Result.success(chat.contacts(keyword, cursor, limit(limit)));
    }

    @PostMapping("/conversations/direct")
    public Result<Conversation> direct(@RequestBody String body) {
        return Result.success(chat.direct(body(body, CreateDirect.class)));
    }

    @GetMapping("/conversations")
    public Result<Snapshot> list(@RequestParam(required = false) String snapshotToken,
            @RequestParam(required = false) String cursor, @RequestParam(required = false) Integer limit) {
        return Result.success(snapshots.list(snapshotToken, cursor, limit(limit)));
    }

    @GetMapping("/conversations/{id}")
    public Result<Conversation> detail(@PathVariable String id) {
        return Result.success(chat.conversation(id(id)));
    }

    @PutMapping("/conversations/{id}/preferences")
    public Result<Preference> preference(@PathVariable String id, @RequestBody String body) {
        return Result.success(chat.preference(id(id), body(body, PreferenceInput.class)));
    }

    @PostMapping("/conversations/{id}/messages")
    public Result<SendResult> send(@PathVariable String id, @RequestBody String body) {
        return Result.success(chat.send(id(id), body(body, SendMessage.class)));
    }

    @GetMapping("/conversations/{id}/messages")
    public Result<MessagePage> history(@PathVariable String id, @RequestParam(required = false) String beforeSeq,
            @RequestParam(required = false) String afterSeq, @RequestParam(required = false) Integer limit) {
        return Result.success(chat.history(id(id), beforeSeq, afterSeq, limit(limit)));
    }

    @GetMapping("/messages/by-client-id")
    public Result<Message> byClient(@RequestParam String clientMessageId) {
        return Result.success(chat.byClient(clientMessageId));
    }

    @PutMapping("/conversations/{id}/read")
    public Result<ReadState> read(@PathVariable String id, @RequestBody String body) {
        return Result.success(chat.read(id(id), body(body, ReadInput.class)));
    }

    @PostMapping("/groups")
    public Result<Conversation> createGroup(@RequestBody String body) {
        return Result.success(chat.createGroup(body(body, CreateGroup.class)));
    }

    @PutMapping("/groups/{id}")
    public Result<Conversation> rename(@PathVariable String id, @RequestBody String body) {
        return Result.success(chat.rename(id(id), body(body, UpdateGroup.class)));
    }

    @GetMapping("/groups/{id}/members")
    public Result<MemberPage> members(@PathVariable String id, @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer groupVersion, @RequestParam(required = false) Integer limit) {
        return Result.success(chat.members(id(id), cursor, groupVersion, limit(limit)));
    }

    @PostMapping("/groups/{id}/members")
    public Result<Conversation> add(@PathVariable String id, @RequestBody String body) {
        return Result.success(chat.add(id(id), body(body, AddMembers.class)));
    }

    @DeleteMapping("/groups/{id}/members/{userId}")
    public Result<Conversation> remove(@PathVariable String id, @PathVariable String userId,
            @RequestParam Integer expectedVersion) {
        return Result.success(chat.remove(id(id), id(userId), expectedVersion));
    }

    @PutMapping("/groups/{id}/members/{userId}/role")
    public Result<Conversation> role(@PathVariable String id, @PathVariable String userId, @RequestBody String body) {
        return Result.success(chat.role(id(id), id(userId), body(body, SetRole.class)));
    }

    @PostMapping("/groups/{id}/transfer")
    public Result<Conversation> transfer(@PathVariable String id, @RequestBody String body) {
        return Result.success(chat.transfer(id(id), body(body, TransferOwner.class)));
    }

    @PostMapping("/groups/{id}/leave")
    public Result<LeftGroup> leave(@PathVariable String id, @RequestBody String body) {
        return Result.success(chat.leave(id(id), body(body, VersionInput.class)));
    }

    @PostMapping("/groups/{id}/dissolve")
    public Result<Conversation> dissolve(@PathVariable String id, @RequestBody String body) {
        return Result.success(chat.dissolve(id(id), body(body, VersionInput.class)));
    }

    @PostMapping("/uploads")
    public Result<UploadReservation> reserve(@RequestBody String body) {
        return Result.success(files.reserve(body(body, ReserveUpload.class)));
    }

    @PostMapping("/uploads/{id}/renew")
    public Result<UploadReservation> renew(@PathVariable String id) {
        return Result.success(files.renew(id(id)));
    }

    @PostMapping("/uploads/{id}/complete")
    public Result<UploadStatus> complete(@PathVariable String id) {
        return Result.success(files.complete(id(id)));
    }

    @GetMapping("/uploads/{id}")
    public Result<UploadStatus> upload(@PathVariable String id) {
        return Result.success(files.get(id(id)));
    }

    @DeleteMapping("/uploads/{id}")
    public Result<UploadStatus> cancel(@PathVariable String id) {
        return Result.success(files.cancel(id(id)));
    }

    @GetMapping("/files/{id}/content")
    public void download(@PathVariable String id, @RequestHeader(value = "Range", required = false) String range,
            HttpServletResponse response) throws IOException {
        try (var download = files.download(id(id), range)) {
            response.setStatus(download.partial() ? 206 : 200);
            response.setContentType(download.file().mime());
            response.setHeader("Content-Disposition",
                    ContentDisposition.attachment()
                        .filename(download.file().name(), StandardCharsets.UTF_8)
                        .build()
                        .toString());
            response.setHeader("Cache-Control", "private, no-store");
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("Accept-Ranges", "bytes");
            response.setContentLengthLong(download.end() - download.start() + 1);
            if (download.partial())
                response.setHeader("Content-Range",
                        "bytes " + download.start() + "-" + download.end() + "/" + download.file().size());
            download.stream().transferTo(response.getOutputStream());
        }
    }

    @PostMapping("/ws-tickets")
    public Result<WsTicket> ticket(@RequestBody String body, @RequestHeader("Authorization") String authorization) {
        return Result.success(tickets.issue(body(body, TicketInput.class), authorization));
    }

    @GetMapping("/sync/events")
    public Result<EventPage> sync(@RequestParam String afterEventSeq, @RequestParam(required = false) Integer limit) {
        return Result.success(events.sync(ImValues.seq(afterEventSeq), limit == null ? 100 : limit(limit)));
    }

}
