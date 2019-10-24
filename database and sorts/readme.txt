Gli orari si trovano su http://www.actv.it/opendata/

Come ho fatto a caricare i dati?
All'inizio ho fatto come dice qui: https://oegeo.wordpress.com/2011/08/17/salt-lake-city-gtfs-into-sqlite/
creando lo script e importando i singoli file. Non crea gli indici, però.
Poi ho usato anche gtfsdb -> https://github.com/OpenTransitTools/gtfsdb
Quest'ultimo crea gli indici ma ha un ordine e un numero dei campi tutto suo, mi pare.

Quindi alla fine, se devi importare dei file nuovi, ti consiglio di usare lo script a. Cerca nella cartella database and sorts, dovrebbero esserci gli script SQL. Da questi ultimi puoi estrarre le righe per creare gli indici e quindi generare un nuovo file .db contenente i dati aggiornati e gli indici. Fatto questo segui le indicazioni su sqlite-asset-helper (https://github.com/jgilfelt/android-sqlite-asset-helper) per capire come aggiornare il DB dell'app

Tenere solo queste oppure no? A questo punto anche no.
Domani devi:
- Mettere le relazioni del DB in GreenDao
- Fare delle prove di caricamento di oggetti annidati con le relazioni
- Modificare l'app perché vada a prendere i valori da lì
(in realtà mi sa che per ora punterò a fare un mapping record -> oggetto con dei cicli. Tanto sembra essere piuttosto veloce, pare.)

Poi forse può anche bastare, come versione preliminare

12L fa 316 e 317

tabelle che non servono:
- shapes (svuotala)

create statement for indexes che devi lanciare manualmente nel file creato con il comando sqlite3 actv.db < import_db.sql

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

Come togliere i dati non necessari dal DB

1- delete from stops where stop_id not in (6081, 6074, 6073, 6084, 6062, 6080, 6027, 6061, 6084, 384, 613, 172, 1172, 3626, 501, 503, 505, 506, 507, 508, 509, 510, 511, 512, 514, 515, 516, 517);
2- delete from stop_times where stop_times.stop_id not in (select stop_id from stops);
