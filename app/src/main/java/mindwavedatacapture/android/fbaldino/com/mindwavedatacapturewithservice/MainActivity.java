package mindwavedatacapture.android.fbaldino.com.mindwavedatacapturewithservice;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {


        private TextView TVLine;
        private TextView TxtFile;
//        private Button BtnChooseDevice;
        private Button BtnConnectandstarmindwave;
        private BluetoothAdapter mBluetoothAdapter;
        private static boolean ServiceIsRunning=false;
        private CheckBox CkbFocused;
        private EditText EdtActivity;
        private EditText EdtWhoami;
        private Button BtnStopCapture;
        private  Button BtnSetParams;
        final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 323;
        static String StrWhoamI="";
        static String StrActivity="";
        static String StrIsUserFocused="";

        static String  MyTAG;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
//            SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);

            MyTAG= getResources().getString(R.string.LogTag);
            setContentView(R.layout.activity_main);
            requestpermissions();
            TxtFile = (TextView) findViewById(R.id.TxtFile);


            CkbFocused = (CheckBox) findViewById(R.id.CkbFocused);
            EdtActivity = (EditText) findViewById(R.id.EdtActivity);
            EdtWhoami = (EditText) findViewById(R.id.EdtWhoamI);

            BtnSetParams = (Button) findViewById(R.id.BtnSetParams);
            BtnStopCapture = (Button) findViewById(R.id.BtnStopCapture);

            BtnStopCapture.setEnabled(false);
            BtnSetParams.setEnabled(false);

            //    LogFileName=generatefilename();

            //   Startopenfileforwrite(LogFileName);
            BtnConnectandstarmindwave = (Button) findViewById(R.id.btnConecttomindwave);
            BtnConnectandstarmindwave.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //   try {
                    Log.d(MyTAG,"Start Capture Clicked");
                    BtnStopCapture.setEnabled(true);
                    BtnSetParams.setEnabled(true);

                    StrWhoamI=EdtWhoami.getText().toString();
                    StrActivity=EdtActivity.getText().toString();;
                    if (CkbFocused.isChecked()){
                        StrIsUserFocused="1";}
                    else {
                        StrIsUserFocused = "0";
                    }
                    EdtActivity.setEnabled(false);
                    EdtWhoami.setEnabled(false);
                    CkbFocused.setEnabled(false);
                    Intent intent = new Intent(MainActivity.this, MindWaveDataExtractorService.class);
                    intent.putExtra("STRWhoami", StrWhoamI);
                    intent.putExtra("STRUSerIsWorkingWithFocus", StrIsUserFocused);
                    intent.putExtra("STRActivity", StrActivity);

                    //                  findMindWaveBT();
                    //                  LogFileName = generatefilename();
                    //                   Startopenfileforwrite(LogFileName);
                    BtnConnectandstarmindwave.setEnabled(false);
                    startService(intent);
                    ServiceIsRunning=true;

                    //  openBT();

                    //    } catch (IOException ex) {
                    //   }
                }
            });


            BtnSetParams.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

     /*           try {
                    //mmOutputStream.write(0x14);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.i(MyTAG, "error:" + e.getMessage());
                    return;
                }
    */        }
            });


            BtnStopCapture.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(MyTAG,"stop service button clicked");
                    BtnStopCapture.setEnabled(false);
                    //   stopWorker = true;
                    BtnSetParams.setEnabled(false);
                    BtnConnectandstarmindwave.setEnabled(true);
                    Intent intent = new Intent(MainActivity.this, MindWaveDataExtractorService.class);
                    EdtActivity.setEnabled(true);
                    EdtWhoami.setEnabled(true);
                    CkbFocused.setEnabled(true);
                    ServiceIsRunning=false;
                    stopService(intent);


      /*          try {
                //    CloseBT();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                EdtActivity.setEnabled(true);
                EdtWhoami.setEnabled(true);
                CkbFocused.setEnabled(true);
                BtnConnectandstarmindwave.setEnabled(true);
           //     seqnumber=0;
*/
                }
            });



            TVLine = (TextView) findViewById(R.id.TxtLine);
            //BtnChooseDevice = (Button) findViewById(R.id.btnChooseDevice);
      /*      BtnChooseDevice.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    Toast.makeText(getApplicationContext(), "connecting ...", Toast.LENGTH_SHORT).show();
                    //  scanDevice();
                    //   seqnumber = 0;

/*                LogFileName=generatefilename();
                seqnumber=0;
                badPacketCount = 0;
                tv_SavingtoFile.setText("Savibg to file "+LogFileName);
                Startopenfileforwrite(LogFileName);
                start();*//*
                }
            });
*/
            try {
                // TODO
                mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
                    Toast.makeText(
                            this,
                            "Please enable your Bluetooth and re-run this program !",
                            Toast.LENGTH_LONG).show();
                    this.finish();
//				return;
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Log.i(MyTAG, "error:" + e.getMessage());
                return;
            }


