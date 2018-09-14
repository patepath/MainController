/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ctsim.maincontroller.factory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.json.simple.JSONObject;

/**
 *
 * @author instructor
 */
public class TrainModel {

	protected Calendar now;
	protected Calendar startWatchDog;
	protected final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

	private Socket socket;
	private BufferedReader in;
	private PrintWriter out;
	protected boolean isCommLoss = true;

	protected String name = "";

	protected Map<String, JSONObject> devs = new HashMap<>();
	protected Map<String, String> getNewByOld = new HashMap<>();

	protected final JSONObject parentMsgIn = new JSONObject();

	protected final JSONObject parentMsgOut = new JSONObject();
	protected final Collection<String> atcMsgOut = new LinkedList<>();
	protected final Collection<String> failMsgOut = new LinkedList<>();

	protected final BlockingQueue<String> childMsgOut = new LinkedBlockingQueue<>();

	protected boolean checkError() throws IOException {
		if (out.checkError()) {
			in.close();
			out.close();
			socket.close();
			isCommLoss = true;
			return true;
		}

		return false;
	}

	protected String getCmdFromSocket(String quationChar, String prefixChar) {
		String childMsgIn;
		String id;

		try {
			if (socket.getInputStream().available() > 0) {
				startWatchDog = Calendar.getInstance();

				childMsgIn = in.readLine().toUpperCase();
				out.println(childMsgIn);
				checkError();

				if (childMsgIn.startsWith(quationChar)) {
					while (!childMsgOut.isEmpty()) {
						out.println(childMsgOut.poll());
						checkError();
					}

				} else if (childMsgIn.startsWith(prefixChar) & childMsgIn.length() == 7) {
					id = getNewByOld.get(childMsgIn.split(" ")[0]);

					if (id != null) {
						return id + " " + childMsgIn.split(" ")[1];
					}

					now = Calendar.getInstance();
					System.out.println(sdf.format(now.getTime()) + " Message \"" + childMsgIn + "\" error!");
				}
			}

			if ((Calendar.getInstance().getTimeInMillis() - startWatchDog.getTimeInMillis()) > 4000) {
				in.close();
				out.close();
				socket.close();
				isCommLoss = true;

				now = Calendar.getInstance();
				System.out.println(sdf.format(now.getTime()) + " ControlPanel.getCmdFromSocket() : " + name + " disconnected.");
			}

		} catch (IOException | NullPointerException ex) {
		}

		return "";
	}
}
