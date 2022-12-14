package service.impl;


import com.alibaba.fastjson.JSONObject;
import com.alibaba.druid.support.opds.udf.SqlCodeStat;
import dao.ModleDao;
import dao.ReviewDao;
import dao.UserDao;
import dao.impl.ModleDaoImpl;
import dao.impl.ReviewDaoImpl;
import dao.impl.UserDaoImpl;
import enums.ReviewPeriod;
import jakarta.servlet.http.HttpServletRequest;
import pojo.po.db.DailyStudy;
import pojo.po.db.Modle;
import pojo.po.db.Review;
import pojo.po.db.Umr;
import pojo.vo.Community;
import pojo.vo.Message;
import service.ModleService;
import service.ReviewService;
import tools.easydao.core.SqlSession;
import tools.handlers.FileHandler;
import tools.handlers.FileHandlerFactory;
import tools.utils.StringUtil;

import javax.activation.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.*;

public class ReviewServiceImpl implements ReviewService {
    private final ModleDao modleDao = new ModleDaoImpl();
    private final ReviewDao reviewDao = new ReviewDaoImpl();
    private final UserDao userDao = new UserDaoImpl();
    private final ModleService modleService=new ModleServiceImpl();
    /**
     * 用户将某个模板加入学习计划
     * 需要传进userId和modleId，更改模板的学习状态,存进review表中
     *
     * @param request req
     * @return ret
     */
    @Override
    public Message joinThePlan(HttpServletRequest request) {
        Message message = null;
        int modleId = Integer.parseInt(request.getParameter("modleId"));
//        int userId = Integer.parseInt(request.getParameter("userId"));
        int userId = (Integer) request.getAttribute("userId");
        //学习状态
        String studyStatus = request.getParameter("studyStatus");

        Umr umr = new Umr();
        umr.setModleId(modleId);
        umr.setUserId(userId);

        //查询要添加进review表中的模板在umr表中的状态，包括学习状态
        Umr resultUmr = modleDao.selectModleByIds(umr);
        if (resultUmr != null) {
            //此时得出该模板属于用户，接着判断模板是否已加入模板
            //只要studyStatus为“复习中”，说明模板已经在review表中了
            if ("复习中".equals(resultUmr.getStudyStatus())) {
                //说明在复习计划表中，这时候添加失败
                message = new Message("该模板已经在复习计划里啦，请不要重复添加");
            } else {
                //能够成功加入复习计划
                resultUmr.setStudyStatus(studyStatus);
                //更改学习状态
                int i = modleDao.updateStudyStatus(resultUmr);
                if (i > 0) {
                    //将模板插入review中
                    String title = modleDao.selectTitleByModleId(modleId);
                    String reviewRecordPath = modleService.writeAsTxt("", title);
                    int insert = reviewDao.joinIntoPlan(userId,modleId,reviewRecordPath);
                    //学习中————>复习中，dailyStudy表中的studyNums要加1；
                    //先查询是否有该条记录
                    DailyStudy dailyStudy = userDao.selectDailyStudyDataByUserId(userId);
                    if (dailyStudy != null) {
                        //说明有该数据，只需要进行更新
                        userDao.updateDailyStudyByIdAndTime(userId, dailyStudy.getStudyNums() + 1,
                                dailyStudy.getStudyTime(), dailyStudy.getReviewNums());
                    } else {
                        //说明没有数据，要执行插入语句
                        userDao.insertDailyStudyData(userId, 1, 0, 0);
                    }

                    if (insert > 0) {
                        message = new Message("成功加入复习计划");
                    } else {
                        message = new Message("加入复习计划失败，请重新添加");
                    }
                }
            }
        } else {
            message = new Message("请先添加模板至记忆库哦！");
        }

        return message;
    }

    /**
     * 移除模板计划
     *
     * @param request req
     * @return ret
     */
    @Override
    public Message removeFromPlan(HttpServletRequest request) {
        Message message = null;
//        int userId = Integer.parseInt(request.getParameter("userId"));
        int userId = (Integer) request.getAttribute("userId");
        int modleId = Integer.parseInt(request.getParameter("modleId"));
//        int reviewId= Integer.parseInt(request.getParameter("reviewId"));
        String studyStatus = request.getParameter("studyStatus");
        //移除前先把复习计划下存储学习记录的文件删掉
         deleteRecordPath(modleId, userId);

        //先从计划表中移除
        Review review = new Review();
        review.setModleId(modleId);
        review.setUserId(userId);
//        review.setReviewId(reviewId);
        int delete = reviewDao.removeModle(review);
        if (delete > 0) {
            Umr umr = new Umr();
            umr.setModleId(modleId);
            umr.setUserId(userId);
//            umr.setReviewId(0);
            umr.setStudyStatus(studyStatus);
//            review.setReviewId(0);
            int update = modleDao.updateStudyStatus(umr);

            if (update > 0) {
                //更新模板成功
                message = new Message("移除成功");
            }
        } else {
            message = new Message("移除失败，请重新尝试");
        }
        return message;
    }


