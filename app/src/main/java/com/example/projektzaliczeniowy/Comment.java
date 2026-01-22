package com.example.projektzaliczeniowy;

public class Comment {
    private String userId;
    private String userName;
    private String commentText;
    private String imageBase64;
    private long timestamp;

    public Comment() {
        // Wymagane dla Firebase
    }

    public Comment(String userId, String userName, String commentText, String imageBase64, long timestamp) {
        this.userId = userId;
        this.userName = userName;
        this.commentText = commentText;
        this.imageBase64 = imageBase64;
        this.timestamp = timestamp;
    }

    // Gettery
    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getCommentText() {
        return commentText;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public long getTimestamp() {
        return timestamp;
    }

    // Settery
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setCommentText(String commentText) {
        this.commentText = commentText;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}