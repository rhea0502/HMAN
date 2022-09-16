

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

/**
 * Manages the details of EvilHangman. This class keeps tracks of the possible
 * words from a dictionary during rounds of hangman, based on guesses so far.
 *
 */
public class HangmanManager {
	private Set<String> words;
	private Set<String> wordsUpdated;
	private boolean debugOn;
	private int wordLen;
	private int numGuesses;
	private HangmanDifficulty diff;
	private ArrayList<String> lettersGuessed;
	private String currentPattern;
	private final int MOD_EASY = 2;
	private final int MOD_MED = 4;
	private final String DASH = "-";

	/**
	 * Create a new HangmanManager from the provided set of words and phrases. pre:
	 * words != null, words.size() > 0
	 * 
	 * @param words   A set with the words for this instance of Hangman.
	 * @param debugOn true if we should print out debugging to System.out.
	 */
	public HangmanManager(Set<String> words, boolean debugOn) {
		this.words = words;
		this.debugOn = debugOn;
	}

	/**
	 * Create a new HangmanManager from the provided set of words and phrases.
	 * Debugging is off. pre: words != null, words.size() > 0
	 * 
	 * @param words A set with the words for this instance of Hangman.
	 */
	public HangmanManager(Set<String> words) {
		this.words = words;
		this.debugOn = false;
	}

	/**
	 * Get the number of words in this HangmanManager of the given length. pre: none
	 * 
	 * @param length The given length to check.
	 * @return the number of words in the original Dictionary with the given length
	 */
	public int numWords(int length) {
		int count = 0;
		Iterator<String> it = words.iterator();
		while (it.hasNext()) {
			if (it.next().length() == length) {
				count++;
			}
		}
		return count;
	}

	/**
	 * Get for a new round of Hangman. Think of a round as a complete game of
	 * Hangman.
	 * 
	 * @param wordLen    the length of the word to pick this time. numWords(wordLen)
	 *                   > 0
	 * @param numGuesses the number of wrong guesses before the player loses the
	 *                   round. numGuesses >= 1
	 * @param diff       The difficulty for this round.
	 */
	public void prepForRound(int wordLen, int numGuesses, HangmanDifficulty diff) {
		this.wordLen = wordLen;
		this.numGuesses = numGuesses;
		this.diff = diff;	
		String pattern = "";
		wordsUpdated = new HashSet<>();

		//adds strings from words to wordsUpdated that are the proper length
		for (String i : words) {
			if (i.length() == this.wordLen) {
				wordsUpdated.add(i);
			}
		}
		
		//creates initial pattern string
		for (int i = 0; i < this.wordLen; i++) {
			pattern += DASH;
		}
		this.currentPattern = pattern;
		this.lettersGuessed = new ArrayList<String>();
	}

	/**
	 * The number of words still possible (live) based on the guesses so far.
	 * Guesses will eliminate possible words.
	 * 
	 * @return the number of words that are still possibilities based on the
	 *         original dictionary and the guesses so far.
	 */
	public int numWordsCurrent() {
		return wordsUpdated.size();
	}

	/**
	 * Get the number of wrong guesses the user has left in this round (game) of
	 * Hangman.
	 * 
	 * @return the number of wrong guesses the user has left in this round (game) of
	 *         Hangman.
	 */
	public int getGuessesLeft() {
		return numGuesses;
	}

	/**
	 * Return a String that contains the letters the user has guessed so far during
	 * this round. The characters in the String are in alphabetical order. The
	 * String is in the form [let1, let2, let3, ... letN]. For example [a, c, e, s,
	 * t, z]
	 * 
	 * @return a String that contains the letters the user has guessed so far during
	 *         this round.
	 */
	public String getGuessesMade() {
		Collections.sort(lettersGuessed);
		StringBuilder sb = new StringBuilder();
		String comma = ", ";
		sb.append("[");
		for (int i = 0; i < lettersGuessed.size(); i++) {
			if (i == 0) {
				sb.append(lettersGuessed.get(i));
			} else {
				sb.append(comma + lettersGuessed.get(i));
			}
		}
		sb.append("]");
		return sb.toString();
	}

	/**
	 * Check the status of a character.
	 * 
	 * @param guess The characater to check.
	 * @return true if guess has been used or guessed this round of Hangman, false
	 *         otherwise.
	 */
	public boolean alreadyGuessed(char guess) {
		String guessString = "" + guess;
		if (lettersGuessed.contains(guessString)) {
			System.out.println(lettersGuessed);
			return true;
		}
		return false;
	}

	/**
	 * Get the current pattern. The pattern contains '-''s for unrevealed (or
	 * guessed) characters and the actual character for "correctly guessed"
	 * characters.
	 * 
	 * @return the current pattern.
	 */
	public String getPattern() {
		return currentPattern;
	}

	/**
	 * Update the game status (pattern, wrong guesses, word list), based on the give
	 * guess.
	 * 
	 * @param guess pre: !alreadyGuessed(ch), the current guessed character
	 * @return return a tree map with the resulting patterns and the number of words
	 *         in each of the new patterns. The return value is for testing and
	 *         debugging purposes.
	 */
	public TreeMap<String, Integer> makeGuess(char guess) {
		if (alreadyGuessed(guess)) {
			throw new IllegalArgumentException("This letter has already been guessed."); 
		}
		
		lettersGuessed.add(guess + "");
		TreeMap<String, ArrayList<String>> familyMap = 
				new TreeMap<String, ArrayList<String>>();
		TreeMap<String, Integer> familyCount = new TreeMap<String, Integer>();
		String stringGuess = "" + guess;
		
		//updates maps with new patterns and chooses the current pattern based
		//on difficulty and round number
		patternCreator(stringGuess, familyMap, familyCount);
		currentPattern = mapConverter(familyCount);
		
		if (debugOn) {
			System.out.println("DEBUGGING: New pattern is: " + currentPattern
					+ ". New family has " + familyCount.get(currentPattern) + " words.\n");
		}
		wordsUpdated = new HashSet<>(familyMap.get(currentPattern));
		
		if (!currentPattern.contains(stringGuess)) {
			numGuesses--;
		}
		return familyCount;
	}

