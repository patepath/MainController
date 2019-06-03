/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ctsim.maincontroller.factory;

import com.ctsim.maincontroller.interfaces.ControlPanel;
import java.util.Iterator;
import org.json.simple.JSONObject;
import java.util.Calendar;

/**
 *
 * @author Patipat Punboonrat
 */
public class TrainModel2EmuA extends MCSControlPanel implements ControlPanel {

	private final int DOOR_LEFT = 1;
	private final int DOOR_RIGHT = 2;

	private final JSONObject msgAcar = new JSONObject();
	private final JSONObject msgC1car = new JSONObject();
	private final JSONObject msgATC = new JSONObject();

	private int doorStatus = 0;
	private Calendar timeStart;

	@Override
	public void initDevices() {
		name = "TRAINMODEL2";
		initMapping("trainmodel2.xml");
	}

	@Override
	public JSONObject process() {
		msgAcar.clear();
		msgC1car.clear();
		msgATC.clear();
		msgOut.clear();

		processMsgIn();

		if(doorStatus != 0) {

			if (Calendar.getInstance().getTimeInMillis() - timeStart.getTimeInMillis() > 5000) {
				if(doorStatus == 1) {
					// Yellow Lamps	
					msgToControlPanel.offer("E409 S1");
					msgToControlPanel.offer("E412 S1");
					doorStatus = 0;

				} else if(doorStatus == 2) {
					msgToControlPanel.offer("E409 S0");
					msgToControlPanel.offer("E412 S0");
					doorStatus = 0;
				}
			}
		}

		String cmd = getCmdFromSocket("T", "E");

		if (!cmd.equals("")) {
			String id = cmd.split(" ")[0];
			String data = cmd.split(" ")[1].substring(1);
			devs.get(id).replace("status", data);

			switch ((String) devs.get(id).get("type")) {
				case "Valve B09":
					handleValveB09((JSONObject) devs.get(id));
					break;
			}
		}

		if (!msgAcar.isEmpty()) {
			msgOut.put("ACAR", msgAcar);
		}

		if (!msgC1car.isEmpty()) {
			msgOut.put("C1CAR", msgC1car);
		}

		if (!msgATC.isEmpty()) {
			msgOut.put("ATC", msgATC);
		}

		return msgOut;
	}

	private void handleValveB09(JSONObject dev) {
		printDev((String) dev.get("id"));

		JSONObject valve = new JSONObject();
		JSONObject status = new JSONObject();

		status.put("no", Integer.parseInt(((String) dev.get("name")).substring(2)));
		status.put("status", Integer.parseInt((String) dev.get("status")));
		valve.put("valveb09", status);
		
		status.put("car", "c1-car");
		msgAcar.put("status", valve);
		msgC1car.put("status", valve);
	}

	private void processMsgIn() {
		Iterator keys = msgIn.keySet().iterator();
		String key;

		while (keys.hasNext()) {
			key = (String) keys.next();

			switch (key) {

				case "turnon":
					//closeAllDoor();
					msgToControlPanel.offer("E499 S0");
					break;

				case "opendoor":
					openDoor((int) msgIn.get(key));
					break;

				case "closedoor":
					closeDoor((int) msgIn.get(key));
					break;

				case "red_lamp_on" :
					doRedLampOn((int) msgIn.get(key));
					break;

				case "red_lamp_off": 
					doRedLampOff((int) msgIn.get(key));
					break;
			}
		}

		msgIn.clear();
	}

	private void openDoor(int side) {
		if (side == DOOR_LEFT) {
			doorStatus = 1;
			timeStart = Calendar.getInstance();
			msgToControlPanel.offer("E097 S1");
			// Yellow Lamps	
//			msgToControlPanel.offer("E409 S1");
//			msgToControlPanel.offer("E412 S1");

		} else if (side == DOOR_RIGHT) {
			msgToControlPanel.offer("E098 S1");
		}
	}

	private void closeDoor(int side) {
		if (side == DOOR_LEFT) {
			doorStatus = 2;
			timeStart = Calendar.getInstance();
			msgToControlPanel.offer("E097 S0");
				//msgToControlPanel.offer("E099 S0");
				//msgToControlPanel.offer("E408 S1");
			//msgToControlPanel.offer("E409 S0");
			//msgToControlPanel.offer("E412 S0");
				//msgToControlPanel.offer("E416 S0");
		} else if (side == DOOR_RIGHT) {
			msgToControlPanel.offer("E098 S0");
		}
	}

	private void closeAllDoor() {
		msgToControlPanel.offer("E099 S0");
	}

	private void doRedLampOn(int carNo) {
		switch(carNo) {
			case 1:
				msgToControlPanel.offer("E405 S1");
				msgToControlPanel.offer("E407 S1");
				break;

			case 2:
				msgToControlPanel.offer("E409 S1");
				msgToControlPanel.offer("E411 S1");
				break;

			case 3:
				msgToControlPanel.offer("E413 S1");
				msgToControlPanel.offer("E415 S1");
				break;
		}
	}


	private void doRedLampOff(int carNo) {
		switch(carNo) {
			case 1:
				msgToControlPanel.offer("E405 S0");
				msgToControlPanel.offer("E407 S0");
				break;

			case 2:
				msgToControlPanel.offer("E409 S0");
				msgToControlPanel.offer("E411 S0");
				break;

			case 3:
				msgToControlPanel.offer("E413 S0");
				msgToControlPanel.offer("E415 S0");
				break;
		}
	}
}
