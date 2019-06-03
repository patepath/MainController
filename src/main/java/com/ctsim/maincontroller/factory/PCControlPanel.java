/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ctsim.maincontroller.factory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONObject;

/**
 *
 * @author Patipat Punboonrat
 */
public class PCControlPanel {

	protected Calendar now;
	protected Calendar startWatchDog;
	protected final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

	private Socket socket;
	private BufferedReader in;
	private PrintWriter out;
	protected boolean isCommLoss = true;

	protected String name = "";
	protected final BlockingQueue<String> msgToPC = new LinkedBlockingQueue<>();
	protected final JSONObject msgIn = new JSONObject();
	protected final JSONObject msgOut = new JSONObject();

	public void setSocket(Socket socket) {
		if (socket != null) {
			try {
				if (!socket.isClosed()) {
					this.socket = socket;
					in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
					out = new PrintWriter(socket.getOutputStream(), true);
					isCommLoss = false;
					startWatchDog = Calendar.getInstance();
				}

			} catch (IOException ex) {
				Logger.getLogger(ACarEmuA.class.getName()).log(Level.SEVERE, null, ex);
			}

		} else {
			isCommLoss = true;
		}
	}

	public boolean isSocketSet() {
		return socket != null;
	}

	protected String getCmdFromSocket() {
		String msg = "";

		try {
			if (in.ready()) {
				msg = in.readLine();
				//System.out.println("In : " + name + " - " + msg);
			}
				
			while (!msgToPC.isEmpty()) {
				msg = msgToPC.poll();
				out.println(msg);
				checkError();

				try {
					Thread.sleep(5);
				} catch (InterruptedException ex) {
				}

				//System.out.println("Out : " + name + " - " + msg);
				msg = "";
			}

			if (Calendar.getInstance().getTimeInMillis() - startWatchDog.getTimeInMillis() > 2000) {
				JSONObject cmd = new JSONObject();
				cmd.put("watchdog", 1);
				out.println(cmd.toJSONString());
				checkError();
				startWatchDog = Calendar.getInstance();
			}

		} catch (IOException ex) {
			Logger.getLogger(PCControlPanel.class.getName()).log(Level.SEVERE, null, ex);
		}

		return msg;
	}

	public void putMsg(JSONObject msg) {
		Iterator keys = msg.keySet().iterator();
		String key;
		while (keys.hasNext()) {
			key = (String) keys.next();
			msgIn.put(key, msg.get(key));
		}
	}

	protected boolean checkError() throws IOException {
		if (out.checkError()) {
			in.close();
			out.close();
			socket.close();
			isCommLoss = true;
			socket = null;

			System.out.println(sdf.format(
				Calendar.getInstance().getTime())
				+ " PCControlPanel.checkError() : "
				+ name
				+ " disconnected."
			);

			return true;
		}

		return false;
	}

	public boolean isCommunicatinLoss() {
		return isCommLoss;
	}

}
