package live.toon.api.repository;

import live.toon.api.entity.Room;
import live.toon.api.entity.RoomType;
import live.toon.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findAllByType(RoomType type);

    List<Room> findAllByOwner(User owner);
}
