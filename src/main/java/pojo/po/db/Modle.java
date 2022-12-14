package pojo.po.db;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Modle {
    private int modleId;
    private String modleTitle;
    private String modlePath;
    private int userId;
    private int modleLabel;
    private int great;
    private Integer mStatus;
    private String content;

    private LocalDate createTime;
    //存储学习状态
    private String studyStatus;

    /**
     * 模板是否上传到社区
     */
    private int common;

    public LocalDate getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDate createTime) {
        this.createTime = createTime;
    }

    public Integer getmStatus() {
        return mStatus;
    }

    public void setmStatus(Integer mStatus) {
        this.mStatus = mStatus;
    }


    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }



    public Integer getMStatus() {
        return mStatus;
    }

    public void setMStatus(Integer mStatus) {
        this.mStatus = mStatus;
    }

    private int reward;

    public int getReward() {
        return reward;
    }

    public void setReward(int reward) {
        this.reward = reward;
    }

    public int getCommon() {
        return common;
    }

    public void setCommon(int common) {
        this.common = common;
    }


    public String getStudyStatus() {
        return studyStatus;
    }

    public void setStudyStatus(String studyStatus) {
        this.studyStatus = studyStatus;
    }

    /**
     * sql语句中limit使用
     */
    private int pageIndex;

    public int getPageIndex() {
        return pageIndex;
    }

    public void setPageIndex(int pageIndex) {
        this.pageIndex = pageIndex;
    }

    public int getGreat() {
        return great;
    }

    public void setGreat(int great) {
        this.great = great;
    }

    public Modle() {
    }



    public Modle(int modleId, String modleTitle, String modlePath, int userId, int modleLabel) {
        this.modleId = modleId;
        this.modleTitle = modleTitle;
        this.modlePath = modlePath;
        this.userId = userId;
    }

    public int getModleId() {
        return modleId;
    }

    public void setModleId(int modleId) {
        this.modleId = modleId;
    }

    public String getModleTitle() {
        return modleTitle;
    }

    public void setModleTitle(String modleTitle) {
        this.modleTitle = modleTitle;
    }

    public String getModlePath() {
        return modlePath;
    }

    public void setModlePath(String modlePath) {
        this.modlePath = modlePath;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }


    public int getModleLabel() {
        return modleLabel;
    }

    public void setModleLabel(int modleLabel) {
        this.modleLabel = modleLabel;
    }

    @Override
    public String toString() {
        return "Modle{" +
                "modleId=" + modleId +
                ", modleTitle='" + modleTitle + '\'' +
                ", modlePath='" + modlePath + '\'' +
                ", userId=" + userId +
                ", modleLabel=" + modleLabel +
                ", great=" + great +
                ", mStatus=" + mStatus +
                ", content='" + content + '\'' +
                ", createTime=" + createTime +
                ", studyStatus='" + studyStatus + '\'' +
                ", common=" + common +
                ", reward=" + reward +
                ", pageIndex=" + pageIndex +
                '}';
    }
}
