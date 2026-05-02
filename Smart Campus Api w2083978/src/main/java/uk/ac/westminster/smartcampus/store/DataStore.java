package uk.ac.westminster.smartcampus.store;

import uk.ac.westminster.smartcampus.model.Room;
import uk.ac.westminster.smartcampus.model.Sensor;
import uk.ac.westminster.smartcampus.model.SensorReading;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory store. JAX-RS resources are by default per-request,
 * so all shared state is held here as ConcurrentHashMaps to avoid race
 * conditions between concurrent requests.
 *
 * No database technology is used (per coursework spec).
 */
public final class DataStore {

    private static final DataStore INSTANCE = new DataStore();

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, Sensor> sensors = new ConcurrentHashMap<>();
    // sensorId -> ordered list of readings
    private final Map<String, List<SensorReading>> readings = new ConcurrentHashMap<>();

    private DataStore() {
        seed();
    }

    public static DataStore getInstance() {
        return INSTANCE;
    }

    private void seed() {
        Room lib = new Room("LIB-301", "Library Quiet Study", 40);
        Room cs  = new Room("CS-101",  "CS Lecture Theatre", 120);
        Room eng = new Room("ENG-200", "Engineering Lab 200", 60);
        rooms.put(lib.getId(), lib);
        rooms.put(cs.getId(),  cs);
        rooms.put(eng.getId(), eng);

        Sensor t1 = new Sensor("TEMP-001", "Temperature", "ACTIVE",      21.5, "LIB-301");
        Sensor c1 = new Sensor("CO2-001",  "CO2",         "ACTIVE",     420.0, "LIB-301");
        Sensor o1 = new Sensor("OCC-001",  "Occupancy",   "MAINTENANCE",  0.0, "CS-101");
        sensors.put(t1.getId(), t1);
        sensors.put(c1.getId(), c1);
        sensors.put(o1.getId(), o1);

        lib.getSensorIds().add(t1.getId());
        lib.getSensorIds().add(c1.getId());
        cs .getSensorIds().add(o1.getId());

        readings.put(t1.getId(), new ArrayList<>());
        readings.put(c1.getId(), new ArrayList<>());
        readings.put(o1.getId(), new ArrayList<>());
    }

    public Map<String, Room>   rooms()    { return rooms; }
    public Map<String, Sensor> sensors()  { return sensors; }
    public Map<String, List<SensorReading>> readings() { return readings; }
}
