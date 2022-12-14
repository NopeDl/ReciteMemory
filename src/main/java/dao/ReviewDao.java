package dao;

import pojo.po.db.*;
import pojo.vo.Community;

import java.util.List;

/**
 * 复习表
 */
public interface ReviewDao {
    /**
     * 将要复习的模板写进review表中
     * @param userId 用户的id
     * @param modleId 模板id
     * @param reviewRecordPath 学习记录存储的路径
     * @return 返回int
     */
    int joinIntoPlan(int userId, int modleId ,String reviewRecordPath);


    /**
     * 将模板从计划表中移除
     * @param review
     * @return
     */
    int removeModle(Review review);

    /**
     * 查询计划表中是否存在该模板
     * @param review
     * @return
     */
    boolean selectModle(Review review);

    /**
     * 根据复习周期查询返回的模板
     * @param review
     * @return
     */
    List<Community> selectModleByPeriod(Review review);

    /**
     * 查询该模板是否已经加入复习计划
     * @param umr
     * @return
     */
    boolean selectModleIsReview(Umr umr);

    /**
     *根据模板id查询模板的周期情况
     * @param review
     * @return
     */
    Review selectModlePeriod(Review review);

    /**
     * 更新周期，以及注册日期
     * @param review
     * @return
     */
    int updatePeriodAndDate(Review review);

    /**
     * 查询复习计划表
     * @param modle
     * @return
     */
    List<Community> selectReviewPlan(Modle modle);
//
//    /**
//     * 获取刚插入的reviewId
//     * @return
//     */
//    int selectReviewId();
//
//    /**
//     * 计算reviewId的个数，从而得到下一条插入的reviewId应该是多少
//     * @return
//     */
//    long countReviewId(Review review);

    /**
     * 获取该用户总的复习篇数
     * @param userId
     * @return
     */
    int  getTotalReviewNums(int userId, int period, int days);

    /**
     * 获取复习复习模板的学习记录
     * @param modleId 模板id
     * @param userId 用户id
     * @return 返回文件的路径
     */
    String selectReviewRecordPath(int modleId,int userId);

    /**
     * 查找与modleId相关的复习列表
     * @param modleId 模板id
     * @return 返回Review类型的lsit
     */
    List<Review> selectReviewByModleId(int modleId);
}
