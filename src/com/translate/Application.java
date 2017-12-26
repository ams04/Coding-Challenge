package com.translate;
/**
 * Note: Please put the input files in the root folder of the project.
 * 		  
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * @author Akshay
 * 
 * I have used multi-threading to solve this problem. As soon as the input is entered through command line(input here
 * represents the array of file names), an array of threads are executed. The number of threads in the thread pool 
 * depends upon the number of files that have been entered. Each thread then performs the translation action and uses the common
 * resources in turns (since the common resources have been synchronized) to write data to both Batched.txt and AsYouGo.txt. 
 * 
 * Since we are using the translation API provided by Google, there are certain limitations to the number of requests
 * that could be sent. A possible problem that may arrive is if the program is in the middle of processing and the API 
 * goes offline. Now we are stuck with some data that is processed and some data that is not processed/translated. A 
 * fallback mechanism in this situation would be to at least write the data that has already been processed to Batched.txt
 * (Since the method that writes data to AsYouGo.txt is called immediately after each word is processed, we don't have to worry about that).
 * Hence I decided to spin a new thread to handle this writing of the translated words to Batched.txt. This thread would 
 * constantly check my if any batch has been processed and if it is, then the data is written to the Batched.txt. 
 * This would make sure that whenever the API goes offline in the middle of processing, the data
 * that has already been processed, at least that is written to Batched.txt. But, I realized it made the solution too 
 * complicated so I just use the CountDownLatch class to indicate if the threads are still running. And whenever
 * a thread encounters that the API is offline, the latch counter is decremented, indicating that this thread has now
 * terminated. Now, since the API is offline, all the threads in the thread pool would decrement the latch counter and
 * hence all the threads would get terminated. Once the threads are terminated forcefully like this, the BatchedIncomplete()
 * method is called. This method will write whatever data that has been processed into the Batched.txt in a manner asked in
 * the problem.
 * 
 * I tried to do a Thread.sleep(500), so that the API does not detect a spam of requests from my network. But, that didn't work
 * and the API still showed at 302 after a certain number of requests.
 * 
 *
 */
public class Application extends Thread {

	/**
	 * Fields for writing data to AsYouGo.txt/
	 */
	private static File fileAsYouGo;
	private static BufferedWriter bwAsYouGo;
	private static FileWriter fwAsYouGo;

	/**
	 * Fields for writing data to Batched.txt/
	 */
	private static File fileBatched;
	private static BufferedWriter bwBatched;
	private static FileWriter fwBatched;

	/**
	 * Maps for processing data and organizing them in Batched.txt.
	 */
	private static Map<String, String> temp;
	private static Map<Integer, Integer> forCounter = new HashMap<>();

	private static String[] input;

	/**
	 * Boolean variable to indicate if the API we are working with is offline or online.
	 */
	private static boolean isApiOffline = false;

	/**
	 * Default constructor for initializing all the elements needed to perform the task.
	 * 
	 * @param input This represents the array of file names that are passed through command line.
	 * @throws IOException Exception thrown for handling IO operations.
	 */
	Application(String[] input) throws IOException {

		fileAsYouGo = new File("./AsYouGo.txt");
		fwAsYouGo = new FileWriter(fileAsYouGo.getAbsoluteFile(), true);
		bwAsYouGo = new BufferedWriter(fwAsYouGo);

		fileBatched = new File("./Batched.txt");
		fwBatched = new FileWriter(fileBatched.getAbsoluteFile(), true);
		bwBatched = new BufferedWriter(fwBatched);

		temp = new HashMap<>();

		this.input = input;

	}

	/** 
	 * Execution starts in this method. Threads spin off from this method and perform their
	 * individual tasks.
	 * 
	 * @param args Name of the files that are passed through command line.
	 * @throws IOException Exception handled for IO operations.
	 */
	public static void main(String[] args) throws IOException {

		new Application(args);

		CountDownLatch latch = new CountDownLatch(args.length);

		for (String s : args) {

			new ProcessThreads(s, latch).start();

		}

		Object[] o = cleanTheInputs(args).toArray();
		input = Arrays.copyOf(o, o.length, String[].class);

		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (getApiOffline()) {
			System.out.println("Translate API is offline; Writing the translated words to Batched.txt in batches");
			BatchedIncomplete();
		}
		else
			BatchedTxt();
		bwBatched.close();
		bwAsYouGo.close();

	}

