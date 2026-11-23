-- Giai đoạn 3: tour, lịch trình, đợt khởi hành

-- Lịch trình là bảng riêng: tour giữ lịch trình mẫu, mỗi đợt khởi hành giữ bản sao của riêng mình
create table itineraries (
    id bigint auto_increment primary key
);

create table itinerary_days (
    id           bigint auto_increment primary key,
    itinerary_id bigint not null,
    day_number   integer not null check (day_number >= 1),
    title        varchar(200) not null,
    description  varchar(2000),
    -- Thay lịch trình thì Itinerary.replaceDays sửa ngày cũ tại chỗ, không chèn ngày mới trùng số trước khi xóa ngày cũ
    unique (itinerary_id, day_number),
    foreign key (itinerary_id) references itineraries (id) on delete cascade
);

create table itinerary_activities (
    id             bigint auto_increment primary key,
    day_id         bigint not null,
    sort_order     integer not null,
    start_time     time,
    title          varchar(200) not null,
    description    varchar(2000),
    destination_id bigint,         -- null: hoạt động không gắn điểm đến (ăn trưa, nghỉ...)
    foreign key (day_id) references itinerary_days (id) on delete cascade,
    foreign key (destination_id) references destinations (id)
);

create table tours (
    id              bigint auto_increment primary key,
    code            varchar(20) not null unique,
    name            varchar(200) not null,
    summary         varchar(500),
    description     varchar(5000),
    duration_days   integer not null check (duration_days >= 1),
    duration_nights integer not null check (duration_nights >= 0),
    status          varchar(20) not null check (status in ('DRAFT', 'ON_SALE', 'STOPPED')),
    featured        boolean not null default false,
    itinerary_id    bigint not null unique,
    created_at      datetime(6) not null,
    foreign key (itinerary_id) references itineraries (id)
);

create table tour_regions (
    tour_id   bigint not null,
    region_id bigint not null,
    primary key (tour_id, region_id),
    foreign key (tour_id) references tours (id) on delete cascade,
    foreign key (region_id) references regions (id)
);

create table tour_category_links (
    tour_id     bigint not null,
    category_id bigint not null,
    primary key (tour_id, category_id),
    foreign key (tour_id) references tours (id) on delete cascade,
    foreign key (category_id) references tour_categories (id)
);

create table tour_amenities (
    tour_id    bigint not null,
    amenity_id bigint not null,
    included   boolean not null,                               -- true: bao gồm trong giá; false: không bao gồm
    primary key (tour_id, amenity_id),
    foreign key (tour_id) references tours (id) on delete cascade,
    foreign key (amenity_id) references amenities (id)
);

create table tour_surcharges (
    tour_id      bigint not null,
    surcharge_id bigint not null,
    primary key (tour_id, surcharge_id),
    foreign key (tour_id) references tours (id) on delete cascade,
    foreign key (surcharge_id) references surcharges (id)
);

create table tour_images (
    tour_id    bigint not null,
    sort_order integer not null,
    url        varchar(500) not null,
    caption    varchar(200),
    cover      boolean not null,
    primary key (tour_id, sort_order),
    -- "Đúng một ảnh đại diện" do Tour.replaceImages kiểm tra. Không đặt unique index vì khi đổi ảnh đại diện,
    -- Hibernate cập nhật từng dòng nên có lúc tạm thời hai dòng cùng là đại diện.
    foreign key (tour_id) references tours (id) on delete cascade
);

create table departures (
    id               bigint auto_increment primary key,
    tour_id          bigint not null,
    code             varchar(40) not null unique,
    departure_at     datetime(6) not null,                        -- giờ Việt Nam
    return_at        datetime(6) not null,
    booking_deadline datetime(6) not null,
    capacity         integer not null check (capacity > 0),
    booked_seats     integer not null default 0 check (booked_seats >= 0),
    status           varchar(20) not null check (status in ('OPEN', 'CLOSED', 'CANCELLED')),
    itinerary_id     bigint not null unique,
    version          bigint not null default 0,                 -- khóa lạc quan: hai người sửa cùng lúc thì người sau bị báo
    check (booked_seats <= capacity),                           -- lớp bảo vệ cuối cùng chống bán vượt chỗ
    check (departure_at < return_at),
    check (booking_deadline <= departure_at),
    foreign key (tour_id) references tours (id),
    foreign key (itinerary_id) references itineraries (id)
);

create table departure_prices (
    departure_id      bigint not null,
    passenger_type_id bigint not null,
    price             bigint not null check (price >= 0),       -- đồng
    primary key (departure_id, passenger_type_id),
    foreign key (departure_id) references departures (id) on delete cascade,
    foreign key (passenger_type_id) references passenger_types (id)
);

create table departure_pickups (
    departure_id    bigint not null,
    pickup_point_id bigint not null,
    pickup_time     time not null,
    fee             bigint not null check (fee >= 0),           -- phí đón mỗi hành khách, đồng
    primary key (departure_id, pickup_point_id),
    foreign key (departure_id) references departures (id) on delete cascade,
    foreign key (pickup_point_id) references pickup_points (id)
);
