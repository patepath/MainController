/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ctsim.maincontroller.factory;

import org.json.simple.JSONObject;
import com.ctsim.maincontroller.interfaces.ControlPanel;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Iterator;

/**
 *
 * @author Patipat Punboonrat
 */
public class DriverDeskEmuA extends MCSControlPanel implements ControlPanel {

	private final int DOOR_LEFT = 1;
	private final int DOOR_RIGHT = 2;

	private final JSONObject msgAcar = new JSONObject();
	private final JSONObject msgDMI = new JSONObject();
	private final JSONObject msgTRAINMODEL1 = new JSONObject();
	private final JSONObject msgTRAINMODEL2 = new JSONObject();
	private final JSONObject msgZoomDoor = new JSONObject();
	private final JSONObject msgATC = new JSONObject();

	private boolean isAuxiliariesOn = false;
	private boolean isKeyOn = false;
	private int keyMode;

	private String cmdMeter;
	private String oldCmdMeter = "";
	private boolean isDoorClose = false;

	@Override
	public void initDevices() {
		name = "Driver Desk";
		super.initMapping("driverdesk.xml");
	}

	@Override
	public JSONObject process() {
		msgAcar.clear();
		msgDMI.clear();
		msgTRAINMODEL1.clear();
		msgTRAINMODEL2.clear();
		msgZoomDoor.clear();
		msgATC.clear();
		msgOut.clear();

		processMsgIn();
		String cmd = getCmdFromSocket("Q", "D");

		if (isAuxiliariesOn) {

			if (!cmd.equals("")) {
				String id = cmd.split(" ")[0];
				String data = cmd.split(" ")[1].substring(1);
				devs.get(id).replace("status", data);

				switch ((String) devs.get(id).get("type")) {
					case "Key":
						handleKey(id);
						break;

					case "Switch":
						handleSwitchs(id);
						break;
				}
			}
		}

		if (!msgAcar.isEmpty()) {
			msgOut.put("ACAR", msgAcar);
		}

		if (!msgDMI.isEmpty()) {
			msgOut.put("DMI", msgDMI);
		}

		if (!msgTRAINMODEL1.isEmpty()) {
			msgOut.put("TRAINMODEL1", msgTRAINMODEL1);
		}

		if (!msgTRAINMODEL2.isEmpty()) {
			msgOut.put("TRAINMODEL2", msgTRAINMODEL2);
		}

		if (!msgZoomDoor.isEmpty()) {
			msgOut.put("ZOOMDOOR", msgZoomDoor);
		}

		if (!msgATC.isEmpty()) {
			msgOut.put("ATC", msgATC);
		}

		return msgOut;
	}

//********************************************************************************I
// KEY
//********************************************************************************I
	private void handleKey(String id) {

		switch (id) {
			case "D410":
				handleKeyOff();
				break;

			case "D411":
				handleKeySelect();
				break;

			case "D412":
				handleKeyOn();
				break;

			case "D413":
				handleModeRV();
				break;

			case "D414":
				handleModeForward();
				break;

			case "D415":
				handleModeWM();
				break;

			case "D416":
				handleModeRM2();
				break;
		}
	}

	private void handleKeyOff() {
		if (devs.get("D410").get("status").equals("1")) {
			now = Calendar.getInstance();
			System.out.println(sdf.format(now.getTime()) + " Switch - Key Off Activated");

			isKeyOn = false;
			JSONObject status = new JSONObject();

			status.put("MODE", 0);
			status.put("DISABLE_BUTTON", true);
			msgDMI.put("status", status);
			msgAcar.put("cmd", "key_off");
		}
	}

	private void handleKeySelect() {
		if (devs.get("D411").get("status").equals("1")) {
			now = Calendar.getInstance();
			System.out.println(sdf.format(now.getTime()) + " Switch - Key Select Activated");
		}
	}

	private void handleKeyOn() {
		if (devs.get("D412").get("status").equals("1")) {
			now = Calendar.getInstance();
			System.out.println(sdf.format(now.getTime()) + " Switch - Key On Activated");

			msgAcar.put("cmd", "key_on");
			isKeyOn = true;
			JSONObject status = new JSONObject();

			switch (keyMode) {
				case 1:
					status.put("MODE", 0);
					status.put("DISABLE_BUTTON", true);
					status.put("MODE", 7);
					msgDMI.put("status", status);
					break;

				case 2:
					status.put("MODE", 0);
					status.put("REQ_MODE", 1);
					msgDMI.put("status", status);
					break;

				case 3:
					status.put("MODE", 0);
					status.put("DISABLE_BUTTON", true);
					status.put("MODE", 8);
					msgDMI.put("status", status);
					break;
			}
		}
	}

