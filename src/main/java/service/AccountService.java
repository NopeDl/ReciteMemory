package service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pojo.vo.Message;

public interface AccountService {
    /**
     * 通过number和password查找账户
     *
     * @return number和password对的上返回account，否则返回null
     */
    Message checkAccount(HttpServletRequest request, HttpServletResponse response);

//    /**
//     * 修改密码
//     *
//     * @param request
//     * @return
//     */
//    Message changePassword(HttpServletRequest request);

    /**
     * 根据手机号查找userId
     *
     * @param request
     * @return
     */
    Integer getIdByNumber(HttpServletRequest request);

    /**
     * 检测手机号是否存在
     *
     * @param request
     * @return
     */
    Message checkNumberExists(HttpServletRequest request);
}
