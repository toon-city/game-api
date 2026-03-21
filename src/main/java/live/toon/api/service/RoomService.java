package live.toon.api.service;

import live.toon.api.dto.ChatMessageDto;
import live.toon.api.dto.RoomDto;
import live.toon.api.entity.Room;
import live.toon.api.entity.RoomType;
import live.toon.api.repository.ChatMessageRepository;
import live.toon.api.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final ChatMessageRepository chatMessageRepository;

    public List<RoomDto> getPublicRooms() {
        return roomRepository.findAllByType(RoomType.PUBLIC).stream()
                .map(this::toDto)
                .toList();
    }

    public RoomDto getRoom(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Room not found: " + id));
        return toDto(room);
    }

    public List<ChatMessageDto> getChatHistory(Long roomId, int limit) {
        roomRepository.findById(roomId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Room not found: " + roomId));
        List<ChatMessageDto> messages = chatMessageRepository
                .findByRoomIdOrderBySentAtDesc(roomId, PageRequest.of(0, limit))
                .stream()
                .map(m -> ChatMessageDto.builder()
                        .id(m.getId())
                        .roomId(m.getRoomId())
                        .userId(m.getUserId().toString())
                        .username(m.getUsername())
                        .message(m.getMessage())
                        .sentAt(m.getSentAt().toString())
                        .build())
                .toList();
        return messages.reversed();
    }

    private RoomDto toDto(Room room) {
        return RoomDto.builder()
                .id(room.getId())
                .name(room.getName())
                .houseData(room.getHouseData())
                .maxUsers(room.getMaxUsers())
                .type(room.getType().name())
                .build();
    }
}
