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
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.comm.TcpConnection;
import com.zebra.sdk.common.card.exceptions.ZebraCardException;
import com.zebra.sdk.common.card.printer.ZebraCardPrinter;
import com.zebra.sdk.common.card.printer.ZebraCardPrinterFactory;

import java.io.File;

public class MainActivity extends AppCompatActivity {


    private Spinner imageSelectionSpinner;
    private static final String TAG = "CardImagePath";
    private String ip;
    private String port;
    private String uriTransferPath = null ;
    private Button buttonCardUsb;
    private Button buttonCardNetwork;
    private EditText ipAddressEditText;
    private EditText portNumberEditText;
    private EditText printStoragePath;
    private UIHelper helper = new UIHelper(this);
    private PathFileHelper picPathFile = new PathFileHelper();
    private NetworkHelper tcpCardHandler = new NetworkHelper();
    private PrintCardHelper printthisCard = new PrintCardHelper();
    Context context;

    private static int TAKE_PICTURE = 1;
    private static int PICTURE_FROM_GALLERY = 2;
    private static File file = null;
    static final int REQUEST_IMAGE_CAPTURE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = getApplicationContext();


        ipAddressEditText = (EditText) this.findViewById(R.id.ipAddressInput);
        portNumberEditText = (EditText) this.findViewById(R.id.portInput);

        TextView t2 = (TextView) findViewById(R.id.launchpad_link);
        t2.setMovementMethod(LinkMovementMethod.getInstance());


        printStoragePath = (EditText) findViewById(R.id.printerStorePath);

        buttonCardNetwork = (Button) this.findViewById(R.id.buttonCardTCP);

        buttonCardNetwork.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Thread threadmac = new Thread(new Runnable() {
                    @Override
                    public void run() {

                        try  {
                            Connection connection = null;
                            ZebraCardPrinter printer = null;
                            try {
                                ip = ipAddressEditText.getText().toString();
                                System.out.format("Status: %s%n", ip);
                                port = portNumberEditText.getText().toString();
                                System.out.format("Status: %s%n", port);
                                tcpCardHandler.getPrinterStatusOverTcp(ip, port);
                                connection = new TcpConnection(ip,Integer.parseInt(port));
                                connection.open();
                                printer = ZebraCardPrinterFactory.getInstance(connection);
                                if (printer != null) {
                                    printthisCard.printCard(printer, uriTransferPath, context);
                                }
                            } catch (Exception e) {
                                System.out.format("Status: %s%n", e.getMessage());
                                }
                            finally {
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
                threadmac.start();
            }
        });


        buttonCardUsb = (Button) this.findViewById(R.id.buttonCardUsb);

        buttonCardUsb.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!(uriTransferPath==null)) {
                    Intent intent = new Intent(MainActivity.this, UsbDiscoveryAndPrintExample.class);
                    intent.putExtra("imagePath", uriTransferPath);
                    startActivity(intent);
                 }
            }
        });

        imageSelectionSpinner = (Spinner) findViewById(R.id.imageSelection);
        ArrayAdapter<CharSequence> imageAdapter = ArrayAdapter.createFromResource(this, R.array.image_selection, android.R.layout.simple_spinner_item);
        imageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        imageSelectionSpinner.setAdapter(imageAdapter);
        imageSelectionSpinner.setOnItemSelectedListener(new OnItemsSelectedListener());

    }


    /**
     *
     * @param requestCode  if the condition satisfies it will pull the data
     * @param resultCode
     * @param data
     */

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == TAKE_PICTURE ) {
                Bundle extras = data.getExtras();
                Bitmap myImagemap = (Bitmap) extras.get("data");
                File finalFile = new File(picPathFile.getPathFromUri(getApplicationContext(), picPathFile.getImageUri(getApplicationContext(), myImagemap)));
                Log.i(TAG, String.valueOf(finalFile));

                if(finalFile.exists() ){
                    Bitmap myBitmap = BitmapFactory.decodeFile(finalFile.getAbsolutePath());

                        ImageView myImage = (ImageView) findViewById(R.id.imageFrontView);
                        myImage.setImageBitmap(myBitmap);
                        uriTransferPath = finalFile.toString();

                }
            }
            if (requestCode == PICTURE_FROM_GALLERY) {
                Uri imgPath = data.getData();

                // CALL THIS METHOD TO GET THE ACTUAL PATH
                File finalFile = new File(picPathFile.getPathFromUri(getApplicationContext(), imgPath));
                Log.i(TAG, String.valueOf(finalFile));

                if(finalFile.exists()){
                    Bitmap myBitmap = BitmapFactory.decodeFile(finalFile.getAbsolutePath());
                    ImageView myImage = (ImageView) findViewById(R.id.imageFrontView);
                    myImage.setImageBitmap(myBitmap);
                }
                uriTransferPath = finalFile.toString();
                Log.i(TAG, String.valueOf(imgPath));
            }
        }
    }


    /**
     * OnItemSelectedListener for imageSelectionSpinner
     */

    public class OnItemsSelectedListener implements AdapterView.OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            imageSelectionSpinner.setSelection(0);
            if (position == 1) {
                getPhotoFromCamera();
            } else if (position == 2) {
                getPhotosFromGallery();
            }
        }
        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }


    /**
     * Intents to make a call when image is captured using camera
     */

    private void getPhotoFromCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, TAKE_PICTURE);
        }

    }

    /**
     * Intents to make a call when photos are selected from gallery
     */

    private void getPhotosFromGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICTURE_FROM_GALLERY);
    }

    private void toggleEditField(EditText editText, boolean set) {
        /*
         * Note: Disabled EditText fields may still get focus by some other means, and allow text input.
         *       See http://code.google.com/p/android/issues/detail?id=2771
         */
        editText.setEnabled(set);
        editText.setFocusable(set);
        editText.setFocusableInTouchMode(set);
    }


}
