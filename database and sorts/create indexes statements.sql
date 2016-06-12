CREATE INDEX calendar_ix1 ON calendar (start_date, end_date);
CREATE INDEX ix_calendar_dates_date ON calendar_dates (date);
CREATE INDEX ix_calendar_dates_service_id ON calendar_dates (service_id);
CREATE INDEX ix_calendar_service_id ON calendar (service_id);
CREATE INDEX ix_routes_agency_id ON routes (agency_id);
CREATE INDEX ix_routes_route_id ON routes (route_id);
CREATE INDEX ix_routes_route_type ON routes (route_type);
CREATE INDEX ix_stop_times_departure_time ON stop_times (departure_time);
CREATE INDEX ix_stop_times_stop_id ON stop_times (stop_id);
CREATE INDEX ix_stop_times_trip_id ON stop_times (trip_id);
CREATE INDEX ix_stops_location_type ON stops (location_type);
CREATE INDEX ix_stops_stop_id ON stops (stop_id);
CREATE INDEX ix_trips_route_id ON trips (route_id);