package dao.impl;

import pojo.po.db.*;
import pojo.vo.Community;
import tools.easydao.core.SqlSession;
import tools.easydao.core.SqlSessionFactory;
import dao.UserDao;
import tools.utils.DaoUtil;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class UserDaoImpl implements UserDao {
    private final SqlSessionFactory sqlSessionFactory;

    public UserDaoImpl() {
        this.sqlSessionFactory = DaoUtil.getSqlSessionFactory();
    }

    /**
     * 用户根据userId来查找用户昵称和头像
     * @param community community对象
     * @return 用户
     */
    @Override
    public User selectNameImgById(Community community) {
        SqlSession sqlSession=sqlSessionFactory.openSession();
        List<Object> objects = sqlSession.selectList("UserMapper.selectNameImgById", community);
        sqlSession.close();
        if(objects.size()>0){
            //说明查得到消息
            return (User) objects.get(0);
        }else{
            return null;
        }
    }

    /**
     * 通过用户id获取该用户
     *
     * @param userId 需要查找的用户id
     * @return 用户
     */
    @Override
    public synchronized User selectUserById(int userId) {
        SqlSession sqlSession = sqlSessionFactory.openSession();
        User user = new User();
        user.setUserId(userId);
        List<Object> objectList = sqlSession.selectList("UserMapper.selectUserById", user);
        sqlSession.close();
        return objectList.size() == 0 ? null : (User) objectList.get(0);
    }

    /**
     * 通过用户id获取该用户
     *
     * @param nickName 需要查找的用户昵称
     * @return (有bug, 如果存在重名用户则返回查到的第一个)
     */
    @Override
    public User selectUserByNickName(String nickName) {
        SqlSession sqlSession = sqlSessionFactory.openSession();
        User user = new User();
        user.setNickName(nickName);
        List<Object> objectList = sqlSession.selectList("UserMapper.selectUserByNickName", user);
        sqlSession.close();
        return objectList.size() == 0 ? null : (User) objectList.get(0);
    }

    /**
     * 创建新用户
     *
     * @param number 手机号
     * @param password 密码
     * @param nickName 昵称
     * @return 执行sql条数
     */
    @Override
    public int createUserByNumber(String number, String password, String nickName) {
        SqlSession sqlSession = sqlSessionFactory.openSession();
        User user = new User();
        user.setNickName(nickName);
        //插入用户
        int insert = sqlSession.insert("UserMapper.insertUserByNickName", user);
        System.out.println(insert);
        if (insert > 0) {
            sqlSession.commit();
        } else {
            sqlSession.rollBack();
            return insert;
        }
        //插入用户成功
        //获取userId
        user = selectUserByNickName(nickName);
        int userId = user.getUserId();
        //插入用户对应账号
        Account account = new Account();
        account.setNumber(number);
        account.setPassword(password);
        account.setUserId(userId);
        insert = sqlSession.insert("AccountMapper.insertAccount", account);
        if (insert > 0) {
            sqlSession.commit();
        } else {
            sqlSession.rollBack();
        }
        sqlSession.close();
        return insert;
    }


    /**
     * 获取用户名
     *
     * @param nickName 昵称
     * @return 执行条数
     */
    @Override
    public String selectNickName(String nickName) {
        SqlSession sqlSession = sqlSessionFactory.openSession();
        User user = new User();
        user.setNickName(nickName);
        List<Object> userList = sqlSession.selectList("UserMapper.selectNickName", user);
        sqlSession.close();
        if (userList.size() > 0) {
            return ((User) userList.get(0)).getNickName();
        } else {
            return null;
        }
    }

    /**
     * 获取前十信息
     * @return 返回前十的昵称和星星数量
     */
    @Override
    public List<User> selectTopTen() {
        SqlSession sqlSession = sqlSessionFactory.openSession();
        List<Object> objects = sqlSession.selectList("UserMapper.selectTopTen", "");
        sqlSession.close();
        if (objects != null){
            List<User> userList = new ArrayList<>();
            for (Object object : objects) {
                userList.add((User) object);
            }
            return userList;
        }else {
            return null;
        }
    }

    /**
     * 获取用户榜单排名
     * @param userId 用户ID
     * @return 返回值
     */
    @Override
    public Integer selectUserRanking(int userId) {
        SqlSession sqlSession = sqlSessionFactory.openSession();
        User user = new User();
        user.setUserId(userId);
        List<Object> objects = sqlSession.selectList("UserMapper.selectUserRanking", user);
        sqlSession.close();
        return objects.size()>0?((Count)objects.get(0)).getNumber().intValue():null;
    }

    /**
     * 修改用户名
     * @param userId 用户ID
     * @param nickName 昵称
     * @return 执行成功条数
     */
    @Override
    public int updateNickNameByUserID(int userId, String nickName) {
        User user = new User();
        user.setUserId(userId);
        user.setNickName(nickName);
        SqlSession sqlSession = sqlSessionFactory.openSession();
        int update = sqlSession.update("UserMapper.updateNickNameByUserID", user);
        if (update>0){
            sqlSession.commit();
        }else {
            sqlSession.rollBack();
        }
        sqlSession.close();
        return update;
    }

    /**
     * 修改头像
     * @param userId 用户ID
     * @param image 头像
     * @return 返回值
     */
    @Override
    public int updateImageByUserID(int userId, String image) {
        User user = new User();
        user.setUserId(userId);
        user.setImage(image);
        SqlSession sqlSession = sqlSessionFactory.openSession();
        int update = sqlSession.update("UserMapper.updateImageByUserID", user);
        if (update>0){
            sqlSession.commit();
        }else {
            sqlSession.rollBack();
        }
        sqlSession.close();
        return update;
    }

    /**
     * 修改手机号
     * @param userId 用户ID
     * @param number 手机号
     * @return 返回值
     */
    @Override
    public int updatePhoneByUserID(int userId, String number) {
        Account account = new Account();
        account.setUserId(userId);
        account.setNumber(number);
        SqlSession sqlSession = sqlSessionFactory.openSession();
        int update = sqlSession.update("UserMapper.updatePhoneByUserID", account);
        if (update>0){
            sqlSession.commit();
        }else {
            sqlSession.rollBack();
        }
        sqlSession.close();
        return update;
    }

    /**
     * 更新星星数
     * @param userId  用户ID
     * @param totalStars 星星总数
     * @return update
     */
    @Override
    public int updateStarsByUserId(int userId, int totalStars) {
        User user = new User();
        user.setUserId(userId);
        user.setStars(totalStars);
        SqlSession sqlSession = sqlSessionFactory.openSession();
        int update = sqlSession.update("UserMapper.updateStarsByUserID", user);
        if (update > 0){
            sqlSession.commit();
        }else {
            sqlSession.rollBack();
        }
        sqlSession.close();
        return update;
    }

    /**
     * 更新用户积分
     * @param userId 用户积分
     * @param totalPoint 积分总数
     * @return update
     */
    @Override
    public int updatePointByUserId(int userId, int totalPoint) {
        User user = new User();
        user.setUserId(userId);
        user.setPoints(totalPoint);
        SqlSession sqlSession = sqlSessionFactory.openSession();
        int update = sqlSession.update("UserMapper.updatePointsByUserID", user);
        if (update > 0){
            sqlSession.commit();
        }else {
            sqlSession.rollBack();
        }
        sqlSession.close();
        return update;
    }

    @Override
    public int insertDailyStudyData(int userId, int studyNums, int studyTimes,int reviewNums) {
        DailyStudy dailyStudy = new DailyStudy();
        dailyStudy.setUserId(userId);
        dailyStudy.setStudyNums(studyNums);
        dailyStudy.setStudyTime(studyTimes);
        dailyStudy.setReviewNums(reviewNums);
        SqlSession sqlSession = sqlSessionFactory.openSession();
        int insert = sqlSession.insert("UserMapper.insertDailyStudyByUserId", dailyStudy);
        if (insert > 0){
            sqlSession.commit();
        }else {
            sqlSession.rollBack();
        }
        sqlSession.close();
        return insert;
    }

    /**
     * 查询用户日常学习数据
     * @param userId 用户ID
     * @return 封装好的数据
     */
    @Override
    public DailyStudy selectDailyStudyDataByUserId(int userId) {
        DailyStudy dailyStudy = new DailyStudy();
        dailyStudy.setUserId(userId);
        dailyStudy.setStoreTime(LocalDate.now());
        SqlSession sqlSession = sqlSessionFactory.openSession();
        List<Object> objects = sqlSession.selectList("UserMapper.selectDailyStudyByUserId", dailyStudy);
        sqlSession.close();
        if (objects.size()>0){
            return (DailyStudy) objects.get(0);
        }else {
            return null;
        }
    }

    @Override
    public int updateDailyStudyByIdAndTime(int userId, int studyNums, int studyTimes,int reviewNums) {
        SqlSession sqlSession=sqlSessionFactory.openSession();
        DailyStudy dailyStudy=new DailyStudy();
        dailyStudy.setUserId(userId);
        dailyStudy.setReviewNums(reviewNums);
        dailyStudy.setStudyNums(studyNums);
        dailyStudy.setStudyTime(studyTimes);
        dailyStudy.setStoreTime(LocalDate.now());
        int update = sqlSession.update("UserMapper.updateDailyStudyByIdAndTime", dailyStudy);
        if(update>0){
            sqlSession.commit();
        }else {
            sqlSession.rollBack();
        }
        sqlSession.close();
        return update;
    }







}
