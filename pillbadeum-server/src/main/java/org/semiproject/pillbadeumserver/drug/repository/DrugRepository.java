package org.semiproject.pillbadeumserver.drug.repository;

import org.semiproject.pillbadeumserver.drug.domain.Drug;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** drug 테이블의 기본 조회 및 이미지 식별 후보 조회를 담당한다. */
public interface DrugRepository extends JpaRepository<Drug, String> {

    @Query("""
            select d
            from Drug d
            where (:printFront is null or lower(d.printFront) like lower(concat('%', :printFront, '%')))
              and (:printBack is null or lower(d.printBack) like lower(concat('%', :printBack, '%')))
              and (:shape is null or d.shape = :shape)
              and (:color is null or d.color = :color)
            order by d.name asc
            """)
    List<Drug> findIdentificationCandidates(
            @Param("printFront") String printFront,
            @Param("printBack") String printBack,
            @Param("shape") String shape,
            @Param("color") String color,
            Pageable pageable
    );

    List<Drug> findTop10ByNameContainingIgnoreCaseOrderByNameAsc(String name);
}
