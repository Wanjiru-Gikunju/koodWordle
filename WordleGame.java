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
        List<String> words = readWords();
        if (words.isEmpty()) {
            System.out.println("Word list file is missing or empty.");
            return;
        }

        int wordIndex = getWordIndex(args, words.size());
        if (wordIndex < 0) {
            System.out.println("Invalid word index.");
            return;
        }

        System.out.print("Enter your username: ");
        if (!scanner.hasNextLine()) {
            return;
        }
        String username = scanner.nextLine().trim();
        if (username.isEmpty()) {
            username = "Player";
        }

        playGame(username, words.get(wordIndex));
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

    private int getWordIndex(String[] args, int wordCount) {
        if (args.length != 1) {
            return -1;
        }
        try {
            int index = Integer.parseInt(args[0]);
            if (index >= 0 && index < wordCount) {
                return index;
            }
        } catch (NumberFormatException exception) {
            return -1;
        }
        return -1;
    }

    private void playGame(String username, String secretWord) {
        Set<Character> remainingLetters = createAlphabet();
        int attemptsUsed = 0;
        boolean won = false;

        System.out.println("Welcome to Wordle!");
        while (attemptsUsed < 6) {
            System.out.print("Enter a 5-letter word: ");
            if (!scanner.hasNextLine()) {
                return;
            }

            String guess = scanner.nextLine().trim().toLowerCase(Locale.ROOT);
            if (!guess.matches("[a-z]{5}")) {
                System.out.println("Please enter a valid 5-letter word.");
                continue;
            }

            attemptsUsed++;
            String feedback = feedbackFor(guess, secretWord, remainingLetters);
            System.out.println(feedback);
            System.out.println("Remaining attempts: " + (6 - attemptsUsed));
            System.out.println("Remaining letters: " + lettersToString(remainingLetters));

            if (guess.equals(secretWord)) {
                won = true;
                System.out.println("Congratulations! You guessed the word.");
                break;
            }
        }

        if (!won) {
            System.out.println("You ran out of attempts. The word was: " + secretWord.toUpperCase(Locale.ROOT));
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
        System.out.print("View stats? (yes/no): ");
        if (!scanner.hasNextLine()) {
            return;
        }
        String answer = scanner.nextLine().trim().toLowerCase(Locale.ROOT);
        if (answer.equals("yes") || answer.equals("y")) {
            showStats();
        }
    }

    private void showStats() {
        try {
            for (String line : Files.readAllLines(statsPath, StandardCharsets.UTF_8)) {
                System.out.println(line);
            }
        } catch (IOException exception) {
            System.out.println("No stats available.");
        }
    }
}
