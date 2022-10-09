package service.impl;


import dao.ModleDao;
import dao.UserDao;
import dao.impl.ModleDaoImpl;
import dao.impl.UserDaoImpl;
import enums.MsgInf;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import pojo.po.User;
import pojo.vo.Message;
import service.UserService;
import utils.FileUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class UserServiceImpl implements UserService {

    private final UserDao userDao = new UserDaoImpl();

    private final ModleDao modleDao = new ModleDaoImpl();

    /**
     * 注册用户
     *
     * @param request
     * @return
     */
    @Override
    public Message createUser(HttpServletRequest request) {
        String number = request.getParameter("phone");
        String password = request.getParameter("password");
        String nickName = request.getParameter("username");
        int ret = userDao.createUserByNumber(number, password, nickName);
        Message message;
        if (ret == 1) {
            User user = userDao.selectUserByNickName(nickName);
            int userId = user.getUserId();
            message = new Message(MsgInf.OK);
            message.addData("isSuccess", true);
            message.addData("userId", userId);//将id发送给前端
        } else {
            message = new Message("用户创建失败");
            message.addData("isSuccess", true);
        }
        return message;
    }

    @Override
    //通过userId来查找用户资料
    public Message selectUserMsg(HttpServletRequest request) {

        Message message;
//        int userId = Integer.parseInt(getCookie(request, "userId"));//查找userId
//        int userId = (int) request.getSession().getAttribute("userId");//通过session获取userId
        int userId = Integer.parseInt(request.getParameter("userId"));
        User user = userDao.selectUserById(userId);
        //将响应的数据封装到message里
        message = new Message(MsgInf.OK);
        message.addData("user", user);
        return message;
    }

    /**
     * 目前没啥用
     *
     * @param request
     * @param cookieName
     * @return
     */
    @Override
    public String getCookie(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        //如果cookies为空，直接返回null;
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (cookieName.equals(c.getName())) {
                    return c.getValue();
                }
            }
        }
        return null;
    }

    /**
     * 通过用户id来改个人信息
     * @param userId
     * @param request
     * @return
     */
    @Override
    public Message ReMsgById(int userId, HttpServletRequest request) {
        Message message;
        String nickName = request.getParameter("userName");
        String sex = request.getParameter("sex");
        //将生日转化为data
        DateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
        Date birthday = null;
        try {
            birthday = fmt.parse(request.getParameter("birthday"));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Integer points = Integer.parseInt(request.getParameter("points"));
        String imagePath = request.getParameter("imagePath");
        Integer cityId = Integer.parseInt(request.getParameter("cityId"));
        String schoolId = request.getParameter("schoolId");

        User user = new User();
        user.setUserId(userId);
        user.setNickName(nickName);
        user.setBirthday(birthday);
        user.setImage(imagePath);
        user.setPoints(points);
        user.setCityId(cityId);
        user.setSex(sex);
        user.setSchool(schoolId);

        int result = userDao.reMessageById(user);
        if (result > 0) {
            //把修改后的资料带走
            message = new Message(MsgInf.OK);
            message.addData("user", user);
        } else {
            message = new Message("该修改违法！");
            message.addData("isSuccess", false);
        }
        return message;
    }

    /**
     * 根据id设置头像
     *
     * @param request
     * @return
     */
    @Override

    public Message setFileById(HttpServletRequest request) {
        Message message;
        try {
            //获取头像
            Part part = request.getPart("file");
            if (part == null) {
                //为空
                message = new Message("文件上传错误");
                message.addData("uploadSuccess", false);
            } else {
                //不为空
                //获取文件输入流
                InputStream input = part.getInputStream();
                //获取文件名字
                String imgName = UUID.randomUUID() + ".pdf";
                //获取默认上传路径
                String uploadPath = this.getClass().getResource("/upload").getPath().substring(1);
                //设置路径
                String savePath = uploadPath + imgName;
                System.out.println(savePath);
                //储存
                FileUtil.save(savePath, input);
                //将地址保存在数据库
                int userId = (int) request.getSession().getAttribute("userId");
                modleDao.insertFileByUserId(userId, savePath);
                //封装响应消息
                message = new Message("文件上传正常");
                message.addData("uploadSuccess", true);
                input.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
        return message;
    }

    /**
     * 检查昵称是否可用
     *
     * @param request
     * @return
     */
    @Override
    public Message checkNickNameExists(HttpServletRequest request) {
        String nickName = request.getParameter("username");
        String name = userDao.selectNickName(nickName);
        Message msg;
        if (name == null) {
            //用户名可用
            msg = new Message("用户名可用");
            msg.addData("isOk", true);
        } else {
            //用户名不可用
            msg = new Message("用户名已被使用");
            msg.addData("isOk", false);
        }
        return msg;
    }
}
