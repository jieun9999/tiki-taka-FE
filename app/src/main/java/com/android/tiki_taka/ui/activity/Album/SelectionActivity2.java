package com.android.tiki_taka.ui.activity.Album;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.tiki_taka.R;
import com.android.tiki_taka.utils.ImageUtils;
import com.android.tiki_taka.utils.IntentHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class SelectionActivity2 extends AppCompatActivity {
    private static final int PHOTO_PICKER_MULTI_SELECT_REQUEST_CODE = 1 ;
    private static final int REQUEST_IMAGE_CAPTURE = 2;
    private Uri imageUri; //카메라 앱이 전달받을 파일경로
    ArrayList<Uri> selectedUris = new ArrayList<>();
    int folderId; // 특정 폴더
    boolean isExistingFolder = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selection2);

        folderId = IntentHelper.getId(this);

        //카메라
        ImageView cameraIcon = findViewById(R.id.imageView30);
        cameraIcon.setOnClickListener(v -> openCamera());

        //사진
        ImageView photoIcon = findViewById(R.id.imageView35);
        photoIcon.setOnClickListener(v -> launchPhotoPickerInMultiSelectedMode());

        ImageView xBtn = findViewById(R.id.imageView40);
        xBtn.setOnClickListener(v -> dismissActivity());
    }

    // 카메라 인텐트를 생성하고, EXTRA_OUTPUT에 사진을 저장할 파일의 Uri를 전달
    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = ImageUtils.createImageFile(this); //파일 경로를 만듦
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            if (photoFile != null) {
                imageUri = FileProvider.getUriForFile(this,
                        "com.android.tiki_taka.fileprovider", //authorities 문자열이 AndroidManifest.xml 파일과 정확히 일치하는지 확인!
                        photoFile);
                // 로컬 파일 시스템에 있는 파일(photoFile)에 대한 Content Uri를 생성
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    // 갤러리 다중 선택을 허용하는 인텐트를 연다
    @SuppressLint("IntentReset")
    private void launchPhotoPickerInMultiSelectedMode(){
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(Intent.createChooser(intent, "Select Pictures"), PHOTO_PICKER_MULTI_SELECT_REQUEST_CODE);
    }

    private void dismissActivity() {
        finish();
    }

    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            selectedUris.clear();
            selectedUris.add(imageUri);

            Intent intent = new Intent(SelectionActivity2.this, StoryWritingActivity1.class);
            intent.putExtra("id", folderId);
            intent.putParcelableArrayListExtra("selectedUris", selectedUris);
            intent.putExtra("isExistingFolder", isExistingFolder);
            startActivity(intent);

        }else if (requestCode == PHOTO_PICKER_MULTI_SELECT_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            // 초기화를 여기서 수행하면 사용자가 다시 이미지 선택을 시작할 때마다 리스트가 초기화됩니다.
            selectedUris.clear();

            ClipData clipData = data.getClipData();
            if (clipData != null) {
                // 여러 사진 선택
                if (clipData.getItemCount() > 10) {
                    showToast("사진은 10장까지 선택 가능합니다.");
                }
                else {
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        Uri currentUri = clipData.getItemAt(i).getUri();
                        selectedUris.add(currentUri);  //uri를 list에 담는다.
                    }
                }

            } else if (data.getData() != null) {
                // 단일 사진 선택
                selectedUris.add(data.getData());
            }
            if(!selectedUris.isEmpty()){
                Intent intent = new Intent(SelectionActivity2.this, StoryWritingActivity1.class);
                intent.putExtra("id", folderId);
                intent.putParcelableArrayListExtra("selectedUris", selectedUris);
                intent.putExtra("isExistingFolder", isExistingFolder);
                startActivity(intent);
            }

        }
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }
}