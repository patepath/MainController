/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ctsim.maincontroller;

import com.ctsim.maincontroller.factory.Instructor;
import com.ctsim.maincontroller.factory.Video;
import com.ctsim.maincontroller.interfaces.Train;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONObject;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 *
 * @author Patipat Punboonrat
 */
public class App {

	private final ApplicationContext context = new ClassPathXmlApplicationContext("config/app_beans.xml");
	private final Map<String, Socket> sessions = new HashMap<>();
	private final BlockingQueue<JSONObject> msgs = new LinkedBlockingQueue<>();

	private final Train train;
	private final Instructor instructor;
	private final Video video;

	public App() {
		train = (Train) context.getBean("train");
		instructor = (Instructor) context.getBean("instructor");
		video = (Video) context.getBean("video");
	}

	/*
	Message Structure
	=================
		ภายในโปรแกรมนี้มี การรับ-ส่งข้อมูลไปมาระหว่าง Device (A-Car, C-Car,.. หรือ Module 
	(ATC, Fault,..) โดยทั้งหมดนี้จะเรียกว่า Node ซึ่งจะใช้ตัวแปรที่มีชื่อนำหน้าด้วย msg* และต่ั้ง
	ประเภทของตัวแปรเป็น JSONObject เป็นที่เก็บข้อมูลในการรับ-ส่งดังกล่าว
		
	    | Train |
		    |
			|---- A-Car Control Panel
			|---- C-Car Control Panel
			| .
			| .
			| .
			|---- Train Model 2

		การับ-ส่งระหว่าง Node จะมีทั้ง Node ที่อยู่ในระดับเดียวกัน หรือ Node ที่อยู่ต่างระดับกัน
	ยกตัวอย่างเช่น Node Train มี Node A-Car, Node C-Car,... อยู่ภายใน ซึ่งการรับ-ส่งข้อมูล 
	ระหว่าง Node Train และ Node A-Car เป็นการรับ-ส่งที่อยู่่ต่างระดับกัน ส่วน Node A และ 
	Node B จะเป็น Node จะเป็นการรับ-ส่งข้อมูลที่อยู่ระดับเดียวกัน

		ในแต่ละ Node  จะมีตัวแปรที่ชื่อว่า msgIn และ msgOut อยู่ทุกๆ Node ทำหน้าที่เก็บข้อมูล
	ที่รับเข้ามา และส่งออกไป โดยจะประกาศดังนี้

		private final JSONObject msgIn = new JSONObject();
		private final JSONObject msgOut = new JSONObject();
	
	โครงสร้างของข้อมูลใน msgIn และ msgOut มีดังนี้


		{<ชื่อ Node ปลายทาง>:{<OPTION>:<MESSAGE>}}
	
		- ชื่อ Node เรียงตามโครงสร้าง
			- TRAIN
				- ACAR
				- CCAR
				- C1CAR
				- DRIVERDESK
				- DMI
				- LEVER
				- ZOOMDOOR
				- DUMMYBOGIE
				- TRAINMODEL1
				- TRAINMODEL2
				- ATC
				- FAULT
			- INSTRUCTOR
			- VIDEO

		- OPTION เป็นการจัดหมวดหมู่ของ MESSAGE เพื่่อไม่ให้การจำแนกใน  switch/case ยาว
		  จนกินไป ตัวอย่างของ OPTION เช่น
			- CMD
			- STATUS
			- DEVICE

		- MESSAGE คือข้อความหรือข้อมูลที่ต้องการส่ง
	 */

	public void run() {
		try {
			new Thread(new Communication(sessions)).start();
			train.setSessions(sessions);
			instructor.setSessions(sessions);
			video.setSessions(sessions);

			while (true) {
				distributeMsg(train.process());
				distributeMsg(instructor.process());
				distributeMsg(video.process());

				try {
					Thread.sleep(10);
				} catch (InterruptedException ex) {
					Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		} catch (IOException ex) {
			Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void distributeMsg(JSONObject msg) {
		if (msg != null) {
			Iterator keys = msg.keySet().iterator();
			String key;

			while (keys.hasNext()) {
				key = (String) keys.next();

				switch (key.toUpperCase()) {
					case "TRAIN":
						train.putMsg((JSONObject) msg.get(key));
						break;

					case "INSTRUCTOR":
						instructor.putMsg((JSONObject) msg.get(key));
						break;

					case "VIDEO":

						video.putMsg((JSONObject) msg.get(key));
						break;

				}
			}
		}
	}

	private void collectMsg(JSONObject msg) {
		if (msg != null) {
			msgs.offer(msg);
		}
	}

	public static void main(String[] argc) {
		App app = new App();
		app.run();
	}
}
