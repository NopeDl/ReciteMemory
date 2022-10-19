package dao;

import pojo.po.Modle;
import pojo.po.Umr;

import java.util.List;

public interface UMRDao {
    /**
     * 根据umr里面的userId的来查找所对应的modleid
     * @param umr
     * @return
     */
    List<Umr> selectModleByUserId(Umr umr);

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

    /**
     * 删除umr关系
     * @param modleId
     * @return
     */
    int deleteUMRByModleId(int modleId);
}