package com.alex.geiger;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.StrictMode;
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

    private static final int GEIGER_PORT = 1234;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

		editIp = findViewById(R.id.editIp);
        btnGetCpm = findViewById(R.id.btnGetCpm);
        txtCpm = findViewById(R.id.txtCpm);

        btnGetCpm.setOnClickListener(v -> getCpm());
    }

    private void getCpm() {
        String ip = editIp.getText().toString().trim();

        if (ip.isEmpty()) {
            txtCpm.setText("Inserisci IP");
            return;
        }

        txtCpm.setText("Richiesta...");

        new Thread(() -> {
            try {
                InetAddress address = InetAddress.getByName(ip);

                byte[] tx = new byte[8];

                ByteBuffer txBuf = ByteBuffer.wrap(tx);
                txBuf.order(ByteOrder.LITTLE_ENDIAN);
                txBuf.putInt(1); // id
                txBuf.putInt(0); // len

                DatagramSocket socket = new DatagramSocket(GEIGER_PORT);
                socket.setSoTimeout(3000);

                DatagramPacket packet = new DatagramPacket(
                        tx,
                        tx.length,
                        address,
                        GEIGER_PORT
                );

                socket.send(packet);

                byte[] rx = new byte[2];
                DatagramPacket rxPacket = new DatagramPacket(rx, rx.length);
                socket.receive(rxPacket);

                socket.close();

                short cpm = ByteBuffer
                        .wrap(rx)
                        .order(ByteOrder.LITTLE_ENDIAN)
                        .getShort();

                runOnUiThread(() -> txtCpm.setText("CPM: " + cpm));

            } catch (Exception e) {
                runOnUiThread(() ->
                        txtCpm.setText("Errore: " + e.getMessage())
                );
            }
        }).start();
    }
}