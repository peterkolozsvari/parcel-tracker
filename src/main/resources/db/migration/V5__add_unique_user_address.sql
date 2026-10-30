ALTER TABLE users
    ADD CONSTRAINT uq_users_address
        UNIQUE (address_id);