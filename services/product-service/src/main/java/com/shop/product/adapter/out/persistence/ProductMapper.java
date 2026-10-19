package com.shop.product.adapter.out.persistence;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/** SQL nằm trong resources/mapper/ProductMapper.xml. */
@Mapper
public interface ProductMapper {

    ProductRow selectById(@Param("id") String id);

    List<ProductRow> selectByIds(@Param("ids") Collection<String> ids);

    List<ProductRow> selectActive(@Param("limit") int limit, @Param("offset") int offset);
}
