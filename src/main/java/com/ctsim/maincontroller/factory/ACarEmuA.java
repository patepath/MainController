/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ctsim.maincontroller.factory;

import com.ctsim.maincontroller.interfaces.ControlPanel;
import java.util.Calendar;
import org.json.simple.JSONObject;
import java.util.Iterator;

/**
 *
 * @author Patipat Punboonrat
 *
 */
public class ACarEmuA extends MCSControlPanel implements ControlPanel {

    private boolean isShutdown = false;
    private boolean isACarBreakerTripped = false;
    private boolean isCCarBreakerTripped = false;
    private boolean isC1CarBreakerTripped = false;

    private boolean isBreakersTripped = false;
    private boolean isBrakeCutoutAcar1 = false;
    private boolean isBrakeCutoutAcar2 = false;
    private boolean isBrakeCutoutCcar1 = false;
    private boolean isBrakeCutoutCcar2 = false;
    private boolean isBrakeCutoutC1car1 = false;
    private boolean isBrakeCutoutC1car2 = false;
    private boolean isBrakeCutoutAcarUnmaned1 = false;
    private boolean isBrakeCutoutAcarUnmaned2 = false;
    private Calendar brakeCutOut1Start;
    private Calendar brakeCutOut2Start;
    private boolean isBrakeRelaseAcar1 = false;
    private boolean isBrakeRelaseAcar2 = false;
    private boolean isFrictionBrakeOn = false;

    private final boolean isFail = false;
    private boolean isATCByPass = false;

    private final JSONObject msgATC = new JSONObject();
    private final JSONObject msgDMI = new JSONObject();
    private final JSONObject msgDriverDesk = new JSONObject();
    private final JSONObject msgInstructor = new JSONObject();
    private final JSONObject msgTrainModel1 = new JSONObject();
    private final JSONObject msgTrainModel2 = new JSONObject();
    private final JSONObject msgZoomDoor = new JSONObject();

    public ACarEmuA() {

    }

    @Override
    public void initDevices() {
        name = "A-Car Control Panel";
        initMapping("acar.xml");
        msgToControlPanel.offer("A299 S0");	 // All lamp
        //msgToControlPanel.offer("A218 S0");	 // Contactors Lamp
    }

    @Override
    public JSONObject process() {
        msgDriverDesk.clear();
        msgDMI.clear();
        msgATC.clear();
        msgInstructor.clear();
        msgTrainModel1.clear();
        msgTrainModel2.clear();
        msgZoomDoor.clear();
        msgOut.clear();

        processMsgIn();
        String cmd = getCmdFromSocket("Q", "A");

        try {
            String id = cmd.split(" ")[0];
            String data = cmd.split(" ")[1].substring(1);
            devs.get(id).replace("status", data);

            if (id.equals("A101")) {
                doA101((JSONObject) devs.get("A101"));	// Breaker : Main Breaker
            }

            if (!isShutdown) {
                JSONObject dev = devs.get(id);
                String type = (String) dev.get("type");

                switch (type) {
                    case "Breaker":
                        handleBreaker(id);
                        syncWithInstructor((JSONObject) devs.get(id));
                        break;

                    case "Switch":
                        handleSwitchs(id);
                        break;

                    case "Lamp":
                        break;
                }
            }

        } catch (ArrayIndexOutOfBoundsException | NullPointerException ex) {
        }

        processDelay();

        if (!msgDriverDesk.isEmpty()) {
            msgOut.put("DRIVERDESK", msgDriverDesk);
        }

        if (!msgDMI.isEmpty()) {
            msgOut.put("DMI", msgDMI);
        }

        if (!msgATC.isEmpty()) {
            msgOut.put("ATC", msgATC);
        }

        if (!msgInstructor.isEmpty()) {
            msgOut.put("INSTRUCTOR", msgInstructor);
        }

        if (!msgTrainModel1.isEmpty()) {
            msgOut.put("TRAINMODEL1", msgTrainModel1);
        }

        if (!msgTrainModel2.isEmpty()) {
            msgOut.put("TRAINMODEL2", msgTrainModel2);
        }

        if (!msgZoomDoor.isEmpty()) {
            msgOut.put("ZOOMDOOR", msgZoomDoor);
        }

        return msgOut;
    }

