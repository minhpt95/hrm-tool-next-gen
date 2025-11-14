package com.vatek.hrmtoolnextgen.mapping.common;

import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.Set;

public interface BaseMapper<D, E> {
    E toEntity(D var1);

    D toDto(E var1);

    List<E> toEntity(List<D> var1);

    List<D> toDto(List<E> var1);

    Set<E> toEntity(Set<D> var1);

    Set<D> toDto(Set<E> var1);

    E toEntity(D var1, @MappingTarget E var2);

    D toDto(E var1, @MappingTarget D var2);
}
