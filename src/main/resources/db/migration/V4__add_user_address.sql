ALTER TABLE users
    ADD COLUMN address_id BIGINT,
    ADD CONSTRAINT fk_users_address
        FOREIGN KEY (address_id) REFERENCES addresses(id);