    private void processDelay() {
        if (isBrakeCutoutAcar1 & !isBrakeRelaseAcar1) {
            if (Calendar.getInstance().getTimeInMillis() - brakeCutOut1Start.getTimeInMillis() > 3000) {
                msgToControlPanel.offer("A243 S1");
                isBrakeRelaseAcar1 = true;
            }
        }

        if (isBrakeCutoutAcar2 & !isBrakeRelaseAcar2) {
            if (Calendar.getInstance().getTimeInMillis() - brakeCutOut2Start.getTimeInMillis() > 3000) {
                msgToControlPanel.offer("A244 S1");
                isBrakeRelaseAcar2 = true;
            }
        }
    }

    private void processMsgIn() {

        if (msgIn != null) {
            Iterator keys = msgIn.keySet().iterator();
            String key;

            while (keys.hasNext()) {
                key = (String) keys.next();

                switch (key.toLowerCase()) {
                    case "cmd":
                        handleCmd();
                        break;

                    case "device":
                        handleDevice((JSONObject) msgIn.get(key));
                        break;

                    case "status":
                        handleStatus((JSONObject) msgIn.get(key));
                        break;

                    case "is_ccar_mcb_tripped":
                        handleCCarBrakerTipped();
                        break;

                    case "is_c1car_mcb_tripped":
                        handleC1CarBrakerTipped();
                        break;

                }
            }

            msgIn.clear();
        }
    }

    private void handleCmd() {

        switch ((String) msgIn.get("cmd")) {
            case "shutdown":
                shutdown();
                break;

            case "startup":
                startup();
                break;

            case "key_off":
                key_off();
                break;

            case "key_on":
                key_on();
                break;

        }
    }

    private void handleDevice(JSONObject cmd) {
        JSONObject dev = (JSONObject) (devs.get(cmd.keySet().iterator().next()));

        try {
            String id = (String) dev.get("mapping");
            int value = (int) (long) cmd.get(dev.get("id"));
            msgToControlPanel.offer(id + " S" + value);
        } catch (NullPointerException ex) {
        }
    }

    private void shutdown() {
        isShutdown = true;
        isFrictionBrakeOn = false;
        msgToControlPanel.offer("A299 S0");		 // A-Car: turn off all lamp
    }

    private void startup() {
        isShutdown = false;
        isFrictionBrakeOn = false;
        msgToControlPanel.offer("A299 S0");		 // A-Car: turn off all lamp
        //msgToControlPanel.offer("A218 S1");		 // Contactors Lamp
        checkBreakers();
        handleLampBreakerTripped();
    }

    private void key_off() {
        // msgToControlPanel.offer("A218 S0");		 // Contactors Lamp
    }

    private void key_on() {
        msgToControlPanel.offer("A218 S0");		 // Contactors Lamp
    }

    private void handleStatus(JSONObject status) {
        Iterator keys = status.keySet().iterator();
        String key;

        while (keys.hasNext()) {
            key = (String) keys.next();

            switch (key.toLowerCase()) {

                case "general_fault":
                    msgToControlPanel.offer("A252 S2");		// A-Car : Buzzer
                    break;

                case "faultacknowledged":
                    handleFaultAcknowledged();
                    break;

                case "parking_brake_released":
                    handleParkingBrakeReleased((boolean) status.get(key));
                    break;

                case "deadman_released":
                    handleDeadmanReleased(((boolean) status.get(key)));
                    break;

                case "lever_position":
                    handleLever((int) status.get(key));
                    break;

                case "speed":
                    handleSpeed((double) status.get(key));
                    break;

                case "over_speed":
                    if ((boolean) status.get(key)) {
                        doOverSpeed();
                    } else {
                        doNormalSpeed();
                    }

                    break;

                case "safety_loop":
                    handleSafetyLoop((boolean) status.get(key));
                    break;

                case "stop":
                    msgToControlPanel.offer("A222 S1");		// Brake ON
                    msgToControlPanel.offer("A223 S0");		// Brake Release
                    msgToControlPanel.offer("A229 S1");		// Brake ON
                    msgToControlPanel.offer("A243 S0");		// Brake Release
                    msgToControlPanel.offer("A244 S0");		// Brake Release
                    break;

                case "valveb09":
                    handleValveB09((JSONObject) status.get(key));
                    break;

                case "propulsion":
                    msgToControlPanel.offer("A201 S1");
                    msgToControlPanel.offer("A207 S1");
                    msgToControlPanel.offer("A252 S2");		// A-Car : Buzzer
                    break;

                case "400powersupply":
                    msgToControlPanel.offer("A202 S1");
                    msgToControlPanel.offer("A204 S1");
                    msgToControlPanel.offer("A209 S1");
                    msgToControlPanel.offer("A252 S2");		// A-Car : Buzzer
                    break;

                case "bcu":
                    if ((int) status.get(key) == 1) {
                        msgToControlPanel.offer("A211 S1");		// A-Car : Lamp - BCU Fault
                    }

                    msgToControlPanel.offer("A205 S1");		// A-Car : Lamp - BCU Fault
                    msgToControlPanel.offer("A252 S2");		// A-Car : Buzzer
                    break;

                case "lowair":
                    msgToControlPanel.offer("A206 S1");
                    msgToControlPanel.offer("A212 S1");
                    msgToControlPanel.offer("A252 S2");		// A-Car : Buzzer
                    break;

                case "brakecontrol":
                    break;

                case "frictionbrake":
                    isFrictionBrakeOn = true;
                    break;

                case "parkingbrake":
                    break;

                case "doorcontrol":
                    msgToControlPanel.offer("A252 S2");		// A-Car : Buzzer

                    int type = (int) status.get(key);

                    switch (type) {
                        case 1:
                            msgToControlPanel.offer("A220 S1");		// A-Car : Lamp - Door Control
                            msgToControlPanel.offer("A214 S1");		// A-Car : Lamp - Door Control
                            break;

                        case 2:
                            msgToControlPanel.offer("A214 S1");		// A-Car : Lamp - Door Control
                            break;

                        case 3:
                            msgToControlPanel.offer("A214 S1");		// A-Car : Lamp - Door Control
                            break;
                    }
                    break;

                case "traincontrol":
                    break;

                case "per":
                    handlePer((boolean) status.get(key));
                    break;

                case "intercomm":
                    handleIntercomm((boolean) status.get(key));
                    break;

                case "koh":
                    handleKOH((boolean) status.get(key));
                    break;
            }
        }
    }

