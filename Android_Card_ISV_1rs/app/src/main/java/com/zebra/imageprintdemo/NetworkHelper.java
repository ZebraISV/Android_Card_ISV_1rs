/*
 * CONFIDENTIAL AND PROPRIETARY
 *
 * The source code and other information contained herein is the confidential and exclusive property of
 * ZIH Corp. and is subject to the terms and conditions in your end user license agreement.
 * This source code, and any other information contained herein, shall not be copied, reproduced, published,
 * displayed or distributed, in whole or in part, in any medium, by any means, for any purpose except as
 * expressly permitted under such license agreement.
 *
 * This source code shall not create any obligation for ZIH Corp. to continue to develop, productize,
 * support, repair, offer for sale or in any other way continue to provide or
 * develop Software either to Licensee.
 *
 * This source code was developed with Android Studio 3.1.3 and tested with Zebra Mobile Computer TC51 and Android 7.1.2 for TCP communication to the ZC300 printer.
 * This source code was tested with Samsung  Galaxy S5 and Android 6.0.1 for TCP and USB communication with OTG cable to communicate to the ZC300 printer.
 * This source code does not support USB-C or USB Type C port for USB communication to the printer ZC300 printer.
 *
 * THE SOFTWARE IS PROVIDED “AS IS” AND WITHOUT ANY EXPRESS OR IMPLIED WARRANTY OF ANY KIND INCLUDING WARRANTIES
 * OF MERCHANTABILITY OR FITNESS FOR ANY PURPOSE.
 *
 * Copyright ZIH Corp. 2018
 *
 * ALL RIGHTS RESERVED *
 *
 */


package com.zebra.imageprintdemo;

import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.comm.TcpConnection;
import com.zebra.sdk.common.card.containers.PrinterInfo;
import com.zebra.sdk.common.card.containers.PrinterStatusInfo;
import com.zebra.sdk.common.card.exceptions.ZebraCardException;
import com.zebra.sdk.common.card.printer.ZebraCardPrinter;
import com.zebra.sdk.common.card.printer.ZebraCardPrinterFactory;
import com.zebra.sdk.settings.SettingsException;

public class NetworkHelper {

    public NetworkHelper() {}

    protected void getPrinterStatusOverTcp(String theIpAddress, String port) throws ConnectionException, SettingsException, ZebraCardException {
        Connection connection = new TcpConnection(theIpAddress, Integer.parseInt(port));
        ZebraCardPrinter zebraCardPrinter = null;

        try {
            connection.open();
            zebraCardPrinter = ZebraCardPrinterFactory.getInstance(connection);

            PrinterStatusInfo printerStatusInfo = zebraCardPrinter.getPrinterStatus();
            System.out.format("Status: %s%n", printerStatusInfo.status);
            System.out.format("Alarm: %s (%s)%n", printerStatusInfo.alarmInfo.value, printerStatusInfo.alarmInfo.description);
            System.out.format("Error: %s (%s)%n", printerStatusInfo.errorInfo.value, printerStatusInfo.errorInfo.description);
            System.out.format("Total jobs: %s%n", printerStatusInfo.jobsTotal);
            System.out.format("Pending jobs: %s%n", printerStatusInfo.jobsPending);
            System.out.format("Active jobs: %s%n", printerStatusInfo.jobsActive);
            System.out.format("Completed jobs: %s%n%n", printerStatusInfo.jobsComplete);

            PrinterInfo printerInfo = zebraCardPrinter.getPrinterInformation();
            System.out.format("Vendor: %s%n", printerInfo.vendor);
            System.out.format("Model: %s%n", printerInfo.model);
            System.out.format("SerialNumber: %s%n", printerInfo.serialNumber);
            System.out.format("OEM Code: %s%n", printerInfo.oemCode);
            System.out.format("Firmware Version: %s%n%n", printerInfo.firmwareVersion);

        } catch (ConnectionException e) {
            // Handle communications error here.
            e.printStackTrace();
        } finally {
            // Release resources and close the connection
            zebraCardPrinter.destroy();
            connection.close();
        }
    }
}
