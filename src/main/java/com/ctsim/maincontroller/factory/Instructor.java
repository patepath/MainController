/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ctsim.maincontroller.factory;

import java.net.Socket;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author Patipat Punboonrat
 */
public class Instructor extends PCControlPanel {

	private Map<String, Socket> sessions = new HashMap<>();

	private final JSONObject msgTrain = new JSONObject();
	private final JSONObject msgATC = new JSONObject();
	private final JSONObject msgAcar = new JSONObject();
	private final JSONObject msgC1car = new JSONObject();
	private final JSONObject msgCcar = new JSONObject();
	private final JSONObject msgDriverDesk = new JSONObject();

	public Instructor() {
		name = "INSTRUCTOR";
	}

	public JSONObject process() {
		msgTrain.clear();
		msgAcar.clear();
		msgCcar.clear();
		msgC1car.clear();
		msgDriverDesk.clear();
		msgATC.clear();
		msgOut.clear();

		processMsgIn();

		if (!isCommLoss) {
			String cmd = getCmdFromSocket();

			try {
				JSONObject jsonCmd = (JSONObject) new JSONParser().parse(cmd);
				Iterator keys = jsonCmd.keySet().iterator();
				String key;

				while (keys.hasNext()) {
					key = (String) keys.next();

					switch (key.toLowerCase()) {
						case "acar":
							msgAcar.put("device", (JSONObject) jsonCmd.get(key));
							break;

						case "ccar":
							msgCcar.put("device", (JSONObject) jsonCmd.get(key));
							break;

						case "c1car":
							msgC1car.put("device", (JSONObject) jsonCmd.get(key));
							break;

						case "driverdesk":
							msgDriverDesk.put("device", (JSONObject) jsonCmd.get(key));
							break;

						case "atc":
							msgATC.put("status", (JSONObject) jsonCmd.get(key));
							break;

						case "fault":
							handleFault((JSONObject) jsonCmd.get(key));
							break;
							
						case "video":
							msgOut.put("VIDEO", (JSONObject) jsonCmd.get(key));
							break;

					}
				}
			} catch (ParseException ex) {
			}

		} else {
			setSocket(sessions.get("INSTRUCTOR"));
		}

		if (!msgAcar.isEmpty()) {
			msgTrain.put("ACAR", msgAcar);
		}

		if (!msgCcar.isEmpty()) {
			msgTrain.put("CCAR", msgCcar);
		}

		if (!msgC1car.isEmpty()) {
			msgTrain.put("C1CAR", msgC1car);
		}

		if (!msgDriverDesk.isEmpty()) {
			msgTrain.put("DRIVERDESK", msgDriverDesk);
		}

		if (!msgATC.isEmpty()) {
			msgTrain.put("ATC", msgATC);
		}

		if (!msgTrain.isEmpty()) {
			msgOut.put("TRAIN", msgTrain);
		}

		return msgOut;
	}

	private void processMsgIn() {
		if (!msgIn.isEmpty()) {
			msgToPC.offer(msgIn.toJSONString());

			msgIn.clear();
		}
	}

	private void handleAcar(JSONObject cmd) {

	}

	private void handleCcar(JSONObject cmd) {

	}

	private void handleC1car(JSONObject cmd) {

	}

	private void handleDriverDesk(JSONObject cmd) {

	}

	private void handleFault(JSONObject cmd) {

	}

	private void handleVideo(JSONObject cmd) {

	}

	public void setSessions(Map<String, Socket> sessions) {
		this.sessions = sessions;
	}
}
