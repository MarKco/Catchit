UPDATE stop_times
SET arrival_time = REPLACE(arrival_time, '24:', '00:'), late_night = 1
WHERE arrival_time LIKE '24:%';

UPDATE stop_times
SET departure_time = REPLACE(departure_time, '24:', '00:'), late_night = 1
WHERE departure_time LIKE '24:%';

UPDATE stop_times
SET arrival_time = REPLACE(arrival_time, '25:', '01:'), late_night = 1
WHERE arrival_time LIKE '25:%';

UPDATE stop_times
SET departure_time = REPLACE(departure_time, '25:', '01:'), late_night = 1
WHERE departure_time LIKE '25:%';

UPDATE stop_times
SET arrival_time = REPLACE(arrival_time, '26:', '02:'), late_night = 1
WHERE arrival_time LIKE '26:%';

UPDATE stop_times
SET departure_time = REPLACE(departure_time, '26:', '02:'), late_night = 1
WHERE departure_time LIKE '26:%';

UPDATE stop_times
SET arrival_time = REPLACE(arrival_time, '27:', '03:'), late_night = 1
WHERE arrival_time LIKE '27:%';

UPDATE stop_times
SET departure_time = REPLACE(departure_time, '27:', '03:'), late_night = 1
WHERE departure_time LIKE '27:%';

UPDATE stop_times
SET arrival_time = REPLACE(arrival_time, '28:', '04:'), late_night = 1
WHERE arrival_time LIKE '28:%';

UPDATE stop_times
SET departure_time = REPLACE(departure_time, '28:', '04:'), late_night = 1
WHERE departure_time LIKE '28:%';

UPDATE stop_times
SET arrival_time = REPLACE(arrival_time, '29:', '05:'), late_night = 1
WHERE arrival_time LIKE '29:%';

UPDATE stop_times
SET departure_time = REPLACE(departure_time, '29:', '05:'), late_night = 1
WHERE departure_time LIKE '29:%';

