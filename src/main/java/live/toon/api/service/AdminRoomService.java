package live.toon.api.service;

import jakarta.persistence.EntityNotFoundException;
import live.toon.api.dto.AdminRoomDto;
import live.toon.api.dto.AdminRoomUpdateRequest;
import live.toon.api.entity.*;
import live.toon.api.repository.RoomRepository;
import live.toon.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminRoomService {

    private static final int PAGE_SIZE = 30;

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<AdminRoomDto> listRooms(String search, int page) {
        PageRequest pr = PageRequest.of(page, PAGE_SIZE, Sort.by("id").descending());
        if (search != null && !search.isBlank()) {
            // Réutilise la query existante avec type=PUBLIC pour toutes les rooms
            String q = search.trim();
            return roomRepository.findByTypeFiltered(RoomType.PUBLIC, q, false, pr).map(this::toDto);
        }
        return roomRepository.findAll(pr).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public AdminRoomDto getRoom(Long id) {
        return toDto(requireRoom(id));
    }

    @Transactional
    public AdminRoomDto updateRoom(Long id, AdminRoomUpdateRequest req) {
        Room room = requireRoom(id);
        if (req.name() != null)      room.setName(req.name());
        if (req.maxUsers() != null)  room.setMaxUsers(req.maxUsers());
        if (req.houseData() != null) room.setHouseData(req.houseData());
        if (req.access() != null)    room.setAccess(HouseAccess.valueOf(req.access()));
        if (req.ownerId() != null) {
            User newOwner = userRepository.findById(java.util.UUID.fromString(req.ownerId()))
                    .orElseThrow(() -> new IllegalArgumentException("Propriétaire introuvable"));
            room.setOwner(newOwner);
        }
        return toDto(roomRepository.save(room));
    }

    @Transactional
    public void deleteRoom(Long id) {
        roomRepository.delete(requireRoom(id));
    }

    @Transactional
    public AdminRoomDto lockRoom(Long id) {
        Room room = requireRoom(id);
        room.setLocked(true);
        return toDto(roomRepository.save(room));
    }

    @Transactional
    public AdminRoomDto unlockRoom(Long id) {
        Room room = requireRoom(id);
        room.setLocked(false);
        return toDto(roomRepository.save(room));
    }

    /** Remet le compteur à zéro (le game-server gérera la déconnexion réelle). */
    @Transactional
    public AdminRoomDto kickAll(Long id) {
        Room room = requireRoom(id);
        room.setUserCount(0);
        return toDto(roomRepository.save(room));
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Room requireRoom(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Room introuvable : " + id));
    }

    private AdminRoomDto toDto(Room r) {
        return AdminRoomDto.builder()
                .id(r.getId())
                .name(r.getName())
                .type(r.getType().name())
                .access(r.getAccess().name())
                .maxUsers(r.getMaxUsers())
                .userCount(r.getUserCount())
                .locked(r.isLocked())
                .houseData(r.getHouseData())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