//            ServiceIsRunning=false;
        }
        private void requestpermissions() {
            // Here, thisActivity is the current activity
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_CONTACTS)
                    != PackageManager.PERMISSION_GRANTED) {

                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                    // Show an expanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.

                } else {

                    // No explanation needed, we can request the permission.

                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);

                    // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                    // app-defined int constant. The callback method gets the
                    // result of the request.
                }
            }


        }

        @Override
        public void onRequestPermissionsResult(int requestCode,
                                               String permissions[], int[] grantResults) {
            switch (requestCode) {
                case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                    // If request is cancelled, the result arrays are empty.
                    if (grantResults.length > 0
                            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                        String PermissionOK = getResources().getString(R.string.PermissionGranted);
                        Toast.makeText(MainActivity.this,PermissionOK,Toast.LENGTH_LONG).show();

                        // permission was granted, yay! Do the
                        // contacts-related task you need to do.

                    } else {
                        Toast.makeText(MainActivity.this," Wont Save anything",Toast.LENGTH_LONG).show();
                        finish();
                        // permission denied, boo! Disable the
                        // functionality that depends on this permission.
                    }
                    return;
                }

                // other 'case' lines to check for other
                // permissions this app might request
            }
        }





    protected void onStart() {
        Log.d(MyTAG,"onStart Called");
        super.onStart();
        registerReceiver(Servicereceiver, new IntentFilter(
                MindWaveDataExtractorService.NOTIFICATIONFromService));
        if (ServiceIsRunning)
        {
            EdtWhoami.setText(StrWhoamI);
            EdtActivity.setText(StrActivity);
            CkbFocused.setChecked(StrIsUserFocused.equals("1"));
            EdtActivity.setEnabled(false);
            EdtWhoami.setEnabled(false);
            CkbFocused.setEnabled(false);
            BtnStopCapture.setEnabled(true);
            BtnSetParams.setEnabled(true);
            BtnConnectandstarmindwave.setEnabled(false);
        }
        else
        {
            SharedPreferences sharedPref;
            sharedPref = this.getPreferences(Context.MODE_PRIVATE);
            StrActivity = sharedPref.getString(getString(R.string.PrefsStrActivity), "");
            StrWhoamI= sharedPref.getString(getString(R.string.PrefsWhoamI), "");
            StrIsUserFocused= sharedPref.getString(getString(R.string.PrefsIsUserFocused), "0");

            EdtWhoami.setText(StrWhoamI);
            EdtActivity.setText(StrActivity);
            CkbFocused.setChecked(StrIsUserFocused.equals("1"));
            EdtActivity.setEnabled(true);
            EdtWhoami.setEnabled(true);
            CkbFocused.setEnabled(true);
            BtnStopCapture.setEnabled(false);
            BtnSetParams.setEnabled(false);
            BtnConnectandstarmindwave.setEnabled(true);
        }

    }



        @Override
        protected void onPause() {
 //          unregisterReceiver(Servicereceiver);

            super.onPause();


        }
        @Override
        public void onStop() {
            unregisterReceiver(Servicereceiver);
            super.onStop();
            Log.d(MyTAG, "app stoped");
            SharedPreferences sharedPref;
            sharedPref = this.getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(getString(R.string.PrefsStrActivity), StrActivity );
            editor.putString(getString(R.string.PrefsWhoamI), StrWhoamI);
            editor.putString(getString(R.string.PrefsIsUserFocused), StrIsUserFocused);
            editor.commit();
        }
        @Override
        protected void onDestroy() {
         //  unregisterReceiver(Servicereceiver);
            Intent intent = new Intent(MainActivity.this, MindWaveDataExtractorService.class);
            stopService(intent);

            super.onDestroy();

        }

        private BroadcastReceiver Servicereceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    String AllString = bundle.getString(MindWaveDataExtractorService.MessageFromService);
                    String filename = bundle.getString(MindWaveDataExtractorService.ServiceLogfilename);
                    int resultCode = bundle.getInt(MindWaveDataExtractorService.ResultCode);
                    if (resultCode == RESULT_OK) {
                        TVLine.setText(AllString);
                        TxtFile.setText(filename);
                    } else {
                    }
                }
            }
        };

    }
