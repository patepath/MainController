/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ctsim.maincontroller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Patipat Punboonrat
 */
public class Communication implements Runnable {

	private Calendar now;
	private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

	private final Map<String, Socket> sessions;
	private final ServerSocket servSocket;

	private Socket socket;
	private BufferedReader in;
	private PrintWriter out;

	/**
	 *
	 * @param sessions
	 * @throws IOException
	 */
	public Communication(Map<String, Socket> sessions) throws IOException {
		this.servSocket = new ServerSocket(2510);
		this.sessions = sessions;
	}

	@Override
	public void run() {
		now = Calendar.getInstance();
		System.out.println(sdf.format(now.getTime()) + " Communication.run() : Communication started.");

		String msg;
		String sessionId;

		while (true) {
			try {
				socket = servSocket.accept();
				in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
				out = new PrintWriter(socket.getOutputStream(), true);

				sessionId = "";

				while (sessionId.equals("")) {
					msg = in.readLine().toUpperCase();
					out.println(msg);
					out.flush();

					if (msg.startsWith("SESSIONID")) {
						sessionId = msg.split("=")[1];
						sessions.put(sessionId, socket);

						now = Calendar.getInstance();
						System.out.println(sdf.format(now.getTime()) + " Communication.run() : " + sessionId + " connected.");
					}
				}

			} catch (IOException ex) {
				Logger.getLogger(Communication.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}
}
