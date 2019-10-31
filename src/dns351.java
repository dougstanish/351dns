import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Random;

public class dns351 {


    public static void main(String[] args) {

        if(args.length < 2 || args.length > 3){
            System.err.println("Invalid number of arguments");
            System.err.println("Usage:\n./351dns @<server:port> <name>\n" +
                    "port (Optional) The UDP port number of the DNS server. Default value: 53.\n" +
                    "server (Required) The IP address of the DNS server, in a.b.c.d format.\n" +
                    "name (Required) The name to query for");
            System.exit(1);
        }

        String address = args[0];

        if(address.charAt(0) != '@'){
            System.err.println("Invalid formatting on address");
            System.err.println("Usage:\n./351dns @<server:port> <name>\n" +
                    "port (Optional) The UDP port number of the DNS server. Default value: 53.\n" +
                    "server (Required) The IP address of the DNS server, in a.b.c.d format.\n" +
                    "name (Required) The name to query for");
            System.exit(1);
        }

        String[] splitAddress = address.substring(1).split(":");

        int port = 53;

        if(splitAddress.length == 2){
            port = Integer.parseInt(splitAddress[1]);
        }

        byte[] serverIP = new byte[4];
        String[] ipBytes = splitAddress[0].split("\\.");
        if(ipBytes.length != 4) {
            System.out.println("Server IP address must be in a.b.c.d format: " + splitAddress[0]);
            System.exit(1);
        }

        for(int i = 0; i < 4; i++) {
            serverIP[i] = (byte) Integer.parseInt(ipBytes[i]);
        }
        
        String name = args[1];

        String requestType = "a";

        byte[] request = createRequest(address, port, name, requestType);
        byte[] responseData = new byte[512];

        try {
            DatagramSocket socket = new DatagramSocket();
            DatagramPacket dnsReq = new DatagramPacket(request, request.length, InetAddress.getByAddress(serverIP), port);
            DatagramPacket dnsResponse = new DatagramPacket(responseData, 512);
            socket.send(dnsReq);
            socket.receive(dnsResponse);
        } catch(SocketException e) {
            System.out.println("Could not bind to a datagram socket.");
            System.exit(1);
        } catch (UnknownHostException e) {
            System.out.println(splitAddress[0] + " is not a valid IP address.");
            System.exit(1);
        } catch (IOException e) {
            System.out.println("Could not send packet to server.");
            System.exit(1);
        }

        if(responseData[0] != request[0] && responseData[1] != request[1]) {
            System.out.println("MessageID of response does not match!!!");
        }

    }

    private static byte[] createRequest(String address, int port, String name, String requestType) {
        
        byte[] header = createHeader();

        byte[] query = createQuery(name, requestType);

        // Final Packet.
        byte[] request = new byte[header.length + query.length];

        // Byte Buffers provide useful utilities.
        ByteBuffer rqBuf = ByteBuffer.wrap(request);

        // Combine header and query
        rqBuf.put(header);
        rqBuf.put(query);

        return request;
    }

    private static byte[] createQuery(String name, String requestType) {

        String[] nameSplit = name.split("\\.");

        // Creates byte array to hold characters plus length values
        byte[] qname = new byte[(name.length() - nameSplit.length + 1) + nameSplit.length + 1];

        int bytePos = 0;

        // For each substring separated by '.'
        for(String substring: nameSplit){

            // Convert length of substring to byte and add to byte array
            qname[bytePos] = (byte) substring.length();
            bytePos++;

            // For each character in substring
            for(char character: substring.toLowerCase().toCharArray()){

                // Add to byte array
                qname[bytePos] = (byte) character;

                bytePos++;

            }
        }

        // Sets the final length of the substring to 00
        qname[bytePos] = 0x00;

        // Create array to hold type of query
        byte[] qtype = new byte[2];

        // If it is an A record lookup, sets appropriate values
        if(requestType.equals("a")){
            qtype[0] = 0x00;
            qtype[1] = 0x01;
        }

        // Sets value for internet lookup
        byte[] qclass = {0x00, 0x01};

        // Combines parts into one array
        byte[] completeQuery = new byte[qname.length + qtype.length + qclass.length];
        System.arraycopy(qname, 0, completeQuery, 0, qname.length);
        System.arraycopy(qtype, 0, completeQuery, qname.length, qtype.length);
        System.arraycopy(qclass, 0, completeQuery, qname.length + qtype.length, qclass.length);

        // Returns combined parts
        return completeQuery;
    }

    private static byte[] createHeader() {

        Random r = new Random();

        byte[] output = new byte[12];

        // Create random message id
        short messageID = (short) r.nextInt(Short.MAX_VALUE + 1);

        // Create a buffer we can construct the header in.
        ByteBuffer buf = ByteBuffer.wrap(output);

        // Place the message into the buffer.
        buf.putShort(messageID);

        // QR, OPCODE, AA, TC, RD, RA, and RCODE, are conveniently all 0
        buf.putShort((short) 0);

        // QDCOUNT, we're making one request.
        buf.putShort((short) 1);

        // Rest are 0
        buf.putShort((short) 0);
        buf.putShort((short) 0);
        buf.putShort((short) 0);

        return output;
    }

}
