package mindwavedatacapture.android.fbaldino.com.mindwavedatacapturewithservice;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import static android.app.Activity.RESULT_OK;

/**
 * Created by fbald on 07/11/2016.
 */

public class MindWaveDataExtractorService extends Service {
    /* Parser types */
    public static final String NOTIFICATIONFromService= "com.fbaldino.android.mindwavedatacapture.MindWaveDataCaptureService";
    public static final String MessageFromService = "StringAllValues";
    public static final String ResultCode = "Result";
    public static final String ServiceLogfilename = "Filename";
    static final int PARSER_TYPE_NULL = 0x00;
    static final int PARSER_TYPE_PACKETS = 0x01; /* Stream bytes as ThinkGear Packets */
    static final int PARSER_TYPE_2BYTERAW = 0x02;/* Stream bytes as 2-byte raw data */

    /* Data CODE definitions */
    static final int PARSER_BATTERY_CODE = 0x01;
    static final int PARSER_POOR_SIGNAL_CODE = 0x02;
    static final int PARSER_ATTENTION_CODE = 0x04;
    static final int PARSER_MEDITATION_CODE = 0x05;
    static final int PARSER_RAW_CODE = 0x80;
    static final int PARSER_ASIC_EEG_POWER = 0x83;
    static final int PARSER_HEART_RATE = 0x03;

    static final int SYNC = 0xAA;
    static final int EXCODE = 0x55;

    static String  MyTAG;
    Thread workerThread;
    volatile boolean stopWorker;
    byte[] readBuffer;
    int readBufferPosition;
    public static byte[] mindwavemessage;
    static String StrWhoamI="";
    static String StrActivity="";
    static String StrIsUserFocused="";


    //Bluetoorh and file variables
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBluetoothDevice;

    // File Variables
    private String LogFileName;
    static File file=null;
    static FileWriter fileWriter = null;
    static String filepath;

    static int seqnumber = 0;

    static String previouspackageString;


    /*    @Override
        public void onCreate()
        {
            super.onCreate();


        }*/
    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MyTAG= getResources().getString(R.string.LogTag);
        Log.i(MyTAG, "Service onStartCommand");
        StrWhoamI="";
        StrActivity="";
        StrIsUserFocused="";

