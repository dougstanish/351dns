import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class dns351 {


    /**
     * Main class that will take a DNS server and a website address, and return the IP address to access it
     *
     * @param args - An array of the given arguments
     */
    public static void main(String[] args) {

        // Makes sure correct number of args are present
        if(args.length < 2 || args.length > 3){
            System.err.println("Invalid number of arguments");
            System.err.println("Usage:\n./351dns @<server:port> <name>\n" +
                    "port (Optional) The UDP port number of the DNS server. Default value: 53.\n" +
                    "server (Required) The IP address of the DNS server, in a.b.c.d format.\n" +
                    "name (Required) The name to query for");
            System.exit(1);
        }

        String address = args[0];

        // Ensures the DNS ip is preceded with an '@' symbol
        if(address.charAt(0) != '@'){
            System.err.println("Invalid formatting on address");
            System.err.println("Usage:\n./351dns @<server:port> <name>\n" +
                    "port (Optional) The UDP port number of the DNS server. Default value: 53.\n" +
                    "server (Required) The IP address of the DNS server, in a.b.c.d format.\n" +
                    "name (Required) The name to query for");
            System.exit(1);
        }

        // Splits the address into the ip and the port
        String[] splitAddress = address.substring(1).split(":");

        // Sets default port
        int port = 53;

        // If a port is designated, sets port
        if(splitAddress.length == 2){
            port = Integer.parseInt(splitAddress[1]);
        }

        // Byte array to hold the server ip
        byte[] serverIP = new byte[4];

        // Splits the IP by the '.'
        String[] ipBytes = splitAddress[0].split("\\.");

        // Ensures that IP is in proper format
        if(ipBytes.length != 4) {
            System.out.println("Server IP address must be in a.b.c.d format: " + splitAddress[0]);
            System.exit(1);
        }

        // Converts the IP to bytes
        for(int i = 0; i < 4; i++) {
            serverIP[i] = (byte) Integer.parseInt(ipBytes[i]);
        }
        
        String name = args[1];

        // Hard-coded request type
        String requestType = "a";

        // Creates the byte payload to be sent to the DNS server
        byte[] request = createRequest(name, requestType);

        // Array to hold DNS response
        byte[] responseData = new byte[512];

        try {

            // Creates a socket to send and receive the DNS query and response
            DatagramSocket socket = new DatagramSocket();

            // Specifically parse the IP into a byte array by hand because we cant use getByName
            DatagramPacket dnsReq = new DatagramPacket(request, request.length, InetAddress.getByAddress(serverIP), port);
            DatagramPacket dnsResponse = new DatagramPacket(responseData, 512);

            // Sends the DNS request
            socket.send(dnsReq);

            // Creates a time to time out the request, used if it keeps recieving junk data
            Instant timeout = Instant.now().plusSeconds(5);

            // Sets the time to time out if the socket does not receive any data
            socket.setSoTimeout(5000);

            while(true) {

                // Wait to receive data from the set port
                socket.receive(dnsResponse);

                // If the data matches the expected data, break loop
                if (responseData[0] != request[0] && responseData[1] != request[1]) {
                } else {
                    break;
                }

                // If the system has not received the response in 5 seconds
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
        } catch (SocketTimeoutException e) {
            System.out.println("NORESPONSE");
            System.exit(1);
        } catch (IOException e) {
            System.out.println("ERROR\tCould not send packet to server.");
            System.exit(1);
        }
        for(byte b : responseData) {
            System.out.print((b & 0xff) + ", ");
        }
        System.out.println();

        // Parses the response and prints the data
        parseResponse(responseData);

    }

    private static void dumpPacket(byte[] bytes) {

    }

    /**
     * Method that creates a DNS request
     *
     * @param name - The name that we are querying the DNS server with
     * @param requestType - The type of request we are trying to send
     * @return - A byte array containing the query
     */
    private static byte[] createRequest(String name, String requestType) {

        // Creates the header for the request
        byte[] header = createHeader();

        // Creates the query for the request
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
            qtype[1] = 0x01;
        }
        else if (requestType.equals("ns")){
            qtype[1] = 0x02;
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

        // Creates random number generator
        Random r = new Random();

        byte[] output = new byte[12];

        // Create random message id
        short messageID = (short) r.nextInt(Short.MAX_VALUE + 1);

        // Create a buffer we can construct the header in.
        ByteBuffer buf = ByteBuffer.wrap(output);

        // Place the message into the buffer.
        buf.putShort(messageID);

        // Sets QR, OPCODE, AA, TC, RD, RA, and RCODE
        buf.put((byte)0x01);
        buf.put((byte)0x20);

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
            ArrayList<Integer> currentLabels = new ArrayList<>();
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
                currentLabels.add(pntr);
                for(Integer n : currentLabels) {
                    if(foundLabels.containsKey(n)) {
                        foundLabels.put(n, foundLabels.get(n) + "." + working.toString());
                    } else {
                        foundLabels.put(n, working.toString());
                    }
                }
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

            // If it is an A record
            if(type == 1) {

                // If it is an invalid length
                if(rdlength != 4) {
                    System.out.println("ERROR\tA records should only have a 4 byte RDATA");
                    System.exit(1);
                }

                // Output the IP record
                System.out.println("IP\t" + (response[curByte++] & 0xff) + "." + (response[curByte++] & 0xff) + "." +
                        (response[curByte++] & 0xff) + "." + (response[curByte++] & 0xff) + "\tnonauth");
            }
            else if(type == 5){

                // Creates string to hold combined record
                String cnameRecord = "";

                // Gets length of subsection of CNAME record
                int curLength = (int) response[curByte];

                curByte++;

                int totalLength = 0;

                // While there are still chars to be read
                while(totalLength != rdlength){

                    // If the current subsection has no more chars
                    if(curLength == 0){

                        // Sets length of next subsection
                        curLength = (int) response[curByte] & 0xff;

                        // If this subsection was the last
                        if(curLength == 0){

                            curByte++;

                            // Breaks out of loop
                            break;
                        } else if((curLength & 0b11000000) == 0b11000000) {
                            // It's a pointer.
                            curLength = (int) (response[curByte++] << 8) | response[curByte];
                            cnameRecord += ".";
                            cnameRecord += foundLabels.get(curLength & 0b0011111111111111);

                            // Pointers are always the END of a label.
                            curByte++;
                            break;
                        }

                        // Adds period to divide subsections
                        cnameRecord += ".";

                    }
                    // Otherwise adds next char to string
                    else{
                        byte temp = response[curByte];
                        cnameRecord += (char) response[curByte];

                        // Decreases size of current subsection
                        curLength--;
                    }

                    // Increases total length of CNAME record
                    totalLength++;


                    // Increments the currently selected byte
                    curByte++;

                }
                System.out.println("CNAME\t" + cnameRecord + "\tnonauth");
            }
            else if(type == 2){

                // Creates string to hold combined record
                String nsRecord = "";

                // Gets length of subsection of CNAME record
                int curLength = (int) response[curByte];

                curByte++;

                int totalLength = 0;

                // While there are still chars to be read
                while(totalLength != rdlength){

                    // If the current subsection has no more chars
                    if(curLength == -1){

                        // Sets length of next subsection
                        curLength = (int) response[curByte];

                        // If this subsection was the last
                        if(curLength == 0){

                            curByte++;

                            // Breaks out of loop
                            break;
                        } else if((curLength & 0b11000000) == 0b11000000) {
                            // It's a pointer.
                            curLength = (int) (response[curByte++] << 8) | response[curByte];
                            nsRecord += ".";
                            nsRecord += foundLabels.get(curLength & 0b0011111111111111);

                            // Pointers are always the END of a label.
                            curByte++;
                            break;
                        }

                        // Adds period to divide subsections
                        nsRecord += ".";

                    }
                    // Otherwise adds next char to string
                    else{
                        nsRecord += (char) response[curByte];

                        // Decreases size of current subsection
                        curLength--;

                    }

                    // Increases total length of CNAME record
                    totalLength++;

                    // Increments the currently selected byte
                    curByte++;

                }
                System.out.println("NS\t" + nsRecord + "\tnonauth");
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
            // If CNAME
            else if(type == 5){

                // Creates string to hold combined record
                String cnameRecord = "";

                // Gets length of subsection of CNAME record
                int curLength = (int) response[curByte];

                curByte++;

                int totalLength = 0;

                // While there are still chars to be read
                while(totalLength != rdlength){

                    // If the current subsection has no more chars
                    if(curLength == 0){

                        // Sets length of next subsection
                        curLength = (int) response[curByte];

                        // If this subsection was the last
                        if(curLength == 0){

                            curByte++;

                            // Breaks out of loop
                            break;
                        } else if((curLength & 0b11000000) == 0b11000000) {
                            // It's a pointer.
                            curLength = (int) (response[curByte++] << 8) | response[curByte];
                            cnameRecord += ".";
                            cnameRecord += foundLabels.get(curLength & 0b0011111111111111);

                            // Pointers are always the END of a label.
                            curByte++;
                            break;
                        }

                        // Adds period to divide subsections
                        cnameRecord += ".";

                    }
                    // Otherwise adds next char to string
                    else{
                        cnameRecord += (char) response[curByte];

                        // Decreases size of current subsection
                        curLength--;
                    }

                    // Increases total length of CNAME record
                    totalLength++;


                    // Increments the currently selected byte
                    curByte++;

                }
                System.out.println("CNAME\t" + cnameRecord + "\tauth");
            }
            else if(type == 2){

                // Creates string to hold combined record
                String nsRecord = "";

                // Gets length of subsection of CNAME record
                int curLength = (int) response[curByte];

                curByte++;

                int totalLength = 0;

                // While there are still chars to be read
                while(totalLength != rdlength){

                    // If the current subsection has no more chars
                    if(curLength == 0){

                        // Sets length of next subsection
                        curLength = (int) response[curByte];

                        // If this subsection was the last
                        if(curLength == 0){

                            curByte++;

                            // Breaks out of loop
                            break;
                        } else if((curLength & 0b11000000) == 0b11000000) {
                            // It's a pointer.
                            curLength = (int) (response[curByte++] << 8) | response[curByte];
                            nsRecord += ".";
                            nsRecord += foundLabels.get(curLength & 0b0011111111111111);

                            // Pointers are always the END of a label.
                            curByte++;
                            break;
                        }

                        // Adds period to divide subsections
                        nsRecord += ".";

                    }
                    // Otherwise adds next char to string
                    else{
                        nsRecord += (char) response[curByte];

                        // Decreases size of current subsection
                        curLength--;
                    }

                    // Increases total length of CNAME record
                    totalLength++;


                    // Increments the currently selected byte
                    curByte++;

                }
                System.out.println("NS\t" + nsRecord + "\tauth");

            }
        }
    }

}
