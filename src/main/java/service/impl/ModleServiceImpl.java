package service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import dao.*;
import dao.impl.*;
import enums.Difficulty;
import pojo.po.db.*;
import pojo.vo.Community;
import service.LikesService;
import tools.easydao.core.SqlSession;
import tools.easydao.utils.Resources;
import enums.MsgInf;
import tools.handlers.FileHandler;
import tools.handlers.FileHandlerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import pojo.vo.Message;
import pojo.vo.ShowModle;
import service.ModleService;
import tools.utils.StringUtil;

import java.io.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;

public class ModleServiceImpl implements ModleService {
    private final ModleDao modleDao = new ModleDaoImpl();

    private final UMRDao umrDao = new UMRDaoImpl();
    private final LabelDao labelDao = new LabelDaoImp();
    private final UserDao userDao = new UserDaoImpl();
    private final ReviewDao reviewDao = new ReviewDaoImpl();
    private final LikesService likesService = new LikesServiceImpl();

    @Override
    public Message collectModle(HttpServletRequest request) {
        Message message;
        //用户收藏非自己的模板
        int userId = (Integer) request.getAttribute("userId");
        int modleId = Integer.parseInt(request.getParameter("modleId"));
        //收藏状态，1为收藏，否则为取消收藏
        int mStatus = Integer.parseInt(request.getParameter("mStatus"));
        Umr umr = new Umr();
        umr.setUserId(userId);
        umr.setModleId(modleId);
        umr.setMStatus(mStatus);

        //先去modle表里找用户收藏的是不是自己的模板
        int integer1 = modleDao.selectIfContain(umr);
        if (integer1 > 0) {
            //说明是用户自己的模板,用户不能操作自己的模板
            message = new Message("该操作违法！");
            return message;
        } else {


            //先查看下该模板是否已被该用户所收藏了
            //是为umr有该条表，说明此时已经收藏成功
            int integer = umrDao.slelectIfCollect(umr);

            //大于0为有这条记录
            //第一种，用户自己收藏过
            if (integer > 0) {
                //umr 表中已经有对应的数据，说明此时已经是收藏了的
                if (mStatus == 1) {
                    //收藏了又收藏，报错
                    message = new Message("收藏失败，该模板已被收藏");
                } else {
                    //此时如果不收藏，那就是取消收藏
                    //获取存储学习记录的路径
                    String recordPath = umrDao.selectRecordPath(modleId, userId);
                    int i = umrDao.deleteUMRByModleId(umr);
                    if (i > 0) {
                        //取消收藏成功
                        //要把收藏到的模板的学习记录删除
                        File file = new File(recordPath);
                        file.delete();
                        message = new Message("取消收藏成功");
                    } else {
                        message = new Message("取消收藏失败");
                    }
                }

                //未收藏过
            } else {
                //获取文章标题
                String modleTitle = modleDao.selectTitleByModleId(modleId);
                String recordPath = writeAsTxt("", modleTitle);
                umr.setRecordPath(recordPath);
                //用户想要收藏
                if (mStatus == 1) {
                    //说明此时用户想要收藏
                    int i = umrDao.insertUMR(userId, modleId, mStatus, recordPath);
                    if (i > 0) {
                        //说明收藏成功
                        message = new Message("收藏成功");
                    } else {
                        message = new Message("收藏失败");
                    }
                } else {
                    //umr表都没这个收藏，（用户又取消收藏，这时候要报错）
                    message = new Message("例表里没有这个收藏呢，请收藏后再进行操作");
                }
            }
        }

//        //调用modleDao来将收藏的东西insert
//        int i = modleDao.collectModleById(userId, modleId,mStatus);
//        if(i>0){
//            //成功插入
//            message=new Message("收藏成功");
//        }else{
//            message=new Message("收藏失败");
//        }
//        return message;
        return message;
    }


    /**
     * 解析文件
     *
     * @param request req
     * @return ret
     */
    @Override
    public Message parseFile(HttpServletRequest request) {
        Message msg;
        try {
            boolean kuohao = Boolean.parseBoolean(request.getParameter("kuohao"));
            //获取上传的文件
            Part upLoadFile = request.getPart("upLoadFile");
            //获取输入流
            if (upLoadFile != null) {
                String fileType = upLoadFile.getSubmittedFileName();
                InputStream input = upLoadFile.getInputStream();
                //根据文件类型获得文件处理器
                fileType = fileType.substring(fileType.lastIndexOf(".") + 1);
                FileHandler handler = FileHandlerFactory.getHandler(fileType, input);
                String context = handler.parseContent();
                if (context != null) {
                    //将换行转换为前端html换行标签
                    context = context.replaceAll("\\r\\n", "<\\br>");

                    msg = new Message("文件解析成功");
                    if (kuohao) {
                        context = StringUtil.parseQuote(context);
                    }
                    msg.addData("context", context);
                } else {
                    msg = new Message("文件解析失败");
                }
            } else {
                msg = new Message("文件上传失败");
            }

        } catch (IOException | ServletException e) {
            e.printStackTrace();
            msg = new Message("文件上传失败");
        }
        return msg;
    }


