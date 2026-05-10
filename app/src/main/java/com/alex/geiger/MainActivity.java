package com.alex.geiger;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {

    private EditText editIp;
    private EditText editSsid;
    private EditText editPassword;

    private Button btnGetCpm;
    private Button btnAuto;
    private Button btnChangeNetwork;

    private Spinner spinnerSeconds;

    private TextView txtCpm;
    private TextView txtUsv_h;

    private static final int GEIGER_PORT = 1234;

    private static final int ID_GET_CPM  = 0x01;
    private static final int ID_SET_SSID = 0x02;
    private static final int ID_SET_PWD  = 0x03;
    private static final int ID_CONNECT  = 0x04;

    private DatagramSocket rxSocket;

    private Thread rxThread;
    private Thread autoThread;

    private volatile boolean rxRunning = false;
    private volatile boolean autoRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editIp = findViewById(R.id.editIp);
        editSsid = findViewById(R.id.editSsid);
        editPassword = findViewById(R.id.editPassword);

        btnGetCpm = findViewById(R.id.btnGetCpm);
        btnAuto = findViewById(R.id.btnAuto);
        btnChangeNetwork = findViewById(R.id.btnChangeNetwork);

        spinnerSeconds = findViewById(R.id.spinnerSeconds);

        txtCpm = findViewById(R.id.txtCpm);
        txtUsv_h = findViewById(R.id.txtuSv_h);

        String[] values = {"10", "20", "30", "40", "50", "60"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                values
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSeconds.setAdapter(adapter);

        btnGetCpm.setOnClickListener(v -> {
            startRxIfNeeded();
            txtCpm.setText("Richiesta...");
            sendOneShot(ID_GET_CPM, null);
        });

        btnAuto.setOnClickListener(v -> {
            if (!autoRunning) {
                startAuto();
            } else {
                stopAuto();
            }
        });

        btnChangeNetwork.setOnClickListener(v -> changeNetwork());
    }

    private synchronized void startRxIfNeeded() {
        if (rxRunning) {
            return;
        }

        rxRunning = true;

        rxThread = new Thread(() -> {
            try {
                rxSocket = new DatagramSocket(GEIGER_PORT);
                rxSocket.setBroadcast(true);

                while (rxRunning) {
                    byte[] rx = new byte[2];
                    DatagramPacket rxPacket = new DatagramPacket(rx, rx.length);

                    rxSocket.receive(rxPacket);

                    short cpm = ByteBuffer
                            .wrap(rx)
                            .order(ByteOrder.LITTLE_ENDIAN)
                            .getShort();

                    runOnUiThread(() -> {
                        txtCpm.setText("CPM: " + cpm);

                        double usv_h = cpm * 0.0057;
                        txtUsv_h.setText(String.format("uSv/h: %.3f", usv_h));
                    });
                }

            } catch (Exception e) {
                if (rxRunning) {
                    runOnUiThread(() ->
                            txtCpm.setText("RX errore: " + e.getMessage())
                    );
                }
            } finally {
                closeRxSocket();
                rxRunning = false;
            }
        });

        rxThread.start();
    }

    private void startAuto() {
        startRxIfNeeded();

        autoRunning = true;
        btnAuto.setText("Stop");

        autoThread = new Thread(() -> {
            while (autoRunning) {
                sendOneShot(ID_GET_CPM, null);

                int seconds = getSelectedSeconds();

                try {
                    Thread.sleep(seconds * 1000L);
                } catch (InterruptedException e) {
                    break;
                }
            }

            autoRunning = false;

            runOnUiThread(() -> btnAuto.setText("Auto"));
        });

        autoThread.start();
    }

    private void stopAuto() {
        autoRunning = false;

        if (autoThread != null) {
            autoThread.interrupt();
        }

        btnAuto.setText("Auto");
    }

    private int getSelectedSeconds() {
        try {
            String value = spinnerSeconds.getSelectedItem().toString();
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 10;
        }
    }

    private void sendOneShot(int id, byte[] payload) {
        String ip = editIp.getText().toString().trim();

        if (ip.isEmpty()) {
            txtCpm.setText("Inserisci IP");
            return;
        }

        Thread txThread = new Thread(() -> {
            try {
                InetAddress address = InetAddress.getByName(ip);
                byte[] tx = buildMsg(id, payload);

                DatagramPacket packet = new DatagramPacket(
                        tx,
                        tx.length,
                        address,
                        GEIGER_PORT
                );

                synchronized (this) {
                    if (rxSocket != null && !rxSocket.isClosed()) {
                        rxSocket.send(packet);
                    } else {
                        DatagramSocket tmpSocket = new DatagramSocket();
                        tmpSocket.setBroadcast(true);
                        tmpSocket.send(packet);
                        tmpSocket.close();
                    }
                }

            } catch (Exception e) {
                runOnUiThread(() ->
                        txtCpm.setText("TX errore: " + e.getMessage())
                );
            }
        });

        txThread.start();
    }

    private void changeNetwork() {
        String ip = editIp.getText().toString().trim();
        String ssid = editSsid.getText().toString();
        String pwd = editPassword.getText().toString();

        if (ip.isEmpty()) {
            txtCpm.setText("Inserisci IP");
            return;
        }

        txtCpm.setText("Cambio rete...");

        new Thread(() -> {
            stopAutoAndJoin();
            stopRxAndJoin();

            try {
                InetAddress address = InetAddress.getByName(ip);

                DatagramSocket socket = new DatagramSocket();
                socket.setBroadcast(true);

                sendWithSocket(socket, address, ID_SET_SSID, stringPayload(ssid));
                Thread.sleep(100);

                sendWithSocket(socket, address, ID_SET_PWD, stringPayload(pwd));
                Thread.sleep(100);

                sendWithSocket(socket, address, ID_CONNECT, null);

                socket.close();

                runOnUiThread(() ->
                        txtCpm.setText("Cambio rete inviato")
                );

            } catch (Exception e) {
                runOnUiThread(() ->
                        txtCpm.setText("Change network errore: " + e.getMessage())
                );
            }
        }).start();
    }

    private void sendWithSocket(
            DatagramSocket socket,
            InetAddress address,
            int id,
            byte[] payload
    ) throws Exception {

        byte[] tx = buildMsg(id, payload);

        DatagramPacket packet = new DatagramPacket(
                tx,
                tx.length,
                address,
                GEIGER_PORT
        );

        socket.send(packet);
    }

    private byte[] stringPayload(String s) {
        byte[] raw = s.getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[raw.length + 1];

        System.arraycopy(raw, 0, out, 0, raw.length);

        out[out.length - 1] = 0; // terminatore '\0'

        return out;
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

    private void stopAutoAndJoin() {
        autoRunning = false;

        if (autoThread != null) {
            autoThread.interrupt();

            try {
                autoThread.join(1000);
            } catch (Exception ignored) {
            }

            autoThread = null;
        }

        runOnUiThread(() -> btnAuto.setText("Auto"));
    }

    private void stopRxAndJoin() {
        rxRunning = false;
        closeRxSocket();

        if (rxThread != null) {
            try {
                rxThread.join(1000);
            } catch (Exception ignored) {
            }

            rxThread = null;
        }
    }

    private synchronized void closeRxSocket() {
        if (rxSocket != null && !rxSocket.isClosed()) {
            rxSocket.close();
        }

        rxSocket = null;
    }

    @Override
    protected void onDestroy() {
        stopAutoAndJoin();
        stopRxAndJoin();
        super.onDestroy();
    }
}