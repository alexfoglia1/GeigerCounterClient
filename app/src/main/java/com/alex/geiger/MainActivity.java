package com.alex.geiger;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MainActivity extends AppCompatActivity {

    private EditText editIp;
    private Button btnGetCpm;
    private TextView txtCpm;
    private TextView txtUsv_h;

    private static final int GEIGER_PORT = 1234;

    private static final int ID_GET_CPM  = 0x01;
    private static final int ID_SET_SSID = 0x02;
    private static final int ID_SET_PWD  = 0x03;
    private static final int ID_CONNECT  = 0x04;

    private DatagramSocket socket;
    private Thread rxThread;
    private volatile boolean running = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editIp = findViewById(R.id.editIp);
        btnGetCpm = findViewById(R.id.btnGetCpm);
        txtCpm = findViewById(R.id.txtCpm);
        txtUsv_h = findViewById(R.id.txtuSv_h);

        try {
            socket = new DatagramSocket(GEIGER_PORT);
            socket.setBroadcast(true);
        } catch (Exception e) {
            txtCpm.setText("Socket error: " + e.getMessage());
            return;
        }

        startRxThread();

        btnGetCpm.setOnClickListener(v -> {
            txtCpm.setText("Richiesta...");
            sendOneShot(ID_GET_CPM, null);
        });
    }

    private void startRxThread() {
        rxThread = new Thread(() -> {
            while (running) {
                try {
                    byte[] rx = new byte[2];
                    DatagramPacket rxPacket = new DatagramPacket(rx, rx.length);

                    socket.receive(rxPacket);

                    short cpm = ByteBuffer
                            .wrap(rx)
                            .order(ByteOrder.LITTLE_ENDIAN)
                            .getShort();

                    runOnUiThread(() -> {
                        txtCpm.setText("CPM: " + cpm);

                        double usv_h = cpm * 0.0057; // valore indicativo, cambia col tuo tubo
                        txtUsv_h.setText(String.format("µSv/h: %.3f", usv_h));
                    });

                } catch (Exception e) {
                    if (running) {
                        runOnUiThread(() ->
                                txtCpm.setText("RX errore: " + e.getMessage())
                        );
                    }
                }
            }
        });

        rxThread.start();
    }

    private void sendOneShot(int id, byte[] payload) {
        String ip = editIp.getText().toString().trim();

        if (ip.isEmpty()) {
            txtCpm.setText("Inserisci IP");
            return;
        }

        new Thread(() -> {
            try {
                InetAddress address = InetAddress.getByName(ip);
                byte[] tx = buildMsg(id, payload);

                DatagramPacket packet = new DatagramPacket(
                        tx,
                        tx.length,
                        address,
                        GEIGER_PORT
                );

                socket.send(packet);

            } catch (Exception e) {
                runOnUiThread(() ->
                        txtCpm.setText("TX errore: " + e.getMessage())
                );
            }
        }).start();
    }

    private byte[] buildMsg(int id, byte[] payload) {
        int payloadSize = payload == null ? 0 : payload.length;
        byte[] tx = new byte[8 + payloadSize];

        ByteBuffer txBuf = ByteBuffer.wrap(tx);
        txBuf.order(ByteOrder.LITTLE_ENDIAN);

        txBuf.putInt(id);
        txBuf.putInt(payloadSize);

        if (payload != null) {
            txBuf.put(payload);
        }

        return tx;
    }

    @Override
    protected void onDestroy() {
        running = false;

        if (socket != null && !socket.isClosed()) {
            socket.close();
        }

        super.onDestroy();
    }
}