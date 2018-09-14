/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ctsim.maincontroller.factory;

import java.util.Iterator;
import org.json.simple.JSONObject;
import com.ctsim.maincontroller.interfaces.ControlPanel;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author Patipat Punboonrat
 */
public class DMIEmuA extends PCControlPanel implements ControlPanel {

	private final JSONObject msgLever = new JSONObject();
	private final JSONObject msgATC = new JSONObject();

	@Override
	public void initDevices() {
		name = "DMI";
	}

	@Override
	public JSONObject process() {
		msgLever.clear();
		msgATC.clear();
		msgOut.clear();

		processMsgIn();

		try {
			JSONObject cmd = (JSONObject) new JSONParser().parse(getCmdFromSocket());
			Iterator keys = cmd.keySet().iterator();
			String key;

			while (keys.hasNext()) {
				key = (String) keys.next();

				switch (key) {
					case "ATC_MODE":
						handleATCMode((int) (long) cmd.get(key));
						break;

					case "ATP_BRAKE":
						msgLever.put("emergencybrake_active", (int) (long) cmd.get(key) != 0);
						break;
				}
			}

		} catch (ParseException | NullPointerException ex) {
		}

		if (!msgLever.isEmpty()) {
			msgOut.put("LEVER", msgLever);
		}

		if (!msgATC.isEmpty()) {
			msgOut.put("ATC", msgATC);
		}

		return msgOut;
	}

	private void handleATCMode(int status) {
		JSONObject data = new JSONObject();
		data.put("ATC_MODE", status);
		msgATC.put("status", data);
	}

	private void processMsgIn() {

		if (!msgIn.isEmpty()) {
			Iterator keys = msgIn.keySet().iterator();
			String key;

			while (keys.hasNext()) {
				key = (String) keys.next();

				switch (key) {
					case "cmd":
						handleCMD((String) msgIn.get(key));
						break;

					case "status":
						msgToPC.offer(((JSONObject) msgIn.get(key)).toJSONString());
						break;
				}
			}

			msgIn.clear();
		}
	}

	private void handleCMD(String cmd) {
		switch (cmd) {
			case "turnoff":
				handleShutdown();
				break;

			case "turnon":
				handleStartUp();
				break;
		}
	}

	private void handleShutdown() {
		JSONObject cmd = new JSONObject();
		cmd.put("IS_TURNON", false);
		cmd.put("DMI", "OFF");
		msgToPC.offer(cmd.toJSONString());
	}

	private void handleStartUp() {
		JSONObject cmd = new JSONObject();
		cmd.put("IS_TURNON", true);
		cmd.put("DMI", "ON");
		msgToPC.offer(cmd.toJSONString());
	}
}