    private void handleKOH(boolean isKOH) {
        if (isKOH) {
            msgToControlPanel.offer("A214 S0");		// A-Car : Lamp - Door control ACar
            msgToControlPanel.offer("A220 S0");		// A-Car : Lamp - Door control 
        }
    }

    private void handlePer(boolean isPer) {
        if (isPer) {
            msgToControlPanel.offer("A214 S1");		// A-Car : Lamp - Door control  ACar
            msgToControlPanel.offer("A220 S1");		// A-Car : Lamp - Door control 
            msgToControlPanel.offer("A252 S2");		// A-Car : Buzzer

        } else {
            msgToControlPanel.offer("A252 S0");		// A-Car : Buzzer
        }
    }

    private void handleIntercomm(boolean isIntercomm) {
        if (isIntercomm) {
            msgToControlPanel.offer("A252 S0");		// A-Car : Buzzer
        }
    }

    private void handleFaultAcknowledged() {
        System.out.println("handleFaultACK");
        msgToControlPanel.offer("A252 S0");		// A-Car : Buzzer
    }

    private void handleSafetyLoop(boolean isSafetyLoop) {
        if (isSafetyLoop) {
            msgToControlPanel.offer("A226 S1");		// Lamp: Safety Loop
        } else {
            msgToControlPanel.offer("A226 S0");		// Lamp: Safety Loop
        }
    }

    private void handleParkingBrakeReleased(boolean isReleased) {
        if (isReleased) {
            msgToControlPanel.offer("A230 S1");
        } else {
            msgToControlPanel.offer("A230 S0");
        }
    }

    private void handleCCarBrakerTipped() {
        isCCarBreakerTripped = (boolean) msgIn.get("is_ccar_mcb_tripped");
        handleLampBreakerTripped();
    }

    private void handleC1CarBrakerTipped() {
        isC1CarBreakerTripped = (boolean) msgIn.get("is_c1car_mcb_tripped");
        handleLampBreakerTripped();
    }

    private void handleLever(int position) {
        switch (position) {
            case 1:		// Lever position "D"
                if (!isFrictionBrakeOn) {
                    msgToControlPanel.offer("A222 S0");		// Brake ON
                    msgToControlPanel.offer("A223 S1");		// Brake Release
                    msgToControlPanel.offer("A229 S0");		// Brake ON
                    msgToControlPanel.offer("A243 S1");		// Brake Release
                    msgToControlPanel.offer("A244 S1");		// Brake Release
                }
                break;

            case 3:		// Lever position "B"
                msgToControlPanel.offer("A222 S1");		// Brake ON
                msgToControlPanel.offer("A223 S0");		// Brake Release
                msgToControlPanel.offer("A229 S1");		// Brake ON
                msgToControlPanel.offer("A243 S0");		// Brake Release
                msgToControlPanel.offer("A244 S0");		// Brake Release
                break;

        }
    }

