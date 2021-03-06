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

package edu.berkeley.ground.api.versions.cassandra;

import edu.berkeley.ground.api.versions.GroundType;
import edu.berkeley.ground.api.versions.Version;
import edu.berkeley.ground.api.versions.VersionSuccessor;
import edu.berkeley.ground.api.versions.VersionSuccessorFactory;
import edu.berkeley.ground.db.CassandraClient.CassandraConnection;
import edu.berkeley.ground.db.DBClient;
import edu.berkeley.ground.db.DBClient.GroundDBConnection;
import edu.berkeley.ground.db.DbDataContainer;
import edu.berkeley.ground.db.QueryResults;
import edu.berkeley.ground.exceptions.EmptyResultException;
import edu.berkeley.ground.exceptions.GroundException;
import edu.berkeley.ground.util.IdGenerator;

import java.util.ArrayList;
import java.util.List;

public class CassandraVersionSuccessorFactory extends VersionSuccessorFactory {
    public <T extends Version> VersionSuccessor<T> create(GroundDBConnection connectionPointer, String fromId, String toId) throws GroundException {
        CassandraConnection connection = (CassandraConnection) connectionPointer;

        // check to see if both are valid ids since we don't have foreign key constraints
        QueryResults results;
        List<DbDataContainer> predicates = new ArrayList<>();

        predicates.add(new DbDataContainer("id", GroundType.STRING, fromId));
        try {
            results = connection.equalitySelect("Versions", DBClient.SELECT_STAR, predicates);
        } catch (EmptyResultException eer) {
            throw new GroundException("Id " + fromId + " is not valid.");
        }

        if (!results.next()) {
            throw new GroundException("Id " + fromId + " is not valid.");
        }

        predicates.clear();
        predicates.add(new DbDataContainer("id", GroundType.STRING, toId));
        try {
            results = connection.equalitySelect("Versions", DBClient.SELECT_STAR, predicates);
        } catch (EmptyResultException eer) {
            throw new GroundException("Id " + toId + " is not valid.");
        }
        if (!results.next()) {
            throw new GroundException("Id " + toId + " is not valid.");
        }

        List<DbDataContainer> insertions = new ArrayList<>();


        String dbId = IdGenerator.generateId(fromId + toId);

        insertions.add(new DbDataContainer("successor_id", GroundType.STRING, dbId));
        insertions.add(new DbDataContainer("vfrom", GroundType.STRING, fromId));
        insertions.add(new DbDataContainer("vto", GroundType.STRING, toId));

        connection.insert("VersionSuccessors", insertions);

        return VersionSuccessorFactory.construct(dbId, toId, fromId);
    }

    public <T extends Version> VersionSuccessor<T> retrieveFromDatabase(GroundDBConnection connectionPointer, String dbId) throws GroundException {
        CassandraConnection connection = (CassandraConnection) connectionPointer;

        List<DbDataContainer> predicates = new ArrayList<>();
        predicates.add(new DbDataContainer("successor_id", GroundType.STRING, dbId));

        QueryResults resultSet;
        try {
            resultSet = connection.equalitySelect("VersionSuccessors", DBClient.SELECT_STAR, predicates);
        } catch (EmptyResultException eer) {
            throw new GroundException("No VersionSuccessor found with id " + dbId + ".");
        }

        if (!resultSet.next()) {
            throw new GroundException("No VersionSuccessor found with id " + dbId + ".");
        }

        String fromId = resultSet.getString("vfrom");
        String toId = resultSet.getString("vto");

        return VersionSuccessorFactory.construct(dbId, fromId, toId);
    }
}
