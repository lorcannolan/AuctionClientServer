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
   private Item currentItem, newItem;
   private int currentItemArrayIndex = 0;
   
   private String newItemName = "";
   private int newItemValue = 0;
   private int addingNewItem = 0;
   
   private Timer displayItemTimer, countdownTimer;
   
   private int seconds = 60;

   public AuctionServer(int port)
   {
		addAuctionItems();
		// Auction starts when server starts
		itemCountdown();
		clockCountdown();
		try {
			System.out.println("Binding to port " + port + ", please wait  ...");
			// Create ServerSocket object
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
	
	// Creating new items and adding them one-by-one to ArrayList items
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
				// Put the server into awaiting state
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

	// This method finds the index of a client thread based on port number
	private int findClient(int ID) {
		for (int i = 0; i < clientCount; i++) {
			if (clients[i].getID() == ID) {
				return i;
			}
		}
		return -1;
	}
	
	// This method displays the current item up for auction every 60 seconds
	public void itemCountdown() {
		displayItemTimer = new Timer();
		TimerTask displayItemTask = new TimerTask() {
			public void run() {
				displayCurrentItem();
				countdownTimer.cancel();
				countdownTimer.purge();
				seconds = 60;
				clockCountdown();
			}
		};
		displayItemTimer.scheduleAtFixedRate(displayItemTask, 0, 10000);
	}
	
	// Display current item to clients
	public synchronized void displayCurrentItem() {
		Item previousItem;
		if (currentItemArrayIndex == 0) {
			previousItem = items.get(items.size()-1);
		}
		else {
			previousItem = items.get(currentItemArrayIndex-1);
		}
		
		/*
		If time ran out on the last item and a bid was made for it,
		alert bidder of previous item if they were successful in their bid
		*/
		if (previousItem.isBiddedFor()) {
			try {
				clients[findClient(previousItem.getHighestBidder())]
					.send("\n\nCongratulations! You successfully purchased "
					+ previousItem.getName() + " for a value of " + previousItem.getHighestBid());
				items.remove(previousItem);
				// As items in the list are shifted back one position, current index must re-adjust
				if (currentItemArrayIndex != 0) {
					currentItemArrayIndex--;
				}
			}
			catch (ArrayIndexOutOfBoundsException aioobe) {
				/* 
				If client makes bid and leaves the auction server, 
				reset item and return to it at later stage of the auction
				*/
				previousItem.setHighestBid(previousItem.getStartingPrice());
				previousItem.setBiddedFor(false);
				previousItem.setHighestBidder(0);
				previousItem.setHighestBidderName("");
				for (int i = 0; i < clientCount; i++) {
					// Inform the cilents of reset
					clients[i].send("\n\n" + previousItem.getName() 
						+ " value has been reset as highest bidder"
						+ " left the Auction before receiving item.");
				}
			}
		}
		
		// Get the current item being auctioned for
		currentItem = items.get(currentItemArrayIndex);
		
		for (int i = 0; i < clientCount; i++) {
			// If a client is currently adding a new item, do not send message
			if (clients[i].getID() != addingNewItem) {
				// If a bid was just made for this item inform the bidder and other clients respectively
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
				// If no bid has been made for new item being displayed
				else {
					clients[i].send("\n\nTo leave auction, enter QUIT.\nTo add a new item enter NEW."
									+ "\n\nThe current item on auction is:\t" 
									+ currentItem.getName()
									+ "\nThe starting price for this item is:\t" 
									+ currentItem.getStartingPrice() + "\n\n"
									+ "Enter Bid>\t");
				}
			}
		}
		
		// Assume no bid has been made and increment current index
		currentItemArrayIndex++;
		
		// If last item in list has been reached, return to first item
		if (currentItemArrayIndex == items.size()) {
			currentItemArrayIndex = 0;
		}
		notifyAll();
	}
	
	// This method displays the time remaining on current auction item every 10 seconds
	public void clockCountdown() {
		countdownTimer = new Timer();
		TimerTask displayCountdownTask = new TimerTask() {
			public void run() {
				displayCurrentTime();
			}
		};
		countdownTimer.scheduleAtFixedRate(displayCountdownTask, 10000, 10000);
	}
	
	public synchronized void displayCurrentTime() {
		seconds -= 10;
		for (int i = 0; i < clientCount; i++) {
			if (clients[i].getID() != addingNewItem) {
				clients[i].send("\n\n" + seconds + " seconds left on current item!"
								+ "\n\nEnter Bid>\t");
			}
		}
	}

	// This method handles input received from clients
	public synchronized void getInput(int ID, String bid) {
		int whiteSpaceIndex = bid.lastIndexOf(' ');
		/*
		***Name*** is a unique string attached to the end of client input which indicates
		that the first part of this string the name of the new item to be added.
		*/
		if (bid.contains("***Name***")) {
			newItemName = bid.substring(0, whiteSpaceIndex);
			addNewItemValue(ID);
		}
		/*
		Similarly, ***Value*** indicates that the value of the new item is to be added.
		*/
		else if (bid.contains("***Value***")) {
			newItemValue = Integer.parseInt(bid.substring(0, whiteSpaceIndex));
			newItem = new Item(newItemValue, newItemName);
			items.add(newItem);
			addingNewItem = 0;
			clients[findClient(ID)].send("\n\nThe current item on auction is:\t" 
											+ currentItem.getName()
											+ "\nThe starting price for this item is:\t" 
											+ currentItem.getStartingPrice() + "\n\n"
											+ "Enter Bid>\t");
		}
		// If client wants to enter new item
		else if (bid.substring(0, whiteSpaceIndex).equals("NEW")) {
			addNewItemName(ID);
			addingNewItem = ID;
		}
		// If the above 2 unique strings have not been attached, assume a bid has been entered by client
		else {
			int clientBid = Integer.parseInt(bid.substring(0, whiteSpaceIndex));
			handleBid(ID, bid);
		}
		notifyAll();
	}
	
	// This method will check if inputted bid is greater than current bid
	public void handleBid(int ID, String bid) {
		int whiteSpaceIndex = bid.indexOf(' ');
		int clientBid = Integer.parseInt(bid.substring(0, whiteSpaceIndex));
		String bidderName = bid.substring(whiteSpaceIndex + 1, bid.length());
		if (clientBid <= currentItem.getHighestBid()) {
			// Inform client that they did not enter enough
			clients[findClient(ID)].send("\nBid must be greater than current bid!\n\nEnter Bid>\t");
		}
		else if (clientBid > currentItem.getHighestBid()) {
			// cancel current timers and remove cancelled tasks from timer queue
			displayItemTimer.cancel();
			displayItemTimer.purge();
			countdownTimer.cancel();
			countdownTimer.purge();
			seconds = 60;
			// Set new values for current item on auction
			currentItem.setHighestBid(clientBid);
			currentItem.setHighestBidder(ID);
			currentItem.setHighestBidderName(bidderName);
			if (!currentItem.isBiddedFor()) {
				currentItem.setBiddedFor(true);
			}
			
			/*
			If an item was bid for successfully, current item displayed will need to be that
			same item to give other clients a chance to outbid each other.
			*/
			if (currentItemArrayIndex == 0) {
				currentItemArrayIndex = items.size() - 1;
			}
			else {
				currentItemArrayIndex--;
			}
			// Start new countdown
			itemCountdown();
		}
	}
	
	// Inform client who wants to add new item to enter the name of the new item
	public void addNewItemName(int ID) {
		clients[findClient(ID)].send("\nEnter Name of new item:\t");
	}
	
	// Inform client who wants to add new item to enter the starting price of the new item
	public void addNewItemValue(int ID) {
		clients[findClient(ID)].send("\nEnter Value of new item:\t");
	}
	
	// retrieves current item being auctioned for
	public Item getCurrentItem() {
		return currentItem;
	}
	
	// This method removes a client
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
				// Set up input and output streams
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