/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ctsim.maincontroller.factory;

import com.ctsim.maincontroller.interfaces.ATC;
import com.ctsim.maincontroller.interfaces.Train;
import java.net.Socket;
import java.util.Iterator;
import java.util.Map;
import org.json.simple.JSONObject;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import com.ctsim.maincontroller.interfaces.ControlPanel;

/**
 *
 * @author Patipat Punboonrat
 */
public class TrainEmuA implements Train {

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

	private final ApplicationContext context = new ClassPathXmlApplicationContext("config/train_beans.xml");
	private final ControlPanel acarControlPanel;
	private final ControlPanel ccarControlPanel;
	private final ControlPanel c1carControlPanel;
	private final ControlPanel driverDesk;
	private final ControlPanel lever;
	private final ControlPanel dummybogie;
	private final ControlPanel zoomdoor;
	private final ControlPanel trainModel1;
	private final ControlPanel trainModel2;
	private final ControlPanel dmi;
	private final ATC atc;

	private boolean isOn;

	private Map<String, Socket> sessions;

	private final JSONObject msgIn = new JSONObject();
	private final JSONObject msgOut = new JSONObject();

	public TrainEmuA() {
		acarControlPanel = (ControlPanel) context.getBean("acarControlPanel");
		acarControlPanel.initDevices();

		ccarControlPanel = (ControlPanel) context.getBean("ccarControlPanel");
		ccarControlPanel.initDevices();

		c1carControlPanel = (ControlPanel) context.getBean("c1carControlPanel");
		c1carControlPanel.initDevices();

		driverDesk = (ControlPanel) context.getBean("driverDesk");
		driverDesk.initDevices();

		dmi = (ControlPanel) context.getBean("dmi");
		dmi.initDevices();

		lever = (ControlPanel) context.getBean("lever");
		lever.initDevices();

		zoomdoor = (ControlPanel) context.getBean("zoomdoor");
		zoomdoor.initDevices();

		dummybogie = (ControlPanel) context.getBean("dummybogie");
		dummybogie.initDevices();

		trainModel1 = (ControlPanel) context.getBean("trainModel1");
		trainModel1.initDevices();

		trainModel2 = (ControlPanel) context.getBean("trainModel2");
		trainModel2.initDevices();

		atc = (ATC) context.getBean("atc");
	}

	@Override
	public void setSessions(Map<String, Socket> sessions) {
		this.sessions = sessions;
	}

	@Override
	public JSONObject process() {
		msgOut.clear();

		processMsgIn();

		processControlPanel("ACAR", acarControlPanel);
		processControlPanel("CCAR", ccarControlPanel);
		processControlPanel("C1CAR", c1carControlPanel);
		processControlPanel("DRIVERDESK", driverDesk);
		processControlPanel("DMI", dmi);
		processControlPanel("LEVER", lever);
		processControlPanel("ZOOMDOOR", zoomdoor);
		processControlPanel("DUMMYBOGIE", dummybogie);
		processControlPanel("TRAINMODEL1", trainModel1);
		processControlPanel("TRAINMODEL2", trainModel2);

		distributeMsg(atc.process());
		//distributeMsg(rollingStock.process());

		return msgOut;
	}

	private void processControlPanel(String sessionName, ControlPanel controlPanel) {
		if (sessions.get(sessionName) != null) {
			if (controlPanel.isCommunicatinLoss()) {
				controlPanel.setSocket(sessions.get(sessionName));
			}

			distributeMsg(controlPanel.process());

			if (controlPanel.isCommunicatinLoss()) {
				sessions.replace(sessionName, null);
			}
		}

		distributeMsg(atc.process());
	}

	private void processMsgIn() {
		distributeMsg(msgIn);

		msgIn.clear();
	}

	private void distributeMsg(JSONObject msg) {
		if (msg != null) {
			Iterator keys = msg.keySet().iterator();
			String key;

			while (keys.hasNext()) {
				key = (String) keys.next();

				switch (key) {
					case "ACAR":
						acarControlPanel.putMsg((JSONObject) msg.get(key));
						break;

					case "CCAR":
						ccarControlPanel.putMsg((JSONObject) msg.get(key));
						break;

					case "C1CAR":
						c1carControlPanel.putMsg((JSONObject) msg.get(key));
						break;

					case "DRIVERDESK":
						driverDesk.putMsg((JSONObject) msg.get(key));
						break;

					case "LEVER":
						lever.putMsg((JSONObject) msg.get(key));
						break;

					case "ATC":
						atc.putMsg((JSONObject) msg.get(key));
						break;

					case "DMI":
						dmi.putMsg((JSONObject) msg.get(key));
						break;

					case "ZOOMDOOR":
						zoomdoor.putMsg((JSONObject) msg.get(key));
						break;

					case "TRAINMODEL1":
						trainModel1.putMsg((JSONObject) msg.get(key));
						break;

					case "TRAINMODEL2":
						trainModel2.putMsg((JSONObject) msg.get(key));
						break;

					case "DUMMYBOGIE":
						dummybogie.putMsg((JSONObject) msg.get(key));
						break;

					default:
						msgOut.put(key, (JSONObject) msg.get(key));
				}
			}
		}
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

	@Override
	public int getSpeed() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public String getTrackId() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void setBalise(String id
	) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public String getBaliseId() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

}
