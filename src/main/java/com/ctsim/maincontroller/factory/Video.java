/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ctsim.maincontroller.factory;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 *
 * @author Patipat Punboonrat
 */
public final class Video extends PCControlPanel {

	private Map<String, Socket> sessions = new HashMap<>();
	private final JSONObject msgATC = new JSONObject();
	private final JSONObject msgTrain = new JSONObject();

	public Video() {
		initDevices();
	}

	public JSONObject process() {
		msgATC.clear();
		msgTrain.clear();
		msgOut.clear();

		processMsgIn();

		if(!isCommLoss) {

			try {
				JSONObject cmd = (JSONObject) new JSONParser().parse(getCmdFromSocket());
				Iterator keys = cmd.keySet().iterator();
				String key;

				while (keys.hasNext()) {
					key = (String) keys.next();

					switch (key.toLowerCase()) {
						case "position":
							handlePosition((double) cmd.get(key));
							break;

						case "type":
							break;

						case "speed":
							handleSpeed((int) (long) cmd.get(key));
							break;
					}
				}

			} catch (ParseException | NullPointerException ex) {
			}

		} else {
			setSocket(sessions.get("VIDEO"));
		}

		if(!msgATC.isEmpty()) {
			msgTrain.put("ATC", msgATC);
			msgOut.put("TRAIN", msgTrain);
		}

		return msgOut;
	}

	private void handlePosition(double position) {

	}

	private void handleSpeed(int speed) {
		JSONObject data = new JSONObject();
		data.put("ceiling_speed", speed);
		msgATC.put("status", data);
	}

	private void processMsgIn() {
		if (!msgIn.isEmpty()) {
			String msg = msgIn.toJSONString();
			msgToPC.offer(msg);
			msgIn.clear();
		}
	}

	public void initDevices() {
		name = "VIDEO";
	}

	public void setSessions(Map<String, Socket> sessions) {
		this.sessions = sessions;
	}
}
