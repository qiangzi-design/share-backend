package com.share.dto;

import lombok.Data;

import java.util.List;

/**
 * 通用分页响应对象
 * @param <T> 数据项类型
 */
@Data
public class PageResult<T> {
    private List<T> list;
    private long total;
    private int page;
    private int pageSize;
}

