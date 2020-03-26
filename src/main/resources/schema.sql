CREATE TABLE IF NOT EXISTS event
(
    id          integer primary key auto_increment,
    meetup_id   varchar(30),
    title       varchar(255),
    description varchar,
    price       double,
    date        datetime,
    duration    varchar(200),
    deadline    datetime,
    address     varchar(200),
    venue       varchar(200),
    url         varchar,
    status      varchar(30),
    type        varchar(30)
);

CREATE TABLE IF NOT EXISTS student
(
    id    integer primary key auto_increment,
    login varchar(200) unique,
    name  varchar(200),
    email varchar(100)
);

CREATE TABLE IF NOT EXISTS student_event
(
    student              integer,
    event                integer,
    submitted_summary    boolean,
    url                  varchar(600),
    time_of_submission   datetime,
    deadline             datetime,
    accepted             boolean,
    used_for_certificate boolean,
    primary key (student, event)
);