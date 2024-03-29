package com.android.tiki_taka.ui.activity.Album;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;

import com.android.tiki_taka.R;
import com.android.tiki_taka.adapters.CommentInputAdapter;

import java.util.ArrayList;

public class StoryWritingActivity3 extends AppCompatActivity {
    RecyclerView recyclerView;
    ArrayList<Uri> selectedUris;
    CommentInputAdapter adapter;
    int scrollToPosition;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story_writing3);

        setupUIListeners();

        recyclerView = findViewById(R.id.commentRecyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        Intent intent = getIntent();
        // 동영상 인지 이미지 인지 확인하고 분기해서 렌더링
        if (intent != null) {
            TextView imageNumView = findViewById(R.id.textView35);

            if(intent.hasExtra("selectedImages")){
                selectedUris = intent.getParcelableArrayListExtra("selectedImages");
                String imageNum = selectedUris.size() + "장";
                imageNumView.setText(imageNum);

            } else if (intent.hasExtra("selectedVideo")) {
                selectedUris = intent.getParcelableArrayListExtra("selectedVideo");
                imageNumView.setText("동영상");

            }
            adapter = new CommentInputAdapter(selectedUris);
            recyclerView.setAdapter(adapter);
            scrollToPosition = intent.getIntExtra("scrollToPosition", -1);
            setScrollPosition(scrollToPosition);
        }

    }

    private void setScrollPosition(int position){
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                recyclerView.scrollToPosition(position);
            }
        },200);
    }

    private void setupUIListeners(){
        TextView cancelBtn = findViewById(R.id.textView33);
        TextView saveBtn = findViewById(R.id.textView34);
        cancelBtn.setOnClickListener(v -> finish());
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<String> comments = adapter.collectCommentText();
                Intent resultIntent = new Intent();
                resultIntent.putStringArrayListExtra("comments", comments);
                //cardID는 php 코드에서 처리함
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });

    }
}