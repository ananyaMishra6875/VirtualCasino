/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package virtual.casino;

/**
 *
 * @author ananya.mishra
 */

//imports
import java.util.*;
import javax.swing.JOptionPane;
import java.io.*;
import virtual.casino.core.BlackJack;
import virtual.casino.core.Dice;
import virtual.casino.core.HiLo;
import virtual.casino.core.Lottery;
import virtual.casino.core.PasswordReset;
import virtual.casino.core.SkillTest;

public class VirtualCasino {

    /**
     * @param args the command line arguments
     */
    
    public static void main(String[] args) {
        //set up imports (scanner, random)
        Scanner s = new Scanner(System.in);
        Random r = new Random();
        //display splash screen
        for (int i=1; i<=6; i++) {
            System.out.println("");
            for (int j = 1; j<=i; j++) {
                System.out.print("* ");
            }
        }
        System.out.println("\nV I R T U A L");
        System.out.print("C A S I N O !");
        for (int i=6; i>=1; i--) {
            System.out.println("");
            for (int j=1; j<=i; j++) {
                System.out.print("* ");
            }
        }
        
        //game prize money amount
        final int dice = Dice.PRIZE;
        final int hiLo = HiLo.PRIZE;
        final int bj = BlackJack.PRIZE;
        final int resetPwd = PasswordReset.COST;
        final int lotto = Lottery.TICKET_COST;
        
        //default user settings
        int vcAccess = 0;
        int tokens = 100;
        int debt = 0;
        String correctPassword = "secret";
        String userPassword = "";
        
        LinkedHashMap <String, LinkedHashMap<String, String>> fileData = new LinkedHashMap<>();
        int lotteryNumAmt = Lottery.NUMBERS;
        int init = Lottery.MIN;
        int end = Lottery.MAX;
        
        //ask for name and password
        String name = JOptionPane.showInputDialog("Enter your username.");
        //if casino manager enters, they dont need to enter password
        if (name.equals("M0N3YL0VR")){
            vcAccess = 2;
        }
        //login for users
        else{
            userPassword = JOptionPane.showInputDialog("Enter your password.");
            //load all user data from file into hashmap
            fileData = readInfo();
            //if name is registered on file, then fetch user info
            if (fileData.get(name) != null) {
                correctPassword = fileData.get(name).get("password");
                tokens = Integer.parseInt(fileData.get(name).get("token"));
                debt = Integer.parseInt(fileData.get(name).get("debt"));
            }
            //if they get the password right, give them access
            if(userPassword.equals(correctPassword)) {
                vcAccess = 1;
            }
            //give them two more tries for the password
            else {
                for (int i=0; i<2; i++) {
                    userPassword = JOptionPane.showInputDialog("Incorrect Password - Try Again.");
                    //if they get the password right, give them access
                    if(userPassword.equals(correctPassword)){
                        vcAccess = 1;
                        break;
                    }
                }
            }
        }
        int hasEntered = vcAccess;
        //if they have access, go to vc
        while(vcAccess == 1) {
            //display welcome only the first time they enter
            if (hasEntered == 1) {
                System.out.println("\n\nHello "+name+", and welcome to The Virtual Casino!");
                hasEntered = 0;
            }
            menu(tokens, debt, dice, hiLo, bj, resetPwd, lotto);
            String menuSelect = s.nextLine().toLowerCase();
            //determine what to do based on what menu option they select
            switch(menuSelect) {
                case "dice" -> {
                    System.out.println("\nWelcome to CRAPS!");
                    String userCRAPSGuess = JOptionPane.showInputDialog("Select a number between 4-12.");
                    int crapsGuess = Integer.parseInt(userCRAPSGuess);
                    //the rules now live in core/Dice.java
                    Dice.Result diceResult = new Dice(r).play(crapsGuess);
                    if (diceResult.won()) {
                        System.out.println("You won! +"+dice+" Tokens");
                    }
                    else {
                        System.out.println("You lost! -"+dice+" Tokens");
                    }
                    tokens += diceResult.tokenChange();
                }
                case "hi-lo" -> {
                    System.out.println("\nWelcome to Hi-Lo!");
                    //create the deck (52 cards) and shuffle
                    ArrayList<Integer> hiLoDeck = shuffleDeck(createDeck("hiLo"));
                    tokens += hiLoGame(hiLoDeck);
                }
                case "blackjack" -> {
                	System.out.println("\nWelcome to BlackJack!");
                	//create the deck (52 cards) and shuffle
                    ArrayList<Integer> bjDeck = shuffleDeck(createDeck("bj"));
                    //the rules now live in core/BlackJack.java
                    BlackJack game = new BlackJack(bjDeck);

                    //player's turn
                    while (!game.isOver()) {
                    	System.out.println("\nYour hand: "+game.playerHand()+" ("+BlackJack.handTotal(game.playerHand())+")");
                    	System.out.println("- Hit\n- Stand");
                    	System.out.print("Do you want to hit or stand? ");
                    	String userBjChoice = s.nextLine().toLowerCase();
                    	switch(userBjChoice) {
                    		case "hit" -> game.hit();
                    		case "stand" -> game.stand();
                    	}
                    }

                    //display hands
                    BlackJack.Result bjResult = game.result();
                    System.out.println("Your hand: "+game.playerHand()+" ("+bjResult.playerTotal()+")");
                    System.out.println("Dealer's hand: "+game.dealerHand()+" ("+bjResult.dealerTotal()+")");

                	//win/lose tokens based on cards
                    switch (bjResult.outcome()) {
                    	case BUST -> System.out.println("You bust! -"+bj+" Tokens");
                    	case WIN -> System.out.println("You won! +"+bj+" Tokens");
                    	case LOSE -> System.out.println("You lost! -"+bj+" Tokens");
                    	case PUSH -> System.out.println("Push! It's a tie, no tokens won or lost.");
                    }
                    tokens += bjResult.tokenChange();
                }
                case "reset password" -> {
                    JOptionPane.showMessageDialog(null, "Generating new password...");
                    //the rules now live in core/PasswordReset.java
                    userPassword = PasswordReset.generate(r);

                    JOptionPane.showMessageDialog(null,"Your new password is: "+userPassword);
                    tokens -= resetPwd;
                }
                case "lottery ticket" -> {
                    int[] userNumList = new int[lotteryNumAmt];
                    System.out.println("\nWelcome to the Lottery!");
                    System.out.println("- Choose\n- Randomize");
                    System.out.print("Would you like to choose or randomize your numbers ("+lotteryNumAmt+")? ");
                    String lotteryChoice = s.nextLine().toLowerCase();
                    switch(lotteryChoice){
                        //let them choose 5 distinct numbers from 1-30
                        case "choose" -> {
                            System.out.print("\n");
                            for(int i=0; i<lotteryNumAmt; i++){
                                System.out.print("Enter a number from "+init+"-"+end+": ");
                                int userNum = s.nextInt();
                                while(!Lottery.inRange(userNum) || Lottery.numberExists(userNum,userNumList)) {
                                	//check for number in range
                                    while(!Lottery.inRange(userNum)){
                                        System.out.print("Number out of given range. Enter a number from "+init+"-"+end+": ");
                                        userNum = s.nextInt();
                                    }
                                    //check if number entered already exists in the list
                                    while (Lottery.numberExists(userNum,userNumList)){
                                        System.out.print("Number already used. Enter a different number from "+init+"-"+end+": ");
                                        userNum = s.nextInt();
                                    }
                                }
                                userNumList[i] = userNum;
                            }
                            //flush scanner
                            s.nextLine();
                            System.out.println("Your lottery ticket has been submitted!");
                            tokens -= lotto;
                        }
                        //randomize 5 distinct numbers from 1-30
                        case "randomize" -> {
                            System.out.println("Randomizing "+lotteryNumAmt+" numbers from "+init+"-"+end+"...");
                            userNumList = Lottery.randomNumbers(r);
                            System.out.println("Your lottery ticket has been submitted!");
                            tokens -= lotto;
                        }
                        default -> System.out.println("Invalid option. No ticket was bought.");
                    }
                    //store lotto data in file, only if a ticket was actually bought
                    if (Lottery.isValidTicket(userNumList)) {
                        lotteryData(name,userNumList);
                    }
                }
                case "cash out" -> {
                    //the rules now live in core/SkillTest.java
                    SkillTest skillTest = new SkillTest(r);
                    if (tokens<0){
                        System.out.println("\nYour balance is "+0+" Tokens");
                        System.out.println("**You are "+(tokens*(-1))+" tokens in debt!**");
                    }
                    else{
                        System.out.println("\nYour balance is "+tokens+" Tokens");
                    }
                    System.out.println("Skill Testing Question required to exit:");
                    //display question in the format a+b*c, with 3 tries
                    while (!skillTest.passed()) {
                        System.out.print(skillTest.question().text()+" = ");
                        int userStqAnswer = s.nextInt();
                        skillTest.answer(userStqAnswer);
                        if (skillTest.failed()) {
                            System.exit(0);
                        }
                        if (!skillTest.passed()) {
                            System.out.println("Incorrect Answer. Try Again:");
                        }
                    }
                    System.out.println("\nCorrect answer! Be sure to return to The Virtual Casino!");
                    
                    //if name is found in file (not null), put info
                    if (fileData.get(name) != null) {
                        //Update the hashmap with updated user data
                        fileData.get(name).put("password",userPassword);
                        fileData.get(name).put("token",String.valueOf(tokens));
                    }
                    //if name is not found in file, create hashmap for new info
                    else {
                        LinkedHashMap <String,String> localHM = new LinkedHashMap<>();
                        localHM.put("password",userPassword);
                        localHM.put("token",String.valueOf(tokens));
                        fileData.put(name,localHM);
                    }
                    //save the fileData hashmap to the file
                    userInfo(fileData);
                    
                    System.exit(0);
                }
                default -> {
                	System.out.println("\nInvalid menu option. Redirecting back to menu...");
                }
            }
        }
        //if casino manager enters
        if(vcAccess == 2){
            JOptionPane.showMessageDialog(null, "Welcome, Casino Manager! Would you like to draw the lottery?");
            //generate and store lottery numbers in array
            int[] lotteryNums = Lottery.randomNumbers(new Random());
            System.out.println("\n\nLottery Numbers: "+Arrays.toString(lotteryNums));
            //call lotteryPrize() method to payout lottery wins
            lotteryPrize(lotteryNums);
            System.out.println("\nThank you for visisting!");
        }
    }
    
