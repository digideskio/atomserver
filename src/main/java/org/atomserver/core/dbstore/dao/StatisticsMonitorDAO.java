/*
 Copyright (c) 2007 HomeAway, Inc.
  All rights reserved.  http://www.atomserver.org

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/
package org.atomserver.core.dbstore.dao;

import org.atomserver.monitor.WorkspaceCollectionDocumentCount;
import org.atomserver.monitor.WorkspaceCollectionMaxIndex;

import java.util.List;

/**
 * StatisticsMonitorDAO - DAO to retrieve statistical values
 */
public interface StatisticsMonitorDAO extends AtomServerDAO {

    /**
     * Retrieve  document count for each workspace and collection
     *
     * @return a list of WorkspaceCollectionDocumentCount objects
     */
    List<WorkspaceCollectionDocumentCount> getDocumentCountPerWorkspaceCollection();

    /**
     * Retrieve maximum index for each workspace and collection.
     *
     * @return a list of WorkspaceCollectionMaxIndex objects
     */
    List<WorkspaceCollectionMaxIndex> getLastIndexPerWorkspaceCollection();

}
