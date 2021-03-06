/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.berkeley.ground.plugins.hive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.hive.common.ObjectPair;
import org.apache.hadoop.hive.metastore.api.InvalidObjectException;
import org.apache.hadoop.hive.metastore.api.MetaException;
import org.apache.hadoop.hive.metastore.api.Partition;
import org.apache.hive.common.util.HiveStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import edu.berkeley.ground.api.models.Edge;
import edu.berkeley.ground.api.models.Node;
import edu.berkeley.ground.api.models.NodeVersion;
import edu.berkeley.ground.api.models.Structure;
import edu.berkeley.ground.api.models.StructureVersion;
import edu.berkeley.ground.api.models.Tag;
import edu.berkeley.ground.api.versions.GroundType;
import edu.berkeley.ground.exceptions.GroundException;

public class GroundPartition {

    static final private Logger LOG = LoggerFactory.getLogger(GroundTable.class.getName());

    private static final String DUMMY_NOT_USED = "DUMMY_NOT_USED";
    private static final List<String> EMPTY_PARENT_LIST = new ArrayList<String>();

    private GroundReadWrite ground = null;

    public GroundPartition(GroundReadWrite ground) {
        this.ground = ground;
    }

    public Node getNode(String partitionName) throws GroundException {
        try {
            LOG.debug("Fetching partition node: " + partitionName);
            return ground.getNodeFactory().retrieveFromDatabase(partitionName);
        } catch (GroundException ge1) {
            LOG.debug("Not found - Creating partition node: {}", partitionName);

            Node node = ground.getNodeFactory().create(partitionName);
            Structure nodeStruct = ground.getStructureFactory().create(node.getName());

            return node;
        }
    }

    public Structure getNodeStructure(String partitionName) throws GroundException {
        try {
            Node node = this.getNode(partitionName);
            return ground.getStructureFactory().retrieveFromDatabase(partitionName);
        } catch (GroundException e) {
            LOG.error("Unable to fetch parition node structure");
            throw e;
        }
    }

    public Edge getEdge(String partitionName) throws GroundException {
        try {
            LOG.debug("Fetching table partition edge: " + partitionName);
            return ground.getEdgeFactory().retrieveFromDatabase(partitionName);
        } catch (GroundException ge1) {
            LOG.debug("Not found - Creating table partition edge: {}", partitionName);

            Edge edge = ground.getEdgeFactory().create(partitionName);
            Structure edgeStruct = ground.getStructureFactory().create(partitionName);
            return edge;
        }
    }

    public Structure getEdgeStructure(String partitionName) throws GroundException {
        try {
            Edge edge = getEdge(partitionName);
            return ground.getStructureFactory().retrieveFromDatabase(partitionName);
        } catch (GroundException e) {
            LOG.error("Unable to fetch table partition edge structure");
            throw e;
        }
    }

    Partition fromJSON(String json) {
        Gson gson = new Gson();
        return (Partition) gson.fromJson(json.replace("\\",""), Partition.class);
    }

    String toJSON(Partition part) {
        Gson gson = new Gson();
        return gson.toJson(part);
    }

    public NodeVersion createPartition(String dbName, String tableName, Partition part)
            throws InvalidObjectException, MetaException {
        try {
            ObjectPair<String, String> objectPair = new ObjectPair<>(HiveStringUtils.normalizeIdentifier(dbName),
                    HiveStringUtils.normalizeIdentifier(tableName));
            String partId = objectPair.toString();
            for (String value : part.getValues()) {
                partId += ":" + value;
            }

            Tag partTag = new Tag(DUMMY_NOT_USED, partId, toJSON(part), GroundType.STRING);

            Node node = this.getNode(partId);
            String nodeId = node.getId();
            Structure partStruct = this.getNodeStructure(partId);
            Map<String, GroundType> structVersionAttribs = new HashMap<>();
            structVersionAttribs.put(partId, GroundType.STRING);
            StructureVersion sv = ground.getStructureVersionFactory().create(partStruct.getId(), structVersionAttribs,
                    new ArrayList<String>());

            String reference = part.getSd().getLocation();
            HashMap<String, Tag> tags = new HashMap<>();
            tags.put(partId, partTag);

            String versionId = sv.getId();
            List<String> parentId = new ArrayList<String>();

            Map<String, String> parameters = part.getParameters();

            NodeVersion partNodeVersion = ground.getNodeVersionFactory().create(tags, versionId, reference, parameters,
                    nodeId, parentId);

            return partNodeVersion;
        } catch (GroundException e) {
            throw new MetaException("Unable to create partition " + e.getMessage());
        }
    }
}
