-- Giai đoạn 8: tin tức, cấu hình trang, tin nhắn liên hệ

create table articles (
    id              bigserial primary key,
    title           varchar(200) not null,
    slug            varchar(220) not null unique,   -- đường dẫn thân thiện, ví dụ le-hoi-ok-om-bok-2026
    summary         varchar(500),
    content         text not null,                  -- HTML do admin soạn
    cover_image_url varchar(500),
    category        varchar(20) not null,
    status          varchar(20) not null,
    view_count      bigint not null default 0,
    published_at    timestamp,                      -- lần đầu xuất bản; ẩn rồi xuất bản lại giữ ngày cũ
    created_at      timestamp not null,
    updated_at      timestamp not null
);

create index articles_published_idx on articles (status, published_at desc);

-- Mỗi dòng một mục cấu hình (hotline, địa chỉ, link mạng xã hội, trang chính sách...)
create table site_settings (
    setting_key varchar(40) primary key,
    value       text not null
);

create table contact_messages (
    id         bigserial primary key,
    full_name  varchar(150) not null,
    phone      varchar(15),
    email      varchar(150),
    subject    varchar(200),
    message    varchar(3000) not null,
    status     varchar(20) not null,
    created_at timestamp not null,
    handled_at timestamp,
    handled_by bigint references accounts (id),
    check (phone is not null or email is not null)
);

create index contact_messages_status_idx on contact_messages (status, created_at desc);
