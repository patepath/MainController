/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ctsim.maincontroller.factory;

import com.ctsim.maincontroller.interfaces.ControlPanel;
import java.util.Calendar;
import java.util.Iterator;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author Patipat Punboonrat
 */
public class LeverEmuA extends PCControlPanel implements ControlPanel {

	private final JSONObject msgAcar = new JSONObject();
	private final JSONObject msgDriverDesk = new JSONObject();
	private final JSONObject msgDMI = new JSONObject();
	private final JSONObject msgTRAINMODEL1 = new JSONObject();
	private final JSONObject msgDummyBogie = new JSONObject();
	private final JSONObject msgVideo = new JSONObject();
	private final JSONObject msgATC = new JSONObject();

	private final int TRAIN_WEIGHT = 32000;
	private final int MAX_FORCE = 200000;
	private final double EB_FORCE = MAX_FORCE * 0.1;

	private double leverValue;
	private boolean isLeverPressed = false;
	private String leverPosition = "";
	private String cmdBuzzer = "";
	private boolean isEB = false;
    private boolean isEnable = false;

	private double motorForce;
	private double trainSpeed, oldTrainSpeed = -1;
	private long t1, t2;
	private Calendar ebTime;

	public LeverEmuA() {
		t1 = Calendar.getInstance().getTimeInMillis();
	}

	@Override
	public void initDevices() {
		name = "LEVER";
	}

	@Override
	public JSONObject process() {
        clearMsgs();
		processMsgIn();
        processSocketMessage();
        chkMsg();
		return msgOut;
	}

    private void processSocketMessage() {
		JSONObject cmd = new JSONObject();

		try {
			cmd = (JSONObject) new JSONParser().parse(getCmdFromSocket());

			if(isEnable) {
				Iterator keys = cmd.keySet().iterator();
				String key;

				while (keys.hasNext()) {
					key = (String) keys.next();

					switch (key) {
						case "lever_value":
							handleLeverValue((double) cmd.get(key));
							break;

						case "lever_press":
							handleLeverPressed((boolean) cmd.get(key));
							break;

						case "lever_position":
							handleLeverPosition((String) cmd.get(key));
							break;
					}
				}

				// Send lever information to ATC
				if (!cmd.isEmpty()) {
					msgATC.put("status", cmd);
				}
				
			}
		} catch (ParseException | NullPointerException ex) {
		}
		
		// Check Dead-man Release over 3 secs
		if(cmdBuzzer.equals("ON")) {
			cmd = new JSONObject();
			
			if(!isEB & Calendar.getInstance().getTimeInMillis() - ebTime.getTimeInMillis() > 3000) {
				isEB = true;
				msgATC.put("cmd", "emergency_brake");
			}

		} else {
			ebTime = Calendar.getInstance();
		}
    }

    private void clearMsgs() {
        msgAcar.clear();
        msgDriverDesk.clear();
        msgDMI.clear();
        msgTRAINMODEL1.clear();
        msgDummyBogie.clear();
        msgVideo.clear();
        msgATC.clear();
        msgOut.clear();
    }

    private void chkMsg(){
		if(msgOut != null) {
			if (!msgATC.isEmpty()) {
				msgOut.put("ATC", msgATC);
			}

			if (!msgAcar.isEmpty()) {
				msgOut.put("ACAR", msgAcar);
			}

			if (!msgDriverDesk.isEmpty()) {
				msgOut.put("DRIVERDESK", msgDriverDesk);
			}

			if (!msgDMI.isEmpty()) {
				msgOut.put("DMI", msgDMI);
			}

			if (!msgTRAINMODEL1.isEmpty()) {
				msgOut.put("TRAINMODEL1", msgTRAINMODEL1);
			}

			if (!msgDummyBogie.isEmpty()) {
				msgOut.put("DUMMYBOGIE", msgDummyBogie);
			}

			if (!msgVideo.isEmpty()) {
				msgOut.put("VIDEO", msgVideo);
			}
		}
    }

	private void handleLeverValue(double value) {
		leverValue = value;
	}

	private void handleLeverPressed(boolean isPressed) {
		isLeverPressed = isPressed;
//		JSONObject status = new JSONObject();
//
//		if (trainSpeed > 0 & !isLeverPressed) {
//			if (!cmdBuzzer.equals("ON")) {
//				cmdBuzzer = "ON";
//				status.put("deadman_released", true);
//				msgAcar.put("status", status);			// tell A-Car deadman is released.
//			}
//
//		} else {
//			if(!cmdBuzzer.equals("OFF")) {
//				cmdBuzzer = "OFF";
//				status.put("deadman_released", false);
//				msgAcar.put("status", status);			// tell A-Car deadman is released.
//			}
//		}
	}

	private void handleLeverPosition(String position) {
		leverPosition = position;
		JSONObject status = new JSONObject();

		switch (leverPosition) {
			case "D":
				status.put("lever_position", 1);
				msgAcar.put("status", status);
				break;

			case "0":
				status.put("lever_position", 2);
				msgAcar.put("status", status);
				break;
				
			case "B":
				status.put("lever_position", 3);
				msgAcar.put("status", status);
				break;

			case "EB":
				isEB = true;
				status.put("emergencybrake_active", true);
				msgATC.put("status", status);
				status.clear();
				status.put("ATP_BRAKE", 1);
				msgDMI.put("status", status);		// show ATP Brake icon at DMI.
				break;

			default:
				break;
		}
	}

	private void processMsgIn() {
		Iterator keys = msgIn.keySet().iterator();
		String key;

		while (keys.hasNext()) {
			key = (String) keys.next();

			switch (key) {
				case "startup":
					isEnable = true;
					isEB = false;
					break;

				case "speed":
					trainSpeed = (double) msgIn.get(key);
					break;

				case "emergency_brake":
					isEB = (boolean) msgIn.get(key);
					break;

				case "clear_emergency_brake":
					isEB = false;
					break;

                case "enable":
                    isEnable = (boolean) msgIn.get(key);
                    break;
			}
		}
	}

}