    /**
     * 根据周期获取模板
     *
     * @param request req
     * @return ret
     */
    @Override
    public Message getModleByPeriod(HttpServletRequest request) {
        Message message = null;
        //存储返回的模板内容
//        Map<String ,List<Community>> map=new HashMap<>();
        List<Community>[] lists = new List[8];
//        int userId = Integer.parseInt(request.getParameter("userId"));
        int userId = (Integer) request.getAttribute("userId");
        Review review = new Review();
        review.setUserId(userId);

        //学习周期
//        String period = request.getParameter("period");
        //查询所有周期,遍历一遍,一共七个周期
        for (int i = 1; i <= 8; i++) {
            review.setPeriod(i);

            //周期显示所需要的时间
            ReviewPeriod reviewPeriod = ReviewPeriod.getReviewPeriod(i);
            review.setDays(reviewPeriod.getDate());
            List<Community> communities = reviewDao.selectModleByPeriod(review);
            if (communities.size() > 0) {

                for (int j = 0; j < communities.size(); j++) {
                    String modlePath = communities.get(j).getModlePath();
                    InputStream input;
                    try {
                        input = new FileInputStream(modlePath);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                    //读取文本
                    FileHandler txtFileHandler = FileHandlerFactory.getHandler("txt", input);
                    String content = txtFileHandler.parseContent();
                    communities.get(j).setContent(content);
                    communities.get(j).setModlePath("");
                }
            }
//            map.put("周期" + (i), communities);用这个的排序为21436587？
//            map.put("第"+i+"周期",communities);
            lists[i - 1] = communities;
        }

        message = new Message();
//        message.addData("ModlesOfPeriod",map);
        message.addData("ModlesOfPeriod", lists);
        return message;
    }


    /**
     * 将复习完的模板更新周期
     *
     * @param request req
     * @return ret
     */
    @Override
    public Message updatePeriod(HttpServletRequest request) {
        Message message = null;
//        int userId = Integer.parseInt(request.getParameter("userId"));
        int userId = (Integer) request.getAttribute("userId");
        int modleId = Integer.parseInt(request.getParameter("modleId"));
//        int reviewId=Integer.parseInt(request.getParameter("reviewId"));
        Review review = new Review();
        review.setModleId(modleId);
        review.setUserId(userId);
//        review.setReviewId(reviewId);

        //两种情况，一种是还在周期内复习；一种是完成了八个周期的复习，应该离开复习计划，
        //先查模板的周期
        Review result = reviewDao.selectModlePeriod(review);
        int period = result.getPeriod();
        if (period == 8) {
            //说明用户此次复习已经是最后一阶段的复习了，应该离开复习计划，并且修改学习状态
            int i = reviewDao.removeModle(review);
            if (i > 0) {

                Umr umr = new Umr();
                umr.setStudyStatus("已学习");
                umr.setUserId(userId);
                umr.setModleId(result.getModleId());
                int update = modleDao.updateStudyStatus(umr);
                if (update > 0) {
                    DailyStudy dailyStudy = userDao.selectDailyStudyDataByUserId(userId);
                    if (dailyStudy != null) {
                        //说明已有改数据，只需要进行更新
                        userDao.updateDailyStudyByIdAndTime(userId, dailyStudy.getStudyNums() + 1, dailyStudy.getStudyTime(),
                                dailyStudy.getReviewNums() + 1);
                    } else {
                        //创建并更新
                        userDao.insertDailyStudyData(userId, 1,0, 1);
                    }

                    //将学习记录的文件删除
                    Boolean deleteRecordPath = deleteRecordPath(modleId, userId);
                    if(deleteRecordPath){
                        message=new Message("恭喜你，已经牢牢掌握了这个模板内容啦!");
                        message.addData("todayFinish",1);
                    }

                }
            }
        } else {
            //继续周期性学习，周期+1，日期重置
            review.setReTime(LocalDate.now());
            System.out.println(LocalDate.now());
            int i = reviewDao.updatePeriodAndDate(review);
            if (i > 0) {
                //复习完毕，今日复习篇数同步更新
                DailyStudy dailyStudy = userDao.selectDailyStudyDataByUserId(userId);
                if (dailyStudy != null) {
                    //说明已有改数据，只需要进行更新
                    userDao.updateDailyStudyByIdAndTime(userId, dailyStudy.getStudyNums() + 1, dailyStudy.getStudyTime(),
                            dailyStudy.getReviewNums() + 1);
                } else {
                    //创建并更新
                    userDao.insertDailyStudyData(userId, 1, 0, 1);
                }
                //更新周期和时间成功
                //将学习记录清空
                String reviewRecordPath = reviewDao.selectReviewRecordPath(modleId, userId);
                boolean b = modleService.replaceContext("", reviewRecordPath);
                if(b){
                    message = new Message("恭喜你完成这个周期的复习啦，下个周期见吧");
                    message.addData("todayFinish", 1);
                }

            }
        }
        return message;
    }

    /***
     * 将复习模板的学习记录保存
     * @param request 获取信息的request
     * @return 返回message
     */
    @Override
    public Message saveReviewRecord(HttpServletRequest request) {
        Message message=null;
        int modleId = Integer.parseInt(request.getParameter("modleId"));
        int userId = (int) request.getAttribute("userId");
//        String[] blanks = request.getParameterValues("blanks");

        //是否保存当前学习记录，1为是，0为否
        int ifSave = Integer.parseInt(request.getParameter("ifSave"));
        //保存
        if(1==ifSave){
//填空情况
            String[] blanks;
            //换成json
            String blanksJson = request.getParameter("blanks");

            System.out.println(blanksJson);

            if (JSONObject.isValid(blanksJson)) {
                //解析json
                JSONObject jsonObject = JSONObject.parseObject(blanksJson);
                List<String> arr = (List<String>) jsonObject.get("arr");
                blanks = new String[arr.size()];
                arr.toArray(blanks);
            }else {
                return new Message("Json格式错误");
            }

            String context="";
            for (int i = 0; i < blanks.length; i++) {
                if(i==0){
                    context=blanks[i];
                }else{
                    context+=";"+blanks[i];
                }
            }
            String recordPath = reviewDao.selectReviewRecordPath(modleId, userId);
            boolean b = modleService.replaceContext(context, recordPath);
            if(b){
                //替换成功
                message=new Message("成功保存学习记录");
                message.addData("save",true);
            }else{
                message=new Message("保存失败");
                message.addData("save",false);

            }
        }else{
            //不保存本地记录
            message=new Message("取消保存成功");
            message.addData("doNOtSave",true);

        }
        return message;
    }

    /**
     * 获取复习板块的学习记录
     * @param request 获取信息的request
     * @return 返回message
     */
    @Override
    public Message showReviewRecord(HttpServletRequest request) {
        int modleId = Integer.parseInt(request.getParameter("modleId"));
        int userId = (int) request.getAttribute("userId");
        Message message=null;
        //获取学习记录文件的路径
        String reviewRecordPath = reviewDao.selectReviewRecordPath(modleId, userId);
        File file=new File(reviewRecordPath);
        if(file.length()>0){

            //这里为有学习记录
            //看看用户是否要重投开始学习，1为是，0为否
            int restart = Integer.parseInt(request.getParameter("restart"));
            if(0==restart) {
                //获取文件位置
                String context = null;
                try {
                    InputStream input = new FileInputStream(reviewRecordPath);
                    //获取模板文件处理器
                    FileHandler txtHandler = FileHandlerFactory.getHandler("txt", input);
                    //解析文件内容
                    context = txtHandler.parseContent();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                context = context.substring(0, context.lastIndexOf("\n"));
                String[] split = context.split(";");
                message = new Message("成功获取学习记录");
                message.addData("reviewRecord", split);
            }else{
                //重头开始
                modleService.replaceContext("",reviewRecordPath);
                message=new Message("重新开始学习");
                message.addData("restart",true);
            }
        }else{
            message=new Message("暂时没有记录哦");
            message.addData("reviewRecord",false);
        }

        return message;

    }

    /**
     * 获取准确率
     *
     * @param request req
     * @return 准确率
     */
    @Override
    public Message getAccuracy(HttpServletRequest request) {
        String json = request.getParameter("matchStr");
        Message msg;
        if (JSONObject.isValid(json)) {
            List<String> acc = StringUtil.stringMatch(JSONObject.parseObject(json));
            String total = StringUtil.avgStringMatch(acc);
            msg = new Message("计算成功");
            msg.addData("calcSuccess", true);
            msg.addData("accuracy", acc);
            msg.addData("total",total);
        } else {
            msg = new Message("请重新验证Json格式");
            msg.addData("calcSuccess", true);
        }
        return msg;
    }
    /**
     * 将存储复习板块的学习记录文件删除
     * @param modleId 模板id
     * @param userId 用户id
     * @return
     */
    @Override
    public Boolean deleteRecordPath(int modleId, int userId) {
        String reviewRecordPath = reviewDao.selectReviewRecordPath(modleId, userId);
        File file=new File(reviewRecordPath);
        boolean delete = file.delete();
        if(delete){
            return true;
        }
        return  false;
    }


}
