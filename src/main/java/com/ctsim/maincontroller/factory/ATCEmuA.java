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
import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author Patipat Punboonrat
 */
public class ATCEmuA implements ATC {

    public static final int MODE_IDLE = 0;
    public static final int MODE_YARD_SR = 1;
    public static final int MODE_AUTO = 4;
    public static final int MODE_YARD_EOA = 2;
    public static final int MODE_RV = 7;
    public static final int MODE_LINE_SR = 6;
    public static final int MODE_MCS = 3;
    public static final int MODE_RM2 = 9;
    public static final int MODE_WM = 8;
    public static final int MODE_ATB = 5;

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
    protected final JSONObject msgTrainModel1 = new JSONObject();
    protected final JSONObject msgTrainModel2 = new JSONObject();
    protected final JSONObject msgDummyBogie = new JSONObject();
    protected final JSONObject msgZoomDoor = new JSONObject();

    private final JSONObject dmiStatus = new JSONObject();

    private int mode;
    private boolean isOverSpeedAlarm = false;
    private boolean isDeadManAlarm = false;
    private boolean isDeadManNotify = false;
    private final Calendar now;
    private boolean isATCBypass = false;

    //Door variables
    private boolean isSafetyLoop = false;

    //Brake variables
    private boolean isBrake;
    private int bcuStatus;
    private boolean isParkingBrakeReleased = false;
    private boolean isNonATPBrake = false;
    private boolean isATPBrake = false;
    private boolean isServiceBrakeActive = false;
    private boolean isEmergencyBrakeActive = false;
    private boolean isEmergencyBrakeShow = false;
    private boolean isEmergencyBrakeWaitForClear = false;
    private Calendar overSpeedTime;

    //Lever variables
    private double leverValue = 0;
    private String leverPosition = "0";
    private boolean isLeverPressed = false;

    //Video variables
    private double trainPosition = 0.0;
    private double trainSpeedScreen = 0.0;
    private int ceilingSpeed = 0, oldCeilingSpeed = 0;
    private double targetSpeed;
    private double targetDistance;

    //Train variables
    private final int TRAIN_WEIGHT = 32000;
    private final int MAX_FORCE = 200000;
    private final double EB_FORCE = MAX_FORCE * 0.1;
    private double motorForce;
    private double trainSpeed, oldTrainSpeed = -1;
    private long t1, t2;
    private Calendar deadManTime;
    private int baliseNo = 0;
    private boolean isDoorByPass = false;
    private final boolean isDoorPermissive = true;
    private boolean isATOStart = false;
    private boolean isATB = false;
    private final boolean isSignal = false;

    //Buzzer variables
    private String cmdBuzzer;
    private final String oldCmdBuzzer = "OFF";

    public ATCEmuA() {
        now = Calendar.getInstance();
    }

    @Override
    public JSONObject process() {
        clearMsgOut();	   // clear all out going message variables before they are used.  
        processMsgIn();
        operate();
        processMsgOut();

        return msgOut;
    }

    @Override
    public void setMode(int mode) {
        this.mode = mode;
    }

    @Override
    public int getMode() {
        return this.mode;
    }

    private void clearMsgOut() {
        msgAcar.clear();
        msgCcar.clear();
        msgC1car.clear();
        msgDriverDesk.clear();
        msgDMI.clear();
        msgVideo.clear();
        msgLever.clear();
        msgTrainModel1.clear();
        msgTrainModel2.clear();
        msgDummyBogie.clear();
        msgZoomDoor.clear();
        msgOut.clear();

        dmiStatus.clear();
    }

    private void operate() {
        if (!isATOStart) {
            handleSpeed();
            handleLamps();
        }
    }

