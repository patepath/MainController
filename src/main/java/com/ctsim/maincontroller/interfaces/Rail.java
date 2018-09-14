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
public interface Rail {

	public JSONObject operate();

	public JSONObject processMsg(JSONObject msg);

}
