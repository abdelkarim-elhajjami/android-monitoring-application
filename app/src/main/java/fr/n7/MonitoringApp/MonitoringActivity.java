package fr.n7.MonitoringApp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Scanner;

public class MonitoringActivity extends AppCompatActivity implements View.OnClickListener {

    int p;      // the number of active processes

    int UIDs[];         // array of application UIDs
    String Names[];     // array of application names
    String RSSs[];      // array of RSSs

    int clickCount[];       // array of the number of clicks corresponding to each process
    int popUp[];        // to display the handler pop-up only once

    int btnEnabledId = 0;     // to enable the update
    int btnDisabledId = 0;    // to disable the update

    Boolean stop = false; // to stop threads correctly

    final int MSG_CALCUL = 1;
    final int MSG_ARRET = 2;


    Runnable r = new Runnable() {
        public void run() {
            if (!stop) {
                LinearLayout LL = (LinearLayout) findViewById(R.id.LL);
                LL.postDelayed(r, 5000);
                for (int j = 0; j < p; j++) {
                    if (clickCount[j] == 1) {
                        processesData();
                        if (popUp[j] == 0) {
                            String messageString = "Periodic memory update is enabled";
                            btnEnabledId = j;
                            Message msg = mHandler.obtainMessage(
                                    MSG_CALCUL, (Object) messageString);
                            mHandler.sendMessage(msg);
                            popUp[j]++;
                        }
                    } else if (clickCount[j] == 2) {
                         if (popUp[j] == 1) {
                            String messageString = "Periodic memory update is disabled";
                            btnDisabledId = j;
                            Message msg = mHandler.obtainMessage(
                                    MSG_ARRET, (Object) messageString);
                            mHandler.sendMessage(msg);
                            popUp[j]++;
                         }
                    }
                }
            }
        }
    };

    final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == MSG_CALCUL) {
                Button btn = (Button) findViewById(btnEnabledId);
                btn.setBackgroundColor(Color.parseColor("#a5d152"));
                TextView txt = (TextView) findViewById(btnEnabledId + p);
                txt.setText("RSS:" + RSSs[btnEnabledId]);
                Toast.makeText(getBaseContext(),
                        "Info:" + (String) msg.obj,
                        Toast.LENGTH_LONG).show();
            }
            if (msg.what == MSG_ARRET) {
                Button btn = (Button) findViewById(btnDisabledId);
                btn.setBackgroundColor(Color.parseColor("#ee1010"));
                Toast.makeText(getBaseContext(),
                        "Info:" + (String) msg.obj,
                        Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitoring);
        Intent intent = getIntent();
    }

    public View createProcessView(int UID, String name, String RSS, int id) {
        RelativeLayout layout = new RelativeLayout(this);

        RelativeLayout.LayoutParams paramsTopLeft =
                new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);
        paramsTopLeft.addRule(RelativeLayout.ALIGN_PARENT_LEFT,
                RelativeLayout.TRUE);
        paramsTopLeft.addRule(RelativeLayout.ALIGN_PARENT_TOP,
                RelativeLayout.TRUE);
        TextView applicationTxt = new TextView(this);
        applicationTxt.setText("[" + UID + "]" + name);
        layout.addView(applicationTxt, paramsTopLeft);

        RelativeLayout.LayoutParams paramsBottomLeft =
                new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);
        paramsBottomLeft.addRule(RelativeLayout.ALIGN_PARENT_LEFT,
                RelativeLayout.TRUE);
        paramsBottomLeft.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,
                RelativeLayout.TRUE);
        TextView RSSTxt = new TextView(this);
        RSSTxt.setId(id + p);
        RSSTxt.setText("RSS:" + RSS);
        layout.addView(RSSTxt, paramsBottomLeft);

        RelativeLayout.LayoutParams paramsTopRight =
                new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);
        paramsTopRight.addRule(RelativeLayout.ALIGN_PARENT_RIGHT,
                RelativeLayout.TRUE);
        paramsTopRight.addRule(RelativeLayout.ALIGN_PARENT_TOP,
                RelativeLayout.TRUE);
        Button btn = new Button(this);
        btn.setId(id);
        btn.setText("Monitor");
        btn.setOnClickListener(this);
        layout.addView(btn, paramsTopRight);

        return layout;
    }

    public void processesData() {
        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List pkgAppsList = getPackageManager()
                .queryIntentActivities(mainIntent, 0);
        Names = new String[pkgAppsList.size()];
        UIDs = new int[pkgAppsList.size()];
        RSSs = new String[pkgAppsList.size()];
        Process process = null;
        try {
            process = new ProcessBuilder("ps").start();
        } catch (IOException e) {
            return;
        }
        InputStream in = process.getInputStream();
        Scanner scanner = new Scanner(in);
        p = 0;
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.startsWith("u0_")) {
                String[] temp = line.split(" ");
                String packageName = temp[temp.length - 1];
                for (Object object : pkgAppsList) {
                    ResolveInfo info = (ResolveInfo) object;
                    String strPackageName = info.activityInfo
                            .applicationInfo.packageName;
                    if (strPackageName.equals(packageName)) {
                        Names[p] = strPackageName;
                        UIDs[p] = info.activityInfo.applicationInfo.uid;
                        RSSs[p] = temp[temp.length - 5];
                        p++;
                    }
                }
            }
        }
    }

    @Override
    public void onClick(View v) {
        int btnId = v.getId();
        clickCount[btnId]++;
        new Thread(r).start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stop = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        processesData();
        clickCount = new int[p];
        popUp = new int[p];
        LinearLayout LL = (LinearLayout) findViewById(R.id.LL);
        for (int i = 0; i < p; ++i) {
            View v = createProcessView(UIDs[p], Names[i], RSSs[i], i);
            LL.addView(v);
        }
    }
}
