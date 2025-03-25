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
        final int dice = 10;
        final int hiLo = 15;
        final int bj = 20;
        final int resetPwd = 5;
        final int lotto = 5;
        
        //default user settings
        int vcAccess = 0;
        int tokens = 100;
        int debt = 0;
        String correctPassword = "secret";
        String userPassword = "";
        
        LinkedHashMap <String, LinkedHashMap<String, String>> fileData = new LinkedHashMap<>();
        int lotteryNumAmt = 5;
        int init = 1;
        int end = 30;
        
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
                    int diceRoll = (r.nextInt(6)+1)+(r.nextInt(6)+1);
                    if (diceRoll == 2 || diceRoll == 3) {
                        diceRoll = (r.nextInt(6)+1)+(r.nextInt(6)+1);
                        if (crapsGuess == diceRoll) {
                            System.out.println("You won! +"+dice+" Tokens");
                            tokens += dice;
                        }
                        else {
                            System.out.println("You lost! -"+dice+" Tokens");
                            tokens -= dice;
                        }
                    }
                    else if (crapsGuess == diceRoll) {
                        System.out.println("You won! +"+dice+" Tokens");
                        tokens += dice;
                    }
                    else {
                        System.out.println("You lost! -"+dice+" Tokens");
                        tokens -= dice;
                    }
                }
                case "hi-lo" -> {
                    System.out.println("\nWelcome to Hi-Lo!");
                    //create the deck (52 cards) and shuffle
                    ArrayList<Integer> hiLoDeck = shuffleDeck(createDeck("hiLo"));
                    tokens += hiLoGame(hiLoDeck,hiLo);
                }
                case "blackjack" -> {
                	System.out.println("\nWelcome to BlackJack!");
                	//create the deck (52 cards) and shuffle
                    ArrayList<Integer> bjDeck = shuffleDeck(createDeck("bj"));
                    ArrayList<Integer> userHand = new ArrayList<>();
                    ArrayList<Integer> dealerHand = new ArrayList<>();
                    int card = 0;
                    //start off by dealing 2 cards to the dealer and player
                    for (int i=0; i<4; i++) {
                    	if (i%2 == 0) {
                    		userHand.add(bjDeck.get(card));
                    	}
                    	else {
                    		dealerHand.add(bjDeck.get(card));
                    	}
                    	bjDeck.remove(card);
                    }
                    
                    boolean bjContinue = true;
                    
                    //player's turn
                    do {
                    	System.out.println("\nYour hand: "+userHand);
                    	System.out.println("- Hit\n- Stand");
                    	System.out.print("Do you want to hit or stand? ");
                    	String userBjChoice = s.nextLine().toLowerCase();
                    	switch(userBjChoice) {
                    		case "hit" -> {
                    			userHand.add(bjDeck.get(card));
                    			bjDeck.remove(card);
                    		}
                    		case "stand" -> {
                    			bjContinue = false;
                    		}
                    	}
                    } while ((handSum(userHand) < 21) && (bjContinue));
                    
                    //dealer's turn
                    while (handSum(dealerHand) < 17) {
                    	dealerHand.add(bjDeck.get(card));
            			bjDeck.remove(card);
                    }
                    
                    //display hands
                    System.out.println("Your hand: "+userHand);
                    System.out.println("Dealer's hand: "+dealerHand);
                    
                	//win/lose tokens based on cards
                    if (handSum(userHand) > 21) {
                    	System.out.println("You bust! -"+bj+" Tokens");
                    	tokens -= bj;
                    }
                    else if ((handSum(userHand) > handSum(dealerHand)) || (handSum(dealerHand) > 21)) {
                    	System.out.println("You won! +"+bj+" Tokens");
                    	tokens += bj;
                    }
                    else if (handSum(userHand) < handSum(dealerHand)) {
                    	System.out.println("You lost! -"+bj+" Tokens");
                    	tokens -= bj;
                    }
                }
                case "reset password" -> {
                    JOptionPane.showMessageDialog(null, "Generating new password...");
                    userPassword = "";
                    
                    //generate a random letter for first character
                    userPassword = resetPassword(r, userPassword, 2);
                    
                    //generate a random character for the second, third, fourth, fifth letter
                    for(int i = 0; i<4; i++) {
                        userPassword = resetPassword(r, userPassword, 3);
                    }
                    
                    //generate a random letter for last character
                    userPassword = resetPassword(r, userPassword, 2);
                    
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
                                while((userNum<init || userNum>end) || (numberExists(userNum,userNumList))) {
                                	//check for number in range
                                    while(userNum<init || userNum>end){
                                        System.out.print("Number out of given range. Enter a number from "+init+"-"+end+": ");
                                        userNum = s.nextInt();
                                    }
                                    //check if number entered already exists in the list
                                    while (numberExists(userNum,userNumList)){
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
                            userNumList = lottery(init, end, lotteryNumAmt);
                            System.out.println("Your lottery ticket has been submitted!");
                            tokens -= lotto;
                        }
                    }
                    //store lotto data in file
                    lotteryData(name,userNumList);
                }
                case "cash out" -> {
                    //randomize A B and C for the stq and get the answer
                    int stqA = r.nextInt(15)+1;
                    int stqB = r.nextInt(15)+1;
                    int stqC = r.nextInt(15)+1;
                    int stqAnswer = stqA+stqB*stqC;
                    if (tokens<0){
                        System.out.println("\nYour balance is "+0+" Tokens");
                        System.out.println("**You are "+(tokens*(-1))+" tokens in debt!**");
                    }
                    else{
                        System.out.println("\nYour balance is "+tokens+" Tokens");
                    }
                    System.out.println("Skill Testing Question required to exit:");
                    System.out.print(stqA+" + "+stqB+" * "+stqC+" = ");
                    int userStqAnswer = s.nextInt();
                    if (userStqAnswer != stqAnswer) {
                        //give them two more tries
                        for (int i = 0; i<2; i++) {
                            stqA = r.nextInt(15)+1;
                            stqB = r.nextInt(15)+1;
                            stqC = r.nextInt(15)+1;
                            stqAnswer = stqA+stqB*stqC;
                            //display question in the format a+b*c
                            System.out.println("Incorrect Answer. Try Again:");
                            System.out.print(stqA+" + "+stqB+" * "+stqC+" = ");
                            userStqAnswer = s.nextInt();
                            if (userStqAnswer == stqAnswer) {
                                break;
                            }
                            else if(i == 1 && userStqAnswer != stqAnswer) {
                                System.exit(0);
                            }
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
            int[] lotteryNums = lottery(init, end, lotteryNumAmt);
            System.out.println("\n\nLottery Numbers: "+Arrays.toString(lotteryNums));
            //call lotteryPrize() method to payout lottery wins
            lotteryPrize(lotteryNums);
            System.out.println("\nThank you for visisting!");
        }
    }
    
    //create a method to reset the password with Random, password, and how many numbers to randomize parameters
    public static String resetPassword(Random r, String password, int num){
        int passwordChar = r.nextInt(num);
        switch(passwordChar){
            //randomizes lowercase, uppercase, and numbers based on randomized case
            case 0 -> password = password + (char)(r.nextInt(26)+'a');
            case 1 -> password = password + (char)(r.nextInt(26)+'A');
            case 2 -> password = password + (r.nextInt(10)+1);
        }
        return password;
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
    
    //create a method to check if number entered already exists in the list (for the lottery)
    public static boolean numberExists(int number, int[] numList){
        //System.out.println("number: " + number);
        //System.out.println("numList: " + Arrays.toString(numList));
        boolean result = false;
        for (int num: numList){
            if (number == num){
                result = true;
                break;
            }
        }
        return result;
    }
    
    //create a method to randomly generate distinct lottery numbers
    public static int[] lottery(int init, int end, int nums){
        Random r = new Random();
        int[] lotteryNums = new int[nums];
        int randNum = 0;
        for (int i=0; i<nums; i++){
            boolean isDistinct;
            do{
                isDistinct = true;
                //generate random number within range
                randNum = r.nextInt(end-init+1)+init;
                //check if number is already used
                for (int value : lotteryNums){
                    if (randNum == value) {
                        isDistinct = false; //not distinct
                    }
                }
            }
            while (!isDistinct); //repeats until distinct number is found
            lotteryNums[i] = randNum;
        }
        return lotteryNums;
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
        int lotteryNumAmt = lotteryNums.length;
        try{
            Scanner s = new Scanner(new File("lottery.txt"));
            while(s.hasNext()){
                int count = 0;
                int prize = 0;
                
                //read the line into a variable
                String sLine = s.nextLine();
                
                //get the name from the lottery file
                String name = sLine.split("\\|")[0];
                
                String lotteryNumbers = sLine.split("\\|")[1];
                //count how many numbers are correct
                String lottoList = lotteryNumbers.split("\\[")[1].split("\\]")[0]; //gets string of lotto numbers
                for(int i=0; i<lotteryNumAmt; i++){
                    for (int lottoNum : lotteryNums){
                        if (Integer.parseInt(lottoList.split(", ")[i]) == lottoNum){
                            count++;
                        }
                    }
                }
                //determine prize money based on count
                switch(count){
                    case 3 -> prize = 5;
                    case 4 -> prize = 2000;
                    case 5 -> prize = 10000;
                }
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
    public static int hiLoGame(ArrayList<Integer> deck, int hiLo) {
    	//System.out.println(deck);
    	Scanner s = new Scanner(System.in);
    	boolean win = false;
    	int ties = 0;
    	int cardIndex = 0;
    	int card = deck.get(cardIndex);
    	String cardDisplay = "";
    	int newCard = deck.get(cardIndex+1);
    	switch(card) {
    		case  1 -> cardDisplay = "A";
    		case 11 -> cardDisplay = "J";
    		case 12 -> cardDisplay = "Q";
    		case 13 -> cardDisplay = "K";
    		default -> cardDisplay = String.valueOf(card);
    	}
    	System.out.println("A "+cardDisplay+" has been drawn.");
		System.out.println("- Lower\n- Higher");
		System.out.print("Is the next card going to be lower or higher? ");
		String userGuess = s.nextLine();
		
		//if new card is the same as the old one
		if (newCard == card) {
			//keep dealing cards until no more ties
			do {
				System.out.println("It appears it was a tie!");
				ties++;
				cardIndex++;
				card = deck.get(cardIndex);
		    	newCard = deck.get(cardIndex+1);
		    	System.out.println("- Lower\n- Higher");
				System.out.print("Is the next card going to be lower or higher? ");
				userGuess = s.nextLine();
			} while(newCard == card);
			
			//if they only tied once, they lose, otherwise they win 2n times the bet
			if (ties > 1) {
				System.out.println("You won! +"+(hiLo*ties*2)+" Tokens");
				return hiLo*ties*2;
			}
			else {
				System.out.println("You lost! -"+hiLo+" Tokens");
				return hiLo*(-1);
			}
		}
		
		//if new card is higher than the old one
		else if (newCard < card) {
			win = (userGuess.toLowerCase().equals("lower")) ? true : false;
		}
		
		//if new card is lower than the old one
		else if (newCard > card) {
			win = (userGuess.toLowerCase().equals("higher")) ? true : false;
		}
		
		System.out.print("The next card was a "+newCard+". ");
		
		//return winning/losing money
		if (win == true) {
			System.out.println("You won! +"+hiLo+" Tokens");
			return hiLo;
		}
		else {
			System.out.println("You lost! -"+hiLo+" Tokens");
			return hiLo*(-1);
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
    
    //create a method to calculate the sum of a hand
    public static int handSum(ArrayList<Integer> hand) {
    	int sum = 0;
    	for (int i=0; i<hand.size(); i++) {
    		sum += hand.get(i);
    	}
    	return sum;
    }
    
    
}