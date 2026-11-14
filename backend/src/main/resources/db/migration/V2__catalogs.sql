-- Giai đoạn 2: các danh mục dùng chung cho tour, đặt tour và gợi ý lịch trình

create table regions (
    id   bigserial primary key,
    name varchar(100) not null unique
);

-- Loại hình tour, đồng thời là "thẻ sở thích" của điểm đến (sinh thái, văn hóa...)
create table tour_categories (
    id   bigserial primary key,
    name varchar(100) not null unique
);

-- Dịch vụ có/không bao gồm trong tour (xe đưa đón, hướng dẫn viên...)
create table amenities (
    id   bigserial primary key,
    name varchar(100) not null unique
);

create table passenger_types (
    id      bigserial primary key,
    name    varchar(100) not null unique,
    min_age integer not null check (min_age >= 0),
    max_age integer not null,
    seats   integer not null check (seats between 0 and 2),   -- số chỗ chiếm trên xe (em bé ngồi lòng: 0)
    check (min_age <= max_age)
);

create table pickup_points (
    id      bigserial primary key,
    name    varchar(150) not null unique,
    address varchar(300),
    active  boolean not null default true
);

create table surcharges (
    id          bigserial primary key,
    name        varchar(150) not null unique,
    method      varchar(20) not null check (method in ('PER_PERSON', 'PER_BOOKING', 'PER_UNIT')),
    unit_price  bigint not null check (unit_price >= 0),       -- đồng
    description varchar(500),
    active      boolean not null default true
);

create table cancellation_policies (
    id             bigserial primary key,
    name           varchar(150) not null,
    effective_from date not null,
    description    varchar(1000)
);

create table cancellation_policy_tiers (
    policy_id         bigint not null references cancellation_policies (id) on delete cascade,
    days_before_from  integer not null,
    days_before_to    integer not null,
    penalty_percent   integer not null check (penalty_percent between 0 and 100),
    check (days_before_from <= days_before_to)
);

create table destinations (
    id                 bigserial primary key,
    code               varchar(20) not null unique,
    name               varchar(150) not null,
    region_id          bigint not null references regions (id),
    address            varchar(300),
    latitude           double precision not null check (latitude between -90 and 90),
    longitude          double precision not null check (longitude between -180 and 180),
    coordinate_quality varchar(20) not null,
    open_time          time not null,
    close_time         time not null,
    hours_quality      varchar(20) not null,
    ticket_price_adult bigint check (ticket_price_adult >= 0),   -- đồng; null = chưa rõ giá
    ticket_price_child bigint check (ticket_price_child >= 0),
    price_quality      varchar(20) not null,
    visit_minutes      integer not null check (visit_minutes > 0),
    popularity         integer not null check (popularity between 1 and 5),
    description        varchar(2000),
    sources            varchar(500),
    active             boolean not null default true,
    check (open_time < close_time)
);

create table destination_categories (
    destination_id bigint not null references destinations (id) on delete cascade,
    category_id    bigint not null references tour_categories (id),
    primary key (destination_id, category_id)
);

create table destination_closed_days (
    destination_id bigint not null references destinations (id) on delete cascade,
    day_of_week    varchar(10) not null,
    primary key (destination_id, day_of_week)
);

-- Dữ liệu danh mục ban đầu
insert into regions (name) values ('Vĩnh Long (cũ)'), ('Bến Tre (cũ)'), ('Trà Vinh (cũ)');

insert into tour_categories (name) values
    ('Sinh thái'), ('Văn hóa'), ('Tâm linh'), ('Lịch sử'), ('Làng nghề'),
    ('Biển'), ('Chợ & ẩm thực'), ('Vui chơi giải trí'), ('Khác');

insert into passenger_types (name, min_age, max_age, seats) values
    ('Người lớn', 12, 120, 1),
    ('Trẻ em', 5, 11, 1),
    ('Em bé', 0, 4, 0);
