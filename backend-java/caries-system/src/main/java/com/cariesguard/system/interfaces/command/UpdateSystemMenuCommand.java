package com.cariesguard.system.interfaces.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class UpdateSystemMenuCommand {

    private Long parentId;

    @NotBlank
    private String menuName;

    @NotBlank
    private String menuTypeCode;

    private String routePath;

    private String componentPath;

    private String permissionCode;

    private String icon;

    private String visibleFlag;

    private String cacheFlag;

    @NotNull
    private Integer orderNum;

    @NotBlank
    private String status;

    private String remark;

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getMenuName() {
        return menuName;
    }

    public void setMenuName(String menuName) {
        this.menuName = menuName;
    }

    public String getMenuTypeCode() {
        return menuTypeCode;
    }

    public void setMenuTypeCode(String menuTypeCode) {
        this.menuTypeCode = menuTypeCode;
    }

    public String getRoutePath() {
        return routePath;
    }

    public void setRoutePath(String routePath) {
        this.routePath = routePath;
    }

    public String getComponentPath() {
        return componentPath;
    }

    public void setComponentPath(String componentPath) {
        this.componentPath = componentPath;
    }

    public String getPermissionCode() {
        return permissionCode;
    }

    public void setPermissionCode(String permissionCode) {
        this.permissionCode = permissionCode;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getVisibleFlag() {
        return visibleFlag;
    }

    public void setVisibleFlag(String visibleFlag) {
        this.visibleFlag = visibleFlag;
    }

    public String getCacheFlag() {
        return cacheFlag;
    }

    public void setCacheFlag(String cacheFlag) {
        this.cacheFlag = cacheFlag;
    }

    public Integer getOrderNum() {
        return orderNum;
    }

    public void setOrderNum(Integer orderNum) {
        this.orderNum = orderNum;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
