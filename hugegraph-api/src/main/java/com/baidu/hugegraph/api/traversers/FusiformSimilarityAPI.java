/*
 * Copyright 2017 HugeGraph Authors
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.baidu.hugegraph.api.traversers;

import java.util.List;

import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

import org.slf4j.Logger;

import com.baidu.hugegraph.HugeGraph;
import com.baidu.hugegraph.api.API;
import com.baidu.hugegraph.core.GraphManager;
import com.baidu.hugegraph.schema.EdgeLabel;
import com.baidu.hugegraph.server.RestServer;
import com.baidu.hugegraph.structure.HugeVertex;
import com.baidu.hugegraph.traversal.algorithm.FusiformSimilarityTraverser;
import com.baidu.hugegraph.type.define.Directions;
import com.baidu.hugegraph.type.define.Frequency;
import com.baidu.hugegraph.util.E;
import com.baidu.hugegraph.util.JsonUtil;
import com.baidu.hugegraph.util.Log;
import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.annotation.JsonProperty;

import static com.baidu.hugegraph.traversal.algorithm.HugeTraverser.*;

@Path("graphs/{graph}/traversers/fusiformsimilarity")
@Singleton
public class FusiformSimilarityAPI extends API {

    private static final Logger LOG = Log.logger(RestServer.class);

    @POST
    @Timed
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON_WITH_CHARSET)
    public String post(@Context GraphManager manager,
                       @PathParam("graph") String graph,
                       FusiformSimilarityRequest request) {
        E.checkArgumentNotNull(request, "The fusiform similarity " +
                               "request body can't be null");
        E.checkArgumentNotNull(request.sources,
                               "The sources of fusiform similarity " +
                               "request can't be null");
        E.checkArgument(request.label != null,
                        "The edge label can't be null");
        if (request.direction == null) {
            request.direction = Directions.BOTH;
        }
        E.checkArgument(request.minEdgeCount > 0,
                        "The min edge count must be > 0, but got: %s",
                        request.minEdgeCount);
        E.checkArgument(request.degree > 0 || request.degree == NO_LIMIT,
                        "The degree of request must be > 0, but got: %s",
                        request.degree);
        E.checkArgument(request.alpha > 0 && request.alpha <= 1.0,
                        "The alpha of request must be in range (0, 1], " +
                        "but got '%s'", request.alpha);
        E.checkArgument(request.top >= 0,
                        "The top must be >= 0, but got: %s",
                        request.minEdgeCount);

        LOG.debug("Graph [{}] get fusiform nodes from '{}' with " +
                  "direction '{}', edge label '{}', min edge count '{}', " +
                  "alpha '{}', group property '{}' and min group count '{}'",
                  graph, request.sources, request.direction, request.label,
                  request.minEdgeCount, request.alpha, request.groupProperty,
                  request.minGroupCount);

        HugeGraph g = graph(manager, graph);
        List<HugeVertex> sources = request.sources.sourcesVertices(g);
        E.checkArgument(sources != null && !sources.isEmpty(),
                        "The source vertices can't be empty");
        EdgeLabel edgeLabel = g.edgeLabel(request.label);
        E.checkArgument(edgeLabel.frequency() == Frequency.SINGLE,
                        "The edge label frequency must be single");

        FusiformSimilarityTraverser traverser =
                                    new FusiformSimilarityTraverser(g);
        return JsonUtil.toJson(traverser.fusiformSimilarity(
                               sources, request.direction, edgeLabel.id(),
                               request.minEdgeCount, request.degree,
                               request.alpha, request.top,
                               request.groupProperty, request.minGroupCount,
                               request.capacity, request.limit));
    }

    private static class FusiformSimilarityRequest {

        @JsonProperty("sources")
        public SourceVertices sources;
        @JsonProperty("label")
        public String label;
        @JsonProperty("direction")
        public Directions direction;
        @JsonProperty("min_edge_count")
        public int minEdgeCount;
        @JsonProperty("max_degree")
        public long degree = Long.valueOf(DEFAULT_DEGREE);
        @JsonProperty("alpha")
        public float alpha;
        @JsonProperty("top")
        public int top;
        @JsonProperty("group_property")
        public String groupProperty;
        @JsonProperty("min_group_count")
        public int minGroupCount;
        @JsonProperty("capacity")
        public long capacity = Long.valueOf(DEFAULT_CAPACITY);
        @JsonProperty("limit")
        public long limit = Long.valueOf(DEFAULT_PATHS_LIMIT);

        @Override
        public String toString() {
            return String.format("FusiformSimilarityRequest{sources=%s," +
                                 "label=%s,direction=%s,minEdgeCount=%s," +
                                 "degree=%s,alpha=%s,top=%s,groupProperty=%s," +
                                 "minGroupCount=%s,capacity=%s,limit=%s}",
                                 this.sources, this.label, this.direction,
                                 this.minEdgeCount, this.degree, this.alpha,
                                 this.top,  this.groupProperty,
                                 this.minGroupCount, this.capacity, this.limit);
        }
    }
}
