package com.ginga.naviai.knowledge.repository;

import com.ginga.naviai.knowledge.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * 指定記事IDに紐づく非削除コメントを著者情報ごと一括取得。
     * N+1を回避するためにLEFT JOIN FETCHで著者をまとめて取得する。
     */
    @Query("SELECT c FROM Comment c LEFT JOIN FETCH c.author " +
           "WHERE c.knowledge.id = :knowledgeId AND c.deleted = false " +
           "ORDER BY c.createdAt ASC")
    List<Comment> findByKnowledgeIdWithAuthor(@Param("knowledgeId") Long knowledgeId);
}
