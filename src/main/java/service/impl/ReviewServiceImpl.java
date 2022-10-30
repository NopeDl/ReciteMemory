package service.impl;

import dao.ModleDao;
import dao.ReviewDao;
import dao.impl.ModleDaoImpl;
import dao.impl.ReviewDaoImpl;
import jakarta.servlet.http.HttpServletRequest;
import pojo.po.db.Modle;
import pojo.po.db.Review;
import pojo.vo.Message;
import service.ModleService;
import service.ReviewService;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

public class ReviewServiceImpl implements ReviewService {
  private final ModleDao modleDao=new ModleDaoImpl();
  private final ReviewDao reviewDao=new ReviewDaoImpl();
    /**
     * 用户将某个模板加入学习计划
     * 需要传进userId和modleId，更改模板的学习状态,存进review表中
     * @param request
     * @return
     */
    @Override
    public Message joinThePlan(HttpServletRequest request) {
        Message message=null;
        String userId = request.getParameter("userId");
        String modleId = request.getParameter("modleId");
        //学习状态
        String studyStatus = request.getParameter("studyStatus");
        Modle modle=new Modle();
        modle.setUserId(Integer.parseInt(userId));
        modle.setModleId(Integer.parseInt(modleId));
        modle.setStudyStatus(studyStatus);

        //加入学习计划，即将是学习状态改变为复习中
        int i = modleDao.updateStudyStatus(modle);
        if(i>0){
            //进入复习计划，将modleId加入review表中
            Review review=new Review();
            review.setModleId(modle.getModleId());
            //数据库表中默认的周期为0
            int insert = reviewDao.joinIntoPlan(review);
            if(insert>0){
                //此时已加入学习计划
                message=new Message("成功加入复习计划");
            }
        }else{
            message=new Message("加入复习计划失败，请重新添加");
        }
        return message;
    }

}