    private void handleSpeed() {
        JSONObject statusDMI = new JSONObject();
        JSONObject statusAcar = new JSONObject();

        t2 = Calendar.getInstance().getTimeInMillis();

        if (t2 - t1 > 250) {			// calculate every 250 ms.  
            motorForce = calMotorForce(leverPosition, leverValue);
            trainSpeed = calSpeedValue(motorForce, trainSpeed, t2 - t1);
            //sendSpeedValue();

            if (mode == TrainEmuA.MODE_RM2) {
                if (trainSpeed > 35) {
                    trainSpeed = 35;
                }

            } else if (mode == TrainEmuA.MODE_WM | mode == TrainEmuA.MODE_RV) {
                if (trainSpeed > 3) {
                    trainSpeed = 3;
                }
            }

            // Case Deadman Release
            if (!isLeverPressed & trainSpeed > 0 & !isEmergencyBrakeActive) {

                if (!isDeadManAlarm & !isDeadManNotify) {
                    statusAcar.put("deadman_released", true);
                    deadManTime = Calendar.getInstance();
                    isDeadManNotify = true;
                    isDeadManAlarm = true;
                }

                if (Calendar.getInstance().getTimeInMillis() - deadManTime.getTimeInMillis() > 3000) {
                    statusAcar.put("deadman_released", false);
                    statusAcar.put("safety_loop", true);
                    statusDMI.put("non_atp_brake", true);
                    isSafetyLoop = true;
                    isNonATPBrake = true;
                    isEmergencyBrakeActive = true;
                }

            } else if (isDeadManAlarm & !isEmergencyBrakeActive) {
                statusAcar.put("deadman_released", false);
                isDeadManAlarm = false;
                isDeadManNotify = false;
            }

            // Case Over Speed
            if (trainSpeed > ceilingSpeed + 1 & !isOverSpeedAlarm) {
                statusAcar.put("over_speed", true);
                statusDMI.put("atp_brake", 2);
                isOverSpeedAlarm = true;
                overSpeedTime = Calendar.getInstance();

            } else if (trainSpeed > ceilingSpeed + 3 & ceilingSpeed > 0) {
                isATPBrake = true;
                isEmergencyBrakeActive = true;
                isEmergencyBrakeWaitForClear = true;
                statusDMI.put("atp_brake", 1);
                statusDMI.put("message", "Over Speed");
            }

            if (isOverSpeedAlarm & !isServiceBrakeActive) {
                if (Calendar.getInstance().getTimeInMillis() - overSpeedTime.getTimeInMillis() > 3000) {
                    isServiceBrakeActive = true;
                }
            }

            if (isEmergencyBrakeActive & !isEmergencyBrakeShow) {
                msgDriverDesk.put("isEmergencyBrake", true);
                isEmergencyBrakeShow = true;
            }

            if (trainSpeed == 0) {

                if (isNonATPBrake & !isEmergencyBrakeActive & leverPosition.equals("0")) {
                    statusAcar.put("safety_loop", false);
                    statusDMI.put("non_atp_brake", false);
                    isSafetyLoop = false;
                    isNonATPBrake = false;
                    isEmergencyBrakeActive = false;
                    isServiceBrakeActive = false;
                }

                if (isATPBrake & isEmergencyBrakeWaitForClear & leverPosition.equals("0")) {
                    statusAcar.put("over_speed", false);
                    isOverSpeedAlarm = false;
                    statusDMI.put("atp_brake", 2);
                    isEmergencyBrakeWaitForClear = false;
                }

                if (!isBrake) {
                    statusAcar.put("stop", true);

                    if (!isEmergencyBrakeActive) {
                        msgDriverDesk.put("isbrake", true);
                    }

                    isBrake = true;
                }
            }

            if (trainSpeed != oldTrainSpeed) {
                statusDMI.put("speed", trainSpeed);
                msgDriverDesk.put("speed", trainSpeed);
                msgTrainModel1.put("speed", trainSpeed);
                msgDummyBogie.put("speed", trainSpeed);
                msgVideo.put("speed", trainSpeed);
                msgLever.put("speed", trainSpeed);		// ใช้ตรวจสอบว่ามีการปล่อย Dead-man ในขณะที่รถกำลังเคลื่อนที่อยู่หรือไม่

                oldTrainSpeed = trainSpeed;
            }

            if (!statusDMI.isEmpty()) {
                msgDMI.put("status", statusDMI);
            }

            if (!statusAcar.isEmpty()) {
                msgAcar.put("status", statusAcar);
            }

            t1 = t2;
        }
    }

    private void handleLamps() {
        if (isSafetyLoop) {

        }
    }

