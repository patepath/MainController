/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ctsim.maincontroller.factory;

import com.ctsim.maincontroller.interfaces.ControlPanel;
import java.util.Iterator;
import org.json.simple.JSONObject;

/**
 *
 * @author Patipat Punboonrat
 */
public class TrainModel2EmuA extends MCSControlPanel implements ControlPanel {

	private final int DOOR_LEFT = 1;
	private final int DOOR_RIGHT = 2;

	private final JSONObject msgAcar = new JSONObject();
	private final JSONObject msgATC = new JSONObject();

	@Override
	public void initDevices() {
		name = "TRAINMODEL2";
		initMapping("trainmodel2.xml");
	}

	@Override
	public JSONObject process() {
		msgAcar.clear();
		msgATC.clear();
		msgOut.clear();

		processMsgIn();

		String cmd = getCmdFromSocket("T", "E");

		if (msgAcar.isEmpty()) {
			msgOut.put("ACAR", msgAcar);
		}

		if (msgATC.isEmpty()) {
			msgOut.put("ATC", msgATC);
		}

		return msgOut;
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
