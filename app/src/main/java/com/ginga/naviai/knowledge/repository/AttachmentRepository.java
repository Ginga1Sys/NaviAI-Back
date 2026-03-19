package com.ginga.naviai.knowledge.repository;

import com.ginga.naviai.knowledge.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    /**
     * 指定記事IDに紐づく添付ファイルを一括取得。
     */
    @Query("SELECT a FROM Attachment a WHERE a.knowledge.id = :knowledgeId ORDER BY a.uploadedAt ASC")
    List<Attachment> findByKnowledgeId(@Param("knowledgeId") Long knowledgeId);
}
