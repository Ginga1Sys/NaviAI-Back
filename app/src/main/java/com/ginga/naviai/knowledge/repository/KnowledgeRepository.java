package com.ginga.naviai.knowledge.repository;

import com.ginga.naviai.knowledge.entity.Knowledge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface KnowledgeRepository extends JpaRepository<Knowledge, String>, JpaSpecificationExecutor<Knowledge> {
}
