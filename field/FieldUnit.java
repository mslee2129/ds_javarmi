package field;
/*
 * Updated on Feb 2023
 */
import centralserver.ICentralServer;
import common.MessageInfo;
import field.ILocationSensor;
import field.LocationSensor;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

 /* You can add/change/delete class attributes if you think it would be
  * appropriate.
  *
  * You can also add helper methods and change the implementation of those
  * provided if you think it would be appropriate, as long as you DO NOT
  * CHANGE the provided interface.
  */

public class FieldUnit implements IFieldUnit {
    private ICentralServer central_server;
    protected Registry registry;

    /* Note: Could you discuss in one line of comment what do you think can be
     * an appropriate size for buffsize?
     * (Which is used to init DatagramPacket?)
     */

    private static final int buffsize = 2048;
    private int timeout = 50000;
    protected int expected = 0;
    protected int counter = 0;
    protected int port;
    protected List<MessageInfo> receivedMessages;
    protected List<Float> movingAverages;


    public FieldUnit () {
        this.receivedMessages = new ArrayList<>();
        this.movingAverages = new ArrayList<>();
    }

    @Override
    public void addMessage (MessageInfo msg) {
        this.receivedMessages.add(msg);
    }

    @Override
    public void sMovingAverage (int k) {
        System.out.println("[Field Unit] Computing SMAs");

        /* Compute SMA and store values in a class attribute */
        try {
            for(int i = 0; i < this.expected; i++) {
                if(i < k) {
                    this.movingAverages.add(receivedMessages.get(i).getMessage());
                }
                else {
                    float sum = 0;
                    for(int j = 0; j < k; j++) {
                        // reconsider at some point
                        sum += receivedMessages.get(i-j).getMessage();
                    }
                    this.movingAverages.add(sum/k);
                }
                
            }
            
        } catch (UnsupportedOperationException e) {
            System.err.println("UnsupportedOperationException => " + e.getMessage());
        }
    }



    @Override
    public void receiveMeasures(int port, int timeout) throws SocketException {
        try {
            // create new socket
            DatagramSocket socket = new DatagramSocket(port);
            System.out.println("[Field Unit] Listening on port: " + port);

            // set timeout
            this.timeout = timeout;
            socket.setSoTimeout(this.timeout);
    
            // runs until all package is received
            boolean listen = true;

            while (listen) {
                // create buffer
                byte[] buffer = new byte[buffsize];

                // create new datagram packet
                DatagramPacket receivePacket = new DatagramPacket(
                        buffer, buffer.length);

                // receive packet in socket
                socket.receive(receivePacket);
                String messageString = new String(receivePacket.getData());

                MessageInfo message = new MessageInfo(messageString);

                // if first message, set expected to total
                if(this.expected == 0) { 
                    this.expected = message.getTotalMessages(); 
                }

                this.counter++; // increment counter

                System.out.printf("[UDP Controller] Message %d out of %d " +
                                "received. Value = %f\n", message.getMessageNum(),
                                this.expected, message.getMessage());

                this.addMessage(message); // save message
                
                // if count reaches total, break
                if(this.counter >= this.expected) { listen = false; }
            }
            socket.close();
            printStats();
        } catch (UnknownHostException e) {
            System.err.println("UnknownHostException => " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("IllegalArgumentException => " + e.getMessage());
        } catch (SocketException e) {
            System.err.println("SocketException => " + e.getMessage());
        } catch (IOException e) {
            System.err.println("IOException => " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Exception => " + e.getMessage());
        }

    }

    public static void main (String[] args) throws SocketException {
        if (args.length < 2) {
            System.out.println("Usage: ./fieldunit.sh <UDP rcv port> <RMI server HostName/IPAddress>");
            return;
        }
        /* Construct Field Unit Object */
        FieldUnit field_unit = new FieldUnit();

        /* Parse arguments */
        field_unit.port = Integer.parseInt(args[0]);
        String address = args[1];

        /* Call initRMI on the Field Unit Object */
        field_unit.initRMI(address);

        /* Wait for incoming transmission */
        field_unit.receiveMeasures(field_unit.port, field_unit.timeout);
    
        /* Compute Averages - call sMovingAverage()
            on Field Unit object */
        field_unit.sMovingAverage(7);

        /* Re-initialise data structures for next time */
        field_unit.expected = 0;
        field_unit.counter = 0;
        field_unit.receivedMessages = new ArrayList<>();

        /* Send data to the Central Serve via RMI and
                wait for incoming transmission again*/
        field_unit.sendAverages();

        // Ready to listen again
        try{
        field_unit.receiveMeasures(field_unit.port, field_unit.timeout);
        } catch(SocketException e){
            System.err.println("SocketException => " + e.getMessage());
        }
    }

    @Override
    public void initRMI (String address) {
        try {
            String rmiUrl = new String("rmi://"+address+":1099/CentralServer");

            // Bind to RMIServer 
            this.central_server = (ICentralServer) Naming.lookup(rmiUrl);
            
            /* Send pointer to LocationSensor to RMI Server */
            LocationSensor loc = new LocationSensor();
            ILocationSensor iloc = (ILocationSensor) UnicastRemoteObject.exportObject(loc, 1099);
            this.central_server.setLocationSensor(iloc);

        } catch (Exception e) {
            System.err.println("Exception => " + e.getMessage());
        }
    }

    @Override
    public void sendAverages () {

        System.out.println("[Field Unit] Sending SMAs to RMI");

        /* Attempt to send messages the specified number of times */
        try {
            for(int i = 1; i < this.movingAverages.size()+1; i++){
                MessageInfo msg = new MessageInfo(this.movingAverages.size(), i, this.movingAverages.get(i));
                this.central_server.receiveMsg(msg);
            }
        } catch (Exception e) {
            System.err.println("Exception => " + e.getMessage());
        }
    }

    @Override
    public void printStats () {
        
        System.out.printf("Total missing messages %d out of %d \n", 
        this.expected - this.counter, this.expected);

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

        System.out.println("The messages that were lost are the following: " + 
                unreceivedMessages);
        System.out.println("===============================");
    } 
}
