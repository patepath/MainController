/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ctsim.maincontroller.factory;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import java.util.Iterator;
import com.ctsim.maincontroller.interfaces.ControlPanel;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.parser.ParseException;

/**
 *
 * @author instructor
 */
public class ZoomDoorEmuA extends PCControlPanel implements ControlPanel {

	private final JSONObject msgDriverDesk = new JSONObject();
	private final JSONObject msgATC = new JSONObject();

	@Override
	public void initDevices() {
		name = "ZOOMDOOR";
	}

	@Override
	public JSONObject process() {
		msgATC.clear();
		msgDriverDesk.clear();
		msgOut.clear();

		processMsgIn();

		try {
			String cmd = getCmdFromSocket();

			if (!cmd.equals("")) {

				JSONObject json = (JSONObject) new JSONParser().parse(cmd);
				Iterator keys = json.keySet().iterator();
				String key;

				while (keys.hasNext()) {
					key = (String) keys.next();

					switch (key) {
						case "door":
							handleDoor((String) json.get(key));
							break;

						case "per":
							handlePer((String) json.get(key));
							break;

						case "key":
							handleKey((String) json.get(key));
							break;
					}
				}
			}

		} catch (ParseException ex) {
		}

		if (!msgATC.isEmpty()) {
			msgOut.put("ATC", msgATC);
		}

		if (!msgDriverDesk.isEmpty()) {
			msgOut.put("DRIVERDESK", msgDriverDesk);
		}

		return msgOut;
	}

	private void handleKey(String isKOH) {
		if(isKOH.equals("on")) {
			JSONObject status = new JSONObject();
			status.put("koh", true);
			msgATC.put("status", status);
		}
	}

	private void handlePer(String status) {
		JSONObject fault = new JSONObject();

		if(status.equals("on")) {
			fault.put("PER", true);	
			msgATC.put("fault", fault);
		} 
	}

	private void handleDoor(String status) {
		switch (status) {
			case "opened":
				msgDriverDesk.put("door", "opened");
				break;

			case "closed":
				msgDriverDesk.put("door", "closed");
				break;

		}
	}

	private void processMsgIn() {
		Iterator keys = msgIn.keySet().iterator();
		String key;
		JSONObject json = new JSONObject();

		while (keys.hasNext()) {
			key = (String) keys.next();

			switch (key) {
				case "door":
					json.put("door", msgIn.get(key));
					msgToPC.offer(json.toJSONString());
					break;

			}
		}

		msgIn.clear();
	}

}
