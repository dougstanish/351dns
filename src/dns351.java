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

        String[] split_address = address.substring(1).split(":");

        System.out.printf("");

        int port = 53;

        if(split_address.length == 2){
            port = Integer.parseInt(split_address[1]);
        }

        // TODO: Check validity of ip



    }

}
