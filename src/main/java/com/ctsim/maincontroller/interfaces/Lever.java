/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ctsim.maincontroller.interfaces;

import java.net.Socket;
import org.json.simple.JSONObject;

/**
 *
 * @author instructor
 */
public interface Lever {

	public void initDevices();

	public void setSocket(Socket socket);

	public boolean isCommunicatinLoss();

	public JSONObject process();

}
