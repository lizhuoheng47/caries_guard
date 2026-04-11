package com.cariesguard.system.infrastructure.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("sys_menu")
public class SysMenuDO {

    private Long id;
    private Long parentId;
    private String menuName;
    private String menuTypeCode;
    private String routePath;
    private String componentPath;
    private String permissionCode;
    private String visibleFlag;
    private String cacheFlag;
    private Integer orderNum;
    private Long orgId;
    private String status;
    private Long deletedFlag;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getDeletedFlag() {
        return deletedFlag;
    }

    public void setDeletedFlag(Long deletedFlag) {
        this.deletedFlag = deletedFlag;
    }
}
