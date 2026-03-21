package live.toon.api.repository;

import live.toon.api.entity.Deditoon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeditoonRepository extends JpaRepository<Deditoon, Long> {
    List<Deditoon> findTop10ByOrderByCreatedAtDesc();
}
