package live.toon.api.controller;

import live.toon.api.dto.ChatMessageDto;
import live.toon.api.repository.ChatMessageRepository;
import live.toon.api.entity.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/chat")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ROLE_MODERATOR','ROLE_ADMIN')")
public class AdminChatController {

    private final ChatMessageRepository chatMessageRepository;

    @GetMapping
    public ResponseEntity<Page<ChatMessageDto>> listMessages(
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String username,
            @RequestParam(defaultValue = "0") int page) {

        PageRequest pr = PageRequest.of(page, 50, Sort.by("sentAt").descending());
        Page<ChatMessage> result;

        if (roomId != null && username != null) {
            result = chatMessageRepository.findByRoomIdAndUsernameContainingIgnoreCase(roomId, username, pr);
        } else if (username != null) {
            result = chatMessageRepository.findByUsernameContainingIgnoreCase(username, pr);
        } else if (roomId != null && userId != null) {
            result = chatMessageRepository.findByRoomIdAndUserId(roomId, UUID.fromString(userId), pr);
        } else if (roomId != null) {
            result = chatMessageRepository.findByRoomId(roomId, pr);
        } else if (userId != null) {
            result = chatMessageRepository.findByUserId(UUID.fromString(userId), pr);
        } else {
            result = chatMessageRepository.findAll(pr);
        }

        return ResponseEntity.ok(result.map(m -> ChatMessageDto.builder()
                .id(m.getId())
                .roomId(m.getRoomId())
                .userId(m.getUserId().toString())
                .username(m.getUsername())
                .message(m.getMessage())
                .sentAt(m.getSentAt().toString())
                .build()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long id) {
        chatMessageRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
