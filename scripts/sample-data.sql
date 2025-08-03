INSERT INTO airport (id, code, name, city, country, timezone_id, created_at, updated_at) VALUES
(1, 'AKL', 'Auckland Airport', 'Auckland', 'New Zealand', 'Pacific/Auckland', NOW(), NOW()),
(2, 'WLG', 'Wellington Airport', 'Wellington', 'New Zealand', 'Pacific/Auckland', NOW(), NOW()),
(3, 'CHC', 'Christchurch Airport', 'Christchurch', 'New Zealand', 'Pacific/Auckland', NOW(), NOW()),
(4, 'SYD', 'Kingsford Smith Airport', 'Sydney', 'Australia', 'Australia/Sydney', NOW(), NOW()),
(5, 'MEL', 'Melbourne Airport', 'Melbourne', 'Australia', 'Australia/Melbourne', NOW(), NOW());

-- FLIGHTS (Same as existing)
INSERT INTO flight (id, flight_code, airline, origin_id, destination_id, created_at, updated_at) VALUES
(1, 'NZ101', 'ACME AIR', 1, 4, NOW(), NOW()),  -- AKL -> SYD
(2, 'NZ102', 'ACME AIR', 4, 1, NOW(), NOW()),  -- SYD -> AKL
(3, 'NZ405', 'ACME AIR', 1, 2, NOW(), NOW()),  -- AKL -> WLG
(4, 'NZ406', 'ACME AIR', 2, 1, NOW(), NOW()),  -- WLG -> AKL
(5, 'NZ549', 'ACME AIR', 1, 3, NOW(), NOW()),  -- AKL -> CHC
(6, 'NZ550', 'ACME AIR', 3, 1, NOW(), NOW()),  -- CHC -> AKL
(7, 'JQ201', 'Jetstar', 1, 5, NOW(), NOW()),         -- AKL -> MEL
(8, 'JQ202', 'Jetstar', 5, 1, NOW(), NOW());         -- MEL -> AKL

-- FLIGHT SCHEDULES (Simplified - 15 rows using all flight IDs and specified dates)
INSERT INTO flight_schedule (id, flight_id, departure_time, arrival_time, price, currency, total_seats, created_at, updated_at) VALUES

-- Flight ID 1: AKL -> SYD
(1, 1, '2025-01-01 09:00:00+13:00', '2025-01-01 11:30:00+11:00', 349.00, 'NZD', 180, NOW(), NOW()),
(2, 1, '2025-12-25 08:00:00+13:00', '2025-12-25 10:30:00+11:00', 299.00, 'NZD', 180, NOW(), NOW()),
(3, 1, '2026-01-01 14:00:00+13:00', '2026-01-01 16:30:00+11:00', 329.00, 'NZD', 180, NOW(), NOW()),

-- Flight ID 2: SYD -> AKL
(5, 2, '2012-12-25 11:00:00+11:00', '2012-12-25 16:00:00+13:00', 199.00, 'NZD', 180, NOW(), NOW()),
(6, 2, '2025-12-25 12:00:00+11:00', '2025-12-25 17:00:00+13:00', 299.00, 'NZD', 180, NOW(), NOW()),

-- Flight ID 3: AKL -> WLG
(7, 3, '2025-12-25 07:00:00+13:00', '2025-12-25 08:15:00+13:00', 89.00, 'NZD', 120, NOW(), NOW()),
(8, 3, '2026-01-01 08:00:00+13:00', '2026-01-01 09:15:00+13:00', 109.00, 'NZD', 120, NOW(), NOW()),

-- Flight ID 4: WLG -> AKL
(9, 4, '2025-12-25 09:00:00+13:00', '2025-12-25 10:15:00+13:00', 89.00, 'NZD', 120, NOW(), NOW()),

-- Flight ID 5: AKL -> CHC
(10, 5, '2025-12-25 06:30:00+13:00', '2025-12-25 08:00:00+13:00', 129.00, 'NZD', 150, NOW(), NOW()),
(11, 5, '2026-01-01 07:00:00+13:00', '2026-01-01 08:30:00+13:00', 149.00, 'NZD', 150, NOW(), NOW()),

-- Flight ID 6: CHC -> AKL
(12, 6, '2025-12-25 16:00:00+13:00', '2025-12-25 17:30:00+13:00', 129.00, 'NZD', 150, NOW(), NOW()),

-- Flight ID 7: AKL -> MEL (Jetstar)
(13, 7, '2025-12-25 10:00:00+13:00', '2025-12-25 12:00:00+11:00', 249.00, 'NZD', 180, NOW(), NOW()),
(14, 7, '2026-01-01 11:00:00+13:00', '2026-01-01 13:00:00+11:00', 289.00, 'NZD', 180, NOW(), NOW()),

-- Flight ID 8: MEL -> AKL (Jetstar)
(15, 8, '2025-12-25 13:30:00+11:00', '2025-12-25 18:30:00+13:00', 249.00, 'NZD', 180, NOW(), NOW());

-- SEATS (Simplified for 15 flight schedules)
INSERT INTO seat (id, seat_number, schedule_id, status, created_at, updated_at)
SELECT
    ROW_NUMBER() OVER(PARTITION BY schedule_id ORDER BY seat_pos) + (schedule_id * 50) as id,
    CASE
        WHEN (ROW_NUMBER() OVER(PARTITION BY schedule_id ORDER BY seat_pos) - 1) % 3 = 0 THEN CONCAT(((ROW_NUMBER() OVER(PARTITION BY schedule_id ORDER BY seat_pos) - 1) / 3) + 1, 'A')
        WHEN (ROW_NUMBER() OVER(PARTITION BY schedule_id ORDER BY seat_pos) - 1) % 3 = 1 THEN CONCAT(((ROW_NUMBER() OVER(PARTITION BY schedule_id ORDER BY seat_pos) - 1) / 3) + 1, 'B')
        ELSE CONCAT(((ROW_NUMBER() OVER(PARTITION BY schedule_id ORDER BY seat_pos) - 1) / 3) + 1, 'C')
    END as seat_number,
    schedule_id,
    CASE WHEN RANDOM() < 0.2 THEN 'BOOKED' ELSE 'AVAILABLE' END as status,
    NOW() as created_at,
    NOW() as updated_at
FROM
    generate_series(1, 15) as seat_pos,
    (SELECT id as schedule_id FROM flight_schedule) as schedules;