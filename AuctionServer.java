import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class AuctionServer implements Runnable
{  
   
   // Array of clients	
   private AuctionServerThread clients[] = new AuctionServerThread[50];
   private ServerSocket server = null;
   private Thread thread = null;
   private int clientCount = 0;
   
   private ArrayList<Item> items = new ArrayList<Item>();
   private Item currentItem;
   private int currentItemArrayIndex = 0;
   
   private Timer timer;

   public AuctionServer(int port)
   {
		addAuctionItems();
		itemCountdown();
		try {
			System.out.println("Binding to port " + port + ", please wait  ...");
			// Step 1: Create ServerSocket object
			server = new ServerSocket(port);
			System.out.println("Server started: " + server.getInetAddress());
			// Start new thread if no thread exists
			start();
		}
		catch(IOException ioe)
		{
			System.out.println("Can not bind to port " + port + ": " + ioe.getMessage());
		}
	}
	
	public void addAuctionItems() {
		Item item1 = new Item(50, "Raleigh Road Bike");
		items.add(item1);
		Item item2 = new Item(100, "XBox One");
		items.add(item2);
		Item item3 = new Item(1000, "Chevy Corvette");
		items.add(item3);
		Item item4 = new Item(25, "Game of Thrones Box Set");
		items.add(item4);
		Item item5 = new Item(50, "Signed Manchester United Jersey");
		items.add(item5);
		Item item6 = new Item(75, "Beats Headphones");
		items.add(item6);
		Item item7 = new Item(100, "Holiday to Australia");
		items.add(item7);
		Item item8 = new Item(20, "Star Wars Premier Tickets");
		items.add(item8);
		Item item9 = new Item(100, "Trip to Safari");
		items.add(item9);
		Item item10 = new Item(50, "Glastonbury Festival Tickets");
		items.add(item10);
	}

	public void run()
	{
		while (thread != null)
		{
			try {
				System.out.println("Waiting for a client ...");
				// Step 2: Put the server into awaiting state
				addThread(server.accept());

				int pause = (int)(Math.random()*3000);
				Thread.sleep(pause);

			}
			catch(IOException ioe) {
				System.out.println("Server accept error: " + ioe);
				stop();
			}
			catch (InterruptedException e) {
				System.out.println(e);
			}
		}
   }

	public void start() {
		if (thread == null) {
			thread = new Thread(this);
			thread.start();
		}
    }

	public void stop() {
		thread = null;
	}

	private int findClient(int ID) {
		for (int i = 0; i < clientCount; i++) {
			if (clients[i].getID() == ID) {
				return i;
			}
		}
		return -1;
	}
	
	// Display current item to clients
	public synchronized void displayCurrentItem() {
		if (currentItemArrayIndex != 0) {
			Item previousItem = items.get(currentItemArrayIndex-1);
			// alert bidder of previous item if bid was made
			if (previousItem.isBiddedFor()) {
				try {
					clients[findClient(previousItem.getHighestBidder())]
						.send("\n\nCongratulations! You successfully purchased "
						+ previousItem.getName() + " for a value of " + previousItem.getHighestBid());
					items.remove(previousItem);
					// As items in the list are shifted back one position, current index must re-adjust
					currentItemArrayIndex--;
				}
				catch (ArrayIndexOutOfBoundsException aioobe) {
					// if client makes bid and leaves, reset item and return to it at later stage
					previousItem.setHighestBid(previousItem.getStartingPrice());
					previousItem.setBiddedFor(false);
					previousItem.setHighestBidder(0);
					previousItem.setHighestBidderName("");
					for (int i = 0; i < clientCount; i++) {
						// Step 4: Send data
						clients[i].send("\n\n" + previousItem.getName() 
							+ " value has been reset as highest bidder"
							+ " left the Auction before receiving item.");
					}
				}
			}
		}
		
		currentItem = items.get(currentItemArrayIndex);
		
		for (int i = 0; i < clientCount; i++) {
			// Step 4: Send data
			if (currentItem.isBiddedFor()) {
				if (currentItem.getHighestBidder() == clients[i].getID()) {
					clients[i].send("\nYou just bid " 
						+ currentItem.getHighestBid()
						+ " for " + currentItem.getName() + "\n\n"
						+ "Enter Bid>\t");
				}
				else {
					clients[i].send("\n\n" + currentItem.getHighestBidderName()
						+ " just bid " + currentItem.getHighestBid()
						+ " for " + currentItem.getName() + "\n\n"
						+ "Enter Bid>\t");
				}
			}
			else {
				clients[i].send("\n\nThe current item on auction is:\t" 
								+ currentItem.getName()
								+ "\nThe starting price for this item is:\t" 
								+ currentItem.getStartingPrice() + "\n\n"
								+ "Enter Bid>\t");
			}
		}
		
		// Assume no bid has been made
		currentItemArrayIndex++;
		
		// If last item in list has been reached, return to first item
		if (currentItemArrayIndex == items.size()) {
			currentItemArrayIndex = 0;
		}
		notifyAll();
	}
	
	public void itemCountdown() {
		timer = new Timer();
		TimerTask task = new TimerTask() {
			public void run() {
				displayCurrentItem();
			}
		};
		timer.scheduleAtFixedRate(task, 0, 10000);
	}

	public synchronized void getBid(int ID, String bid) {
		int whiteSpaceIndex = bid.indexOf(' ');
		int clientBid = Integer.parseInt(bid.substring(0, whiteSpaceIndex));
		String bidderName = bid.substring(whiteSpaceIndex + 1, bid.length());
		if (clientBid <= currentItem.getHighestBid()) {
			// Step 4: send data
			// send alert message to client
			clients[findClient(ID)].send("\nBid must be greater than current bid!\n\nEnter Bid>\t");
		}
		else if (clientBid > currentItem.getHighestBid()) {
			timer.cancel();
			timer.purge();
			currentItem.setHighestBid(clientBid);
			currentItem.setHighestBidder(ID);
			currentItem.setHighestBidderName(bidderName);
			if (!currentItem.isBiddedFor()) {
				currentItem.setBiddedFor(true);
			}
			currentItemArrayIndex--;
			itemCountdown();
		}
		notifyAll();
	}
	
	public Item getCurrentItem() {
		return currentItem;
	}
	
	public synchronized void remove(int ID)
	{
		int pos = findClient(ID);
		if (pos >= 0) {
			AuctionServerThread toTerminate = clients[pos];
			System.out.println("Removing client thread " + ID + " at " + pos);

			if (pos < clientCount-1) {
				for (int i = pos+1; i < clientCount; i++) {
					clients[i-1] = clients[i];
				}
			}
			clientCount--;

			try {
				toTerminate.close();
			}
			catch(IOException ioe) {
				System.out.println("Error closing thread: " + ioe);
			}
			toTerminate = null;
			System.out.println("Client " + pos + " removed");
			notifyAll();
		}
	}

   private void addThread(Socket socket)
   {
		if (clientCount < clients.length){
			System.out.println("Client accepted: " + socket);
			// adding server and socket information to AuctionServerThread array
			clients[clientCount] = new AuctionServerThread(this, socket);
			try {
				// Step 3: Set up input and output streams
				clients[clientCount].open();
				// Call the run method of the client just added
				clients[clientCount].start();
				clientCount++;
			}
			catch(IOException ioe) {
				System.out.println("Error opening thread: " + ioe);
			}
		}
		else {
			System.out.println("Client refused: maximum " + clients.length + " reached.");
		}
	}


   public static void main(String args[]) {
		AuctionServer server = null;
		if (args.length != 1) {
			System.out.println("Usage: java AuctionServer port");
		}
		else {
			server = new AuctionServer(Integer.parseInt(args[0]));
		}
   }

}