    //create a method to display the menu
    public static void menu(int tokens, int debt, int dice, int hiLo, int bj, int resetPwd, int lotto){
        System.out.println("\nBalance: "+((tokens<0) ? 0 : tokens)+" Tokens");
            if (tokens<0) {
                debt = tokens*(-1);
                tokens = 0;
                System.out.println("**You are "+debt+" Tokens in debt!**");
            }
            System.out.println("Menu:\n- Dice ("+dice+" Tokens)\n- Hi-Lo ("+hiLo+" Tokens)\n- BlackJack ("+bj+" Tokens)\n- Reset Password ("+resetPwd+" Tokens) \n- Lottery Ticket ("+lotto+" Tokens)\n- Cash Out");
            System.out.print("What would you like to do? ");
    }
    
    //create a method to store user information in a file
    public static void userInfo(LinkedHashMap <String, LinkedHashMap<String, String>> fileData){
        try{
            //create file to write to
            PrintWriter pw = new PrintWriter(new File("userInfo.txt"));
            //read each user data in the for loop
            for (String key : fileData.keySet()){
                String fileRecord = key + " " + fileData.get(key).get("password") + " " + fileData.get(key).get("token") + " " + fileData.get(key).get("debt");
                pw.println(fileRecord);
            }
            pw.close();
        }
        catch(IOException e){
            System.out.println("An error occurred: "+e);
        }
    }
    
