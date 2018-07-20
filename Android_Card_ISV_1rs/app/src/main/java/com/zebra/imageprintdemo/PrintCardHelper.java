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
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.widget.Toast;

import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.common.card.containers.GraphicsInfo;
import com.zebra.sdk.common.card.containers.JobStatusInfo;
import com.zebra.sdk.common.card.enumerations.CardSide;
import com.zebra.sdk.common.card.enumerations.GraphicType;
import com.zebra.sdk.common.card.enumerations.OrientationType;
import com.zebra.sdk.common.card.enumerations.PrintType;
import com.zebra.sdk.common.card.errors.ZebraCardErrors;
import com.zebra.sdk.common.card.exceptions.ZebraCardException;
import com.zebra.sdk.common.card.graphics.ZebraCardGraphics;
import com.zebra.sdk.common.card.graphics.ZebraGraphics;
import com.zebra.sdk.common.card.graphics.enumerations.RotationType;
import com.zebra.sdk.common.card.printer.ZebraCardPrinter;
import com.zebra.sdk.device.ZebraIllegalArgumentException;
import com.zebra.sdk.settings.SettingsException;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PrintCardHelper extends Activity {
    private static final String TAG = "CardImagePath";
    private static final int CARD_FEED_TIMEOUT = 30000;

    public PrintCardHelper() {}

    protected void printCard(ZebraCardPrinter printer, String pathImage, final Context context) throws ZebraCardException, ConnectionException,
            IOException, SettingsException, ZebraIllegalArgumentException {
        ZebraGraphics graphics = null;
        try {
            List<GraphicsInfo> graphicsData = new ArrayList<GraphicsInfo>();
            graphics = new ZebraCardGraphics(printer);

            generatePrintJobImage(graphics, graphicsData, pathImage);

            int jobId = printer.print(1, graphicsData);
            pollJobStatus(printer, jobId, context);

            final JobStatusInfo jStatus = printer.getJobStatus(jobId);
            this.runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(context.getApplicationContext(), "Job completed: " + jStatus.printStatus, Toast.LENGTH_LONG).show();
                    System.out.format("Job Completed: %s%n", jStatus.printStatus);
                }
            });


        } finally {
            if (graphics != null) {
                graphics.clear();
                graphics.close();
            }
        }
    }

    private void generatePrintJobImage(ZebraGraphics graphics, List<GraphicsInfo> graphicsData, String pathImage) throws IOException {
        GraphicsInfo grInfo = new GraphicsInfo();

        graphics.initialize(0, 0, OrientationType.Landscape, PrintType.Color, Color.WHITE);

        // Front Color
        grInfo.side = CardSide.Front;
        grInfo.printType = PrintType.Color;
        grInfo.graphicType = GraphicType.BMP;

        String filepath=null;
        //sample card : Image Name : DriverLicense_Name.bmp
        //filepath = "/storage/emulated/0/Download/ID_SampleLicense.bmp"

        filepath = pathImage;

        Log.i(TAG, String.valueOf(filepath));
        byte[] imageData = FileUtils.readFileToByteArray(new File(pathImage));

        graphics.drawImage(imageData, 0, 0, 0, 0, RotationType.RotateNoneFlipNone);
        grInfo.graphicData = graphics.createImage();
        graphics.clear();

        graphicsData.add(grInfo);

        // Front Full Overlay
        grInfo = new GraphicsInfo();
        grInfo.side = CardSide.Front;
        grInfo.printType = PrintType.Overlay;
        grInfo.graphicType = GraphicType.NA;
        grInfo.graphicData = null;
        graphicsData.add(grInfo);
    }

    private boolean pollJobStatus(ZebraCardPrinter printer, int actionID, final Context context) throws ConnectionException, ZebraCardException, ZebraIllegalArgumentException {
        boolean success = false;
        long dropDeadTime = System.currentTimeMillis() + 40000;
        long pollInterval = 500;

        // Poll job status
        JobStatusInfo jobStatus = null;

        long start = System.currentTimeMillis();

        do {
            jobStatus = printer.getJobStatus(actionID);
            System.out.println(String.format("Job %d, Status:%s, Card Position:%s, " + "ReadyForNextJob:%s, Mag Status:%s, Contact Status:%s, Contactless Status:%s, " + "Error Code:%d, Alarm Code:%d", actionID, jobStatus.printStatus, jobStatus.cardPosition,
                    jobStatus.readyForNextJob, jobStatus.magneticEncoding, jobStatus.contactSmartCard, jobStatus.contactlessSmartCard, jobStatus.errorInfo.value, jobStatus.alarmInfo.value));

            if (jobStatus.contactSmartCard.contains("at_station") || jobStatus.contactlessSmartCard.contains("at_station")) {
                success = true;
                break;
            } else if (jobStatus.printStatus.contains("done_ok")) {
                success = true;
                break;
            } else if (jobStatus.printStatus.contains("error") || jobStatus.printStatus.contains("cancelled")) {
                System.out.println("The job encountered an error [" + jobStatus.errorInfo.description + "] and was cancelled.");
                success = false;
                break;
            } else if (jobStatus.errorInfo.value!= 0) {
                System.out.println("The job encountered an error [" + jobStatus.errorInfo.description + "] and was cancelled.");
                printer.cancel(actionID);
                success = false;
                break;
            } else if ((jobStatus.printStatus.contains("in_progress") && jobStatus.cardPosition.contains("feeding")) // ZMotif printers
                    || (jobStatus.printStatus.contains("alarm_handling") && jobStatus.alarmInfo.value == ZebraCardErrors.MEDIA_OUT_OF_CARDS)) { // ZXP printers
                if (System.currentTimeMillis() > start + CARD_FEED_TIMEOUT) {
                    this.runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(context.getApplicationContext(), "The job timed out waiting for a card and was cancelled.", Toast.LENGTH_LONG).show();
                            System.out.format("The job timed out waiting for a card and was cancelled.");
                        }
                    });

                    printer.cancel(actionID);
                    success = false;
                    break;
                }
            }

            if (System.currentTimeMillis() > dropDeadTime) {
                success = false;
            }

            try {
                Thread.sleep(pollInterval);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } while (true);

        return success;
    }
}
