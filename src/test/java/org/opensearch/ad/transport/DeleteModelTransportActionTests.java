/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package org.opensearch.ad.transport;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Function;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.opensearch.Version;
import org.opensearch.action.ActionRequestValidationException;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.ad.caching.CacheProvider;
import org.opensearch.ad.caching.EntityCache;
import org.opensearch.ad.common.exception.JsonPathNotFoundException;
import org.opensearch.ad.constant.ADCommonMessages;
import org.opensearch.ad.feature.FeatureManager;
import org.opensearch.ad.ml.EntityColdStarter;
import org.opensearch.ad.ml.ModelManager;
import org.opensearch.ad.task.ADTaskCacheManager;
import org.opensearch.cluster.ClusterName;
import org.opensearch.cluster.node.DiscoveryNode;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.io.stream.BytesStreamOutput;
import org.opensearch.common.xcontent.XContentFactory;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.xcontent.ToXContent;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.timeseries.AbstractTimeSeriesTest;
import org.opensearch.timeseries.NodeStateManager;
import org.opensearch.transport.TransportService;

import test.org.opensearch.ad.util.JsonDeserializer;

import com.google.gson.JsonElement;

public class DeleteModelTransportActionTests extends AbstractTimeSeriesTest {
    private DeleteModelTransportAction action;
    private String localNodeID;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        ThreadPool threadPool = mock(ThreadPool.class);

        ClusterService clusterService = mock(ClusterService.class);
        localNodeID = "foo";
        when(clusterService.localNode()).thenReturn(new DiscoveryNode(localNodeID, buildNewFakeTransportAddress(), Version.CURRENT));
        when(clusterService.getClusterName()).thenReturn(new ClusterName("test"));

        TransportService transportService = mock(TransportService.class);
        ActionFilters actionFilters = mock(ActionFilters.class);
        NodeStateManager nodeStateManager = mock(NodeStateManager.class);
        ModelManager modelManager = mock(ModelManager.class);
        FeatureManager featureManager = mock(FeatureManager.class);
        CacheProvider cacheProvider = mock(CacheProvider.class);
        EntityCache entityCache = mock(EntityCache.class);
        when(cacheProvider.get()).thenReturn(entityCache);
        ADTaskCacheManager adTaskCacheManager = mock(ADTaskCacheManager.class);
        EntityColdStarter coldStarter = mock(EntityColdStarter.class);

        action = new DeleteModelTransportAction(
            threadPool,
            clusterService,
            transportService,
            actionFilters,
            nodeStateManager,
            modelManager,
            featureManager,
            cacheProvider,
            adTaskCacheManager,
            coldStarter
        );
    }

    public void testNormal() throws IOException, JsonPathNotFoundException {
        DeleteModelRequest request = new DeleteModelRequest("123");
        assertThat(request.validate(), is(nullValue()));

        DeleteModelNodeRequest nodeRequest = new DeleteModelNodeRequest(request);
        BytesStreamOutput nodeRequestOut = new BytesStreamOutput();
        nodeRequestOut.setVersion(Version.CURRENT);
        nodeRequest.writeTo(nodeRequestOut);
        StreamInput siNode = nodeRequestOut.bytes().streamInput();

        DeleteModelNodeRequest nodeResponseRead = new DeleteModelNodeRequest(siNode);

        DeleteModelNodeResponse nodeResponse1 = action.nodeOperation(nodeResponseRead);
        DeleteModelNodeResponse nodeResponse2 = action.nodeOperation(new DeleteModelNodeRequest(request));

        DeleteModelResponse response = action.newResponse(request, Arrays.asList(nodeResponse1, nodeResponse2), Collections.emptyList());

        assertEquals(2, response.getNodes().size());
        assertTrue(!response.hasFailures());

        XContentBuilder builder = XContentFactory.jsonBuilder();
        builder.startObject();
        response.toXContent(builder, ToXContent.EMPTY_PARAMS);
        builder.endObject();

        String json = builder.toString();
        Function<JsonElement, String> function = (s) -> {
            try {
                return JsonDeserializer.getTextValue(s, CronNodeResponse.NODE_ID);
            } catch (Exception e) {
                Assert.fail(e.getMessage());
            }
            return null;
        };
        assertArrayEquals(
            JsonDeserializer.getArrayValue(json, function, CronResponse.NODES_JSON_KEY),
            new String[] { localNodeID, localNodeID }
        );
    }

    public void testEmptyDetectorID() {
        ActionRequestValidationException e = new DeleteModelRequest().validate();
        assertThat(e.validationErrors(), Matchers.hasItem(ADCommonMessages.AD_ID_MISSING_MSG));
    }
}
