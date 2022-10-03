package service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pojo.po.Account;
import pojo.vo.Message;

public interface AccountService {
    /**
     * 通过number和password查找账户
     * @return number和password对的上返回account，否则返回null
     */
    Message checkAccount(HttpServletRequest request, HttpServletResponse response);
}