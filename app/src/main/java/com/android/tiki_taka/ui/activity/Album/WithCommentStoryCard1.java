package com.android.tiki_taka.ui.activity.Album;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.tiki_taka.R;
import com.android.tiki_taka.adapters.CommentAdapter;
import com.android.tiki_taka.listeners.DeleteCommentListener;
import com.android.tiki_taka.models.dtos.CommentItem;
import com.android.tiki_taka.models.dtos.StoryCard;
import com.android.tiki_taka.models.responses.ApiResponse;
import com.android.tiki_taka.services.StoryApiService;
import com.android.tiki_taka.utils.ImageUtils;
import com.android.tiki_taka.utils.IntentHelper;
import com.android.tiki_taka.utils.RetrofitClient;
import com.android.tiki_taka.utils.SharedPreferencesHelper;
import com.android.tiki_taka.utils.TimeUtils;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class WithCommentStoryCard1 extends AppCompatActivity implements DeleteCommentListener {
    StoryApiService service;
    int userId;
    int cardId;
    CommentAdapter adapter;
    ArrayList<CommentItem> commentList;
    RecyclerView recyclerView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_with_comment_story_card1);

        Retrofit retrofit = RetrofitClient.getClient();
        service = retrofit.create(StoryApiService.class);
        userId = SharedPreferencesHelper.getUserId(this);
        cardId = IntentHelper.getId(this);

        loadCardDetails();

        recyclerView = findViewById(R.id.commentRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 어댑터 설정 이후 데이터 로드
        // 1.빈 어댑터로 초기화
        commentList = new ArrayList<>();
        adapter = new CommentAdapter(commentList,this);
        recyclerView.setAdapter(adapter);

        // 2.데이터를 비동기적으로 가져오는 메서드 호출
        loadComments();

        TextView sendCommentButton = findViewById(R.id.send_comment_view);
        sendCommentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                postComment();
            }
        });

    }
    private void loadCardDetails(){
        service.getCardDetails(cardId).enqueue(new Callback<StoryCard>() {
            @Override
            public void onResponse(Call<StoryCard> call, Response<StoryCard> response) {
                try {
                    processCardDetailsResponse(response);
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onFailure(Call<StoryCard> call, Throwable t) {
                Log.e("Network Error", "네트워크 호출 실패: " + t.getMessage());
            }
        });
    }

    private void processCardDetailsResponse(Response<StoryCard> response) throws ParseException {
        if(response.isSuccessful() && response.body() != null){
            StoryCard storyCard = response.body();

            // 상단바
            ImageView profileImgView = findViewById(R.id.imageView41);
            TextView nameView = findViewById(R.id.textView37);
            TextView timeView = findViewById(R.id.textView38);
            ImageUtils.loadImage(storyCard.getUserProfile(), profileImgView, this);
            nameView.setText(storyCard.getUserName());
            String outputDateString = TimeUtils.convertDateString(storyCard.getCreatedAt());
            timeView.setText(outputDateString);

            // 본문
            ImageView cardImgView = findViewById(R.id.imageview);
            ImageView myLikesView = findViewById(R.id.imageView31);
            ImageView partnerLikesView = findViewById(R.id.imageView32);
            Log.d("storyCard.getImage()", storyCard.getImage());
            Log.d(" cardImgView", String.valueOf(cardImgView));
            ImageUtils.loadImage(storyCard.getImage(), cardImgView, this);

            int myLikes = storyCard.getUserGood();
            int partnerLikes = storyCard.getPartnerGood();
            if(myLikes == 0){
                ImageUtils.loadDrawableIntoView(this, myLikesView, "akar_icons_heart");
            }
            if(partnerLikes == 0){
                partnerLikesView.setVisibility(View.GONE);
            }

        }else {
            Log.e("Error", "서버에서 불러오기에 실패: " + response.code());
        }
    }

    private void loadComments(){
        service.getComments(cardId).enqueue(new Callback<List<CommentItem>>() {
            @Override
            public void onResponse(Call<List<CommentItem>> call, Response<List<CommentItem>> response) {
                processCommentsResponse(response);
            }

            @Override
            public void onFailure(Call<List<CommentItem>> call, Throwable t) {
                Log.e("Network Error", "네트워크 호출 실패: " + t.getMessage());
            }
        });
    }

    private void processCommentsResponse(Response<List<CommentItem>> response){
        if (response.isSuccessful() && response.body() != null) {
            List<CommentItem> newCommentsData = response.body();
            // Call<List<CommentItem>> getCommentsForStory(@Query("cardId") int cardId);
            // 인터페이스에서 Retrofit은 자동으로 JSON 응답을 List<CommentItem> 형식의 객체로 변환해줌
            adapter.setCommentsData(newCommentsData);

        }else {
            Log.e("Error", "서버에서 불러오기에 실패: " + response.code());
        }
    }

    private void postComment(){
        EditText inputCommentView = findViewById(R.id.inputbox_comment);
        String inputText = inputCommentView.getText().toString();
        Log.d("inputText", inputText);

        if(!inputText.isEmpty()){
            //새 댓글 객체 생성
            CommentItem newComment = new CommentItem(cardId, userId, inputText);
            service.postComment(newComment).enqueue(new Callback<ApiResponse>() {
                @Override
                public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                    if(response.isSuccessful() && response.body() != null){
                        if(response.body().isSuccess()){
                            // success가 true일 때의 처리
                            // 댓글 업로드 성공 후 전체 댓글 목록 새로고침
                            loadComments();
                            inputCommentView.setText("");
                        }
                    }else {
                        // success가 false일 때의 처리
                        Log.e("ERROR", "댓글 업로드 실패");
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse> call, Throwable t) {
                    Log.e("ERROR", "네트워크 오류");
                }

            });
        }
    }

    @Override
    public void onDeleteClick(int position) {
        CommentItem commentItemToDelete = commentList.get(position);
        deleteCommentFromServer(commentItemToDelete.getCommentId());
    }

    private void deleteCommentFromServer(int commentId){
        service.deleteComment(commentId).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if(response.isSuccessful() && response.body() != null){
                    if(response.body().isSuccess()){
                        // success가 true일 때의 처리
                        loadComments();
                    }
                }else {
                    // success가 false일 때의 처리
                    Log.e("ERROR", "댓글 업로드 실패");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Log.e("ERROR", "네트워크 오류");
            }
        });
    }
}