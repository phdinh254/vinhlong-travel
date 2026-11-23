-- Giai đoạn 6: yêu cầu hủy, hoàn tiền, chuyển đợt

create table cancellation_requests (
    id               bigint auto_increment primary key,
    booking_id       bigint not null,
    scope            varchar(20) not null check (scope in ('FULL', 'PARTIAL')),
    status           varchar(20) not null check (status in ('PENDING', 'APPROVED', 'REJECTED')),
    reason           varchar(1000),
    penalty_percent  integer not null check (penalty_percent between 0 and 100),   -- chốt lúc khách gửi yêu cầu
    penalty_amount   bigint not null check (penalty_amount >= 0),
    estimated_refund bigint not null check (estimated_refund >= 0),
    requested_at     datetime(6) not null,
    requested_by     bigint,            -- null: khách vãng lai
    decided_at       datetime(6),
    decided_by       bigint,            -- null khi hệ thống tự duyệt/tự đóng
    decision_note    varchar(500),
    -- Bằng booking_id khi yêu cầu đang chờ, null khi đã xử lý; unique trên cột này = mỗi đơn chỉ một yêu cầu đang chờ
    -- (MySQL không có index có điều kiện như PostgreSQL; unique bỏ qua null)
    pending_booking_id bigint as (case when status = 'PENDING' then booking_id end) virtual,
    unique key cancellation_requests_one_pending (pending_booking_id),
    foreign key (booking_id) references bookings (id),
    foreign key (requested_by) references accounts (id),
    foreign key (decided_by) references accounts (id)
);

create table cancellation_request_passengers (
    request_id   bigint not null,
    passenger_id bigint not null,
    primary key (request_id, passenger_id),
    foreign key (request_id) references cancellation_requests (id) on delete cascade,
    foreign key (passenger_id) references booking_passengers (id)
);

create table refunds (
    id                      bigint auto_increment primary key,
    booking_id              bigint not null,
    cancellation_request_id bigint,
    amount                  bigint not null check (amount > 0),
    method                  varchar(20) not null check (method in ('BANK_TRANSFER', 'CASH')),
    transaction_no          varchar(50),
    proof_url               varchar(500),
    note                    varchar(500),
    refunded_at             datetime(6) not null,
    refunded_by             bigint not null,
    foreign key (booking_id) references bookings (id),
    foreign key (cancellation_request_id) references cancellation_requests (id),
    foreign key (refunded_by) references accounts (id)
);
create index refunds_booking_idx on refunds (booking_id);

create table departure_transfers (
    id                bigint auto_increment primary key,
    booking_id        bigint not null,
    from_departure_id bigint not null,
    to_departure_id   bigint not null,
    price_difference  bigint not null,                            -- tổng mới - tổng cũ (âm: rẻ hơn)
    reason            varchar(500),
    transferred_at    datetime(6) not null,
    transferred_by    bigint not null,
    foreign key (booking_id) references bookings (id),
    foreign key (from_departure_id) references departures (id),
    foreign key (to_departure_id) references departures (id),
    foreign key (transferred_by) references accounts (id)
);
create index departure_transfers_booking_idx on departure_transfers (booking_id);
