package centralserver;

import common.*;
import field.ILocationSensor;

import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
/*
 * Updated on Feb 2023
 */
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

import javax.swing.text.html.CSS;

 /* You can add/change/delete class attributes if you think it would be
  * appropriate.
  *
  * You can also add helper methods and change the implementation of those
  * provided if you think it would be appropriate, as long as you DO NOT
  * CHANGE the provided interface.
  */

/* extend appropriate classes and implement the appropriate interfaces */
public class CentralServer implements ICentralServer {

    private ILocationSensor locationSensor;
    protected List<MessageInfo> receivedMessages;

    protected int expected = 0;
    protected int counter = 0;

    protected CentralServer () throws RemoteException {
        super();
        
        /* Initialise Array receivedMessages */
        this.receivedMessages = new ArrayList<>();

    }

    public static void main (String[] args) throws RemoteException {
        try{
            CentralServer cs = new CentralServer();

            /* Create (or Locate) Registry */
            Registry registry = LocateRegistry.createRegistry(9999);
            
            /* Bind to Registry */        
            registry.rebind("CentralServer", cs);
            
            // Central server readt
            System.out.println("Central Server ready"); 

            try{
            Thread.sleep(10000);
            } catch(Exception e){
                System.err.println("I fucked up");
            }
            // Set Location Sensor
            cs.setLocationSensor(cs.locationSensor);
    
        } catch (AccessException e) {
            System.err.println("AccessException => " + e.getMessage());
        } catch (RemoteException e) {
            System.err.println("RemoteException => " + e.getMessage());
        } catch (NullPointerException e) {
            System.err.println("NullPointerException => " + e.getMessage());
        }
    }


    @Override
    public void receiveMsg (MessageInfo msg) {
        try{
            //  If first message:, initialise expected
            if(this.expected == 0) {this.expected = msg.getTotalMessages();}
            this.counter++; // increment counter

            System.out.println("[Central Server] Received message " + 
                (msg.getMessageNum()) + " out of " + msg.getTotalMessages() +
                    ". Measure = " + msg.getMessage());
                
            // Save current message 
            this.receivedMessages.add(msg);

            // If done with receiving prints stats.
            if(this.expected == msg.getTotalMessages()){
                this.printStats();
            }
        // change exception
        } catch(Exception e){
            System.err.println("Exception => " + e.getMessage());
        }
    }


    public void printStats() {
        try{
            /* Find out how many messages were missing */
            System.err.printf("Total missing messages %d out of %d \n", 
                            this.expected - this.counter, this.expected);

            /* Print stats (i.e. how many message missing?
            * do we know their sequence number? etc.) */
            ArrayList<Integer> unreceivedMessages = new ArrayList<Integer>();
            for(int j = 1; j <= this.expected; j++){
                Boolean found = false;
                for(int i = 0; i < this.receivedMessages.size(); i++){
                    if(this.receivedMessages.get(i).getMessageNum() == j){
                        found = true;
                        break;
                    }
                }
                if(!found){
                    unreceivedMessages.add(j); 
                }    
            }
            System.err.println("The messages that were lost are the following: " + 
                            unreceivedMessages);

            /* Print the location of the Field Unit that sent the messages */
            printLocation();

            /* Now re-initialise data structures for next time */
            this.counter = 0;
            this.expected = 0;
            this.receivedMessages = new ArrayList<>();
        } catch(Exception e){
            System.err.println("Exception => " + e.getMessage());
        }
    }

    @Override
    public void setLocationSensor (ILocationSensor locationSensor) throws RemoteException {
        // Set location sensor
        this.locationSensor = locationSensor;
        System.out.println("Location Sensor Set");
    }

    public void printLocation() throws RemoteException {
        // Print location on screen from remote reference
        System.out.printf("[Field Unit] Current Location: lat = %f long = %f\n", 
            this.locationSensor.getCurrentLocation().getLatitude(), 
            this.locationSensor.getCurrentLocation().getLongitude());
    }
}
