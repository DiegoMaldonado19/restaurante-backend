-- =============================================================================
-- IAM
-- =============================================================================

CREATE TABLE app_user (
    user_id       BIGINT       NOT NULL AUTO_INCREMENT,
    full_name     VARCHAR(80)  NOT NULL,
    username      VARCHAR(40)  NOT NULL,
    password_hash VARCHAR(72)  NOT NULL,
    role          VARCHAR(30)  NOT NULL,
    status        VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_app_user      PRIMARY KEY (user_id),
    CONSTRAINT uq_app_user_name UNIQUE (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX ix_app_user_role ON app_user (role, status);

-- =============================================================================
-- RESTAURANT
-- =============================================================================

CREATE TABLE restaurant_table (
    restaurant_table_id BIGINT      NOT NULL AUTO_INCREMENT,
    table_number        INT         NOT NULL,
    capacity            INT         NOT NULL,
    zone                VARCHAR(30) NOT NULL,
    status              VARCHAR(30) NOT NULL DEFAULT 'FREE',
    active              BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_restaurant_table        PRIMARY KEY (restaurant_table_id),
    CONSTRAINT uq_restaurant_table_number UNIQUE (table_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX ix_restaurant_table_status ON restaurant_table (status, active);

CREATE TABLE restaurant_setting (
    setting_id               BIGINT        NOT NULL,
    tax_percent              DECIMAL(5,2)  NOT NULL,
    tip_suggested_percent    DECIMAL(5,2)  NOT NULL,
    points_per_currency_unit DECIMAL(12,4) NOT NULL,
    currency_per_point       DECIMAL(12,4) NOT NULL,
    CONSTRAINT pk_restaurant_setting        PRIMARY KEY (setting_id),
    CONSTRAINT ck_restaurant_setting_single CHECK (setting_id = 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- CUSTOMER
-- =============================================================================

CREATE TABLE customer (
    customer_id BIGINT      NOT NULL AUTO_INCREMENT,
    full_name   VARCHAR(80) NOT NULL,
    phone       VARCHAR(20) NOT NULL,
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_customer       PRIMARY KEY (customer_id),
    CONSTRAINT uq_customer_phone UNIQUE (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE loyalty_transaction (
    loyalty_transaction_id BIGINT      NOT NULL AUTO_INCREMENT,
    customer_id            BIGINT      NOT NULL,
    transaction_type       VARCHAR(30) NOT NULL,
    points                 INT         NOT NULL,
    invoice_id             BIGINT      NULL,
    created_at             DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_loyalty_transaction    PRIMARY KEY (loyalty_transaction_id),
    CONSTRAINT fk_loyalty_transaction_cu FOREIGN KEY (customer_id) REFERENCES customer (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX ix_loyalty_transaction_customer ON loyalty_transaction (customer_id, created_at);

-- =============================================================================
-- INVENTORY
-- =============================================================================

CREATE TABLE supply_category (
    supply_category_id BIGINT      NOT NULL AUTO_INCREMENT,
    name               VARCHAR(60) NOT NULL,
    active             BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_supply_category      PRIMARY KEY (supply_category_id),
    CONSTRAINT uq_supply_category_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE supply (
    supply_id          BIGINT        NOT NULL AUTO_INCREMENT,
    supply_category_id BIGINT        NOT NULL,
    name               VARCHAR(80)   NOT NULL,
    measure_unit       VARCHAR(30)   NOT NULL,
    unit_cost          DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    current_stock      DECIMAL(12,3) NOT NULL DEFAULT 0.000,
    min_stock          DECIMAL(12,3) NOT NULL DEFAULT 0.000,
    max_stock          DECIMAL(12,3) NULL,
    active             BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_supply          PRIMARY KEY (supply_id),
    CONSTRAINT uq_supply_name     UNIQUE (name),
    CONSTRAINT fk_supply_category FOREIGN KEY (supply_category_id)
        REFERENCES supply_category (supply_category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX ix_supply_low_stock ON supply (active, current_stock, min_stock);

CREATE TABLE stock_movement (
    stock_movement_id BIGINT        NOT NULL AUTO_INCREMENT,
    supply_id         BIGINT        NOT NULL,
    movement_type     VARCHAR(30)   NOT NULL,
    quantity          DECIMAL(12,3) NOT NULL,
    unit_cost         DECIMAL(12,2) NULL,
    waste_reason      VARCHAR(30)   NULL,
    reason            VARCHAR(255)  NULL,
    order_item_id     BIGINT        NULL,
    user_id           BIGINT        NOT NULL,
    created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_stock_movement        PRIMARY KEY (stock_movement_id),
    CONSTRAINT fk_stock_movement_supply FOREIGN KEY (supply_id) REFERENCES supply (supply_id),
    CONSTRAINT fk_stock_movement_user   FOREIGN KEY (user_id)   REFERENCES app_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX ix_stock_movement_supply ON stock_movement (supply_id, created_at);
CREATE INDEX ix_stock_movement_type   ON stock_movement (movement_type, created_at);

-- =============================================================================
-- MENU
-- =============================================================================

CREATE TABLE dish_category (
    dish_category_id BIGINT      NOT NULL AUTO_INCREMENT,
    name             VARCHAR(60) NOT NULL,
    display_order    INT         NOT NULL DEFAULT 0,
    active           BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_dish_category      PRIMARY KEY (dish_category_id),
    CONSTRAINT uq_dish_category_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE dish (
    dish_id          BIGINT        NOT NULL AUTO_INCREMENT,
    dish_category_id BIGINT        NOT NULL,
    name             VARCHAR(80)   NOT NULL,
    description      VARCHAR(255)  NULL,
    sale_price       DECIMAL(12,2) NOT NULL,
    image_url        VARCHAR(255)  NULL,
    prep_minutes     INT           NOT NULL DEFAULT 0,
    manual_available BOOLEAN       NOT NULL DEFAULT TRUE,
    active           BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_dish          PRIMARY KEY (dish_id),
    CONSTRAINT uq_dish_name     UNIQUE (name),
    CONSTRAINT fk_dish_category FOREIGN KEY (dish_category_id)
        REFERENCES dish_category (dish_category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX ix_dish_category ON dish (dish_category_id, active);

CREATE TABLE dish_modifier (
    dish_modifier_id BIGINT        NOT NULL AUTO_INCREMENT,
    dish_id          BIGINT        NOT NULL,
    name             VARCHAR(60)   NOT NULL,
    extra_price      DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    active           BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_dish_modifier      PRIMARY KEY (dish_modifier_id),
    CONSTRAINT uq_dish_modifier_name UNIQUE (dish_id, name),
    CONSTRAINT fk_dish_modifier_dish FOREIGN KEY (dish_id) REFERENCES dish (dish_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE combo (
    combo_id    BIGINT        NOT NULL AUTO_INCREMENT,
    name        VARCHAR(80)   NOT NULL,
    description VARCHAR(255)  NULL,
    combo_price DECIMAL(12,2) NOT NULL,
    active      BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_combo      PRIMARY KEY (combo_id),
    CONSTRAINT uq_combo_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE combo_item (
    combo_item_id BIGINT NOT NULL AUTO_INCREMENT,
    combo_id      BIGINT NOT NULL,
    dish_id       BIGINT NOT NULL,
    quantity      INT    NOT NULL DEFAULT 1,
    CONSTRAINT pk_combo_item       PRIMARY KEY (combo_item_id),
    CONSTRAINT uq_combo_item_dish  UNIQUE (combo_id, dish_id),
    CONSTRAINT fk_combo_item_combo FOREIGN KEY (combo_id) REFERENCES combo (combo_id),
    CONSTRAINT fk_combo_item_dish  FOREIGN KEY (dish_id)  REFERENCES dish (dish_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE recipe (
    recipe_id        BIGINT   NOT NULL AUTO_INCREMENT,
    dish_id          BIGINT   NULL,
    dish_modifier_id BIGINT   NULL,
    version          INT      NOT NULL DEFAULT 1,
    effective_from   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    effective_to     DATETIME NULL,
    current_flag     BOOLEAN  NULL,
    created_by       BIGINT   NOT NULL,
    CONSTRAINT pk_recipe             PRIMARY KEY (recipe_id),
    CONSTRAINT uq_recipe_dish        UNIQUE (dish_id, current_flag),
    CONSTRAINT uq_recipe_modifier    UNIQUE (dish_modifier_id, current_flag),
    CONSTRAINT ck_recipe_owner       CHECK ((dish_id IS NULL) <> (dish_modifier_id IS NULL)),
    CONSTRAINT ck_recipe_flag        CHECK (current_flag IS NULL OR current_flag = TRUE),
    CONSTRAINT fk_recipe_dish        FOREIGN KEY (dish_id)          REFERENCES dish (dish_id),
    CONSTRAINT fk_recipe_modifier    FOREIGN KEY (dish_modifier_id) REFERENCES dish_modifier (dish_modifier_id),
    CONSTRAINT fk_recipe_created_by  FOREIGN KEY (created_by)       REFERENCES app_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE recipe_item (
    recipe_item_id BIGINT        NOT NULL AUTO_INCREMENT,
    recipe_id      BIGINT        NOT NULL,
    supply_id      BIGINT        NOT NULL,
    quantity       DECIMAL(12,3) NOT NULL,
    CONSTRAINT pk_recipe_item        PRIMARY KEY (recipe_item_id),
    CONSTRAINT uq_recipe_item_supply UNIQUE (recipe_id, supply_id),
    CONSTRAINT fk_recipe_item_recipe FOREIGN KEY (recipe_id) REFERENCES recipe (recipe_id),
    CONSTRAINT fk_recipe_item_supply FOREIGN KEY (supply_id) REFERENCES supply (supply_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- ORDERING
-- =============================================================================

CREATE TABLE table_account (
    table_account_id       BIGINT      NOT NULL AUTO_INCREMENT,
    restaurant_table_id    BIGINT      NOT NULL,
    waiter_id              BIGINT      NOT NULL,
    guest_count            INT         NOT NULL DEFAULT 1,
    status                 VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    merged_into_account_id BIGINT      NULL,
    cancellation_reason    VARCHAR(255) NULL,
    opened_at              DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at              DATETIME    NULL,
    CONSTRAINT pk_table_account        PRIMARY KEY (table_account_id),
    CONSTRAINT fk_table_account_table  FOREIGN KEY (restaurant_table_id)
        REFERENCES restaurant_table (restaurant_table_id),
    CONSTRAINT fk_table_account_waiter FOREIGN KEY (waiter_id) REFERENCES app_user (user_id),
    CONSTRAINT fk_table_account_merged FOREIGN KEY (merged_into_account_id)
        REFERENCES table_account (table_account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX ix_table_account_table  ON table_account (restaurant_table_id, status);
CREATE INDEX ix_table_account_waiter ON table_account (waiter_id, opened_at);

CREATE TABLE account_split (
    account_split_id BIGINT        NOT NULL AUTO_INCREMENT,
    table_account_id BIGINT        NOT NULL,
    mode             VARCHAR(30)   NOT NULL,
    label            VARCHAR(40)   NULL,
    share_amount     DECIMAL(12,2) NULL,
    created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_account_split         PRIMARY KEY (account_split_id),
    CONSTRAINT fk_account_split_account FOREIGN KEY (table_account_id)
        REFERENCES table_account (table_account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX ix_account_split_account ON account_split (table_account_id);

CREATE TABLE order_ticket (
    order_ticket_id  BIGINT   NOT NULL AUTO_INCREMENT,
    table_account_id BIGINT   NOT NULL,
    waiter_id        BIGINT   NOT NULL,
    submitted_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_order_ticket         PRIMARY KEY (order_ticket_id),
    CONSTRAINT fk_order_ticket_account FOREIGN KEY (table_account_id)
        REFERENCES table_account (table_account_id),
    CONSTRAINT fk_order_ticket_waiter  FOREIGN KEY (waiter_id) REFERENCES app_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX ix_order_ticket_submitted ON order_ticket (submitted_at);
CREATE INDEX ix_order_ticket_account   ON order_ticket (table_account_id);

CREATE TABLE order_item (
    order_item_id       BIGINT        NOT NULL AUTO_INCREMENT,
    order_ticket_id     BIGINT        NOT NULL,
    dish_id             BIGINT        NOT NULL,
    combo_id            BIGINT        NULL,
    account_split_id    BIGINT        NULL,
    quantity            INT           NOT NULL,
    unit_price          DECIMAL(12,2) NOT NULL,
    unit_cost           DECIMAL(12,2) NOT NULL,
    note                VARCHAR(255)  NULL,
    status              VARCHAR(30)   NOT NULL DEFAULT 'RECEIVED',
    submitted_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ready_at            DATETIME      NULL,
    delivered_at        DATETIME      NULL,
    cancelled_by        BIGINT        NULL,
    cancellation_reason VARCHAR(255)  NULL,
    CONSTRAINT pk_order_item           PRIMARY KEY (order_item_id),
    CONSTRAINT fk_order_item_ticket    FOREIGN KEY (order_ticket_id)
        REFERENCES order_ticket (order_ticket_id),
    CONSTRAINT fk_order_item_dish      FOREIGN KEY (dish_id)          REFERENCES dish (dish_id),
    CONSTRAINT fk_order_item_combo     FOREIGN KEY (combo_id)         REFERENCES combo (combo_id),
    CONSTRAINT fk_order_item_split     FOREIGN KEY (account_split_id)
        REFERENCES account_split (account_split_id),
    CONSTRAINT fk_order_item_cancelled FOREIGN KEY (cancelled_by)     REFERENCES app_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX ix_order_item_ticket ON order_item (order_ticket_id, status);
CREATE INDEX ix_order_item_status ON order_item (status, submitted_at);
CREATE INDEX ix_order_item_dish   ON order_item (dish_id, submitted_at);
CREATE INDEX ix_order_item_split  ON order_item (account_split_id);

CREATE TABLE order_item_modifier (
    order_item_modifier_id BIGINT        NOT NULL AUTO_INCREMENT,
    order_item_id          BIGINT        NOT NULL,
    dish_modifier_id       BIGINT        NOT NULL,
    extra_price            DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    CONSTRAINT pk_order_item_modifier      PRIMARY KEY (order_item_modifier_id),
    CONSTRAINT uq_order_item_modifier      UNIQUE (order_item_id, dish_modifier_id),
    CONSTRAINT fk_order_item_modifier_item FOREIGN KEY (order_item_id)
        REFERENCES order_item (order_item_id),
    CONSTRAINT fk_order_item_modifier_mod  FOREIGN KEY (dish_modifier_id)
        REFERENCES dish_modifier (dish_modifier_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- CASHBOX
-- =============================================================================

CREATE TABLE cash_shift (
    cash_shift_id   BIGINT        NOT NULL AUTO_INCREMENT,
    cashier_id      BIGINT        NOT NULL,
    opening_balance DECIMAL(12,2) NOT NULL,
    expected_cash   DECIMAL(12,2) NULL,
    counted_cash    DECIMAL(12,2) NULL,
    difference      DECIMAL(12,2) NULL,
    status          VARCHAR(30)   NOT NULL DEFAULT 'OPEN',
    opened_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at       DATETIME      NULL,
    CONSTRAINT pk_cash_shift         PRIMARY KEY (cash_shift_id),
    CONSTRAINT fk_cash_shift_cashier FOREIGN KEY (cashier_id) REFERENCES app_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX ix_cash_shift_cashier ON cash_shift (cashier_id, status, opened_at);

CREATE TABLE cash_movement (
    cash_movement_id BIGINT        NOT NULL AUTO_INCREMENT,
    cash_shift_id    BIGINT        NOT NULL,
    movement_type    VARCHAR(30)   NOT NULL,
    amount           DECIMAL(12,2) NOT NULL,
    invoice_id       BIGINT        NULL,
    created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_cash_movement       PRIMARY KEY (cash_movement_id),
    CONSTRAINT fk_cash_movement_shift FOREIGN KEY (cash_shift_id)
        REFERENCES cash_shift (cash_shift_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX ix_cash_movement_shift ON cash_movement (cash_shift_id, created_at);

-- =============================================================================
-- BILLING
-- =============================================================================

CREATE TABLE invoice_sequence (
    sequence_id BIGINT NOT NULL,
    next_number BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT pk_invoice_sequence        PRIMARY KEY (sequence_id),
    CONSTRAINT ck_invoice_sequence_single CHECK (sequence_id = 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE invoice (
    invoice_id          BIGINT        NOT NULL AUTO_INCREMENT,
    invoice_number      BIGINT        NOT NULL,
    table_account_id    BIGINT        NOT NULL,
    account_split_id    BIGINT        NULL,
    restaurant_table_id BIGINT        NOT NULL,
    cash_shift_id       BIGINT        NOT NULL,
    cashier_id          BIGINT        NOT NULL,
    waiter_id           BIGINT        NOT NULL,
    customer_id         BIGINT        NULL,
    subtotal            DECIMAL(12,2) NOT NULL,
    discount_amount     DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    tax_amount          DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    tip_amount          DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    total               DECIMAL(12,2) NOT NULL,
    redeemed_points     INT           NOT NULL DEFAULT 0,
    accrued_points      INT           NOT NULL DEFAULT 0,
    status              VARCHAR(30)   NOT NULL DEFAULT 'ISSUED',
    void_reason         VARCHAR(255)  NULL,
    issued_at           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    voided_at           DATETIME      NULL,
    CONSTRAINT pk_invoice          PRIMARY KEY (invoice_id),
    CONSTRAINT uq_invoice_number   UNIQUE (invoice_number),
    CONSTRAINT uq_invoice_split    UNIQUE (account_split_id),
    CONSTRAINT fk_invoice_account  FOREIGN KEY (table_account_id)
        REFERENCES table_account (table_account_id),
    CONSTRAINT fk_invoice_split    FOREIGN KEY (account_split_id)
        REFERENCES account_split (account_split_id),
    CONSTRAINT fk_invoice_table    FOREIGN KEY (restaurant_table_id)
        REFERENCES restaurant_table (restaurant_table_id),
    CONSTRAINT fk_invoice_shift    FOREIGN KEY (cash_shift_id) REFERENCES cash_shift (cash_shift_id),
    CONSTRAINT fk_invoice_cashier  FOREIGN KEY (cashier_id)    REFERENCES app_user (user_id),
    CONSTRAINT fk_invoice_waiter   FOREIGN KEY (waiter_id)     REFERENCES app_user (user_id),
    CONSTRAINT fk_invoice_customer FOREIGN KEY (customer_id)   REFERENCES customer (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX ix_invoice_shift    ON invoice (cash_shift_id);
CREATE INDEX ix_invoice_issued   ON invoice (issued_at, status);
CREATE INDEX ix_invoice_waiter   ON invoice (waiter_id, issued_at);
CREATE INDEX ix_invoice_customer ON invoice (customer_id, issued_at);

CREATE TABLE invoice_payment (
    invoice_payment_id BIGINT        NOT NULL AUTO_INCREMENT,
    invoice_id         BIGINT        NOT NULL,
    method             VARCHAR(30)   NOT NULL,
    amount             DECIMAL(12,2) NOT NULL,
    CONSTRAINT pk_invoice_payment         PRIMARY KEY (invoice_payment_id),
    CONSTRAINT fk_invoice_payment_invoice FOREIGN KEY (invoice_id) REFERENCES invoice (invoice_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX ix_invoice_payment_invoice ON invoice_payment (invoice_id);

CREATE TABLE service_rating (
    service_rating_id BIGINT       NOT NULL AUTO_INCREMENT,
    invoice_id        BIGINT       NOT NULL,
    waiter_id         BIGINT       NOT NULL,
    score             INT          NOT NULL,
    comment_text      VARCHAR(255) NULL,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_service_rating         PRIMARY KEY (service_rating_id),
    CONSTRAINT uq_service_rating_invoice UNIQUE (invoice_id),
    CONSTRAINT ck_service_rating_score   CHECK (score BETWEEN 1 AND 5),
    CONSTRAINT fk_service_rating_invoice FOREIGN KEY (invoice_id) REFERENCES invoice (invoice_id),
    CONSTRAINT fk_service_rating_waiter  FOREIGN KEY (waiter_id)  REFERENCES app_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX ix_service_rating_waiter ON service_rating (waiter_id, created_at);

-- =============================================================================
-- DINING
-- =============================================================================

CREATE TABLE reservation (
    reservation_id      BIGINT       NOT NULL AUTO_INCREMENT,
    customer_id         BIGINT       NOT NULL,
    restaurant_table_id BIGINT       NOT NULL,
    table_account_id    BIGINT       NULL,
    reserved_at         DATETIME     NOT NULL,
    guest_count         INT          NOT NULL,
    status              VARCHAR(30)  NOT NULL DEFAULT 'BOOKED',
    cancellation_reason VARCHAR(30)  NULL,
    note                VARCHAR(255) NULL,
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_reservation          PRIMARY KEY (reservation_id),
    CONSTRAINT fk_reservation_customer FOREIGN KEY (customer_id) REFERENCES customer (customer_id),
    CONSTRAINT fk_reservation_table    FOREIGN KEY (restaurant_table_id)
        REFERENCES restaurant_table (restaurant_table_id),
    CONSTRAINT fk_reservation_account  FOREIGN KEY (table_account_id)
        REFERENCES table_account (table_account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX ix_reservation_slot   ON reservation (reserved_at, status);
CREATE INDEX ix_reservation_table  ON reservation (restaurant_table_id, reserved_at);

CREATE TABLE waitlist_entry (
    waitlist_entry_id   BIGINT      NOT NULL AUTO_INCREMENT,
    customer_id         BIGINT      NOT NULL,
    guest_count         INT         NOT NULL,
    status              VARCHAR(30) NOT NULL DEFAULT 'WAITING',
    restaurant_table_id BIGINT      NULL,
    table_account_id    BIGINT      NULL,
    arrived_at          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    seated_at           DATETIME    NULL,
    CONSTRAINT pk_waitlist_entry          PRIMARY KEY (waitlist_entry_id),
    CONSTRAINT fk_waitlist_entry_customer FOREIGN KEY (customer_id) REFERENCES customer (customer_id),
    CONSTRAINT fk_waitlist_entry_table    FOREIGN KEY (restaurant_table_id)
        REFERENCES restaurant_table (restaurant_table_id),
    CONSTRAINT fk_waitlist_entry_account  FOREIGN KEY (table_account_id)
        REFERENCES table_account (table_account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX ix_waitlist_entry_queue ON waitlist_entry (status, arrived_at);

-- =============================================================================
-- FOREIGN KEYS DIFERIDAS
-- =============================================================================

ALTER TABLE stock_movement
    ADD CONSTRAINT fk_stock_movement_order_item FOREIGN KEY (order_item_id)
        REFERENCES order_item (order_item_id);

ALTER TABLE cash_movement
    ADD CONSTRAINT fk_cash_movement_invoice FOREIGN KEY (invoice_id)
        REFERENCES invoice (invoice_id);

ALTER TABLE loyalty_transaction
    ADD CONSTRAINT fk_loyalty_transaction_invoice FOREIGN KEY (invoice_id)
        REFERENCES invoice (invoice_id);