    private void handleSpeed(double speed) {
        if (speed == 0.0) {
            msgToControlPanel.offer("A222 S1");		// Brake ON
            msgToControlPanel.offer("A223 S0");		// Brake Release
            msgToControlPanel.offer("A229 S1");		// Brake ON
            msgToControlPanel.offer("A243 S0");		// Brake Release
            msgToControlPanel.offer("A244 S0");		// Brake Release
        }
    }

    private void handleValveB09(JSONObject status) {

        if (((int) status.get("status")) == 1) {		// if one of valve B09 on

            switch ((String) status.get("car")) {

                case "a-car":
                    // check it is from a-car
                    if (((int) status.get("no")) == 1) {
                        isBrakeCutoutAcar1 = true;
                        brakeCutOut1Start = Calendar.getInstance();

                    } else {
                        isBrakeCutoutAcar2 = true;
                        brakeCutOut2Start = Calendar.getInstance();
                    }
                    //msgTrainModel1.put("red_lamp_on", 1);
                    break;

                case "c-car":
                    // check it is from c-car
                    if (((int) status.get("no")) == 1) {
                        isBrakeCutoutCcar1 = true;
                    } else {
                        isBrakeCutoutCcar2 = true;
                    }
                    //msgTrainModel1.put("red_lamp_on", 2);
                    break;

                case "c1-car":
                    // check it is from c1-car
                    if (((int) status.get("no")) == 1) {
                        isBrakeCutoutC1car1 = true;
                    } else {
                        isBrakeCutoutC1car2 = true;
                    }
                    break;

                case "a-car_unmaned":
                    if (((int) status.get("no")) == 1) {
                        isBrakeCutoutAcarUnmaned1 = true;
                    } else {
                        isBrakeCutoutAcarUnmaned2 = true;
                    }
                    //msgTrainModel1.put("red_lamp_on", 3);
                    break;

                default:
                    break;
            }

        } else {		// if one of valve B09 off

            switch ((String) status.get("car")) {

                case "a-car":
                    if (((int) status.get("no")) == 1) {
                        isBrakeCutoutAcar1 = false;
                    } else {
                        isBrakeCutoutAcar2 = false;
                    }
                    break;

                case "c-car":
                    if (((int) status.get("no")) == 1) {
                        isBrakeCutoutCcar1 = false;
                    } else {
                        isBrakeCutoutCcar2 = false;
                    }
                    break;

                case "c1-car":
                    if (((int) status.get("no")) == 1) {
                        isBrakeCutoutC1car1 = false;
                    } else {
                        isBrakeCutoutC1car2 = false;
                    }
                    break;

                case "a-car_unmaned":
                    if (((int) status.get("no")) == 1) {
                        isBrakeCutoutAcarUnmaned1 = false;
                    } else {
                        isBrakeCutoutAcarUnmaned2 = false;
                    }
                    break;

                default:
                    break;
            }
        }

        handleBrakeCutOutLamp();
    }

    private void handleBrakeCutOutLamp() {
        if (isBrakeCutoutAcar1
                | isBrakeCutoutAcar2
                | isBrakeCutoutCcar1
                | isBrakeCutoutCcar2
                | isBrakeCutoutC1car1
                | isBrakeCutoutC1car2
                | isBrakeCutoutAcarUnmaned1
                | isBrakeCutoutAcarUnmaned2) {

            msgToControlPanel.offer("A224 S1");		// Lamp Brake Cut-Out
            devs.get("A325").put("status", 1);		// Lamp Brake Cut-Out (new code)

        } else {
            msgToControlPanel.offer("A224 S0");		// Lamp Brake Cut-Out
            devs.get("A325").put("status", 0);		// Lamp Brake Cut-Out (new code)
        }

        if (isBrakeCutoutAcar1) {
            msgToControlPanel.offer("A231 S1");		// Lamp Brake Cut-Out (A-Car)
            devs.get("A332").put("status", 1);		// Lamp Brake Cut-Out (new code)
        } else {
            msgToControlPanel.offer("A231 S0");		// Lamp Brake Cut-Out (A-Car)
            devs.get("A332").put("status", 0);		// Lamp Brake Cut-Out (new code)
        }

        if (isBrakeCutoutAcar2) {
            msgToControlPanel.offer("A232 S1");		// Lamp Brake Cut-Out (A-Car)
            devs.get("A333").put("status", 1);		// Lamp Brake Cut-Out (new code)
        } else {
            msgToControlPanel.offer("A232 S0");		// Lamp Brake Cut-Out (A-Car)
            devs.get("A333").put("status", 0);		// Lamp Brake Cut-Out (new code)
        }
    }