    //create a method to read in the info stored in userInfo() and update password
    public static LinkedHashMap <String, LinkedHashMap<String, String>> readInfo(){
        //create hashmaps to store user info
        LinkedHashMap <String, LinkedHashMap<String, String>> fileData = new LinkedHashMap <>();
        try{
            Scanner s = new Scanner(new File("userInfo.txt"));
            while(s.hasNext()){
                LinkedHashMap <String, String> userData = new LinkedHashMap<>();
                //grab next() and add local variables for names, passwords, and tokens to hashmap
                String localName = s.next();
                String localPassword = s.next();
                String localToken = s.next();
                String localDebt = s.next();
                
                userData.put("password",localPassword);
                userData.put("token",localToken);
                userData.put("debt", localDebt);
                fileData.put(localName, userData);
            }
        } catch(IOException e){
            System.out.println("An error has occurred: "+e);
        }
        //return file data with all users info
        return fileData;
    }
    
    //create a method to store user lottery ticket numbers in a file
    public static void lotteryData(String name, int[] lottoNums){
        try{
            //create file to write to
            PrintWriter pw = new PrintWriter(new FileWriter("lottery.txt",true));
            pw.println(name+"|"+Arrays.toString(lottoNums));
            pw.close();
        }
        catch(IOException e){
            System.out.println("An error has occurred: "+e);
        }
    }
    
