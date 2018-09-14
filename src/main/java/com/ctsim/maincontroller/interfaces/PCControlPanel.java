/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ctsim.maincontroller.interfaces;

import java.net.Socket;

/**
 *
 * @author instructor
 */
public interface PCControlPanel {

	public void setSocket(Socket socket);

	public boolean isCommunicatinLoss();

}
