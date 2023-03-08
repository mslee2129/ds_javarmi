package sensor;
/*
 * Updated on Feb 2023
 */
import common.MessageInfo;

import java.io.IOException;
import java.net.*;
import java.util.Random;
import java.nio.charset.StandardCharsets;

 /* You can add/change/delete class attributes if you think it would be
  * appropriate.
  *
  * You can also add helper methods and change the implementation of those
  * provided if you think it would be appropriate, as long as you DO NOT
  * CHANGE the provided interface.
  */

public class Sensor implements ISensor {
    private float measurement;

    private final static int max_measure = 50;
    private final static int min_measure = 10;

    private DatagramSocket s;
    private byte[] buffer;

    /* Note: Could you discuss in one line of comment what do you think can be
     * an appropriate size for buffsize?
     * (Which is used to init DatagramPacket?)
     */
    private static final int buffsize = 2048;

    protected int port;
    protected String address;
    protected int totMsg;

    public Sensor(String address, int port, int totMsg) {
        this.address = address;
        this.port = port;
        this.totMsg = totMsg;
        this.measurement = 0;
    }

    @Override  // TO DO THROW THE INTERRUPTED EXCEPTION
    public void run (int N) throws InterruptedException {
        for(int i = 1; i <= N; i++){
            this.measurement = getMeasurement();
            
            MessageInfo msg = new MessageInfo(N, i, this.measurement);
            sendMessage(this.address, this.port, msg);

            // Print update
            System.out.printf("[Sensor] Sending message %d out of %d." +
                            " Measure = %f \n", i, N, msg.getMessage());
        }
    }

    public static void main (String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: ./sensor.sh field_unit_address port number_of_measures");
            return;
        }

        /* Parse input arguments */
        String address = args[0];
        int port = Integer.parseInt(args[1]);
        int totMsg = Integer.parseInt(args[2]);

        /* Start new sensor */
        Sensor sensor =  new Sensor(address, port, totMsg);

        /* Run Sensor */
        try{
            sensor.run(totMsg);
        } catch(InterruptedException e){
            System.err.println("InterruptedException => " + e.getMessage());
        }
    }

    @Override
    public void sendMessage (String dstAddress, int dstPort, MessageInfo msg) {
        try {
            // create new socket
            DatagramSocket socket = new DatagramSocket();

            // save message to buffer
            byte[] message = msg.toString().getBytes(StandardCharsets.UTF_8);

            // get address and port
            InetAddress address = InetAddress.getByName(dstAddress);

            // build datagram packet from message
            DatagramPacket packet = new DatagramPacket(message, message.length,
                    address, dstPort);

            // send packet
            socket.send(packet);

            // close socket
	        socket.close();
        } catch (NumberFormatException e) {
            System.err.println("NumberFormatException => " + e.getMessage());
        } catch (UnknownHostException e) {
            System.err.println("UnknownHostException => " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("IllegalArgumentException => " + e.getMessage());
        } catch (IOException e) {
            System.err.println("IOException => " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Exception => " + e.getMessage());
        }
    }

    @Override
    public float getMeasurement () {
        Random r = new Random();
        measurement = r.nextFloat() * (max_measure - min_measure) + min_measure;

        return measurement;
    }
}