    /**
     * 创建模板
     * <p>
     * 逻辑解释：先获取三种方式创建模板都应该有的东西context，userId，modleTitle，overWrite（只有选择已有模板创作才能选1或0，否则都应该是0）
     * 如果是1则说明覆盖模板，那么获取模板id,这时候只需要替换原模板路径的txt文本内容就好
     * 否则还应该比对改作者的模板名称是否有和此次想同的，没有则创建成功，否则创建失败，
     * <p>
     * <p>
     * 逻辑解释：先获取三种方式创建模板都应该有的东西context，userId，modleTitle，overWrite（只有选择已有模板创作才能选1或0，否则都应该是0）
     * 如果是1则说明覆盖模板，那么获取模板id,这时候只需要替换原模板路径的txt文本内容就好
     * 否则还应该比对改作者的模板名称是否有和此次想同的，没有则创建成功，否则创建失败，
     *
     * @param request 请求
     * @return 响应数据封装
     */

    @Override
    public Message createModle(HttpServletRequest request) {
        Message message;
        Modle modle = new Modle();
        //只需要获取文本内容和模板制作者，为该模板起名的标题即可
        String context = request.getParameter("context");
//        int userId = Integer.parseInt(request.getParameter("userId"));
        int userId = (Integer) request.getAttribute("userId");
        String modleTitle = request.getParameter("modleTitle");
        //获取标签id
        String modleLabel = request.getParameter("modleLabel");

        //设置模板作者
        modle.setUserId(userId);
        //设置模板标题
        modle.setModleTitle(modleTitle);
        //设置模板标签
        modle.setModleLabel(Integer.parseInt(modleLabel));
        //设置模板的学习状态

//        modle.setStudyStatus("未学习");

        //先看标题有没有重复的
        //覆盖值为1，不覆盖值为0
        String overWrite = request.getParameter("overWrite");
        if ("1".equals(overWrite)) {
            //此时为覆盖的情况下
            //获取原模板的id
            int modleId = Integer.parseInt(request.getParameter("modleId"));
            //设置模板的id
            modle.setModleId(modleId);

            //这时候只需要将原模板里面的东西替换成context就行
            //标题分为两种情况，一种是改了名字的，一种是没改的
            //查找要覆盖的模板的标题
            String s = modleDao.selectTitleByModleId(modleId);
            if (modleTitle.equals(s)) {
                //说明此时没有改名字
                modle.setModlePath(modleLabel);
            } else {

                int sum = modleDao.selectNumByTitle(modle);

                if (sum > 0) {
//                    说明此时已有名字叫xx的模板,此时生成模板失败，因为名称重复
                    message = new Message("模板标题不能重复 ");
                }

            }
            //根据modleId查路径
            String modlePath = modleDao.selectPathByModleId(modleId);
            boolean b = replaceContext(context, modlePath);
            //这个时候得更新模板标签和标题
            //更改模板的标签

            boolean b1 = modleDao.changeModleTag(modle);
            if (b && b1) {

                //结束覆盖过程
                message = new Message("成功覆盖原模板");
                modle.setModlePath(null);
                //？需不需要返回模板对象
                message.addData("modle", modle);
            } else {
                message = new Message("覆盖失败");
            }


        } else {
            //生成新模板
            //对比该模板制作者的其他模板标题，不允许有有重复的标题
            int sum = modleDao.selectNumByTitle(modle);
            if (sum > 0) {
                //说明此时已有名字叫xx的模板,此时生成模板失败，因为名称重复
                message = new Message("模板标题不能重复 ");
            } else {
                //将模板内容存为txt文本,返回模板路径，封装在modle对象里
                String modlePath = writeAsTxt(context, modleTitle);
                modle.setModlePath(modlePath);
                modle.setModleLabel(Integer.parseInt(modleLabel));
                //保存进数据库
                int result = modleDao.insertModle(modle);
                //获取modleId
                int modleId = modleDao.selectModleIdByUserIdAndTitle(modle).getModleId();
                modle.setModleId(modleId);
                //保存um关系
                Umr umr = new Umr();
                umr.setUserId(userId);
                umr.setModleId(modleId);

                //存储已填过的空，生成新的文件
                String recordPath = writeAsTxt("", modleTitle);
                umr.setRecordPath(recordPath);
                //自己创建是0，收藏是1
                umr.setMStatus(0);
                int i = umrDao.insertUMR(userId, modleId, 0, recordPath);
                //可能存在并发问题，需要事务
                if (result > 0 && i > 0) {

                    //？需不需要返回模板对象
                    modle.setModlePath(null);
//                    modle.setStudyStatus("未学习");
                    //说明此时插入成功
                    message = new Message("生成新模板成功");
                    message.addData("modle", modle);
                } else {
                    message = new Message("生成新模板失败");
                }
            }
        }
        return message;
    }

