package com.android.tiki_taka.ui.activity.Chat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.tiki_taka.R;
import com.android.tiki_taka.adapters.MessageAdapter;
import com.android.tiki_taka.listeners.DateMarkerListener;
import com.android.tiki_taka.listeners.MessageListener;
import com.android.tiki_taka.models.dto.HomeProfiles;
import com.android.tiki_taka.models.dto.Message;
import com.android.tiki_taka.models.dto.PartnerProfile;
import com.android.tiki_taka.models.dto.UserProfile;
import com.android.tiki_taka.services.ChatApiService;
import com.android.tiki_taka.services.ChatClient;
import com.android.tiki_taka.services.ProfileApiService;
import com.android.tiki_taka.utils.RetrofitClient;
import com.android.tiki_taka.utils.SharedPreferencesHelper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class ChatActivity extends AppCompatActivity implements DateMarkerListener {
    ChatApiService service;
    ProfileApiService profileService;
    private int currentUserId;
    private ChatClient chatClient;
    private RecyclerView recyclerView;
    private MessageAdapter messageAdapter;
    private List<Message> messages = new ArrayList<>();
    private int chatRoomId;
    String myProfileImg;
    String partnerProfileImg;
    private static final int REQUEST_CODE_POST_NOTIFICATION = 101; // 요청 코드는 상수로 정의

    //네트워크 작업(채팅)을 수행할 때 주의해야 할 중요한 점 중 하나는 네트워크 작업을 메인 스레드에서 실행하지 않아야 한다는 것!!!

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        setupNetworkAndRetrieveIds();

        // 1. db에서 먼저 과거 메세지 이력을 가져옴
        setupLayoutManager();
        setupAdapter();
        loadMessages();
        getHomeProfile(currentUserId);

        //2. 서버 연결 및 수신 & 전송 준비
        connectToChatServer();
        setupSendButtonClickListener();
        setupToolBarListeners();

        // 알림 권한을 요청하는 메소드 호출
        requestNotificationPermission();
    }

    private void setupNetworkAndRetrieveIds() {
        Retrofit retrofit = RetrofitClient.getClient();
        service = retrofit.create(ChatApiService.class);
        profileService =retrofit.create(ProfileApiService.class);
        currentUserId = SharedPreferencesHelper.getUserId(this);
        chatRoomId = SharedPreferencesHelper.getRoomId(this);
    }

    private void setupLayoutManager(){
        recyclerView = findViewById(R.id.chatRecyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
    }

    private void setupAdapter(){
        messageAdapter = new MessageAdapter(messages);
        recyclerView.setAdapter(messageAdapter);
        scrollToBottom();
    }


    private void loadMessages(){
        service.getMessages(chatRoomId).enqueue(new Callback<List<Message>>() {
            @Override
            public void onResponse(Call<List<Message>> call, Response<List<Message>> response) {
                if (response.isSuccessful() && response.body() != null) {   // 요청 성공 + 응답 존재
                    handleLoadingMessages(response);

                    //초기에 아이템이 렌더링 된 후에 스크롤 맨 아래로 이동
                    scrollToBottom();
                }else {
                    Log.e("Error", "서버에서 불러오기에 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<Message>> call, Throwable t) {
                Log.e("Network Error", "네트워크 호출 실패: " + t.getMessage());
            }
        });
    }

    private void scrollToBottom() {
        if (messageAdapter.getItemCount() > 0) {
            recyclerView.scrollToPosition(messageAdapter.getItemCount() - 1);
        }
    }

    // 상대방과 나의 프로필 이미지를 초기화할때 가져와서, 이미 할당해둔 상태에서
    // updateUIFromDB()나 updateUIFromInputBox()가 실행될때 인자로 넣어준다
    private void getHomeProfile(int currentUserId){
        // 1. 유저 프로필 정보 가져오기
        Call<HomeProfiles> call = profileService.getHomeProfile(currentUserId);
        call.enqueue(new Callback<HomeProfiles>() {
            @Override
            public void onResponse(Call<HomeProfiles> call, Response<HomeProfiles> response) {
                processHomeProfileResponse(response);

            }

            @Override
            public void onFailure(Call<HomeProfiles> call, Throwable t) {
                Log.e("Network Error", "네트워크 호출 실패: " + t.getMessage());
            }
        });
    }

    private void processHomeProfileResponse(Response<HomeProfiles> response){
        if (response.isSuccessful() && response.body() != null) {
            //서버에서 홈프로필 정보를 가져옴
            HomeProfiles homeProfiles = response.body();
            // 유저 프로필 정보 처리
            UserProfile userProfile = homeProfiles.getUserProfile();
            // 파트너 프로필 정보 처리
            PartnerProfile partnerProfile = homeProfiles.getPartnerProfile();

            myProfileImg = userProfile.getProfileImage();
            partnerProfileImg = partnerProfile.getProfileImage();

        } else {
            Log.e("Error", "서버에서 불러오기에 실패: " + response.code());
        }
    }


    private void handleLoadingMessages(Response<List<Message>> response){
        List<Message> messages = response.body();
        if(messages != null){
            messageAdapter.setData(messages, currentUserId);
        }else {
            Log.e("Error", "messages null 입니다");
        }

    }

    // new Thread()를 사용하여 connectToChatServer 메서드 내의 작업을 새로운 스레드에서 실행함으로써
    // NetworkOnMainThreadException 오류를 피하기
    private void connectToChatServer(){
        new Thread(()->{
            try {
                // 웹 서버 주소로 접속함
                chatClient = new ChatClient("52.79.41.79", 1234);

                // 서버가 쉐어드에 저장되어 있는 userId에 접근하지 못하기 때문에, 서버에게 직접 id를 전송해야 함
                chatClient.sendUserId(currentUserId);

                // 액티비티 흐름 중간에 chatClient가 생성된 후 인터페이스가 구현되어야 함
                // 이전에는 액티비티 자체에 구현하였는데 그것은 액티비티 초기화시에 구현되어야 하므로 문제가 여기에는 적절하지 않음
                chatClient.setMessageListener(new MessageListener() {
                    @Override
                    public void onMessageReceived(String message) {
                        // 상대방의 메세지 띄우기
                        runOnUiThread(() -> updateUIFromDB(message, partnerProfileImg));
                    }
                });

                chatClient.listenMessage();
                // 서버로부터 오는 메시지를 지속적으로 수신
                // 수신받은 메세지를 'updateUI' 메세드를 통해 실시간으로 RecyclerView에 메세지를 추가함

            } catch (IOException e) {
                Log.e("ChatClient", "Initialization failed.", e);
                e.printStackTrace();
            }
        }).start();
        // 스레드를 실행시키면, 그 스레드를 통해서 단 한번 서버 연결을 시도하고, 서버에게 id를 보냄

    }

    private void setupSendButtonClickListener(){
        TextView sendButton = findViewById(R.id.send_view);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    sendMessage();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void sendMessage() throws JSONException {
        EditText inputChatView = findViewById(R.id.inputbox_chat);
        String inputText = inputChatView.getText().toString();

        JSONObject messageObject = new JSONObject();
        messageObject.put("senderId", currentUserId);
        messageObject.put("message", inputText);
        messageObject.put("chatRoomId", chatRoomId);

        // JSON 객체를 문자열로 변환
        String message = messageObject.toString();

        // 안드로이드에서 네트워크 연결과 같은 입출력(I/O) 작업은 메인 스레드(또는 UI 스레드)에서 실행해서는 안 됩니다
        // 서버 소켓을 통해 메세지를 보낼 때, 메세지 객체가 아니라 문자열로 전송하는 것이 일반적
        // 백그라운드 스레드에서 소켓을 통해 메세지 전송
        new Thread(() ->{
            // 현재 시간 저장
            String createdAt = nowDateFormatter();

            // 자신이 보낸 메세지 ui 업데이트
            runOnUiThread(() -> {
                        inputChatView.setText("");
                        updateUIFromInputBox(createdAt, inputText, myProfileImg);
                    }
            );

            // 서버 소켓에 메세지 전송
            chatClient.sendMessage(message);

        }).start();

    }

    private String nowDateFormatter(){
        LocalDateTime currentTime = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            currentTime = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return currentTime.format(formatter);
        }

        return "";
    }

    private void updateUIFromDB(String jsonMessage, String partnerProfileImg) {

        // 서버에서온 message json 문자열 파싱
        JsonObject messageObject = JsonParser.parseString(jsonMessage).getAsJsonObject();

        String createdAt = messageObject.get("createdAt").getAsString();
        String content = messageObject.get("content").getAsString();

        // 리사이클러뷰에 메세지 추가
        Message newMessage = new Message(partnerProfileImg, createdAt, content);
        messageAdapter.addMessage(newMessage, currentUserId, chatRoomId,this);
        recyclerView.scrollToPosition(messageAdapter.getItemCount()-1);
    }

    private void updateUIFromInputBox(String createdAt, String message, String myProfileImg){

        // 리사이클러뷰에 메세지 추가
        Message newMessage = new Message(myProfileImg, createdAt, message);
        newMessage.setSenderId(currentUserId); // 내가 보낸 메세지 설정
        messageAdapter.addMessage(newMessage, currentUserId, chatRoomId, this);
        recyclerView.scrollToPosition(messageAdapter.getItemCount()-1);
    }

    @Override
    public void onMessageAdded(Message dateMarker) {
        service.saveDateMarker(dateMarker).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                ProcessResponse(response);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable throwable) {
                Log.e("Network Error", "네트워크 호출 실패: " + throwable.getMessage());
            }
        });
    }

    private void ProcessResponse( Response<ResponseBody> response){
        if(response.isSuccessful()){
            handleResponse(response);

        } else {
            Log.e("Error", "서버에서 불러오기에 실패: " + response.code());
        }
    }

    private void handleResponse(Response<ResponseBody> response){
        if (response.isSuccessful()) {
            try {
                String message = parseResponseData(response);
                Log.d("success",message);

            } catch (JSONException | IOException e) {
                e.printStackTrace();
                Log.e("Error","catch문 오류 발생");
            }
        } else {
            Log.e("Error","서버 응답 오류");
        }
    }

    private String parseResponseData(Response<ResponseBody> response) throws JSONException, IOException {
        String responseJson = response.body().string();
        JSONObject jsonObject = new JSONObject(responseJson);
        return jsonObject.getString("message");
    }

    private void setupToolBarListeners(){
        ImageView cancelBtn = findViewById(R.id.imageView36);
        cancelBtn.setOnClickListener( v -> {

            // 백그라운드 작업을 종료 후 액티비티 종료
            // chatClient가 null이 되거나 다른 문제가 발생하는 상황을 방지할 수 있음

            new Thread(() -> {
                try {

                    if(chatClient != null){
                        chatClient.closeConnection();
                        Log.d("클라이언트 연결 종료", "사용자" +currentUserId );
                    }

                } catch (IOException e) {
                    e.printStackTrace();

                }finally {
                    runOnUiThread(()-> finish());
                }

            }).start();

        });
    }

    // 채팅 액티비티에서 아예 나가게 되면 리소스를 정리
    // 메인 스레드에서 네트워크 연결과 같은 블로킹(시간이 오래 걸리는) 작업을 진행하는 것은 피해야 함
    // 대신 보조 스레드에서 열린 네트워크 연결, 파일 핸들, 스트림 등을 적절하게 닫아주는 것이 중요
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(chatClient != null){
            new Thread(()->{
                try {
                    chatClient.closeConnection();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private void requestNotificationPermission() {
        // Android 13 (API level 33) 이상에서만 알림 권한 요청이 필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // 권한이 이미 부여되었는지 확인
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // 권한이 없다면 사용자에게 요청
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_CODE_POST_NOTIFICATION);
            }
            // 권한이 이미 있다면 추가 작업이 필요 없습니다
        }
        // API level 33 미만에서는 별도의 권한 요청 없이 알림을 보낼 수 있습니다
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_POST_NOTIFICATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 사용자가 권한을 부여했다면 여기에서 알림 관련 추가 작업을 수행할 수 있습니다
            } else {
                // 사용자가 권한을 거부했다면, 권한 없이는 알림 기능을 사용할 수 없음을 알릴 수 있습니다
            }
        }
    }
}