    private void handleLampBreakerTripped() {

        if (isACarBreakerTripped) {
            msgToControlPanel.offer("A245 S1");	 // A-Car: MCB TRIPPED Lamp
        } else {
            msgToControlPanel.offer("A245 S0");	 // A-Car: MCB TRIPPED Lamp
        }

        if (isACarBreakerTripped | isCCarBreakerTripped | isC1CarBreakerTripped) {
            isBreakersTripped = true;
            msgToControlPanel.offer("A240 S1");	 // ALL: MCB TRIPPED Lamp

        } else {
            isBreakersTripped = false;
            msgToControlPanel.offer("A240 S0");	 // ALL: MCB TRIPPED Lamp
        }

        handleBuzzer();
    }

    private void doNormalSpeed() {
        msgToControlPanel.offer("A253 S0");		// A-Car : Buzzer Over Speed
    }

    private void doOverSpeed() {
        msgToControlPanel.offer("A253 S1");		// A-Car : Buzzer Over Speed
    }

    private void handleBuzzer() {
        JSONObject status = new JSONObject();

        if (isBreakersTripped | isFail) {
            msgToControlPanel.offer("A252 S2");		// A-Car : Buzzer
            //status.put("isfault", true);
            msgDriverDesk.put("isfault", true);

        } else {
            msgToControlPanel.offer("A252 S0");		// A-Car : Buzzer
            //status.put("isfault", false);
            msgDriverDesk.put("isfault", false);
        }
    }

    private void handleDeadmanReleased(boolean isRelease) {
        if (isRelease) {
            msgToControlPanel.offer("A252 S2");		// A-Car : Buzzer
        } else {
            msgToControlPanel.offer("A252 S0");		// A-Car : Buzzer
        }
    }

    private void checkBreakers() {
        Iterator keys = devs.keySet().iterator();
        String key;
        JSONObject dev;

        isACarBreakerTripped = false;

        while (keys.hasNext()) {
            key = (String) keys.next();
            dev = devs.get(key);

            if (dev.get("type").equals("Breaker") & dev.get("status").equals("0")) {
                isACarBreakerTripped = true;
                break;
            }
        }
    }

//********************************************************************************
// BREAKER
//********************************************************************************
    private void handleBreaker(String id) {
        checkBreakers();
        handleLampBreakerTripped();

        JSONObject dev = devs.get(id);

        switch (id) {
            case "A101":
                doA101(dev);
                break;

            case "A102":
                doA102(dev);
                break;

            case "A103":
                doA103(dev);
                break;

            case "A104":
                doA104(dev);
                break;

            case "A105":
                doA105(dev);
                break;

            case "A106":
                doA106(dev);
                break;

            case "A107":
                doA107(dev);
                break;

            case "A108":
                doA108(dev);
                break;

            case "A109":
                doA109(dev);
                break;

            case "A110":
                doA110(dev);
                break;

            case "A111":
                doA111(dev);
                break;

            case "A112":
                doA112(dev);
                break;

            case "A113":
                doA113(dev);
                break;

            case "A114":			//Breaker : Auxiliaries Control
                doA114(dev);
                break;

            case "A115":
                doA115(dev);
                break;

            case "A116":
                doA116(dev);
                break;

            case "A117":
                doA117(dev);
                break;

            case "A118":
                doA118(dev);
                break;

            case "A119":
                doA119(dev);
                break;

            case "A120":
                doA120(dev);
                break;

            case "A121":
                doA121(dev);
                break;

            case "A122":
                doA122(dev);
                break;

            case "A123":
                doA123(dev);
                break;

            case "A124":
                doA124(dev);
                break;

            case "A125":
                doA125(dev);
                break;

            case "A126":
                doA126(dev);
                break;

            case "A127":
                doA127(dev);
                break;

            case "A128":
                doA128(dev);
                break;

            case "A129":
                doA119(dev);
                break;

            case "A130":
                doA130(dev);
                break;

            case "A131":
                doA131(dev);
                break;

            case "A132":
                doA132(dev);
                break;

            case "A133":
                doA133(dev);
                break;
        }
    }

    private void syncWithInstructor(JSONObject dev) {
        JSONObject json = new JSONObject();
        json.put((String) dev.get("id"), Integer.parseInt((String) dev.get("status")));
        msgInstructor.put("ACAR", json);
    }

