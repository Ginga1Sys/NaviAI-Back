package com.ginga.naviai.knowledge.repository;

import com.ginga.naviai.knowledge.entity.KnowledgeRevision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeRevisionRepository extends JpaRepository<KnowledgeRevision, Long> {

    /**
     * 指定記事IDに紐づく編集履歴を編集者情報ごと一括取得。
     * N+1を回避するためにLEFT JOIN FETCHで編集者をまとめて取得する。
     */
    @Query("SELECT r FROM KnowledgeRevision r LEFT JOIN FETCH r.editor " +
           "WHERE r.knowledge.id = :knowledgeId " +
           "ORDER BY r.createdAt ASC")
    List<KnowledgeRevision> findByKnowledgeIdWithEditor(@Param("knowledgeId") Long knowledgeId);
}
