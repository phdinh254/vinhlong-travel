-- Giai đoạn 4: đơn đặt tour, hành khách, phụ thu đã chọn, lịch sử trạng thái

create table bookings (
    id                     bigserial primary key,
    code                   varchar(20) not null unique,          -- VLyyMMdd + 5 ký tự ngẫu nhiên
    departure_id           bigint not null references departures (id),
    account_id             bigint references accounts (id),      -- null: khách vãng lai
    contact_name           varchar(150) not null,
    contact_phone          varchar(20) not null,
    contact_email          varchar(150),
    note                   varchar(1000),
    ticket_total           bigint not null check (ticket_total >= 0),
    pickup_total           bigint not null check (pickup_total >= 0),
    surcharge_total        bigint not null check (surcharge_total >= 0),
    total_amount           bigint not null check (total_amount >= 0),
    paid_amount            bigint not null default 0 check (paid_amount >= 0),   -- số tiền thực thu (đã trừ hoàn)
    seats                  integer not null check (seats >= 0),
    status                 varchar(20) not null check (status in ('PENDING_PAYMENT', 'DEPOSITED', 'PAID', 'COMPLETED', 'CANCELLED', 'EXPIRED')),
    hold_until             timestamp,                             -- giờ Việt Nam; hết giờ mà chưa trả tiền thì trả chỗ
    cancellation_policy_id bigint references cancellation_policies (id),   -- chính sách hủy chốt lúc đặt
    created_at             timestamp not null
);
create index bookings_hold_idx on bookings (status, hold_until);
create index bookings_account_idx on bookings (account_id);
create index bookings_departure_idx on bookings (departure_id);

create table booking_passengers (
    id                bigserial primary key,
    booking_id        bigint not null references bookings (id) on delete cascade,
    full_name         varchar(150) not null,
    birth_date        date not null,
    gender            varchar(10) check (gender in ('MALE', 'FEMALE', 'OTHER')),
    phone             varchar(20),
    id_number         varchar(30),                                -- CCCD/hộ chiếu
    passenger_type_id bigint not null references passenger_types (id),
    price             bigint not null check (price >= 0),         -- giá chốt lúc đặt
    pickup_point_id   bigint references pickup_points (id),
    pickup_time       time,
    pickup_fee        bigint not null default 0 check (pickup_fee >= 0),
    status            varchar(20) not null check (status in ('ACTIVE', 'CANCELLED'))
);

create table booking_surcharges (
    booking_id   bigint not null references bookings (id) on delete cascade,
    surcharge_id bigint not null references surcharges (id),
    name         varchar(150) not null,                           -- chốt tên và giá lúc đặt
    method       varchar(20) not null,
    quantity     integer not null check (quantity >= 1),
    unit_price   bigint not null check (unit_price >= 0),
    amount       bigint not null check (amount >= 0),
    primary key (booking_id, surcharge_id)
);

create table booking_status_history (
    id          bigserial primary key,
    booking_id  bigint not null references bookings (id) on delete cascade,
    from_status varchar(20),
    to_status   varchar(20) not null,
    changed_at  timestamp not null,
    changed_by  bigint references accounts (id),                  -- null: hệ thống tự động
    reason      varchar(500)
);
