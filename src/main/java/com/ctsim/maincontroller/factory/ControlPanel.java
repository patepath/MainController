/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ctsim.maincontroller.factory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author Patipat Punboonrat
 */
public class ControlPanel {

	protected Calendar now;
	protected Calendar startWatchDog;
	protected final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

	private Socket socket;
	private BufferedReader in;
	private PrintWriter out;
	protected boolean isCommLoss = true;

	protected String name = "";

	protected Map<String, JSONObject> devs = new HashMap<>();
	protected Map<String, String> getNewByOld = new HashMap<>();

	protected final JSONObject parentMsgIn = new JSONObject();

	protected final JSONObject parentMsgOut = new JSONObject();
	protected final Collection<String> atcMsgOut = new LinkedList<>();
	protected final Collection<String> failMsgOut = new LinkedList<>();

	protected final BlockingQueue<String> childMsgOut = new LinkedBlockingQueue<>();

	public void setSocket(Socket socket) {

		if (socket != null) {
			try {
				this.socket = socket;
				in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
				out = new PrintWriter(socket.getOutputStream(), true);
				isCommLoss = false;
				startWatchDog = Calendar.getInstance();

			} catch (IOException ex) {
				Logger.getLogger(ACarEmuA.class.getName()).log(Level.SEVERE, null, ex);
			}

		} else {
			isCommLoss = true;
		}
	}

	protected boolean checkError() throws IOException {
		if (out.checkError()) {
			in.close();
			out.close();
			socket.close();
			isCommLoss = true;
			return true;
		}

		return false;
	}

	public boolean isCommunicatinLoss() {
		return isCommLoss;
	}

	protected String getCmdFromSocket(String quationChar, String prefixChar) {
		String childMsgIn;
		String id;

		try {
			if (socket.getInputStream().available() > 0) {
				startWatchDog = Calendar.getInstance();

				childMsgIn = in.readLine().toUpperCase();
				out.println(childMsgIn);
				checkError();

				if (childMsgIn.startsWith(quationChar)) {
					while (!childMsgOut.isEmpty()) {
						out.println(childMsgOut.poll());
						checkError();
					}

				} else if (childMsgIn.startsWith(prefixChar) & childMsgIn.length() == 7) {
					id = getNewByOld.get(childMsgIn.split(" ")[0]);

					if (id != null) {
						return id + " " + childMsgIn.split(" ")[1];
					}

					now = Calendar.getInstance();
					System.out.println(sdf.format(now.getTime()) + " Message \"" + childMsgIn + "\" error!");
				}
			}

			if ((Calendar.getInstance().getTimeInMillis() - startWatchDog.getTimeInMillis()) > 4000) {
				in.close();
				out.close();
				socket.close();
				isCommLoss = true;

				now = Calendar.getInstance();
				System.out.println(sdf.format(now.getTime()) + " ControlPanel.getCmdFromSocket() : " + name + " disconnected.");
			}

		} catch (IOException | NullPointerException ex) {
		}

		return "";
	}

	public void initMapping(String xmlPath) {
		File fXmlFile;
		DocumentBuilderFactory dbFactory;
		DocumentBuilder dBuilder;
		Document doc;
		NodeList nodeList;
		Element element;

		String old_id;

		try {
			fXmlFile = FileUtils.toFile(this.getClass().getClassLoader().getResource("config/" + xmlPath));
			dbFactory = DocumentBuilderFactory.newInstance();
			dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(fXmlFile);
			doc.getDocumentElement().normalize();
			nodeList = doc.getElementsByTagName("device");

			for (int i = 0; i < nodeList.getLength(); i++) {
				element = (Element) nodeList.item(i);

				try {
					JSONObject json = new JSONObject();
					json.put("name", element.getElementsByTagName("name").item(0).getTextContent());
					json.put("type", element.getElementsByTagName("type").item(0).getTextContent());

					old_id = element.getElementsByTagName("old_id").item(0).getTextContent();

					if (old_id != null) {
						json.put("mapping", element.getAttribute("id"));
						getNewByOld.put(old_id, element.getAttribute("id"));
					}

					json.put("status", "");

					devs.put(element.getAttribute("id"), json);

				} catch (NullPointerException ex) {
				}
			}
		} catch (ParserConfigurationException | SAXException | IOException ex) {
			Logger.getLogger(ControlPanel.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
}
