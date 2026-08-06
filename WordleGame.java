import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.Set;

public class WordleGame {
    private final Scanner scanner;
    private final Path wordListPath;
    private final Path statsPath;
    private String username;

    public WordleGame() {
        scanner = new Scanner(System.in);
        wordListPath = Path.of("wordle-words.txt");
        statsPath = Path.of("stats.csv");
    }

    public static void main(String[] args) {
        WordleGame game = new WordleGame();
        game.run(args);
    }

    public void run(String[] args) {
        if (args.length == 0) {
            System.out.println("Please provide a number as command line argument");
            return;
        }
        if (args.length > 1) {
            System.out.println("Invalid command-line argument. Please launch with a valid number.");
            return;
        }

        int requestedIndex;
        try {
            requestedIndex = Integer.parseInt(args[0]);
        } catch (NumberFormatException exception) {
            System.out.println("Invalid command-line argument. Please launch with a valid number.");
            return;
        }

        List<String> words = readWords();
        if (words.isEmpty()) {
            System.out.println("Word list file is missing or empty.");
            return;
        }

        if (requestedIndex < 0 || requestedIndex >= words.size()) {
            System.out.println("Press Enter to exit...");
            if (scanner.hasNextLine()) {
                scanner.nextLine();
            }
            return;
        }

        System.out.print("Enter your username: ");
        if (!scanner.hasNextLine()) {
            return;
        }
        username = scanner.nextLine().trim();
        if (username.isEmpty()) {
            username = "Player";
        }

        playGame(username, words.get(requestedIndex), words);
    }

    private List<String> readWords() {
        List<String> words = new ArrayList<String>();
        try {
            for (String line : Files.readAllLines(wordListPath, StandardCharsets.UTF_8)) {
                String word = line.trim().toLowerCase(Locale.ROOT);
                if (word.matches("[a-z]{5}")) {
                    words.add(word);
                }
            }
        } catch (IOException exception) {
            return words;
        }
        return words;
    }

    private void playGame(String username, String secretWord, List<String> words) {
        Set<Character> remainingLetters = createAlphabet();
        int attemptsUsed = 0;
        boolean won = false;

        System.out.println("Welcome to Wordle! Guess the 5-letter word.");
        while (attemptsUsed < 6) {
            System.out.print("Enter your guess: ");
            if (!scanner.hasNextLine()) {
                return;
            }

            String guess = scanner.nextLine();
            if (!guess.matches("[a-z]+")) {
                System.out.println(" Your guess must only contain lowercase letters.");
                continue;
            }
            if (guess.length() != 5) {
                System.out.println(" Your guess must be exactly 5 letters long.");
                continue;
            }
            if (!words.contains(guess)) {
                System.out.println(" Word not in list. Please enter a valid word.");
                continue;
            }

            attemptsUsed++;
            if (guess.equals(secretWord)) {
                won = true;
                System.out.println(" Congratulations! You've guessed the word correctly.");
                break;
            }

            String feedback = feedbackFor(guess, secretWord, remainingLetters);
            System.out.println(" Feedback: " + feedback);
            System.out.println("Remaining letters: " + lettersToString(remainingLetters));
            System.out.println("Attempts remaining: " + (6 - attemptsUsed));
        }

        if (!won) {
            System.out.println("Game over. The correct word was: " + secretWord);
        }

        writeStats(username, secretWord, attemptsUsed, won);
        askToShowStats();
    }

    private Set<Character> createAlphabet() {
        Set<Character> letters = new LinkedHashSet<Character>();
        for (char letter = 'A'; letter <= 'Z'; letter++) {
            letters.add(Character.valueOf(letter));
        }
        return letters;
    }

    private String feedbackFor(String guess, String secretWord, Set<Character> remainingLetters) {
        String[] colours = new String[5];
        int[] secretLetterCounts = new int[26];
        char[] uppercaseGuess = guess.toUpperCase(Locale.ROOT).toCharArray();

        for (int index = 0; index < secretWord.length(); index++) {
            secretLetterCounts[secretWord.charAt(index) - 'a']++;
        }

        for (int index = 0; index < guess.length(); index++) {
            if (guess.charAt(index) == secretWord.charAt(index)) {
                colours[index] = "\u001B[32m";
                secretLetterCounts[guess.charAt(index) - 'a']--;
            }
        }

        for (int index = 0; index < guess.length(); index++) {
            if (colours[index] == null) {
                int letterIndex = guess.charAt(index) - 'a';
                if (secretLetterCounts[letterIndex] > 0) {
                    colours[index] = "\u001B[33m";
                    secretLetterCounts[letterIndex]--;
                } else {
                    colours[index] = "\u001B[37m";
                    remainingLetters.remove(Character.valueOf(uppercaseGuess[index]));
                }
            }
        }

        StringBuilder result = new StringBuilder();
        for (int index = 0; index < uppercaseGuess.length; index++) {
            result.append(colours[index]).append(uppercaseGuess[index]).append("\u001B[0m");
        }
        return result.toString();
    }

    private String lettersToString(Set<Character> letters) {
        StringBuilder result = new StringBuilder();
        for (Character letter : letters) {
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(letter.charValue());
        }
        return result.toString();
    }

    private void writeStats(String username, String secretWord, int attemptsUsed, boolean won) {
        String line = csvValue(username) + "," + csvValue(secretWord) + "," + attemptsUsed + ","
                + (won ? "win" : "loss");
        try (BufferedWriter writer = Files.newBufferedWriter(statsPath, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            writer.write(line);
            writer.newLine();
        } catch (IOException exception) {
            System.out.println("Could not save stats.");
        }
    }

    private String csvValue(String value) {
        return value.replace(",", " ").replace("\n", " ").replace("\r", " ");
    }

    private void askToShowStats() {
        System.out.print("Do you want to see your stats? (yes/no): ");
        if (!scanner.hasNextLine()) {
            return;
        }
        String answer = scanner.nextLine().trim().toLowerCase(Locale.ROOT);
        if (answer.equals("yes") || answer.equals("y")) {
            showStats();
        }
    }

    private void showStats() {
        int gamesPlayed = 0;
        int gamesWon = 0;
        int attempts = 0;
        try {
            for (String line : Files.readAllLines(statsPath, StandardCharsets.UTF_8)) {
                String[] values = line.split(",", -1);
                if (values.length == 4 && values[0].equals(username)) {
                    gamesPlayed++;
                    if (values[3].equals("win")) {
                        gamesWon++;
                    }
                    attempts += Integer.parseInt(values[2]);
                }
            }
        } catch (IOException exception) {
            System.out.println("No stats available.");
            return;
        } catch (NumberFormatException exception) {
            System.out.println("No stats available.");
            return;
        }
        System.out.println("Stats for " + username + ":");
        System.out.println("Games played: " + gamesPlayed);
        System.out.println("Games won: " + gamesWon);
        double averageAttempts = gamesPlayed == 0 ? 0.0 : (double) attempts / gamesPlayed;
        System.out.println("Average attempts per game: " + averageAttempts);
    }

}
