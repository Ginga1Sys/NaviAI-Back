package com.ginga.naviai.knowledge.entity;

import com.ginga.naviai.auth.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "knowledge")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Knowledge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(length = 255)
    private String excerpt;

    @Column(length = 255)
    private String thumbnail;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "knowledge_tag",
        joinColumns = @JoinColumn(name = "knowledge_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();
}

