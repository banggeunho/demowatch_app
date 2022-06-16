package com.example.demowatch;

import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.Socket;

// 얘는 측정 시작 시 쓰레드가 돌아감녀서 계속 메세지를 보내게끔 한다.
public class SocketService {

//    EditText addressInput; //호스트 IP 입력상자
//    EditText portInput; //서버로 전송할 데이터 입력상자
//    TextView socketState;
//
//    String addr;
//    String port;
//
//    String response; //서버 응답
//
//    Handler handler = new Handler(); // 토스트를 띄우기 위한 메인스레드 핸들러 객체 생성
    static PrintWriter sendWriter;
    static BluetoothAdapter myDevice = BluetoothAdapter.getDefaultAdapter();
    static String deviceName = myDevice.getName();
    static boolean isConnected = false;
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.socket);
//
//        addressInput = findViewById(R.id.addressInput);
//        portInput = findViewById(R.id.portInput);
//        Button socketConnectBtn = findViewById(R.id.socketConnectBtn);
//
//        socketConnectBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                addr = addressInput.getText().toString().trim();
//                port = portInput.getText().toString(); //데이터
//
//                if(!isConnected) {
//                    SocketThread thread = new SocketThread(addr, port);
//                    if (thread != null) {
//                        thread.start();
//                        isConnected = true;
//                        Log.d("Socket", "Connect");
//                        Log.d("Socket", addr + " " + port);
//                    }
//                }
//            }
//        });
//    }

static class SocketThread extends Thread{

    String host; // 서버 IP
    int port; // 서버 port

    public SocketThread(String host, String port){
        this.host = host;
        this.port = Integer.parseInt(port);
    }


    @Override
    public void run() {
        try{
            Socket socket = new Socket(host, port); // 소켓 열어주기
            sendWriter = new PrintWriter(socket.getOutputStream());
            sendData("test");
//                socket.close(); // 소켓 해제

        }catch(Exception e){
            e.printStackTrace();
        }
    }

}

public static void sendData(String sendmsg) {
    if(sendWriter == null) return;
    new Thread() {
        @Override
        public void run() {
            super.run();
            try {
                if (sendmsg == null) {
                    sendWriter.println(deviceName);
                } else if (sendmsg.startsWith("Stop")) {
                    isConnected = false;
                    sendWriter.println("Stop");
                } else {
                    sendWriter.println(deviceName + "+" + sendmsg);
                }
                sendWriter.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }.start();
}
}

