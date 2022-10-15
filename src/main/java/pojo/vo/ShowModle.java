package pojo.vo;

public class ShowModle {

    private String context;//模板内容
    private String labelName;//模板标签名字
    private int labelValue;//模板标签编号
    private String title;//模板标题

    public ShowModle() {
    }


    public ShowModle(String context, String labelName, int labelValue, String title) {
        this.context = context;
        this.labelName = labelName;
        this.labelValue = labelValue;
        this.title = title;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLabelName(String s) {
        return labelName;
    }

    public void setLabelName(String labelName) {
        this.labelName = labelName;
    }

    public int getLabelValue() {
        return labelValue;
    }

    public void setLabelValue(int labelValue) {
        this.labelValue = labelValue;
    }
}
