-- Giai đoạn 6: yêu cầu hủy, hoàn tiền, chuyển đợt

create table cancellation_requests (
    id               bigserial primary key,
    booking_id       bigint not null references bookings (id),
    scope            varchar(20) not null check (scope in ('FULL', 'PARTIAL')),
    status           varchar(20) not null check (status in ('PENDING', 'APPROVED', 'REJECTED')),
    reason           varchar(1000),
    penalty_percent  integer not null check (penalty_percent between 0 and 100),   -- chốt lúc khách gửi yêu cầu
    penalty_amount   bigint not null check (penalty_amount >= 0),
    estimated_refund bigint not null check (estimated_refund >= 0),
    requested_at     timestamp not null,
    requested_by     bigint references accounts (id),            -- null: khách vãng lai
    decided_at       timestamp,
    decided_by       bigint references accounts (id),            -- null khi hệ thống tự duyệt/tự đóng
    decision_note    varchar(500)
);
-- Mỗi đơn có nhiều nhất một yêu cầu hủy đang chờ duyệt
create unique index cancellation_requests_one_pending on cancellation_requests (booking_id) where status = 'PENDING';

create table cancellation_request_passengers (
    request_id   bigint not null references cancellation_requests (id) on delete cascade,
    passenger_id bigint not null references booking_passengers (id),
    primary key (request_id, passenger_id)
);

create table refunds (
    id                      bigserial primary key,
    booking_id              bigint not null references bookings (id),
    cancellation_request_id bigint references cancellation_requests (id),
    amount                  bigint not null check (amount > 0),
    method                  varchar(20) not null check (method in ('BANK_TRANSFER', 'CASH')),
    transaction_no          varchar(50),
    proof_url               varchar(500),
    note                    varchar(500),
    refunded_at             timestamp not null,
    refunded_by             bigint not null references accounts (id)
);
create index refunds_booking_idx on refunds (booking_id);

create table departure_transfers (
    id                bigserial primary key,
    booking_id        bigint not null references bookings (id),
    from_departure_id bigint not null references departures (id),
    to_departure_id   bigint not null references departures (id),
    price_difference  bigint not null,                            -- tổng mới - tổng cũ (âm: rẻ hơn)
    reason            varchar(500),
    transferred_at    timestamp not null,
    transferred_by    bigint not null references accounts (id)
);
create index departure_transfers_booking_idx on departure_transfers (booking_id);
