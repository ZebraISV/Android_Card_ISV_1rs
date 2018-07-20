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


import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.common.card.exceptions.ZebraCardException;
import com.zebra.sdk.common.card.printer.ZebraCardPrinter;
import com.zebra.sdk.common.card.printer.ZebraCardPrinterFactory;
import com.zebra.sdk.printer.discovery.DiscoveredPrinterUsb;
import com.zebra.sdk.printer.discovery.UsbDiscoverer;

import java.io.File;

public class UsbDiscoveryAndPrintExample extends Activity{
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private PendingIntent mPermissionIntent;
    private boolean hasPermissionToCommunicate = false;
    private String transferredImage;
    private UsbManager mUsbManager;
    private static final String TAG = "transImage";
    private Button buttonRequestPermission;
    private Button buttonPrint;
    private DiscoveredPrinterUsb discoveredPrinterUsb;
    private UsbDiscoveryHandler handler = new UsbDiscoveryHandler();
    private USBHelper usbHelper = new USBHelper();
    private PrintCardHelper printthisCard = new PrintCardHelper();
    Context context;

    IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);


    // Catches intent indicating if the user grants permission to use the USB device
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            hasPermissionToCommunicate = true;
                        }
                    }
                }
            }
        }
    };

    @Override
    protected void onPause() {
        unregisterReceiver(mUsbReceiver);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mUsbReceiver, filter);
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.usbconnection_screen);
        Bundle bundle = getIntent().getExtras();
        transferredImage = bundle.getString("imagePath");
        Log.i(TAG, String.valueOf(transferredImage));
        context = getApplicationContext();


        // Register broadcast receiver that catches USB permission intent
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        buttonRequestPermission = (Button) findViewById(R.id.RequestPermissionButton);
        buttonPrint = (Button) findViewById(R.id.printUSB_Button);

        File imgFile = new  File(transferredImage);
        if(imgFile.exists()){
            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            ImageView myImage = (ImageView) findViewById(R.id.imageViewCard);
            myImage.setImageBitmap(myBitmap);
        }

        // Request Permission button click
        buttonRequestPermission.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new Thread(new Runnable() {

                    public void run() {
                        // Find connected printers

                        try {
                           UsbDiscoverer.findPrinters(getApplicationContext(), handler);
                            while (!handler.discoveryFinished) {
                                Thread.sleep(100);
                            }

                            if (handler.printers != null && handler.printers.size()!= 0) {
                                discoveredPrinterUsb = handler.printers.get(0);
                                if (!mUsbManager.hasPermission(discoveredPrinterUsb.device)) {
                                    mUsbManager.requestPermission(discoveredPrinterUsb.device, mPermissionIntent);
                                } else {
                                    hasPermissionToCommunicate = true;
                                }

                            }
                        } catch (final Exception e) {
                            UsbDiscoveryAndPrintExample.this.runOnUiThread(new Runnable() {
                                public void run() {
                                    Toast.makeText(getApplicationContext(), e.getMessage() + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                                    System.out.format("Exception: %s%n", e.toString());
                                }
                            });

                        }
                    }
                }).start();
            }
        });

        // Print button click
        buttonPrint.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Thread threadmac2 = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Connection connection = null;
                            ZebraCardPrinter printer = null;
                            try {
                                if (hasPermissionToCommunicate) {
                                    try {
                                        connection = discoveredPrinterUsb.getConnection();
                                        usbHelper.getPrinterInfoOverUsb(connection);
                                        connection.open();
                                        printer = ZebraCardPrinterFactory.getInstance(connection);
                                        if (printer != null) {
                                            printthisCard.printCard(printer, transferredImage, context);
                                        }
                                    } catch (final Exception e1) {
                                        UsbDiscoveryAndPrintExample.this.runOnUiThread(new Runnable() {
                                            public void run() {
                                                Toast.makeText(getApplicationContext(), e1.getMessage() + e1.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                                                System.out.format("Exception: %s%n", e1.toString());
                                            }
                                        });
                                    }
                                } else {
                                    UsbDiscoveryAndPrintExample.this.runOnUiThread(new Runnable() {
                                        public void run() {
                                            Toast.makeText(getApplicationContext(), "No permission to communicate", Toast.LENGTH_LONG).show();
                                            System.out.format("Exception: %s%n","No permission to communicate");
                                        }
                                    });
                                  }
                            } finally {
                                try {
                                    if (printer != null) {
                                        printer.destroy();
                                        printer = null;
                                    }
                                } catch (ZebraCardException e) {
                                    e.printStackTrace();
                                }
                                if (connection != null) {
                                    try {
                                        connection.close();
                                        connection = null;
                                    } catch (ConnectionException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();

                        }
                    }
                });
                threadmac2.start();
            }
        });
    }


}