	private void handleModeRV() {
		if (devs.get("D413").get("status").equals("0")) {
			now = Calendar.getInstance();
			System.out.println(sdf.format(now.getTime()) + " Switch - Key R Activated");

			keyMode = 1;
		}
	}

	private void handleModeForward() {
		if (devs.get("D414").get("status").equals("1")) {
			now = Calendar.getInstance();
			System.out.println(sdf.format(now.getTime()) + " Switch - Key F Activated");

			keyMode = 2;
		}
	}

	private void handleModeWM() {
		if (devs.get("D415").get("status").equals("1")) {
			now = Calendar.getInstance();
			System.out.println(sdf.format(now.getTime()) + " Switch - Key WM Activated");

			keyMode = 3;
		}
	}

	private void handleModeRM2() {
		if (devs.get("D416").get("status").equals("1")) {
			now = Calendar.getInstance();
			System.out.println(sdf.format(now.getTime()) + " Switch - Key RM2 Activated");

			keyMode = 4;
		}
	}

//********************************************************************************I
// SWITCH
//********************************************************************************I
	private void handleSwitchs(String id) {
		switch (id) {
			case "D201":
				doD201();
				break;

			case "D202":
				doD202();
				break;

			case "D203":
				doD203();
				break;

			case "D204":
				doD204();
				break;

			case "D205":
				doD205();
				break;

			case "D206":
				doD206();
				break;

			case "D207":
				doD207();
				break;

			case "D208":
				doD208();
				break;

			case "D209":
				doD209();
				break;

			case "D210":
				doD210();
				break;

			case "D211":
				doD211();
				break;

			case "D212":
				doD212();
				break;

			case "D216":
				doD216();
				break;

			case "D218_ON":
				doD218();
				break;

			case "D218_OFF":
				doD218();
				break;

			case "D219":
				doD219();
				break;

			case "D220":
				doD220();
				break;

			case "D221":
				doD221();
				break;

			case "D222":
				doD222();
				break;

			case "D223":
				doD223();
				break;

			case "D224":
				doD224();
				break;

			case "D225_OFF":
				doD225(false);
				break;
				
			case "D225_ON":
				doD225(true);
				break;

			case "D226":
				doD226();
				break;

			case "D227":
				doD227();
				break;

			case "D228":
				doD228();
				break;

			case "D229":
				doD229();
				break;

			case "D230":
				doD230();
				break;

			case "D231":
				doD231();
				break;

			case "D233":
				doD233();
				break;

			case "D234":
				doD234();
				break;

			case "D235":
				doD235();
				break;

			case "D236":
				doD236();
				break;
		}
	}

	private void doD201() {		// Switch : Automatic Reversing

	}

	private void doD202() {		// Switch : ATC Start

	}

	private void doD203() {		// Switch : Spear

	}

	private void doD204() {		// Switch : Door Permissive

	}

	private void doD205() {		// Switch : Open Door
		printDev("D205");

		JSONObject status = new JSONObject();

		if (isDoorClose) {
			msgTRAINMODEL1.put("opendoor", DOOR_LEFT);
			msgTRAINMODEL2.put("opendoor", DOOR_LEFT);

			status.put("DOOR_INDICATOR", 1);
			msgDMI.put("status", status);

			status.put("DOOR_STATUS", true);
			msgDMI.put("status", status);

			msgZoomDoor.put("door", "open");
			isDoorClose = false;
		}
	}

	private void doD206() {		// Switch : Close Door
		printDev("D206");
		JSONObject status = new JSONObject();

		if (!isDoorClose) {
			msgTRAINMODEL1.put("closedoor", DOOR_LEFT);
			msgTRAINMODEL2.put("closedoor", DOOR_LEFT);

			status.put("DOOR_INDICATOR", 1);
			msgDMI.put("status", status);

			status.put("DOOR_STATUS", false);
			msgDMI.put("status", status);

			msgZoomDoor.put("door", "close");
			isDoorClose = true;
		}
	}

	private void doD207() {		// Switch : PTT

	}

	private void doD208() {		// Switch : Emergency Intercom

	}

	private void doD209() {		// Switch : CAB to CAB

	}

	private void doD210() {		// Switch : CAB to PA

	}

	private void doD211() {		// Switch : Emergency Stop

	}

	private void doD212() {		// Switch : Horn

	}

	private void doD216() {		// Switch : General Fault

	}

