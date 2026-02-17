package com.ginga.naviai.knowledge.repository;

import com.ginga.naviai.knowledge.entity.Knowledge;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KnowledgeRepository extends JpaRepository<Knowledge, Long> {
    Page<Knowledge> findByAuthorId(Long authorId, Pageable pageable);
    Page<Knowledge> findByAuthorUsername(String username, Pageable pageable);
}
