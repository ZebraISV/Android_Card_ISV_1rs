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

import com.zebra.sdk.printer.discovery.DiscoveredPrinter;
import com.zebra.sdk.printer.discovery.DiscoveredPrinterUsb;
import com.zebra.sdk.printer.discovery.DiscoveryHandler;

import java.util.LinkedList;
import java.util.List;


    public class UsbDiscoveryHandler implements DiscoveryHandler {
        public List<DiscoveredPrinterUsb> printers;
        public boolean discoveryFinished = false;

        public UsbDiscoveryHandler() {
            printers = new LinkedList<>();
        }

        public void foundPrinter(final DiscoveredPrinter printer) {
            printers.add((DiscoveredPrinterUsb) printer);
        }

        public void discoveryFinished() {
            discoveryFinished = true;
        }

        public void discoveryError(String message) {
            discoveryFinished = true;
        }
    }

