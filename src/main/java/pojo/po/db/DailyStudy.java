package pojo.po.db;

import java.time.LocalDate;

public class DailyStudy {
    private int userId;
    private int studyNums;
    private int studyTime;
    private int reviewNums;
    private LocalDate storeTime;
    private int totalReviewNums;

    public DailyStudy() {
    }

    public DailyStudy(int userId, int studyNums, int studyTime, int reviewNums, LocalDate storeTime, int totalReviewNums) {
        this.userId = userId;
        this.studyNums = studyNums;
        this.studyTime = studyTime;
        this.reviewNums = reviewNums;
        this.storeTime = storeTime;
        this.totalReviewNums = totalReviewNums;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getStudyNums() {
        return studyNums;
    }

    public void setStudyNums(int studyNums) {
        this.studyNums = studyNums;
    }

    public int getStudyTime() {
        return studyTime;
    }

    public void setStudyTime(int studyTime) {
        this.studyTime = studyTime;
    }

    public int getReviewNums() {
        return reviewNums;
    }

    public void setReviewNums(int reviewNums) {
        this.reviewNums = reviewNums;
    }

    public LocalDate getStoreTime() {
        return storeTime;
    }

    public void setStoreTime(LocalDate storeTime) {
        this.storeTime = storeTime;
    }

    public int getTotalReviewNums() {
        return totalReviewNums;
    }

    public void setTotalReviewNums(int totalReviewNums) {
        this.totalReviewNums = totalReviewNums;
    }

    @Override
    public String toString() {
        return "DailyStudy{" +
                "userId=" + userId +
                ", studyNums=" + studyNums +
                ", studyTime=" + studyTime +
                ", reviewNums=" + reviewNums +
                ", storeTime=" + storeTime +
                ", totalReviewNums=" + totalReviewNums +
                '}';
    }
}