	private void doD218() {		// Switch : Parking Brake
		printDev("D218_ON");
		JSONObject status = new JSONObject();

		if (devs.get("D218_OFF").get("status").equals("1")) {
			msgToControlPanel.offer("D202 S0");
			status.put("parking_brake_released", true);
		}

		if (devs.get("D218_ON").get("status").equals("1")) {
			msgToControlPanel.offer("D202 S1");
			status.put("parking_brake_released", false);
		}

		msgAcar.put("status", status);
		msgATC.put("status", status);
	}

	private void doD219() {		// Switch : PIS Sound

	}

	private void doD220() {		// Switch : Electric Uncouple OFF-ON

	}

	private void doD221() {		// Switch : Uncoupling

	}

	private void doD222() {		// Switch : PNEUMatic Uncouple OFF-ON

	}

	private void doD223() {		// Switch : Head-Neutral-Tail

	}

	private void doD224() {		// Switch : Desk Light

	}

	private void doD225(boolean status) {		// Switch : Saloon Light
		printDev("D225_ON");
		
		if(status) {
			msgToControlPanel.offer("D204 S1");
			//msgToControlPanel.offer("D201 S1");
		} else {
			msgToControlPanel.offer("D204 S0");
			//msgToControlPanel.offer("D201 S0");
		}
	}

	private void doD226() {		// Switch : Dimitter
		
	}

	private void doD227() {		// Switch : CAB Light
		printDev("D227");
		
		if(devs.get("D227").get("status").equals("1")) {
			msgToControlPanel.offer("D205 S1");
		} else {
			msgToControlPanel.offer("D205 S0");
		}
	}

	private void doD228() {		// Switch : Wind Shield Washer OFF-ON

	}

	private void doD229() {		// Switch : Dim - Bright

	}

	private void doD230() {		// Switch : Wind Shield Wipper 0-1-2-3

	}

	private void doD231() {		// Switch : CAB Ventilation 0-3-2-1

	}

	private void doD233() {		// Switch : Radio Control Button 1

	}

	private void doD234() {		// Switch : Radio Control Button 2

	}

	private void doD235() {		// Switch : Radio Control Button 3

	}

	private void doD236() {		// Switch : Radio Control Button 4

	}

	private void processMsgIn() {
		if (msgIn != null) {
			Iterator keys = msgIn.keySet().iterator();
			String key;

			while (keys.hasNext()) {
				key = (String) keys.next();

				switch (key) {
					case "isAuxiliariesOn":
						handleAuxiliariesSwitch();
						break;

					case "SPEED":
						handleSpeed((double) msgIn.get(key));
						break;

					case "door":
						handleDoor((String) msgIn.get(key));
						break;
				}
			}

			msgIn.clear();
		}
	}

	private void handleAuxiliariesSwitch() {
		isAuxiliariesOn = (boolean) msgIn.get("isAuxiliariesOn");

		if (isAuxiliariesOn) {
			msgToControlPanel.offer("D004 S00");	// Speedo Meter
			msgToControlPanel.offer("D202 S1");		// Lamp - parking brake
			msgToControlPanel.offer("D001 S60");	// Driver Desk : init Guage meter red 9 bar
			msgToControlPanel.offer("D002 S40");	// Driver Desk : init Guage meter white 2.4 bar
			msgToControlPanel.offer("D003 S40");	// Driver Desk : Volt meter

		} else {
			msgToControlPanel.offer("D004 S00");	// Speedo Meter
			msgToControlPanel.offer("D299 S0");		// Lamp - parking brake
			msgToControlPanel.offer("D001 S00");	// Driver Desk : init Guage meter red 0 bar
			msgToControlPanel.offer("D002 S00");	// Driver Desk : init Guage meter white 0 bar
			msgToControlPanel.offer("D003 S00");	// Driver Desk : Volt meter
		}
	}

	private void handleSpeed(double speed) {
		DecimalFormat df = new DecimalFormat("00");
		cmdMeter = "D004 S" + df.format(speed);	// Speedo Meter

		if (!cmdMeter.equals(oldCmdMeter)) {
			msgToControlPanel.offer(cmdMeter);
			oldCmdMeter = cmdMeter;
		}
	}

	private void handleDoor(String status) {
		switch (status) {
			case "opened":
				msgToControlPanel.offer("D217 S0");
				msgToControlPanel.offer("D218 S1");
				break;

			case "closed":
				msgToControlPanel.offer("D217 S1");
				msgToControlPanel.offer("D218 S0");
		}
	}
}
