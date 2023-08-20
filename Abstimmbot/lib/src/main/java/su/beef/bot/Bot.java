package su.beef.bot;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class Bot {

	private Properties properties;
	private File file;

	private String url;
	private int threads, tasks;

	private String driverFile;

	private ArrayList<String> negativeList, positiveList;
	
	private void loadConfiguration() {
		file = new File("./config.properties");

		url = null;
		threads = 0;

		try {
			properties = new Properties();
			if (!file.exists()) {
				properties.setProperty("url", "https://mejay.io/c/sXx4zB7");
				properties.setProperty("threads", "20");
				properties.setProperty("tasks", "1000");

				try (FileOutputStream outputStream = new FileOutputStream(file)) {
					properties.store(outputStream, "Konfiguration für den Abstimmungsbot");
				}
			}

			properties.load(new FileInputStream(file));

			url = properties.getProperty("url");
			threads = Integer.parseInt(properties.getProperty("threads"));
			tasks = Integer.parseInt(properties.getProperty("tasks"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void copyDriver() {
		driverFile = null;

		try (InputStream inputStream = Bot.class.getResourceAsStream("/chromedriver.exe")) {
			if (inputStream != null) {
				Path temporaryFile = Files.createTempFile("bot", "chromedriver.exe");
				Files.copy(inputStream, temporaryFile, StandardCopyOption.REPLACE_EXISTING);

				driverFile = temporaryFile.toFile().getPath();

				temporaryFile.toFile().deleteOnExit();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private List<WebElement> findTrackElements(WebDriver driver) {
		return driver.findElements(By.className("track-open"));
	}

	public static int calculateLevenshteinDistance(String str1, String str2) {
		int[][] dp = new int[str1.length() + 1][str2.length() + 1];

		for (int i = 0; i <= str1.length(); i++) {
			dp[i][0] = i;
		}

		for (int j = 0; j <= str2.length(); j++) {
			dp[0][j] = j;
		}

		for (int i = 1; i <= str1.length(); i++) {
			for (int j = 1; j <= str2.length(); j++) {
				int substitutionCost = str1.charAt(i - 1) == str2.charAt(j - 1) ? 0 : 1;

				dp[i][j] = Math.min(dp[i - 1][j] + 1, // Deletion
						Math.min(dp[i][j - 1] + 1, // Insertion
								dp[i - 1][j - 1] + substitutionCost)); // Substitution
			}
		}

		return dp[str1.length()][str2.length()];
	}

	public static double calculateStringSimilarity(String str1, String str2) {
		int maxLength = Math.max(str1.length(), str2.length());
		int editDistance = calculateLevenshteinDistance(str1, str2);

		return 1.0 - (double) editDistance / maxLength;
	}

	private void bot(int identifier) throws Exception {
		ChromeOptions options = new ChromeOptions();
		options.addArguments("incognito");
		options.addArguments("headless");

		WebDriver driver = new ChromeDriver(options);

		driver.get(url);

		WebElement closeButton = driver.findElement(By.cssSelector("#cookie-warning .close"));
		closeButton.click();

		List<WebElement> trackElements = findTrackElements(driver);

		for (int i = 0; i < trackElements.size(); i++) {
			WebElement trackElement = trackElements.get(i);
			WebElement titleElement = trackElement.findElement(By.className("title"));

			String songTitle = titleElement.getAttribute("innerText").toLowerCase();
			
			int found = 0;
			
			for(String title : negativeList) {
				if(calculateStringSimilarity(songTitle, title.toLowerCase()) >= 0.5) {
					found = 1;
				}
			}
			
			for(String title : positiveList) {
				if(calculateStringSimilarity(songTitle, title.toLowerCase()) >= 0.5) {
					found = 2;
				}
			}
			
			if(found == 1) {
				WebElement negativeVoteButton = trackElement.findElement(By.className("trigger-vote-negative"));
				negativeVoteButton.click();
				
				System.out.println("Task " + identifier + ": \"" + songTitle + "\" downvoted.");
			} else if(found == 2) {
				WebElement positiveVoteButton = trackElement.findElement(By.className("trigger-vote-positive"));
				positiveVoteButton.click();
				
				System.out.println("Task " + identifier + ": \"" + songTitle + "\" upvoted.");
			}
			
			if(found != 0) {
//				Thread.sleep(1000);
				Thread.sleep(600);
				trackElements = findTrackElements(driver);
			}
		}

		Thread.sleep(1000);

		driver.quit();
	}

	private void loadNegativeList() {
		File file = new File("./negative.txt");

		negativeList = new ArrayList<>();
		
		if (!file.exists()) {
			try {
				FileWriter fileWriter = new FileWriter(file);
				BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
				
				bufferedWriter.write("don't mine at night - minecraft\n");
				bufferedWriter.write("tiki kinderland\n");
				bufferedWriter.write("0099999 WERBE-TRENNER\n");
				
				bufferedWriter.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		try(BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
			for(String line; (line = bufferedReader.readLine()) != null;) {
				if(line != null) {
					if(!line.isEmpty() && !line.isBlank()) {
						negativeList.add(line);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void loadPositiveList() {
		File file = new File("./positive.txt");

		positiveList = new ArrayList<>();
		
		if (!file.exists()) {
			try {
				FileWriter fileWriter = new FileWriter(file);
				BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
				
				bufferedWriter.write("Basket Case\n");
				bufferedWriter.write("RADW\n");
				
				bufferedWriter.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		try(BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
			for(String line; (line = bufferedReader.readLine()) != null;) {
				if(line != null) {
					if(!line.isEmpty() && !line.isBlank()) {
						positiveList.add(line);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Bot() {
		loadConfiguration();
		copyDriver();

		loadNegativeList();
		loadPositiveList();
		
		System.setProperty("webdriver.chrome.driver", driverFile);

		ExecutorService pool = Executors.newFixedThreadPool(threads);
		for(int i = 0; i < tasks; i++) {
			int taskIdentifier = i;
			pool.submit(() -> {
				try {
					bot(taskIdentifier);
				} catch(Exception e) {
					e.printStackTrace();
				}
			});
		}
		
//		try {
//			Thread.sleep(2000);
//			bot(0);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
	}

	public static void main(String[] args) {
		new Bot();
	}

}
