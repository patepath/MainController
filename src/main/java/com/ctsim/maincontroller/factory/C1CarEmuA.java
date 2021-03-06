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
 * @author instructor
 */
public class C1CarEmuA extends MCSControlPanel implements ControlPanel {

    private boolean isC1CarBreakerTripped;
    private boolean isBrakeCutoutCcar1 = false;
    private boolean isBrakeCutoutCcar2 = false;
    private boolean isBrakeRelaseCcar1 = false;
    private boolean isBrakeRelaseCcar2 = false;
	private Calendar brakeCutOut1Start;
	private Calendar brakeCutOut2Start;

    private final JSONObject msgAcar = new JSONObject();
    private final JSONObject msgATC = new JSONObject();

    @Override
    public void initDevices() {
        name = "C1-Car Control Panel";
        super.initMapping("c1car.xml");

        msgToControlPanel.offer("C099 S0");
    }

    @Override
    public JSONObject process() {
        msgATC.clear();
        msgAcar.clear();
        msgOut.clear();

        processMsgIn();

        String cmd = getCmdFromSocket("U", "C");

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
                    handleSwitch(id);
                    break;
            }
        }

		processDelay();

        if (!msgATC.isEmpty()) {
            msgOut.put("ATC", msgATC);
        }

        if (!msgAcar.isEmpty()) {
            msgOut.put("ACAR", msgAcar);
        }

        return msgOut;
    }

	private void processDelay() {
		if(isBrakeCutoutCcar1 & !isBrakeRelaseCcar1) {
			if(Calendar.getInstance().getTimeInMillis() - brakeCutOut1Start.getTimeInMillis() > 3000) {
				msgToControlPanel.offer("C012 S1");
				isBrakeRelaseCcar1 = true;
				System.out.println("RELEASE 1");
			}
		}
		
		if(isBrakeCutoutCcar2 & !isBrakeRelaseCcar2) {
			if(Calendar.getInstance().getTimeInMillis() - brakeCutOut2Start.getTimeInMillis() > 3000) {
				msgToControlPanel.offer("C013 S1");
				isBrakeRelaseCcar2 = true;
				System.out.println("RELEASE 2");
			}
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
                        handleCmd((String) msgIn.get(key));
                        break;

                    case "status":
                        handleStatus((JSONObject) msgIn.get(key));
                        break;

                }
            }

			msgIn.clear();
        }
    }

    private void handleCmd(String cmd) {

    }

    private void handleStatus(JSONObject status) {
		System.out.println(status.toJSONString());

        if (status != null) {
            Iterator keys = status.keySet().iterator();
            String key;

            while (keys.hasNext()) {
                key = (String) keys.next();

                switch (key) {
                    case "valveb09":
                        handleValveB09((JSONObject) status.get(key));
                        break;

					case "doorcontrol" :
						msgToControlPanel.offer("C001 S1");
						break;

					case "bcu":
						msgToControlPanel.offer("C002 S1");
						break;

					case "aux":
						msgToControlPanel.offer("C004 S1");		// Lamp - Aux
						break;
                }
            }
        }
    }

    private void handleValveB09(JSONObject valveb09) {
        if ((int) valveb09.get("status") == 1) {
            if ((int) valveb09.get("no") == 1) {
                isBrakeCutoutCcar1 = true;
				brakeCutOut1Start = Calendar.getInstance();

            } else if ((int) valveb09.get("no") == 2) {
                isBrakeCutoutCcar2 = true;
				brakeCutOut2Start = Calendar.getInstance();
            }

        } else if ((int) valveb09.get("status") == 2) {
            if ((int) valveb09.get("no") == 1) {
                isBrakeCutoutCcar1 = false;

            } else if ((int) valveb09.get("no") == 2) {
                isBrakeCutoutCcar2 = false;
            }
        }

        if (isBrakeCutoutCcar1) {
            msgToControlPanel.offer("C009 S1");
        } else {
            msgToControlPanel.offer("C009 S0");
        }

        if (isBrakeCutoutCcar2) {
            msgToControlPanel.offer("C010 S1");
        } else {
            msgToControlPanel.offer("C010 S0");
        }
    }

    private void checkBreakers() {
        Iterator keys = devs.keySet().iterator();
        String key;
        JSONObject dev;

        isC1CarBreakerTripped = false;

        while (keys.hasNext()) {
            key = (String) keys.next();
            dev = devs.get(key);

            if (dev.get("type").equals("Breaker") & dev.get("status").equals("0")) {
                isC1CarBreakerTripped = true;
                break;
            }
        }

        handleLampBreakerTripped();
    }

    private void handleLampBreakerTripped() {

        if (isC1CarBreakerTripped) {
            msgToControlPanel.offer("C015 S1");	 // C1-Car: MCB TRIPPED Lamp			
            msgAcar.put("is_c1car_mcb_tripped", true);

        } else {
            msgToControlPanel.offer("C015 S0");	 // C1-Car: MCB TRIPPED Lamp
            msgAcar.put("is_c1car_mcb_tripped", false);
        }
    }

//********************************************************************************
// BREAKER
//********************************************************************************
    private void handleBreakers(String id) {
        checkBreakers();

        switch (id) {
            case "K101":
                doK101();
                break;

            case "K102":
                doK102();
                break;

            case "K103":
                doK103();
                break;

            case "K104":
                doK104();
                break;

            case "K105":
                doK105();
                break;

            case "K106":
                doK106();
                break;

            case "K107":
                doK107();
                break;

            case "K108":
                doK108();
                break;

            case "K109":
                doK109();
                break;

            case "K110":
                doK110();
                break;

            case "K111":
                doK111();
                break;

            case "K112":
                doK112();
                break;

            case "K113":
                doK113();
                break;

            case "K114":
                doK114();
                break;

            case "K115":
                doK115();
                break;

        }
    }

    private void doK101() {		// Braker : Contactors Monitoring

    }

    private void doK102() {		// Braker : AUX. Inverter Control Supply

    }

    private void doK103() {		// Braker : RIO Value

    }

    private void doK104() {		// Braker : Gateway Value

    }

    private void doK105() {		// Braker : Parking Brake Value 

    }

    private void doK106() {		// Braker : PNEUMatic Brake Cut Out

    }

    private void doK107() {		// Braker : AUX. INV. Message

    }

    private void doK108() {		// Braker : Auxiliaries

    }

    private void doK109() {		// Braker : Saloon Light

    }

    private void doK110() {		// Braker : Emergency Light

    }

    private void doK111() {		// Braker : Air Condition Control

    }

    private void doK112() {		// Braker : Emergency Ventilation

    }

    private void doK113() {		// Braker : Door Supply 1

    }

    private void doK114() {		// Braker : Door Supply 2

    }

    private void doK115() {		// Braker : Door Loop

    }

//********************************************************************************
// SWITCH
//********************************************************************************
    private void handleSwitch(String id) {
        if (id.equals("K201")) {

        }
    }
}
