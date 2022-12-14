package dao.impl;

import tools.easydao.core.SqlSession;
import tools.easydao.core.SqlSessionFactory;
import dao.AccountDao;
import pojo.po.db.Account;
import tools.utils.DaoUtil;

import java.util.List;

public class AccountDaoImpl implements AccountDao {
    private final SqlSessionFactory sqlSessionFactory;

    public AccountDaoImpl() {
        this.sqlSessionFactory = DaoUtil.getSqlSessionFactory();
    }

    @Override
    public Account selectAccount(String number, String password) {
        Account src = new Account();
        src.setPassword(password);
        src.setNumber(number);
        SqlSession sqlSession = sqlSessionFactory.openSession();
        List<Object> objectList = sqlSession.selectList("AccountMapper.checkAccount", src);
        sqlSession.close();
        return objectList.size() == 0 ? null : (Account) objectList.get(0);
    }

    @Override
    public int changePasswordByUserId(int userId, String newPassword) {
        Account account = new Account();
        account.setUserId(userId);
        account.setPassword(newPassword);
        SqlSession sqlSession = sqlSessionFactory.openSession();
        int update = sqlSession.update("AccountMapper.changePassword", account);
        if (update > 0) {
            sqlSession.commit();
        } else {
            sqlSession.rollBack();
        }
        sqlSession.close();
        return update;
    }


    @Override
    //根据手机号查找id,返回id
    public Integer selectIdByNumber(String number) {
        SqlSession sqlSession = sqlSessionFactory.openSession();
        Account account = new Account();
        account.setNumber(number);
        List<Object> selectIdByNumber = sqlSession.selectList("AccountMapper.selectIdByNumber", account);
        sqlSession.close();
        if (selectIdByNumber.size() == 0) {
            return null;
        } else {
            return ((Account) selectIdByNumber.get(0)).getUserId();
        }

    }
}