    private double calMotorForce(String position, double value) {		// calculate force and save to "motorForce" 
        double force;

        if (isEmergencyBrakeActive) {
            return motorForce - EB_FORCE;

        } else {
            switch (position) {
                case "D":
                    if (!isSafetyLoop) {
                        force = MAX_FORCE * value / 100;
                    } else {
                        force = 0;
                    }

                    break;

                case "B":
                    force = MAX_FORCE * value / -100;
                    break;

                case "EB":
                    isEmergencyBrakeActive = true;
                    force = MAX_FORCE
                            * value / -100;
                    break;

                default:
                    force = 0;
                    break;
            }

            if (isServiceBrakeActive) {
                return force - MAX_FORCE * 0.6;
            }

            if (!isParkingBrakeReleased) {
                return force - MAX_FORCE * 0.9;
            }

            return force;
        }
    }

    private double calSpeedValue(double force, double currentSpeed, long diffTime) {		// calculate speed and save to "trainSpeed" 
        double speed;
        speed = currentSpeed + ((force - getFriction(currentSpeed)) * diffTime) / (1000 * TRAIN_WEIGHT);		// v = at1 + at2

        if (speed > 77) {
            speed = 77.0;

        } else if (speed < 0) {
            speed = 0.0;
        }

        return speed;
    }

    private double getFriction(double speed) {
        if (isEmergencyBrakeActive) {
            return (speed + 100) / 100 * (MAX_FORCE);
        }

        return (speed + 100) / 100 * (0.02 * MAX_FORCE);
    }

