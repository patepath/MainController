/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ctsim.maincontroller.interfaces;

import org.json.simple.JSONObject;

/**
 *
 * @author instructor
 */
public interface ATC {

	public JSONObject process();

	public void putMsg(JSONObject msg);

	public void setMode(int mode);

	public int getMode();
}
