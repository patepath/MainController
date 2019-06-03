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
		clearMsgOut();
		processMsgIn();
		processMsgSocket();
		packMsgOut();

		return msgOut;
	}

	private void clearMsgOut() {
		msgLever.clear();
		msgATC.clear();
		msgOut.clear();
	}

	private void processMsgSocket() {
		try {
			JSONObject cmd = (JSONObject) new JSONParser().parse(getCmdFromSocket());
			Iterator keys = cmd.keySet().iterator();
			String key;

			while (keys.hasNext()) {
				key = (String) keys.next();
				System.out.println("DMI : " + cmd.toJSONString());

				switch (key.toUpperCase()) {
					case "ATC_MODE":
						handleATCMode((int) (long) cmd.get(key));
						break;

					case "ATP_BRAKE":
						handleATPBrake((int) (long) cmd.get(key));
						break;
					
				}
			}

		} catch (ParseException | NullPointerException ex) {
		}
	}

	private void packMsgOut() {
		if (!msgLever.isEmpty()) {
			msgOut.put("LEVER", msgLever);
		}

		if (!msgATC.isEmpty()) {
			msgOut.put("ATC", msgATC);
		}
	}

	private void handleATCMode(int status) {
		JSONObject data = new JSONObject();
		data.put("ATC_MODE", status);
		msgATC.put("status", data);
	}
	
	private void handleATPBrake(int status) {
		if(status == 0) {
			msgATC.put("cmd", "clear_atp_brake");
		}
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
			case "turn_off":
				handleCmdTurnOff();
				break;

			case "turn_on":
				handleCmdTurnOn();
				break;
		}
	}

	private void handleCmdTurnOff() {
		JSONObject cmd = new JSONObject();
		cmd.put("IS_TURNON", false);
		msgToPC.offer(cmd.toJSONString());
	}

	private void handleCmdTurnOn() {
		JSONObject cmd = new JSONObject();
		cmd.put("IS_TURNON", true);
		cmd.put("ATP_BRAKE", 0);
		cmd.put("ATENNA_STATUS", 1);
		msgToPC.offer(cmd.toJSONString());
	}
}
