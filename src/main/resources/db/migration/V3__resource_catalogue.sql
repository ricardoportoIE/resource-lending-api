CREATE TABLE resources (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    type VARCHAR(30) NOT NULL,
    category VARCHAR(100),
    identifier VARCHAR(100),
    loanable BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_resources_identifier UNIQUE (identifier),
    CONSTRAINT ck_resources_type CHECK (
        type IN ('BOOK', 'LAPTOP', 'EQUIPMENT', 'JOURNAL', 'DOCUMENT', 'OTHER')
    )
);

CREATE TABLE resource_items (
    id UUID PRIMARY KEY,
    resource_id UUID NOT NULL,
    asset_tag VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_resource_items_asset_tag UNIQUE (asset_tag),
    CONSTRAINT fk_resource_items_resource
        FOREIGN KEY (resource_id) REFERENCES resources (id) ON DELETE CASCADE,
    CONSTRAINT ck_resource_items_status CHECK (
        status IN ('AVAILABLE', 'RESERVED', 'ON_LOAN', 'MAINTENANCE', 'LOST', 'RETIRED')
    )
);

CREATE INDEX idx_resources_type ON resources (type);
CREATE INDEX idx_resources_category ON resources (category);
CREATE INDEX idx_resource_items_resource_status ON resource_items (resource_id, status);