	// creates a pattern string for each word in wordsUpdated based on the 
	// player's guess and adds it to the pattern TreeMap
	private void patternCreator(String stringGuess, TreeMap<String, 
			ArrayList<String>> familyMap, TreeMap<String, Integer> familyCount) {
		Iterator<String> it = wordsUpdated.iterator();

		while (it.hasNext()) {
			String updated = "";
			String current = it.next();
			
			for (int i = 0; i < current.length(); i++) {
				String letter = "" + current.charAt(i);
				
				if (lettersGuessed.contains(letter)) {
					updated += current.charAt(i);
				} else if (lettersGuessed.contains(current.charAt(i))) {
					updated += current.charAt(i);
				} else {
					updated += DASH;
				}
			}
			mapAdder(familyMap, familyCount, updated, current);
		}
	}

	//returns the current pattern based on the difficulty
	private String mapConverter(TreeMap<String, Integer> familyCount) {
		Set<String> keyList = familyCount.keySet(); 
		ArrayList<WordFam> wf = new ArrayList<WordFam>();
		
		for (String i : keyList) {
			wf.add(new WordFam(i, familyCount.get(i)));
		}
		Collections.sort(wf);
		//find the index of the pattern we want to use based on difficulty
		int difficulty = difficultyFinder(wf);
		WordFam o = wf.get(difficulty);
		return o.pattern;
	}

	// decides the current pattern based on the difficulty and round number 
	// (which is calculated by the length of lettersGuessed)
	private int difficultyFinder(ArrayList<WordFam> wf) {
		int index = 0;
		String easy = "easiest";
		String med = "medium-difficulty";
		String current = "hardest";
		
		if (diff == HangmanDifficulty.EASY) {
			if (lettersGuessed.size() % MOD_EASY == 0) {
				current = easy;
				if (wf.size() > 1) { //ensures no out of bounds errors
					index = 1;
				}
			}
		} else if (diff == HangmanDifficulty.MEDIUM) {
			if (lettersGuessed.size() % MOD_MED == 0) {
				current = med;
				if (wf.size() > 1) {
					index = 1;
				}
			}
		}

		if (debugOn) {
			if (current.equals(easy) && wf.size() == 1 || current.equals(med) 
					&& wf.size() == 1) {
				System.out.println("\nDEBUGGING: Should pick second hardest pattern "
						+ "this turn, but only one pattern available.");
			}
			System.out.println("\nDEBUGGING: Picking " + current + " list.");
		}
		return index;
	}

	// checks if pattern already exists in map and if it doesn't then it adds the
	// pattern to the map and updates the keys and values accordingly
	private void mapAdder(TreeMap<String, ArrayList<String>> familyMap,
			TreeMap<String, Integer> familyCount, String updated, String current) {

		ArrayList<String> familyArray = new ArrayList<String>();
		
		if (familyMap.containsKey(updated)) {
			familyArray = familyMap.get(updated);
			familyArray.add(current);
			familyMap.put(updated, familyArray);
			int count = familyCount.get(updated);
			count++;
			familyCount.put(updated, count);
		} else {
			familyArray.add(current);
			familyMap.put(updated, familyArray);
			familyCount.put(updated, 1);
		}
	}

	/**
	 * Return the secret word this HangmanManager finally ended up picking for this
	 * round. If there are multiple possible words left one is selected at random.
	 * <br>
	 * pre: numWordsCurrent() > 0
	 * 
	 * @return return the secret word the manager picked.
	 */
	public String getSecretWord() {
		if (numWordsCurrent() <= 0) {
			throw new IllegalArgumentException("The list of active words is size 0."); 
		}
		
		Random r = new Random();
		int word = r.nextInt(wordsUpdated.size());
		Iterator<String> it = wordsUpdated.iterator();
		String secretWord = it.next();
		
		for (int i = 0; i < word; i++) {
			secretWord = it.next();
		}
		return secretWord;
	}

	//a private class that makes comparisons between WordFam objects
	private static class WordFam implements Comparable<WordFam> {
		private String pattern;
		private int numWords;
		private int numDashes;
		private static final String DASH = "-";

		//construct a new WordFam object based on the pattern String and number
		//of words the pattern has
		public WordFam(String pattern, int numWords) {
			this.pattern = pattern;
			this.numWords = numWords;
		}

		//returns the pattern of the WordFam object
		public String getPattern() {
			return this.pattern;
		}

		//returns the number of words that the WordFam object contains
		public int numWords() {
			return numWords;
		}

		//calculates the number of dashes that appear in a given string patter
		public int numDashes() {
			for (int i = 0; i < pattern.length(); i++) {
				if (DASH.equals(pattern.charAt(i))) {
					numDashes++;
				}
			}
			return numDashes;
		}

		//overrides compareTo and makes comparisons between the two WordFam
		//objects based on number of words, number of dashes, and lexiographically
		public int compareTo(WordFam other) {
			int result = other.numWords - numWords;
			if (result == 0) {
				result = other.numDashes - numDashes;
				if (result == 0) {
					result = getPattern().compareTo(other.getPattern());
				}
			}
			return result;
		}
	}
}
