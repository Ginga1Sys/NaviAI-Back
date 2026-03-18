package com.ginga.naviai.admin.repository;

import com.ginga.naviai.admin.entity.KnowledgeModeration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KnowledgeModerationRepository extends JpaRepository<KnowledgeModeration, String> {
}