        StrWhoamI=intent.getStringExtra("STRWhoami");
        StrIsUserFocused= intent.getStringExtra("STRUSerIsWorkingWithFocus");
        StrActivity= intent.getStringExtra("STRActivity");
       // beginListenForData();
        findMindWaveBT();
        try {
            openBT();
        }
        catch ( Exception e){
            e.printStackTrace();
        }
        return  START_STICKY;
    }

    void beginListenForData() {
        final Handler handler = new Handler();

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
   //     findMindWaveBT();
        LogFileName = generatefilename();
        Startopenfileforwrite(LogFileName);
        workerThread = new Thread(new Runnable() {
            public void run() {
                int bytesAvailable;
                // int seqnumber=0;
                mindwavemessage = new byte[180];
                int count = 1;
                int mindwavecount = 0;
                long Actualmilis;
                boolean isfirstpass = true;
                long previousmilis = System.currentTimeMillis();
                long Datediff;
                int actual;
                int previous = 0;
                seqnumber=0;
                while (!Thread.currentThread().isInterrupted() && !stopWorker) {
                    try {   // bytes available contais much more that them the package beeing analysed, so need to tread separately;
                        //store a buffer
                        // threat data
                        while (((actual = mmInputStream.read()) != -1) && !stopWorker) {
                            if (packagestart(actual, previous)) {
                                if ((readandconvertbyte(mindwavemessage, 3) != PARSER_RAW_CODE) && (ischecksumok(mindwavemessage)) && (!isfirstpass)) {
                                    seqnumber++;
                                    Actualmilis = System.currentTimeMillis();
                                    Datediff = Actualmilis - previousmilis;
                                    previousmilis = Actualmilis;
                                    previouspackageString = parsePayload(mindwavemessage, mindwavecount, Datediff);

                                    handler.post(new Runnable() {
                                        public void run() {
                                            //TVLine.setText(previouspackageString);
                                            // TVLine.setText("Packages Captured "+seqnumber);

                                        }
                                    });
                                    //put here call bo broadcast line
                                    WriteEntryinLogfile(LogFileName, previouspackageString);
                                    publishResults(previouspackageString, LogFileName, RESULT_OK);

                                }
                                mindwavecount = 0;
                                mindwavemessage[mindwavecount] = (byte) previous;
                                mindwavecount++;
                                isfirstpass = false;
                                //}
                            }
                            if (!isfirstpass) {
                                mindwavemessage[mindwavecount] = (byte) actual;
                                mindwavecount++;
                            }
                            previous = actual;
                            count++;
                        }
                        //  System.gc();
                    } catch (IOException ex) {
                        stopWorker = true;
                    }
                }
                try {
                    mmOutputStream.close();
                    mmInputStream.close();
                    ;
                    mmSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
        workerThread.start();
    }


    void findMindWaveBT() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Log.d(MyTAG,"No bluetooth adapter available");
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(enableBluetooth, 0);
            Log.d(MyTAG,"Bluetooth is not enabled, please enable it");

        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().equals("MindWave Mobile")) {
                    mmDevice = device;
                    Log.d(MyTAG,"MindWave Bluetooth Found");
                    break;
                }
            }
        }
        //    TVLine.setText("Bluetooth Device Found");
        Log.d(MyTAG,"Bluetooth Device Found");
    }
    // Generates filename
    private String generatefilename() {
        String timeStamp = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date());
        return timeStamp + ".csv";
    }

    //Generate File Header
    private String fileheader() {
//        return "sequence,PoorQuality,Attention,Meditation,Delta,Theta,LowAlpha,HighAlpha,LowBeta,HighBeta,LowGamma,MidGamma,badpacket, Focused,UserName,Activity,Miliseconds" + "\n";
        return "PoorQuality,Attention,Meditation,Delta,Theta,LowAlpha,HighAlpha,LowBeta,HighBeta,LowGamma,MidGamma,TSLP, Focused,UserName,Activity,Seq," + "\n";

    }

    //check External storage Writable
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }
    //check External storage Readable
    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }


    private void Startopenfileforwrite(String filename) {
        if (isExternalStorageReadable() && isExternalStorageWritable()) {
            filepath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();

            /*Write to file*/
            try {
                file = new File(filepath, filename);
                fileWriter = new FileWriter(file);
                fileWriter.append(fileheader());
                fileWriter.flush();
                fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }


        }
        else
            filepath=null;
    }
    private void WriteEntryinLogfile(String filename, String LineString) {

        if (filepath!=null)  {
            try {
                //error
                //  filepath+=File.pathSeparator+filename;
                //FileOutputStream fou = openFileOutput(filepath,MODE_APPEND);
                //File file = new File(filepath, filename);
                //    if (file.exists ()) file.delete ();
                //FileOutputStream out = new FileOutputStream(file, true);
                file = new File(filepath, filename);
                fileWriter = new FileWriter(file,true);
                fileWriter.append(LineString);
                fileWriter.flush();
                fileWriter.close();

//                fileWriter.append(LineString);
                //    Log.d(TAG, "opened File " + filename + File.pathSeparator + filename);
//               out.write(text.getBytes());
                //              fileWriter.flush();
                //             fileWriter.close();
            } catch (IOException e) {
                Log.e("Exception", "File write failed: " + e.toString());
            }
            //f.write(text.getBytes());
            //f.close();
        }
    }
    // MindWaveData Treatment
    public static boolean packagestart(int b1, int b2) {
        return b1 == SYNC && b2 == SYNC;
    }

    //Conversion functions
    public static int readandconvertbyte(byte Data[], int index) {
        byte c;
        int temp;
        c = Data[index];
        temp = c;
        temp = temp & 0x0000ff;
        return temp;
    }

    static boolean ischecksumok(byte packet[]) {
        int pLength = readandconvertbyte(packet, 2);
        if (pLength >= 170)
            return false;
        /* Collect [PAYLOAD...] bytes */
//        fread( payload, 1, pLength, stream );

        /* Compute [PAYLOAD...] chksum */
        int checksum = 0;
        for (int i = 0; i < pLength; i++) {
            checksum += readandconvertbyte(packet, 3 + i);
            //checksum += packet[3+i];
        }
        checksum &= 0xFF;
        checksum = ~checksum & 0xFF;
        /* Verify [PAYLOAD...] chksum against [CKSUM] */
        int c = readandconvertbyte(packet, pLength + 3);
        if (c != checksum)
            return false;
        return (c == checksum);
    }
    static String parsePayload(byte payload[], int pLength,Long Miliseconds) {

        int bytesParsed = 3;
        int code;
        int length;
        int extendedCodeLevel;
        int i;
        int localattention = 0;
        int localmeditation = 0;
        int localdelta = 0;
        int localtheta = 0;
        int locallowalpha = 0;
        int localhighalpha = 0;
        int locallowbeta = 0;
        int localhighbeta = 0;
        int locallowgamma = 0;
        int localmiddlegamma = 0;
        int localbadpacket = 0;
        int localbattery = 0;
        int localpoorsignal = 0;
        int localHeartRate = 0 ;



	    /* Loop until all bytes are parsed from the payload[] array... */
        while (bytesParsed < pLength-2) {

	        /* Parse the extendedCodeLevel, code, and length */
            extendedCodeLevel = 0;
            while (readandconvertbyte(payload, bytesParsed) == EXCODE) {
                extendedCodeLevel++;
                bytesParsed++;
            }
            //    if (bytesParsed>=pLength)
            //  	System.out.print("ERROR");
            code = readandconvertbyte(payload, bytesParsed++);
            //      System.out.print("code & 0x80:"+Integer.toString((code & 0x80)));
            if ((code & 0x80) > 0)
                length = readandconvertbyte(payload, bytesParsed++);
            else
                length = 1;

	        /* TODO: Based on the extendedCodeLevel, code, length,
             * and the [CODE] Definitions Table, handle the next
	         * "length" bytes of data from the payload as
	         * appropriate for your application.
	         */
//	        System.out.print( "EXCODE level: 0x" +Integer.toHexString(extendedCodeLevel) + " CODE: 0x" + Integer.toHexString(code) + " length:"+ Integer.toHexString(length) );
//	        System.out.println(" length"+Integer.toString(length)+" bytes parsed" + Integer.toString(bytesParsed));
//	        System.out.print( "Data value(s):" );
            if (code == PARSER_RAW_CODE)
            {
                length=2;
            }
            if (code == PARSER_ASIC_EEG_POWER) {
                int indextemp = bytesParsed;
   /*         delta, theta, low-alpha
            high-alpha, low-beta, high-beta,
            low-gamma, and mid-gamma EEG band
            power values*/
                localdelta = readandconvertbytearraytoint(payload, indextemp, 3);
                indextemp += 3;
                localtheta = readandconvertbytearraytoint(payload, indextemp, 3);
                indextemp += 3;
                locallowalpha = readandconvertbytearraytoint(payload, indextemp, 3);
                indextemp += 3;
                localhighalpha = readandconvertbytearraytoint(payload, indextemp, 3);
                indextemp += 3;
                locallowbeta = readandconvertbytearraytoint(payload, indextemp, 3);
                indextemp += 3;
                localhighbeta = readandconvertbytearraytoint(payload, indextemp, 3);

                indextemp += 3;
                locallowgamma = readandconvertbytearraytoint(payload, indextemp, 3);
                indextemp += 3;
                localmiddlegamma = readandconvertbytearraytoint(payload, indextemp, 3);
            }
            if (code == PARSER_BATTERY_CODE) {
                localbattery = readandconvertbyte(payload, bytesParsed);
                Log.i(MyTAG,"Battery power"+localbattery);
            }
            if (code == PARSER_HEART_RATE)
            {
                localHeartRate = readandconvertbyte(payload, bytesParsed);
                Log.i(MyTAG,"Heart Rate"+localHeartRate );

            }
            if (code == PARSER_POOR_SIGNAL_CODE) {
                localpoorsignal = readandconvertbyte(payload, bytesParsed);
            }
            if (code == PARSER_ATTENTION_CODE) {
                localattention = readandconvertbyte(payload, bytesParsed);
            }
            if (code == PARSER_MEDITATION_CODE) {
                localmeditation = readandconvertbyte(payload, bytesParsed);
            }
            if((code != PARSER_RAW_CODE)&&(code!=PARSER_ASIC_EEG_POWER)&&(code!=PARSER_POOR_SIGNAL_CODE)&&(code!=PARSER_ATTENTION_CODE)&&(code!=PARSER_MEDITATION_CODE))
            {

                Log.i(MyTAG,"untreated code "+code);
            }

//	        for( i=0; i<length; i++ ) {
//	            System.out.print( "0X"+ Integer.toHexString(payload[bytesParsed+i] & 0xFF)+"," );
            //            if(bytesParsed+i>pLength)
            //          {
            //        	System.out.println("Outofbounds");
            //      }
            //      }
            //        System.out.println();

	        /* Increment the bytesParsed by the length of the Data Value */
            bytesParsed += length;
        }
        //  System.out.println();
        // if (seqnumber==1)
        // System.out.println(fileheader());
        return (AssemblyLineString(seqnumber, localpoorsignal, localattention, localmeditation, localdelta, localtheta, locallowalpha, localhighalpha, locallowbeta, localhighbeta, locallowgamma, localmiddlegamma, Miliseconds,StrIsUserFocused,StrWhoamI,StrActivity)) ;
    }

    private static String AssemblyLineString(int sequence, int signal, int attention, int meditation, int delta, int theta, int lowalpha, int highalpha, int lowbeta, int highbeta, int lowgamma, int middlegamma, Long Miliseconds, String StrFocused, String UserName, String Activity) {
        String StrTemp = " ";
        //  StrTemp = Integer.toString(sequence) + ",";
        StrTemp = StrTemp + Integer.toString(signal) + "," + Integer.toString(attention) + "," + Integer.toString(meditation) + "," + Integer.toString(delta) + "," + Integer.toString(theta) + "," + Integer.toString(lowalpha) + "," + Integer.toString(highalpha) + ",";
        StrTemp = StrTemp + Integer.toString(lowbeta) + "," + Integer.toString(highbeta) + "," + Integer.toString(lowgamma) + "," + Integer.toString(middlegamma) + "," + Miliseconds + "," + StrFocused + "," + UserName + "," + Activity+","+sequence + "\n";
        return StrTemp;

    }
    void openBT() throws IOException {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
        mmSocket.connect();
        mmOutputStream = mmSocket.getOutputStream();
        mmInputStream = mmSocket.getInputStream();
        //publishResults(LogFileName,RESULT_OK);

        beginListenForData();
    }
    public static int readandconvertbytearraytoint(byte Data[], int index, int nbytes) {
        int temp;
        int result = 0;
        for (int i = 0; i < nbytes; i++) {
            temp = readandconvertbyte(Data, index + i);
            result = result << 8;
            result += temp;
        }
        return result;
    }
    @Override
    public void onDestroy( )
    {
        stopWorker=true;
/*        try {
            wait(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

  */
        publishResults("Nodata","NoFile",RESULT_OK);
        super.onDestroy();

    }
    private void publishResults(String EchoStringData,String Filename, int result) {
        Intent intent = new Intent(NOTIFICATIONFromService);
        intent.putExtra(MessageFromService, EchoStringData);
        intent.putExtra(ServiceLogfilename, Filename);
        intent.putExtra(ResultCode, result);
        sendBroadcast(intent);
    }

}
