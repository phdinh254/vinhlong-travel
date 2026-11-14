-- Giai đoạn 5: thanh toán (VNPay + xác nhận thủ công)

-- Số tiền giữ lại khi đơn bị hủy (phí phạt, giai đoạn 6). Đơn đang hiệu lực giữ lại toàn bộ tổng tiền.
alter table bookings add column cancellation_fee bigint not null default 0 check (cancellation_fee >= 0);

create table payments (
    id             bigserial primary key,
    booking_id     bigint not null references bookings (id),
    amount         bigint not null check (amount > 0),                 -- đồng
    method         varchar(20) not null check (method in ('VNPAY', 'BANK_TRANSFER', 'CASH')),
    status         varchar(20) not null check (status in ('PENDING', 'SUCCEEDED', 'FAILED')),
    reference      varchar(100) unique,                                -- vnp_TxnRef gửi sang VNPay
    transaction_no varchar(50),                                        -- mã giao dịch của VNPay / ngân hàng
    bank_code      varchar(20),
    proof_url      varchar(500),                                       -- ảnh chứng từ khi xác nhận thủ công
    note           varchar(500),
    confirmed_by   bigint references accounts (id),                    -- admin xác nhận thủ công
    paid_at        timestamp,
    created_at     timestamp not null,
    raw_response   varchar(2000)                                       -- dữ liệu VNPay gửi về, để tra soát
);
create index payments_booking_idx on payments (booking_id);
