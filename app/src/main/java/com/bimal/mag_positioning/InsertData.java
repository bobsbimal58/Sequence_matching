package com.bimal.mag_positioning;

import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by wmcs on 7/21/2017.
 */

public class InsertData extends AppCompatActivity implements SensorEventListener,View.OnClickListener {
    Sensor magnetometer;
    SensorManager sm;
    TextView magnetismx;
    TextView magnetismy;
    TextView magnetismz;
    TextView magnetismd;
    public Float xaxis;
    public Float yaxis;
    public Float zaxis;
    public Float xaxis_rot;
    public Float yaxis_rot;
    public Float zaxis_rot;
    public Float xbias;
    public Float ybias;
    public Float zbias;
    public Float average;
    public Float total;
    public Integer id;
    boolean recording = false;
    boolean stoprecord = false;

    public boolean isStart = false;
    public FileWriter file_writer;
    public boolean success = true;
    public boolean isInsert = false;
    public boolean calcdiff = false;
    public boolean islocalize = false;
    PointF get;
    public boolean isErrror = false;
    public double errorvalue;

    //Sensor events
    private float [] RotationMatrix = new float[16];
    private float [] mOrientationAngles = new float[3];
    private SensorEvent MagnetData;;
    private SensorEvent RotVectorData;
    private SensorEvent AccelVector;
    public SensorEvent Gravity;
    public  SensorEvent RawMagnetData;
    public SensorEvent GeoRotation;
    public long lastTimeAcc=System.currentTimeMillis();

    //For tilt and azimuth
    public float [] earthAcc= new float[4];
    private float gravity[] = new float[3];
    private final float beta = (float) 0.8;
    float[] magValues = new float[3];
    float[] values = new float[3];
    Integer degreeDisplay;
    Integer degree;
    int offset;

    //Average filter
    private static int k;
    private float[] prevAvg;
    private static float alpha;
    private float[] avg;

    //For coordinates
    public static final int COL = 5;
    public static final int LOW = 10;
    public boolean ismeasure=false;

    //Array
    long curTimeAcc;
    public ArrayList<Float>sensorData= new ArrayList<>();
    public ArrayList<Double> xcordList= new ArrayList<>();
    public ArrayList<Double>ycordList =  new ArrayList<>();
    public ArrayList<Double>probAll= new ArrayList<>();
    public ArrayList<Double>probFinalList= new ArrayList<>();
    float sum_x=0;
    float sum_y=0;
    float probAdd=0;
    double probFinal=0;