	/** 
	 * Separate methods have been written for writing data to Batched.txt; that is when the API is offline 
	 * and when the API is online. This one represents the method which is called when the threads
	 * encounters that the API is offline. This method is invoked only after all the threads have 
	 * been terminated.
	 * 
	 * NOTE: This method resembles the BatchedTxt() method which performs a similar operation as
	 * 		 this method. Separate methods have been written to logically differentiate and 
	 * 		 indicate as to what is happening in the code flow. 
	 *  
	 * 
	 */
	private static void BatchedIncomplete() {

		//System.out.println("cool" + temp.entrySet());
		System.out.println("Status of a line number for all files" + forCounter.entrySet());

		int counter = 1;

		while (true) {
			if (forCounter.size() != 0) {

				List<String> data = new ArrayList<>();

				for (String s : input) {
					String generatedKey = s + "-" + Integer.toString(counter);
					String dataToAdd = temp.get(generatedKey);
					if (dataToAdd != null) {
						data.add(dataToAdd);
						System.out.println(temp.get(generatedKey));
					} else
						continue;
				}
				Collections.sort(data);

				for (String s : data) {
					try {
						bwBatched.write(s);
						bwBatched.newLine();
					} catch (IOException e) {
						System.out.println("I am here");
						e.printStackTrace();
					}
				}
				forCounter.remove(counter);
				counter++;

			}

			if (forCounter.size() == 0)
				break;

		}
	}

	/**
	 * This method is called by each thread that exist in the project. It has been synchronized to avoid 
	 * race conditions.
	 *  
	 * @param result This represents a single translated word from a file which has to be written as soon as it is 
	 * 				 processed.
	 * @throws IOException Exception thrown for handling IO operations.
	 */
	public static synchronized void writeToAsYouGoTxt(String result) throws IOException {

		bwAsYouGo.write(result);
		bwAsYouGo.newLine();

	}

	/**
	 * After all threads have finished processing, this method is called to write data to Batched.txt. This method
	 * sorts the data in each batch and then writes it to the Batched.txt.
	 */
	public static void BatchedTxt() {
		System.out.println(forCounter);
		int counter = 1;

		while (true) {
			if (forCounter.size() != 0) {

				if (forCounter.get(counter) != input.length)
					continue;
				else {

					List<String> data = new ArrayList<>();

					for (String s : input) {
						String generatedKey = s + "-" + Integer.toString(counter);
						data.add(temp.get(generatedKey));
						System.out.println(temp.get(generatedKey));
					}
					Collections.sort(data);

					for (String s : data) {
						try {
							bwBatched.write(s);
							bwBatched.newLine();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							System.out.println("I am gere");
							e.printStackTrace();
						}
					}
					forCounter.remove(counter);
					counter++;

				}
			}

			if (forCounter.size() == 0)
				break;
		}

	}

	/**
	 * This method strips the path from the input file names.
	 * 
	 * @param data This array represents the input file names.
	 * 
	 * @return return the name of the files without their paths.
	 */
	public static List<String> cleanTheInputs(String[] data) {

		List<String> list = new ArrayList<>();
		for (String s : data) {
			String[] str = s.split("\\.");
			String temp1 = str[1].substring(1, str[1].length());
			list.add(temp1);
		}
		return list;
	}

	/**
	 * This map stores data that is used for processing and organizing the output. 
	 */
	public static Map<String, String> getTemp() {
		return temp;
	}

	/**
	 * This map stores data that is used for processing and organizing the output.
	 */
	public static Map<Integer, Integer> getForCounter() {
		return forCounter;
	}

	/**
	 * @param isApiOfflineNew sets the status of the API availabilty.
	 */
	public static void setApiOffline(boolean isApiOfflineNew) {
		isApiOffline = isApiOfflineNew;
	}

	/**
	 * @return indicates whether the API is offline or online.
	 */
	public static boolean getApiOffline() {
		return isApiOffline;
	}

}
