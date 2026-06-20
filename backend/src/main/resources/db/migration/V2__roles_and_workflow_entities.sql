CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE,
    description VARCHAR(500)
);

ALTER TABLE repair_requests
    ADD COLUMN category_id BIGINT,
    ADD COLUMN assigned_to BIGINT,
    ADD CONSTRAINT fk_repair_requests_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_repair_requests_assigned_to FOREIGN KEY (assigned_to) REFERENCES users(id) ON DELETE SET NULL;

CREATE INDEX idx_repair_requests_category_id ON repair_requests(category_id);
CREATE INDEX idx_repair_requests_assigned_to ON repair_requests(assigned_to);

CREATE TABLE request_comments (
    id BIGSERIAL PRIMARY KEY,
    request_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    message VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_request_comments_request FOREIGN KEY (request_id) REFERENCES repair_requests(id) ON DELETE CASCADE,
    CONSTRAINT fk_request_comments_author FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_request_comments_request_id ON request_comments(request_id);

CREATE TABLE request_attachments (
    id BIGSERIAL PRIMARY KEY,
    request_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    mime_type VARCHAR(120) NOT NULL,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_request_attachments_request FOREIGN KEY (request_id) REFERENCES repair_requests(id) ON DELETE CASCADE
);

CREATE INDEX idx_request_attachments_request_id ON request_attachments(request_id);

CREATE TABLE status_history (
    id BIGSERIAL PRIMARY KEY,
    request_id BIGINT NOT NULL,
    changed_by BIGINT,
    old_status VARCHAR(30),
    new_status VARCHAR(30) NOT NULL,
    comment VARCHAR(500),
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_status_history_request FOREIGN KEY (request_id) REFERENCES repair_requests(id) ON DELETE CASCADE,
    CONSTRAINT fk_status_history_changed_by FOREIGN KEY (changed_by) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_status_history_request_id ON status_history(request_id);

INSERT INTO categories (name, description) VALUES
    ('Электрика', 'Неисправности электропроводки и электрооборудования'),
    ('Компьютерная техника', 'Ремонт ПК, ноутбуков и периферии'),
    ('Сантехника', 'Водопровод, отопление, канализация'),
    ('Офисная техника', 'Принтеры, МФУ, телефония');
