package de.gryyy.psexec;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivity extends AppCompatActivity
{
    public static final String EXTRA_MESSAGE = "de.gryyy.psexec.MESSAGE";
    public static final String COMMAND_MESSAGE = "de.gryyy.psexec.COMMAND_MESSAGE";
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences sharedPref = getSharedPreferences("psexecPrefs", MODE_PRIVATE);
        boolean firstStart = sharedPref.getBoolean("firstStart", true);

        // only run setup on first start
        if (firstStart) {
            runSetup();
        }
    }

    public static String readFully(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length = 0;
        while ((length = is.read(buffer)) != -1) {
            baos.write(buffer, 0, length);
        }
        return baos.toString("UTF-8");
    }

    // unzips a zip file to a target directory
    public static void unzip(File zipFile, File targetDirectory) throws IOException {
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)));
        try {
            ZipEntry ze;
            int count;
            byte[] buffer = new byte[32000000];
            while ((ze = zis.getNextEntry()) != null) {
                File file = new File(targetDirectory, ze.getName());
                File dir = ze.isDirectory() ? file : file.getParentFile();
                if (!dir.isDirectory() && !dir.mkdirs())
                    throw new FileNotFoundException("Failed to ensure directory: " + dir.getAbsolutePath());
                if (ze.isDirectory())
                    continue;
                FileOutputStream fout = new FileOutputStream(file);
                try {
                    while ((count = zis.read(buffer)) != -1)
                        fout.write(buffer, 0, count);
                } finally {
                    fout.close();
                }
            }
        } finally {
            // close the input stream if no error occurs
            zis.close();
        }
    }

    public void runSetup()
    {
        Context context = getApplicationContext();
        CharSequence text = "Running Setup...";
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();

        File inZip = new File(getCacheDir() + "/files.zip");

        try {
            InputStream is = getAssets().open("files/files.zip");
            byte[] buffer = new byte[32000000];
            is.read(buffer);
            is.close();


            FileOutputStream fos = new FileOutputStream(inZip);
            fos.write(buffer);
            fos.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        File outDir = getApplicationContext().getFilesDir();

        // unzip files from assets
        try {
            unzip(inZip, outDir);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // make binaries executable
        try {
            Os.chmod(getApplicationContext().getFilesDir().getPath() + "/bin/python3", 0700);
            Os.chmod(getApplicationContext().getFilesDir().getPath() + "/bin/standalone_python.sh", 0700);
        } catch (ErrnoException e) {
            e.printStackTrace();
        }

        // clear cache
        File dir = getCacheDir();
        if (dir.isDirectory())
        {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++)
            {
                new File(dir, children[i]).delete();
            }
        }

        // set fist start to false so we wont run setup again
        SharedPreferences sharedPref = getSharedPreferences("psexecPrefs", MODE_PRIVATE);
        sharedPref.edit().putBoolean("firstStart", false).apply();
    }

    public void runExec(View view)
    {
        EditText pcnameEditText = findViewById(R.id.pcname);
        String pcname = pcnameEditText.getText().toString();

        EditText usernameEditText = findViewById(R.id.username);
        String username = usernameEditText.getText().toString();

        EditText passwordEditText = findViewById(R.id.password);
        String password = passwordEditText.getText().toString();

        EditText programEditText = findViewById(R.id.programname);
        String programnametxt = programEditText.getText().toString();

        EditText argsEditText = findViewById(R.id.args);
        String argstxt = argsEditText.getText().toString();

        byte[] prgnamedata = programnametxt.getBytes(StandardCharsets.UTF_8);
        byte[] argsdata = argstxt.getBytes(StandardCharsets.UTF_8);

        String programname = Base64.encodeToString(prgnamedata, Base64.DEFAULT);
        String args = Base64.encodeToString(argsdata, Base64.DEFAULT);

        CheckBox sysaccBox = findViewById(R.id.usesysacc);
        String usesysacc = sysaccBox.isChecked() ? "True" : "False";

        CheckBox interactBox = findViewById(R.id.interactbox);
        String interact = interactBox.isChecked() ? "True" : "False";

        EditText interactidEditText = findViewById(R.id.interactid);
        String interactid = interactidEditText.getText().toString();

        CheckBox encryptBox = findViewById(R.id.useencryption);
        String encrypt = encryptBox.isChecked() ? "True" : "False";

        CheckBox asynchBox = findViewById(R.id.asynch);
        String asynch = asynchBox.isChecked() ? "True" : "False";

        String command = "sh " + getApplicationContext().getFilesDir().getPath() + "/bin/standalone_python.sh " + getApplicationContext().getFilesDir().getPath() + "/pypsexecparser.py " + pcname + " " + username + " " + password + " " + programname + " --arguments " + args + " --s " + usesysacc + " --i " + interact + " --isession " + interactid + " --encrypt " + encrypt + " --asynch " + asynch;

        String output;

        // try to run the command and get the output
        try {
            Process execute = Runtime.getRuntime().exec(command);
            output = readFully(execute.getInputStream()) + readFully(execute.getErrorStream());
        } catch (Exception e) { throw new RuntimeException(e); }


        // start the new intent and pass the output to it
        Intent intent = new Intent(this, DisplayOutputActivity.class);
        intent.putExtra(EXTRA_MESSAGE, output);
        intent.putExtra(COMMAND_MESSAGE, command);
        startActivity(intent);
    }

    public void saveButton(View view)
    {
        EditText saveNameEditText = findViewById(R.id.saveloadtext);
        String saveName = saveNameEditText.getText().toString();

        EditText pcnameEditText = findViewById(R.id.pcname);
        String pcname = pcnameEditText.getText().toString();

        EditText usernameEditText = findViewById(R.id.username);
        String username = usernameEditText.getText().toString();

        EditText passwordEditText = findViewById(R.id.password);
        String password = passwordEditText.getText().toString();

        EditText programEditText = findViewById(R.id.programname);
        String programname = programEditText.getText().toString();

        EditText argsEditText = findViewById(R.id.args);
        String args = argsEditText.getText().toString();

        CheckBox sysaccBox = findViewById(R.id.usesysacc);
        String usesysacc = sysaccBox.isChecked() ? "True" : "False";

        CheckBox interactBox = findViewById(R.id.interactbox);
        String interact = interactBox.isChecked() ? "True" : "False";

        EditText interactidEditText = findViewById(R.id.interactid);
        String interactid = interactidEditText.getText().toString();

        CheckBox encryptBox = findViewById(R.id.useencryption);
        String encrypt = encryptBox.isChecked() ? "True" : "False";

        CheckBox asynchBox = findViewById(R.id.asynch);
        String asynch = asynchBox.isChecked() ? "True" : "False";

        SharedPreferences sharedPref = getSharedPreferences("psexecPrefs", MODE_PRIVATE);

        String splitstring = "/-/spliz/-/";
        String toSave = pcname + splitstring + username + splitstring + password + splitstring + programname + splitstring + args + splitstring + usesysacc + splitstring + interact + splitstring + interactid + splitstring + encrypt + splitstring + asynch;

        sharedPref.edit().putString(saveName, toSave).apply();
    }

    public void loadButton(View view)
    {
        EditText saveNameEditText = findViewById(R.id.saveloadtext);
        String saveName = saveNameEditText.getText().toString();

        SharedPreferences sharedPref = getSharedPreferences("psexecPrefs", MODE_PRIVATE);

        String loadedData = sharedPref.getString(saveName, "");

        String[] DataArray = loadedData.split("/-/spliz/-/", 0);

        Log.d("loadsave", Arrays.toString(DataArray));

        // restore the status of all inputs
        if (DataArray.length == 10) {
            EditText pcnameEditText = findViewById(R.id.pcname);
            pcnameEditText.setText(DataArray[0]);

            EditText usernameEditText = findViewById(R.id.username);
            usernameEditText.setText(DataArray[1]);

            EditText passwordEditText = findViewById(R.id.password);
            passwordEditText.setText(DataArray[2]);

            EditText programEditText = findViewById(R.id.programname);
            programEditText.setText(DataArray[3]);

            EditText argsEditText = findViewById(R.id.args);
            argsEditText.setText(DataArray[4]);

            CheckBox sysaccBox = findViewById(R.id.usesysacc);
            sysaccBox.setChecked(Boolean.parseBoolean(DataArray[5]));

            CheckBox interactBox = findViewById(R.id.interactbox);
            interactBox.setChecked(Boolean.parseBoolean(DataArray[6]));

            EditText interactidEditText = findViewById(R.id.interactid);
            interactidEditText.setText(DataArray[7]);

            CheckBox encryptBox = findViewById(R.id.useencryption);
            encryptBox.setChecked(Boolean.parseBoolean(DataArray[8]));

            CheckBox asynchBox = findViewById(R.id.asynch);
            asynchBox.setChecked(Boolean.parseBoolean(DataArray[9]));
        }
        else {
            Context context = getApplicationContext();
            CharSequence text = "Save \"" + saveName + "\" not found!";
            int duration = Toast.LENGTH_SHORT;

            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        }
    }
}

