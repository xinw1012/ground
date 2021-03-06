/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.berkeley.ground.api.versions.neo4j;

import edu.berkeley.ground.api.versions.ItemFactory;
import edu.berkeley.ground.api.versions.VersionHistoryDAG;
import edu.berkeley.ground.db.DBClient.GroundDBConnection;
import edu.berkeley.ground.exceptions.GroundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Neo4jItemFactory extends ItemFactory {
    private Neo4jVersionHistoryDAGFactory versionHistoryDAGFactory;

    public Neo4jItemFactory(Neo4jVersionHistoryDAGFactory versionHistoryDAGFactory) {
        this.versionHistoryDAGFactory = versionHistoryDAGFactory;
    }

    public void insertIntoDatabase(GroundDBConnection connection, String id) throws GroundException {
        // DO NOTHING
    }

    public void update(GroundDBConnection connectionPointer, String itemId, String childId, List<String> parentIds) throws GroundException {
        // If a parent is specified, great. If it's not specified, then make it a child of EMPTY.
        if (parentIds.isEmpty()) {
            parentIds.add(itemId);
        }

        VersionHistoryDAG dag;
        try {
            dag = this.versionHistoryDAGFactory.retrieveFromDatabase(connectionPointer, itemId);
        } catch (GroundException e) {
            if (!e.getMessage().contains("No results found for query")) {
                throw e;
            }

            dag = this.versionHistoryDAGFactory.create(itemId);
        }

        for (String parentId : parentIds) {
            if (!parentId.equals(itemId) && !dag.checkItemInDag(parentId)) {
                String errorString = "Parent " + parentId + " is not in Item " + itemId + ".";

                throw new GroundException(errorString);
            }

            this.versionHistoryDAGFactory.addEdge(connectionPointer, dag, parentId, childId, itemId);
        }
    }

    public List<String> getLeaves(GroundDBConnection connection, String itemId) throws GroundException {
        try {
            VersionHistoryDAG<?> dag = this.versionHistoryDAGFactory.retrieveFromDatabase(connection, itemId);

            return dag.getLeaves();
        } catch (GroundException e) {
            if (!e.getMessage().contains("No results found for query")) {
                throw e;
            }

            return new ArrayList<>();
        }
    }

}
