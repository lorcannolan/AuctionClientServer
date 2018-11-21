import java.net.*;
import java.io.*;


public class BidderClient implements Runnable
{  private Socket socket = null;
   private Thread thread = null;
   private BufferedReader console = null;
   private DataOutputStream streamOut = null;
   private BidderClientThread client = null;
   private String clientName;

   
	public BidderClient(String serverName, int serverPort, String name)
	{
		System.out.println("Establishing connection. Please wait ...");

		this.clientName = name;
		try {
			// Step 1: Establish server connection
			socket = new Socket(serverName, serverPort);
			System.out.println("Welcome to the Auction!");
			System.out.println("To bid enter a numeric value greater than the current bid.");
			System.out.println("To leave auction, enter QUIT.");
			System.out.println("To add a new item enter NEW.");
			start();
		}
		catch(UnknownHostException uhe) {
			System.out.println("Host unknown: " + uhe.getMessage());
		}
		catch(IOException ioe)	{
			System.out.println("Unexpected exception: " + ioe.getMessage());
		}
	}

	public void run()
	{
		while (thread != null) {
			try {
				// Step 3: Send data
				String bid = console.readLine();
				try {
					if (!bid.equals("NEW")) {
						Integer.parseInt(bid);
					}
					streamOut.writeUTF(bid + " " + clientName);
					streamOut.flush();
				}
				catch (NumberFormatException nfe) {
					if (bid.equals("QUIT")) {
						stop();
					}
					else {
						System.out.println("Must enter a number");
					}
				}
			}
			catch(IOException ioe) {
				System.out.println("Sending error: " + ioe.getMessage());
				stop();
			}
		}
	}

	public void display(String info) {
		System.out.print(info);
	}
	
	public void start() throws IOException
	{
		console = new BufferedReader(new InputStreamReader(System.in));

		// Step 2: set up output stream
		streamOut = new DataOutputStream(socket.getOutputStream());
		if (thread == null) {
			// creating client thread
			client = new BidderClientThread(this, socket);
			thread = new Thread(this);
			thread.start();
		}
	}

	public void stop()
	{
		try {
			if (console != null) {
				console.close();
			}
			if (streamOut != null) {
				streamOut.close();
			}
			if (socket != null) {
				socket.close();
			}
		}
		catch(IOException ioe) {
			System.out.println("Error closing ...");
		}
		// Step 4: Close the Connection
		client.close();
		thread = null;
	}


	public static void main(String args[])
	{  
		BidderClient client = null;
		if (args.length != 3) {
			System.out.println("Usage: java BidderClient host port name");
		}
		else {
			client = new BidderClient(args[0], Integer.parseInt(args[1]), args[2]);
		}
	}
}
