package com.example.tp_2_m3_2c_2024;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private MqttHandler mqttHandler;

    private TextView txtJson;
    private TextView txtModo;
    private Button cmdModo;
    public IntentFilter filterReceive;
    public IntentFilter filterConncetionLost;
    private ReceptorOperacion receiver = new ReceptorOperacion();
    private ConnectionLost connectionLost = new ConnectionLost();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_func);
        Toolbar toolbar = findViewById(R.id.mytoolbar);
        ImageButton btnSetting = findViewById(R.id.btnSetting);
        btnSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mqttHandler.disconnect();
                startActivity(new Intent(MainActivity.this, FuncActivity.class));
            }
        });

        Button btnMode = findViewById(R.id.btnModo);
        btnMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                publishMessage(MqttHandler.TOPIC_MOVIMIENTO, "CAMBIAR MODO");
            }
        });

        mqttHandler = new MqttHandler(getApplicationContext());

        connect();

        configurarBroadcastReciever();

        setSupportActionBar(toolbar);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.func), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void connect() {
        mqttHandler.connect(mqttHandler.BROKER_URL, mqttHandler.CLIENT_ID, mqttHandler.USER, mqttHandler.PASS);


        try {

            Thread.sleep(1000);
            subscribeToTopic(MqttHandler.TOPIC_MOVIMIENTO);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


    }

    private void configurarBroadcastReciever() {
        filterReceive = new IntentFilter(MqttHandler.ACTION_DATA_RECEIVE);
        filterConncetionLost = new IntentFilter(MqttHandler.ACTION_CONNECTION_LOST);

        filterReceive.addCategory(Intent.CATEGORY_DEFAULT);
        filterConncetionLost.addCategory(Intent.CATEGORY_DEFAULT);

        registerReceiver(receiver, filterReceive);
        registerReceiver(connectionLost, filterConncetionLost);

    }

    @Override
    public Intent registerReceiver(@Nullable BroadcastReceiver receiver, IntentFilter filter) {
        if (Build.VERSION.SDK_INT >= 34 && getApplicationInfo().targetSdkVersion >= 34) {
            return super.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            return super.registerReceiver(receiver, filter);
        }
    }

    @Override
    protected void onDestroy() {
        mqttHandler.disconnect();
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    private void publishMessage(String topic, String message) {
        Toast.makeText(this, "Publishing message: " + message, Toast.LENGTH_SHORT).show();
        mqttHandler.publish(topic, message);
    }

    private void subscribeToTopic(String topic) {
        Toast.makeText(this, "Subscribing to topic " + topic, Toast.LENGTH_SHORT).show();
        mqttHandler.subscribe(topic);
    }


    public class ConnectionLost extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {

            Toast.makeText(getApplicationContext(), "Conexion Perdida", Toast.LENGTH_SHORT).show();

            connect();

        }

    }

    public class ReceptorOperacion extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            TextView txtLcd = findViewById(R.id.txtDisplay);
            String msgJson = intent.getStringExtra("msgJson");
            txtJson.setText(msgJson);

            try {
                JSONObject jsonObject = new JSONObject(msgJson);
                String value = jsonObject.getString("value");
                txtLcd.setText(value+"°");
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }


    }

}



