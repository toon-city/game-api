package live.toon.api.repository;

import live.toon.api.entity.KredsPackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KredsPackageRepository extends JpaRepository<KredsPackage, Integer> {
    List<KredsPackage> findByActiveTrueOrderByPriceCentsAsc();
}
