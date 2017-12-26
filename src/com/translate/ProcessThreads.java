package com.translate;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import org.json.JSONArray;
import org.json.JSONException;

/**
 * @author Akshay
 * 
 * This class is used to process each input file. Since this class extends the Thread class, there would be 
 * multiple invocations of this class. Each thread processes the output individually and performs a write operation
 * on AsYouGo.txt file. This method{@see Application#writeToAsYouGoTxt()} writes the translated data to AsYouGo.txt as soon
 * as they are translated. The method{@see Application#writeToAsYouGoTxt()} that performs that write operations has been 
 * synchronized  to avoid race a condition. This might sometimes slow down the program because a thread has to wait for
 * the resources to get released but it is essential in a multi-threaded environment to keep resources like these 
 * available for everyone in a safe manner.
 */
public class ProcessThreads extends Thread {

	private String fileName;
	private CountDownLatch latch;

	/**
	 * Default constructor to initialize parameters of the threads.
	 * 
	 * @param filename Name of the current file.
	 * @param latch Used to indicate whether the current threads is s
	 */
	public ProcessThreads(String filename, CountDownLatch latch) {
		this.latch = latch;
		this.fileName = filename;

	}

	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		BufferedReader br;
		BufferedWriter bw;
		String result;
		int lineNumber = 1;
		try {
			br = Files.newBufferedReader(Paths.get(fileName));
			String query = "";

			while ((query = br.readLine()) != null) {

				result = translateString(query);
				//result = Integer.toString(lineNumber) + "hello"; /* This variable is just meant for debugging the code when the API is offline.*/
				if (result.equals("")) {
					Application.setApiOffline(true);
					System.out.println(Thread.currentThread().getName()+" has stopped because API is offline");
					latch.countDown();
					return;
				}
				Object[] o = Application.cleanTheInputs(new String[] { fileName }).toArray();
				String input = Arrays.copyOf(o, o.length, String[].class)[0];

				synchronized (Application.getTemp()) {
					Application.getTemp().put(input + "-" + Integer.toString(lineNumber), result);
				}

				synchronized (Application.getForCounter()) {
					if (Application.getForCounter().keySet().contains(lineNumber))
						Application.getForCounter().put(lineNumber, Application.getForCounter().get(lineNumber) + 1);
					else
						Application.getForCounter().put(lineNumber, 1);
				}

				System.out.println(lineNumber + "-" + result);
				lineNumber++;
				Application.writeToAsYouGoTxt(result);

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		latch.countDown();

	}

	/**
	 * This method returns the translated string.
	 * 
	 * @param word The word to be translated.
	 * @return The translated word.
	 * @throws IOException Exception handled for IO operations.
	 * @throws JSONException Exception handled for JSON parsing exceptions.
	 * @throws InterruptedException This is meant for the Thread.sleep() method.
	 * 
	 * Note: I performed Thread.sleep() to test if the API gets saturated if we send requests after a reasonably long
	 * 		 time interval of 0.5 seconds. Because initially the project would send requests within milliseconds and 
	 * 		 I thought maybe a delay could prevent the traffic overload to the API and hence prevent the API from
	 * 		 sending a 302 http response.
	 */
	private synchronized static String translateString(String word)
			throws IOException, JSONException, InterruptedException {

		final String getString = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=sv&dt=t&q=";

		Connection client = new Connection(getString + word);
		String apiResponse = null;
		try {
			apiResponse = client.execute();
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (apiResponse != "") {
			JSONArray value = new JSONArray(apiResponse);
			String v = (String) value.getJSONArray(0).getJSONArray(0).get(0);
			//Thread.sleep(500);
			return v;
		}

		return "";

	}

}
