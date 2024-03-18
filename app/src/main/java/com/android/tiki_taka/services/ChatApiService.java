package com.android.tiki_taka.services;

import com.android.tiki_taka.models.dto.ChatRoom;
import com.android.tiki_taka.models.response.ApiResponse;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ChatApiService {

    @POST("Chat/makeChatRoom.php")
    Call<ResponseBody> makeChatRoom(@Body ChatRoom chatRoom);
}
