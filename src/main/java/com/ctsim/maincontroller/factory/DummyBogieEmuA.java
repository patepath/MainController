/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ctsim.maincontroller.factory;

import com.ctsim.maincontroller.interfaces.ControlPanel;
import java.net.Socket;
import java.util.Iterator;
import org.json.simple.JSONObject;

/**
 *
 * @author Patipat Punboonrat
 */
public class DummyBogieEmuA extends PCControlPanel implements ControlPanel {

	@Override
	public void initDevices() {
		name = "DUMMYBOGIE";
	}

	@Override
	public JSONObject process() {
		msgOut.clear();
		Iterator keys = msgIn.keySet().iterator();
		String key;
		String value;

		while (keys.hasNext()) {
			key = (String) keys.next();

			if (key.equals("SPEED")) {
				value = String.valueOf(msgIn.get(key));
				msgToPC.offer(value);
			}
		}

		String cmd = getCmdFromSocket();
		msgIn.clear();

		return msgOut;
	}

}
