CREATE TABLE knowledge (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    body TEXT,
    excerpt VARCHAR(255),
    author_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP,
    thumbnail VARCHAR(255),
    FOREIGN KEY (author_id) REFERENCES users(id)
);
