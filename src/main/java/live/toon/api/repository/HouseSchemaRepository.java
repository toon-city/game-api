package live.toon.api.repository;

import live.toon.api.entity.HouseSchema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HouseSchemaRepository extends JpaRepository<HouseSchema, Long> {
}
