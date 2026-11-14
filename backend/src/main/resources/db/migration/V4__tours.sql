-- Giai đoạn 3: tour, lịch trình, đợt khởi hành

-- Lịch trình là bảng riêng: tour giữ lịch trình mẫu, mỗi đợt khởi hành giữ bản sao của riêng mình
create table itineraries (
    id bigserial primary key
);

create table itinerary_days (
    id           bigserial primary key,
    itinerary_id bigint not null references itineraries (id) on delete cascade,
    day_number   integer not null check (day_number >= 1),
    title        varchar(200) not null,
    description  varchar(2000),
    -- Kiểm tra lúc commit: khi thay cả lịch trình, Hibernate chèn ngày mới trước rồi mới xóa ngày cũ
    unique (itinerary_id, day_number) deferrable initially deferred
);

create table itinerary_activities (
    id             bigserial primary key,
    day_id         bigint not null references itinerary_days (id) on delete cascade,
    sort_order     integer not null,
    start_time     time,
    title          varchar(200) not null,
    description    varchar(2000),
    destination_id bigint references destinations (id)          -- null: hoạt động không gắn điểm đến (ăn trưa, nghỉ...)
);

create table tours (
    id              bigserial primary key,
    code            varchar(20) not null unique,
    name            varchar(200) not null,
    summary         varchar(500),
    description     varchar(5000),
    duration_days   integer not null check (duration_days >= 1),
    duration_nights integer not null check (duration_nights >= 0),
    status          varchar(20) not null check (status in ('DRAFT', 'ON_SALE', 'STOPPED')),
    featured        boolean not null default false,
    itinerary_id    bigint not null unique references itineraries (id),
    created_at      timestamptz not null
);

create table tour_regions (
    tour_id   bigint not null references tours (id) on delete cascade,
    region_id bigint not null references regions (id),
    primary key (tour_id, region_id)
);

create table tour_category_links (
    tour_id     bigint not null references tours (id) on delete cascade,
    category_id bigint not null references tour_categories (id),
    primary key (tour_id, category_id)
);

create table tour_amenities (
    tour_id    bigint not null references tours (id) on delete cascade,
    amenity_id bigint not null references amenities (id),
    included   boolean not null,                               -- true: bao gồm trong giá; false: không bao gồm
    primary key (tour_id, amenity_id)
);

create table tour_surcharges (
    tour_id      bigint not null references tours (id) on delete cascade,
    surcharge_id bigint not null references surcharges (id),
    primary key (tour_id, surcharge_id)
);

create table tour_images (
    tour_id    bigint not null references tours (id) on delete cascade,
    sort_order integer not null,
    url        varchar(500) not null,
    caption    varchar(200),
    cover      boolean not null,
    primary key (tour_id, sort_order)
    -- "Đúng một ảnh đại diện" do Tour.replaceImages kiểm tra. Không đặt unique index vì khi đổi ảnh đại diện,
    -- Hibernate cập nhật từng dòng nên có lúc tạm thời hai dòng cùng là đại diện.
);

create table departures (
    id               bigserial primary key,
    tour_id          bigint not null references tours (id),
    code             varchar(40) not null unique,
    departure_at     timestamp not null,                        -- giờ Việt Nam
    return_at        timestamp not null,
    booking_deadline timestamp not null,
    capacity         integer not null check (capacity > 0),
    booked_seats     integer not null default 0 check (booked_seats >= 0),
    status           varchar(20) not null check (status in ('OPEN', 'CLOSED', 'CANCELLED')),
    itinerary_id     bigint not null unique references itineraries (id),
    version          bigint not null default 0,                 -- khóa lạc quan: hai người sửa cùng lúc thì người sau bị báo
    check (booked_seats <= capacity),                           -- lớp bảo vệ cuối cùng chống bán vượt chỗ
    check (departure_at < return_at),
    check (booking_deadline <= departure_at)
);

create table departure_prices (
    departure_id      bigint not null references departures (id) on delete cascade,
    passenger_type_id bigint not null references passenger_types (id),
    price             bigint not null check (price >= 0),       -- đồng
    primary key (departure_id, passenger_type_id)
);

create table departure_pickups (
    departure_id    bigint not null references departures (id) on delete cascade,
    pickup_point_id bigint not null references pickup_points (id),
    pickup_time     time not null,
    fee             bigint not null check (fee >= 0),           -- phí đón mỗi hành khách, đồng
    primary key (departure_id, pickup_point_id)
);
