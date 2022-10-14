package dao;

import pojo.po.Modle;
import pojo.po.Umr;

public interface UMRDao {
    /**
     * 根据umr里面的userId的来查找所对应的modleid
     * @param umr
     * @return
     */
    Umr[] selectModleByUserId(Umr umr);

    /**
     * 根据传进来的modle里面的属性modleId来查找相对应的信息
     * @param umr
     * @return
     */
    Modle selectModleByModleId(Umr umr);

    /**
     * 保存umr关系
     * @param umr
     * @return
     */
    int insertUMR(Umr umr);
}
