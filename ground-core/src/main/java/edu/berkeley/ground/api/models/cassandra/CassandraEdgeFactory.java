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

package edu.berkeley.ground.api.models.cassandra;

import edu.berkeley.ground.api.models.Edge;
import edu.berkeley.ground.api.models.EdgeFactory;
import edu.berkeley.ground.api.versions.GroundType;
import edu.berkeley.ground.api.versions.cassandra.CassandraItemFactory;
import edu.berkeley.ground.db.CassandraClient;
import edu.berkeley.ground.db.CassandraClient.CassandraConnection;
import edu.berkeley.ground.db.DBClient;
import edu.berkeley.ground.db.DBClient.GroundDBConnection;
import edu.berkeley.ground.db.DbDataContainer;
import edu.berkeley.ground.db.QueryResults;
import edu.berkeley.ground.exceptions.EmptyResultException;
import edu.berkeley.ground.exceptions.GroundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class CassandraEdgeFactory extends EdgeFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraEdgeFactory.class);
    private CassandraClient dbClient;

    private CassandraItemFactory itemFactory;

    public CassandraEdgeFactory(CassandraItemFactory itemFactory, CassandraClient dbClient) {
        this.dbClient = dbClient;
        this.itemFactory = itemFactory;
    }

    public Edge create(String name) throws GroundException {
        CassandraConnection connection = this.dbClient.getConnection();

        try {
            String uniqueId = "Edges." + name;

            this.itemFactory.insertIntoDatabase(connection, uniqueId);

            List<DbDataContainer> insertions = new ArrayList<>();
            insertions.add(new DbDataContainer("name", GroundType.STRING, name));
            insertions.add(new DbDataContainer("item_id", GroundType.STRING, uniqueId));

            connection.insert("Edges", insertions);

            connection.commit();
            LOGGER.info("Created edge " + name + ".");
            return EdgeFactory.construct(uniqueId, name);
        } catch (GroundException e) {
            connection.abort();

            throw e;
        }
    }

    public Edge retrieveFromDatabase(String name) throws GroundException {
        CassandraConnection connection = this.dbClient.getConnection();

        try {
            List<DbDataContainer> predicates = new ArrayList<>();

            predicates.add(new DbDataContainer("name", GroundType.STRING, name));

            QueryResults resultSet;
            try {
                resultSet = connection.equalitySelect("Edges", DBClient.SELECT_STAR, predicates);
            } catch (EmptyResultException eer) {
                throw new GroundException("No Edge found with name " + name + ".");
            }

            if (!resultSet.next()) {
                throw new GroundException("No Edge found with name " + name + ".");
            }

            String id = resultSet.getString(0);

            connection.commit();
            LOGGER.info("Retrieved edge " + name + ".");

            return EdgeFactory.construct(id, name);
        } catch (GroundException e) {
            connection.abort();

            throw e;
        }
    }

    public void update(GroundDBConnection connection, String itemId, String childId, List<String> parentIds) throws GroundException {
        this.itemFactory.update(connection, itemId, childId, parentIds);
    }
}