    public DTW dtw;
    public boolean isSend=false;
    public ArrayList<Float>testData= new ArrayList<>();
    public ArrayList<Float> testDataFinal= new ArrayList<Float>();
    public ArrayList<Float>mapData= new ArrayList<>();
    public ArrayList<Float>mapDataFinal=new ArrayList<>();
    public Cursor cur;
    float data;
 private static final int PERMISSION_REQUEST_CODE=1;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.insert_data);

        if (Build.VERSION.SDK_INT >= 23)
        {
            if (checkPermission())
            {
                // Code for above or equal 23 API Oriented Device
                // Your Permission granted already .Do next code
            } else {
                requestPermission(); // Code for permission
            }
        }
        else
        {

            // Code for Below 23 API Oriented Device
            // Do next code
        }


        sm = (SensorManager) getSystemService(SENSOR_SERVICE);
       sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);
       sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_NORMAL);
        sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
       //sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_GRAVITY), SensorManager.SENSOR_DELAY_NORMAL);
        //sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED), SensorManager.SENSOR_DELAY_NORMAL);
        //sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_NORMAL);

        magnetismx = (TextView) findViewById(R.id.magnetismx);
        magnetismy = (TextView) findViewById(R.id.magnetismy);
        magnetismz = (TextView) findViewById(R.id.magnetismz);
        magnetismd = (TextView) findViewById(R.id.magnetismd);

        magnetometer = sm.getDefaultSensor(magnetometer.TYPE_MAGNETIC_FIELD);
        if (magnetometer == null) {
            Toast.makeText(this, "Magnetometer not available", Toast.LENGTH_SHORT).show();
            finish();
        }

    }

    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(InsertData.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }
    private void requestPermission() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(InsertData.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText(InsertData.this, "Write External Storage permission allows us to do store images. Please allow this permission in App Settings.", Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(InsertData.this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.e("value", "Permission Granted, Now you can use local drive .");
                } else {
                    Log.e("value", "Permission Denied, You cannot use local drive .");
                }
                break;
        }
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.deleteDatabase:
                DBHelper.getInstance().deleteDataMag();
                break;

            case R.id.btnRecord:
                recording = true;
                Calendar calendar = Calendar.getInstance();
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                int minute = calendar.get(Calendar.MINUTE);
                int second = calendar.get(Calendar.SECOND);

                File folder = new File(Environment.getExternalStorageDirectory() + "/bimal" + hour + "-" + minute + "-" + second);
                if (!folder.exists()) {
                    success = folder.mkdir();
                }
                // Do something on success
                String csv = folder.getAbsolutePath() + "/Accelerometer.csv";
                try {
                    file_writer = new FileWriter(csv, true);

                    if (isStart == false) {
                        String s = "X-Axis, Y-Axis, Z-Axis, ERROR \n";
                        file_writer.append(s);
                        isStart = true;
                        Toast.makeText(getBaseContext(), "Data Recording Started", Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;

            case R.id.btnStop:
                stoprecord = true;
                try {
                    file_writer.close();
                    Toast.makeText(getBaseContext(), "Data Recording Stopped", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;

            case R.id.showDiff:
                calcdiff = true;
                Toast.makeText(getApplicationContext(), "finding difference", Toast.LENGTH_SHORT).show();
                 cur = DBHelper.getInstance().selectDegree();
                break;

            case R.id.loadData:
                LoadData();
                break;

            case R.id.insertData:
                insertData();
                break;

            case R.id.localaizeData:
                try {
                    BackupDatabase();
                    Toast.makeText(getApplicationContext(),"saved",Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.errorData:
                isErrror = true;
                break;
            case R.id.measure:
                ismeasure=true;
        }
    }



    public static void BackupDatabase() throws IOException {
        boolean success = true;
        File file = null;
        file = new File(Environment.getExternalStorageDirectory() + "/bimal");

        if (file.exists()) {
            success = true;
        } else {
            success = file.mkdir();
        }

        if (success) {
            String inFileName = "/data/data/com.bimal.mag_positioning/databases/Mag_Positioning.db";
            File dbFile = new File(inFileName);
            FileInputStream fis = new FileInputStream(dbFile);

            String outFileName = Environment.getExternalStorageDirectory() + "/bimal/Mag_Positioning.db";

            // Open the empty db as the output stream
            OutputStream output = new FileOutputStream(outFileName);

            // Transfer bytes from the inputfile to the outputfile
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }

            output.flush();
            output.close();
            fis.close();
        }
    }

    public void LoadData() {
        /*
        TextView view = (TextView) findViewById(R.id.load);
        SQLiteDatabase db;
        db = openOrCreateDatabase(
                "Mag_Positioning.db"
                , SQLiteDatabase.CREATE_IF_NECESSARY
                , null
        );
        db.setVersion(1);
        db.setLocale(Locale.getDefault());
        db.setLockingEnabled(true);
        Cursor cur = db.query("Fingerprint", null, null, null, null, null, null);
        cur.moveToFirst();
        while (cur.isAfterLast() == false) {
            view.append("\n" + cur.getString(2) + "," + cur.getString(3) + "," + cur.getString(4) + "," +
                    cur.getString(7) + "," + "\n");
            cur.moveToNext();
        }
        cur.close();
    }
*/
        Cursor res = DBHelper.getInstance().getAllData();
        if (res.getCount() == 0) {
            showMessage("Error", "Nothing found");
            return;
        }
        StringBuffer buffer = new StringBuffer();
        while (res.moveToNext()) {
            buffer.append("Id :" + res.getInt(0) + "\n");
            buffer.append("MapId :" + res.getInt(1) + "\n");
            buffer.append("X :" + res.getFloat(2) + "\n");
            buffer.append("Y :" + res.getFloat(3) + "\n");
            buffer.append("X_Axis :" + res.getString(4) + "\n");
            buffer.append("Y_Axis :" + res.getFloat(5) + "\n");
            buffer.append("Z_Axis :" + res.getFloat(6) + "\n");
            buffer.append("Average :" + res.getString(7) + "\n");
            buffer.append("SD :" + res.getFloat(8) + "\n");
            buffer.append("XROT :" + res.getFloat(9) + "\n");
            buffer.append("YROT :" + res.getFloat(10) + "\n");
            buffer.append("ZROT :" + res.getFloat(11) + "\n");
            buffer.append("DEGREE :" + res.getFloat(12) + "\n\n");
        }
        showMessage("Data", buffer.toString());
    }
    public void showMessage(String title, String Message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setTitle(title);
        builder.setMessage(Message);
        builder.show();
    }

    public void writeToCsv(String x, String y, String z, String q, String t, String r, String v, String u) throws IOException {
        if (isStart == true) {
            String s = x + "," + y + "," + z + "," + q + "," + t + "," + r + ","+ v +","+ u + "\n";
            file_writer.append(s);
        }
    }




    @Override
    public void onSensorChanged(SensorEvent event) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CUPCAKE) {
            if(event.accuracy==sm. SENSOR_STATUS_UNRELIABLE){
                Toast.makeText(getApplicationContext(),"Please calibrate the device", Toast.LENGTH_SHORT).show();
            }
            switch (event.sensor.getType()) {
               case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
                    GeoRotation=event;
                    break;

                case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                    curTimeAcc = System.currentTimeMillis();
                    if (curTimeAcc - lastTimeAcc > 40) {
                        RawMagnetData = event;

                    }
                    break;

                case Sensor.TYPE_MAGNETIC_FIELD:
                    curTimeAcc = System.currentTimeMillis();
                    if (curTimeAcc - lastTimeAcc > 40) {
                        MagnetData = event;
                        calculate();
                    }
                    break;

                    case Sensor.TYPE_ACCELEROMETER:
                            AccelVector=event;
                            if(AccelVector.values[0]>0){
                                isSend=true;
                                Toast.makeText(getApplicationContext(), "Accelerating", Toast.LENGTH_SHORT).show();
                            }

                        break;

                case Sensor.TYPE_GRAVITY:
                    Gravity = event;
                    break;


                case Sensor.TYPE_ROTATION_VECTOR:
                    curTimeAcc = System.currentTimeMillis();
                    if (curTimeAcc - lastTimeAcc > 40) {
                        RotVectorData = event;
                        sm.getRotationMatrixFromVector(RotationMatrix, RotVectorData.values);
                    }
                    break;

            }
        }

        }


    private void calculate() {

        Long tsLong = System.currentTimeMillis() / 1000;
        String ts = tsLong.toString();
        float[] res = new float[3];

        //Database values
        Float xaxis_database;
        Float yaxis_database;
        Float zaxis_database;
        Float average_database;
        String cordTime = "";



        // initial magnetic sensor value
        xaxis = MagnetData.values[0];
        yaxis = MagnetData.values[1];
        zaxis = MagnetData.values[2];



        //Applying rotation matrix

        /*xaxis_rot= RotationMatrix[0]*MagnetData.values[0] + RotationMatrix[1]*MagnetData.values[1] +  RotationMatrix[2]*MagnetData.values[2];
        yaxis_rot = RotationMatrix[4]*MagnetData.values[0] + RotationMatrix[5]*MagnetData.values[1] +  RotationMatrix[6]*MagnetData.values[2];
        zaxis_rot = RotationMatrix[8]*MagnetData.values[0] + RotationMatrix[9]*MagnetData.values[1] + RotationMatrix[10]*MagnetData.values[2];

*/
        average = (float) Math.sqrt((Math.pow(xaxis, 2) + Math.pow(yaxis, 2) + Math.pow(zaxis, 2)));

        if(isSend){

                testData.add(average);

            isSend=false;
        }


            if(testData.size()>=3){

                testDataFinal.add(testData.get(testData.size()-1));
                testDataFinal.add(testData.get(testData.size()-2));
                testDataFinal.add(testData.get(testData.size()-3));


            }


        // for finding azimuth angle
        magValues = lowPassFilter(MagnetData.values, magValues);
        if (magValues == null) return;
        SensorManager.getOrientation(RotationMatrix, values);
        degree = (int) (Math.toDegrees(values[0]) + 360) % 360; // translate into (0, 360).
        degree = ((degree + 2)) / 5 * 5; // the value of degree is multiples of 5.
        offset = 350;
        degreeDisplay = roomDirection(degree, offset); // user-defined room direction.
        // Toast.makeText(getApplicationContext(), "degree"+ degree, Toast.LENGTH_SHORT).show();

        if (sensorData.size() < 70) {
            sensorData.add(average);
        }
        //send to database for limiting
        DBHelper.getInstance().sendDegree(degreeDisplay);

        //Dispaly the values
        //magnetismx.setText(String.valueOf(xaxis));
        magnetismy.setText(String.valueOf(yaxis));
        magnetismz.setText(Float.toString(zaxis));

        //magnetismd.setText(String.valueOf(calculateStandarddeviation(sensorData)));
       // magnetismd.setText(String.valueOf(calculateStandarddeviation(sensorData)));


        if (calcdiff) {
            TextView diff = (TextView) findViewById(R.id.calcDiff);
            TextView localize = (TextView) findViewById(R.id.localization);
            HashMap<PointF, Float> difference = new HashMap<>();
            Multimap<PointF, Float> result = HashMultimap.create();
            Multimap<PointF, Double> probResult = HashMultimap.create();

            //This hashmap is for creating array of pointf in one location id

            //HashMap<Integer,List<PointF>>find=new HashMap<>();
            //List<PointF> []tmp = new List[4];
            // for(int i = 0; i < 4; i++){
            // tmp[i] = new ArrayList<>();
            // }

            // Integer mapid;


            if (cur.isLast() == false) {

                while (cur.moveToNext()) {
                    int pos= cur.getPosition();
                    average_database= Float.valueOf(cur.getString(7));
                    mapData.add(average_database);
                    Toast.makeText(getApplicationContext(), "pos:" + pos, Toast.LENGTH_SHORT).show();
                }

                    if (islocalize) {

                        if (result.size() != 0) {
                            Map.Entry<PointF, Float> min = Collections.min(result.entries(), new Comparator<Map.Entry<PointF, Float>>() {
                                @Override
                                public int compare(Map.Entry<PointF, Float> entry1, Map.Entry<PointF, Float> entry2) {
                                    return entry1.getValue().compareTo(entry2.getValue());
                                }
                            });
                            get = min.getKey();
                            localize.setText(String.valueOf(get) + ts);
                            cordTime = String.valueOf(get) + ts;
                            int xloc = (int) get.x;
                            int yloc = (int) get.y;
                            if (isErrror) {
                                TextView E1 = (EditText) findViewById(R.id.xCord);
                                TextView E2 = (EditText) findViewById(R.id.yCord);
                                TextView errortext = (TextView) findViewById(R.id.errorText);

                                String tmp2 = E1.getText().toString();
                                String tmp3 = E2.getText().toString();
                                if (tmp2.equals("") || tmp2.equals(""))
                                    Toast.makeText(getApplicationContext(), "Please insert  Reference Co-Ordinates", Toast.LENGTH_SHORT).show();
                                int test1 = Integer.parseInt(String.valueOf(tmp2));
                                int test2 = Integer.parseInt(String.valueOf(tmp3));

                                //errorvalue=Math.sqrt((xloc-test1)*(xloc-test1) + (yloc-test2)*(yloc-test2));
                                errortext.setText(String.valueOf(errorvalue));
                            }
                        }

                    }


                }
            cur.close();


            Float temp= mapData.get(mapData.size()-1);
            for(int i=mapData.size()-1; i>0;i--){
                mapData.set(i, mapData.get(i-1));
                mapData.set(0,temp);
                if(mapData.size()>=3){
                    mapDataFinal.add(mapData.get(mapData.size()-1));
                    mapDataFinal.add(mapData.get(mapData.size()-2));
                    mapDataFinal.add(mapData.get(mapData.size()-3));
                }
                if(testDataFinal.size()>2){
                    dtw= new DTW(testDataFinal, mapDataFinal);
                    magnetismx.setText(String.valueOf(dtw));
                }
            }



            //dtw= new DTW(testDataFinal,mapData);
          //  Toast.makeText(getApplicationContext(), "DTW:" + dtw, Toast.LENGTH_SHORT).show();

                if (!recording) {
                    return;
                }
                if (stoprecord) {
                    return;
                }

                try {

                    writeToCsv(Float.toString(xaxis), Float.toString(yaxis), Float.toString(zaxis), Float.toString(0), Float.toString(0), Float.toString(0), String.valueOf(errorvalue), ts);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();

                }

            }
        }

    private int roomDirection(Integer myDegree, Integer myOffset) {
		/*
		 * define room direction as 270 degree.
		 */
        int tmp = (int)(myDegree - myOffset);
        if(tmp < 0) tmp += 360;
        else if(tmp >= 360) tmp -= 360;
        return tmp;
    }

    private float[] lowPassFilter(float[] input, float[] output) {
        /*
		 * low pass filter algorithm implement.
		 */
        if (output == null) return input;
        for (int i = 0; i < input.length; i++) {
            output[i] = output[i] + alpha * (input[i] - output[i]);
        }
        return output;
    }
    private void insertData() {
        TextView x = (EditText) findViewById(R.id.editText1);
        TextView y = (EditText) findViewById(R.id.editText2);
        TextView e = (EditText) findViewById(R.id.mapid);

        String tmp1 = e.getText().toString();
        String tmp2 = x.getText().toString();
        String tmp3 = y.getText().toString();

        if(tmp1.equals("") || tmp2.equals("") || tmp3.equals(""))
            Toast.makeText(getApplicationContext(), "Please insert Co-Ordinates First", Toast.LENGTH_SHORT).show();

        if(!tmp1.equals("") && !tmp2.equals("") && !tmp3.equals("")){
            int z1= Integer.parseInt(tmp1);
            int x1 = Integer.parseInt(tmp2);
            int y1 = Integer.parseInt(tmp3);

            Toast.makeText(getApplicationContext(),"Data insertion started", Toast.LENGTH_SHORT).show();
            DBHelper.getInstance().insert(z1, x1, y1, xaxis, yaxis, zaxis, average,calculateStandarddeviation(sensorData),0,0,0,degreeDisplay);

        }
    }


    private float calculateStandarddeviation(List<Float> sensorData) {
        int sum = 0;
        if (!sensorData.isEmpty())
            for (float data : sensorData) {
                sum += data;
            }
        double mean = sum / sensorData.size();
        double temp=0;
        for(int i=5 ; i<sensorData.size(); i++){
            float val= sensorData.get(i);
            double squrDiffToMean = Math.pow(val-mean,2);
            temp+=squrDiffToMean;

        }
        double meanofDiffs=  temp /  (sensorData.size());

       return (float) Math.sqrt(meanofDiffs);
       //magnetismd.setText(String.valueOf(sensorData.get(1)));

    }

    @Override
    protected void onPause() {
        super.onPause();
        sm.unregisterListener(this);
        sensorData.clear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sm.unregisterListener(this);
        sensorData.clear();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

}
