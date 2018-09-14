/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ctsim.maincontroller.factory;

import com.ctsim.maincontroller.interfaces.ATC;
import java.util.Calendar;
import java.util.Iterator;
import org.json.simple.JSONObject;

/**
 *
 * @author Patipat Punboonrat
 */
public class ATCEmuA implements ATC {

	private boolean isShutdown = true;

	private final JSONObject msgIn = new JSONObject();
	private final JSONObject msgOut = new JSONObject();

	protected final JSONObject msgAcar = new JSONObject();
	protected final JSONObject msgCcar = new JSONObject();
	protected final JSONObject msgC1car = new JSONObject();
	protected final JSONObject msgDriverDesk = new JSONObject();
	protected final JSONObject msgVideo = new JSONObject();
	protected final JSONObject msgDMI = new JSONObject();
	protected final JSONObject msgLever = new JSONObject();

	private int bcuStatus;
	private int mode;
	private int ceilingSpeed = 0;
	private double leverValue = 0;
	private String leverPosition = "0";
	private boolean isLeverPressed = false;
	private final boolean isParkingBrakeReleased = false;
	private boolean isEmergencyBrakeActive = false;
	private final Calendar now;

	private String cmdBuzzer;
	private final String oldCmdBuzzer = "OFF";

	public ATCEmuA() {
		now = Calendar.getInstance();
	}

	@Override
	public JSONObject processMsg(JSONObject msg) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public JSONObject operate() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void setMode(int mode) {
		this.mode = mode;
	}

	@Override
	public int getMode() {
		return this.mode;
	}

	@Override
	public JSONObject process() {
        clearMsg();
		processMsgIn();
        packMsg();

		return msgOut;
	}

    private void clearMsg() {
		msgAcar.clear();
		msgCcar.clear();
		msgC1car.clear();
		msgDriverDesk.clear();
		msgDMI.clear();
		msgVideo.clear();
        msgLever.clear();
		msgOut.clear();
    }

    private void packMsg() {
		if (!msgAcar.isEmpty()) {
			msgOut.put("ACAR", msgAcar);
		}

		if (!msgCcar.isEmpty()) {
			msgOut.put("CCAR", msgCcar);
		}

		if (!msgC1car.isEmpty()) {
			msgOut.put("C1CAR", msgC1car);
		}

		if (!msgDriverDesk.isEmpty()) {
			msgOut.put("DRIVERDESK", msgDriverDesk);
		}

		if (!msgDMI.isEmpty()) {
			msgOut.put("DMI", msgDMI);
		}

		if (!msgLever.isEmpty()) {
			msgOut.put("LEVER", msgLever);
		}
    }

	private void processMsgIn() {
		if (msgIn != null) {
			Iterator keys = msgIn.keySet().iterator();
			String key;

			while (keys.hasNext()) {
				key = (String) keys.next();

				switch (key) {
					case "cmd":
						handleCMD((String) msgIn.get(key));
						break;

					case "status":
						handleStatus((JSONObject) msgIn.get(key));
						break;
				}
			}

			msgIn.clear();
		}
	}

	private void handleCMD(String cmd) {
		switch (cmd) {
			case "shutdown":
				shutdown();
				break;

			case "startup":
				startup();
				break;
		}
	}

	private void handleStatus(JSONObject status) {
		Iterator keys = status.keySet().iterator();
		String key;

		while (keys.hasNext()) {
			key = (String) keys.next();

			switch (key.toLowerCase()) {
				case "atc_mode":
                    System.out.println("RECEIVED ATC_MODE");
					mode = (int) status.get(key);
                    handleATCMode(mode);
					break;

				case "bcu":
					handleBCU((int) status.get(key));
					break;

				case "lever_value":
					handleLeverValue((double) status.get(key));
					break;

				case "lever_position":
					handleLeverPosition((String) status.get(key));
					break;

				case "lever_press":
					handleLeverPress((boolean) status.get(key));
					break;

				case "is_emergencybrake_active":
					isEmergencyBrakeActive = (boolean) status.get(key);
					break;

				case "old_mode":
					handleOldMode((String) status.get(key));
					break;

				case "state":
					handleState((String) status.get(key));
					break;

				case "ceiling_speed" :
					handleCeilingSpeed((int) status.get(key));
					break;

			}
		}
	}

	private void handleOldMode(String oldMode) {
		switch (oldMode.toLowerCase()) {
			case "stop":
				startup();
				break;

		}
	}

	private void handleState(String state) {
		switch (state.toLowerCase()) {
			case "STOP":
				break;

		}
	}

	private void handleBCU(int status) {
		bcuStatus = status;
	}

	private void handleATCMode(int mode) {
		this.mode = mode;

        if(mode == TrainEmuA.MODE_IDLE){
            msgLever.put("enable", false);
        } else {
            msgLever.put("enable", true);
        }
	}

	private void handleLeverValue(double value) {
		leverValue = value;
	}

	private void handleLeverPosition(String position) {
		leverPosition = position;
	}

	private void handleLeverPress(boolean isPress) {
		isLeverPressed = isPress;
	}

	private void handleEB(boolean isActive) {
		isEmergencyBrakeActive = isActive;
	}

	private void handleCeilingSpeed(int ceilingSpeed) {
		this.ceilingSpeed = ceilingSpeed;

		JSONObject data = new JSONObject();
		data.put("CEILING_SPEED", (double) ceilingSpeed);
		msgDMI.put("status", data);
	}

	@Override
	public void putMsg(JSONObject msg) {
		Iterator keys = msg.keySet().iterator();
		String key;

		while (keys.hasNext()) {
			key = (String) keys.next();
			msgIn.put(key, msg.get(key));
		}
	}

	private void shutdown() {
		isShutdown = true;

		msgAcar.put("cmd", "shutdown");
		msgCcar.put("cmd", "shutdown");
		msgC1car.put("cmd", "shutdown");
		msgDriverDesk.put("cmd", "shutdown");
		msgDMI.put("cmd", "turnoff");
	}

	private void startup() {
		isShutdown = false;

		msgAcar.put("cmd", "startup");
		msgCcar.put("cmd", "startup");
		msgC1car.put("cmd", "startup");
		msgDriverDesk.put("cmd", "startup");
		msgDMI.put("cmd", "turnoff");
        msgVideo.put("START", "depot");
	}
}
