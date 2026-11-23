-- Giai đoạn 7: lịch trình khách lưu lại từ chức năng gợi ý lịch trình
create table saved_trips (
    id         bigint auto_increment primary key,
    account_id bigint not null,
    title      varchar(150) not null,
    request    mediumtext not null,   -- tham số khách đã nhập (JSON)
    plan       mediumtext not null,   -- lịch trình gợi ý lúc lưu (JSON); admin sửa điểm đến sau này không làm đổi lịch đã lưu
    created_at datetime(6) not null,
    foreign key (account_id) references accounts (id) on delete cascade
);

create index saved_trips_account_idx on saved_trips (account_id, created_at desc);