    private void processMsgOut() {
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

        if (!msgVideo.isEmpty()) {
            msgOut.put("VIDEO", msgVideo);
        }

        if (!dmiStatus.isEmpty()) {
            msgDMI.put("status", dmiStatus);
        }

        if (!msgDMI.isEmpty()) {
            msgOut.put("DMI", msgDMI);
        }

        if (!msgTrainModel1.isEmpty()) {
            msgOut.put("TRAINMODEL1", msgTrainModel1);
        }

        if (!msgTrainModel2.isEmpty()) {
            msgOut.put("TRAINMODEL2", msgTrainModel2);
        }

        if (!msgDummyBogie.isEmpty()) {
            msgOut.put("DUMMYBOGIE", msgDummyBogie);
        }

        if (!msgZoomDoor.isEmpty()) {
            msgOut.put("ZOOMDOOR", msgZoomDoor);
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

                switch (key.toUpperCase()) {

                    case "FAULT":
                        handleFault((JSONObject) msgIn.get(key));
                        break;

                    case "CMD":
                        handleCMD((String) msgIn.get(key));
                        break;

                    case "STATUS":
                        handleStatus((JSONObject) msgIn.get(key));
                        break;
                }
            }

            msgIn.clear();
        }
    }

    private void handleFault(JSONObject faultName) {
        JSONObject statusAcar = new JSONObject();
        JSONObject statusCcar = new JSONObject();
        JSONObject statusC1car = new JSONObject();
        JSONObject statusDMI = new JSONObject();
        String errMsg = "";

        Iterator keys = faultName.keySet().iterator();
        String key;
        Collection<String> cars = new ArrayList<>();

        while (keys.hasNext()) {
            key = (String) keys.next();

            switch (key.toUpperCase()) {
                case "EMERGENCYBRAKEERROR":
                    errMsg = "Emergency brake Error";
                    isATPBrake = true;
                    isSafetyLoop = true;
                    isEmergencyBrakeActive = true;

                    statusAcar.put("safety_loop", true);
                    statusAcar.put("general_fault", true);
                    msgAcar.put("status", statusAcar);
                    msgDriverDesk.put("isfault", true);
                    statusDMI.put("atp_brake", 1);
                    statusDMI.put("atc_mode", 2);
                    break;

                case "VDX":
                    errMsg = "VDX error";
                    isATPBrake = true;
                    isSafetyLoop = true;
                    isEmergencyBrakeActive = true;

                    statusAcar.put("safety_loop", true);
                    statusAcar.put("general_fault", true);
                    msgAcar.put("status", statusAcar);
                    msgDriverDesk.put("isfault", true);

                    statusDMI.put("atp_brake", 1);
                    statusDMI.put("atc_mode", 2);
                    break;

                case "ATP":
                    errMsg = "ATP Software Error";
                    isATPBrake = true;
                    isSafetyLoop = true;
                    isEmergencyBrakeActive = true;

                    statusAcar.put("safety_loop", true);
                    statusAcar.put("general_fault", true);
                    msgAcar.put("status", statusAcar);
                    msgDriverDesk.put("isfault", true);

                    statusDMI.put("atp_brake", 1);
                    statusDMI.put("atc_mode", 2);
                    break;

                case "RADIO":
                    dmiStatus.put("atenna_status", 3);
                    isATPBrake = true;
                    isSafetyLoop = true;
                    isEmergencyBrakeActive = true;

                    statusAcar.put("safety_loop", true);
                    statusAcar.put("general_fault", true);
                    msgAcar.put("status", statusAcar);
                    msgDriverDesk.put("isfault", true);

                    statusDMI.put("atp_brake", 1);
                    statusDMI.put("atc_mode", 2);
                    break;

                case "OVERSPEED":
                    errMsg = "Over Speed 1km/hr.";
                    isATPBrake = true;
                    isSafetyLoop = true;
                    isEmergencyBrakeActive = true;

                    statusAcar.put("safety_loop", true);
                    statusAcar.put("general_fault", true);
                    msgAcar.put("status", statusAcar);
                    msgDriverDesk.put("isfault", true);

                    statusDMI.put("atp_brake", 1);
                    statusDMI.put("atc_mode", 2);
                    break;

                case "BALISE":
                    errMsg = "Balise Error";
                    isATPBrake = true;
                    isSafetyLoop = true;
                    isEmergencyBrakeActive = true;

                    statusAcar.put("safety_loop", true);
                    statusAcar.put("general_fault", true);
                    msgAcar.put("status", statusAcar);
                    msgDriverDesk.put("isfault", true);

                    statusDMI.put("atp_brake", 1);
                    statusDMI.put("atc_mode", 2);
                    break;

                case "TELEGRAMLOSS":
                    errMsg = "Telegram lost";
                    isATPBrake = true;
                    isSafetyLoop = true;
                    isEmergencyBrakeActive = true;

                    statusAcar.put("safety_loop", true);
                    statusAcar.put("general_fault", true);
                    msgAcar.put("status", statusAcar);
                    msgDriverDesk.put("isfault", true);

                    statusDMI.put("atp_brake", 1);
                    statusDMI.put("atc_mode", 2);
                    break;

//------------------------------------------------------------------------------
                case "PROPULSION":
                    statusAcar.put("propulsion", 1);
                    msgAcar.put("status", statusAcar);
                    msgDriverDesk.put("isfault", true);
                    break;

                case "400POWERSUPPLY":
                    cars = (Collection) faultName.get(key);
                    statusAcar.put("400powersupply", 1);
                    msgAcar.put("status", statusAcar);
                    msgDriverDesk.put("isfault", true);

                    cars.stream().forEach((car) -> {
                        switch (car) {
                            case "acar":
                                msgTrainModel1.put("red_lamp_on", 1);
                                break;

                            case "ccar":
                                msgTrainModel1.put("red_lamp_on", 2);
                                statusCcar.put("aux", true);
                                msgCcar.put("status", statusCcar);
                                break;

                            case "c1car":
                                msgTrainModel2.put("red_lamp_on", 2);
                                statusC1car.put("aux", true);
                                msgC1car.put("status", statusC1car);
                                break;
                        }
                    });

                    break;

                case "LOWAIR":
                    statusAcar.put("lowair", 1);

                    msgAcar.put("status", statusAcar);
                    statusCcar.put("lowair", 1);

                    msgCcar.put("status", statusCcar);
                    msgDriverDesk.put("isfault", true);
                    break;

                case "BCU":
                    isEmergencyBrakeActive = true;
                    cars = (Collection) faultName.get(key);
                    cars.stream().forEach((car) -> {
                        switch (car) {
                            case "acar":
                                statusAcar.put("bcu", 1);
                                msgTrainModel1.put("red_lamp_on", 1);
                                break;

                            case "ccar":
                                statusAcar.put("bcu", 2);
                                statusCcar.put("bcu", true);
                                msgCcar.put("status", statusCcar);
                                msgTrainModel1.put("red_lamp_on", 2);
                                break;

                            case "c1car":
                                statusAcar.put("bcu", 3);
                                statusC1car.put("bcu", true);
                                msgC1car.put("status", statusC1car);
                                msgTrainModel2.put("red_lamp_on", 2);
                                break;
                        }
                    });

                    msgAcar.put("status", statusAcar);
                    msgDriverDesk.put("isfault", true);
                    break;

                case "PARKINGNOTRELEASE":
                    msgDriverDesk.put("isparkingbrake", true);
                    break;

                case "DOORCONTROL":
                    msgDriverDesk.put("isfault", true);

                    cars = (Collection) faultName.get(key);
                    cars.stream().forEach((car) -> {
                        JSONObject status = new JSONObject();

                        switch (car) {
                            case "acar":
                                status.put("doorcontrol", 1);
                                msgAcar.put("status", status);
                                msgTrainModel1.put("red_lamp_on", 1);
                                break;

                            case "ccar":
                                status.put("doorcontrol", 2);
                                msgAcar.put("status", status);
                                msgCcar.put("status", status);
                                msgTrainModel1.put("red_lamp_on", 2);
                                break;

                            case "c1car":
                                status.put("doorcontrol", 3);
                                msgAcar.put("status", status);
                                msgC1car.put("status", status);
                                msgTrainModel2.put("red_lamp_on", 2);
                                break;
                        }
                    });
                    break;

                case "PER":
                    handlePer((boolean) faultName.get(key));
                    break;

                case "FRICTIONBRAKE":
                    statusAcar.put("frictionbrake", 1);
                    msgAcar.put("status", statusAcar);
                    break;
            }
        }

        statusDMI.put("message", errMsg);
        msgDMI.put("status", statusDMI);
    }

    private void handlePer(boolean status) {
        JSONObject acarStatus = new JSONObject();
        acarStatus.put("per", status);

        msgAcar.put("status", acarStatus);
        msgDriverDesk.put("per", status);
        msgTrainModel1.put("yellow_lamp_on", 1);
    }

    private void handleCMD(String cmd) {

        switch (cmd) {
            case "shutdown":
                shutdown();
                break;

            case "startup":
                startup();
                break;

            case "key_off":
                handleCmdKeyOff();
                break;

            case "emergency_brake":
                emergencyBrake();
                break;

            case "ato_start":
                msgVideo.put("speed", 20.0);
                isATOStart = true;
                break;

            case "atb_start":
                msgVideo.put("speed", 20.0);
                break;

            case "clear_atp_brake":
                handleCmdClearATPBrake();
                break;
        }
    }

    private void handleCmdKeyOff() {
        if (isATB) {
//			msgVideo.put("speed", 1.0);
            JSONObject status = new JSONObject();
            status.put("atb", 2);
            msgDMI.put("status", status);

//			JSONObject status = new JSONObject();
//			status.put("is_turnon", false);
//			msgDMI.put("status", status);
        } else {
            baliseNo = 0;
            isSafetyLoop = false;
            mode = TrainEmuA.MODE_IDLE;
            ceilingSpeed = oldCeilingSpeed = 0;
            msgDriverDesk.put("isAuxiliariesOn", true);
            msgDMI.put("cmd", "turn_on");
            isATOStart = false;

            handleCmdClearATPBrake();
        }
    }

    private void handleCmdClearATPBrake() {
        JSONObject statusAcar = new JSONObject();
        statusAcar.put("over_speed", false);
        statusAcar.put("safety_loop", false);
        msgAcar.put("status", statusAcar);

        isATPBrake = false;
        isSafetyLoop = false;
        isOverSpeedAlarm = false;
        isEmergencyBrakeActive = false;
        isEmergencyBrakeShow = false;
        isEmergencyBrakeWaitForClear = false;
        isServiceBrakeActive = false;
        isATOStart = false;
        msgLever.put("clear_emergency_brake", true);
    }

    private void handleStatus(JSONObject status) {
        Iterator keys = status.keySet().iterator();
        String key;

        while (keys.hasNext()) {
            key = (String) keys.next();

            switch (key.toLowerCase()) {
                case "tcu1":
                    handleTCU1((int) status.get(key));
                    break;

                case "bcu":
                    handleBCU((int) status.get(key));
                    break;

                case "parking_brake_released":
                    isParkingBrakeReleased = (boolean) status.get(key);
                    break;

                // Message from Driver Desk
                case "emergency_brake":
                    if ((boolean) status.get(key)) {
                        isEmergencyBrakeActive = false;
                    } else {
                        isEmergencyBrakeActive = true;
                    }
                    break;

                // Message from Lever
                case "lever_value":
                    handleLeverValue((double) status.get(key));
                    break;

                case "lever_position":
                    handleLeverPosition((String) status.get(key));
                    break;

                case "lever_press":
                    handleLeverPress((boolean) status.get(key));
                    break;

                case "atc_mode":
                    mode = (int) status.get(key);
                    handleATCMode(mode);
                    break;

                case "old_mode":
                    handleOldMode((String) status.get(key));
                    break;

                // Message from Video
                case "train_position":
                    handleTrainPosition((double) status.get(key));
                    break;

                case "train_speed":
                    handleTrainSpeedScreen((double) status.get(key));
                    break;

                case "balise":
                    handleBalise((JSONObject) status.get(key));
                    break;

                case "station_stop":
                    handleStationStop();
                    break;

                case "ceiling_speed":
                    handleCeilingSpeed((int) status.get(key));
                    break;

                // Message from Instructor
                case "state":
                    handleState((String) status.get(key));
                    break;

                case "atcbypass":
                    isATCBypass = (boolean) status.get(key);
                    break;

                case "doorbypass":
                    isDoorByPass = (boolean) status.get(key);
                    break;

                case "ato":
                    handleATO();
                    break;

                case "atb":
                    handleATB((int) (long) status.get(key));
                    break;

                case "koh":
                    handleKOH((boolean) status.get(key));
                    break;
            }
        }
    }

    private void handleKOH(boolean isKOH) {
        if (isKOH) {
            JSONObject acarStatus = new JSONObject();
            acarStatus.put("koh", true);
            msgAcar.put("status", acarStatus);
            msgDriverDesk.put("isfault", false);
            msgTrainModel1.put("red_lamp_off", 1);
        }
    }

    private void handleATO() {
        //System.out.println("ATO Active");
        JSONObject status = new JSONObject();
        status.put("ato", 1);
        msgDMI.put("status", status);
    }

    private void handleATB(int type) {
        JSONObject status = new JSONObject();

        switch (type) {
            case 1:
                isATB = true;
                status.put("atb", 1);
                msgDMI.put("status", status);
                msgDriverDesk.put("atb", 1);
                break;

            case 3:
                status.put("atb", 3);
                msgDMI.put("status", status);
                break;

        }
    }

    private void handleTrainSpeedScreen(double trainSpeedScreen) {
        this.trainSpeedScreen = trainSpeedScreen;
        //System.out.println("Train Speed on Screen : " + trainSpeedScreen);
    }

    private void handleTrainPosition(double trainPosition) {
        this.trainPosition = trainPosition;
    }

    private void handleBalise(JSONObject data) {
        baliseNo++;

        handleCeilingSpeed((int) data.get("ceiling_speed"));
        dmiStatus.put("target_speed", (double) (int) data.get("target_speed"));

        if (isATOStart) {
            trainSpeed = (double) (int) data.get("train_speed");
            dmiStatus.put("atenna_status", 2);
            dmiStatus.put("speed", trainSpeed);
            msgDriverDesk.put("speed", trainSpeed);
            msgTrainModel1.put("speed", trainSpeed);

            if (trainSpeed == 0) {
                msgVideo.put("speed", 0.0);
            }

        } else {
            if (baliseNo == 1) {
                dmiStatus.put("atenna_status", 2);
                dmiStatus.put("atc_mode", 2);		// Found 1st Balise and change ATC MODE to YARD_EOA
            }

            if (baliseNo == 2) {
                dmiStatus.put("atc_mode", 3);		// Found 2nd Balise and Change ATC MODE to MCS
            }
        }

        dmiStatus.put("target_distance", (int) data.get("target_distance"));
        dmiStatus.put("target_distance_actual", (int) data.get("target_distance"));
    }

    private void handleStationStop() {
        System.out.println("STOP");

        if (mode == TrainEmuA.MODE_AUTO | mode == TrainEmuA.MODE_ATB) {
            msgDriverDesk.put("speed", 0.0);
            dmiStatus.put("speed", 0.0);
            //dmiStatus.put("ato", 1);
            msgVideo.put("speed", 0.0);
            msgTrainModel1.put("opendoor", 1);
            msgTrainModel2.put("opendoor", 1);
            msgZoomDoor.put("door", "open");

        } else {
            if (trainSpeed == 0.0) {
                handleCeilingSpeed(0);
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

    private void emergencyBrake() {
        JSONObject statusAcar = new JSONObject();
        JSONObject statusDMI = new JSONObject();

        isNonATPBrake = true;
        isSafetyLoop = true;
        isEmergencyBrakeActive = true;

        statusAcar.put("safety_loop", true);
        msgAcar.put("status", statusAcar);
        statusDMI.put("non_atp_brake", true);
        msgDMI.put("status", statusDMI);

    }

    private void handleState(String state) {
        switch (state.toLowerCase()) {
            case "STOP":
                break;
        }
    }

    private void handleTCU1(int status) {

        if (status == 0) {
            emergencyBrake();
        }
    }

    private void handleBCU(int status) {
        bcuStatus = status;

        if (bcuStatus == 0) {
            emergencyBrake();
        }
    }

    private void handleATCMode(int mode) {
        this.mode = mode;

        switch (mode) {

            case TrainEmuA.MODE_WM:
                ceilingSpeed = 3;
                break;

            case TrainEmuA.MODE_RV:
                ceilingSpeed = 3;
                break;

            case TrainEmuA.MODE_IDLE:
                ceilingSpeed = 0;
                break;

            case TrainEmuA.MODE_YARD_SR:
                ceilingSpeed = 20;
                break;

            case TrainEmuA.MODE_RM2:
                ceilingSpeed = 35;
                break;

            case TrainEmuA.MODE_AUTO:
                msgDriverDesk.put("atc_start", true);
                break;

            case TrainEmuA.MODE_ATB:
                msgDriverDesk.put("atb", 4);		// ATB Step 4: Activate ATC Flashing
                break;
        }

        handleCeilingSpeed(ceilingSpeed);
        enableLever();
    }

    private void enableLever() {
        if (mode == TrainEmuA.MODE_IDLE & isSafetyLoop) {
            msgLever.put("enable", false);
        } else {
            msgLever.put("enable", true);
        }
    }

    private void handleLeverValue(double value) {
        if (leverValue != value) {
            leverValue = value;
        }
    }

    private void handleLeverPosition(String position) {
        leverPosition = position;

        switch (leverPosition) {
            case "D":
                if (isBrake & !isEmergencyBrakeActive) {
                    msgDriverDesk.put("isbrake", false);
                    isBrake = false;
                }

                break;

            case "0":
                if (trainSpeed > 0) {

                }
                break;

            case "B":
                if (!isBrake & !isEmergencyBrakeActive) {
                    msgDriverDesk.put("isbrake", true);
                    isBrake = true;
                }

                break;

            case "EB":
                emergencyBrake();
                break;
        }
    }

    private void handleLeverPress(boolean isPress) {
        isLeverPressed = isPress;
    }

    private void handleEB(boolean isActive) {
        isEmergencyBrakeActive = isActive;
    }

    private void handleCeilingSpeed(int ceilingSpeed) {
        this.ceilingSpeed = ceilingSpeed;
        dmiStatus.put("CEILING_SPEED", (double) ceilingSpeed);
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

        msgAcar.put("cmd", "shutdown");		// Activate when Main Breaker tripped
        msgCcar.put("cmd", "shutdown");
        msgC1car.put("cmd", "shutdown");
        msgDriverDesk.put("cmd", "shutdown");
        msgDMI.put("cmd", "turnoff");
    }

    private void startup() {		// Activate when Main Breaker on
        isShutdown = false;
        isEmergencyBrakeActive = false;
        isATOStart = false;

        msgAcar.put("cmd", "startup");
        msgCcar.put("cmd", "startup");
        msgC1car.put("cmd", "startup");
        msgDriverDesk.put("cmd", "startup");
        msgLever.put("startup", true);
        msgDMI.put("cmd", "turnoff");
    }
}
