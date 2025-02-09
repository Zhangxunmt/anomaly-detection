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

import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opensearch.ad.model.ADTask;
import org.opensearch.ad.model.AnomalyDetector;
import org.opensearch.ad.model.DetectorProfile;
import org.opensearch.common.io.stream.BytesStreamOutput;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.rest.RestStatus;
import org.opensearch.timeseries.model.Job;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(GetAnomalyDetectorResponse.class)
public class GetAnomalyDetectorActionTests {
    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void testGetRequest() throws IOException {
        BytesStreamOutput out = new BytesStreamOutput();
        GetAnomalyDetectorRequest request = new GetAnomalyDetectorRequest("1234", 4321, false, false, "nonempty", "", false, null);
        request.writeTo(out);
        StreamInput input = out.bytes().streamInput();
        GetAnomalyDetectorRequest newRequest = new GetAnomalyDetectorRequest(input);
        Assert.assertEquals(request.getDetectorID(), newRequest.getDetectorID());

    }

    @Test
    public void testGetResponse() throws Exception {
        BytesStreamOutput out = new BytesStreamOutput();
        AnomalyDetector detector = Mockito.mock(AnomalyDetector.class);
        Job detectorJob = Mockito.mock(Job.class);
        Mockito.doNothing().when(detector).writeTo(out);
        GetAnomalyDetectorResponse response = new GetAnomalyDetectorResponse(
            1234,
            "4567",
            9876,
            2345,
            detector,
            detectorJob,
            false,
            Mockito.mock(ADTask.class),
            Mockito.mock(ADTask.class),
            false,
            RestStatus.OK,
            Mockito.mock(DetectorProfile.class),
            null,
            false
        );
        response.writeTo(out);
        StreamInput input = out.bytes().streamInput();
        PowerMockito.whenNew(AnomalyDetector.class).withAnyArguments().thenReturn(detector);
        GetAnomalyDetectorResponse newResponse = new GetAnomalyDetectorResponse(input);
        Assert.assertNotNull(newResponse);
    }
}