    /**
     * 删除模板
     *
     * @param request
     * @return
     */
    @Override
    public Message deleteModle(HttpServletRequest request) {
        Message msg;
        int modleId = Integer.parseInt(request.getParameter("modleId"));
//        int userId =Integer.parseInt(request.getParameter("userId"));
        int userId = (Integer) request.getAttribute("userId");
        String path = modleDao.selectPathByModleId(modleId);
        //先查询该模板是否存在计划表中
//        Review review = new Review();
//        review.setModleId(modleId);
//        review.setUserId(userId);
//        boolean b = reviewDao.selectModle(review);
//
//        if (b) {
//            //，如果存在，从计划表中删除
//            reviewDao.removeModle(review);
//        }
        File file = null;
        //查询计划表中与moleId相关的模板
        List<Review> reviews = reviewDao.selectReviewByModleId(modleId);
        if (reviews.size() > 0) {
            //删除复习计划表里相关的学习计划的文件
            for (Review review : reviews) {
                file = new File(review.getReviewRecordPath());
                System.out.println(review.getReviewRecordPath());
                file.delete();
            }
        }

        //查询与modleId有关的umr管旭
        List<Umr> list = umrDao.selectUmrByModleId(modleId);
        if (list.size() > 0) {
            //查到所有收藏的modleId，有关的学习记录和和记录都删了
            for (Umr umr : list) {
                file = new File(umr.getRecordPath());
                file.delete();

            }
        }
        int deleteModle = modleDao.deleteModle(modleId);
        if (deleteModle > 0) {
            file = new File(path);
            boolean deleteFile = file.delete();
            msg = new Message("删除成功");
            msg.addData("deleteSuccess", true);
            return msg;
        }
//        int deleteModle = modleDao.deleteModle(modleId);
//
//        File file = new File(path);
//        boolean deleteFile = file.delete();
//        //没有用事务,可能会有bug
//        Message msg;
////        if (deleteUmr != 0 && 0 != deleteModle && deleteFile) {
//        if (0 != deleteModle && deleteFile) {
//            msg = new Message("删除成功");
//            msg.addData("deleteSuccess", true);
//        } else {
//            msg = new Message("删除失败");
//            msg.addData("deleteSuccess", false);
//        }
        msg = new Message("删除失败");
        msg.addData("deleteSuccess", false);
        return msg;
    }

    /**
     * 根据获取的模板id,来读取txt文本
     *
     * @param request req
     * @return ret
     */
    @Override
    public Message reTxt(HttpServletRequest request) {
        Message message;
        //获取模板id
        String modleId = request.getParameter("modleId");
        //获取模板路径
        String modlePath = modleDao.selectPathByModleId(Integer.parseInt(modleId));
        Modle modle = modleDao.selectPathTitlAndTag(Integer.parseInt(modleId));

        try {
            //获取文件位置
            InputStream input = new FileInputStream(modlePath);
            //获取模板文件处理器
            FileHandler txtHandler = FileHandlerFactory.getHandler("txt", input);
            //解析文件内容
            String context = txtHandler.parseContent();
            ShowModle showModle = new ShowModle();
            //存模板内容
            showModle.setContext(context);
            //存模板标题
            showModle.setTitle(modle.getModleTitle());

            //查找模板的标签名字,并且封装
            int modleLabel = modle.getModleLabel();
            //将模板标签编号存进去
            showModle.setLabelValue(modleLabel);

            //将模板标签名字存进去
            String lableName = labelDao.selectLableName(modleLabel);
            showModle.setLabelName(lableName);

            //查找用户该模板是否有学习记录
            int userId = (int) request.getAttribute("userId");
            //查找用户的复习文件
            String recordPath = umrDao.selectRecordPath(Integer.parseInt(modleId), userId);
            File file = new File(recordPath);
            //根据文件的长短来判断文件是否有保存学习的记录
            if (file.length() > 0) {
                showModle.setHaveStudyRecord(true);
            } else {
                showModle.setHaveStudyRecord(false);
            }

            message = new Message("读取模板内容成功");
            //返回响应数据，模板内容
            message.addData("modleContext", showModle);
        } catch (FileNotFoundException e) {
            message = new Message(MsgInf.SERVER_ERROR);
        }
        return message;
    }

