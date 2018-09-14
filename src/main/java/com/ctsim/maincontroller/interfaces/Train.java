/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ctsim.maincontroller.interfaces;

import java.net.Socket;
import java.util.Map;
import org.json.simple.JSONObject;

/**
 *
 * @author Patipat Punboonrat
 */
public interface Train {

	public void setSessions(Map<String, Socket> sessions);

	public JSONObject process();

	public void putMsg(JSONObject msg);

	public int getSpeed();

	public String getTrackId();

	public void setBalise(String id);

	public String getBaliseId();

}
