package com.android.tiki_taka.models.dtos;

import com.google.gson.annotations.SerializedName;

import java.time.LocalDateTime;

public class StoryCardDto {
    // JSON 키와 Java 클래스의 변수명이 다를 경우에도 올바르게 매핑하기 위해서 @SerializedName 어노테이션을 사용
    @SerializedName("card_id")
    private int cardId;
    @SerializedName("folder_id")
    private int folderId;
    @SerializedName("user_id")
    private int userId;
    @SerializedName("image_url")
    private String imageUrl; //이미지라면 url이, 메모라면 텍스트가 들어감
    @SerializedName("user_good")
    private int userGood;
    @SerializedName("partner_good")
    private int partnerGood;

    // 클라이언트 측에서 createdAt과 updatedAt을 직접 만들 필요가 없는 경우라도,
    // 서버로부터 해당 정보를 받아와서 클라이언트 측에서 사용하고자 한다면 클래스의 멤버 변수로 선언해 주는 것이 좋습니다. (데이터 동기화)
    @SerializedName("created_at")
    private String createdAt;
    @SerializedName("updated_at")
    private String updatedAt;
    //서버 응답에서 날짜와 시간을 문자열로 수신한 후, 클라이언트 측에서 문자열을 원하는 형식으로 파싱

    // 생성자
    public StoryCardDto(int cardId, int folderId, int userId, String imageUrl, int userGood, int partnerGood) {
        this.cardId = cardId;
        this.folderId = folderId;
        this.userId = userId;
        this.imageUrl = imageUrl;
        this.userGood = userGood;
        this.partnerGood = partnerGood;
    }

    public int getCardId() {
        return cardId;
    }

    public void setCardId(int cardId) {
        this.cardId = cardId;
    }

    public int getFolderId() {
        return folderId;
    }

    public void setFolderId(int folderId) {
        this.folderId = folderId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String contents) {
        this.imageUrl = contents;
    }

    public int getUserGood() {
        return userGood;
    }

    public void setUserGood(int userGood) {
        this.userGood = userGood;
    }

    public int getPartnerGood() {
        return partnerGood;
    }

    public void setPartnerGood(int partnerGood) {
        this.partnerGood = partnerGood;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}