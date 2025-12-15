package com.vatek.hrmtoolnextgen.projection;

import com.vatek.hrmtoolnextgen.enumeration.EProjectStatus;

/**
 * Projection for project status aggregation.
 */
public interface ProjectStatusCountProjection {
    EProjectStatus getStatus();

    Long getTotal();
}


