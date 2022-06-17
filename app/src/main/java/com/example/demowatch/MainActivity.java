package com.example.demowatch;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.demowatch.R;

import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SensorEventListener  {
    // Debugging
    private static final String TAG = "BluetoothMain";

    // * UI * //
    public Button startBtn, stopBtn, socketBtn;
    public int level,saveN;
    public TextView measureState, socketState;
    public EditText addressInput; //호스트 IP 입력상자
    public EditText portInput; //서버로 전송할 데이터 입력상자

    // * Time * //
    private boolean thread_state;


    // * Sensor * //
    private SensorManager manager;
    private Sensor mHeartRate, mGyro, mAccel, mStep;
    private ArrayList<String> sensorData, copyData;
    public String fileName;
    private int fileVer;

    public SocketService.SocketThread thread;
    public static boolean isConnected = false;
    public String addr;
    public String port;


    // * Message code *
    public static final int REQUEST_ENABLE_BT = 1; // 블루투스 활성화 요청 메시지
    public static final int DISCOVERY_REQUEST = 2; // 기기가 검색될 수 있도록 활성화 요청 메시지
    public static final int PERMISSIONS_REQUEST = 1; // 권한 요청

    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3; // watch는 필요없음. 오직 read만 함.
    public static final int MESSAGE_DEVICE_OBJECT = 4;
    public static final int MESSAGE_TOAST = 5;

    public static final String DEVICE_OBJECT = "device_name";
    public static final String TOAST = "toast";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // 화면 켜두기
        doCheckPermission();
        init();
        stopBtn.setClickable(false);



//        initBLE();

        startBtn.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view) {
                start_sensor();
            }
        });

        stopBtn.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view) {
                Log.d(TAG, "Stop button");
                stop_sensor();
            }
        });

//        socketBtn.setOnClickListener(new View.OnClickListener(){
//            public void onClick(View view) {
//                Intent intent = new Intent(getApplicationContext(), SocketService.class);
//                startActivity(intent);
//            }
//        });
    }

    public void doCheckPermission() {
        // 권한 X
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.BODY_SENSORS, Manifest.permission.ACTIVITY_RECOGNITION}, PERMISSIONS_REQUEST);

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode){
            case PERMISSIONS_REQUEST :
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

                }
                else {

                }
                break;
        }
    }

    public void init() { // initialize & get sensor
        // for UI
        startBtn = (Button) findViewById(R.id.startBtn);
        stopBtn = (Button) findViewById(R.id.stopBtn);
//        socketBtn = (Button) findViewById(R.id.socketBtn);
        measureState = (TextView) findViewById(R.id.measureState);
        socketState = (TextView) findViewById(R.id.socketState);
        addressInput = findViewById(R.id.addressInput);
        portInput = findViewById(R.id.portInput);

//        addressInput = (EditText) findViewById(R.id.addressInput);
//        portInput = (EditText) findViewById(R.id.portInput);
        saveN = 0;

        // for sensor
        manager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mHeartRate = manager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        mGyro = manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mAccel = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mStep = manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        sensorData = new ArrayList<String>();
        copyData = new ArrayList<String>();

        thread_state = false;
        fileVer = 1;
    }


    public void start_sensor(){
        saveN = 0;
        register();
        addr = addressInput.getText().toString().trim();
        port = portInput.getText().toString(); //데이터
        measureState.setText("Measuring : Running!");
        startBtn.setClickable(false);
        stopBtn.setClickable(true);

        thread = new SocketService.SocketThread(addr, port);
        if (thread != null) {
            thread.start();
            isConnected = true;
            socketState.setText("Socket : Connect");
            Log.d("Socket", "Connect");
            Log.d("Socket", addr + " " + port);
        }
    }

    public void stop_sensor(){
        measureState.setText("Measuring : Not run");
        socketState.setText("Socket : Not connect");
        startBtn.setClickable(true);
        stopBtn.setClickable(false);
        sensorData.clear();
        SocketService.sendData("Stop Measuring");
        unregister();
    }

    public void register() { // register listener
        manager.registerListener(this, mHeartRate, SensorManager.SENSOR_DELAY_GAME);
        manager.registerListener(this, mGyro, SensorManager.SENSOR_DELAY_GAME);
        manager.registerListener(this, mAccel, SensorManager.SENSOR_DELAY_GAME);
        manager.registerListener(this, mStep, SensorManager.SENSOR_DELAY_GAME);

    }

    public void unregister() { // unregister listener
        manager.unregisterListener(this);
        sensorData.clear();
    }


    // Sensor work //
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }
    public final void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        int tag = level;

        long date = System.currentTimeMillis();
        String time = Long.toString(date);

        if (sensor.getType() == Sensor.TYPE_HEART_RATE) {
//            sensorData.add(tag+"+"+"HR+"+time+"+"+event.values[0]);
            SocketService.sendData("HeartR+"+time+"+"+event.values[0]);
        }

        if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
//            sensorData.add(tag+"+"+"GX+"+time+"+"+event.values[0]);
//            sensorData.add(tag+"+"+"GY+"+time+"+"+event.values[1]);
//            sensorData.add(tag+"+"+"GZ+"+time+"+"+event.values[2]);
//            SocketService.sendData("GX+"+time+"+"+event.values[0]);
//            SocketService.sendData("GY+"+time+"+"+event.values[1]);
//            SocketService.sendData("GZ+"+time+"+"+event.values[2]);
        }

        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
//            sensorData.add(tag+"+"+"AX+"+time+"+"+event.values[0]);
//            sensorData.add(tag+"+"+"AY+"+time+"+"+event.values[1]);
//            sensorData.add(tag+"+"+"AZ+"+time+"+"+event.values[2]);
//            SocketService.sendData("GX+"+time+"+"+event.values[0]);
//            SocketService.sendData("GY+"+time+"+"+event.values[1]);
//            SocketService.sendData("GZ+"+time+"+"+event.values[2]);
        }

        if (sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
//            sensorData.add(tag+"+"+"SC+"+time+"+"+event.values[0]);
            SocketService.sendData("StepC+"+time+"+"+event.values[0]);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
//        if (controller != null)
//            controller.stop();
//        unregister();
//        if(timeT != null){
//            timeT = null;
//        }
    }

//    @Override
//    public void onStop() {
//        super.onStop();
//        try {
//            SocketService.SocketThread.socket.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

}