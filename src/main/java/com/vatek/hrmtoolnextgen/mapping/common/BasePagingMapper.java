package com.vatek.hrmtoolnextgen.mapping.common;

import org.springframework.data.domain.Page;

public interface BasePagingMapper<D, E> extends BaseMapper<D, E> {
    default Page<D> toDtoPageable(Page<E> page) {
        return page.map(this::toDto);
    }
}
