package de.gryyy.psexec;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

public class DisplayOutputActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_output);

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        String message = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);
        String command = intent.getStringExtra(MainActivity.COMMAND_MESSAGE);

        TextView textView = findViewById(R.id.outputTextView);
        textView.setText(message);
        textView.setMovementMethod(new ScrollingMovementMethod());

        TextView cmdtextView = findViewById(R.id.commandTextView);
        cmdtextView.setText(command);
        cmdtextView.setMovementMethod(new ScrollingMovementMethod());

    }
}