    //create a method to read lottery info and give out prizes to winners
    public static void lotteryPrize(int[] lotteryNums){
        try{
            Scanner s = new Scanner(new File("lottery.txt"));
            while(s.hasNext()){
                //read the line into a variable
                String sLine = s.nextLine();
                
                //get the name from the lottery file
                String name = sLine.split("\\|")[0];
                
                String lotteryNumbers = sLine.split("\\|")[1];
                String lottoList = lotteryNumbers.split("\\[")[1].split("\\]")[0]; //gets string of lotto numbers
                String[] ticketParts = lottoList.split(", ");
                int[] ticket = new int[ticketParts.length];
                for(int i=0; i<ticketParts.length; i++){
                    ticket[i] = Integer.parseInt(ticketParts[i]);
                }
                //count how many numbers are correct and determine prize money (rules in core/Lottery.java)
                int prize = Lottery.prize(Lottery.matches(ticket, lotteryNums));
                //update user payout with prize and save updated data to the file
                userInfo(updateUserPayout(name, prize));
            }
            //clear lottery file
            PrintWriter pw = new PrintWriter(new File("lottery.txt"));
            pw.close();
        }
        catch(IOException e){
            System.out.println("An error has occurred: "+e);
        }
    }
    
    //create a method to update user payout with the amount of tokens won
    public static LinkedHashMap <String, LinkedHashMap<String, String>> updateUserPayout(String name, int prize){
        LinkedHashMap <String, LinkedHashMap<String, String>> fileData = new LinkedHashMap<>();
        try{
            //update token value with prize specific to the name associated with the prize
            fileData = readInfo();
            String sToken = fileData.get(name).get("token");
            sToken = String.valueOf(Integer.parseInt(sToken) + prize);
            fileData.get(name).put("token", sToken);
        }
        catch(Exception e){
            System.out.println("An error has occured: "+e);
        }
        return fileData;
    }
    
    //create a method to shuffle cards
    public static ArrayList<Integer> shuffleDeck(ArrayList<Integer> deck) {
    	ArrayList<Integer> deck2 = new ArrayList<Integer>();
    	Random r = new Random();
    	int size = deck.size();
    	for(int i=0; i<size; i++) {
    		int card = r.nextInt(deck.size());
    		deck2.add(deck.get(card));
    		deck.remove(card);
    	}
    	return deck2;
    }
    
    //create a method for the hi-lo game returning the amount of tokens won/lost after playing
    public static int hiLoGame(ArrayList<Integer> deck) {
    	Scanner s = new Scanner(System.in);
    	//the rules now live in core/HiLo.java
    	HiLo game = new HiLo(deck);
    	System.out.println("A "+HiLo.cardName(game.currentCard())+" has been drawn.");
    	while (true) {
    		System.out.println("- Lower\n- Higher");
    		System.out.print("Is the next card going to be lower or higher? ");
    		String userGuess = s.nextLine().toLowerCase();
    		HiLo.Guess guess = switch (userGuess) {
    			case "lower" -> HiLo.Guess.LOWER;
    			case "higher" -> HiLo.Guess.HIGHER;
    			default -> null;
    		};
    		HiLo.Turn turn = game.guess(guess);

    		//on a tie, guess again
    		if (turn.outcome() == HiLo.Outcome.TIE) {
    			System.out.println("It appears it was a tie! Guess again for a bigger prize.");
    			continue;
    		}

    		System.out.print("The next card was a "+HiLo.cardName(turn.nextCard())+". ");

    		//return winning/losing money
    		if (turn.outcome() == HiLo.Outcome.WIN) {
    			System.out.println("You won! +"+turn.tokenChange()+" Tokens");
    		}
    		else {
    			System.out.println("You lost! -"+HiLo.PRIZE+" Tokens");
    		}
    		return turn.tokenChange();
    	}
    }
    
    //create a method to create a deck of cards
    public static ArrayList<Integer> createDeck(String game) {
    	switch(game) {
    		//deck for hi-lo (52 cards 1-13)
	    	case "hiLo" -> {
	    		ArrayList<Integer> deck = new ArrayList<Integer>();
	        	for(int i=1; i<=13; i++){
	            	for(int j=0; j<4; j++) {
	            		deck.add(i);
	            	}
	            }
	        	return deck;
	    	}
	    	//deck for blackjack
	    	case "bj" -> {
	    		ArrayList<Integer> deck = new ArrayList<Integer>();
	        	for(int i=1; i<=10; i++){
	            	for(int j=0; j<4; j++) {
	            		deck.add(i);
	            	}
	            }
	        	//j,q,k are 10s
	        	for(int i=0; i<12; i++) {
            		deck.add(10);
            	}
	        	return deck;
	    	}
	    	default -> {
	    		ArrayList<Integer> deck = new ArrayList<Integer>();
	    		return deck;
	    	}
    	}
    	
    }
    
    
}