    private void doA101(JSONObject dev) {		//Breaker : Main Breaker
        now = Calendar.getInstance();
        System.out.println(sdf.format(now.getTime()) + " Main Breaker : Tripped");

        if (dev.get("status").equals("0")) {
            msgATC.put("cmd", "shutdown");

        } else {
            msgATC.put("cmd", "startup");
        }
    }

    private void doA102(JSONObject dev) {		//Breaker : Current Collector
        now = Calendar.getInstance();
        System.out.println(sdf.format(now.getTime()) + " Current Collector : Tripped");
    }

    private void doA103(JSONObject dev) {		//Breaker : Train Control 1
        now = Calendar.getInstance();
        System.out.println(sdf.format(now.getTime()) + " Train Control 1 : Tripped");

        JSONObject status = new JSONObject();

        if (dev.get("status").equals("0")) {
            status.put("TCU1", 0);
            msgATC.put("status", status);

        } else {
            status.put("TCU1", 1);
            msgATC.put("status", status);
        }
    }

    private void doA104(JSONObject dev) {		//Breaker : Train Control 2
        now = Calendar.getInstance();
        System.out.println(sdf.format(now.getTime()) + " Train Control 2 : Tripped");
    }

    private void doA105(JSONObject dev) {		//Breaker : 110 V TRAC Container PROP. BLOWER 
        now = Calendar.getInstance();
        System.out.println(sdf.format(now.getTime()) + " 110 V TRAC Container PROP. BLOWER : Tripped");
    }

    private void doA106(JSONObject dev) {		//Breaker : BCU+BCU-P
        now = Calendar.getInstance();
        System.out.println(sdf.format(now.getTime()) + " BCU+BCU-P : Tripped");

        JSONObject status = new JSONObject();

        if (dev.get("status").equals("0")) {
            status.put("BCU", 0);
            msgATC.put("status", status);

        } else {
            status.put("BCU", 1);
            msgATC.put("status", status);
        }
    }

    private void doA107(JSONObject dev) {		//Breaker : Parking Brake Value
        now = Calendar.getInstance();
        System.out.println(sdf.format(now.getTime()) + " Parking Brake Value : Tripped");
    }

    private void doA108(JSONObject dev) {		//Breaker : Parking Brake Control
        now = Calendar.getInstance();
        System.out.println(sdf.format(now.getTime()) + " Parking Brake Control : Tripped");
    }

    private void doA109(JSONObject dev) {		//Breaker : PNEU Brake Cut Out
        now = Calendar.getInstance();
        System.out.println(sdf.format(now.getTime()) + " PNEU Brake Cut Out : Tripped");
    }

    private void doA110(JSONObject dev) {		//Breaker : ATC Display
        now = Calendar.getInstance();
        System.out.println(sdf.format(now.getTime()) + " ATC Display : Tripped");
    }

    private void doA111(JSONObject dev) {		//Breaker : Indication Lights
        now = Calendar.getInstance();
        System.out.println(sdf.format(now.getTime()) + " Indication Lights : Tripped");
    }

    private void doA112(JSONObject dev) {		//Breaker : Safety Loop
        now = Calendar.getInstance();
        System.out.println(sdf.format(now.getTime()) + " Safety Loop : Tripped");
    }

    private void doA113(JSONObject dev) {		//Breaker : Communication
        now = Calendar.getInstance();
        System.out.println(sdf.format(now.getTime()) + " Communication : Tripped");
    }

    private void doA114(JSONObject dev) {		//Breaker : Auxiliaries Control
        now = Calendar.getInstance();
        System.out.println(sdf.format(now.getTime()) + " Auxiliaries Control : Tripped");

        if (dev.get("status").equals("0")) {
            msgATC.put("cmd", "shutdown");

        } else {
            msgATC.put("cmd", "startup");
        }
    }

    private void doA115(JSONObject dev) {		//Breaker : Instruments 24 V
        now = Calendar.getInstance();
        System.out.println(sdf.format(now.getTime()) + " Instruments 24 V : Tripped");
    }

    private void doA116(JSONObject dev) {		//Breaker : Socket AC 230V
        now = Calendar.getInstance();
        System.out.println(sdf.format(now.getTime()) + " Socket AC 230V : Tripped");
    }

    private void doA117(JSONObject dev) {		//Breaker : CAB Blower
        now = Calendar.getInstance();
        System.out.println(sdf.format(now.getTime()) + " CAB Blower : Tripped");
    }

    private void doA118(JSONObject dev) {		//Breaker : PIS
        now = Calendar.getInstance();
        System.out.println(sdf.format(now.getTime()) + " PIS : Tripped");
    }

