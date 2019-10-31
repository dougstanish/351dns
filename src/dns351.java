import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
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
            // Specifically parse the IP into a byte array by hand because we cant use getByName
            DatagramPacket dnsReq = new DatagramPacket(request, request.length, InetAddress.getByAddress(serverIP), port);
            DatagramPacket dnsResponse = new DatagramPacket(responseData, 512);
            socket.send(dnsReq);
            Instant timeout = Instant.now().plusSeconds(5);
            while(true) {
                socket.receive(dnsResponse);
                if (responseData[0] != request[0] && responseData[1] != request[1]) {
                } else {
                    break;
                }
                if(Instant.now().isAfter(timeout)) {
                    System.out.println("NORESPONSE");
                    System.exit(1);
                }
            }
        } catch(SocketException e) {
            System.out.println("ERROR\tCould not bind to a datagram socket.");
            System.exit(1);
        } catch (UnknownHostException e) {
            System.out.println("ERROR\t" + splitAddress[0] + " is not a valid IP address.");
            System.exit(1);
        } catch (IOException e) {
            System.out.println("ERROR\tCould not send packet to server.");
            System.exit(1);
        }
        for(byte b : responseData) {
            System.out.print((b & 0xff) + ", ");
        }
        System.out.println();

        parseResponse(responseData);

    }

    private static void dumpPacket(byte[] bytes) {

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

    public static void parseResponse(byte[] response) {
        // Extract all the parts of the header.
        // Use int to get around Java's lack of unsigned...
        int qr = ((response[2] &     0b10000000) >> 7);
        int opcode = ((response[2] & 0b01111000) >> 3);
        int aa = ((response[2] &     0b00000100) >> 2);
        int tc = ((response[2] &     0b00000010) >> 1);
        int rd = (response[2] &      0b00000001);
        int ra = ((response[3] &     0b10000000) >> 7);
        int res1 = ((response[3] &   0b01000000) >> 6);
        int res2 = ((response[3] &   0b00100000) >> 5);
        int res3 = ((response[3] &   0b00010000) >> 4);
        int rcode = (response[3] &   0b00001111);
        int qdcount = (response[4] << 8 | response[5]);
        int ancount = (response[6] << 8 | response[7]);
        int nscount = (response[8] << 8 | response[9]);
        int arcount = (response[10] << 8 | response[11]);

        System.out.println("QR: " + qr);
        System.out.println("OPCODE: "+ opcode);
        System.out.println("AA: " + aa);
        System.out.println("TC: " + tc);
        System.out.println("RD: " + rd);
        System.out.println("RA: " + ra);
        System.out.println("RES1: " + res1);
        System.out.println("RES2: " + res2);
        System.out.println("RES3: " + res3);
        System.out.println("RCODE: " + rcode);
        System.out.println("QDCOUNT: " + qdcount);
        System.out.println("ANCOUNT: " + ancount);
        System.out.println("NSCOUNT: " + nscount);
        System.out.println("ARCOUNT: " + arcount);

        // WHO DESIGNED THE DNS PACKET FORMAT AND WHY DID THEY ADD POINTERS?!?
        HashMap<Integer, String> foundLabels = new HashMap<>();

        int curByte = 12;
        for(int i = 0; i < qdcount; i++) {
            ArrayList<String> labels = new ArrayList<String>();
            while(true) {
                if((response[curByte] & 0b11000000) != 0) {
                    // Labels have a length value, the first two bits have to be 0.
                    System.out.println("ERROR\tInvalid label length in response.");
                    System.exit(1);
                }
                StringBuilder working = new StringBuilder();
                int labelLen = response[curByte];
                int pntr = curByte++;
                if(labelLen == 0) {
                    break;
                }
                for(int j = 0; j < labelLen; j++, curByte++) {
                    working.append((char) response[curByte]);
                }
                labels.add(working.toString());
                foundLabels.put(pntr, working.toString());
            }

            // Increment curByte every time we use it, meaning it always points to the byte we haven't used.
            short qtype = (short) ((response[curByte++] << 8) | response[curByte++]);
            short qclass = (short) ((response[curByte++] << 8) | response[curByte++]);
        }

        // This for loop handles all the Answer section parts.
        for(int i = 0; i < ancount; i++) {
            StringBuilder recordName = new StringBuilder();
            boolean nameDone = false;
            while(!nameDone) {
                if((response[curByte] & 0b11000000) == 0b11000000) {
                    recordName.append(foundLabels.get(((response[curByte++] & 0b00111111) << 8) | response[curByte++]));
                    nameDone = true;
                } else if ((response[curByte] & 0b11000000) == 0) {
                    StringBuilder working = new StringBuilder();
                    int labelLen = response[curByte];
                    int pntr = curByte++;
                    if(labelLen == 0) {
                        break;
                    }
                    for(int j = 0; j < labelLen; j++, curByte++) {
                        working.append((char) response[curByte]);
                    }
                    recordName.append(working.toString());
                    foundLabels.put(pntr, working.toString());
                } else {
                    System.out.println("ERROR\tInvalid label.");
                }
            }
            short type = (short) ((response[curByte++] << 8) | response[curByte++]);
            short dnsclass = (short) ((response[curByte++] << 8) | response[curByte++]);
            int ttl = ((response[curByte++] << 24) | (response[curByte++] << 16) | (response[curByte++] << 8) | response[curByte++]);
            short rdlength = (short) ((response[curByte++] << 8) | response[curByte++]);

            if(type == 1) {
                if(rdlength != 4) {
                    System.out.println("ERROR\tA records should only have a 4 byte RDATA");
                    System.exit(1);
                }
                System.out.println("IP\t" + (response[curByte++] & 0xff) + "." + (response[curByte++] & 0xff) + "." +
                        (response[curByte++] & 0xff) + "." + (response[curByte++] & 0xff) + "\tnonauth");
            }
        }

        for(int i = 0; i < nscount; i++) {
            StringBuilder recordName = new StringBuilder();
            boolean nameDone = false;
            while(!nameDone) {
                if((response[curByte] & 0b11000000) == 0b11000000) {
                    recordName.append(foundLabels.get(((response[curByte++] & 0b00111111) << 8) | response[curByte++]));
                    nameDone = true;
                } else if ((response[curByte] & 0b11000000) == 0) {
                    StringBuilder working = new StringBuilder();
                    int labelLen = response[curByte];
                    int pntr = curByte++;
                    if(labelLen == 0) {
                        break;
                    }
                    for(int j = 0; j < labelLen; j++, curByte++) {
                        working.append((char) response[curByte]);
                    }
                    recordName.append(working.toString());
                    foundLabels.put(pntr, working.toString());
                } else {
                    System.out.println("ERROR\tInvalid label.");
                }
            }
            short type = (short) ((response[curByte++] << 8) | response[curByte++]);
            short dnsclass = (short) ((response[curByte++] << 8) | response[curByte++]);
            int ttl = ((response[curByte++] << 24) | (response[curByte++] << 16) | (response[curByte++] << 8) | response[curByte++]);
            short rdlength = (short) ((response[curByte++] << 8) | response[curByte++]);

            if(type == 1) {
                if(rdlength != 4) {
                    System.out.println("ERROR\tA records should only have a 4 byte RDATA");
                    System.exit(1);
                }
                System.out.println("IP\t" + (response[curByte++] & 0xff) + "." + (response[curByte++] & 0xff) + "." +
                        (response[curByte++] & 0xff) + "." + (response[curByte++] & 0xff) + "\tauth");
            }
        }
    }

}
