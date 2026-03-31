package com.minhpt.hrmtoolnextgen.projection;

import com.minhpt.hrmtoolnextgen.enumeration.EProjectStatus;

/**
 * Projection for project status aggregation.
 */
public interface ProjectStatusCountProjection {
    EProjectStatus getStatus();

    Long getTotal();
}