    private void doA119(JSONObject dev) {		//Breaker : Head Light
        now = Calendar.getInstance();
        System.out.println(sdf.format(now.getTime()) + " Head Light : Tripped");
    }

    private void doA120(JSONObject dev) {		//Breaker : Lighting Control
        now = Calendar.getInstance();
        System.out.println(sdf.format(now.getTime()) + " Lighting Control : Tripped");
    }

    private void doA121(JSONObject dev) {		//Breaker : Light
        now = Calendar.getInstance();
        System.out.println(sdf.format(now.getTime()) + " Light : Tripped");
    }

    private void doA122(JSONObject dev) {		//Breaker : Emergency Light
        now = Calendar.getInstance();
        System.out.println(sdf.format(now.getTime()) + " Emergency Light : Tripped");
    }

    private void doA123(JSONObject dev) {		//Breaker : Air COND. Control 1
        now = Calendar.getInstance();
        System.out.println(sdf.format(now.getTime()) + " Air COND. Control 1 : Tripped");
    }

    private void doA124(JSONObject dev) {		//Breaker : Air COND. Control 2
        now = Calendar.getInstance();
        System.out.println(sdf.format(now.getTime()) + " Air COND. Control 2 : Tripped");
    }

    private void doA125(JSONObject dev) {		//Breaker : Coupling Control
        now = Calendar.getInstance();
        System.out.println(sdf.format(now.getTime()) + " Coupling Control : Tripped");
    }

    private void doA126(JSONObject dev) {		//Breaker : PASS. Infomation
        now = Calendar.getInstance();
        System.out.println(sdf.format(now.getTime()) + " PASS. Infomation : Tripped");
    }

    private void doA127(JSONObject dev) {		//Breaker : Door Control 1
        now = Calendar.getInstance();
        System.out.println(sdf.format(now.getTime()) + " Door Control 1 : Tripped");
    }

    private void doA128(JSONObject dev) {		//Breaker : Door Supply 1
        now = Calendar.getInstance();
        System.out.println(sdf.format(now.getTime()) + " Door Supply 1 : Tripped");
    }

    private void doA129(JSONObject dev) {		//Breaker : Door Supply 2
        now = Calendar.getInstance();
        System.out.println(sdf.format(now.getTime()) + " Door Supply 2 : Tripped");
    }

    private void doA130(JSONObject dev) {		//Breaker : Door Control 2
        now = Calendar.getInstance();
        System.out.println(sdf.format(now.getTime()) + " Door Control 2 : Tripped");
    }

    private void doA131(JSONObject dev) {		//Breaker : Tail Light
        now = Calendar.getInstance();
        System.out.println(sdf.format(now.getTime()) + " Tail Light : Tripped");
    }

    private void doA132(JSONObject dev) {		//Breaker : Washer/Wiper Horn
        now = Calendar.getInstance();
        System.out.println(sdf.format(now.getTime()) + " Washer/Wiper Horn : Tripped");
    }

    private void doA133(JSONObject dev) {		//Breaker : Instruments 110 V
        now = Calendar.getInstance();
        System.out.println(sdf.format(now.getTime()) + " Instruments 110 V : Tripped");
    }

//********************************************************************************
// SWITCH
//********************************************************************************
    private void handleSwitchs(String id) {
        JSONObject dev = devs.get(id);
        printDev(id);

        switch (id) {
            case "A201":
                doA201();
                break;

            case "A202":
                doA202();
                break;

            case "A203":
                doA203();
                break;

            case "A204":
                doA204();
                break;

            case "A205":
                doA205();
                break;

            case "A206":
                doA206();
                break;

            case "A207":
                doA207();
                break;

            case "A208":
                doA208();
                break;

            case "A209_ON":
                doA209();
                break;

            case "A209_OFF":
                doA209();
                break;

            case "A210_ON":
                doA210();		// AIR RECIRCULATION Rotary
                break;

            case "A210_OFF":
                doA210();		// AIR RECIRCULATION Rotary
                break;

            case "A211_ON":
                doA211();
                break;

            case "A211_OFF":
                doA211();
                break;

            case "A212":
                doA212();
                break;

            case "A213_OFF":		// Auxiliaries Switch
                doA213();
                break;

            case "A213_ON":			// Auxiliaries Switch
                doA213();
                break;

            case "A214":
                doA214();
                break;

            case "A215":
                doA215();
                break;

            case "A216":
                doA216();
                break;

            case "A217":
                doA217();
                break;

        }

    }

