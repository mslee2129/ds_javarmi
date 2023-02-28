package field;
/*
 * Updated on Feb 2023
 */
import common.Location;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/* TODO extend appropriate classes and implement the appropriate interfaces */

public class LocationSensor {
    private Location location;

    public LocationSensor () throws RemoteException {
        super();
        location = new Location(40.831436, 14.242832);
    }

    public LocationSensor(Location location) throws RemoteException {
        super();
        this.location = location;
    }

    @Override
    public Location getCurrentLocation () throws RemoteException {
        return location;
    }
}
