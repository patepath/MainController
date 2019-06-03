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
	private boolean isBrake = true;

	private final JSONObject msgAcar = new JSONObject();
	private final JSONObject msgDMI = new JSONObject();
	private final JSONObject msgVideo = new JSONObject();
	private final JSONObject msgTRAINMODEL1 = new JSONObject();
	private final JSONObject msgTRAINMODEL2 = new JSONObject();
	private final JSONObject msgZoomDoor = new JSONObject();
	private final JSONObject msgATC = new JSONObject();

	private boolean isAuxiliariesOn = false;
	private boolean isKeyOn = false;
	private boolean isFault = false;
	private int keyMode;
	private boolean isDoorEnable = true;
	private boolean isDoorPermissive = true;
	private boolean isATO = false;
	private boolean isATOReadyToRun = false;
	private boolean isPer = false;
	private boolean isATB = false;
	private boolean isParkingNotRelease = false;


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
		clearMsgOut();
		processMsgIn();
		processMsgSocket();
		packMsgOut();

		return msgOut;
	}

	private void clearMsgOut() {
		msgAcar.clear();
		msgDMI.clear();
		msgVideo.clear();
		msgTRAINMODEL1.clear();
		msgTRAINMODEL2.clear();
		msgZoomDoor.clear();
		msgATC.clear();
		msgOut.clear();
	}

	private void packMsgOut() {
		if (!msgAcar.isEmpty()) {
			msgOut.put("ACAR", msgAcar);
		}

		if (!msgDMI.isEmpty()) {
			msgOut.put("DMI", msgDMI);
		}

		if (!msgVideo.isEmpty()) {
			msgOut.put("VIDEO", msgVideo);
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
	}

	private void processMsgSocket() {
		String cmd = getCmdFromSocket("Q", "D");

		if (isAuxiliariesOn) {

			if (!cmd.equals("")) {
				String id = cmd.split(" ")[0];
				String data = cmd.split(" ")[1].substring(1);
				devs.get(id).replace("status", data);

				switch (((String) devs.get(id).get("type")).toLowerCase()) {
					case "key":
						handleKey(id);
						break;

					case "switch":
						handleSwitchs(id);
						break;
				}
			}
		}
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
			msgToControlPanel.offer("D226 S0");
			msgToControlPanel.offer("D227 S1");

			if(!isATB) {
				isKeyOn = false;
				msgDMI.put("cmd", "turn_on");
				msgAcar.put("cmd", "key_off");
				msgATC.put("cmd", "key_off");
				msgTRAINMODEL1.put("turnon", true);
				msgTRAINMODEL2.put("turnon", true);

			} else {
				msgToControlPanel.offer("D209 S1");		// Lamp ATC Start
				msgATC.put("cmd", "key_off");
			}
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
			msgToControlPanel.offer("D226 S1");
			msgToControlPanel.offer("D227 S0");
			

			msgAcar.put("cmd", "key_on");
			isKeyOn = true;
			JSONObject status = new JSONObject();
			JSONObject statusATC = new JSONObject();

			switch (keyMode) {
				case 1:		// Mode RV
					status.put("dmi", true);
					status.put("disable_button", true);
					status.put("mode", TrainEmuA.MODE_RV);
					msgDMI.put("status", status);
					statusATC.put("atc_mode", TrainEmuA.MODE_RV);
					msgATC.put("status", statusATC);
					break;

				case 2:		// Mode IDLE
					status.put("dmi", true);
					status.put("mode", TrainEmuA.MODE_IDLE);
					status.put("door_indicator", 0);
					status.put("req_mode", 1);
					msgDMI.put("status", status);
					break;

				case 3:		// Mode WM
					status.put("dmi", true);
					status.put("mode", 0);
					status.put("disable_button", true);
					status.put("mode", TrainEmuA.MODE_WM);
					msgDMI.put("status", status);
					statusATC.put("atc_mode", TrainEmuA.MODE_WM);
					msgATC.put("status", statusATC);
					break;

				case 4:		// Mode RM2
					status.put("dmi", false);
					msgDMI.put("status", status);
					statusATC.put("atc_mode", TrainEmuA.MODE_RM2);
					msgATC.put("status", statusATC);
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

			case "D205L":		// Door Open
				if(isDoorEnable) {
					doD205L();
				}
				break;

			case "D206L":		// Door Close
				if(isDoorEnable) {
					doD206L();
				}
				break;

			case "D205R":		// Door Open
				if(isDoorEnable) {
					doD205R();
				}
				break;

			case "D206R":		// Door Close
				if(isDoorEnable) {
					doD206R();
				}
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

			case "D211":		// Switch : Emergency Stop
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

			case "D223_head":
				doD223(id);
				break;

			case "D223_tail":
				doD223(id);
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
		printDev("D201");

	}

	private void doD202() {		// Switch : ATC Start
		printDev("D202");

		if(isATOReadyToRun){
			msgToControlPanel.offer("D209 S0");			// Lamp ATC Start
			msgATC.put("cmd", "ato_start");
			isATOReadyToRun = false;

		} else if(isATB) {
			msgToControlPanel.offer("D209 S0");			// Lamp ATC Start
			msgATC.put("cmd", "atb_start");
		}
	}

	private void doD203() {		// Switch : Spear

	}

	private void doD204() {		// Switch : Door Permissive
		printDev("D204");

	}

	private void doD205L() {		// Switch : Open Door
		printDev("D205L");

		JSONObject status = new JSONObject();

		if (isDoorClose) {
			msgTRAINMODEL1.put("opendoor", DOOR_LEFT);
			msgTRAINMODEL2.put("opendoor", DOOR_LEFT);

			status.put("door_indicator", 1);
			status.put("door_status", true);
			msgDMI.put("status", status);

			msgZoomDoor.put("door", "open");
			isDoorClose = false;
		}
	}

	private void doD206L() {		// Switch : Close Door
		printDev("D206L");
		JSONObject status = new JSONObject();

		if (!isDoorClose) {
			msgTRAINMODEL1.put("closedoor", DOOR_LEFT);
			msgTRAINMODEL2.put("closedoor", DOOR_LEFT);

			status.put("door_indicator", 1);
			status.put("door_status", false);
			msgDMI.put("status", status);

			msgZoomDoor.put("door", "close");
			isDoorClose = true;
		}
	}

	private void doD205R() {		// Switch : Open Door
		printDev("D205R");

		JSONObject status = new JSONObject();

		if (isDoorClose) {
			msgTRAINMODEL1.put("opendoor", DOOR_RIGHT);
			msgTRAINMODEL2.put("opendoor", DOOR_RIGHT);

			status.put("door_indicator", 2);
			status.put("door_status", true);
			msgDMI.put("status", status);

			msgZoomDoor.put("door", "open");
			isDoorClose = false;
		}
	}

	private void doD206R() {		// Switch : Close Door
		printDev("D206R");
		JSONObject status = new JSONObject();

		if (!isDoorClose) {
			msgTRAINMODEL1.put("closedoor", DOOR_RIGHT);
			msgTRAINMODEL2.put("closedoor", DOOR_RIGHT);

			status.put("door_indicator", 2);
			status.put("door_status", false);
			msgDMI.put("status", status);

			msgZoomDoor.put("door", "close");
			isDoorClose = true;
		}
	}
	private void doD207() {		// Switch : PTT
		printDev("D207");
	}

	private void doD208() {		// Switch : Emergency Intercom
		printDev("D208");

		if(isPer) {
			if(devs.get("D208").get("status").equals("1")) {
				msgToControlPanel.offer("D219 S1");

//				JSONObject acarStatus = new JSONObject();
//				acarStatus.put("intercomm", true);
//				msgAcar.put("status", acarStatus);

			} else {
				msgToControlPanel.offer("D219 S0");
			}
		}
	}

	private void doD209() {		// Switch : CAB to CAB
		printDev("D209");
	}

	private void doD210() {		// Switch : CAB to PA
		printDev("D210");
	}

	private void doD211() {		// Switch : Emergency Stop
		printDev("D211");
		JSONObject atcStatus = new JSONObject();
		
		if(devs.get("D211").get("status").equals("1")) {
			msgATC.put("cmd", "emergency_brake");

		} else {
			atcStatus.put("emergency_brake", false);
			msgATC.put("status", atcStatus);
		}
			
	}

	private void doD212() {		// Switch : Horn
		printDev("D212");
	}

	private void doD216() {		// Switch : General Fault
		printDev("D216");
		JSONObject status = new JSONObject();

		if(devs.get("D216").get("status").equals("1")) {
			if(isFault) {
				msgToControlPanel.offer("D201 S1");			// Lamp : General Fault
				status.put("faultAcknowledged", true);
				msgAcar.put("status", status);
			}
		}
	}

	private void doD218() {		// Switch : Parking Brake
		printDev("D218_ON");

		if(!isParkingNotRelease) {
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
	}

	private void doD219() {		// Switch : PIS Sound
		printDev("D219");

		if(devs.get("D219").get("status").equals("0")) {
			msgToControlPanel.offer("D207 S0");
			msgToControlPanel.offer("D208 S1");
		} else {
			msgToControlPanel.offer("D207 S1");
			msgToControlPanel.offer("D208 S0");
		}
	}

	private void doD220() {		// Switch : Electric Uncouple OFF-ON
		printDev("D220");

	}

	private void doD221() {		// Switch : Uncoupling
		printDev("D221");

	}

	private void doD222() {		// Switch : PNEUMatic Uncouple OFF-ON
		printDev("D222");

	}

	private void doD223(String id) {		// Switch : Head-al-Tail
		printDev(id);

		if(id.equals("D223_head") & devs.get("status").equals("1")) {
			msgToControlPanel.offer("D226 S0");
			msgToControlPanel.offer("D227 S1");
		}

		if(id.equals("D223_tail") & devs.get("status").equals("1")) {
			msgToControlPanel.offer("D226 S1");
			msgToControlPanel.offer("D227 S0");
		}

	}

	private void doD224() {		// Switch : Desk Light
		printDev("D224");

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
		printDev("D226");
		
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
		printDev("D228");

	}

	private void doD229() {		// Switch : Dim - Bright
		printDev("D229");

	}

	private void doD230() {		// Switch : Wind Shield Wipper 0-1-2-3
		printDev("D230");

	}

	private void doD231() {		// Switch : CAB Ventilation 0-3-2-1
		printDev("D231");

	}

	private void doD233() {		// Switch : Radio Control Button 1
		printDev("D233");

	}

	private void doD234() {		// Switch : Radio Control Button 2
		printDev("D234");

	}

	private void doD235() {		// Switch : Radio Control Button 3
		printDev("D235");

	}

	private void doD236() {		// Switch : Radio Control Button 4
		printDev("D236");

	}

	private void processMsgIn() {
		if (msgIn != null) {
			Iterator keys = msgIn.keySet().iterator();
			String key;

			while (keys.hasNext()) {
				key = (String) keys.next();

				switch (key.toLowerCase()) {
					case "isfault":
						handleFault((boolean) msgIn.get(key));
						break;

					case "isbrake":
						handleBrake((boolean) msgIn.get(key));
						break;

					case "isparkingbrake":
						JSONObject status = new JSONObject();

						isParkingNotRelease = (boolean) msgIn.get(key);
						msgToControlPanel.offer("D202 S1");
						status.put("parking_brake_released", false);
						msgAcar.put("status", status);
						msgATC.put("status", status);
						break;

					case "isemergencybrake":
						handleEmergencyBrake((boolean) msgIn.get(key));
						break;

					case "isauxiliarieson":
						handleAuxiliariesSwitch();
						break;

					case "speed":
						handleSpeed((double) msgIn.get(key));
						break;

					case "door":
						handleDoor((String) msgIn.get(key));		// Lamp ATC Start
						break;
					
					case "atc_start":
						msgToControlPanel.offer("D209 S2");
						isATOReadyToRun = true;
						break;

					case "per":
						handlePer((boolean) msgIn.get(key));
						break;

					case "atb":
						switch((int) msgIn.get(key)) {
							case 1:
								isATB = true;
								break;

							case 4:
								msgToControlPanel.offer("D209 S2");		// Lamp ATC Start
								break;
						}
				}
			}

			msgIn.clear();
		}
	}

	private void handlePer(boolean isPer) {
		if(isPer) {
			this.isPer = isPer;
			this.isFault = true;
			msgToControlPanel.offer("D219 S2");		// Lamp Intercomm
			msgToControlPanel.offer("D201 S2");		// Lamp General fault
		}
	}

	private void handleFault(boolean isFault) {
		this.isFault = isFault;

		if(isFault) {
			msgToControlPanel.offer("D201 S2");		// Lamp General fault

		} else {
			msgToControlPanel.offer("D201 S0");		// Lamp General fault
		}
	}

	private void handleEmergencyBrake(boolean isEB) {
		if(isEB) {
			msgToControlPanel.offer("D001 S30");	// Driver Desk : Braking guage meter red 2.4 bar
		}
	}
	
	private void handleBrake(boolean isbrake) {
		this.isBrake = isbrake;

		if(isBrake) {
			msgToControlPanel.offer("D001 S20");	// Driver Desk : Braking guage meter red 2.4 bar
		} else {
			msgToControlPanel.offer("D001 S00");	// Driver Desk : Braking guage meter red 2.4 bar
		}
	}

	private void handleAuxiliariesSwitch() {
		isAuxiliariesOn = (boolean) msgIn.get("isAuxiliariesOn");

		if (isAuxiliariesOn) {
			msgToControlPanel.offer("D004 S00");	// Speedo Meter
			msgToControlPanel.offer("D202 S1");		// Lamp - parking brake
			msgToControlPanel.offer("D001 S20");	// Driver Desk : init Guage meter red 2.4 bar
			msgToControlPanel.offer("D002 S50");	// Driver Desk : init Guage meter white 9 bar
			msgToControlPanel.offer("D003 S40");	// Driver Desk : Volt meter
			msgToControlPanel.offer("D207 S0");		// LED PIS-ON
			msgToControlPanel.offer("D208 S1");		// LED PIS-OFF

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

		if(speed == 0 & !isDoorEnable) {
			isDoorEnable = true;
		} else if(speed > 0 & isDoorEnable) {
			isDoorEnable = false;
		}
	}

	private void handleDoor(String status) {
		switch (status) {
			case "opened":
				msgToControlPanel.offer("D217 S0");
				msgToControlPanel.offer("D218 S1");
				msgToControlPanel.offer("D230 S1");
				msgToControlPanel.offer("D231 S0");
				break;

			case "closed":
				msgToControlPanel.offer("D217 S1");
				msgToControlPanel.offer("D218 S0");
				msgToControlPanel.offer("D230 S0");
				msgToControlPanel.offer("D231 S1");
		}
	}
}