    private void doA201() {		// Door Release Bypass
        printDev("A201");

        String isDoorPermissive = (String) devs.get("A201").get("status");
        JSONObject status = new JSONObject();

        if (isDoorPermissive.equals("1")) {
            status.put("doorbypass", true);
        } else {
            status.put("doorbypass", false);
        }

        msgATC.put("status", status);
    }

    private void doA202() {		// Door Loop Bypass

    }

    private void doA203() {		// ATC Fault Switch Bypass
        printDev("A203");

        JSONObject status = new JSONObject();

        if (((String) devs.get("A203").get("status")).equals("1")) {
            isATCByPass = true;
            status.put("atcbypass", true);
            msgATC.put("status", status);

        } else {
            isATCByPass = false;
            status.put("atcbypass", false);
            msgATC.put("status", status);
        }
    }

    private void doA204() {		// Safety Loop Bypass
        JSONObject atcStatus = new JSONObject();

        if (((String) devs.get("A204").get("status")).equals("1")) {
            isATCByPass = true;
            atcStatus.put("atcbypass", true);
            msgATC.put("status", atcStatus);

        } else {
            isATCByPass = false;
            atcStatus.put("atcbypass", false);
            msgATC.put("status", atcStatus);
        }

    }

    private void doA205() {		// PA Master

    }

    private void doA206() {		// Full Traction Effort

    }

    private void doA207() {		// ATO Door Opening Yes-No
        printDev("A207");
        syncWithInstructor((JSONObject) devs.get("A207"));
    }

    private void doA208() {		// Lamp Test

    }

    private void doA209() {		// Air Conditioning Rotary
        printDev("A209");

        if (((String) devs.get("A209_OFF").get("status")).equals("1")) {
            devs.get("A209").put("status", "0");
            msgToControlPanel.offer("A251 S0");

        } else if (((String) devs.get("A209_ON").get("status")).equals("1")) {
            devs.get("A209").put("status", "1");
            msgToControlPanel.offer("A251 S1");

        } else {
            devs.get("A209").put("status", "2");
        }

        syncWithInstructor((JSONObject) devs.get("A209"));
    }

    private void doA210() {		// Air Recirulation Rotary
        printDev("A210");

        if (((String) devs.get("A210_OFF").get("status")).equals("1")) {
            devs.get("A210").put("status", "0");
            msgToControlPanel.offer("A249 S0");

        } else if (((String) devs.get("A210_ON").get("status")).equals("1")) {
            devs.get("A210").put("status", "1");
            msgToControlPanel.offer("A249 S1");

        } else {
            devs.get("A210").put("status", "2");
        }

        syncWithInstructor((JSONObject) devs.get("A218"));
    }

    private void doA211() {		// Current Collector Shoes Rotary
        printDev("A211");

        if (((String) devs.get("A211_OFF").get("status")).equals("1")) {
            devs.get("A211").put("status", "0");
        } else if (((String) devs.get("A211_ON").get("status")).equals("1")) {
            devs.get("A211").put("status", "1");
        } else {
            devs.get("A211").put("status", "2");
        }

        syncWithInstructor((JSONObject) devs.get("A211"));
    }

    private void doA212() {		// Clear Line

    }

    private void doA213() {		// Auxiliaries Switch
        now = Calendar.getInstance();
        System.out.println(sdf.format(now.getTime()) + " Auxiliaries Switch : Activated");

        if (devs.get("A213_OFF").get("status").equals("1")) {
            //msgATC.put("cmd", "key_off");
            startup();
            msgToControlPanel.offer("A250 S0");	 // Auxiliaries Lamp
            msgDriverDesk.put("isAuxiliariesOn", false);
            msgDMI.put("cmd", "turn_off");
            msgToControlPanel.offer("A218 S0");		 // Contactors Lamp

        } else if (devs.get("A213_ON").get("status").equals("1")) {
            msgToControlPanel.offer("A218 S1");		 // Contactors Lamp
            msgATC.put("cmd", "key_off");
            msgToControlPanel.offer("A250 S1");	 // Auxiliaries Lamp
            msgTrainModel1.put("turnon", 1);
            msgTrainModel2.put("turnon", 1);
            msgZoomDoor.put("door", "close");
        }

        syncWithInstructor((JSONObject) devs.get("A213"));
    }

    private void doA214() {		// AUX. CONV. Help Start
        printDev("A215");
        syncWithInstructor((JSONObject) devs.get("A215"));
    }

    private void doA215() {		// Spare

    }

    private void doA216() {		// Spare

    }

    private void doA217() {		// Spare

    }

}
