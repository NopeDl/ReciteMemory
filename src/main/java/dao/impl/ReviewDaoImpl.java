package dao.impl;

import dao.ReviewDao;
import pojo.po.db.*;
import pojo.vo.Community;
import tools.easydao.core.SqlSession;
import tools.easydao.core.SqlSessionFactory;
import tools.utils.DaoUtil;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


public class ReviewDaoImpl implements ReviewDao {

    private final SqlSessionFactory sqlSessionFactory ;

    public ReviewDaoImpl() {
        sqlSessionFactory = DaoUtil.getSqlSessionFactory();
    }


    /**
     * 插入复习计划表
     * @param umr
     * @return
     */
    @Override
    public int joinIntoPlan(Umr umr) {
        SqlSession sqlSession = sqlSessionFactory.openSession();
        int insert = sqlSession.insert("ReviewMapper.joinIntoPlan", umr);
        if(insert>0){
            sqlSession.commit();
        }else{
            sqlSession.rollBack();
        }
        sqlSession.close();
       return insert;
    }

    /**
     * 从计划表中移除模板
     * @param review
     * @return
     */
    @Override
    public int removeModle(Review review) {
        SqlSession sqlSession=sqlSessionFactory.openSession();
        int delete = sqlSession.delete("ReviewMapper.removeModle", review);
        if(delete>0){
            //删除成功
            sqlSession.commit();
        }else {
            sqlSession.rollBack();
        }
        sqlSession.close();
        return delete;
    }

    /**
     * 查询计划表中是否存在某个模板
     * @param review
     * @return
     */
    @Override
    public boolean selectModle(Review review) {
        SqlSession sqlSession=sqlSessionFactory.openSession();
        boolean flag=false;
        List<Object> objects = sqlSession.selectList("ReviewMapper.selectModle", review);
        sqlSession.close();
        if(objects.size()>0){
            //此时存在
            flag=true;
        }
        return flag;
    }


    /**
     * 根据模板的复习周期返查找模板,返回一个Community类型的list
     * @param review
     * @return
     */
    @Override
    public List<Community> selectModleByPeriod(Review review) {
        SqlSession sqlSession=sqlSessionFactory.openSession();
        List<Community> communities= new ArrayList<>();
        List<Object> objects = sqlSession.selectList("ReviewMapper.selectModleByPeriod", review);
        if(objects.size()>0){
            //查得到返回list<Modle>
            for (int i = 0; i < objects.size(); i++) {
                communities.add((Community) objects.get(i));
            }
        }
        //否则返回null
        return communities;
    }


    /**
     * 查询该模板是否已经加入复习计划
     * @param umr
     * @return
     */
    @Override
    public boolean selectModleIsReview(Umr umr) {
        SqlSession sqlSession=sqlSessionFactory.openSession();
        boolean flag=false;
        List<Object> objects = sqlSession.selectList("ReviewMapper.selectModleIsReview", umr);
        sqlSession.close();
        if(objects.size()>0){
            flag=true;
        }
        return flag;
    }

    /**
     * 根据模板id查询模板所处的周期
     * @param review
     * @return
     */
    @Override
    public Review selectModlePeriod(Review review) {
        SqlSession sqlSession=sqlSessionFactory.openSession();
        List<Object> objects = sqlSession.selectList("ReviewMapper.selectModlePeriod", review);
        sqlSession.close();
        if(objects.size()>0){
            //成功查询
            return (Review) objects.get(0);
        }
        return null;
    }

    /**
     * 更新周期和日期
     * @param review
     * @return
     */
    @Override
    public int updatePeriodAndDate(Review review) {
        SqlSession sqlSession=sqlSessionFactory.openSession();
        int update = sqlSession.update("ReviewMapper.updatePeriodAndDate", review);
        if(update>0){
            sqlSession.commit();
        }else{
            sqlSession.rollBack();
        }
        return update;
    }


    /**
     * 查询用户的复习计划表
     * @param modle
     * @return
     */
    @Override
    public List<Community> selectReviewPlan(Modle modle) {
        SqlSession sqlSession=sqlSessionFactory.openSession();
        List<Community> communities=new ArrayList<>();
        List<Object> objects = sqlSession.selectList("ReviewMapper.selectReviewPlan", modle);
        sqlSession.close();
        if(objects.size()>0){
            //说明有加入复习的东西
            //存为Community类型的list
            for (int i = 0; i < objects.size(); i++) {
                communities.add((Community) objects.get(i));
            }
        }

        return communities;
    }

    @Override
    public int getTotalReviewNums(int userId,int period,int days) {
        SqlSession sqlSession=sqlSessionFactory.openSession();
        int number=0;
        Review review=new Review();
        review.setUserId(userId);
        review.setPeriod(period);
        review.setDays(days);
        List<Object> result = sqlSession.selectList("ReviewMapper.getTotalReviewNums", review);
        if(result.size()>0){
            number=Math.toIntExact(((Count) result.get(0)).getNumber());
            return number;
        }
        return number;
    }
}
