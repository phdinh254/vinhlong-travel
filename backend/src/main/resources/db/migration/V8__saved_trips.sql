-- Giai đoạn 7: lịch trình khách lưu lại từ chức năng gợi ý lịch trình
create table saved_trips (
    id         bigserial primary key,
    account_id bigint not null references accounts (id) on delete cascade,
    title      varchar(150) not null,
    request    text not null,   -- tham số khách đã nhập (JSON)
    plan       text not null,   -- lịch trình gợi ý lúc lưu (JSON); admin sửa điểm đến sau này không làm đổi lịch đã lưu
    created_at timestamp not null
);

create index saved_trips_account_idx on saved_trips (account_id, created_at desc);
