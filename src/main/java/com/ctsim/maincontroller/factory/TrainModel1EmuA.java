/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ctsim.maincontroller.factory;

import org.json.simple.JSONObject;
import com.ctsim.maincontroller.interfaces.ControlPanel;
import java.util.Iterator;
import java.text.DecimalFormat;

/**
 *
 * @author instructor
 */
public class TrainModel1EmuA extends MCSControlPanel implements ControlPanel {

	private final int DOOR_LEFT = 1;
	private final int DOOR_RIGHT = 2;

	private final JSONObject msgAcar = new JSONObject();
	private final JSONObject msgATC = new JSONObject();

	@Override
	public void initDevices() {
		name = "TRAINMODEL1";
		initMapping("trainmodel1.xml");

	}

	@Override
	public JSONObject process() {
		msgAcar.clear();
		msgOut.clear();

		processMsgIn();

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

		return msgOut;
	}

	private void handleValveB09(JSONObject dev) {
		printDev((String) dev.get("id"));

		JSONObject status = new JSONObject();

		if (((String) dev.get("name")).startsWith("1")) {
			status.put("car", "a-car");

		} else if (((String) dev.get("name")).startsWith("2")) {
			status.put("car", "c-car");

		} else {
			status.put("car", "a-car_unmaned");
		}

		status.put("no", Integer.parseInt(((String) dev.get("name")).substring(2)));
		status.put("status", Integer.parseInt((String) dev.get("status")));

		JSONObject valve = new JSONObject();
		valve.put("valveb09", status);
		msgAcar.put("status", valve);
	}

	private void processMsgIn() {
		Iterator keys = msgIn.keySet().iterator();
		String key;

		while (keys.hasNext()) {
			key = (String) keys.next();

			switch (key) {

				case "turnon":
					closeDoor(1);
					closeDoor(2);
					break;
					
				case "SPEED":
					doSpeed((double) msgIn.get(key));
					break;

				case "opendoor":
					openDoor((int) msgIn.get(key));
					break;

				case "closedoor":
					closeDoor((int) msgIn.get(key));
					break;
			}
		}

		msgIn.clear();
	}

	private void doSpeed(double speed) {
		DecimalFormat df = new DecimalFormat("00");
		msgToControlPanel.offer("E602 S" + df.format(speed));
	}

	private void openDoor(int side) {
		if (side == DOOR_LEFT) {
			msgToControlPanel.offer("E097 S1");
		} else if (side == DOOR_RIGHT) {
			msgToControlPanel.offer("E098 S1");
		}
	}

	private void closeDoor(int side) {
		if (side == DOOR_LEFT) {
			msgToControlPanel.offer("E097 S0");
		} else if (side == DOOR_RIGHT) {
			msgToControlPanel.offer("E098 S0");
		}
	}
}
