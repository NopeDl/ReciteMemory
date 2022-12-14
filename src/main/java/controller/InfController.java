package controller;

import enums.MsgInf;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pojo.vo.Message;
import service.AccountService;
import service.ModleService;
import service.ReviewService;
import service.UserService;
import service.impl.AccountServiceImpl;
import service.impl.ModleServiceImpl;
import service.impl.ReviewServiceImpl;
import service.impl.UserServiceImpl;
import tools.utils.ResponseUtil;
import tools.utils.StringUtil;

import java.io.IOException;

/**
 * 信息获取控制器
 */
@WebServlet("/inf.get/*")
public class InfController extends HttpServlet {
    private final UserService userService = new UserServiceImpl();
    private final AccountService accountService = new AccountServiceImpl();
    private final ModleService modleService = new ModleServiceImpl();

    private final ReviewService reviewService = new ReviewServiceImpl();

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String uri = StringUtil.parseUri(request.getRequestURI());
        Message msg;
        if ("checkUsedNumber".equals(uri)) {
            //检查手机号是否存在
            msg = accountService.checkNumberExists(request);
        } else if ("checkUserNickName".equals(uri)) {
            //检查昵称是否存在
            msg = userService.checkNickNameExists(request);
        } else if ("getModlesByTag".equals(uri)) {
            //获取标签下所有模板
            msg = modleService.getModlesByTag(request);
            //随机获取模板
        } else if ("getHotModle".equals(uri)) {
            //获取热门模板
            msg = modleService.getHotModle(request);
        } else if ("getRandomModles".equals(uri)) {
            msg = modleService.getRandomModles(request);
        } else if ("labels".equals(uri)) {
            //获取所有标签信息
            msg = modleService.getLabels();
        } else if ("rankingList".equals(uri)) {
            //获取排行榜前十信息
            msg = userService.rankingList(request);
        } else if ("studyData".equals(uri)) {
            //获取用户日常学习信息： 学习篇数和学习时长
            msg = userService.getUserDailyStudyData(request);
        } else if ("getAccuracy".equals(uri)) {
            msg = reviewService.getAccuracy(request);
        } else {
            msg = new Message(MsgInf.NOT_FOUND);
        }
        ResponseUtil.send(response, msg);
    }
}
