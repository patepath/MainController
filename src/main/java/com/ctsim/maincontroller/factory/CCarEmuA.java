/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ctsim.maincontroller.factory;

import org.json.simple.JSONObject;
import com.ctsim.maincontroller.interfaces.ControlPanel;
import java.util.Iterator;
import java.util.Calendar;

/**
 *
 * @author Patipat Punboonrat
 */
public class CCarEmuA extends MCSControlPanel implements ControlPanel {

	private boolean isShutdown = false;
	private boolean isCCarBreakerTripped;
	private final JSONObject msgAcar = new JSONObject();
	private final JSONObject msgATC = new JSONObject();

	public CCarEmuA() {
	}

	@Override
	public void initDevices() {
		name = "C-Car Control Panel";
		super.initMapping("ccar.xml");

		msgToControlPanel.offer("C099 S0");
	}

	@Override
	public JSONObject process() {
		msgATC.clear();
		msgAcar.clear();
		msgOut.clear();

		processMsgIn();

		String cmd = getCmdFromSocket("U", "C");

		if (!isShutdown) {
			if (!cmd.equals("")) {
				String id = cmd.split(" ")[0];
				String data = cmd.split(" ")[1].substring(1);
				devs.get(id).replace("status", data);

				JSONObject dev = devs.get(id);
				String type = (String) dev.get("type");

				switch (type) {
					case "Breaker":
						handleBreakers(id);
						break;

					case "Switch":
						handleSwitchs(id);
						break;
				}
			}

		}

		if (!msgATC.isEmpty()) {
			msgOut.put("ATC", msgATC);
		}

		if (!msgAcar.isEmpty()) {
			msgOut.put("ACAR", msgAcar);
		}

		return msgOut;
	}

	private void processMsgIn() {
		if (msgIn != null) {
			Iterator keys = msgIn.keySet().iterator();
			String key;

			while (keys.hasNext()) {
				key = (String) keys.next();

				switch (key) {
					case "cmd":
						handleCmd((String) msgIn.get(key));
						break;
				}
			}

			msgIn.clear();
		}
	}

	private void handleCmd(String cmd) {

		switch (cmd) {
			case "shutdown":
				shutdown();
				break;

			case "startup":
				startup();
				break;
		}
	}

	private void shutdown() {
		isShutdown = true;
		msgToControlPanel.offer("C099 S0");     // C-Car: All lamp
	}

	private void startup() {
		isShutdown = false;
		msgToControlPanel.offer("C099 S0");     // C-Car: All lamp
		checkBreakers();
		handleLampBreakerTripped();
	}

	private void handleLampBreakerTripped() {

		if (isCCarBreakerTripped) {
			msgToControlPanel.offer("C021 S1");     // C-Car: MCB TRIPPED Lamp
			msgAcar.put("is_ccar_mcb_tripped", true);

		} else {
			msgToControlPanel.offer("C021 S0");     // C-Car: MCB TRIPPED Lamp
			msgAcar.put("is_ccar_mcb_tripped", false);
		}
	}

	private void checkBreakers() {
		Iterator keys = devs.keySet().iterator();
		String key;
		JSONObject dev;

		isCCarBreakerTripped = false;

		while (keys.hasNext()) {
			key = (String) keys.next();
			dev = devs.get(key);

			if (dev.get("type").equals("Breaker") & dev.get("status").equals("0")) {
				isCCarBreakerTripped = true;
				break;
			}
		}

		handleLampBreakerTripped();
	}

//********************************************************************************
// BREAKER
//********************************************************************************
	private void handleBreakers(String id) {

		checkBreakers();

		switch (id) {
			case "C101":
				doC101();
				break;

			case "C102":
				doC102();
				break;

			case "C103":
				doC103();
				break;

			case "C104":
				doC104();
				break;

			case "C105":
				doC105();
				break;

			case "C106":
				doC106();
				break;

			case "C107":
				doC107();
				break;

			case "C108":
				doC108();
				break;

			case "C109":
				doC109();
				break;

			case "C110":
				doC110();
				break;

			case "C111":
				doC111();
				break;

			case "C112":
				doC112();
				break;

			case "C113":
				doC113();
				break;

			case "C114":
				doC114();
				break;

			case "C115":
				doC115();
				break;

			case "C116":
				doC116();
				break;

			case "C117":
				doC117();
				break;

			case "C118":
				doC118();
				break;
		}
	}

	private void doC101() {		// Breaker : Batter Main Connector
		printDev("C101");

	}

	private void doC102() {		// Breaker : Current Collector 750V. Monitoring
		printDev("C102");

	}

	private void doC103() {		// Breaker : AUX. Inverter
		printDev("C103");

	}

	private void doC104() {
		printDev("C104");
		String status = (String) devs.get("C104").get("status");

		if (status.equals("0")) {
			msgATC.put("fault_ccar_bcu", false);
		} else {
			msgATC.put("fault_ccar_bcu", true);
		}
	}

	private void doC105() {		// Breaker : Parking Brake Value
		printDev("C105");

	}

	private void doC106() {		// Breaker : Parking Brake Control
		printDev("C106");

	}

	private void doC107() {		// Breaker : PNEUM Brake Cut Out
		printDev("C107");

	}

	private void doC108() {		// Breaker : AUX. Inverter Air COND. Control
		printDev("C108");

	}

	private void doC109() {		// Breaker : Air Compressor Indication Lights
		printDev("C109");

	}

	private void doC110() {		// Breaker : Emergency Brake
		printDev("C110");

	}

	private void doC111() {		// Breaker : PA Central Unit
		printDev("C111");

	}

	private void doC112() {		// Breaker : Light
		printDev("C112");

	}

	private void doC113() {		// Breaker : Emergency Light
		printDev("C113");

	}

	private void doC114() {		// Breaker : Air COND. Control 1
		printDev("C114");

	}

	private void doC115() {		// Breaker : Air COND. Control 2
		printDev("C115");

	}

	private void doC116() {		// Breaker : Door Supply 1
		printDev("C116");

	}

	private void doC117() {		// Breaker : Door Supply 2
		printDev("C117");

	}

	private void doC118() {		// Breaker : Door Control
		printDev("C118");

	}

//********************************************************************************
// SWITCH
//********************************************************************************
	private void handleSwitchs(String id) {
		if (id.equals("C201")) {
			now = Calendar.getInstance();
			System.out.println(sdf.format(now.getTime()) + " " + devs.get(id).get("type") + " - " + devs.get(id).get("name") + " activated");

		}
	}
}