    /**
     * 将String类型的字符串存为txt文本，并且返回文件的地址
     *
     * @param context    文本
     * @param modleTitle 模板标题
     * @return 文件路径
     */
    @Override
    public String writeAsTxt(String context, String modleTitle) {
        String filePath = Resources.getResource("static/modles/" + System.currentTimeMillis() + modleTitle + ".txt");
        try {
            File file = new File(filePath);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            if (!file.exists()) {
                file.createNewFile();
            }
            FileHandler txtHandler = FileHandlerFactory.getHandler("txt", null);
            String path = txtHandler.saveFile(filePath, context);
            return path;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 修改模板内容,根据传进来的modleId查找modlePath，从而修改文本
     *
     * @param context 内容
     * @param path    文件路径
     * @return 返回
     */
    @Override
    public boolean replaceContext(String context, String path) {
        //覆盖成功返回true，失败返回false
        try {
            PrintWriter printWriter = new PrintWriter(path);
            printWriter.write(context);
            printWriter.flush();
            printWriter.close();
            return true;
        } catch (FileNotFoundException e) {
            System.out.println("replaceContext:" + path);
            e.printStackTrace();
        }
        return false;

    }

    /**
     * 获取标签下所有模板
     *
     * @param request 请求
     * @return 返回
     */
    @Override
    public Message getModlesByTag(HttpServletRequest request) {
        String pageIndexStr = request.getParameter("pageIndex");
        String modleLabelStr = request.getParameter("modleLabel");

        Message msg = null;
        if (pageIndexStr != null && modleLabelStr != null) {
            //获取分页起始处和模板分类标签
            int pageIndex = Integer.parseInt(pageIndexStr) * 5;
            int modleLabel = Integer.parseInt(modleLabelStr);
            //封装查询数据
            Modle modle = new Modle();
            modle.setCommon(1);
            modle.setModleLabel(modleLabel);
            modle.setPageIndex(pageIndex);

            //获得查询信息
            List<Community> modleList;
            if (modleLabel != 0) {
                //不查询官方模板
                modleList = modleDao.selectModlesByTag(modle);
            } else {
                //查询官方模板
                //即 查询YY号的模板
                int userId = 50;
                modleList = modleDao.selectModleByUserId(userId, pageIndex);
            }
            //返回一个Community类型（包含modle里面的 所有属性）
            InputStream input;
            if (modleList != null && modleList.size() > 0) {
                for (Community community : modleList) {
                    //根据路径读取文件内容
                    //获取改模板的路径；根据路径读取文件内容
                    String modlePath = community.getModlePath();
                    try {
                        input = new FileInputStream(modlePath);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                    //读取文本
                    FileHandler txtFileHandler = FileHandlerFactory.getHandler("txt", input);
                    String content = txtFileHandler.parseContent();
                    community.setContent(content);
                    community.setModlePath("");

                    User user = userDao.selectNameImgById(community);
                    if (user != null) {
                        //传进来昵称和头像
                        community.setNickName(user.getNickName());
                        //获取用户头像的显示地址
                        String imagepath = user.getImage();

                        //从我开始
                        if ("".equals(imagepath) || imagepath == null) {
                            //说明此时头像为默认头像，不需要重新读取
                            //将响应的数据封装到message里
                            user.setBase64("");
                            community.setBase64("");
                        } else {
//                            //说明头像已经改变过了，需要重新读取
                            try {
//                                inputStream = new FileInputStream(imagepath);
                                input = Files.newInputStream(Paths.get(imagepath));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            //读取文本,这里表现为读取头像的base64路径
                            FileHandler imgHandler = FileHandlerFactory.getHandler("img", input);
                            String base64 = imgHandler.parseContent();
                            community.setBase64(base64);
                        }
                    }


                    //从我结束
                    //不回显给前端路径
                    community.setModlePath(null);

                    //下面解决点赞问题
                    //先判断用户对该帖子的点赞情况
                    int userId = (int) request.getAttribute("userId");
                    boolean b = likesService.ifUserLike(userId, community.getModleId());
                    community.setLikeStatus(b);
                    //查询帖子的点赞数量，这里查到的不是数据库表的，应该还有缓存的
                    int totalLike = likesService.getLikeNumsByModleId(community.getModleId());
                    community.setLikeNum(totalLike);
                    //这玩意没用
                    community.setGreat(0);
                }


//                System.out.println(modleList.get(0));
                //封装响应信息
                msg = new Message("获取成功");
                msg.addData("selectSuccess", true);
                msg.addData("modleList", modleList);
                if (modleList.size() < 5) {
                    msg.addData("indexEnd", false);
                }
//
            } else {
                //没有获取到参数
                msg = new Message("无模板");
                msg.addData("selectSuccess", false);
            }
        }
        return msg;
    }

    @Override
    public Message getUserMemory(HttpServletRequest request) {
        Message message;

        //获取用户的id,从而获取用户的模板
//        int userId = Integer.parseInt(request.getParameter("userId"));
        int userId = (Integer) request.getAttribute("userId");
        Umr umr = new Umr();
        umr.setUserId(userId);

        List<Umr> umrs = umrDao.selectModleByUserId(userId);
        if (umrs == null) {
            //说明该用户的记忆库啥也没有
            message = new Message();
            message.addData("userMemory", "这里空空如也，快去模板社区进行探索吧！");

        } else {
            //返回有modle的信息和它的状态（已收藏还是未收藏）
            //创建一个hashmap来解决
            ArrayList<Modle> modles = new ArrayList<>();
            //查询用临时变量
            int modleIdTemp;
            //查询用临时modle对象
            Modle tempMod = new Modle();
//            Map<String,Boolean> haveRecord = new HashMap();
//            Map<String,Boolean> haveReviewRecord = new HashMap();

            for (Umr value : umrs) {
                modleIdTemp = value.getModleId();
                tempMod.setModleId(modleIdTemp);
                //封装查询的modle数据
                Modle modle = modleDao.selectModleByModleId(modleIdTemp);
                modle.setStudyStatus(value.getStudyStatus());
                modle.setMStatus(value.getMStatus());
                //获取模板内容
                //下面内容考虑下封装
                String modlePath = modle.getModlePath();
                InputStream input;
                File file = null;
                try {
                    input = new FileInputStream(modlePath);

                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
                //读取文本
                FileHandler txtFileHandler = FileHandlerFactory.getHandler("txt", input);
                String content = txtFileHandler.parseContent();
                //判断学习记录的情况
                String recordPath = umrDao.selectRecordPath(modle.getModleId(), modle.getUserId());
                String reviewRecordPath = reviewDao.selectReviewRecordPath(modle.getModleId(), modle.getUserId());
//                haveRecord.put(""+(modle.getModleId()),judgeIfRecord(recordPath));
//                haveReviewRecord.put(""+(modle.getModleId()),judgeIfRecord(reviewRecordPath));

                modle.setContent(content);
                modle.setModlePath(null);
                //放modle和状态
                modles.add(modle);
            }
            message = new Message();
            message.addData("userModle", modles);
            //是否有学习记录

//            JSONObject json =new JSONObject(haveReviewRecord);
//            JSONObject jsonObject = JSONObject.fromObject(haveReviewRecord);
//            message.addData("haveRecord", haveRecord);
//            //复习计划的是否有学习记录
////            String json2=JSON.toJSONString(haveReviewRecord);
//            message.addData("haveReviewRecord", haveReviewRecord);

        }
        return message;
    }


    /**
     * 获取所有标签信息
     *
     * @return 所有标签信息
     */
    @Override
    public Message getLabels() {
        List<Label> labels = modleDao.selectLabels();
        Message msg;
        if (labels != null) {
            msg = new Message("获取成功");
            msg.addData("labelList", labels);
        } else {
            msg = new Message("暂无标签");
        }
        return msg;
    }


    /**
     * 系统自动挖空
     *
     * @param request 请求
     * @return 返回值
     */
    @Override
    public Message autoDig(HttpServletRequest request) {
        String difficultyStr = request.getParameter("difficulty");
        String modleIdStr = request.getParameter("modleId");
        Message msg;
        if (difficultyStr != null && modleIdStr != null) {
            //获取挖空比例
            Difficulty difficulty = Difficulty.getRatio(difficultyStr);
            //获取模板ID
            int modleId = Integer.parseInt(modleIdStr);
            //挖空好的内容
            String content = StringUtil.autoDig(modleId, difficulty);

            msg = new Message("挖空成功");
            msg.addData("content", content);
        } else {
            msg = new Message("难度或模板id不能为空");
        }
        return msg;
    }

    /**
     * 上传至模板社区
     *
     * @param request 请求
     * @return 响应数据封装
     */
    @Override
    public Message toCommunity(HttpServletRequest request) {
        Message msg;

        int modleId = Integer.parseInt(request.getParameter("modleId"));
        int common = Integer.parseInt(request.getParameter("common"));
        if (common == 1) {
            //说明用户想要上传模板
            int success = modleDao.updateModleCommon(modleId, common);
            if (success > 0) {
                msg = new Message("发布成功");
                msg.addData("isPublic", true);
            } else {
                msg = new Message("发布失败");
                msg.addData("isPublic", false);
            }
        } else {
            //说明用户想要下架模板
            int success = modleDao.updateModleCommon(modleId, common);
            if (success > 0) {
                msg = new Message("删除成功");
                msg.addData("isPublic", false);
            } else {
                msg = new Message("删除失败");
                msg.addData("isPublic", true);
            }
        }

        return msg;
    }


    @Override
    public Message updateModleStatus(HttpServletRequest request) {
        Message message = null;
        //更新模板的学习状态需要根据userId和modleId在umr表中进行修改
//        int userId = Integer.parseInt(request.getParameter("userId"));
        int userId = (Integer) request.getAttribute("userId");
        int modleId = Integer.parseInt(request.getParameter("modleId"));
        String studyStatus = request.getParameter("studyStatus");

//        Modle modle=new Modle();
//        modle.setModleId(modleId);
//        modle.setStudyStatus(studyStatus);
        Umr umr = new Umr();
        umr.setUserId(userId);
        umr.setModleId(modleId);
        umr.setStudyStatus(studyStatus);

        int update = modleDao.updateStudyStatus(umr);
        if (update > 0) {
            //更新成功
            //更新后要把文件里的记录删除
            String recordPath = umrDao.selectRecordPath(modleId, userId);
            boolean b = replaceContext("", recordPath);
            if (b) {
                //学习状态改变后，学习篇数+1
                DailyStudy dailyStudy = userDao.selectDailyStudyDataByUserId(userId);
                if (dailyStudy != null) {
                    //说明已有改数据，只需要进行更新
                    userDao.updateDailyStudyByIdAndTime(userId, dailyStudy.getStudyNums() + 1, dailyStudy.getStudyTime(),
                            dailyStudy.getReviewNums());
                } else {
                    //创建并更新
                    userDao.insertDailyStudyData(userId, 1, 0, 0);
                }
                message = new Message("学习状态更新成功");
            }

        } else {
            message = new Message("更新失败，请先完成模板学习");
        }
        return message;
    }

    /**
     * 随机获取模板
     *
     * @param request req
     * @return 随机模板
     */
    @Override
    public Message getRandomModles(HttpServletRequest request) {
        int modleLabel = Integer.parseInt(request.getParameter("modleLabel"));
        List<Community> modleList;
        if (modleLabel != 0) {
            //不是官方模板
            modleList = modleDao.selectRandomModles(modleLabel);
        } else {
            //是官方模板
            int userId = 50;
            modleList = modleDao.selectRandomModlesByUserId(userId);
        }

        Message msg;
        if (modleList == null) {
            msg = new Message("无模板");
            msg.addData("selectSuccess", false);
        } else {
            modleList.forEach((community) -> {
                //转化文本
                String modlePath = community.getModlePath();
                if (modlePath != null && !"".equals(modlePath)) {
                    try {
                        InputStream input = new FileInputStream(modlePath);
                        FileHandler txtHandler = FileHandlerFactory.getHandler("txt", input);
                        if (txtHandler != null) {
                            community.setContent(txtHandler.parseContent());
                        } else {
                            community.setContent("");
                        }
                        input.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    community.setContent("");
                }
                community.setModlePath(null);
                //转化头像
                User user = userDao.selectNameImgById(community);
                if (user != null) {
                    community.setNickName(user.getNickName());
                    String image = user.getImage();
                    if (image != null && !"".equals(image)) {
                        try {
                            //有头像
                            InputStream input = new FileInputStream(image);
                            FileHandler imgHandler = FileHandlerFactory.getHandler("img", input);
                            if (imgHandler != null) {
                                community.setBase64(imgHandler.parseContent());
                            } else {
                                community.setBase64("");
                            }
                            input.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        //没头像
                        community.setBase64("");
                    }
                } else {
                    community.setNickName("error user");
                    community.setBase64("");
                }

                //下面解决点赞问题
                //先判断用户对该帖子的点赞情况
                int userId = (int) request.getAttribute("userId");
                boolean b = likesService.ifUserLike(userId, community.getModleId());
                community.setLikeStatus(b);
                //查询帖子的点赞数量，这里查到的不是数据库表的，应该还有缓存的
                int totalLike = likesService.getLikeNumsByModleId(community.getModleId());
                community.setLikeNum(totalLike);

            });
            msg = new Message("获取成功");
            msg.addData("selectSuccess", true);
            msg.addData("modle", modleList);
        }
        return msg;
    }

    /**
     * 用来保存学习中填的空
     *
     * @param request 用来获取所需要的信息
     * @return 返回Message
     */
    @Override
    public Message saveRecord(HttpServletRequest request) {
        Message message = null;
        int modleId = Integer.parseInt(request.getParameter("modleId"));
        int userId = (Integer) request.getAttribute("userId");
        //保存返回1，不保存返沪0
        int ifSave = Integer.parseInt(request.getParameter("ifSave"));
        //保存的情况
        if (1 == ifSave) {
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
            } else {
                return new Message("Json格式错误");
            }


            //获取复习的文件的路径
            String recordPath = umrDao.selectRecordPath(modleId, userId);
            //把填空从数组中读出来
            String context = "";
            for (int i = 0; i < blanks.length; i++) {
                if (i == 0) {
                    context = blanks[0];
                } else {
                    context += ";" + blanks[i];
                }
            }
            boolean b = replaceContext(context, recordPath);
            if (b) {
                message = new Message("保存成功！");
                message.addData("save", true);
            } else {
                message = new Message("保存失败");
                message.addData("save", false);
            }
        } else {
            //以下是不保存的情况
            message = new Message("取消保存成功");
            message.addData("doNotSave", true);
        }

        return message;
    }


    /**
     * 获取学习记录
     *
     * @param request 用来获取模板id
     * @return 返回message对象
     */
    @Override
    public Message showRecord(HttpServletRequest request) {
        Message message = null;
        int modleId = Integer.parseInt(request.getParameter("modleId"));
        int userId = (int) request.getAttribute("userId");
        String recordPath = umrDao.selectRecordPath(modleId, userId);
        //读取文件里面的数据
        //获取文件位置
        File file = new File(recordPath);
        if (file.length() > 0) {
            //这里为有学习记录
            //看看用户是否要重投开始学习，1为是，0为否
            int restart = Integer.parseInt(request.getParameter("restart"));
            if (0 == restart) {
                //要继续上一次的学习
                String context = null;
                try {
                    InputStream input = new FileInputStream(recordPath);
                    //获取模板文件处理器
                    FileHandler txtHandler = FileHandlerFactory.getHandler("txt", input);
                    //解析文件内容
                    context = txtHandler.parseContent();
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //处理从文件出来的context最后面是回车"\n"
                context = context.substring(0, context.lastIndexOf("\n"));
                String[] split = context.split(";");
                message = new Message("成功获取上一次的学习记录");
                message.addData("record", split);
            } else {
                //重头开始
                replaceContext("", recordPath);
                message = new Message("重新开始学习");
                message.addData("restart", true);
            }


        } else {
            message = new Message("暂时没有记录哦");
            message.addData("record", false);
        }
        return message;
    }

    /**
     * 判断是否有学习记录
     *
     * @param path 路径
     * @return 有返回true, 否则false
     */
    @Override
    public boolean judgeIfRecord(String path) {
        if (!path.equals("")) {
            File file = new File(path);
            if (file.length() > 0) {
                return true;
            }

        }
        return false;
    }

    /**
     * 获取热门模板
     *
     * @param request 请求
     * @return 热门模板
     */
    @Override
    public Message getHotModle(HttpServletRequest request) {
        int pageIndex = Integer.parseInt(request.getParameter("pageIndex"));
        List<Community> list = modleDao.selectHotModles(pageIndex * 5);
        Message msg;
        if (list != null) {
            list.forEach((community) -> {
                //转化文本
                String modlePath = community.getModlePath();
                if (modlePath != null && !"".equals(modlePath)) {
                    try {
                        InputStream input = new FileInputStream(modlePath);
                        FileHandler txtHandler = FileHandlerFactory.getHandler("txt", input);
                        if (txtHandler != null) {
                            community.setContent(txtHandler.parseContent());
                        } else {
                            community.setContent("");
                        }
                        input.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    community.setContent("");
                }
                community.setModlePath(null);
                //转化头像
                User user = userDao.selectNameImgById(community);
                if (user != null) {
                    community.setNickName(user.getNickName());
                    String image = user.getImage();
                    if (image != null && !"".equals(image)) {
                        try {
                            //有头像
                            InputStream input = new FileInputStream(image);
                            FileHandler imgHandler = FileHandlerFactory.getHandler("img", input);
                            if (imgHandler != null) {
                                community.setBase64(imgHandler.parseContent());
                            } else {
                                community.setBase64("");
                            }
                            input.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        //没头像
                        community.setBase64("");
                    }
                } else {
                    community.setNickName("error user");
                    community.setBase64("");
                }

                //下面解决点赞问题
                //先判断用户对该帖子的点赞情况
                int userId = (int) request.getAttribute("userId");
                boolean b = likesService.ifUserLike(userId, community.getModleId());
                community.setLikeStatus(b);
                //查询帖子的点赞数量，这里查到的不是数据库表的，应该还有缓存的
                int totalLike = likesService.getLikeNumsByModleId(community.getModleId());
                community.setLikeNum(totalLike);

            });
            msg = new Message("获取成功");
            msg.addData("selectSuccess", true);
            msg.addData("modleList", list);
            if (list.size() < 5) {
                msg.addData("indexEnd", false);
            }
        } else {
            msg = new Message("无模板");
            msg.addData("selectSuccess", false);
        }
        return msg;
    }

    /**
     * 判断该模板是否有学习记录
     *
     * @param request 获取传入的modleId
     * @return 返回message
     */
    @Override
    public Message judgeStudyRecord(HttpServletRequest request) {
        int modleId = Integer.parseInt(request.getParameter("modleId"));
        int userId = (int) request.getAttribute("userId");
        //是否是复习阶段学习记录，1为是，0为非复习状态下的学习记录
        String path = "";
        Message message = null;
        int ifReviewRecord = Integer.parseInt(request.getParameter("ifReviewRecord"));
        if (ifReviewRecord == 1) {
            //复习阶段的学习记录
            path = reviewDao.selectReviewRecordPath(modleId, userId);
        } else {
            //非复习状态下的学习记录
            path = umrDao.selectRecordPath(modleId, userId);
        }

        File file = new File(path);
        if (file.length() > 0) {
            //说明有学习记录
            message = new Message("有学习记录");
            message.addData("haveRecord", true);
        } else {
            //说明没有学习记录
            message = new Message("没有学习记录");
            message.addData("haveRecord", false);
        }
        return message;
    }

    /**
     * 通过标题搜索模板
     * @param request 获取传入模板的标题
     * @return 返回message类
     */
    @Override
    public Message searchModelByTitle(HttpServletRequest request) {
        String modleTitle = request.getParameter("modleTitle");
        Message message=null;
        if(modleTitle==null){
            message=new Message("查找不到相对应的模板，请换一个搜索词吧~");
            message.addData("searchResult",false);
        }else{
            //去dao层查找
            List<Community> list=null;
            if("1".equals(request.getParameter("common"))){
                //说明查找的是社区的模板
                list = modleDao.selectCommonModleByTitle(modleTitle);
            }else{
                //找的是记忆库的模板
                int userId = (int) request.getAttribute("userId");
                list = modleDao.selectUserModleByTitle(modleTitle, userId);
            }


            //查不到资料
            if(list==null){
                message=new Message("查找模板失败");
                message.addData("searchModle",false);
            }else {
                list.forEach((community) -> {
                            //转化文本
                            String modlePath = community.getModlePath();
                            if (modlePath != null && !"".equals(modlePath)) {
                                try {
                                    InputStream input = new FileInputStream(modlePath);
                                    FileHandler txtHandler = FileHandlerFactory.getHandler("txt", input);
                                    if (txtHandler != null) {
                                        community.setContent(txtHandler.parseContent());
                                    } else {
                                        community.setContent("");
                                    }
                                    input.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                community.setContent("");
                            }
                        });
                message=new Message("查找模板");
                message.addData("searchModle",true);
                message.addData("modleList",list);
            }
            return message;
        }
        return null;
    }

}