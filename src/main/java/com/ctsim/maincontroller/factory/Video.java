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
	private final JSONObject msgDMI = new JSONObject();
	private final JSONObject msgTrain = new JSONObject();

	private final JSONObject statusATC = new JSONObject();
	
	public Video() {
		initDevices();
	}

	public JSONObject process() {
		msgATC.clear();
		msgDMI.clear();
		msgTrain.clear();
		msgOut.clear();

		statusATC.clear();

		processMsgIn();

		if(!isCommLoss) {

			try {
				JSONObject cmd = (JSONObject) new JSONParser().parse(getCmdFromSocket());
				Iterator keys = cmd.keySet().iterator();
				String key;
				double position = 0;
				String type = "";
				int currentSpeed = 0;
				int ceilingSpeed = 0;
				int trainSpeed = 0;
				int targetSpeed = 0;
				int targetDistance = 0;
				int radio = 0;
				int mode = 0;

				System.out.println(cmd.toJSONString());

				while (keys.hasNext()) {
					key = (String) keys.next();

					switch (key.toLowerCase()) {

						case "position":
							sendPositionToATC((double) cmd.get(key));
							break;

						case "train_speed":
							sendTrainSpeedToATC((double) (long) cmd.get(key));
							trainSpeed = (int)(long) cmd.get(key);
							break;

						case "type":
							type = (String) cmd.get(key);
							break;

						case "current_speed":
							currentSpeed = (int) (long) cmd.get(key);
							break;

						case "ceiling_speed":
							ceilingSpeed = (int) (long) cmd.get(key);
							break;

						case "target_speed":
							targetSpeed = (int) (long) cmd.get(key);
							break;

						case "target_distance":
							targetDistance = (int) (long) cmd.get(key);
							break;

						case "radio":
							radio = (int) (long) cmd.get(key);
							break;

						case "mode":
							mode = (int) (long) cmd.get(key);
							break;

					}
				}
				
				switch(type.toLowerCase()) {
					case "start":
						handleStart(mode);
						break;

					case "balise":
						handleBalise(ceilingSpeed, targetSpeed, targetDistance, trainSpeed);
						break;

					case "stop":
						handleStop();
						break;
				}

			} catch (ParseException | NullPointerException ex) {
			}

		} else {
			setSocket(sessions.get("VIDEO"));
		}

		JSONObject trainData = new JSONObject();
		
		if(!statusATC.isEmpty()) {
			System.out.println(statusATC.toJSONString());
			msgATC.put("status", statusATC);
		}
		
		if(!msgATC.isEmpty()) {
			trainData.put("ATC", msgATC);
		}

		if(!msgDMI.isEmpty()) {
			trainData.put("DMI", msgDMI);
		}

		msgOut.put("TRAIN", trainData);

		return msgOut;
	}

	private void sendPositionToATC(double position) {
		//statusATC.put("train_position", position);
	}

	private void sendTrainSpeedToATC(double trainSpeed) {
		//statusATC.put("train_speed", trainSpeed);
	}

//	private void handleCeilingSpeed(int speed) {
//		statusATC.put("ceiling_speed", speed);
//	}

//	private void handleTargetSpeed(int speed) {
//		JSONObject data = new JSONObject();
//		data.put("target_speed", speed);
//		msgDMI.put("status", data);
//	}

	private void handleStart(int mode) {
		System.out.println("START");
		System.out.println("Mode : " + mode);
	}

	private void handleBalise(int ceilingSpeed, int targetSpeed, int targetDistance, int trainSpeed) {
		JSONObject data = new JSONObject();

		data.put("ceiling_speed", ceilingSpeed);
		data.put("target_speed", targetSpeed);
		data.put("target_distance", targetDistance);
		data.put("train_speed", trainSpeed);
		statusATC.put("balise", data); 

		//System.out.println("Balise " + data.toJSONString());
	}

	private void handleStop() {
		statusATC.put("station_stop", true);
	}

	private void processMsgIn() {
		if (!msgIn.isEmpty()) {
			System.out.println("VIDEO : " + msgIn.toJSONString());

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
