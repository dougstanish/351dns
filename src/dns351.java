import java.util.BitSet;
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
        
        String name = args[1];

        String requestType = "a";

        createRequest(address, port, name, requestType);



    }

    private static void createRequest(String address, int port, String name, String requestType) {
        
        createHeader();

        byte[] query = createQuery(name, requestType);
        
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

    private static void createHeader() {

        Random r = new Random();

        // Create random message id
        short messageID = (short) r.nextInt(Short.MAX_VALUE + 1);


    }

}
