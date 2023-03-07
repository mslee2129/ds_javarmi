package field;
/*
 * Updated on Feb 2023
 */
import centralserver.ICentralServer;
import common.MessageInfo;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
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

    /* Note: Could you discuss in one line of comment what do you think can be
     * an appropriate size for buffsize?
     * (Which is used to init DatagramPacket?)
     */

    private static final int buffsize = 2048;
    private int timeout = 50000;
    protected int expected = 0;
    protected int counter = 0;
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
        
        /* TODO: Compute SMA and store values in a class attribute */
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
                if(expected == 0) { expected = message.getTotalMessages(); }
                counter++; // increment counter

                System.out.printf("[UDP Controller] Message %d out of %d " +
                                "received. Value = %f\n", message.getMessageNum(),
                                this.expected, message.getMessage());

                addMessage(message); // save message
                
                // if count reaches total, break
                if(counter >= expected) { break; }
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

        /* TODO: Parse arguments */


        /* TODO: Construct Field Unit Object */

        /* TODO: Call initRMI on the Field Unit Object */



            /* TODO: Wait for incoming transmission */

            /* TODO: Compute Averages - call sMovingAverage()
                on Field Unit object */

            /* TODO: Compute and print stats */

            /* TODO: Send data to the Central Serve via RMI and
             *        wait for incoming transmission again
             */

    }


    @Override
    public void initRMI (String address) {
        /* If you are running the program within an IDE instead of using the
         * provided bash scripts, you can use the following line to set
         * the policy file
         */

        /* System.setProperty("java.security.policy","file:./policy\n"); */

        /* TODO: Bind to RMIServer */
        try {
            String name = "Compute";
            FieldUnit engine = new FieldUnit();
            FieldUnit stub =
                (FieldUnit) UnicastRemoteObject.exportObject(engine, 0);
            Registry registry = LocateRegistry.getRegistry();
            registry.bind(name, stub);
            System.out.println("FieldUnit bound");
        } catch (Exception e) {
            System.err.println("FieldUnit exception:");
            e.printStackTrace();
        }

        /* TODO: Send pointer to LocationSensor to RMI Server */

    }

    @Override
    public void sendAverages () {
        /* TODO: Attempt to send messages the specified number of times */

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

        /* Re-initialise data structures for next time */
        this.expected = 0;
        this.counter = 0;
        this.receivedMessages = new ArrayList<>();
    }
}